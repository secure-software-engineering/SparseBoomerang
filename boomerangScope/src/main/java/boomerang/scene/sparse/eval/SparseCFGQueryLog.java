package boomerang.scene.sparse.eval;

import com.google.common.base.Stopwatch;
import java.util.concurrent.TimeUnit;

/**
 * For logging if a SparseCFG was retrieved from cache, or it was built for the first time. And how
 * long did it take to build it.
 */
public class SparseCFGQueryLog {

  private boolean retrievedFromCache;

  private final Stopwatch watch;

  public SparseCFGQueryLog(boolean retrievedFromCache) {
    this.retrievedFromCache = retrievedFromCache;
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
    throw new RuntimeException("Retrieved from cache, so duration wasn't measured!");
  }

  public void logEnd() {
    if (!retrievedFromCache) {
      this.watch.stop();
    }
    throw new RuntimeException("Retrieved from cache, so duration wasn't measured!");
  }

  /**
   * in microseconds
   *
   * @return
   */
  public long getDuration() {
    if (!retrievedFromCache) {
      return this.watch.elapsed(TimeUnit.MICROSECONDS);
    }else{
      return -1;
    }
  }

  public boolean isRetrievedFromCache(){
    return retrievedFromCache;
  }

}
