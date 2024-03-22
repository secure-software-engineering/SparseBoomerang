package boomerang.scene;

import boomerang.scene.jimple.JimpleDeclaredMethod;
import boomerang.scene.jimple.JimpleMethod;
import com.google.common.base.Predicate;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sootup.callgraph.CallGraph;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

public class SootDataFlowScope {
  private static final Logger LOGGER = LoggerFactory.getLogger(SootDataFlowScope.class);
  private static final String HASH_CODE_SUB_SIG = "int hashCode()";
  private static final String TO_STRING_SUB_SIG = "java.lang.String toString()";
  private static final String EQUALS_SUB_SIG = "boolean equals(java.lang.Object)";
  private static final String CLONE_SIG = "java.lang.Object clone()";
  public static Predicate<JavaSootClass>[] classFilters;
  public static Predicate<JavaSootMethod>[] methodFilters;

  /**
   * Default data-flow scope that only excludes phantom and native methods.
   *
   * @param view
   * @param cg
   * @return
   */
  public static DataFlowScope make(JavaView view, CallGraph cg) {
    reset(view, cg);
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        return method.isNative();
        // previously: || ((JavaSootClass) m.getDeclaringClass().getDelegate()).isPhantomClass()
      }

      public boolean isExcluded(Method method) {
        return method.isNative();
      }
    };
  }

  /**
   * Excludes hashCode, toString, equals methods and the implementors of java.util.Collection,
   * java.util.Maps and com.google.common.collect.Multimap
   */
  public static DataFlowScope excludeComplex(JavaView view, CallGraph cg) {
    reset(view, cg);
    return new DataFlowScope() {
      @Override
      public boolean isExcluded(DeclaredMethod method) {
        JimpleDeclaredMethod m = (JimpleDeclaredMethod) method;
        for (Predicate<JavaSootClass> f : classFilters) {
          if (f.apply((JavaSootClass) m.getDeclaringClass().getDelegate())) {
            return true;
          }
        }
        for (Predicate<JavaSootMethod> f : methodFilters) {
          if (f.apply((JavaSootMethod) m.getDelegate())) {
            return true;
          }
        }
        return m.isNative();
      }

      public boolean isExcluded(Method method) {
        JimpleMethod m = (JimpleMethod) method;
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
        return m.isNative();
      }
    };
  }

  private static class MapFilter implements Predicate<JavaSootClass> {
    private static final String MAP = "java.util.Map";
    private static final String GUAVA_MAP = "com.google.common.collect.Multimap";
    private Set<JavaSootClass> excludes = Sets.newHashSet();

    public MapFilter(JavaView view) {
      JavaClassType mapClassType = JavaIdentifierFactory.getInstance().getClassType(MAP);
      Set<ClassType> mapImplementerTypes = view.getTypeHierarchy().implementersOf(mapClassType);
      List<JavaSootClass> mapSubClasses =
          mapImplementerTypes.stream()
              .map(t -> view.getClass(t).get())
              .collect(Collectors.toList());
      excludes.add(view.getClass(mapClassType).get());
      excludes.addAll(mapSubClasses);

      JavaClassType guavaMapClassType = JavaIdentifierFactory.getInstance().getClassType(GUAVA_MAP);
      Optional<JavaSootClass> guavaMapClassOp = view.getClass(guavaMapClassType);
      if (guavaMapClassOp.isPresent()) {
        JavaSootClass guavaMapClass = guavaMapClassOp.get();
        if (guavaMapClass.isInterface()) {
          Set<ClassType> guavaMapImplementerTypes =
              view.getTypeHierarchy().implementersOf(guavaMapClassType);
          List<JavaSootClass> guavaMapSubClasses =
              guavaMapImplementerTypes.stream()
                  .map(t -> view.getClass(t).get())
                  .collect(Collectors.toList());
          excludes.addAll(guavaMapSubClasses);
        }
      }

      for (JavaSootClass c : view.getClasses()) {
        if (c.hasOuterClass() && excludes.contains(c.getOuterClass())) {
          excludes.add(c);
        }
      }
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
    private Set<JavaSootClass> excludes = Sets.newHashSet();

    public IterableFilter(JavaView view) {
      JavaClassType iterClassType = JavaIdentifierFactory.getInstance().getClassType(ITERABLE);
      Set<ClassType> iterImplementerTypes = view.getTypeHierarchy().implementersOf(iterClassType);
      List<JavaSootClass> iterSubClasses =
          iterImplementerTypes.stream()
              .map(t -> view.getClass(t).get())
              .collect(Collectors.toList());
      excludes.addAll(iterSubClasses);

      for (JavaSootClass c : view.getClasses()) {
        if (c.hasOuterClass() && excludes.contains(c.getOuterClass())) {
          excludes.add(c);
        }
      }

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
    private Set<JavaSootMethod> excludes = Sets.newHashSet();

    public SubSignatureFilter(String subSig, sootup.callgraph.CallGraph cg, JavaView view) {
      Set<MethodSignature> reachableMethods = cg.getMethodSignatures();
      for (MethodSignature reachableMethod : reachableMethods) {
        if (reachableMethod.getSubSignature().toString().equals(subSig)) {
          excludes.add((JavaSootMethod) view.getMethod(reachableMethod).get());
        }
      }

      if (excludes.isEmpty()) {
        LOGGER.debug("Excludes empty for {}", subSig);
      }
    }

    @Override
    public boolean apply(JavaSootMethod m) {
      return excludes.contains(m);
    }
  }

  private static void reset(JavaView view, CallGraph cg) {
    classFilters = new Predicate[] {new MapFilter(view), new IterableFilter(view)};
    methodFilters =
        new Predicate[] {
          new SubSignatureFilter(HASH_CODE_SUB_SIG, cg, view),
          new SubSignatureFilter(TO_STRING_SUB_SIG, cg, view),
          new SubSignatureFilter(EQUALS_SUB_SIG, cg, view),
          new SubSignatureFilter(CLONE_SIG, cg, view)
        };
  }
}
