package boomerang.scene.sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.aliasaware.AliasAwareSparseCFGCache;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import boomerang.scene.sparse.typebased.TypeBasedSparseCFGCache;
import java.util.List;
import soot.SootMethod;
import soot.jimple.Stmt;

public interface SparseCFGCache {

  enum SparsificationStrategy {
    TYPE_BASED,
    ALIAS_AWARE,
    NONE;
  }

  static SparseCFGCache getInstance(SparsificationStrategy strategy, boolean ignoreAfterQuery) {
    switch (strategy) {
      case TYPE_BASED:
        return TypeBasedSparseCFGCache.getInstance();
      case ALIAS_AWARE:
        return AliasAwareSparseCFGCache.getInstance(ignoreAfterQuery);
      default:
        throw new RuntimeException("SparsificationStrategy not implemented");
    }
  }

  /**
   * For retrieving the same {@link SparseAliasingCFG} built by the backward query
   *
   * @param m
   * @param stmt
   * @return
   */
  SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt, Val val);

  /**
   * For building the {@link SparseAliasingCFG} for the first time for a backward query.
   *
   * @param initialQueryVal
   * @param initialQueryStmt
   * @param currentMethod
   * @param currentVal
   * @param currentStmt
   * @return
   */
  SparseAliasingCFG getSparseCFGForBackwardPropagation(
      Val initialQueryVal,
      Statement initialQueryStmt,
      Method currentMethod,
      Val currentVal,
      Statement currentStmt);

  List<SparseCFGQueryLog> getQueryLogs();
}
