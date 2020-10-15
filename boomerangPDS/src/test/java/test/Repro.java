package test;

import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.jimple.*;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Set;
import soot.*;
import soot.jimple.AssignStmt;
import soot.options.Options;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import wpds.impl.Weight.NoWeight;
import wpds.interfaces.State;

public class Repro {
  static BoomerangOptions opts = new IntAndStringBoomerangOptions();

  private static void setupSoot(String classPath) {
    Options.v().set_whole_program(true);
    Options.v().setPhaseOption("cg.spark", "on");
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    Options.v().set_keep_line_number(true);

    /* ********* Uncomment this line to see methods invoked on Foo! ********* */
    // Options.v().set_exclude(Collections.singletonList("Foo"));

    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_soot_classpath(classPath);
    Options.v().set_prepend_classpath(true);
    Options.v().set_process_dir(Arrays.asList(classPath.split(":")));
    Scene.v().loadNecessaryClasses();
  }

  private static void analyze() {
    PackManager.v().getPack("wjtp").add(new Transform("wjtp.repro", new ReproTransformer()));
    PackManager.v().getPack("cg").apply();
    PackManager.v().getPack("wjtp").apply();
  }

  public static void main(String[] args) {
    setupSoot("Test.jar");
    analyze();
  }

  private static Map<Edge, DeclaredMethod> getMethodsInvokedFromInstanceInStatement(Statement queryStatement) {
    Val var = new AllocVal(queryStatement.getLeftOp(), queryStatement, queryStatement.getRightOp());
    ForwardQuery fwq = new ForwardQuery(new Edge(queryStatement, queryStatement.getMethod().getControlFlowGraph().getSuccsOf(queryStatement).stream().findFirst().get()),var);
    Boomerang solver = new Boomerang(new SootCallGraph(), SootDataFlowScope.make(Scene.v()), opts);
    ForwardBoomerangResults<NoWeight> results = solver.solve(fwq);
    Table<ControlFlowGraph.Edge, Val, NoWeight> statementValNoWeightTable = results.asStatementValWeightTable();
    for(Cell<ControlFlowGraph.Edge, Val, NoWeight> c :
    statementValNoWeightTable.cellSet()){
      //  System.out.println(c.getRowKey() +" " + c.getColumnKey());
      ControlFlowGraph.Edge edge = c.getRowKey();
      Statement s = edge.getStart();
      if(s.containsInvokeExpr()){
        if(s.getInvokeExpr().isInstanceInvokeExpr() && s.getInvokeExpr().getBase().equals(c.getColumnKey())){
           System.out.println(s + "  " + c.getColumnKey());
        }
      }
    }

    return results.getInvokedMethodOnInstance();
  }

  static class ReproTransformer extends SceneTransformer {
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

      Statement newFoo = method.getControlFlowGraph().getStatements().stream().filter(
          x -> x.toString().contains("$stack2 = new Foo")).findFirst().get();
      Statement newList = method.getControlFlowGraph().getStatements().stream().filter(
          x -> x.toString().contains("$stack4 = new LinkedList")).findFirst().get();

      // This will only show results if set_exclude above gets uncommented
      System.out.println("\nFoo invoked methods:");
      for (Map.Entry<Edge, DeclaredMethod> e : getMethodsInvokedFromInstanceInStatement(newFoo).entrySet()) {
        System.out.println("\t" + e.getKey().toString());
        System.out.println("\t\t" + e.getValue().toString());
      }

      // This will show results regardless of the exclusion
      System.out.println("\nList invoked methods:");
      for (Map.Entry<Edge, DeclaredMethod> e : getMethodsInvokedFromInstanceInStatement(newList).entrySet()) {
        System.out.println("\t" + e.getKey().toString());
        System.out.println("\t\t" + e.getValue().toString());
      }
    }
  }
}