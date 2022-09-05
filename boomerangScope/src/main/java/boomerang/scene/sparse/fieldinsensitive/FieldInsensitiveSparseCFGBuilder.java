package boomerang.scene.sparse.fieldinsensitive;

import boomerang.scene.Pair;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGBuilder;
import boomerang.scene.sparse.aliasaware.DefinedOutside;
import com.google.common.graph.MutableGraph;
import java.util.*;
import java.util.logging.Logger;
import soot.SootMethod;
import soot.Type;
import soot.Unit;
import soot.Value;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class FieldInsensitiveSparseCFGBuilder extends SparseCFGBuilder {

  private Deque<Value> backwardStack = new ArrayDeque<>();
  private Deque<Value> forwardStack = new ArrayDeque<>();
  private Map<Value, Unit> definitions = new HashMap<>();
  private Map<Value, LinkedHashSet<Unit>> valueToUnits = new HashMap<>();
  private Map<Value, Pair<Value, Unit>> valueKillebyValuedAt = new HashMap<>();
  private Type queryVarType;

  private static final Logger LOGGER =
      Logger.getLogger(FieldInsensitiveSparseCFGBuilder.class.getName());

  private boolean enableExceptions;

  public FieldInsensitiveSparseCFGBuilder(boolean enableExceptions) {
    this.enableExceptions = enableExceptions;
  }

  public SparseAliasingCFG buildSparseCFG(
      Val initialQueryVar, SootMethod m, Val queryVar, Unit queryStmt) {
    queryVarType = SootAdapter.getTypeOfVal(initialQueryVar);
    DirectedGraph<Unit> unitGraph =
        (this.enableExceptions
            ? new ExceptionalUnitGraph(m.getActiveBody())
            : new BriefUnitGraph(m.getActiveBody()));

    Unit head = getHead(unitGraph);

    MutableGraph<Unit> mCFG = numberStmtsAndConvertToMutableGraph(unitGraph);
    // LOGGER.info(m.getName() + " original");
    // logCFG(LOGGER, mCFG);

    // if (!m.getName().equals("test")) {
    findStmtsToKeep(mCFG, SootAdapter.asValue(queryVar), queryStmt);

    List<Unit> tails = unitGraph.getTails();
    for (Unit tail : tails) {
      sparsify(mCFG, head, tail, queryStmt);
    }
    // LOGGER.info(m.getName() + " sparse");
    // logCFG(LOGGER, mCFG);
    // }
    return new SparseAliasingCFG(queryVar, mCFG, queryStmt, valueToUnits.keySet(), null);
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
          && !unit.equals(tail)) {
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

  private boolean existInValueToUnits(Unit stmt) {
    for (Set<Unit> units : valueToUnits.values()) {
      for (Unit unit : units) {
        if (stmt.equals(unit)) {
          return true;
        }
      }
    }
    return false;
  }

  private void findStmtsToKeep(MutableGraph<Unit> mCFG, Value queryVar, Unit queryStmt) {
    Unit def = findBackwardDefForValue(mCFG, queryStmt, queryVar, new HashSet<>(), false);
    putToValueToUnits(queryVar, def);
    backwardPass(mCFG, def);
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
      if (queryVar.equals(rightOp)) {
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
      } else if (queryVar.equals(leftOp)) {
        // kill: need to find the alloc of the killer object
        valueKillebyValuedAt.put(queryVar, new Pair<>(rightOp, unit));
        backwardStack.push(rightOp);
        return true;
      }
    }
    if (keepInvokeForValue(unit, queryVar)) {
      return true;
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
        // TODO: check this looks absurd
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
          Set<Value> params = getParams(invokeExpr, d);
          params.forEach(backwardStack::push);
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

  private Unit findBackwardDefForValue(
      MutableGraph<Unit> mCFG, Unit tail, Value queryVar, Set<Unit> visited, boolean existingDef) {
    if (tail == null) {
      return null;
    }
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
    if (unit instanceof AbstractDefinitionStmt) {
      AbstractDefinitionStmt stmt = (AbstractDefinitionStmt) unit;
      Value leftOp = stmt.getLeftOp();
      Value rightOp = stmt.getRightOp();
      // handle Casts (Not mentioned)
      if (rightOp instanceof JCastExpr) {
        JCastExpr cast = (JCastExpr) rightOp;
        rightOp = cast.getOp();
      }
      if (queryVar.equals(leftOp)) {
        if (isAllocLike(stmt, queryVar)) {
          forwardStack.push(queryVar);
          Set<Value> params = getParams(rightOp, queryVar);
          params.forEach(backwardStack::add);
        } else { // rightOp is not alloc or method call
          // TODO: recursion
          if (rightOp instanceof JInstanceFieldRef) { // only keep the base
            Value base = ((JInstanceFieldRef) rightOp).getBase();
            backwardStack.push(base);
          } else {
            backwardStack.push(rightOp);
          }
        }
        //
        definitions.put(queryVar, unit);
        return true;
      }
    }
    return false;
  }

  private boolean isAllocLike(AbstractDefinitionStmt stmt, Value d) {
    return isIdentityStmt(stmt, d) || isAlloc(stmt, d) || isMethodAssignment(stmt, d);
  }

  private boolean isIdentityStmt(AbstractDefinitionStmt stmt, Value d) {
    if (stmt instanceof JIdentityStmt) {
      JIdentityStmt idStmt = (JIdentityStmt) stmt;
      Value leftOp = idStmt.getLeftOp();
      if (d.equals(leftOp)) {
        return true;
      }
    }
    return false;
  }

  private boolean isAlloc(AbstractDefinitionStmt stmt, Value d) {
    Value rightOp = stmt.getRightOp();
    if (rightOp instanceof JNewExpr) {
      JNewExpr newExpr = (JNewExpr) rightOp;
      Type type = newExpr.getType();
      if (type.equals(d.getType())) {
        return true;
      }
    }
    return false;
  }

  private boolean isMethodAssignment(AbstractDefinitionStmt stmt, Value d) {
    Value rightOp = stmt.getRightOp();
    if (rightOp instanceof JSpecialInvokeExpr
        || rightOp instanceof JStaticInvokeExpr
        || rightOp instanceof JVirtualInvokeExpr
        || rightOp instanceof JInterfaceInvokeExpr
        || rightOp instanceof JDynamicInvokeExpr) {
      return true;
    }
    return false;
  }

  private Set<Value> getParams(Value invoke, Value d) {
    Set<Value> otherArgs = new HashSet<>();
    if (invoke instanceof AbstractInvokeExpr) {
      List<Value> args = ((AbstractInvokeExpr) invoke).getArgs();
      for (Value arg : args) {
        if (!arg.equals(d) && (arg.getType().equals(d.getType()))
            || arg.getType().equals(queryVarType)) {
          otherArgs.add(arg);
        }
      }
    }
    return otherArgs;
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
}
