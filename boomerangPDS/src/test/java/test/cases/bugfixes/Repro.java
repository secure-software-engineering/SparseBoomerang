package test.cases.bugfixes;

import boomerang.Boomerang;
import boomerang.ForwardQuery;
import boomerang.options.BoomerangOptions;
import boomerang.options.IntAndStringAllocationSite;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scope.AllocVal;
import boomerang.scope.ControlFlowGraph.Edge;
import boomerang.scope.DeclaredMethod;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import boomerang.scope.Statement;
import com.google.common.collect.Sets;
import java.util.*;
import org.junit.Test;
import test.AbstractTestingFramework;
import test.FrameworkScopeFactory;
import wpds.impl.NoWeight;

/**
 * This code was added to test https://github.com/CodeShield-Security/SPDS/issues/5.
 * Thanks @copumpkin for sharing code for testing purpose.
 */
public class Repro extends AbstractTestingFramework {

  @Test
  public void excludeFoo() {
    FrameworkScope scope = setup(Collections.singletonList("Foo"));
    assertResults("<Foo: void baz()>", "<Foo: void bar()>", "<Foo: void <init>()>");
  }

  @Test
  public void includeFoo() {
    FrameworkScope scope = setup(Collections.emptyList());
    assertResults(
        "<Foo: void baz()>",
        "<Foo: void bar()>",
        "<Foo: void <init>()>",
        "<java.lang.Object: void <init>()>");
  }

  @Override
  protected void initializeWithEntryPoint() {
    // empty
  }

  private FrameworkScope setup(List<String> excluded) {
    return FrameworkScopeFactory.init(
        "src/test/resources/Test.jar",
        getTestCaseClassName(),
        testMethodName.getMethodName(),
        Collections.emptyList(),
        excluded);
  }

  @Override
  public void analyze() {}

  private void assertResults(String... expectedCalledMethodsOnFoo) {
    Method method = frameworkScope.resolveMethod("<Test: java.util.List foos()>");
    System.out.println("All method units:");
    for (Statement s : method.getControlFlowGraph().getStatements()) {
      System.out.println("\t" + s.toString());
    }

    Statement newFoo =
        method.getControlFlowGraph().getStatements().stream()
            .filter(x -> x.toString().contains("$stack2 = new Foo"))
            .findFirst()
            .get();

    // This will only show results if set_exclude above gets uncommented
    System.out.println("\nFoo invoked methods:");
    Collection<Statement> statements =
        getMethodsInvokedFromInstanceInStatement(frameworkScope, newFoo);
    Set<String> methodCalledOnFoo = Sets.newHashSet();
    for (Statement s : statements) {
      System.out.println("\t" + s);
      DeclaredMethod calledMethod = s.getInvokeExpr().getMethod();
      System.out.println("\t\t" + calledMethod);
      methodCalledOnFoo.add(calledMethod.toString());
    }

    assert methodCalledOnFoo.equals(Sets.newHashSet(expectedCalledMethodsOnFoo));
  }

  private static Collection<Statement> getMethodsInvokedFromInstanceInStatement(
      FrameworkScope scopeFactory, Statement queryStatement) {
    AllocVal var =
        new AllocVal(queryStatement.getLeftOp(), queryStatement, queryStatement.getRightOp());
    ForwardQuery fwq =
        new ForwardQuery(
            new Edge(
                queryStatement,
                queryStatement.getMethod().getControlFlowGraph().getSuccsOf(queryStatement).stream()
                    .findFirst()
                    .get()),
            var);
    Boomerang solver =
        new Boomerang(
            scopeFactory, BoomerangOptions.WITH_ALLOCATION_SITE(new IntAndStringAllocationSite()));
    ForwardBoomerangResults<NoWeight> results = solver.solve(fwq);
    return results.getInvokeStatementsOnInstance();
  }
}
