package boomerang.scene.sparse.factspecific;

import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGBuilder;
import boomerang.scene.sparse.aliasaware.AliasAwareSparseCFGBuilder;
import com.google.common.graph.MutableGraph;
import java.util.*;
import java.util.logging.Logger;
import soot.*;
import soot.jimple.InvokeExpr;
import soot.jimple.Stmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.BriefUnitGraph;
import soot.toolkits.graph.DirectedGraph;
import soot.toolkits.graph.ExceptionalUnitGraph;

public class FactSpecificSparseCFGBuilder extends SparseCFGBuilder {
  private static final Logger LOGGER = Logger.getLogger(AliasAwareSparseCFGBuilder.class.getName());

  private boolean enableExceptions;

  public FactSpecificSparseCFGBuilder(boolean enableExceptions) {
    this.enableExceptions = enableExceptions;
  }

  private Unit initialQueryStmt;

  public SparseAliasingCFG buildSparseCFG(
      Val queryVar, SootMethod m, Unit queryStmt, Unit initialQueryStmt) {
    this.initialQueryStmt = initialQueryStmt;
    DirectedGraph<Unit> unitGraph =
        (this.enableExceptions
            ? new ExceptionalUnitGraph(m.getActiveBody())
            : new BriefUnitGraph(m.getActiveBody()));

    MutableGraph<Unit> mCFG = numberStmtsAndConvertToMutableGraph(unitGraph);
    // LOGGER.info(m.getName() + " original");
    // logCFG(LOGGER, mCFG);
    // if (m.getName().equals("id")) {
    Unit head = getHead(unitGraph);
    Set<Unit> stmtsToKeep = findStmtsToKeep(mCFG, head, SootAdapter.asValue(queryVar));
    stmtsToKeep.add(queryStmt);
    List<Unit> tails = unitGraph.getTails();
    for (Unit tail : tails) {
      sparsify(mCFG, stmtsToKeep, head, tail);
    }
    // LOGGER.info(m.getName() + " sparse");
    // logCFG(LOGGER, mCFG);
    // }
    return new SparseAliasingCFG(queryVar, mCFG, queryStmt, null);
  }

  private void sparsify(MutableGraph<Unit> mCFG, Set<Unit> stmtsToKeep, Unit head, Unit tail) {
    Iterator<Unit> iter = getBFSIterator(mCFG, head);
    Set<Unit> stmsToRemove = new HashSet<>();
    while (iter.hasNext()) {
      Unit unit = iter.next();
      if (!isControlStmt(unit)
          && !stmtsToKeep.contains(unit)
          && !unit.equals(head)
          && !unit.equals(tail)
          && !initialQueryStmt.equals(unit)) {
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

  private Set<Unit> findStmtsToKeep(MutableGraph<Unit> mCFG, Unit head, Value queryVar) {
    Set<Unit> stmtsToKeep = new HashSet<>();
    Iterator<Unit> iter = getBFSIterator(mCFG, head);
    while (iter.hasNext()) {
      Unit unit = iter.next();
      if (keepStmt(unit, queryVar)) {
        stmtsToKeep.add(unit);
      }
    }
    return stmtsToKeep;
  }

  private boolean keepStmt(Unit unit, Value queryVar) {
    if (unit instanceof Stmt) {
      Stmt stmt = (Stmt) unit;
      if (stmt.containsInvokeExpr()) {
        InvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Value> args = invokeExpr.getArgs();
        // v as arg
        for (Value arg : args) {
          if (arg.equals(queryVar)) {
            return true;
          }
        }
        // v as base v.m()
        if (isInvokeBase(queryVar, invokeExpr)) {
          return true;
        }
      }
    }
    if (unit instanceof JAssignStmt) {
      JAssignStmt stmt = (JAssignStmt) unit;
      Value leftOp = stmt.getLeftOp();
      Value rightOp = stmt.getRightOp();

      // handle Casts (Not mentioned)
      if (rightOp instanceof JCastExpr) {
        JCastExpr cast = (JCastExpr) rightOp;
        rightOp = cast.getOp();
      }

      if (rightOp.toString().startsWith("this$")) { // TODO: quickfix
        return true;
      }

      // left or right of the same type
      if (leftOp.equals(queryVar) || rightOp.equals(queryVar) || isFieldBase(leftOp, queryVar)) {
        return true;
      }
    }
    return false;
  }

  private boolean isInvokeBase(Value queryVar, InvokeExpr invokeExpr) {
    if (invokeExpr instanceof JVirtualInvokeExpr) {
      Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
      if (base.equals(queryVar)) {
        return true;
      }
    } else if (invokeExpr instanceof JSpecialInvokeExpr) {
      Value base = ((JSpecialInvokeExpr) invokeExpr).getBase();
      if (base.equals(queryVar)) {
        return true;
      }
    }
    return false;
  }

  private boolean isFieldBase(Value op, Value queryVar) {
    if (op instanceof JInstanceFieldRef) {
      JInstanceFieldRef ref = (JInstanceFieldRef) op;
      return ref.getBase().equals(queryVar);
    }
    return false;
  }
}
