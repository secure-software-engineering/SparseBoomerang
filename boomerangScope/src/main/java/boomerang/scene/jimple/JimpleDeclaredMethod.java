package boomerang.scene.jimple;

import boomerang.scene.DeclaredMethod;
import boomerang.scene.InvokeExpr;
import boomerang.scene.WrappedClass;
import boomerang.scene.up.SootUpClient;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class JimpleDeclaredMethod extends DeclaredMethod {

  private JavaSootMethod delegate;

  public JimpleDeclaredMethod(InvokeExpr inv, JavaSootMethod method) {
    super(inv);
    this.delegate = method;
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
  public String toString() {
    return delegate.toString();
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
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleDeclaredMethod other = (JimpleDeclaredMethod) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public boolean isConstructor() {
    return MethodUtil.isConstructor(delegate);
  }

  @Override
  public String getSignature() {
    return delegate.getSignature().toString();
  }

  @Override
  public WrappedClass getDeclaringClass() {
    JavaSootClass sootClass =
        SootUpClient.getInstance()
            .getSootClass(delegate.getDeclaringClassType().getFullyQualifiedName());
    return new JimpleWrappedClass(sootClass);
  }

  public Object getDelegate() {
    return delegate;
  }
}
