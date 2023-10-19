package boomerang.scene.jimple;

import boomerang.scene.Field;
import boomerang.scene.InstanceFieldRef;
import boomerang.scene.Val;
import boomerang.scene.up.SootUpClient;
import sootup.core.jimple.common.ref.JInstanceFieldRef;

public class JimpleInstanceFieldRef implements InstanceFieldRef {

  private JInstanceFieldRef delegate;
  private JimpleMethod m;

  public JimpleInstanceFieldRef(JInstanceFieldRef ifr, JimpleMethod m) {
    this.delegate = ifr;
    this.m = m;
  }

  public Val getBase() {
    return new JimpleVal(delegate.getBase(), m);
  }

  public Field getField() {
    return new JimpleField(SootUpClient.getInstance().getSootField(delegate.getFieldSignature()));
  }
}
