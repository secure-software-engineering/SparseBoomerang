package boomerang.scene.sparse;

import boomerang.scene.Pair;
import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.StaticFieldRef;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

/** Value is the type of DFF */
public class SparseAliasingCFGBuilder {

  Map<Unit, Integer> unitToNumber = new HashMap<>();

  private Deque<Value> backwardStack = new ArrayDeque<>();
  private Deque<Value> forwardStack = new ArrayDeque<>();
  private Map<Value, Unit> definitions = new HashMap<>();
  private Map<Value, LinkedHashSet<Unit>> valueToUnits = new HashMap<>();
  private Map<Value, Pair<Value, Unit>> valueKillebyValuedAt = new HashMap<>();
  private Type queryVarType;
  private Unit targetStmt;

  private static final Logger LOGGER = Logger.getLogger(SparseAliasingCFGBuilder.class.getName());

  private boolean enableExceptions;

  public SparseAliasingCFGBuilder(boolean enableExceptions) {
    this.enableExceptions = enableExceptions;
  }

  public SparseAliasingCFG buildSparseCFG(
      Value initialQueryVar, SootMethod m, Value queryVar, Unit queryStmt, Unit targetStmt) {
    queryVarType = initialQueryVar.getType();
    DirectedGraph<Unit> unitGraph =
        (this.enableExceptions
            ? new ExceptionalUnitGraph(m.getActiveBody())
            : new BriefUnitGraph(m.getActiveBody()));

    Unit head = getHead(unitGraph);

    MutableGraph<Unit> mCFG = numberStmtsAndConvertToMutableGraph(unitGraph);
    LOGGER.info(m.getName() + " original");
    logCFG(mCFG);

    // if (!m.getName().equals("test")) {
    findStmtsToKeep(mCFG, queryVar, queryStmt);

    List<Unit> tails = unitGraph.getTails();
    for (Unit tail : tails) {
      sparsify(mCFG, head, tail, queryStmt);
    }
    LOGGER.info(m.getName() + " sparse");
    logCFG(mCFG);
    // }
    return new SparseAliasingCFG(queryVar, mCFG, queryStmt, valueToUnits.keySet());
  }

  private void sparsify(MutableGraph<Unit> mCFG, Unit head, Unit tail, Unit queryStmt) {
    Iterator<Unit> iter = getBFSIterator(mCFG, head);
    Set<Unit> stmsToRemove = new HashSet<>();
    while (iter.hasNext()) {
      Unit unit = iter.next();
      if (!isControlStmt(unit)
          && !existInValueToUnits(unit)
          && !unit.equals(queryStmt)
          && !unit.equals(head)
          && !unit.equals(tail)
          && !isTargetCallSite(unit)) {
        stmsToRemove.add(unit);
      }
    }
    for (Unit unit : stmsToRemove) {
      Set<Unit> preds = mCFG.predecessors(unit);
      Set<Unit> succs = mCFG.successors(unit);
      if (preds.size() == 1 && succs.size() == 1) {
        // TODO: revise this
        // if a node has multiple in and out edges, don't remove it
        mCFG.removeNode(unit);
        mCFG.putEdge(preds.iterator().next(), succs.iterator().next());
      }
    }
  }

  private boolean isTargetCallSite(Unit unit) {
    if (unit instanceof Stmt) {
      Stmt stmt = (Stmt) unit;
      if (stmt.containsInvokeExpr()) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Value> args = invokeExpr.getArgs();
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
    }
    return false;
  }

  private boolean existInValueToUnits(Unit stmt) {
    for (Set<Unit> units : valueToUnits.values()) {
      for (Unit unit : units) {
        if (unit.equals(stmt)) {
          return true;
        }
      }
    }
    return false;
  }

  private boolean isControlStmt(Unit stmt) {
    if (stmt instanceof JIfStmt
        || stmt instanceof JNopStmt
        || stmt instanceof JGotoStmt
        || stmt instanceof JReturnStmt
        || stmt instanceof JReturnVoidStmt) {
      return true;
    }
    if (stmt instanceof JIdentityStmt) {
      //      JIdentityStmt id = (JIdentityStmt) stmt;
      //      if (id.getRightOp() instanceof JCaughtExceptionRef) {
      return true;
      //      }
    }
    return false;
  }

  private void findStmtsToKeep(MutableGraph<Unit> mCFG, Value queryVar, Unit queryStmt) {
    Unit def = findBackwardDefForValue(mCFG, queryStmt, queryVar, new HashSet<>(), false);
    putToValueToUnits(queryVar, def);
    backwardPass(mCFG, def);
    forwardPass(mCFG, queryStmt);
    findStmtsForFieldBase(mCFG, queryStmt);
    findStmtsAfterKill(mCFG, queryStmt);
  }

  private void findStmtsForFieldBase(MutableGraph<Unit> mCFG, Unit queryStmt) {
    while (!backwardStack.isEmpty()) {
      backwardPass(mCFG, queryStmt);
      while (!forwardStack.isEmpty()) {
        forwardPass(mCFG, queryStmt);
      }
    }
  }

  private void findStmtsAfterKill(MutableGraph<Unit> mCFG, Unit queryStmt) {
    Unit def;
    while (!backwardStack.isEmpty()) {
      Value val = backwardStack.pop();
      Unit killedAt = null;
      Collection<Pair<Value, Unit>> killers = valueKillebyValuedAt.values();
      for (Pair<Value, Unit> killer : killers) {
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

  private void backwardPass(MutableGraph<Unit> mCFG, Unit def) {
    while (!backwardStack.isEmpty()) {
      Value val = backwardStack.pop();
      Unit existingDef = definitions.get(val);
      if (existingDef != null) {
        def = findBackwardDefForValue(mCFG, existingDef, val, new HashSet<>(), true);
      } else {
        def = findBackwardDefForValue(mCFG, def, val, new HashSet<>(), false);
      }
      if (def != null) {
        putToValueToUnits(val, def);
      }
    }
  }

  private void forwardPass(MutableGraph<Unit> mCFG, Unit queryStmt) {
    while (!forwardStack.isEmpty()) {
      Value val = forwardStack.pop();
      Unit defStmt = definitions.get(val);
      putToValueToUnits(val, defStmt);
      Set<Unit> unitsForVal =
          findForwardDefUseForValue(mCFG, defStmt, val, queryStmt, new HashSet<>());
      for (Unit unit : unitsForVal) {
        putToValueToUnits(val, unit);
      }
    }
  }

  private void putToValueToUnits(Value val, Unit stmt) {
    if (valueToUnits.containsKey(val)) {
      Set<Unit> units = valueToUnits.get(val);
      units.add(stmt);
    } else {
      LinkedHashSet<Unit> units = new LinkedHashSet<>();
      units.add(stmt);
      valueToUnits.put(val, units);
    }
  }

  private Unit findBackwardDefForValue(
      MutableGraph<Unit> mCFG, Unit tail, Value queryVar, Set<Unit> visited, boolean existingDef) {
    visited.add(tail);
    if (!existingDef && isDefOfValue(tail, queryVar)) {
      return tail;
    }
    Set<Unit> preds = mCFG.predecessors(tail);
    for (Unit pred : preds) {
      if (!visited.contains(pred)) {
        Unit u = findBackwardDefForValue(mCFG, pred, queryVar, visited, false);
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

  private boolean isDefOfValue(Unit unit, Value queryVar) {
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
          forwardStack.push(queryVar);
        } else {
          if (equalsFieldRef(rightOp, queryVar)
              && rightOp instanceof JInstanceFieldRef) { // recursion
            Value base = ((JInstanceFieldRef) rightOp).getBase();
            if (base.equals(leftOp)) {
              backwardStack.push(leftOp);
              definitions.put(leftOp, unit);
              forwardStack.push(rightOp);
              definitions.put(rightOp, unit);
              return false;
            }
          }
          backwardStack.push(rightOp);
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
        forwardStack.push(queryVar);
        definitions.put(queryVar, unit);
        return true;
      }
    }
    return false;
  }

  private boolean equalsFieldRef(Value op, Value queryVar) {
    if (op instanceof JInstanceFieldRef && queryVar instanceof JInstanceFieldRef) {
      return ((JInstanceFieldRef) queryVar).getBase().equals(((JInstanceFieldRef) op).getBase())
          && ((JInstanceFieldRef) queryVar).getField().equals(((JInstanceFieldRef) op).getField());
    } else if (op instanceof StaticFieldRef && queryVar instanceof StaticFieldRef) {
      return ((StaticFieldRef) op).getFieldRef().equals(((StaticFieldRef) queryVar).getFieldRef());
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

  private Set<Unit> findForwardDefUseForValue(
      MutableGraph<Unit> mCFG, Unit head, Value queryVar, Unit queryStmt, Set<Unit> visited) {
    visited.add(head);
    Set<Unit> stmtsToKeep = new HashSet<>();
    Set<Unit> succs = mCFG.successors(head);
    for (Unit succ : succs) {
      if (unitToNumber.get(succ) >= unitToNumber.get(queryStmt)) {
        // do not process after queryStmt
        return stmtsToKeep;
      }
      if (!valueKillebyValuedAt.containsKey(queryVar)
          && keepContainingStmtsForward(succ, queryVar)) {
        stmtsToKeep.add(succ);
      }
      if (!visited.contains(succ)) {
        Set<Unit> ret = findForwardDefUseForValue(mCFG, succ, queryVar, queryStmt, visited);
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
  private boolean keepContainingStmtsForward(Unit unit, Value queryVar) {
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
          forwardStack.push(leftOp);
          definitions.put(leftOp, unit);
        }
        if (leftOp instanceof JInstanceFieldRef) {
          // we need to find how the base was created too
          Value base = ((JInstanceFieldRef) leftOp).getBase();
          backwardStack.push(base);
        }
        return true;
      } else if (queryVar.equals(leftOp)
          || equalsFieldRef(leftOp, queryVar)
          || equalsQueryBase(leftOp, queryVar)
          || equalsArrayItem(leftOp, queryVar)) {
        // kill: need to find the alloc of the killer object
        valueKillebyValuedAt.put(queryVar, new Pair<>(rightOp, unit));
        backwardStack.push(rightOp);
        return true;
      } else if (equalsInitialFieldType(leftOp, queryVar)) {
        backwardStack.push(rightOp);
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
      Type fieldType = ((JInstanceFieldRef) op).getField().getType();
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
      Type fieldType = ((JInstanceFieldRef) op).getField().getType();
      if (base.equals(queryVar) && fieldType.equals(queryVar.getType())) {
        return true;
      }
    }
    return false;
  }

  private boolean keepInvokeForValue(Unit unit, Value d) {
    if (unit instanceof Stmt) {
      Stmt stmt = (Stmt) unit;
      if (stmt.containsInvokeExpr()) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Value> args = invokeExpr.getArgs();
        // v as arg
        for (Value arg : args) {
          if (d.equals(arg)) {
            for (Value otherArg : args) {
              if (!otherArg.equals(d) && otherArg.getType().equals(d.getType())) {
                // other args of the same type can cause aliasing so we need to find their alloc
                // sites
                if (!definitions.containsKey(otherArg)) {
                  backwardStack.push(otherArg);
                }
              }
            }
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

  private boolean isInvokeBase(Value d, InvokeExpr invokeExpr) {
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

  private void logCFG(MutableGraph<Unit> graph) {
    LOGGER.info(
        graph.nodes().stream()
            .map(Objects::toString)
            .collect(Collectors.joining(System.lineSeparator())));
  }

  private Iterator<Unit> getBFSIterator(MutableGraph<Unit> graph, Unit head) {
    Traverser<Unit> traverser = Traverser.forGraph(graph);
    return traverser.breadthFirst(head).iterator();
  }

  private Unit getHead(DirectedGraph<Unit> graph) {
    List<Unit> heads = graph.getHeads();
    if (heads.size() > 1) {
      throw new RuntimeException("Multiple Heads");
    }
    return heads.get(0);
  }

  private MutableGraph<Unit> numberStmtsAndConvertToMutableGraph(DirectedGraph<Unit> rawGraph) {
    MutableGraph<Unit> mGraph = GraphBuilder.directed().build();
    Unit head = getHead(rawGraph);
    convertToMutableGraph(rawGraph, head, mGraph, 0);
    return mGraph;
  }

  private void convertToMutableGraph(
      DirectedGraph<Unit> graph, Unit curr, MutableGraph<Unit> mutableGraph, int depth) {
    Integer num = unitToNumber.get(curr);
    if (num == null || num < depth) {
      unitToNumber.put(curr, depth);
    }
    depth++;
    List<Unit> succsOf = graph.getSuccsOf(curr);
    for (Unit succ : succsOf) {
      if (!mutableGraph.hasEdgeConnecting(curr, succ)) {
        mutableGraph.putEdge(curr, succ);
        convertToMutableGraph(graph, succ, mutableGraph, depth);
      }
    }
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
  private void buildCompleteCFG(Unit curr, DirectedGraph<Unit> graph, SparseAliasingCFG cfg) {
    List<Unit> succs = graph.getSuccsOf(curr);
    if (succs == null || succs.isEmpty()) {
      return;
    }
    for (Unit succ : succs) {
      if (cfg.addEdge(curr, succ)) {
        buildCompleteCFG(succ, graph, cfg);
      }
    }
  }
}
