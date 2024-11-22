package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.QueryGraph;
import boomerang.guided.targets.*;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.jimple.IntAndStringBoomerangOptions;
import com.google.common.collect.Sets;
import java.io.Serializable;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import test.FrameworkScopeFactory;
import wpds.impl.Weight.NoWeight;

public class DemandDrivenGuidedAnalysisTest {

  private String classPathStr = Paths.get("target/test-classes").toAbsolutePath().toString();

  @Test
  public void integerCastTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, IntegerCastTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.IntegerCastTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);

    BackwardQuery query = selectFirstBaseOfToString(m);
    Specification spec =
        Specification.create("<java.lang.Integer: ON{B}java.lang.Integer valueOf(GO{B}int)>");
    runAnalysis(scopeFactory, spec, query, 1);
  }

  @Test
  public void basicTarget() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, BasicTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.BasicTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  @Ignore(
      "We need additional logic to tell the analysis to continue at some unknown parent context")
  public void leftUnbalancedTargetTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, LeftUnbalancedTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.LeftUnbalancedTarget: void bar(java.lang.String)>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void contextSensitiveTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, ContextSensitiveTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void nestedContextTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, NestedContextTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.NestedContextTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void nestedContextAndBranchingTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, NestedContextAndBranchingTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.NestedContextAndBranchingTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar", "foo");
  }

  @Test
  public void contextSensitiveAndLeftUnbalanced2StacksTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(
            classPathStr, ContextSensitiveAndLeftUnbalanced2StacksTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalanced2StacksTarget: void context()>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(
            classPathStr, ContextSensitiveAndLeftUnbalancedTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget: void context(java.lang.String)>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithFieldTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(
            classPathStr, ContextSensitiveAndLeftUnbalancedFieldTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedFieldTarget: void context(java.lang.String)>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithThisFieldTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(
            classPathStr, ContextSensitiveAndLeftUnbalancedThisFieldTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedThisFieldTarget$MyObject: void context()>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void contextSensitiveAndLeftUnbalancedWithFieldTest2() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(
            classPathStr, ContextSensitiveAndLeftUnbalancedTarget2.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ContextSensitiveAndLeftUnbalancedTarget2: void context()>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstBaseOfToString(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void wrappedInNewStringTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, WrappedInNewStringTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.WrappedInNewStringTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void wrappedInNewStringInnerTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, WrappedInNewStringInnerTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.WrappedInNewStringInnerTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void wrappedInNewStringTwiceTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, WrappedInStringTwiceTest.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.WrappedInStringTwiceTest: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar");
  }

  @Test
  public void branchingTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, BranchingTest.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.BranchingTest: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar", "foo");
  }

  @Test
  public void branchingAfterNewTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, BranchingAfterNewStringTest.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.BranchingAfterNewStringTest: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, query, "bar", "foo");
  }

  @Test
  public void pingPongTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, PingPongTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.PingPongTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, getPingPongSpecification(), query, "hello", "world");
  }

  @Test
  public void pingPongInterpoceduralTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, PingPongInterproceduralTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.PingPongInterproceduralTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstFileInitArgument(m);

    runAnalysis(scopeFactory, getPingPongSpecification(), query, "hello", "world");
  }

  @Test
  public void arrayContainerTest() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, ArrayContainerTarget.class.getName());
    String methodSignatureStr =
        "<boomerang.guided.targets.ArrayContainerTarget: void main(java.lang.String[])>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstBaseOfToString(m);

    runAnalysis(scopeFactory, new ArrayContainerCollectionManager(), query, "hello", "world");
  }

  @Test
  public void valueOfTarget() {
    FrameworkScope scopeFactory =
        FrameworkScopeFactory.init(classPathStr, ValueOfTarget.class.getName());
    String methodSignatureStr = "<boomerang.guided.targets.ValueOfTarget: void foo(int,int)>";
    Method m = scopeFactory.getMethod(methodSignatureStr);
    BackwardQuery query = selectFirstArgOfQueryTarget(m);

    runAnalysis(scopeFactory, query, 1);
  }

  public static BackwardQuery selectFirstArgOfQueryTarget(Method method) {
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

  public static BackwardQuery selectFirstFileInitArgument(Method method) {
    // TODO: [ms] this line seems to make no sense?
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

  public static BackwardQuery selectFirstBaseOfToString(Method method) {
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

  protected void runAnalysis(
      FrameworkScope scopeFactory, BackwardQuery query, Object... expectedValues) {
    Specification specification =
        Specification.create(
            "<java.lang.Integer: ON{B}java.lang.Integer valueOf(GO{B}int)>",
            "<ON{B}java.lang.Integer: java.lang.Integer <init>(GO{B}int)>",
            "<GO{F}java.lang.String: void <init>(ON{F}java.lang.String)>",
            "<ON{B}java.lang.String: void <init>(GO{B}java.lang.String)>",
            "<GO{B}java.lang.String: ON{B}byte[] getBytes()>");
    runAnalysis(scopeFactory, specification, query, expectedValues);
  }

  private boolean isStringOrIntAllocation(Statement stmt) {
    return stmt.isAssign()
        && (stmt.getRightOp().isIntConstant() || stmt.getRightOp().isStringConstant());
  }

  protected void runAnalysis(
      FrameworkScope scopeFactory,
      Specification specification,
      BackwardQuery query,
      Object... expectedValues) {
    runAnalysis(
        scopeFactory, new SimpleSpecificationGuidedManager(specification), query, expectedValues);
  }

  protected void runAnalysis(
      FrameworkScope scopeFactory,
      IDemandDrivenGuidedManager queryManager,
      BackwardQuery query,
      Object... expectedValues) {
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
            },
            scopeFactory.getDataFlowScope(),
            scopeFactory.buildCallGraph(),
            scopeFactory);

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

    Set<? extends Serializable> collect =
        res.map(t -> ((AllocVal) t.var()).getAllocVal())
            .filter(x -> x.isStringConstant() || x.isIntConstant())
            .map(x -> (x.isIntConstant() ? x.getIntValue() : x.getStringValue()))
            .collect(Collectors.toSet());

    Assert.assertEquals(Sets.newHashSet(expectedValues), collect);
  }
}
