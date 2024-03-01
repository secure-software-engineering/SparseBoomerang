package boomerang.scene.jimple;

import boomerang.scene.CallGraph;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.analysis.interprocedural.icfg.CGEdgeUtil;
import sootup.analysis.interprocedural.icfg.CalleeMethodSignature;
import sootup.core.signatures.MethodSignature;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class SootCallGraph extends CallGraph {
  Logger LOGGER = LoggerFactory.getLogger(SootCallGraph.class);

  public SootCallGraph(
      JavaView view, sootup.callgraph.CallGraph cg, List<MethodSignature> entryPoints) {
    Set<Pair<MethodSignature, CalleeMethodSignature>> callEdges = CGEdgeUtil.getCallEdges(view, cg);
    for (Pair<MethodSignature, CalleeMethodSignature> callEdge : callEdges) {
      Optional<JavaSootMethod> methodOpt = view.getMethod(callEdge.getRight().getMethodSignature());
      if (methodOpt.isPresent() && methodOpt.get().hasBody()) {
        JimpleMethod targetMethod = JimpleMethod.of(methodOpt.get());
        JimpleMethod sourceMethod = JimpleMethod.of(view.getMethod(callEdge.getLeft()).get());
        this.addEdge(
            new Edge(
                JimpleStatement.create(callEdge.getRight().getSourceStmt(), sourceMethod),
                targetMethod));
      }
    }

    for (MethodSignature m : entryPoints) {
      this.addEntryPoint(JimpleMethod.of(view.getMethod(m).get()));
    }
  }
}
