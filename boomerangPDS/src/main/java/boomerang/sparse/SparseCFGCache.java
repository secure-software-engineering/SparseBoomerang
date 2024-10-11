package boomerang.sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.sparse.eval.SparseCFGQueryLog;
import java.util.List;

public interface SparseCFGCache<M extends Method, S extends Statement> {

  static SparseCFGCache getInstance(SparsificationStrategy strategy, boolean ignoreAfterQuery) {

    // FIXME: [ms] fix/refactor mapping
    /*
    switch (strategy) {
      case TYPE_BASED:
        return TypeBasedSparseCFGCache.getInstance();
      case ALIAS_AWARE:
        return AliasAwareSparseCFGCache.getInstance(ignoreAfterQuery);
    }
    */
    throw new RuntimeException("SparsificationStrategy not implemented");
  }

  /**
   * For retrieving the same {@link SparseAliasingCFG} built by the backward query
   *
   * @param m
   * @param stmt
   * @return
   */
  SparseAliasingCFG getSparseCFGForForwardPropagation(M m, S stmt, Val val);

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
      Val initialQueryVal, S initialQueryStmt, M currentMethod, Val currentVal, S currentStmt);

  List<SparseCFGQueryLog> getQueryLogs();
}
