package boomerang.options;

import boomerang.scene.AllocVal;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Optional;

public interface IAllocationSite {

  Optional<AllocVal> getAllocationSite(Method method, Statement statement, Val fact);
}
