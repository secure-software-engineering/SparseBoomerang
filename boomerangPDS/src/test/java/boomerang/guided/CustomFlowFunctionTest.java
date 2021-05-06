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
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.SootCallGraph;
import boomerang.solver.BackwardBoomerangSolver;
import com.google.common.collect.Lists;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import wpds.impl.Weight.NoWeight;

public class CustomFlowFunctionTest {

  public static String CG = "cha";

  @Test
  public void killOnSystemExitBackwardTestInteger() {
    setupSoot(CustomFlowFunctionTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.CustomFlowFunctionIntTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectQueryForStatement(m);

    SootCallGraph sootCallGraph = new SootCallGraph();
    Boomerang solver =
        new Boomerang(
            sootCallGraph,
            SootDataFlowScope.make(Scene.v()),
            new CustomIntAndStringBoomerangOptions());

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
    setupSoot(CustomFlowFunctionTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectQueryForStatement(m);

    SootCallGraph sootCallGraph = new SootCallGraph();
    Boomerang solver =
        new Boomerang(
            sootCallGraph, SootDataFlowScope.make(Scene.v()), new CustomBoomerangOptions());

    System.out.println("Solving query: " + query);
    BackwardBoomerangResults<NoWeight> backwardQueryResults = solver.solve(query);
    System.out.println(backwardQueryResults.getAllocationSites());

    // For the query no allocation site is found, as between queryFor and the allocation site there
    // exists a System.exit call.
    Assert.assertEquals(true, backwardQueryResults.isEmpty());
  }

  @Test
  public void killOnSystemExitForwardTest() {
    setupSoot(CustomFlowFunctionTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.CustomFlowFunctionTarget: void main(java.lang.String[])>");
    ForwardQuery query = selectFirstIntAssignment(m);

    SootCallGraph sootCallGraph = new SootCallGraph();
    Boomerang solver =
        new Boomerang(
            sootCallGraph, SootDataFlowScope.make(Scene.v()), new CustomBoomerangOptions());

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

  public static BackwardQuery selectQueryForStatement(SootMethod m) {
    Method method = JimpleMethod.of(m);
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

  public static ForwardQuery selectFirstIntAssignment(SootMethod m) {
    Method method = JimpleMethod.of(m);
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

  protected void setupSoot(Class cls) {
    G.v().reset();
    setupSoot();
    setApplicationClass(cls);
    PackManager.v().runPacks();
    BoomerangPretransformer.v().reset();
    BoomerangPretransformer.v().apply();
  }

  private void setupSoot() {
    Options.v().set_whole_program(true);
    Options.v().setPhaseOption("cg." + CG, "on");
    Options.v().setPhaseOption("cg." + CG, "verbose:true");
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_keep_line_number(true);
    Options.v().set_prepend_classpath(true);
    Options.v().set_process_dir(getProcessDir());
  }

  private void setApplicationClass(Class cls) {
    Scene.v().loadNecessaryClasses();
    List<SootMethod> eps = Lists.newArrayList();
    for (SootClass sootClass : Scene.v().getClasses()) {
      if (sootClass.toString().equals(cls.getName())
          || (sootClass.toString().contains(cls.getName() + "$"))) {
        sootClass.setApplicationClass();
        eps.addAll(sootClass.getMethods());
      }
    }
    Scene.v().setEntryPoints(eps);
  }

  private List<String> getProcessDir() {
    Path path = Paths.get("target/test-classes");
    return Lists.newArrayList(path.toAbsolutePath().toString());
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
