package boomerang.scene.jimple;

import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.WrappedClass;
import boomerang.scene.up.SootUpClient;
import com.google.common.collect.Interner;
import com.google.common.collect.Interners;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import sootup.core.jimple.basic.Local;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;

public class JimpleMethod extends Method {
  private JavaSootMethod delegate;

  protected static Interner<JimpleMethod> INTERNAL_POOL = Interners.newWeakInterner();
  protected ControlFlowGraph cfg;
  private List<Val> parameterLocalCache;
  private Set<Val> localCache;

  protected JimpleMethod(JavaSootMethod m) {
    this.delegate = m;
    if (!m.hasBody()) {
      throw new RuntimeException("Building a Jimple method without active body present");
    }
  }

  public static JimpleMethod of(JavaSootMethod m) {
    return INTERNAL_POOL.intern(new JimpleMethod(m));
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
    JimpleMethod other = (JimpleMethod) obj;
    if (delegate == null) {
      if (other.delegate != null) return false;
    } else if (!delegate.equals(other.delegate)) return false;
    return true;
  }

  @Override
  public String toString() {
    return delegate != null ? delegate.toString() : "METHOD EPS";
  }

  public boolean isStaticInitializer() {
    return delegate.isStaticInitializer();
  }

  public boolean isParameterLocal(Val val) {
    if (val.isStatic()) return false;
    if (!delegate.hasBody()) {
      throw new RuntimeException("Soot Method has no active body");
    }

    List<Val> parameterLocals = getParameterLocals();
    return parameterLocals.contains(val);
  }

  public boolean isThisLocal(Val val) {
    if (val.isStatic()) return false;
    if (!delegate.hasBody()) {
      throw new RuntimeException("Soot Method has no active body");
    }
    if (delegate.isStatic()) return false;
    Val parameterLocals = getThisLocal();
    return parameterLocals.equals(val);
  }

  public Set<Val> getLocals() {
    if (localCache == null) {
      localCache = Sets.newHashSet();
      Set<Local> locals = delegate.getBody().getLocals();
      for (Local l : locals) {
        localCache.add(new JimpleVal(l, this));
      }
    }
    return localCache;
  }

  public Val getThisLocal() {
    return new JimpleVal(delegate.getBody().getThisLocal(), this);
  }

  public List<Val> getParameterLocals() {
    if (parameterLocalCache == null) {
      parameterLocalCache = Lists.newArrayList();
      for (Local v : delegate.getBody().getParameterLocals()) {
        parameterLocalCache.add(new JimpleVal(v, this));
      }
    }
    return parameterLocalCache;
  }

  public boolean isStatic() {
    return delegate.isStatic();
  }

  public boolean isNative() {
    return delegate.isNative();
  }

  public List<Statement> getStatements() {
    return getControlFlowGraph().getStatements();
  }

  public WrappedClass getDeclaringClass() {
    JavaSootClass sootClass =
        SootUpClient.getInstance().getSootClass(delegate.getDeclaringClassType().getFullyQualifiedName());
    return new JimpleWrappedClass(sootClass);
  }

  public ControlFlowGraph getControlFlowGraph() {
    if (cfg == null) {
      cfg = new JimpleControlFlowGraph(this);
    }
    return cfg;
  }

  public String getSubSignature() {
    return delegate.getSignature().getSubSignature().toString();
  }

  public JavaSootMethod getDelegate() {
    return delegate;
  }

  public String getName() {
    return delegate.getName();
  }

  @Override
  public boolean isConstructor() {
    return delegate.isConstructor();
  }

  @Override
  public boolean isPublic() {
    return delegate.isPublic();
  }
}
