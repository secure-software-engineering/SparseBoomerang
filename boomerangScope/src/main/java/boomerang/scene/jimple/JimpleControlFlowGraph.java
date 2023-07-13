package boomerang.scene.jimple;

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Statement;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import java.util.Collection;
import java.util.List;
import sootup.core.graph.StmtGraph;
import sootup.core.jimple.common.stmt.JIdentityStmt;
import sootup.core.jimple.common.stmt.Stmt;

public class JimpleControlFlowGraph implements ControlFlowGraph {

  protected StmtGraph<?> graph;

  protected boolean cacheBuild = false;
  protected List<Statement> startPointCache = Lists.newArrayList();
  protected List<Statement> endPointCache = Lists.newArrayList();
  protected Multimap<Statement, Statement> succsOfCache = HashMultimap.create();
  protected Multimap<Statement, Statement> predsOfCache = HashMultimap.create();
  protected List<Statement> statements = Lists.newArrayList();

  protected JimpleMethod method;

  public JimpleControlFlowGraph() {}

  public JimpleControlFlowGraph(JimpleMethod method) {
    this.method = method;
    this.graph = method.getDelegate().getBody().getStmtGraph();
  }

  public Collection<Statement> getStartPoints() {
    buildCache();
    return startPointCache;
  }

  protected void buildCache() {
    if (cacheBuild) return;
    cacheBuild = true;
    Collection<Stmt> heads = graph.getEntrypoints();
    for (Stmt u : heads) {
      // We add a nop statement to the body and ignore IdentityStmt ($stack14 := @caughtexception)
      if (u instanceof JIdentityStmt) {
        continue;
      }
      Statement stmt = JimpleStatement.create(u, method);
      startPointCache.add(stmt);
    }
    List<Stmt> tails = graph.getTails();
    for (Stmt u : tails) {
      Statement stmt = JimpleStatement.create(u, method);
      endPointCache.add(stmt);
    }

    List<Stmt> units = method.getDelegate().getBody().getStmts();
    for (Stmt u : units) {
      Statement first = JimpleStatement.create(u, method);
      statements.add(first);

      for (Stmt succ : graph.successors(u)) {
        Statement succStmt = JimpleStatement.create(succ, method);
        succsOfCache.put(first, succStmt);
      }

      for (Stmt pred : graph.predecessors(u)) {
        Statement predStmt = JimpleStatement.create(pred, method);
        predsOfCache.put(first, predStmt);
      }
    }
  }

  public Collection<Statement> getEndPoints() {
    buildCache();
    return endPointCache;
  }

  public Collection<Statement> getSuccsOf(Statement curr) {
    buildCache();
    return succsOfCache.get(curr);
  }

  public Collection<Statement> getPredsOf(Statement curr) {
    buildCache();
    return predsOfCache.get(curr);
  }

  public List<Statement> getStatements() {
    buildCache();
    return statements;
  }
}
