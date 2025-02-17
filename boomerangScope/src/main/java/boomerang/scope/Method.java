package boomerang.scope;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import de.fraunhofer.iem.Location;
import java.util.Collection;
import java.util.List;
import java.util.Set;

public abstract class Method implements Location {
  private static Method epsilon;

  protected Method() {}

  public static Method epsilon() {
    if (epsilon == null)
      epsilon =
          new Method() {
            @Override
            public int hashCode() {
              return System.identityHashCode(this);
            }

            @Override
            public boolean equals(Object obj) {
              return obj == this;
            }

            @Override
            public boolean isStaticInitializer() {
              return false;
            }

            @Override
            public boolean isParameterLocal(Val val) {
              return false;
            }

            @Override
            public List<Type> getParameterTypes() {
              return Lists.newArrayList();
            }

            @Override
            public Type getParameterType(int index) {
              return null;
            }

            @Override
            public Type getReturnType() {
              return null;
            }

            @Override
            public boolean isThisLocal(Val val) {
              return false;
            }

            @Override
            public Set<Val> getLocals() {
              return Sets.newHashSet();
            }

            @Override
            public Val getThisLocal() {
              return null;
            }

            @Override
            public List<Val> getParameterLocals() {
              return Lists.newArrayList();
            }

            @Override
            public boolean isStatic() {
              return false;
            }

            @Override
            public boolean isNative() {
              return false;
            }

            @Override
            public List<Statement> getStatements() {
              return Lists.newArrayList();
            }

            @Override
            public WrappedClass getDeclaringClass() {
              return null;
            }

            @Override
            public ControlFlowGraph getControlFlowGraph() {
              return null;
            }

            @Override
            public String getSubSignature() {
              return null;
            }

            @Override
            public String getName() {
              return null;
            }

            @Override
            public boolean isConstructor() {
              return false;
            }

            @Override
            public boolean isPublic() {
              return false;
            }
          };
    return epsilon;
  }

  // TODO: [ms] looks as if this toString() should go into epsilon..
  @Override
  public String toString() {
    return "METHOD EPS";
  }

  public abstract boolean isStaticInitializer();

  public abstract boolean isParameterLocal(Val val);

  public abstract List<Type> getParameterTypes();

  public abstract Type getParameterType(int index);

  public abstract Type getReturnType();

  public abstract boolean isThisLocal(Val val);

  public abstract Set<Val> getLocals();

  public abstract Val getThisLocal();

  public abstract List<Val> getParameterLocals();

  public abstract boolean isStatic();

  public abstract boolean isNative();

  public abstract List<Statement> getStatements();

  public abstract WrappedClass getDeclaringClass();

  public abstract ControlFlowGraph getControlFlowGraph();

  public abstract String getSubSignature();

  public abstract String getName();

  public Val getParameterLocal(int i) {
    return getParameterLocals().get(i);
  }

  public abstract boolean isConstructor();

  public abstract boolean isPublic();

  private Collection<Val> returnLocals;

  public Collection<Val> getReturnLocals() {
    if (returnLocals == null) {
      returnLocals = Sets.newHashSet();
      for (Statement s : getStatements()) {
        if (s.isReturnStmt()) {
          returnLocals.add(s.getReturnOp());
        }
      }
    }
    return returnLocals;
  }

  @Override
  public boolean accepts(Location other) {
    return this.equals(other);
  }
}
