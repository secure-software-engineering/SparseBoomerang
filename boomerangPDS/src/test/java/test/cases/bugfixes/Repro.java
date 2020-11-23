package test.cases.bugfixes;

import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.jimple.*;
import com.google.common.collect.Sets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import org.junit.Test;
import soot.*;
import soot.options.Options;
import wpds.impl.Weight.NoWeight;

/**
 * This code was added to test https://github.com/CodeShield-Security/SPDS/issues/5.
 * Thanks @copumpkin for sharing code for testing purpose.
 */
public class Repro {
  static BoomerangOptions opts = new IntAndStringBoomerangOptions();

  @Test
  public void excludeFoo() {
    G.reset();
    setupSoot("src/test/resources/Test.jar", true);
    analyze("<Foo: void baz()>", "<Foo: void bar()>", "<Foo: void <init>()>");
  }

  @Test
  public void includeFoo() {
    G.reset();
    setupSoot("src/test/resources/Test.jar", false);
    analyze(
        "<Foo: void baz()>",
        "<Foo: void bar()>",
        "<Foo: void <init>()>",
        "<java.lang.Object: void <init>()>");
  }

  private static void setupSoot(String classPath, boolean excludeFoo) {
    Options.v().set_whole_program(true);
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_keep_line_number(true);

    /* ********* Uncomment this line to see methods invoked on Foo! ********* */
    if (excludeFoo) {
      Options.v().set_exclude(Collections.singletonList("Foo"));
    }

    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_soot_classpath(classPath);
    Options.v().set_prepend_classpath(true);
    Options.v().set_process_dir(Arrays.asList(classPath.split(":")));
    Scene.v().loadNecessaryClasses();
  }

  private static void analyze(String... expectedCallSignatureOnFoo) {
    PackManager.v()
        .getPack("wjtp")
        .add(new Transform("wjtp.repro", new ReproTransformer(expectedCallSignatureOnFoo)));
    PackManager.v().getPack("cg").apply();
    PackManager.v().getPack("wjtp").apply();
  }

  private static Map<Edge, DeclaredMethod> getMethodsInvokedFromInstanceInStatement(
      Statement queryStatement) {
    Val var = new AllocVal(queryStatement.getLeftOp(), queryStatement, queryStatement.getRightOp());
    ForwardQuery fwq =
        new ForwardQuery(
            new Edge(
                queryStatement,
                queryStatement.getMethod().getControlFlowGraph().getSuccsOf(queryStatement).stream()
                    .findFirst()
                    .get()),
            var);
    Boomerang solver = new Boomerang(new SootCallGraph(), SootDataFlowScope.make(Scene.v()), opts);
    ForwardBoomerangResults<NoWeight> results = solver.solve(fwq);
    return results.getInvokedMethodOnInstance();
  }

  static class ReproTransformer extends SceneTransformer {

    Set<String> expectedCalledMethodsOnFoo;

    public ReproTransformer(String... expectedCallSignatureOnFoo) {
      expectedCalledMethodsOnFoo = Sets.newHashSet(expectedCallSignatureOnFoo);
    }

    @Override
    protected void internalTransform(String name, Map<String, String> options) {
      BoomerangPretransformer.v().reset();
      BoomerangPretransformer.v().apply();

      SootMethod m = Scene.v().getMethod("<Test: java.util.List foos()>");
      Method method = JimpleMethod.of(m);

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
      Set<Entry<Edge, DeclaredMethod>> entries =
          getMethodsInvokedFromInstanceInStatement(newFoo).entrySet();
      Set<String> methodCalledOnFoo = Sets.newHashSet();
      for (Map.Entry<Edge, DeclaredMethod> e : entries) {
        System.out.println("\t" + e.getKey().toString());
        System.out.println("\t\t" + e.getValue().toString());
        methodCalledOnFoo.add(e.getValue().toString());
      }

      assert methodCalledOnFoo.equals(Sets.newHashSet(expectedCalledMethodsOnFoo));
    }
  }
}
