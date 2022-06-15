package boomerang.scene.sparse.eval;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;

/**
 * For logging if a SparseCFG was retrieved from cache, or it was built for the first time. And how
 * long did it take to build it.
 */
public class SparseCFGQueryLog {

  private boolean retrievedFromCache;

  private QueryDirection direction;

  public enum QueryDirection {
    FWD,
    BWD;
  }

  private final Stopwatch watch;

  public SparseCFGQueryLog(boolean retrievedFromCache, QueryDirection direction) {
    this.retrievedFromCache = retrievedFromCache;
    this.direction = direction;
    if (!retrievedFromCache) {
      this.watch = Stopwatch.createUnstarted();
    } else {
      this.watch = null;
    }
  }

  public void logStart() {
    if (!retrievedFromCache) {
      this.watch.start();
    }
  }

  public void logEnd() {
    if (!retrievedFromCache) {
      this.watch.stop();
    }
  }

  /**
   * in microseconds
   *
   * @return
   */
  public long getDuration() {
    if (!retrievedFromCache) {
      return this.watch.elapsed(TimeUnit.MICROSECONDS);
    } else {
      return -1;
    }
  }

  public boolean isRetrievedFromCache() {
    return retrievedFromCache;
  }

  public QueryDirection getDirection() {
    return direction;
  }
}
