package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.DefaultBoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.flowfunction.IBackwardFlowFunction;
import boomerang.flowfunction.IForwardFlowFunction;
import boomerang.guided.flowfunction.CustomBackwardFlowFunction;
import boomerang.guided.flowfunction.CustomForwardFlowFunction;
import boomerang.guided.targets.CustomFlowFunctionTarget;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.solver.BackwardBoomerangSolver;
import java.nio.file.Paths;
import org.junit.Assert;
import org.junit.Test;
import test.FrameworkScopeFactory;
import wpds.impl.Weight.NoWeight;

public class CustomFlowFunctionTest {

  public static String CG = "cha";
  private String classPathStr = Paths.get("target/test-classes").toAbsolutePath().toString();

  @Test
  public void killOnSystemExitBackwardTestInteger() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s =
        "<boomerang.guided.targets.CustomFlowFunctionIntTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(s);
    BackwardQuery query = selectQueryForStatement(m);

    CallGraph CallGraph = scopeFactory.buildCallGraph();
    Boomerang solver =
        new Boomerang(
            scopeFactory.buildCallGraph(),
            scopeFactory.getDataFlowScope(),
            new CustomIntAndStringBoomerangOptions(),
            scopeFactory);

    System.out.println("Solving query: " + query);
    BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
    for (BackwardBoomerangSolver bw : solver.getBackwardSolvers().values()) {
      Assert.assertEquals(true, bw.getCallAutomaton().getTransitions().size() < 3);
    }
    System.out.println(backwardQueryResults.getAllocationSites());

    // For the query no allocation site is found, as between queryFor and the allocation site there
    // exists a System.exit call.
    Assert.assertEquals(true, backwardQueryResults.isEmpty());
  }

  @Test
  public void killOnSystemExitBackwardTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s = "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(s);
    BackwardQuery query = selectQueryForStatement(m);

    Boomerang solver =
        new Boomerang(
            scopeFactory.buildCallGraph(),
            scopeFactory.getDataFlowScope(),
            new CustomBoomerangOptions(),
            scopeFactory);

    System.out.println("Solving query: " + query);
    BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
    System.out.println(backwardQueryResults.getAllocationSites());

    // For the query no allocation site is found, as between queryFor and the allocation site there
    // exists a System.exit call.
    Assert.assertEquals(true, backwardQueryResults.isEmpty());
  }

  @Test
  public void killOnSystemExitForwardTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, CustomFlowFunctionTarget.class.getName());
    String s = "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(s);
    ForwardQuery query = selectFirstIntAssignment(m);

    Boomerang solver =
        new Boomerang(
            scopeFactory.buildCallGraph(),
            scopeFactory.getDataFlowScope(),
            new CustomBoomerangOptions(),
            scopeFactory);

    System.out.println("Solving query: " + query);
    ForwardBoomerangResults<NoWeight> res = solver.solve(query);
    System.out.println(res.asStatementValWeightTable());

    boolean t =
        res.asStatementValWeightTable().cellSet().stream()
            .map(c -> c.getRowKey().getTarget())
            .anyMatch(
                statement ->
                    statement.containsInvokeExpr()
                        && statement.getInvokeExpr().getMethod().getName().equals("queryFor"));
    Assert.assertEquals(false, t);
  }

  public static BackwardQuery selectQueryForStatement(Method method) {
    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement queryStatement =
        method.getStatements().stream()
            .filter(x -> x.containsInvokeExpr())
            .filter(x -> x.getInvokeExpr().getMethod().getName().equals("queryFor"))
            .findFirst()
            .get();
    Val arg = queryStatement.getInvokeExpr().getArg(0);

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(queryStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, queryStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  public static ForwardQuery selectFirstIntAssignment(Method method) {
    method.getStatements().stream().forEach(x -> System.out.println(x.toString()));
    Statement intAssignStmt =
        method.getStatements().stream()
            .filter(x -> x.isAssign() && !x.getLeftOp().getType().isRefType())
            .findFirst()
            .get();
    Val arg = new AllocVal(intAssignStmt.getLeftOp(), intAssignStmt, intAssignStmt.getRightOp());

    Statement succs =
        method.getControlFlowGraph().getSuccsOf(intAssignStmt).stream().findFirst().get();
    Edge cfgEdge = new Edge(intAssignStmt, succs);
    return new ForwardQuery(cfgEdge, arg);
  }

  private class CustomBoomerangOptions extends DefaultBoomerangOptions {

    @Override
    public IForwardFlowFunction getForwardFlowFunctions() {
      return new CustomForwardFlowFunction(this);
    }

    @Override
    public IBackwardFlowFunction getBackwardFlowFunction() {
      return new CustomBackwardFlowFunction(this);
    }
  }

  private class CustomIntAndStringBoomerangOptions extends IntAndStringBoomerangOptions {

    @Override
    public IForwardFlowFunction getForwardFlowFunctions() {
      return new CustomForwardFlowFunction(this);
    }

    @Override
    public IBackwardFlowFunction getBackwardFlowFunction() {
      return new CustomBackwardFlowFunction(this);
    }
  }
}
