package boomerang.controlflowgraph;

import boomerang.BoomerangOptions;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.aliasaware.SparseAliasingCFG;
import boomerang.scene.sparse.aliasaware.SparseAliasingCFGCache;
import boomerang.scene.sparse.typebased.TypeBasedSparseCFGCache;

import java.util.Set;

import soot.Unit;

public class StaticCFG implements ObservableControlFlowGraph {

    private BoomerangOptions.SparsificationStrategy sparsificationStrategy;

    public void setSparse(BoomerangOptions.SparsificationStrategy sparsificationStrategy) {
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
        if (sparsificationStrategy != BoomerangOptions.SparsificationStrategy.NONE) {
            SparseAliasingCFG sparseCFG = getSparseCFG(method, curr);
            if (sparseCFG == null) {
                sparsificationStrategy = BoomerangOptions.SparsificationStrategy.NONE;
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
        sparsificationStrategy = BoomerangOptions.SparsificationStrategy.NONE;
    }

    private SparseAliasingCFG getSparseCFG(Method method, Statement stmt) {
        SparseAliasingCFG sparseCFG;
        JimpleMethod jMethod = (JimpleMethod) method;
        JimpleStatement jStmt = (JimpleStatement) stmt;
        if (sparsificationStrategy == BoomerangOptions.SparsificationStrategy.TYPE_BASED) {
            sparseCFG =
                    TypeBasedSparseCFGCache.getInstance()
                            .getSparseCFG(jMethod.getDelegate(), jStmt.getDelegate());
        } else if (sparsificationStrategy == BoomerangOptions.SparsificationStrategy.ALIAS_AWARE) {
            sparseCFG =
                    SparseAliasingCFGCache.getInstance()
                            .getSparseCFG(jMethod.getDelegate(), jStmt.getDelegate());
        } else {
            throw new RuntimeException("Sparsification strategy not implemented");
        }
        return sparseCFG;
    }

    @Override
    public void step(Statement curr, Statement succ) {
    }

    @Override
    public void unregisterAllListeners() {
    }
}
