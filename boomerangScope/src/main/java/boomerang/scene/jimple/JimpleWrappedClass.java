package boomerang.scene.jimple;

import boomerang.scene.Method;
import boomerang.scene.Type;
import boomerang.scene.WrappedClass;
import boomerang.scene.up.SootUpClient;
import com.google.common.collect.Sets;
import java.util.Optional;
import java.util.Set;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;

public class JimpleWrappedClass implements WrappedClass {

  private JavaSootClass delegate;
  private Set<Method> methods;

  public JimpleWrappedClass(JavaSootClass delegate) {
    this.delegate = delegate;
  }

  public Set<Method> getMethods() {
    Set<JavaSootMethod> ms = (Set<JavaSootMethod>) delegate.getMethods();
    if (methods == null) {
      methods = Sets.newHashSet();
      for (JavaSootMethod m : ms) {
        if (m.hasBody()) methods.add(JimpleMethod.of(m));
      }
    }
    return methods;
  }

  public boolean hasSuperclass() {
    return delegate.hasSuperclass();
  }

  public WrappedClass getSuperclass() {
    Optional<JavaClassType> superclassType = (Optional<JavaClassType>) delegate.getSuperclass();
    JavaSootClass superClass = SootUpClient.getInstance().getSootClass(superclassType.get().getFullyQualifiedName());
    return new JimpleWrappedClass(superClass);
  }

  public Type getType() {
    return new JimpleType(delegate.getType());
  }

  public boolean isApplicationClass() {
    return delegate.isApplicationClass();
  }

  @Override
  public String toString() {
    return delegate.toString();
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
    JimpleWrappedClass other = (JimpleWrappedClass) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String getFullyQualifiedName() {
    return delegate.getName();
  }

  @Override
  public String getName() {
    return delegate.getName();
  }

  @Override
  public Object getDelegate() {
    return delegate;
  }
}
