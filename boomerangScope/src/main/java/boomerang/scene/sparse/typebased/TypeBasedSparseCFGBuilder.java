package boomerang.scene.sparse.typebased;

import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGBuilder;
import boomerang.scene.sparse.aliasaware.AliasAwareSparseCFGBuilder;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import com.google.common.graph.MutableGraph;
import java.util.*;
import java.util.logging.Logger;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.basic.Immediate;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.model.SootMethod;
import sootup.core.types.Type;
import sootup.java.core.views.JavaView;

public class TypeBasedSparseCFGBuilder extends SparseCFGBuilder {
  private static final Logger LOGGER = Logger.getLogger(AliasAwareSparseCFGBuilder.class.getName());

  private boolean enableExceptions;
  private Deque<Type> typeWorklist;
  private Set<Type> containerTypes;
  private JavaView view; // TODO: wire

  public TypeBasedSparseCFGBuilder(boolean enableExceptions) {
    this.enableExceptions = enableExceptions;
  }

  public SparseAliasingCFG buildSparseCFG(
      Val queryVar, SootMethod m, Stmt queryStmt, SparseCFGQueryLog queryLog) {
    typeWorklist = new ArrayDeque<>();
    containerTypes = new HashSet<>();
    StmtGraph<?> unitGraph = m.getBody().getStmtGraph();

    Stmt head = getHead(unitGraph);

    MutableGraph<Stmt> mCFG = numberStmtsAndConvertToMutableGraph(unitGraph);

    int initialStmtCount = mCFG.nodes().size();

    //    LOGGER.info(m.getName() + " original");
    //    logCFG(LOGGER, mCFG);
    // if (m.getName().equals("id")) {
    Type typeOfQueryVar = SootAdapter.getTypeOfVal(queryVar);

    Set<Stmt> stmtsToKeep = findStmtsToKeep(mCFG, head, typeOfQueryVar);

    containerTypes.add(typeOfQueryVar);
    while (!typeWorklist.isEmpty()) {
      Type containerType = typeWorklist.pop();
      stmtsToKeep.addAll(findStmtsToKeep(mCFG, head, containerType));
    }
    stmtsToKeep.add(queryStmt);
    List<Stmt> tails = unitGraph.getTails();
    for (Stmt tail : tails) {
      sparsify(mCFG, stmtsToKeep, head, tail);
    }

    //    LOGGER.info(m.getName() + " ta-sparse");
    //    logCFG(LOGGER, mCFG);
    // }
    int finalStmtCount = mCFG.nodes().size();
    queryLog.setInitialStmtCount(initialStmtCount);
    queryLog.setFinalStmtCount(finalStmtCount);

    return new SparseAliasingCFG(queryVar, mCFG, queryStmt, null, unitToNumber);
  }

  private void sparsify(MutableGraph<Stmt> mCFG, Set<Stmt> stmtsToKeep, Stmt head, Stmt tail) {
    Iterator<Stmt> iter = getBFSIterator(mCFG, head);
    Set<Stmt> stmsToRemove = new HashSet<>();
    while (iter.hasNext()) {
      Stmt unit = iter.next();
      if (!isControlStmt(unit)
          && !stmtsToKeep.contains(unit)
          && !unit.equals(head)
          && !unit.equals(tail)) {
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

  private Set<Stmt> findStmtsToKeep(MutableGraph<Stmt> mCFG, Stmt head, Type queryVarType) {
    Set<Stmt> stmtsToKeep = new HashSet<>();
    Iterator<Stmt> iter = getBFSIterator(mCFG, head);
    while (iter.hasNext()) {
      Stmt unit = iter.next();
      if (keepStmt(unit, queryVarType)) {
        stmtsToKeep.add(unit);
      }
    }
    return stmtsToKeep;
  }

  private boolean isAssignableType(Type targetType, Type sourceType) {
    return view.getTypeHierarchy().isSubtype(targetType, sourceType)
        || view.getTypeHierarchy().isSubtype(sourceType, targetType);
  }

  private boolean keepStmt(Stmt unit, Type queryVarType) {
    boolean keep =
        false; // Incase of Container.f = base.m(f_type), keep both base and Container as container
    // types
    if (unit instanceof Stmt) {
      Stmt stmt = (Stmt) unit;
      if (stmt.containsInvokeExpr()) {
        AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
        List<Immediate> args = invokeExpr.getArgs();
        // v as arg
        for (Value arg : args) {
          if (isFieldTypeRelevant(queryVarType, arg)) {
            handleInvokeBase(invokeExpr, queryVarType);
            keep = true;
          }
          if (isAssignableType(arg.getType(), queryVarType)) {
            handleInvokeBase(invokeExpr, queryVarType);
            keep = true;
          }
        }
        // v as base v.m()
        if (isInvokeBase(queryVarType, invokeExpr)) {
          keep = true;
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
        keep = true;
      }

      // left's field is of the same type
      if (isFieldTypeRelevant(queryVarType, leftOp) || isFieldTypeRelevant(queryVarType, rightOp)) {
        keep = true;
      }

      // left or right of the same type
      if (isAssignableType(leftOp.getType(), queryVarType)
          || isAssignableType(rightOp.getType(), queryVarType)) {
        if (stmt.containsInvokeExpr()) {
          AbstractInvokeExpr invokeExpr = stmt.getInvokeExpr();
          // always check invoke base if its beign assigned to the type we track, because we need to
          // know how it was allocated
          if (invokeExpr instanceof AbstractInstanceInvokeExpr) {
            handleInvokeBase(invokeExpr, queryVarType);
          }
        }
        keep = true;
      }
    }
    return keep;
  }

  /**
   * To keep base of an invoke in the cfg
   *
   * @param invokeExpr
   * @param queryVarType
   */
  private void handleInvokeBase(AbstractInvokeExpr invokeExpr, Type queryVarType) {
    if (invokeExpr instanceof JVirtualInvokeExpr) {
      Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
      handleContainerType(base, queryVarType);
    } else if (invokeExpr instanceof JSpecialInvokeExpr) {
      Value base = ((JSpecialInvokeExpr) invokeExpr).getBase();
      handleContainerType(base, queryVarType);
    }
  }

  private void handleContainerType(Value base, Type queryVarType) {
    if (!base.getType().equals(queryVarType)
        && !containerTypes.contains(base.getType())) { // we already track this type
      typeWorklist.push(base.getType()); // we need to find how base is allocated
      containerTypes.add(base.getType()); // we will track all the bases where queryType is a field
    }
  }

  /**
   * value is an instance field. The field is the same type as queryVarType. Keep the base type of
   * that field.
   *
   * @param queryVarType
   * @param value
   * @return
   */
  private boolean isFieldTypeRelevant(Type queryVarType, Value value) {
    // left's field is of the same type
    if (value instanceof JInstanceFieldRef) { // TODO: rightOp case?
      JInstanceFieldRef fieldRef = (JInstanceFieldRef) value;
      if (isAssignableType(fieldRef.getType(), queryVarType)) {
        Value base = fieldRef.getBase();
        handleContainerType(base, queryVarType);
        return true;
      }
    }
    return false;
  }

  private boolean isInvokeBase(Type queryVarType, AbstractInvokeExpr invokeExpr) {
    if (invokeExpr instanceof JVirtualInvokeExpr) {
      Value base = ((JVirtualInvokeExpr) invokeExpr).getBase();
      if (isAssignableType(base.getType(), queryVarType)) {
        return true;
      }
    } else if (invokeExpr instanceof JSpecialInvokeExpr) {
      Value base = ((JSpecialInvokeExpr) invokeExpr).getBase();
      if (isAssignableType(base.getType(), queryVarType)) {
        return true;
      }
    }
    return false;
  }
}
