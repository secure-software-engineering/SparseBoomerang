package boomerang.framework.sootup;

import boomerang.scene.*;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

public class SootUpDataFlowScopeUtil {
  private static final Logger LOGGER = LoggerFactory.getLogger(SootUpDataFlowScopeUtil.class);
  private static final String HASH_CODE_SUB_SIG = "int hashCode()";
  private static final String TO_STRING_SUB_SIG = "java.lang.String toString()";
  private static final String EQUALS_SUB_SIG = "boolean equals(java.lang.Object)";
  private static final String CLONE_SIG = "java.lang.Object clone()";
  public static Predicate<JavaSootClass>[] classFilters;
  public static Predicate<JavaSootMethod>[] methodFilters;

  /**
   * Default data-flow scope that only excludes phantom and native methods.
   *
   * @return
   */
  public static DataFlowScope make(SootUpFrameworkScope scope) {
    reset(scope);
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        JimpleUpDeclaredMethod m = (JimpleUpDeclaredMethod) method;
        return m.getDeclaringClass().getDelegate() instanceof SootUpFrameworkScope.PhantomClass
            || m.isNative();
      }

      public boolean isExcluded(Method method) {
        JimpleUpMethod m = (JimpleUpMethod) method;
        return m.getDeclaringClass().getDelegate() instanceof SootUpFrameworkScope.PhantomClass
            || m.isNative();
      }
    };
  }

  /**
   * Excludes hashCode, toString, equals methods and the implementors of java.util.Collection,
   * java.util.Maps and com.google.common.collect.Multimap
   */
  public static DataFlowScope excludeComplex(SootUpFrameworkScope scope) {
    reset(scope);
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        JimpleUpDeclaredMethod m = (JimpleUpDeclaredMethod) method;
        for (Predicate<JavaSootClass> f : classFilters) {
          if (f.apply((JavaSootClass) m.getDeclaringClass().getDelegate())) {
            return true;
          }
        }
        for (Predicate<JavaSootMethod> f : methodFilters) {
          if (f.apply(m.getDelegate())) {
            return true;
          }
        }
        return m.getDeclaringClass().getDelegate() instanceof SootUpFrameworkScope.PhantomClass
            || m.isNative();
      }

      public boolean isExcluded(Method method) {
        JimpleUpMethod m = (JimpleUpMethod) method;
        for (Predicate<JavaSootClass> f : classFilters) {
          if (f.apply((JavaSootClass) m.getDeclaringClass().getDelegate())) {
            return true;
          }
        }
        for (Predicate<JavaSootMethod> f : methodFilters) {
          if (f.apply(m.getDelegate())) {
            return true;
          }
        }
        return m.getDeclaringClass().getDelegate() instanceof SootUpFrameworkScope.PhantomClass
            || m.isNative();
      }
    };
  }

  private static class MapFilter implements Predicate<JavaSootClass> {
    private static final String MAP = "java.util.Map";
    private static final String GUAVA_MAP = "com.google.common.collect.Multimap";
    private final Set<ClassType> excludes = Sets.newHashSet();

    public MapFilter(JavaView view) {
      JavaClassType mapClassType = view.getIdentifierFactory().getClassType(MAP);
      List<ClassType> mapSubClasses =
          view.getTypeHierarchy().implementersOf(mapClassType).collect(Collectors.toList());
      excludes.add(mapClassType);
      excludes.addAll(mapSubClasses);

      JavaClassType guavaMapType = view.getIdentifierFactory().getClassType(GUAVA_MAP);
      Optional<JavaSootClass> guavaMapOpt = view.getClass(guavaMapType);
      if (guavaMapOpt.isPresent()) {
        JavaSootClass c = guavaMapOpt.get();
        if (c.isInterface()) {
          view.getTypeHierarchy().implementersOf(guavaMapType).forEach(excludes::add);
        }
      }
      view.getClasses()
          .filter(c -> c.hasOuterClass() && excludes.contains(c.getOuterClass().get()))
          .forEach(
              c -> {
                excludes.add(guavaMapType);
              });

      if (excludes.isEmpty()) {
        LOGGER.debug("Excludes empty for {}", MAP);
      }
    }

    @Override
    public boolean apply(JavaSootClass c) {
      return excludes.contains(c);
    }
  }

  private static class IterableFilter implements Predicate<JavaSootClass> {
    private static final String ITERABLE = "java.lang.Iterable";
    private final Set<ClassType> excludes = Sets.newHashSet();

    public IterableFilter(JavaView view) {
      JavaClassType iterableClassType = view.getIdentifierFactory().getClassType(ITERABLE);

      view.getTypeHierarchy().implementersOf(iterableClassType).forEach(excludes::add);

      view.getClasses()
          .filter(c -> c.hasOuterClass() && excludes.contains(c.getOuterClass().get()))
          .forEach(
              c -> {
                excludes.add(iterableClassType);
              });

      if (excludes.isEmpty()) {
        LOGGER.debug("Excludes empty for {}", ITERABLE);
      }
    }

    @Override
    public boolean apply(JavaSootClass c) {
      return excludes.contains(c);
    }
  }

  private static class SubSignatureFilter implements Predicate<JavaSootMethod> {
    private Set<String> excludes = Sets.newHashSet();

    public SubSignatureFilter(SootUpFrameworkScope scope, String subSig) {
      scope.getCallGraph().getReachableMethods().stream()
          .map(Method::getSubSignature)
          .filter(methodSig -> methodSig.equals(subSig))
          .forEach(excludes::add);
      if (excludes.isEmpty()) {
        LOGGER.debug("Excludes empty for {}", subSig);
      }
    }

    @Override
    public boolean apply(JavaSootMethod m) {
      return excludes.contains(m);
    }
  }

  private static void reset(SootUpFrameworkScope scope) {
    classFilters =
        new Predicate[] {new MapFilter(scope.getView()), new IterableFilter(scope.getView())};
    methodFilters =
        new Predicate[] {
          new SubSignatureFilter(scope, HASH_CODE_SUB_SIG),
          new SubSignatureFilter(scope, TO_STRING_SUB_SIG),
          new SubSignatureFilter(scope, EQUALS_SUB_SIG),
          new SubSignatureFilter(scope, CLONE_SIG)
        };
  }
}
