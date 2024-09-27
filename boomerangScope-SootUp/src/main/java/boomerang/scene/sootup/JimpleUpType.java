package boomerang.scene.sootup;

import boomerang.scene.AllocVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import sootup.core.types.ArrayType;
import sootup.core.types.ClassType;
import sootup.core.types.NullType;
import sootup.core.types.PrimitiveType;
import sootup.core.types.ReferenceType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.types.JavaClassType;

import java.util.stream.Collectors;

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
    JavaClassType targetType = (JavaClassType) ((JimpleUpType) targetVal).getDelegate();
    if (this.getDelegate() instanceof NullType) {
      return true;
    }
    JavaClassType sourceType = (JavaClassType) this.getDelegate();
    JavaSootClass targetClass =
            SootUpClient.getInstance().getSootClass(targetType);
    JavaSootClass sourceClass =
            SootUpClient.getInstance().getSootClass(sourceType);
    // if (targetClass.isPhantomClass() || sourceClass.isPhantomClass()) return false;
    if (target instanceof AllocVal && ((AllocVal) target).getAllocVal().isNewExpr()) {
      boolean castFails =
              SootUpClient.getInstance().getView().getTypeHierarchy().isSubtype(targetType, sourceType);
      return !castFails;
    }
    // TODO this line is necessary as canStoreType does not properly work for
    // interfaces, see Java doc.
    if (targetClass.isInterface()) {
      return false;
    }
    boolean castFails =
            SootUpClient.getInstance().getView().getTypeHierarchy().isSubtype(targetType, sourceType)
                    || SootUpClient.getInstance()
                    .getView()
                    .getTypeHierarchy()
                    .isSubtype(sourceType, targetType);
    return !castFails;
  }

  @Override
  public boolean isSubtypeOf(String type) {
    if (delegate.toString().equals(type)) return true;
    if (!(delegate instanceof ReferenceType)) {
      if (delegate instanceof PrimitiveType) {
        return type.equals(delegate.toString());
      }
      return false;
    }

    JavaClassType superType = SootUpClient.getInstance().getIdentifierFactory().getClassType(type);
    JavaSootClass superClass = SootUpClient.getInstance().getSootClass(superType);

    JavaClassType allocatedType = (JavaClassType) delegate;
    JavaSootClass sootClass = SootUpClient.getInstance().getSootClass(allocatedType);
    if (!superClass.isInterface()) {
      return SootUpClient.getInstance()
              .getView()
              .getTypeHierarchy()
              .isSubtype(sootClass.getType(), superClass.getType());
    }

    if (SootUpClient.getInstance()
            .getView()
            .getTypeHierarchy()
            .subclassesOf(superClass.getType())
            .collect(Collectors.toSet())
            .contains(allocatedType)) {
      return true;
    }
    return SootUpClient.getInstance()
            .getView()
            .getTypeHierarchy()
            .implementersOf(superClass.getType())
            .collect(Collectors.toSet())
            .contains(allocatedType);
  }

  @Override
  public boolean isBooleanType() {
    return false;
  }

  public sootup.core.types.Type getDelegate() {
    return delegate;
  }
}
