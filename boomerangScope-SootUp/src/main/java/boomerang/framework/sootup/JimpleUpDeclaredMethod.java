package boomerang.framework.sootup;

import boomerang.scene.DeclaredMethod;
import boomerang.scene.Method;
import boomerang.scene.WrappedClass;
import java.util.Arrays;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class JimpleUpDeclaredMethod extends DeclaredMethod {

  private final JavaSootMethod delegate;

  public JimpleUpDeclaredMethod(JimpleUpInvokeExpr invokeExpr, JavaSootMethod delegate) {
    super(invokeExpr);

    this.delegate = delegate;
  }

  public JavaSootMethod getDelegate() {
    return delegate;
  }

  @Override
  public boolean isNative() {
    return delegate.isNative();
  }

  @Override
  public String getSubSignature() {
    return delegate.getSignature().getSubSignature().toString();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public boolean isStatic() {
    return delegate.isStatic();
  }

  @Override
  public boolean isConstructor() {
    return SootUpFrameworkScope.isConstructor(delegate);
  }

  @Override
  public String getSignature() {
    return delegate.getSignature().toString();
  }

  @Override
  public Method getCalledMethod() {
    return JimpleUpMethod.of(delegate);
  }

  @Override
  public WrappedClass getDeclaringClass() {
    JavaSootClass sootClass =
        SootUpFrameworkScope.getInstance().getSootClass(delegate.getDeclaringClassType());
    return new JimpleUpWrappedClass(sootClass);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(new Object[] {delegate});
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;

    JimpleUpDeclaredMethod other = (JimpleUpDeclaredMethod) obj;
    if (delegate == null) {
      return other.delegate == null;
    } else return delegate.equals(other.delegate);
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
