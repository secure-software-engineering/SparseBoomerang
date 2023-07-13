package boomerang.scene.sparse.aliasaware;

import boomerang.scene.Pair;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGBuilder;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import com.google.common.graph.MutableGraph;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;
import sootup.core.types.Type;

/** Value is the type of DFF */
public class AliasAwareSparseCFGBuilder extends SparseCFGBuilder {

  private Deque<Value> backwardWorklist;
  private Deque<Value> forwardWorklist;
  private Map<Value, Stmt> definitions;
  private Map<Value, LinkedHashSet<Stmt>> valueToUnits;
  private Map<Value, Pair<Value, Stmt>> valueKillebyValuedAt;
  private Type queryVarType;
  private SootMethod currentMethod;
  private Stmt queryStmt;
  private String activePass;
  private int lastNodeIndex = -1;

  private static final Logger LOGGER = Logger.getLogger(AliasAwareSparseCFGBuilder.class.getName());

  private boolean enableExceptions;
  private boolean ignoreAfterQuery;

  public AliasAwareSparseCFGBuilder(boolean enableExceptions, boolean ignoreAfterQuery) {
    this.enableExceptions = enableExceptions;
    this.ignoreAfterQuery = ignoreAfterQuery;
    init();
  }

  private void init() {
    backwardWorklist = new ArrayDeque<>();
    forwardWorklist = new ArrayDeque<>();
    definitions = new HashMap<>();
    valueToUnits = new HashMap<>();
    valueKillebyValuedAt = new HashMap<>();
  }

  public SparseAliasingCFG buildSparseCFG(
      Val initialQueryVar, SootMethod m, Val queryVar, Stmt queryStmt, SparseCFGQueryLog queryLog) {
    currentMethod = m;
    this.queryStmt = queryStmt;
    queryVarType = SootAdapter.getTypeOfVal(initialQueryVar);

    StmtGraph<?> stmtGraph = m.getBody().getStmtGraph();

    Stmt head = getHead(stmtGraph);

    MutableGraph<Stmt> mCFG = numberStmtsAndConvertToMutableGraph(stmtGraph);

    int initialStmtCount = mCFG.nodes().size();
    // if (isDebugTarget()) {
    // LOGGER.info(m.getName() + " original");
    // logCFG(LOGGER, mCFG);
    // }
    //    if (!m.getName().equals("main")) {

    findStmtsToKeep(mCFG, SootAdapter.asValue(queryVar), queryStmt);

    List<Stmt> tails = stmtGraph.getTails();
    for (Stmt tail : tails) {
      sparsify(mCFG, head, tail, queryStmt);
    }
    // if (isDebugTarget()) {
    // LOGGER.info(m.getName() + " aa-sparse");
    // logCFG(LOGGER, mCFG);
    // }
    //    }
    int finalStmtCount = mCFG.nodes().size();
    queryLog.setInitialStmtCount(initialStmtCount);
    queryLog.setFinalStmtCount(finalStmtCount);
    return new SparseAliasingCFG(queryVar, mCFG, queryStmt, valueToUnits.keySet(), unitToNumber);
  }

  private void sparsify(MutableGraph<Stmt> mCFG, Stmt head, Stmt tail, Stmt queryStmt) {
    Iterator<Stmt> iter = getBFSIterator(mCFG, head);
    Set<Stmt> stmsToRemove = new HashSet<>();
    while (iter.hasNext()) {
      Stmt unit = iter.next();
      if (!isControlStmt(unit)
          && !existInValueToUnits(unit)
          && !unit.equals(queryStmt)
          && !unit.equals(head)
          && !unit.equals(tail)
          && !isTargetCallSite(unit)) {
        stmsToRemove.add(unit);
      }
    }
    for (Stmt unit : stmsToRemove) {
      Set<Stmt> preds = mCFG.predecessors(unit);
      Set<Stmt> succs = mCFG.successors(unit);
      if (preds.size() == 1 && succs.size() == 1) {
        // TODO: revise this
        // if a node has multiple in and out edges, don't remove it
        mCFG.removeNode(unit);
        mCFG.putEdge(preds.iterator().next(), succs.iterator().next());
      }
    }
  }

  private boolean isTargetCallSite(Stmt stmt) {
    if (stmt.containsInvokeExpr()) {
      AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
      List<Immediate> args = invokeExpr.getArgs();
      for (Value d : valueToUnits.keySet()) {
        // v as arg
        for (Value arg : args) {
          if (d.equals(arg)) {
            return true;
          }
        }
        // v as base v.m()
        if (isInvokeBase(d, invokeExpr)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean existInValueToUnits(Stmt stmt) {
    for (Set<Stmt> units : valueToUnits.values()) {
      for (Stmt unit : units) {
        if (stmt.equals(unit)) {
          return true;
        }
      }
    }
    return false;
  }

  private void findStmtsToKeep(MutableGraph<Stmt> mCFG, Value queryVar, Stmt queryStmt) {
    activePass = "findBackwardDef";
    Stmt def = findBackwardDefForValue(mCFG, queryStmt, queryVar, new HashSet<>(), false);
    putToValueToUnits(queryVar, def);
    activePass = "backwardPass";
    backwardPass(mCFG, def);
    activePass = "forwardPass";
    forwardPass(mCFG, queryStmt);
    activePass = "findStmtsForFieldBase";
    findStmtsForFieldBase(mCFG, queryStmt);
    activePass = "findStmtsAfterKill";
    findStmtsAfterKill(mCFG, queryStmt);
  }

  private void findStmtsForFieldBase(MutableGraph<Stmt> mCFG, Stmt queryStmt) {
    while (!backwardWorklist.isEmpty()) {
      backwardPass(mCFG, queryStmt);
      while (!forwardWorklist.isEmpty()) {
        forwardPass(mCFG, queryStmt);
      }
    }
  }

  private void findStmtsAfterKill(MutableGraph<Stmt> mCFG, Stmt queryStmt) {
    Stmt def;
    while (!backwardWorklist.isEmpty()) {
      Value val = popBackwardStack();
      Stmt killedAt = null;
      Collection<Pair<Value, Stmt>> killers = valueKillebyValuedAt.values();
      for (Pair<Value, Stmt> killer : killers) {
        if (killer.getX().equals(val)) {
          killedAt = killer.getY();
          break;
        }
      }
      if (killedAt != null) {
        def = findBackwardDefForValue(mCFG, killedAt, val, new HashSet<>(), false);
        putToValueToUnits(val, def);
      } else {
        throw new RuntimeException("if a val is killed there must be a killer");
      }
    }
    forwardPass(mCFG, queryStmt);
  }

  private void backwardPass(MutableGraph<Stmt> mCFG, Stmt def) {
    while (!backwardWorklist.isEmpty()) {
      Value val = popBackwardStack();
      Stmt existingDef = definitions.get(val);
      Stmt newDef;
      if (existingDef != null) {
        newDef = findBackwardDefForValue(mCFG, existingDef, val, new HashSet<>(), true);
      } else {
        newDef = findBackwardDefForValue(mCFG, def, val, new HashSet<>(), false);
      }
      def = newDef != null && !(newDef instanceof DefinedOutside) ? newDef : def;
      if (def != null) {
        putToValueToUnits(val, def);
      }
    }
  }

  private void forwardPass(MutableGraph<Stmt> mCFG, Stmt queryStmt) {
    while (!forwardWorklist.isEmpty()) {
      Value val = popForwardStack();
      Stmt defStmt = definitions.get(val);
      putToValueToUnits(val, defStmt);
      Set<Stmt> unitsForVal =
          findForwardDefUseForValue(mCFG, defStmt, val, queryStmt, new HashSet<>());
      for (Stmt unit : unitsForVal) {
        putToValueToUnits(val, unit);
      }
    }
  }

  private boolean isDebugTarget() {
    return false
        && currentMethod.getName().equals("main")
        && queryStmt.toString().startsWith("b_q1");
  }

  private void logStackOp(Value val, String op) {
    if (isDebugTarget()) {
      System.out.println(activePass + ": " + op + ": " + val);
    }
  }

  private void pushToForwardStack(Value val) {
    logStackOp(val, "Forw push");
    forwardWorklist.push(val);
  }

  private void pushToBackwardStack(Value val) {
    logStackOp(val, "Back push");
    backwardWorklist.push(val);
  }

  private Value popBackwardStack() {
    Value val = backwardWorklist.pop();
    logStackOp(val, "Back pop");
    return val;
  }

  private Value popForwardStack() {
    Value val = forwardWorklist.pop();
    logStackOp(val, "Forw pop");
    return val;
  }

  private void putToValueToUnits(Value val, Stmt stmt) {
    if (valueToUnits.containsKey(val)) {
      Set<Stmt> units = valueToUnits.get(val);
      units.add(stmt);
    } else {
      LinkedHashSet<Stmt> units = new LinkedHashSet<>();
      units.add(stmt);
      valueToUnits.put(val, units);
    }
    if (isDebugTarget()) {
      System.out.println("putToValueToUnits:");
      for (Value k : valueToUnits.keySet()) {
        System.out.print(k + ": ");
        System.out.println(
            valueToUnits.get(k).stream().map(Objects::toString).collect(Collectors.joining("---")));
      }
    }
  }

  private Stmt findBackwardDefForValue(
      MutableGraph<Stmt> mCFG, Stmt tail, Value queryVar, Set<Stmt> visited, boolean existingDef) {
    if (tail == null) {
      return null;
    }
    visited.add(tail);
    if (!existingDef && isDefOfValue(tail, queryVar)) {
      return tail;
    }
    Set<Stmt> preds = mCFG.predecessors(tail);
    for (Stmt pred : preds) {
      if (!visited.contains(pred)) {
        Stmt u = findBackwardDefForValue(mCFG, pred, queryVar, visited, false);
        if (u == null) {
          continue;
        } else {
          return u;
        }
      }
    }
    if (queryVar instanceof JInstanceFieldRef) {
      return new DefinedOutside(queryVar);
    } else {
      return null; // throw new RuntimeException("No def found for value:" + queryVar);
    }
  }

  private boolean isDefOfValue(Stmt unit, Value queryVar) {
    if (unit instanceof JAssignStmt) {
      JAssignStmt stmt = (JAssignStmt) unit;
      Value leftOp = stmt.getLeftOp();
      Value rightOp = stmt.getRightOp();
      // handle Casts (Not mentioned)
      if (rightOp instanceof JCastExpr) {
        JCastExpr cast = (JCastExpr) rightOp;
        rightOp = cast.getOp();
      }
      if (queryVar.equals(leftOp)
          || equalsFieldRef(leftOp, queryVar)
          || equalsQueryBase(leftOp, queryVar)
          || equalsArrayItem(leftOp, queryVar)
          || equalsFieldType(leftOp, queryVar)) {
        if (isAllocOrMethodAssignment(stmt, queryVar)) {
          pushToForwardStack(queryVar);
          Set<Value> params = getInvokeBaseAndParams(rightOp, queryVar);
          params.forEach(backwardWorklist::add);
        } else {
          if (equalsFieldRef(rightOp, queryVar)
              && rightOp instanceof JInstanceFieldRef) { // recursion
            Value base = ((JInstanceFieldRef) rightOp).getBase();
            if (base.equals(leftOp)) {
              pushToBackwardStack(leftOp);
              definitions.put(leftOp, unit);
              pushToForwardStack(rightOp);
              definitions.put(rightOp, unit);
              return false;
            }
          }
          if (rightOp instanceof JInstanceFieldRef) {
            Value base = ((JInstanceFieldRef) rightOp).getBase();
            pushToBackwardStack(base);
            // return true;
          }
          pushToBackwardStack(rightOp);
        }
        definitions.put(queryVar, unit);
        return true;
      }
    } else if (unit instanceof JIdentityStmt) {
      JIdentityStmt stmt = (JIdentityStmt) unit;
      Value leftOp = stmt.getLeftOp();
      if (queryVar.equals(leftOp)
          || equalsFieldRef(leftOp, queryVar)
          || equalsArrayItem(leftOp, queryVar)) {
        // we treat it like alloc
        pushToForwardStack(queryVar);
        definitions.put(queryVar, unit);
        return true;
      }
    }
    return false;
  }

  private boolean equalsFieldRef(Value op, Value queryVar) {
    if (op instanceof JInstanceFieldRef && queryVar instanceof JInstanceFieldRef) {
      return ((JInstanceFieldRef) queryVar).getBase().equals(((JInstanceFieldRef) op).getBase())
          && ((JInstanceFieldRef) queryVar)
              .getFieldSignature()
              .equals(((JInstanceFieldRef) op).getFieldSignature());
    } else if (op instanceof JStaticFieldRef && queryVar instanceof JStaticFieldRef) {
      return ((JStaticFieldRef) op)
          .getFieldSignature()
          .equals(((JStaticFieldRef) queryVar).getFieldSignature());
    }
    return false;
  }

  private boolean equalsArrayItem(Value op, Value queryVar) {
    if (op instanceof JArrayRef && queryVar instanceof JArrayRef) {
      return ((JArrayRef) queryVar).getBase().equals(((JArrayRef) op).getBase())
          && ((JArrayRef) queryVar).getIndex().equals(((JArrayRef) op).getIndex());
    }
    return false;
  }

  private int getLastNodeIndex() {
    if (lastNodeIndex == -1) {
      lastNodeIndex =
          Collections.max(unitToNumber.entrySet(), Comparator.comparingInt(Map.Entry::getValue))
              .getValue();
    }
    return lastNodeIndex;
  }

  /**
   * Idealy it makes sense to stop searching for stmts after the query stmt, because of flow
   * sensitivity. But boomerang apparently sometimes need the whole method.
   *
   * @param currentStmt
   * @param queryStmt
   * @return
   */
  private boolean shouldStopSearch(Stmt currentStmt, Stmt queryStmt) {
    if (ignoreAfterQuery) {
      return unitToNumber.get(currentStmt) >= unitToNumber.get(queryStmt);
    } else {
      return unitToNumber.get(currentStmt) >= getLastNodeIndex();
    }
  }

  private Set<Stmt> findForwardDefUseForValue(
      MutableGraph<Stmt> mCFG, Stmt head, Value queryVar, Stmt queryStmt, Set<Stmt> visited) {
    visited.add(head);
    Set<Stmt> stmtsToKeep = new HashSet<>();
    Set<Stmt> succs = mCFG.successors(head);
    for (Stmt succ : succs) {
      if (shouldStopSearch(succ, queryStmt)) {
        // do not process after queryStmt
        return stmtsToKeep;
      }
      if (!valueKillebyValuedAt.containsKey(queryVar)
          && keepContainingStmtsForward(succ, queryVar)) {
        stmtsToKeep.add(succ);
      }
      if (!visited.contains(succ)) {
        Set<Stmt> ret = findForwardDefUseForValue(mCFG, succ, queryVar, queryStmt, visited);
        stmtsToKeep.addAll(ret);
      }
    }
    return stmtsToKeep;
  }

  /**
   * @param unit
   * @param queryVar
   * @return
   */
  private boolean keepContainingStmtsForward(Stmt unit, Value queryVar) {
    if (unit instanceof JAssignStmt) {
      JAssignStmt stmt = (JAssignStmt) unit;
      Value leftOp = stmt.getLeftOp();
      Value rightOp = stmt.getRightOp();

      // handle Casts (Not mentioned)
      if (rightOp instanceof JCastExpr) {
        JCastExpr cast = (JCastExpr) rightOp;
        rightOp = cast.getOp();
      }
      if (queryVar.equals(rightOp)
          || equalsFieldRef(rightOp, queryVar)
          || equalsQueryBase(rightOp, queryVar)
          || equalsFieldType(rightOp, queryVar)
          || equalsArrayItem(rightOp, queryVar)
          || equalsInitialFieldType(rightOp, queryVar)) {
        if (!leftOp.equals(queryVar)) { // oth it would create a loop
          pushToForwardStack(leftOp);
          definitions.put(leftOp, unit);
        }
        if (leftOp instanceof JInstanceFieldRef) {
          // we need to find how the base was created too
          Value base = ((JInstanceFieldRef) leftOp).getBase();
          pushToBackwardStack(base);
        }
        return true;
      } else if (queryVar.equals(leftOp)
          || equalsFieldRef(leftOp, queryVar)
          || equalsQueryBase(leftOp, queryVar)
          || equalsArrayItem(leftOp, queryVar)) {
        // kill: need to find the alloc of the killer object
        valueKillebyValuedAt.put(queryVar, new Pair<>(rightOp, unit));
        pushToBackwardStack(rightOp);
        return true;
      } else if (equalsInitialFieldType(leftOp, queryVar)) {
        pushToBackwardStack(rightOp);
        return true;
      }
    }
    if (keepInvokeForValue(unit, queryVar)) {
      return true;
    }
    return false;
  }

  /**
   * queryVar is a field reference, and its base equals current rightOp e.g.: queryVar: box.f box2 =
   * box we want to find this
   *
   * @param rightOp
   * @param queryVar
   * @return
   */
  private boolean equalsQueryBase(Value rightOp, Value queryVar) {
    if (queryVar instanceof JInstanceFieldRef) {
      Value queryBase = ((JInstanceFieldRef) queryVar).getBase();
      if (queryBase.equals(rightOp)) {
        return true;
      }
    }
    return false;
  }

  private boolean equalsInitialFieldType(Value op, Value queryVar) {
    if (op instanceof JInstanceFieldRef) {
      Value base = ((JInstanceFieldRef) op).getBase();
      Type fieldType = ((JInstanceFieldRef) op).getType();
      if (base.equals(queryVar) && fieldType.equals(queryVarType)) {
        return true;
      }
    }
    return false;
  }

  /**
   * queryVar: this leftOp: this.N and N is of the same type as this we want to keep this.N because
   * it might be used for recursive field store
   *
   * @param op
   * @param queryVar
   * @return
   */
  private boolean equalsFieldType(Value op, Value queryVar) {
    if (op instanceof JInstanceFieldRef) {
      Value base = ((JInstanceFieldRef) op).getBase();
      Type fieldType = ((JInstanceFieldRef) op).getType();
      if (base.equals(queryVar) && fieldType.equals(queryVar.getType())) {
        return true;
      }
    }
    return false;
  }

  /*
  private boolean isAllocOrMethodAssignment(JAssignStmt stmt, Value d) {
    Value rightOp = stmt.getRightOp();
    if (rightOp instanceof JNewExpr) {
      JNewExpr newExpr = (JNewExpr) rightOp;
      Type type = newExpr.getType();
      if (type.equals(d.getType())) {
        return true;
      }
    } else if (rightOp instanceof JSpecialInvokeExpr
            || rightOp instanceof JStaticInvokeExpr
            || rightOp instanceof JVirtualInvokeExpr
            || rightOp instanceof JInterfaceInvokeExpr
            || rightOp instanceof JDynamicInvokeExpr) {
      return true;
    }
    return false;
  }*/

  private Set<Value> getInvokeBaseAndParams(Value invoke, Value d) {
    Set<Value> otherArgs = new HashSet<>();
    if (invoke instanceof AbstractInvokeExpr) {
      List<Immediate> args = ((AbstractInvokeExpr) invoke).getArgs();
      for (Value arg : args) {
        if (!arg.equals(d) && (arg.getType().equals(d.getType()))
            || arg.getType().equals(queryVarType)) {
          otherArgs.add(arg);
        }
      }
    }
    if (invoke instanceof AbstractInstanceInvokeExpr) {
      Value base = ((AbstractInstanceInvokeExpr) invoke).getBase();
      otherArgs.add(base);
    }
    return otherArgs;
  }

  private boolean keepInvokeForValue(Stmt unit, Value d) {
    if (unit instanceof Stmt) {
      Stmt stmt = (Stmt) unit;
      if (stmt.containsInvokeExpr()) {
        AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Immediate> args = invokeExpr.getArgs();
        // v as arg
        // TODO: check this looks absurd
        for (Value arg : args) {
          if (d.equals(arg)) {
            for (Value otherArg : args) {
              if (!otherArg.equals(d) && otherArg.getType().equals(d.getType())) {
                // other args of the same type can cause aliasing so we need to find their alloc
                // sites
                if (!definitions.containsKey(otherArg)) {
                  pushToBackwardStack(otherArg);
                }
              }
            }
            return true;
          }
        }
        // v as base v.m()
        if (isInvokeBase(d, invokeExpr)) {
          Set<Value> params = getInvokeBaseAndParams(invokeExpr, d);
          params.forEach(backwardWorklist::push);
          return true;
        }
      }
    }
    return false;
  }

  private boolean isInvokeBase(Value d, AbstractInvokeExpr invokeExpr) {
    if (invokeExpr instanceof JVirtualInvokeExpr) {
      Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
      if (d.equals(base)) {
        return true;
      }
    } else if (invokeExpr instanceof JSpecialInvokeExpr) {
      Value base = ((JSpecialInvokeExpr) invokeExpr).getBase();
      if (d.equals(base)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAllocOrMethodAssignment(JAssignStmt stmt, Value d) {
    Value rightOp = stmt.getRightOp();
    if (rightOp instanceof JNewExpr) {
      JNewExpr newExpr = (JNewExpr) rightOp;
      Type type = newExpr.getType();
      if (type.equals(d.getType())) {
        return true;
      }
    } else if (rightOp instanceof JSpecialInvokeExpr
        || rightOp instanceof JStaticInvokeExpr
        || rightOp instanceof JVirtualInvokeExpr
        || rightOp instanceof JInterfaceInvokeExpr
        || rightOp instanceof JDynamicInvokeExpr) {
      return true;
    }
    return false;
  }

  /**
   * DFS traverse Original Graph and keep all the stmts
   *
   * @param curr
   * @param graph
   * @param cfg
   */
  private void buildCompleteCFG(Stmt curr, StmtGraph<?> graph, SparseAliasingCFG cfg) {
    List<Stmt> succs = graph.successors(curr);
    if (succs == null || succs.isEmpty()) {
      return;
    }
    for (Stmt succ : succs) {
      if (cfg.addEdge(curr, succ)) {
        buildCompleteCFG(succ, graph, cfg);
      }
    }
  }
}
