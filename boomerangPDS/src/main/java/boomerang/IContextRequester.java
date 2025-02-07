package boomerang;

import boomerang.scene.Statement;
import java.util.Collection;

public interface IContextRequester {
  Collection<Context> getCallSiteOf(Context child);

  Context initialContext(Statement stmt);
}
