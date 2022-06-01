package boomerang.scene.sparse;

import java.util.HashMap;
import java.util.Map;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class SparseAliasingCFGCache {

  Map<String, SparseAliasingCFG> cache;
  SparseAliasingCFGBuilder sparseCFGBuilder;

  private static SparseAliasingCFGCache INSTANCE;

  private SparseAliasingCFGCache() {}

  public static SparseAliasingCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new SparseAliasingCFGCache(new SparseAliasingCFGBuilder(true));
    }
    return INSTANCE;
  }

  private SparseAliasingCFGCache(SparseAliasingCFGBuilder sparseCFGBuilder) {
    this.cache = new HashMap<>();
    this.sparseCFGBuilder = sparseCFGBuilder;
  }

  public SparseAliasingCFG getSparseCFG(SootMethod m, Stmt stmt) {
    for (String s : cache.keySet()) {
      if (s.startsWith(m.getSignature())) {
        SparseAliasingCFG sparseAliasingCFG = cache.get(s);
        if (sparseAliasingCFG.getGraph().nodes().contains(stmt)) {
          return sparseAliasingCFG;
        }
      }
    }
    throw new RuntimeException("CFG not found for:" + m + " s:" + stmt);
    // return null;
  }

  public synchronized SparseAliasingCFG getSparseCFG(
      Value queryVal, Stmt queryStmt, SootMethod m, Value d, Stmt stmt) {
    String key =
        new StringBuilder(m.getSignature())
            .append("-")
            .append(queryVal)
            .append("-")
            .append(queryStmt)
            .toString();

    if (cache.containsKey(key)) {
      if (cache.get(key).getGraph().nodes().contains(stmt)) {
        return cache.get(key);
      } else {
        SparseAliasingCFG cfg = sparseCFGBuilder.buildSparseCFG(queryVal, m, d, stmt);
        cache.put(key + stmt, cfg);
        return cfg;
      }
    } else if (cache.containsKey(key + stmt)) {
      return cache.get(key + stmt);
    } else {
      SparseAliasingCFG cfg = sparseCFGBuilder.buildSparseCFG(queryVal, m, d, stmt);
      cache.put(key, cfg);
      return cfg;
    }
  }
}
