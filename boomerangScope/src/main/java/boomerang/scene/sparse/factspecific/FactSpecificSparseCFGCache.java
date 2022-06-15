package boomerang.scene.sparse.factspecific;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import soot.SootMethod;
import soot.jimple.Stmt;

public class FactSpecificSparseCFGCache implements SparseCFGCache {

    @Override
    public SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt) {
        return null;
    }

    @Override
    public SparseAliasingCFG getSparseCFGForBackwardPropagation(Val initialQueryVal, Statement initialQueryStmt, Method currentMethod, Val currentVal, Statement currentStmt) {
        return null;
    }
}
