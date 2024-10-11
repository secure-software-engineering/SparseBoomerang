package boomerang.sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.sparse.eval.PropagationCounter;

public interface SparsificationStrategy<M extends Method, S extends Statement> {
  SparseCFGCache<M, S> getInstance(boolean ignoreAfterQuery);

  PropagationCounter getCounter();

  class NoSparsification implements SparsificationStrategy {
    @Override
    public SparseCFGCache getInstance(boolean ignoreAfterQuery) {
      return null;
    }

    @Override
    public PropagationCounter getCounter() {
      return new PropagationCounter(this);
    }
  }
}
