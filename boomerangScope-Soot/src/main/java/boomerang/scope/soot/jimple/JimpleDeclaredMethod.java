package boomerang.scope.soot.jimple;

import boomerang.scope.DeclaredMethod;
import boomerang.scope.InvokeExpr;
import boomerang.scope.Method;
import boomerang.scope.Type;
import boomerang.scope.WrappedClass;
import java.util.ArrayList;
import java.util.List;
import soot.SootMethod;

public class JimpleDeclaredMethod extends DeclaredMethod {

  private final SootMethod delegate;

  public JimpleDeclaredMethod(InvokeExpr inv, SootMethod method) {
    super(inv);
    this.delegate = method;
  }

  @Override
  public boolean isNative() {
    return delegate.isNative();
  }

  @Override
  public String getSubSignature() {
    return delegate.getSubSignature();
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
      return other.delegate == null;
    } else return delegate.equals(other.delegate);
  }

  @Override
  public boolean isConstructor() {
    return delegate.isConstructor();
  }

  @Override
  public String getSignature() {
    return delegate.getSignature();
  }

  @Override
  public Method getCalledMethod() {
    return JimpleMethod.of(delegate);
  }

  @Override
  public WrappedClass getDeclaringClass() {
    return new JimpleWrappedClass(delegate.getDeclaringClass());
  }

  @Override
  public List<Type> getParameterTypes() {
    List<Type> types = new ArrayList<>();

    for (soot.Type type : delegate.getParameterTypes()) {
      types.add(new JimpleType(type));
    }
    return types;
  }

  @Override
  public Type getParameterType(int index) {
    return new JimpleType(delegate.getParameterType(index));
  }

  @Override
  public Type getReturnType() {
    return new JimpleType(delegate.getReturnType());
  }

  public Object getDelegate() {
    return delegate;
  }
}
