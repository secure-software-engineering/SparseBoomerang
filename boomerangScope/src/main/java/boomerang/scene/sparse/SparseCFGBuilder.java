package boomerang.scene.sparse;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import com.google.common.graph.Traverser;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.*;

public class SparseCFGBuilder {

  private static final Logger LOGGER = Logger.getLogger(SparseCFGBuilder.class.getName());

  protected Map<Stmt, Integer> unitToNumber = new HashMap<>();

  protected MutableGraph<Stmt> numberStmtsAndConvertToMutableGraph(StmtGraph<?> rawGraph) {
    MutableGraph<Stmt> mGraph = GraphBuilder.directed().build();
    List<Stmt> heads = new ArrayList<>(rawGraph.getEntrypoints());
    for (Stmt head : heads) {
      convertToMutableGraph(rawGraph, head, mGraph, 0);
    }
    return mGraph;
  }

  protected void convertToMutableGraph(
      StmtGraph<?> graph, Stmt curr, MutableGraph<Stmt> mutableGraph, int depth) {
    Integer num = unitToNumber.get(curr);
    if (num == null || num < depth) {
      unitToNumber.put(curr, depth);
    }
    depth++;
    List<Stmt> succsOf = graph.successors(curr);
    for (Stmt succ : succsOf) {
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
  protected Stmt getHead(StmtGraph<?> graph) {
    List<Stmt> heads = new ArrayList<>(graph.getEntrypoints());
    List<Stmt> res = new ArrayList<>();
    for (Stmt head : heads) {
      if (head instanceof JIdentityStmt || graph.successors(head).isEmpty()) {
        continue;
      }
      res.add(head);
    }
    if (res.size() > 1) {
      LOGGER.warning("Multiple heads!");
    }
    return res.get(0);
  }

  protected Iterator<Stmt> getBFSIterator(MutableGraph<Stmt> graph, Stmt head) {
    Traverser<Stmt> traverser = Traverser.forGraph(graph);
    return traverser.breadthFirst(head).iterator();
  }

  protected void logCFG(Logger logger, MutableGraph<Stmt> graph) {
    logger.info(
        graph.nodes().stream()
            .map(Objects::toString)
            .collect(Collectors.joining(System.lineSeparator())));
  }

  protected boolean isControlStmt(Stmt stmt) {
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
