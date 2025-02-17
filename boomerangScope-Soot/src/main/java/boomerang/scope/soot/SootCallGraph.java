package boomerang.scope.soot;

import boomerang.scope.soot.jimple.JimpleMethod;
import boomerang.scope.soot.jimple.JimpleStatement;
import boomerang.scope.CallGraph;
import boomerang.scope.Statement;

public class SootCallGraph extends CallGraph {

  public SootCallGraph(soot.jimple.toolkits.callgraph.CallGraph callGraph) {
    for (soot.jimple.toolkits.callgraph.Edge e : callGraph) {
      if (e.src().hasActiveBody() && e.tgt().hasActiveBody() && e.srcStmt() != null) {
        Statement callSite = JimpleStatement.create(e.srcStmt(), JimpleMethod.of(e.src()));
        if (callSite.containsInvokeExpr()) {
          LOGGER.trace("Call edge from {} to target method {}", callSite, e.tgt());
          this.addEdge(new Edge(callSite, JimpleMethod.of(e.tgt())));
        }
      }
    }

    // TODO
    // for (SootMethod m : Scene.v().getEntryPoints()) {
    //  if (m.hasActiveBody()) this.addEntryPoint(JimpleMethod.of(m));
    // }

    // if (getEdges().isEmpty()) {
    //  throw new IllegalStateException("CallGraph is empty!");
    // }
  }
}
