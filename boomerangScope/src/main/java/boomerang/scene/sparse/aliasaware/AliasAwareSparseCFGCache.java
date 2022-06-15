package boomerang.scene.sparse.aliasaware;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import java.util.HashMap;
import java.util.Map;

import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class AliasAwareSparseCFGCache implements SparseCFGCache {

  Map<String, SparseAliasingCFG> cache;
  AliasAwareSparseCFGBuilder sparseCFGBuilder;

  private static AliasAwareSparseCFGCache INSTANCE;

  private AliasAwareSparseCFGCache() {}

  public static AliasAwareSparseCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new AliasAwareSparseCFGCache(new AliasAwareSparseCFGBuilder(true));
    }
    return INSTANCE;
  }

  private AliasAwareSparseCFGCache(AliasAwareSparseCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  public SparseAliasingCFG getSparseCFGForForwardPropagation(SootMethod m, Stmt stmt) {
    for (String s : cache.keySet()) {
      if (s.startsWith(m.getSignature())) {
        SparseAliasingCFG sparseAliasingCFG = cache.get(s);
        if (sparseAliasingCFG.getGraph().nodes().contains(stmt)) {
          return sparseAliasingCFG;
        }
      }
    }
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
    Value sootInitialQueryVal = SootAdapter.asValue(initialQueryVal);
    Value sootCurrentQueryVal = SootAdapter.asValue(currentVal);

    String key =
        new StringBuilder(sootSurrentMethod.getSignature())
            .append("-")
            .append(initialQueryVal)
            .append("-")
            .append(sootInitialQueryStmt)
            .toString();

    if (cache.containsKey(key)) {
      if (cache.get(key).getGraph().nodes().contains(sootCurrentStmt)) {
        return cache.get(key);
      } else {
        SparseAliasingCFG cfg =
            sparseCFGBuilder.buildSparseCFG(
                sootInitialQueryVal, sootSurrentMethod, sootCurrentQueryVal, sootCurrentStmt);
        cache.put(key + currentStmt, cfg);
        return cfg;
      }
    } else if (cache.containsKey(key + currentStmt)) {
      return cache.get(key + currentStmt);
    } else {
      SparseAliasingCFG cfg =
          sparseCFGBuilder.buildSparseCFG(
              sootInitialQueryVal, sootSurrentMethod, sootCurrentQueryVal, sootCurrentStmt);
      cache.put(key, cfg);
      return cfg;
    }
  }
}
