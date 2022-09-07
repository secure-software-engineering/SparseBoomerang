package boomerang.controlflowgraph;

import boomerang.BoomerangOptions;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.PropagationCounter;
import java.util.*;
import soot.SootMethod;
import soot.Unit;
import soot.jimple.Stmt;

public class StaticCFG implements ObservableControlFlowGraph {

  private SparseCFGCache.SparsificationStrategy sparsificationStrategy;

  private BoomerangOptions options;

  private Val currentVal;

  public void setCurrentVal(Val val) {
    this.currentVal = val;
  }

  public StaticCFG(BoomerangOptions options) {
    this.options = options;
    this.sparsificationStrategy = options.getSparsificationStrategy();
  }

  @Override
  public void addPredsOfListener(PredecessorListener l) {
    for (Statement s : l.getCurr().getMethod().getControlFlowGraph().getPredsOf(l.getCurr())) {
      l.getPredecessor(s);
    }
  }

  @Override
  public void addSuccsOfListener(SuccessorListener l) {
    Method method = l.getCurr().getMethod();
    Statement curr = l.getCurr();
    if (sparsificationStrategy != SparseCFGCache.SparsificationStrategy.NONE) {
        SparseAliasingCFG sparseCFG = getSparseCFG(method, curr, currentVal);
        if (sparseCFG != null) {
          propagateSparse(l, method, curr, sparseCFG);
        } else if (options.handleSpecialInvokeAsNormalPropagation()) {
          propagateDefault(l);
        }
    } else {
      propagateDefault(l);
    }
  }


  private void propagetWithSucc(
      SuccessorListener l,
      List<Statement> nextSucc,
      Method method,
      ControlFlowGraph.Edge edge,
      Val value) {
    SparseAliasingCFG sparseCFG = getSparseCFG(method, edge.getTarget(), value);
    if (nextSucc.isEmpty()) {
      if (sparseCFG.getGraph().nodes().contains(SootAdapter.asStmt(edge.getTarget()))) {
        propagateSparse(l, method, edge.getTarget(), sparseCFG);
      } else {
        propagateSparse(l, method, edge.getStart(), sparseCFG);
      }
      return;
    } else if (nextSucc.size() == 1) {
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(nextSucc.get(0));
    } else if (nextSucc.size() == 2) {
      JimpleStatement u1 = (JimpleStatement) nextSucc.get(0);
      JimpleStatement u2 = (JimpleStatement) nextSucc.get(1);
      Unit earlier = sparseCFG.getEarlier(u1.getDelegate(), u2.getDelegate());
      PropagationCounter.getInstance(sparsificationStrategy).countForward();
      l.getSuccessor(SootAdapter.asStatement(earlier, method));
    } else {
      throw new RuntimeException("handle this");
    }
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

  private SparseAliasingCFG getSparseCFG(Method method, Statement stmt, Val currentVal) {
    SootMethod sootMethod = ((JimpleMethod) method).getDelegate();
    Stmt sootStmt = ((JimpleStatement) stmt).getDelegate();
    SparseCFGCache sparseCFGCache = SparseCFGCache.getInstance(sparsificationStrategy);
    SparseAliasingCFG sparseCFG =
        sparseCFGCache.getSparseCFGForForwardPropagation(sootMethod, sootStmt, currentVal);
    return sparseCFG;
  }

  @Override
  public void step(Statement curr, Statement succ) {}

  @Override
  public void unregisterAllListeners() {}
}
