package boomerang.scene.sparse.typebased;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.sparse.SootAdapter;
import boomerang.scene.sparse.aliasaware.SparseAliasingCFG;
import java.util.HashMap;
import java.util.Map;
import soot.SootMethod;
import soot.Value;
import soot.jimple.Stmt;

public class TypeBasedSparseCFGCache {

  Map<String, SparseAliasingCFG> cache;
  TypeBasedSparseCFGBuilder sparseCFGBuilder;

  private static TypeBasedSparseCFGCache INSTANCE;

  private TypeBasedSparseCFGCache() {}

  public static TypeBasedSparseCFGCache getInstance() {
    if (INSTANCE == null) {
      INSTANCE = new TypeBasedSparseCFGCache(new TypeBasedSparseCFGBuilder(true));
    }
    return INSTANCE;
  }

  private TypeBasedSparseCFGCache(TypeBasedSparseCFGBuilder sparseCFGBuilder) {
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
    // throw new RuntimeException("CFG not found for:" + m + " s:" + stmt);
    return null;
  }

  public synchronized SparseAliasingCFG getSparseCFG(
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
                sootInitialQueryVal, sootSurrentMethod, sootCurrentStmt);
        cache.put(key + currentStmt, cfg);
        return cfg;
      }
    } else if (cache.containsKey(key + currentStmt)) {
      return cache.get(key + currentStmt);
    } else {
      SparseAliasingCFG cfg =
          sparseCFGBuilder.buildSparseCFG(sootInitialQueryVal, sootSurrentMethod, sootCurrentStmt);
      cache.put(key, cfg);
      return cfg;
    }
  }
}
