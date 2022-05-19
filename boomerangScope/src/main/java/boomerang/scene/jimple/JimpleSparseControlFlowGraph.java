package boomerang.scene.jimple;

import boomerang.scene.Statement;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseAliasingCFGCache;
import boomerang.scene.sparse.SparseAliasingUnitGraph;
import java.util.Iterator;
import java.util.List;
import soot.Body;
import soot.Unit;
import soot.jimple.IdentityStmt;
import soot.jimple.Stmt;

public class JimpleSparseControlFlowGraph extends JimpleControlFlowGraph {

  public JimpleSparseControlFlowGraph(JimpleMethod method, JimpleStatement stmt, JimpleVal value) {
    this.method = method;
    this.graph = buildSparseCFG(method.getDelegate().getActiveBody(), stmt, value);
  }

  private SparseAliasingUnitGraph buildSparseCFG(Body body, JimpleStatement stmt, JimpleVal value) {
    SparseAliasingCFG sparseCFG =
        SparseAliasingCFGCache.getInstance()
            .getSparseCFG(body.getMethod(), value.getDelegate(), stmt.getDelegate());
    return new SparseAliasingUnitGraph(sparseCFG);
  }

  @Override
  protected void buildCache() {
    if (cacheBuild) return;
    cacheBuild = true;
    List<Unit> heads = graph.getHeads();
    for (Unit u : heads) {
      // We add a nop statement to the body and ignore IdentityStmt ($stack14 := @caughtexception)
      if (u instanceof IdentityStmt) {
        continue;
      }
      Statement stmt = JimpleStatement.create((Stmt) u, method);
      startPointCache.add(stmt);
    }
    List<Unit> tails = graph.getTails();
    for (Unit u : tails) {
      Statement stmt = JimpleStatement.create((Stmt) u, method);
      endPointCache.add(stmt);
    }

    for (Iterator<Unit> it = graph.iterator(); it.hasNext(); ) {
      Unit u = it.next();
      Statement first = JimpleStatement.create((Stmt) u, method);
      statements.add(first);

      for (Unit succ : graph.getSuccsOf(u)) {
        Statement succStmt = JimpleStatement.create((Stmt) succ, method);
        succsOfCache.put(first, succStmt);
      }

      for (Unit pred : graph.getPredsOf(u)) {
        Statement predStmt = JimpleStatement.create((Stmt) pred, method);
        predsOfCache.put(first, predStmt);
      }
    }
  }
}
