package boomerang.scene.sparse.aliasaware;

import soot.SootFieldRef;
import soot.jimple.StaticFieldRef;

public class MStaticFieldRef extends StaticFieldRef {

  public MStaticFieldRef(SootFieldRef fieldRef) {
    super(fieldRef);
  }
}
