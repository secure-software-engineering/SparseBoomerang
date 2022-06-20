package boomerang.controlflowgraph;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.PropagationCounter;
import java.util.Set;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class StaticCFG implements ObservableControlFlowGraph {

  private SparseCFGCache.SparsificationStrategy sparsificationStrategy;

  public void setSparse(SparseCFGCache.SparsificationStrategy sparsificationStrategy) {
    this.sparsificationStrategy = sparsificationStrategy;
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
    if (sparsificationStrategy != SparseCFGCache.SparsificationStrategy.NONE) {
      SparseAliasingCFG sparseCFG = getSparseCFG(method, curr);
      if (sparseCFG != null) {
        propagateSparse(l, method, curr, sparseCFG);
      } else {
        propagateDefault(l); // TODO: parameterize this only for FlowDroid
      }
    } else {
      propagateDefault(l);
    }
    sparsificationStrategy = SparseCFGCache.SparsificationStrategy.NONE;
  }

  private void propagateSparse(
      SuccessorListener l, Method method, Statement curr, SparseAliasingCFG sparseCFG) {
    Set<Unit> successors = sparseCFG.getGraph().successors(SootAdapter.asStmt(curr));
    for (Unit succ : successors) {
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(SootAdapter.asStatement(succ, method));
    }
  }

  private void propagateDefault(SuccessorListener l) {
    for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getSuccsOf(l.getCurr())) {
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(s);
    }
  }

  private SparseAliasingCFG getSparseCFG(Method method, Statement stmt) {
    SootMethod sootMethod = ((JimpleMethod) method).getDelegate();
    Stmt sootStmt = ((JimpleStatement) stmt).getDelegate();
    SparseCFGCache sparseCFGCache = SparseCFGCache.getInstance(sparsificationStrategy);
    SparseAliasingCFG sparseCFG =
        sparseCFGCache.getSparseCFGForForwardPropagation(sootMethod, sootStmt);
    return sparseCFG;
  }

  @Override
  public void step(Statement curr, Statement succ) {}

  @Override
  public void unregisterAllListeners() {}
}
