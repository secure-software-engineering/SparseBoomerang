package boomerang.scene.sparse.eval;

import boomerang.scene.sparse.SparseCFGCache;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

public class EvalPrinter {

  private String evalName;

  public EvalPrinter(String evalName) {
    this.evalName = evalName;
  }

  /**
   * how many SparseCFGs retrieved from cache, how many was built. How long did it take in total.
   *
   * @param cache
   */
  public void printCachePerformance(SparseCFGCache cache) {
    List<SparseCFGQueryLog> queryLogs = cache.getQueryLogs();
    try (FileWriter writer = new FileWriter(evalName + "-" + "sparseCFGCache.csv")) {
      long count = 0;
      for (SparseCFGQueryLog queryLog : queryLogs) {
        count++;
        StringBuilder str = new StringBuilder(Long.toString(count));
        str.append(",");
        str.append(queryLog.isRetrievedFromCache());
        str.append(",");
        str.append(queryLog.getDirection());
        str.append(",");
        str.append(queryLog.getDuration());
        str.append(System.lineSeparator());
        writer.write(str.toString());
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
