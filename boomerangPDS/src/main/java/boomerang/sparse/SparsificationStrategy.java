package boomerang.sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;

public interface SparsificationStrategy<M extends Method, S extends Statement> {
    SparseCFGCache<M,S> getInstance(boolean ignoreAfterQuery);
}
