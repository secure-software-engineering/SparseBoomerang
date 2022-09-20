package boomerang.scene.sparse.eval;

import com.google.common.base.Stopwatch;
import java.time.Duration;

/**
 * For logging if a SparseCFG was retrieved from cache, or it was built for the first time. And how
 * long did it take to build it.
 */
public class SparseCFGQueryLog {

  private boolean retrievedFromCache;

  private QueryDirection direction;
  private CacheAccessType accessType;

  public enum QueryDirection {
    FWD,
    BWD;
  }

  public enum CacheAccessType {
    B1,
    B2,
    B3,
    B4,
    F1,
    F2;
  }

  private final Stopwatch watch;
  private final Stopwatch cfgNumberWatch;
  private final Stopwatch findStmtsWatch;
  private final Stopwatch sparsifysWatch;

  public SparseCFGQueryLog(
      boolean retrievedFromCache, QueryDirection direction, CacheAccessType accessType) {
    this.retrievedFromCache = retrievedFromCache;
    this.direction = direction;
    this.accessType = accessType;
    if (!retrievedFromCache) {
      this.watch = Stopwatch.createUnstarted();
      this.cfgNumberWatch = Stopwatch.createUnstarted();
      this.findStmtsWatch = Stopwatch.createUnstarted();
      this.sparsifysWatch = Stopwatch.createUnstarted();
    } else {
      this.watch = null;
      this.cfgNumberWatch = null;
      this.findStmtsWatch = null;
      this.sparsifysWatch = null;
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
   * SparseCFG built time in microseconds, 0 if it was retrieved from the cache
   *
   * @return
   */
  public Duration getDuration() {
    if (!retrievedFromCache) {
      return this.watch.elapsed();
    } else {
      return Duration.ZERO;
    }
  }

  public void startCFGNumber() {
    if (!retrievedFromCache) {
      this.cfgNumberWatch.start();
    }
  }

  public void stopCFGNumber() {
    if (!isRetrievedFromCache()) {
      this.cfgNumberWatch.stop();
    }
  }

  public Duration getCFGNumberDuration() {
    if (!retrievedFromCache) {
      return this.cfgNumberWatch.elapsed();
    } else {
      return Duration.ZERO;
    }
  }

  public void startFindStmts() {
    if (!retrievedFromCache) {
      this.findStmtsWatch.start();
    }
  }

  public void stopFindStmts() {
    if (!isRetrievedFromCache()) {
      this.findStmtsWatch.stop();
    }
  }

  public Duration getFindStmtsDuration() {
    if (!retrievedFromCache) {
      return this.findStmtsWatch.elapsed();
    } else {
      return Duration.ZERO;
    }
  }

  public void startSparsify() {
    if (!retrievedFromCache) {
      this.sparsifysWatch.start();
    }
  }

  public void stopSparsify() {
    if (!isRetrievedFromCache()) {
      this.sparsifysWatch.stop();
    }
  }

  public Duration getSparsifyDuration() {
    if (!retrievedFromCache) {
      return this.sparsifysWatch.elapsed();
    } else {
      return Duration.ZERO;
    }
  }

  public boolean isRetrievedFromCache() {
    return retrievedFromCache;
  }

  public QueryDirection getDirection() {
    return direction;
  }

  public CacheAccessType getCacheAccessType() {
    return accessType;
  }
}
