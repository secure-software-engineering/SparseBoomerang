package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.QueryGraph;
import boomerang.guided.targets.ArrayContainerTarget;
import boomerang.guided.targets.BasicTarget;
import boomerang.guided.targets.BranchingAfterNewStringTest;
import boomerang.guided.targets.BranchingTest;
import boomerang.guided.targets.ContextSensitiveAndLeftUnbalanced2StacksTarget;
import boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedFieldTarget;
import boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget;
import boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget2;
import boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedThisFieldTarget;
import boomerang.guided.targets.ContextSensitiveTarget;
import boomerang.guided.targets.IntegerCastTarget;
import boomerang.guided.targets.LeftUnbalancedTarget;
import boomerang.guided.targets.NestedContextAndBranchingTarget;
import boomerang.guided.targets.NestedContextTarget;
import boomerang.guided.targets.PingPongInterproceduralTarget;
import boomerang.guided.targets.PingPongTarget;
import boomerang.guided.targets.ValueOfTarget;
import boomerang.guided.targets.WrappedInNewStringInnerTarget;
import boomerang.guided.targets.WrappedInNewStringTarget;
import boomerang.guided.targets.WrappedInStringTwiceTest;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.BoomerangPretransformer;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import boomerang.scene.jimple.JimpleMethod;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import soot.G;
import soot.PackManager;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.options.Options;
import wpds.impl.Weight.NoWeight;

public class DemandDrivenGuidedAnalysisTest {

  public static String CG = "cha";

  @Test
  public void integerCastTest() {
    setupSoot(IntegerCastTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.IntegerCastTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstBaseOfToString(m);
    Specification spec =
        Specification.create("<java.lang.Integer: ON{B}java.lang.Integer valueOf(GO{B}int)>");
    runAnalysis(spec, query, 1);
  }

  @Test
  public void basicTarget() {
    setupSoot(BasicTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod("<boomerang.guided.targets.BasicTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  @Ignore(
      "We need additional logic to tell the analysis to continue at some unknown parent context")
  public void leftUnbalancedTargetTest() {
    setupSoot(LeftUnbalancedTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.LeftUnbalancedTarget: void bar(java.lang.String)>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveTest() {
    setupSoot(ContextSensitiveTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void nestedContextTest() {
    setupSoot(NestedContextTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.NestedContextTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void nestedContextAndBranchingTest() {
    setupSoot(NestedContextAndBranchingTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.NestedContextAndBranchingTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  @Test
  public void contextSensitiveAndLeftUnbalanced2StacksTest() {
    setupSoot(ContextSensitiveAndLeftUnbalanced2StacksTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalanced2StacksTarget: void context()>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedTest() {
    setupSoot(ContextSensitiveAndLeftUnbalancedTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget: void context(java.lang.String)>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithFieldTest() {
    setupSoot(ContextSensitiveAndLeftUnbalancedFieldTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedFieldTarget: void context(java.lang.String)>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithThisFieldTest() {
    setupSoot(ContextSensitiveAndLeftUnbalancedThisFieldTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedThisFieldTarget$MyObject: void context()>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithFieldTest2() {
    setupSoot(ContextSensitiveAndLeftUnbalancedTarget2.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget2: void context()>");
    BackwardQuery query = selectFirstBaseOfToString(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void wrappedInNewStringTest() {
    setupSoot(WrappedInNewStringTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.WrappedInNewStringTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void wrappedInNewStringInnerTest() {
    setupSoot(WrappedInNewStringInnerTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.WrappedInNewStringInnerTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void wrappedInNewStringTwiceTest() {
    setupSoot(WrappedInStringTwiceTest.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.WrappedInStringTwiceTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar");
  }

  @Test
  public void branchingTest() {
    setupSoot(BranchingTest.class);
    SootMethod m =
        Scene.v()
            .getMethod("<boomerang.guided.targets.BranchingTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  @Test
  public void branchingAfterNewTest() {
    setupSoot(BranchingAfterNewStringTest.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.BranchingAfterNewStringTest: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(query, "bar", "foo");
  }

  @Test
  public void pingPongTest() {
    setupSoot(PingPongTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod("<boomerang.guided.targets.PingPongTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(getPingPongSpecification(), query, "hello", "world");
  }

  @Test
  public void pingPongInterpoceduralTest() {
    setupSoot(PingPongInterproceduralTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.PingPongInterproceduralTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(getPingPongSpecification(), query, "hello", "world");
  }

  @Test
  public void arrayContainerTest() {
    setupSoot(ArrayContainerTarget.class);
    SootMethod m =
        Scene.v()
            .getMethod(
                "<boomerang.guided.targets.ArrayContainerTarget: void main(java.lang.String[])>");
    BackwardQuery query = selectFirstBaseOfToString(m);

    runAnalysis(new ArrayContainerCollectionManager(), query, "hello", "world");
  }

  @Test
  public void valueOfTarget() {
    setupSoot(ValueOfTarget.class);
    SootMethod m =
        Scene.v().getMethod("<boomerang.guided.targets.ValueOfTarget: void foo(int,int)>");
    BackwardQuery query = selectFirstArgOfQueryTarget(m);

    runAnalysis(query, 1);
  }

  public static BackwardQuery selectFirstArgOfQueryTarget(SootMethod m) {
    Method method = JimpleMethod.of(m);
    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement =
        method.getStatements().stream()
            .filter(x -> x.containsInvokeExpr())
            .filter(
                x ->
                    x.getInvokeExpr().getMethod().getName().equals("queryFor")
                        && x.getInvokeExpr()
                            .getMethod()
                            .getDeclaringClass()
                            .getFullyQualifiedName()
                            .equals("boomerang.guided.targets.Query"))
            .findFirst()
            .get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  private Specification getPingPongSpecification() {
    return Specification.create(
        "<ON{B}java.lang.StringBuilder: java.lang.StringBuilder append(GO{B}java.lang.String)>",
        "<ON{F}java.lang.StringBuilder: java.lang.StringBuilder append(GO{B}java.lang.String)>",
        "<ON{F}java.lang.StringBuilder: GO{F}java.lang.StringBuilder append(java.lang.String)>",
        "<GO{B}java.lang.StringBuilder: ON{B}java.lang.String toString()>");
  }

  public static BackwardQuery selectFirstFileInitArgument(SootMethod m) {
    Method method = JimpleMethod.of(m);
    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement =
        method.getStatements().stream()
            .filter(x -> x.containsInvokeExpr())
            .filter(
                x ->
                    x.getInvokeExpr().getMethod().getName().equals("<init>")
                        && x.getInvokeExpr()
                            .getMethod()
                            .getDeclaringClass()
                            .getFullyQualifiedName()
                            .equals("java.io.File"))
            .findFirst()
            .get();
    Val arg = newFileStatement.getInvokeExpr().getArg(0);

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  public static BackwardQuery selectFirstBaseOfToString(SootMethod m) {
    Method method = JimpleMethod.of(m);
    method.getStatements().stream().filter(x -> x.containsInvokeExpr()).forEach(x -> x.toString());
    Statement newFileStatement =
        method.getStatements().stream()
            .filter(x -> x.containsInvokeExpr())
            .filter(x -> x.getInvokeExpr().getMethod().getName().equals("toString"))
            .findFirst()
            .get();
    Val arg = newFileStatement.getInvokeExpr().getBase();

    Statement predecessor =
        method.getControlFlowGraph().getPredsOf(newFileStatement).stream().findFirst().get();
    Edge cfgEdge = new Edge(predecessor, newFileStatement);
    return BackwardQuery.make(cfgEdge, arg);
  }

  protected void runAnalysis(BackwardQuery query, Object... expectedValues) {
    Specification specification =
        Specification.create(
            "<java.lang.Integer: ON{B}java.lang.Integer valueOf(GO{B}int)>",
            "<ON{B}java.lang.Integer: java.lang.Integer <init>(GO{B}int)>",
            "<GO{F}java.lang.String: void <init>(ON{F}java.lang.String)>",
            "<ON{B}java.lang.String: void <init>(GO{B}java.lang.String)>",
            "<GO{B}java.lang.String: ON{B}byte[] getBytes()>");
    runAnalysis(specification, query, expectedValues);
  }

  private boolean isStringOrIntAllocation(Statement stmt) {
    return stmt.isAssign()
        && (stmt.getRightOp().isIntConstant() || stmt.getRightOp().isStringConstant());
  }

  protected void runAnalysis(
      Specification specification, BackwardQuery query, Object... expectedValues) {
    runAnalysis(new SimpleSpecificationGuidedManager(specification), query, expectedValues);
  }

  protected void runAnalysis(
      IDemandDrivenGuidedManager queryManager, BackwardQuery query, Object... expectedValues) {
    DemandDrivenGuidedAnalysis demandDrivenGuidedAnalysis =
        new DemandDrivenGuidedAnalysis(
            queryManager,
            new IntAndStringBoomerangOptions() {
              @Override
              public Optional<AllocVal> getAllocationVal(Method m, Statement stmt, Val fact) {
                if (stmt.isAssign()
                    && stmt.getLeftOp().equals(fact)
                    && isStringOrIntAllocation(stmt)) {
                  return Optional.of(new AllocVal(stmt.getLeftOp(), stmt, stmt.getRightOp()));
                }
                return super.getAllocationVal(m, stmt, fact);
              }

              @Override
              public int analysisTimeoutMS() {
                return 5000;
              }

              @Override
              public boolean allowMultipleQueries() {
                return true;
              }
            });

    QueryGraph<NoWeight> queryGraph = demandDrivenGuidedAnalysis.run(query);
    demandDrivenGuidedAnalysis.cleanUp();
    // Filter out query graph's node to only return the queries of interest (ForwardQueries &
    // String/Int Allocation sites).
    Stream<Query> res =
        queryGraph.getNodes().stream()
            .filter(
                x ->
                    x instanceof ForwardQuery
                        && isStringOrIntAllocation(x.asNode().stmt().getStart()));
    Assert.assertEquals(
        Sets.newHashSet(expectedValues),
        res.map(t -> ((AllocVal) t.var()).getAllocVal())
            .filter(x -> x.isStringConstant() || x.isIntConstant())
            .map(x -> (x.isIntConstant() ? x.getIntValue() : x.getStringValue()))
            .collect(Collectors.toSet()));
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
}
