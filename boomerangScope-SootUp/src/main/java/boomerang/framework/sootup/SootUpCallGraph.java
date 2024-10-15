package boomerang.framework.sootup;

import boomerang.scene.CallGraph;
import boomerang.scene.Statement;
import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.analysis.interprocedural.icfg.CGEdgeUtil;
import sootup.analysis.interprocedural.icfg.CalleeMethodSignature;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootMethod;

public class SootUpCallGraph extends CallGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(SootUpCallGraph.class);

  public SootUpCallGraph(
      sootup.callgraph.CallGraph callGraph, Collection<MethodSignature> entryPoints) {
    Collection<Pair<MethodSignature, CalleeMethodSignature>> edges =
        CGEdgeUtil.getCallEdges(SootUpClient.getInstance().getView(), callGraph);

    for (Pair<MethodSignature, CalleeMethodSignature> edge : edges) {
      JavaSootMethod source = SootUpClient.getInstance().getSootMethod(edge.getLeft());
      JavaSootMethod target =
          SootUpClient.getInstance().getSootMethod(edge.getRight().getMethodSignature());

      if (!source.hasBody() || !target.hasBody()) {
        continue;
      }

      Statement callSite =
          JimpleUpStatement.create(edge.getRight().getSourceStmt(), JimpleUpMethod.of(source));
      this.addEdge(new Edge(callSite, JimpleUpMethod.of(target)));

      LOGGER.trace("Added edge {} -> {}", callSite, target);
    }

    for (MethodSignature signature : entryPoints) {
      JavaSootMethod entryPoint = SootUpClient.getInstance().getSootMethod(signature);
      this.addEntryPoint(JimpleUpMethod.of(entryPoint));

      LOGGER.trace("Added entry point from method {}", entryPoint);
    }
  }
}
