package boomerang.framework.soot.jimple;

import boomerang.scene.Field;
import boomerang.scene.InstanceFieldRef;
import boomerang.scene.Val;

public class JimpleInstanceFieldRef implements InstanceFieldRef {

  private final soot.jimple.InstanceFieldRef delegate;
  private final JimpleMethod m;

  public JimpleInstanceFieldRef(soot.jimple.InstanceFieldRef ifr, JimpleMethod m) {
    this.delegate = ifr;
    this.m = m;
  }

  public Val getBase() {
    return new JimpleVal(delegate.getBase(), m);
  }

  public Field getField() {
    return new JimpleField(delegate.getField());
  }
}
