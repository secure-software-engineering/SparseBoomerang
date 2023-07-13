package boomerang.scene.sparse;

import boomerang.scene.Val;
import com.google.common.graph.MutableGraph;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.stmt.Stmt;

public class SparseAliasingCFG {

  private static Logger log = LoggerFactory.getLogger(SparseAliasingCFG.class);

  private MutableGraph<Stmt> graph;
  private Val d; // which dff this SCFG belongs to
  private Stmt queryStmt; // in contrast to sparseCFG queryStmt affects the graph
  private Set<Value> fallbackAliases;
  private Map<Stmt, Integer> unitToNumber;

  public SparseAliasingCFG(
      Val d,
      MutableGraph<Stmt> graph,
      Stmt queryStmt,
      Set<Value> fallbackAliases,
      Map<Stmt, Integer> unitToNumber) {
    this.d = d;
    this.queryStmt = queryStmt;
    this.graph = graph;
    this.fallbackAliases = fallbackAliases;
    this.unitToNumber = unitToNumber;
  }

  public Set<Value> getFallBackAliases() {
    return fallbackAliases;
  }

  public synchronized boolean addEdge(Stmt node, Stmt succ) {
    return graph.putEdge(node, succ);
  }

  public Set<Stmt> getSuccessors(Stmt node) {
    return graph.successors(node);
  }

  public List<Stmt> getNextUses(Stmt node) {
    Set<Stmt> successors = getSuccessors(node);
    return new ArrayList<>(successors);
  }

  public MutableGraph<Stmt> getGraph() {
    return this.graph;
  }
}
