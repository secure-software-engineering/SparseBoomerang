package boomerang.scene.sparse;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import soot.Unit;
import soot.jimple.internal.*;
import soot.toolkits.graph.DirectedGraph;

public class SparseCFGBuilder {

  protected Map<Unit, Integer> unitToNumber = new HashMap<>();

  protected MutableGraph<Unit> numberStmtsAndConvertToMutableGraph(DirectedGraph<Unit> rawGraph) {
    MutableGraph<Unit> mGraph = GraphBuilder.directed().build();
    Unit head = getHead(rawGraph);
    convertToMutableGraph(rawGraph, head, mGraph, 0);
    return mGraph;
  }

  protected void convertToMutableGraph(
      DirectedGraph<Unit> graph, Unit curr, MutableGraph<Unit> mutableGraph, int depth) {
    Integer num = unitToNumber.get(curr);
    if (num == null || num < depth) {
      unitToNumber.put(curr, depth);
    }
    depth++;
    List<Unit> succsOf = graph.getSuccsOf(curr);
    for (Unit succ : succsOf) {
      if (!mutableGraph.hasEdgeConnecting(curr, succ) && !curr.equals(succ)) {
        mutableGraph.putEdge(curr, succ);
        convertToMutableGraph(graph, succ, mutableGraph, depth);
      }
    }
  }

  protected Unit getHead(DirectedGraph<Unit> graph) {
    List<Unit> heads = graph.getHeads();
    if (heads.size() > 1) {
      throw new RuntimeException("Multiple Heads");
    }
    return heads.get(0);
  }

  protected Iterator<Unit> getBFSIterator(MutableGraph<Unit> graph, Unit head) {
    Traverser<Unit> traverser = Traverser.forGraph(graph);
    return traverser.breadthFirst(head).iterator();
  }

  protected void logCFG(Logger logger, MutableGraph<Unit> graph) {
    logger.info(
        graph.nodes().stream()
            .map(Objects::toString)
            .collect(Collectors.joining(System.lineSeparator())));
  }

  protected boolean isControlStmt(Unit stmt) {
    if (stmt instanceof JIfStmt
        || stmt instanceof JNopStmt
        || stmt instanceof JGotoStmt
        || stmt instanceof JReturnStmt
        || stmt instanceof JReturnVoidStmt) {
      return true;
    }
    if (stmt instanceof JIdentityStmt) {
      //      JIdentityStmt id = (JIdentityStmt) stmt;
      //      if (id.getRightOp() instanceof JCaughtExceptionRef) {
      return true;
      //      }
    }
    return false;
  }
}
