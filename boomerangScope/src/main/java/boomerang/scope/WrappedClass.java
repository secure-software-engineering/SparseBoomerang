package boomerang.scope;

import java.util.Set;

public interface WrappedClass {

  Set<Method> getMethods();

  boolean hasSuperclass();

  WrappedClass getSuperclass();

  Type getType();

  boolean isApplicationClass();

  String getFullyQualifiedName();

  String getName();

  Object getDelegate();
}
