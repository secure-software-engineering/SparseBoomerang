package boomerang.scene.sootup;

import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.NullType;
import sootup.core.types.ReferenceType;
import sootup.java.core.JavaSootClass;

public class JimpleUpType implements Type {

  private final sootup.core.types.Type delegate;

  public JimpleUpType(sootup.core.types.Type delegate) {
    this.delegate = delegate;
  }

  @Override
  public boolean isNullType() {
    return delegate instanceof NullType;
  }

  @Override
  public boolean isRefType() {
    return delegate instanceof ReferenceType;
  }

  @Override
  public boolean isArrayType() {
    return delegate instanceof ArrayType;
  }

  @Override
  public Type getArrayBaseType() {
    return new JimpleUpType(delegate).getArrayBaseType();
  }

  @Override
  public WrappedClass getWrappedClass() {
    ClassType classType = (ClassType) delegate;
    JavaSootClass sootClass = SootUpClient.getInstance().getSootClass(classType);
    return new JimpleUpWrappedClass(sootClass);
  }

  @Override
  public boolean doesCastFail(Type targetVal, Val target) {
    return false;
  }

  @Override
  public boolean isSubtypeOf(String type) {
    return false;
  }

  @Override
  public boolean isBooleanType() {
    return false;
  }

  public sootup.core.types.Type getDelegate() {
    return delegate;
  }
}
