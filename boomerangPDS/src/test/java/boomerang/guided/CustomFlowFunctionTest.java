package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.flowfunction.DefaultBackwardFlowFunctionOptions;
import boomerang.flowfunction.DefaultForwardFlowFunctionOptions;
import boomerang.guided.flowfunction.CustomBackwardFlowFunction;
import boomerang.guided.flowfunction.CustomForwardFlowFunction;
import boomerang.guided.targets.CustomFlowFunctionTarget;
import boomerang.options.BoomerangOptions;
import boomerang.options.IntAndStringAllocationSite;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scope.AllocVal;
import boomerang.scope.ControlFlowGraph.Edge;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import boomerang.scope.Statement;
import boomerang.scope.Val;
import boomerang.solver.BackwardBoomerangSolver;
import com.google.common.collect.Table;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;
import test.FrameworkScopeFactory;
import wpds.impl.Weight.NoWeight;

public class CustomFlowFunctionTest {

  public static String CG = "cha";
  private final String classPathStr = Paths.get("target/test-classes").toAbsolutePath().toString();

  @Test
  public void killOnSystemExitBackwardTestInteger() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s =
        "<boomerang.guided.targets.CustomFlowFunctionIntTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.resolveMethod(s);
    BackwardQuery query = selectQueryForStatement(m);

    Boomerang solver = new Boomerang(scopeFactory, customOptions());

    System.out.println("Solving query: " + query);
    BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
    for (BackwardBoomerangSolver bw : solver.getBackwardSolvers().values()) {
      Assert.assertTrue(bw.getCallAutomaton().getTransitions().size() < 3);
    }
    System.out.println(backwardQueryResults.getAllocationSites());

    // For the query no allocation site is found, as between queryFor and the allocation site there
    // exists a System.exit call.
    Assert.assertTrue(backwardQueryResults.isEmpty());
  }

  /*
  soot uses 1829 classes here:
  methodname: <init>
  methodname: exit
  methodname: queryFor
  Solving query: BackwardQuery: (z (boomerang.guided.targets.CustomFlowFunctionTarget.<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>),exit(y) -> queryFor(z))
  {}
  * */
  @Test
  public void killOnSystemExitBackwardTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s = "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.resolveMethod(s);
    BackwardQuery query = selectQueryForStatement(m);

    Boomerang solver = new Boomerang(scopeFactory, customOptions());

    System.out.println("Solving query: " + query);
    BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
    System.out.println(backwardQueryResults.getAllocationSites());

    // For the query no allocation site is found, as between queryFor and the allocation site there
    // exists a System.exit call.
    Assert.assertTrue(backwardQueryResults.isEmpty());
  }

  @Test
  public void killOnSystemExitForwardTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s = "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.resolveMethod(s);
    ForwardQuery query = selectFirstIntAssignment(m);

    Boomerang solver = new Boomerang(scopeFactory, customOptions());

    System.out.println("Solving query: " + query);
    ForwardBoomerangResults<NoWeight> res = solver.solve(query);
    System.out.println(res.asEdgeValWeightTable());

    boolean t =
        res.asStatementValWeightTable().cellSet().stream()
            .map(Table.Cell::getRowKey)
            .anyMatch(
                statement ->
                    statement.containsInvokeExpr()
                        && statement.getInvokeExpr().getMethod().getName().equals("queryFor"));
    Assert.assertFalse(t);
  }

  public static BackwardQuery selectQueryForStatement(Method method) {
    Statement queryStatement =
        method.getStatements().stream()
            .filter(Statement::containsInvokeExpr)
            .filter(
                x -> {
                  System.out.println("methodname: " + x.getInvokeExpr().getMethod().getName());
                  return x.getInvokeExpr().getMethod().getName().equals("queryFor");
                })
            .findFirst()
            .get();
    Val arg = queryStatement.getInvokeExpr().getArg(0);

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(queryStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, queryStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  public static ForwardQuery selectFirstIntAssignment(Method method) {
    method.getStatements().forEach(x -> System.out.println(x.toString()));
    Statement intAssignStmt =
        method.getStatements().stream()
            .filter(x -> x.isAssignStmt() && !x.getLeftOp().getType().isRefType())
            .findFirst()
            .get();
    AllocVal arg =
        new AllocVal(intAssignStmt.getLeftOp(), intAssignStmt, intAssignStmt.getRightOp());

    Statement succs =
        method.getControlFlowGraph().getSuccsOf(intAssignStmt).stream().findFirst().get();
    Edge cfgEdge = new Edge(intAssignStmt, succs);
    return new ForwardQuery(cfgEdge, arg);
  }

  private static BoomerangOptions customOptions() {
    return BoomerangOptions.builder()
        .withAllocationSite(new IntAndStringAllocationSite())
        .withForwardFlowFunction(
            new CustomForwardFlowFunction(DefaultForwardFlowFunctionOptions.DEFAULT()))
        .withBackwardFlowFunction(
            new CustomBackwardFlowFunction(DefaultBackwardFlowFunctionOptions.DEFAULT()))
        .build();
  }
}
