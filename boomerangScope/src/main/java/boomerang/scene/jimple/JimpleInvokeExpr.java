package boomerang.scene.jimple;

import boomerang.scene.DeclaredMethod;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Val;
import boomerang.scene.up.Client;
import com.google.common.collect.Lists;
import java.util.ArrayList;
import java.util.List;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.JSpecialInvokeExpr;
import sootup.core.jimple.common.expr.JStaticInvokeExpr;
import sootup.java.core.JavaSootMethod;

public class JimpleInvokeExpr implements InvokeExpr {

  private sootup.core.jimple.common.expr.AbstractInvokeExpr delegate;
  private Method m;
  private ArrayList<Val> cache;

  public JimpleInvokeExpr(sootup.core.jimple.common.expr.AbstractInvokeExpr ive, Method m) {
    this.delegate = ive;
    this.m = m;
  }

  public Val getArg(int index) {
    if (delegate.getArg(index) == null) {
      return Val.zero();
    }
    return new JimpleVal(delegate.getArg(index), m);
  }

  public List<Val> getArgs() {
    if (cache == null) {
      cache = Lists.newArrayList();
      for (int i = 0; i < delegate.getArgCount(); i++) {
        cache.add(getArg(i));
      }
    }
    return cache;
  }

  public boolean isInstanceInvokeExpr() {
    return delegate instanceof AbstractInstanceInvokeExpr;
  }

  public Val getBase() {
    AbstractInstanceInvokeExpr iie = (AbstractInstanceInvokeExpr) delegate;
    return new JimpleVal(iie.getBase(), m);
  }

  public DeclaredMethod getMethod() {
    JavaSootMethod sootMethod = Client.getSootMethod(delegate.getMethodSignature());
    return new JimpleDeclaredMethod(this, sootMethod);
  }

  public boolean isSpecialInvokeExpr() {
    return delegate instanceof JSpecialInvokeExpr;
  }

  public boolean isStaticInvokeExpr() {
    return delegate instanceof JStaticInvokeExpr;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
