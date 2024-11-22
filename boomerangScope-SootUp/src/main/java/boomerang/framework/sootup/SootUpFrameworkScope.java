package boomerang.framework.sootup;

import boomerang.scene.*;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static boomerang.framework.sootup.SootUpDataFlowScopeUtil.excludeComplex;

public class SootUpFrameworkScope implements FrameworkScope {

    @Nonnull
    private final JavaView view;
    private final CallGraph cg;
    @Nonnull
    private final List<Method> entrypoints;

    public SootUpFrameworkScope(@Nonnull JavaView view, @Nonnull sootup.callgraph.CallGraph cg, @Nonnull List<MethodSignature> entrypoints) {
        INSTANCE = this; // FIXME! [ms] this hack is disgusting!
        this.view = view;
        this.entrypoints = entrypoints.stream().map(ep -> view.getMethod(ep).get()).map(JimpleUpMethod::of).collect(Collectors.toList());

        this.cg = new SootUpCallGraph(cg, getEntrypoints());
    }

    private static SootUpFrameworkScope INSTANCE;

    public static SootUpFrameworkScope getInstance() {
        if (INSTANCE == null) {
            throw new RuntimeException("Client hasn't been initialized. Call setInstance first");
        }
        return INSTANCE;
    }

    @Override
    public List<Method> getEntrypoints() {
        return entrypoints;
    }

    @Override
    @Nonnull
    public Val getTrueValue(Method m) {
        return new JimpleUpVal(
                sootup.core.jimple.common.constant.IntConstant.getInstance(1), (JimpleUpMethod) m);
    }

    @Override
    @Nonnull
    public Val getFalseValue(Method m) {
        return new JimpleUpVal(
                sootup.core.jimple.common.constant.IntConstant.getInstance(0), (JimpleUpMethod) m);
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
    public Method getMethod(String signatureStr) {
        return JimpleUpMethod.of(
                view.getMethod(view.getIdentifierFactory().parseMethodSignature(signatureStr)).get());
    }

    @Override
    public CallGraph buildCallGraph() {
        return cg;
    }

    @Override
    public DataFlowScope getDataFlowScope() {
        return SootUpDataFlowScope.make();
    }

    @Override
    public void executeFramework() {
        // TODO: implement me?
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

    public JavaSootClass getSootClass(ClassType classType) {
        Optional<JavaSootClass> sootClass = view.getClass(classType);
        if (sootClass.isPresent()) {
            return sootClass.get();
        }
        throw new RuntimeException("Class not found: " + classType.getFullyQualifiedName());
    }

    public Optional<JavaSootMethod> getSootMethod(MethodSignature methodSignature) {
        Optional<JavaSootMethod> method = view.getMethod(methodSignature);
        if (method.isPresent()) {
            return method;
        }

        System.out.println("get" + methodSignature);

        Optional<ClassType> declaredClassOfMethod = view.getTypeHierarchy().superClassesOf(methodSignature.getDeclClassType()).filter(type -> {
            System.out.println("is it in? " + type);
            Optional<JavaSootClass> aClass = view.getClass(type);
            return aClass.map(javaSootClass -> {
                return javaSootClass.getMethod(methodSignature.getSubSignature()).isPresent();
            }).orElse(false);
        }).findAny();

        if(declaredClassOfMethod.isEmpty()){
            return Optional.empty();
        };
        ClassType declClassType = declaredClassOfMethod.get();

        Optional<JavaSootClass> aClass = view.getClass(declClassType);
        return aClass.flatMap(javaSootClass -> javaSootClass.getMethod(methodSignature.getSubSignature()));
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
}
