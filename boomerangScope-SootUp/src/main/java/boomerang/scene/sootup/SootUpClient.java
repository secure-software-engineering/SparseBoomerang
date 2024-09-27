package boomerang.scene.sootup;

import java.util.Optional;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class SootUpClient {

  private static final String CONSTRUCTOR_NAME = "<init>";
  private static final String STATIC_INITIALIZER_NAME = "<clinit>";

  private final JavaView view;
  private final JavaIdentifierFactory identifierFactory;

  private SootUpClient(JavaView view, JavaIdentifierFactory idFactory) {
    this.view = view;
    this.identifierFactory = idFactory;
  }

  private static SootUpClient INSTANCE;

  public static void setInstance(JavaView view, JavaIdentifierFactory idFactory) {
    INSTANCE = new SootUpClient(view, idFactory);
  }

  public static SootUpClient getInstance() {
    if (INSTANCE == null) {
      throw new RuntimeException("Client hasn't been initialized. Call setInstance first");
    }
    return INSTANCE;
  }

  public JavaView getView() {
    return view;
  }

  public JavaIdentifierFactory getIdentifierFactory() {
    return identifierFactory;
  }

  public JavaSootClass getSootClass(ClassType classType) {
    Optional<JavaSootClass> sootClass = view.getClass(classType);
    if (sootClass.isPresent()) {
      return sootClass.get();
    }
    throw new RuntimeException("Class not found: " + classType.getFullyQualifiedName());
  }

  public JavaSootMethod getSootMethod(MethodSignature methodSignature) {
    Optional<JavaSootMethod> method = view.getMethod(methodSignature);
    if (method.isPresent()) {
      return method.get();
    }
    throw new RuntimeException("Method not found: " + methodSignature);
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
