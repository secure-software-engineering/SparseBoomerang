package boomerang.scene.jimple;

import boomerang.scene.AllocVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import boomerang.scene.up.Client;
import sootup.core.types.*;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;

public class JimpleType implements Type {

  private sootup.core.types.Type delegate;

  public JimpleType(sootup.core.types.Type type) {
    this.delegate = type;
  }

  public boolean isNullType() {
    return delegate instanceof NullType;
  }

  public boolean isRefType() {
    return delegate instanceof ReferenceType;
  }

  @Override
  public boolean isBooleanType() {
    return delegate instanceof PrimitiveType.BooleanType;
  }

  public boolean isArrayType() {
    return delegate instanceof ArrayType;
  }

  public Type getArrayBaseType() {
    return new JimpleType(((ArrayType) delegate).getBaseType());
  }

  public WrappedClass getWrappedClass() {
    ClassType ref = (ClassType) this.delegate;
    JavaSootClass sootClass = Client.getSootClass(ref.getFullyQualifiedName());
    return new JimpleWrappedClass(sootClass);
  }

  public sootup.core.types.Type getDelegate() {
    return delegate;
  }

  @Override
  public boolean doesCastFail(Type targetVal, Val target) {
    JavaClassType targetType = (JavaClassType) ((JimpleType) targetVal).getDelegate();
    JavaClassType sourceType = (JavaClassType) this.getDelegate();
    JavaSootClass targetClass = Client.getSootClass(targetType.getFullyQualifiedName());
    JavaSootClass sourceClass = Client.getSootClass(sourceType.getFullyQualifiedName());
    if (targetClass.isPhantomClass() || sourceClass.isPhantomClass()) return false;
    if (target instanceof AllocVal && ((AllocVal) target).getAllocVal().isNewExpr()) {
      boolean castFails = Client.getView().getTypeHierarchy().isSubtype(targetType, sourceType);
      return !castFails;
    }
    // TODO this line is necessary as canStoreType does not properly work for
    // interfaces, see Java doc.
    if (targetClass.isInterface()) {
      return false;
    }
    boolean castFails =
        Client.getView().getTypeHierarchy().isSubtype(targetType, sourceType)
            || Client.getView().getTypeHierarchy().isSubtype(sourceType, targetType);
    return !castFails;
  }

  public boolean isSubtypeOf(String type) {
    JavaSootClass interfaceType = Client.getSootClass(type);
    if (delegate.toString().equals(type)) return true;
    if (!(delegate instanceof ReferenceType)) {
      if (delegate instanceof ArrayType) {
        return true;
      }
      if (delegate instanceof PrimitiveType) {
        return type.equals(delegate.toString());
      }
      throw new RuntimeException("More");
    }

    JavaClassType allocatedType = (JavaClassType) delegate;
    JavaSootClass sootClass = Client.getSootClass(allocatedType.getFullyQualifiedName());
    if (!interfaceType.isInterface()) {
      return Client.getView()
          .getTypeHierarchy()
          .isSubtype(sootClass.getType(), interfaceType.getType());
    }

    if (Client.getView()
        .getTypeHierarchy()
        .subclassesOf(interfaceType.getType())
        .contains(allocatedType)) {
      return true;
    }
    return Client.getView()
        .getTypeHierarchy()
        .implementersOf(interfaceType.getType())
        .contains(allocatedType);
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
    JimpleType other = (JimpleType) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String toString() {
    return delegate.toString();
  }
}
