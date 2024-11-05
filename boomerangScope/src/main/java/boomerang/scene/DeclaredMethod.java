package boomerang.scene;

import java.util.List;

public abstract class DeclaredMethod {

  private final InvokeExpr inv;

  public DeclaredMethod(InvokeExpr inv) {
    this.inv = inv;
  }

  public abstract boolean isNative();

  public abstract String getSubSignature();

  public abstract String getName();

  public abstract boolean isStatic();

  public abstract boolean isConstructor();

  public abstract String getSignature();

  public abstract WrappedClass getDeclaringClass();

  public abstract List<Type> getParameterTypes();

  public abstract Type getParameterType(int index);

  public InvokeExpr getInvokeExpr() {
    return inv;
  }
}
