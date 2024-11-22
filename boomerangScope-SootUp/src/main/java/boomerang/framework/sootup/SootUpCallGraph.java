package boomerang.framework.sootup;

import boomerang.scene.CallGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import java.util.Collection;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.analysis.interprocedural.icfg.CGEdgeUtil;
import sootup.analysis.interprocedural.icfg.CalleeMethodSignature;
import sootup.core.model.SootMethod;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class SootUpCallGraph extends CallGraph {

  private static final Logger LOGGER = LoggerFactory.getLogger(SootUpCallGraph.class);

  public SootUpCallGraph(
      sootup.callgraph.CallGraph callGraph, Collection<Method> entryPoints) {

    Collection<Pair<MethodSignature, CalleeMethodSignature>> edges =
        CGEdgeUtil.getCallEdges(SootUpFrameworkScope.getInstance().getView(), callGraph);

    for (Pair<MethodSignature, CalleeMethodSignature> edge : edges) {
      Optional<JavaSootMethod> sourceOpt = SootUpFrameworkScope.getInstance().getSootMethod(edge.getLeft());
      Optional<JavaSootMethod> targetOpt =
          SootUpFrameworkScope.getInstance().getSootMethod(edge.getRight().getMethodSignature());

      if(sourceOpt.isEmpty() || targetOpt.isEmpty()){
        continue;
      }

      JavaSootMethod sourceMethod = sourceOpt.get();
      JavaSootMethod targetMethod = targetOpt.get();
      if (!sourceMethod.hasBody() || !targetMethod.hasBody()) {
        continue;
      }

      Statement callSite =
          JimpleUpStatement.create(edge.getRight().getSourceStmt(), JimpleUpMethod.of(sourceMethod));
      this.addEdge(new Edge(callSite, JimpleUpMethod.of(targetMethod)));

      LOGGER.trace("Added edge {} -> {}", callSite, targetMethod);
    }

    for (Method em : entryPoints) {
      this.addEntryPoint(em);
      LOGGER.trace("Added entry point: {}", em);
    }

    if( getEdges().isEmpty() ) {
      throw new IllegalStateException("CallGraph is empty!");
    }
  }
}
