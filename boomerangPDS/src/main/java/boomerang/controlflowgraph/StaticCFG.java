package boomerang.controlflowgraph;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.aliasaware.SparseAliasingCFG;
import boomerang.scene.sparse.typebased.TypeBasedSparseCFGCache;
import java.util.Set;
import soot.Unit;

public class StaticCFG implements ObservableControlFlowGraph {

  private boolean sparse;

  public void setSparse(boolean sparse) {
    this.sparse = sparse;
  }

  @Override
  public void addPredsOfListener(PredecessorListener l) {
    for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getPredsOf(l.getCurr())) {
      l.getPredecessor(s);
    }
  }

  // Stmt stmt = asStmt(curr.getStart());
  //      Set<Unit> predecessors = sparseCFG.getGraph().predecessors(stmt);
  //      for (Unit pred : predecessors) {
  //        Collection<State> flow =
  //            computeNormalFlow(method, new Edge(asStatement(pred, method), curr.getStart()),
  // value);
  @Override
  public void addSuccsOfListener(SuccessorListener l) {
    Method method = l.getCurr().getMethod();
    Statement curr = l.getCurr();
    if (sparse) {
      SparseAliasingCFG sparseCFG = getSparseCFG(method, curr);
      if (sparseCFG == null) {
        sparse = false;
        return;
      }
      Set<Unit> successors = sparseCFG.getGraph().successors(SootAdapter.asStmt(curr));
      for (Unit succ : successors) {
        l.getSuccessor(SootAdapter.asStatement(succ, method));
      }
    } else {
      for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getSuccsOf(l.getCurr())) {
        l.getSuccessor(s);
      }
    }
    sparse = false;
  }

  private SparseAliasingCFG getSparseCFG(Method method, Statement stmt) {
    JimpleMethod jMethod = (JimpleMethod) method;
    JimpleStatement jStmt = (JimpleStatement) stmt;
    SparseAliasingCFG sparseCFG =
        TypeBasedSparseCFGCache.getInstance()
            .getSparseCFG(jMethod.getDelegate(), jStmt.getDelegate());
    return sparseCFG;
  }

  @Override
  public void step(Statement curr, Statement succ) {}

  @Override
  public void unregisterAllListeners() {}
}
