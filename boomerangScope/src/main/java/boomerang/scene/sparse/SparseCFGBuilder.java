package boomerang.scene.sparse;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import soot.jimple.IdentityStmt;
import soot.jimple.internal.*;
import soot.toolkits.graph.DirectedGraph;
import sootup.core.jimple.common.stmt.Stmt;

public class SparseCFGBuilder {

  private static final Logger LOGGER = Logger.getLogger(SparseCFGBuilder.class.getName());

  protected Map<Stmt, Integer> unitToNumber = new HashMap<>();

  protected MutableGraph<Stmt> numberStmtsAndConvertToMutableGraph(DirectedGraph<Stmt> rawGraph) {
    MutableGraph<Stmt> mGraph = GraphBuilder.directed().build();
    List<Stmt> heads = rawGraph.getHeads();
    for (Stmt head : heads) {
      convertToMutableGraph(rawGraph, head, mGraph, 0);
    }
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

  /**
   * Boomerang uses BriefUnitGraph and chooses nonidentitiy stmt as the head
   *
   * @param graph
   * @return
   */
  protected Unit getHead(DirectedGraph<Unit> graph) {
    List<Unit> heads = graph.getHeads();
    List<Unit> res = new ArrayList<>();
    for (Unit head : heads) {
      if (head instanceof IdentityStmt || graph.getSuccsOf(head).isEmpty()) {
        continue;
      }
      res.add(head);
    }
    if (res.size() > 1) {
      LOGGER.warning("Multiple heads!");
    }
    return res.get(0);
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
