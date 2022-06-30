package boomerang.scene.sparse.factspecific;

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
import soot.Value;
import soot.jimple.Stmt;

public class FactSpecificSparseCFGCache implements SparseCFGCache {

  List<SparseCFGQueryLog> logList = new ArrayList<>();

  Map<String, SparseAliasingCFG> cache;
  FactSpecificSparseCFGBuilder sparseCFGBuilder;
  Map<String, Stmt> methodToInitialQueryStmt;

  private static FactSpecificSparseCFGCache INSTANCE;

  private FactSpecificSparseCFGCache() {}

  public static FactSpecificSparseCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new FactSpecificSparseCFGCache(new FactSpecificSparseCFGBuilder(true));
    }
    return INSTANCE;
  }

  private FactSpecificSparseCFGCache(FactSpecificSparseCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.methodToInitialQueryStmt = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  public SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt, Val val) {
    Value sootCurrentQueryVal = SootAdapter.asValue(val);
    String key =
        new StringBuilder(m.getSignature()).append("-").append(sootCurrentQueryVal).toString();
    if (cache.containsKey(key)) {
      SparseCFGQueryLog queryLog =
          new SparseCFGQueryLog(true, SparseCFGQueryLog.QueryDirection.FWD);
      logList.add(queryLog);
      SparseAliasingCFG cfg = cache.get(key);
      return cfg;
    } else {
      // build for forward access too
      SparseCFGQueryLog queryLog =
          new SparseCFGQueryLog(false, SparseCFGQueryLog.QueryDirection.FWD);
      queryLog.logStart();
      SparseAliasingCFG cfg =
          sparseCFGBuilder.buildSparseCFG(
              val, m, stmt, methodToInitialQueryStmt.get(m.getSignature()));
      queryLog.logEnd();
      cache.put(key, cfg);
      return cfg;
    }
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
    //    Value sootInitialQueryVal = SootAdapter.asValue(initialQueryVal);
    Value sootCurrentQueryVal = SootAdapter.asValue(currentVal);
    methodToInitialQueryStmt.put(sootSurrentMethod.getSignature(), sootInitialQueryStmt);

    String key =
        new StringBuilder(sootSurrentMethod.getSignature())
            .append("-")
            .append(sootCurrentQueryVal)
            .toString();

    if (cache.containsKey(key)) {
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
              currentVal, sootSurrentMethod, sootCurrentStmt, sootInitialQueryStmt);
      queryLog.logEnd();
      cache.put(key, cfg);
      return cfg;
    }
  }

  @Override
  public List<SparseCFGQueryLog> getQueryLogs() {
    return logList;
  }
}
