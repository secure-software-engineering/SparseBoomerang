package boomerang.sparse;

import boomerang.scene.Statement;
import boomerang.scene.Val;
import com.google.common.graph.MutableGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SparseAliasingCFG<U extends Statement, V extends Val> {

  private static Logger log = LoggerFactory.getLogger(SparseAliasingCFG.class);

  private MutableGraph<U> graph;
  private Val d; // which dff this SCFG belongs to
  private U queryStmt; // in contrast to sparseCFG queryStmt affects the graph
  private Set<V> fallbackAliases;
  private Map<U, Integer> unitToNumber;

  public SparseAliasingCFG(
      Val d,
      MutableGraph<U> graph,
      U queryStmt,
      Set<V> fallbackAliases,
      Map<U, Integer> unitToNumber) {
    this.d = d;
    this.queryStmt = queryStmt;
    this.graph = graph;
    this.fallbackAliases = fallbackAliases;
    this.unitToNumber = unitToNumber;
  }

  public Set<V> getFallBackAliases() {
    return fallbackAliases;
  }

  public synchronized boolean addEdge(U node, U succ) {
    return graph.putEdge(node, succ);
  }

  public Set<U> getSuccessors(U node) {
    return graph.successors(node);
  }

  public List<U> getNextUses(U node) {
    Set<U> successors = getSuccessors(node);
    return new ArrayList<>(successors);
  }

  public MutableGraph<U> getGraph() {
    return this.graph;
  }
}
