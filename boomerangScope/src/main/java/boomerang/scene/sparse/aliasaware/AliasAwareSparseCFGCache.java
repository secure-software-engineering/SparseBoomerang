package boomerang.scene.sparse.aliasaware;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.scene.sparse.eval.SparseCFGQueryLog;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import soot.SootMethod;
import soot.jimple.Stmt;

public class AliasAwareSparseCFGCache implements SparseCFGCache {

  List<SparseCFGQueryLog> logList = new ArrayList<>();

  Map<String, SparseAliasingCFG> cache;
  AliasAwareSparseCFGBuilder sparseCFGBuilder;

  private static AliasAwareSparseCFGCache INSTANCE;
  private static boolean ignore;

  private AliasAwareSparseCFGCache() {}

  public static AliasAwareSparseCFGCache getInstance(boolean ignoreAfterQuery) {
    if (INSTANCE == null || ignore != ignoreAfterQuery) {
      ignore = ignoreAfterQuery;
      INSTANCE =
          new AliasAwareSparseCFGCache(new AliasAwareSparseCFGBuilder(true, ignoreAfterQuery));
    }
    return INSTANCE;
  }

  private AliasAwareSparseCFGCache(AliasAwareSparseCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  // TODO: unify in super
  public SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt, Val val) {
    for (String s : cache.keySet()) {
      if (s.startsWith(m.getSignature())) {
        SparseAliasingCFG sparseAliasingCFG = cache.get(s);
        if (sparseAliasingCFG.getGraph().nodes().contains(stmt)) {
          SparseCFGQueryLog queryLog =
              new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.FWD);
          logList.add(queryLog);
          return sparseAliasingCFG;
        }
      }
    }
    SparseCFGQueryLog queryLog = new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.FWD);
    logList.add(queryLog);
    // throw new RuntimeException("CFG not found for:" + m + " s:" + stmt);
    return null;
  }

  public synchronized SparseAliasingCFG getSparseCFGForBackwardPropagation(
      Val initialQueryVal,
      Statement initialQueryStmt,
      Method currentMethod,
      Val currentVal,
      Statement currentStmt) {

    SootMethod sootSurrentMethod = SootAdapter.asSootMethod(currentMethod);
    Stmt sootInitialQueryStmt = SootAdapter.asStmt(initialQueryStmt);
    Stmt sootCurrentStmt = SootAdapter.asStmt(currentStmt);
    // Value sootInitialQueryVal = SootAdapter.asValue(initialQueryVal);
    // Value sootCurrentQueryVal = SootAdapter.asValue(currentVal);

    String key =
        new StringBuilder(sootSurrentMethod.getSignature())
            .append("-")
            .append(initialQueryVal)
            .append("-")
            .append(sootInitialQueryStmt)
            .toString();

    if (cache.containsKey(key)) {
      if (cache.get(key).getGraph().nodes().contains(sootCurrentStmt)) {
        SparseCFGQueryLog queryLog =
            new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.BWD);
        logList.add(queryLog);
        return cache.get(key);
      } else {
        SparseCFGQueryLog queryLog =
            new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.BWD);
        queryLog.logStart();
        SparseAliasingCFG cfg =
            sparseCFGBuilder.buildSparseCFG(
                initialQueryVal, sootSurrentMethod, currentVal, sootCurrentStmt, queryLog);
        queryLog.logEnd();
        cache.put(key + currentStmt, cfg);
        logList.add(queryLog);
        return cfg;
      }
    } else if (cache.containsKey(key + currentStmt)) {
      SparseCFGQueryLog queryLog =
          new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.BWD);
      logList.add(queryLog);
      return cache.get(key + currentStmt);
    } else {
      SparseCFGQueryLog queryLog =
          new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.BWD);
      queryLog.logStart();
      SparseAliasingCFG cfg =
          sparseCFGBuilder.buildSparseCFG(
              initialQueryVal, sootSurrentMethod, currentVal, sootCurrentStmt, queryLog);
      queryLog.logEnd();
      cache.put(key, cfg);
      logList.add(queryLog);
      return cfg;
    }
  }

  @Override
  public List<SparseCFGQueryLog> getQueryLogs() {
    return logList;
  }
}
