package boomerang.framework.sootup;

import static boomerang.framework.sootup.SootUpDataFlowScopeUtil.excludeComplex;

import boomerang.framework.sootup.jimple.JimpleUpField;
import boomerang.framework.sootup.jimple.JimpleUpMethod;
import boomerang.framework.sootup.jimple.JimpleUpStaticFieldVal;
import boomerang.framework.sootup.jimple.JimpleUpVal;
import boomerang.scene.*;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import sootup.core.inputlocation.EagerInputLocation;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.model.ClassModifier;
import sootup.core.model.SourceType;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.*;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;

public class SootUpFrameworkScope implements FrameworkScope {

  protected final JavaView view;
  protected final CallGraph sootUpCallGraph;
  protected DataFlowScope dataflowScope;

  public SootUpFrameworkScope(
      @Nonnull JavaView view,
      @Nonnull sootup.callgraph.CallGraph callGraph,
      @Nonnull Collection<JavaSootMethod> entryPoints,
      @Nonnull DataFlowScope dataFlowScope) {
    INSTANCE = this; // FIXME! [ms] this hack is disgusting!

    this.view = view;

    this.sootUpCallGraph = new SootUpCallGraph(callGraph);
    Collection<JimpleUpMethod> entryPointMethods =
        entryPoints.stream().map(JimpleUpMethod::of).collect(Collectors.toList());
    entryPointMethods.forEach(sootUpCallGraph::addEntryPoint);

    this.dataflowScope = dataFlowScope;
  }

  private static SootUpFrameworkScope INSTANCE;

  public static SootUpFrameworkScope getInstance() {
    if (INSTANCE == null) {
      throw new RuntimeException("Client hasn't been initialized. Call setInstance first");
    }
    return INSTANCE;
  }

  @Override
  @Nonnull
  public Val getTrueValue(Method m) {
    return new JimpleUpVal(IntConstant.getInstance(1), m);
  }

  @Override
  @Nonnull
  public Val getFalseValue(Method m) {
    return new JimpleUpVal(IntConstant.getInstance(0), m);
  }

  @Override
  @Nonnull
  public Stream<Method> handleStaticFieldInitializers(Val fact) {
    JimpleUpStaticFieldVal val = ((JimpleUpStaticFieldVal) fact);
    ClassType declaringClassType =
        ((JimpleUpField) val.field()).getDelegate().getDeclaringClassType();

    return view.getClass(declaringClassType).get().getMethods().stream()
        .filter(sootup.core.model.SootMethod::hasBody)
        .map(JimpleUpMethod::of);
  }

  @Override
  @Nonnull
  public StaticFieldVal newStaticFieldVal(Field field, Method m) {
    return new JimpleUpStaticFieldVal((JimpleUpField) field, m);
  }

  @Nonnull
  @Override
  public Method resolveMethod(String signatureStr) {
    return JimpleUpMethod.of(
        view.getMethod(view.getIdentifierFactory().parseMethodSignature(signatureStr)).get());
  }

  @Override
  public CallGraph getCallGraph() {
    return sootUpCallGraph;
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return dataflowScope;
  }

  @Override
  public void updateDataFlowScope(DataFlowScope dataFlowScope) {
    this.dataflowScope = dataFlowScope;
  }

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    return excludeComplex(this);
  }

  // ---

  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final String STATIC_INITIALIZER_NAME = "<clinit>";

  public JavaView getView() {
    return view;
  }

  public JavaIdentifierFactory getIdentifierFactory() {
    return view.getIdentifierFactory();
  }

  public JavaSootClass getSootClass(JavaClassType classType) {
    Optional<JavaSootClass> sootClass = view.getClass(classType);
    if (sootClass.isPresent()) {
      return sootClass.get();
    }

    OverridingJavaClassSource phantomClassSource =
        new OverridingJavaClassSource(
            new EagerInputLocation(),
            Paths.get("/phantom-class-in-memory"),
            classType,
            null,
            Collections.emptySet(),
            null,
            Collections.emptySet(),
            Collections.emptySet(),
            NoPositionInformation.getInstance(),
            EnumSet.noneOf(ClassModifier.class),
            Collections.emptyList(),
            Collections.emptyList(),
            Collections.emptyList());
    return new PhantomClass(phantomClassSource, SourceType.Application);
  }

  public Optional<JavaSootMethod> getSootMethod(MethodSignature methodSignature) {
    Optional<JavaSootMethod> method = view.getMethod(methodSignature);
    if (method.isPresent()) {
      return method;
    }

    //   System.out.println("get" + methodSignature);

    Optional<ClassType> declaredClassOfMethod =
        view.getTypeHierarchy()
            .superClassesOf(methodSignature.getDeclClassType())
            .filter(
                type -> {
                  //            System.out.println("is it in? " + type);
                  Optional<JavaSootClass> aClass = view.getClass(type);
                  return aClass
                      .map(
                          javaSootClass -> {
                            return javaSootClass
                                .getMethod(methodSignature.getSubSignature())
                                .isPresent();
                          })
                      .orElse(false);
                })
            .findAny();

    if (declaredClassOfMethod.isEmpty()) {
      return Optional.empty();
    }
    ClassType declClassType = declaredClassOfMethod.get();

    Optional<JavaSootClass> aClass = view.getClass(declClassType);
    return aClass.flatMap(
        javaSootClass -> javaSootClass.getMethod(methodSignature.getSubSignature()));
    // throw new RuntimeException("Method not found: " + methodSignature);
  }

  public JavaSootField getSootField(FieldSignature fieldSignature) {
    Optional<JavaSootField> field = view.getField(fieldSignature);
    if (field.isPresent()) {
      return field.get();
    }
    throw new RuntimeException("Field not found: " + fieldSignature);
  }

  public static boolean isConstructor(JavaSootMethod sootMethod) {
    return sootMethod.getSignature().getName().equals(CONSTRUCTOR_NAME);
  }

  public static boolean isStaticInitializer(JavaSootMethod sootMethod) {
    return sootMethod.getSignature().getName().equals(STATIC_INITIALIZER_NAME);
  }

  /** Dummy Phantom Class representation to mimic what Soot would do */
  public static class PhantomClass extends JavaSootClass {
    public PhantomClass(JavaSootClassSource classSource, SourceType sourceType) {
      super(classSource, sourceType);
    }
  }
}
