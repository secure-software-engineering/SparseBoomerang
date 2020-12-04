package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.guided.Specification.Parameter;
import boomerang.guided.Specification.QueryDirection;
import boomerang.guided.Specification.QuerySelector;
import boomerang.guided.Specification.SootMethodWithSelector;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleStatement;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import soot.jimple.Stmt;

public class SimpleSpecificationGuidedManager implements IDemandDrivenGuidedManager {

  private final Specification spec;

  public SimpleSpecificationGuidedManager(Specification spec) {
    this.spec = spec;
  }

  @Override
  public Collection<Query> onForwardFlow(ForwardQuery query, Edge dataFlowEdge, Val dataFlowVal) {
    Statement stmt = dataFlowEdge.getStart();
    Set<Query> res = Sets.newHashSet();
    if (stmt.containsInvokeExpr()) {
      Set<SootMethodWithSelector> selectors =
          spec.getMethodAndQueries().stream()
              .filter(x -> isInOnList(x, stmt, dataFlowVal, QueryDirection.FORWARD))
              .collect(Collectors.toSet());
      for (SootMethodWithSelector sel : selectors) {
        res.addAll(createNewQueries(sel, stmt));
      }
    }
    return res;
  }

  @Override
  public Collection<Query> onBackwardFlow(BackwardQuery query, Edge dataFlowEdge, Val dataFlowVal) {
    Statement stmt = dataFlowEdge.getStart();
    Set<Query> res = Sets.newHashSet();
    if (stmt.containsInvokeExpr()) {
      Set<SootMethodWithSelector> selectors =
          spec.getMethodAndQueries().stream()
              .filter(x -> isInOnList(x, stmt, dataFlowVal, QueryDirection.BACKWARD))
              .collect(Collectors.toSet());
      for (SootMethodWithSelector sel : selectors) {
        res.addAll(createNewQueries(sel, stmt));
      }
    }
    return res;
  }

  private Collection<Query> createNewQueries(SootMethodWithSelector sel, Statement stmt) {
    Set<Query> results = Sets.newHashSet();
    Method method = stmt.getMethod();
    for (QuerySelector qSel : sel.getGo()) {
      Optional<Val> parameterVal = getParameterVal(stmt, qSel.argumentSelection);
      if (parameterVal.isPresent()) {
        if (qSel.direction == QueryDirection.BACKWARD) {
          for (Statement pred : method.getControlFlowGraph().getPredsOf(stmt)) {
            results.add(BackwardQuery.make(new Edge(pred, stmt), parameterVal.get()));
          }
        } else if (qSel.direction == QueryDirection.FORWARD) {
          for (Statement succ : method.getControlFlowGraph().getSuccsOf(stmt)) {
            results.add(
                new ForwardQuery(
                    new Edge(stmt, succ),
                    new AllocVal(parameterVal.get(), stmt, parameterVal.get())));
          }
        }
      }
    }
    return results;
  }

  public boolean isInOnList(
      SootMethodWithSelector methodSelector, Statement stmt, Val fact, QueryDirection direction) {
    if (stmt instanceof JimpleStatement) {
      // This only works for Soot propagations
      Stmt jimpleStmt = ((JimpleStatement) stmt).getDelegate();
      if (jimpleStmt
          .getInvokeExpr()
          .getMethod()
          .getSignature()
          .equals(methodSelector.getSootMethod())) {
        Collection<QuerySelector> on = methodSelector.getOn();
        return isInList(on, direction, stmt, fact);
      }
    }
    return false;
  }

  private boolean isInList(
      Collection<QuerySelector> list, QueryDirection direction, Statement stmt, Val fact) {
    return list.stream()
        .anyMatch(
            sel -> (sel.direction == direction && isParameter(stmt, fact, sel.argumentSelection)));
  }

  private boolean isParameter(Statement stmt, Val fact, Parameter argumentSelection) {
    if (stmt.getInvokeExpr().isInstanceInvokeExpr() && argumentSelection.equals(Parameter.base())) {
      return stmt.getInvokeExpr().getBase().equals(fact);
    }
    if (argumentSelection.equals(Parameter.returnParam())) {
      return stmt.isAssign() && stmt.getLeftOp().equals(fact);
    }
    return stmt.getInvokeExpr().getArgs().size() > argumentSelection.getValue()
        && argumentSelection.getValue() >= 0
        && stmt.getInvokeExpr().getArg(argumentSelection.getValue()).equals(fact);
  }

  private Optional<Val> getParameterVal(Statement stmt, Parameter selector) {
    if (stmt.containsInvokeExpr()
        && !stmt.getInvokeExpr().isStaticInvokeExpr()
        && selector.equals(Parameter.base())) {
      return Optional.of(stmt.getInvokeExpr().getBase());
    }
    if (stmt.isAssign() && selector.equals(Parameter.returnParam())) {
      return Optional.of(stmt.getLeftOp());
    }
    if (stmt.getInvokeExpr().getArgs().size() > selector.getValue() && selector.getValue() >= 0) {
      return Optional.of(stmt.getInvokeExpr().getArg(selector.getValue()));
    }
    return Optional.empty();
  }
}
