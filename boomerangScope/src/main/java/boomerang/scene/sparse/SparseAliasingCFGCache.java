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

  public synchronized SparseAliasingCFG getSparseCFG(SootMethod m, Value d, Stmt stmt) {
    String key =
        new StringBuilder(m.getSignature())
            .append("-")
            .append(d)
            .append("-")
            .append(stmt)
            .toString();

    if (cache.containsKey(key)) {
      return cache.get(key);
    } else {
      SparseAliasingCFG cfg = sparseCFGBuilder.buildSparseCFG(m, d, stmt);
      cache.put(key, cfg);
      return cfg;
    }
  }
}
