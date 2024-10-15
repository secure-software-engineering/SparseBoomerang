package sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import sparse.eval.PropagationCounter;

public interface SparsificationStrategy<M extends Method, S extends Statement> {

  SparseCFGCache<M, S> getInstance(boolean ignoreAfterQuery);

  PropagationCounter getCounter();

  class NoSparsification<M extends Method, S extends Statement>
      implements SparsificationStrategy<M, S> {
    @Override
    public SparseCFGCache<M, S> getInstance(boolean ignoreAfterQuery) {
      // FIXME [ms]
      throw new UnsupportedOperationException("not implemented yet.");
    }

    @Override
    public PropagationCounter getCounter() {
      return new PropagationCounter(this);
    }
  }
}
