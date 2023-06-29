package boomerang.scene.up;

import java.util.Optional;
import sootup.core.IdentifierFactory;
import sootup.core.model.SootField;
import sootup.core.model.SootMethod;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

/** View needs to be provided by the client. */
public class Client {

  private static JavaView view;

  private static IdentifierFactory identifierFactory;

  public Client(JavaView view) {
    this.view = view;
  }

  // TODO: remove static for wip
  public static JavaView getView() {
    return view;
  }

  public static IdentifierFactory getIdentifierFactory() {
    return identifierFactory;
  }

  // TODO: move these to util
  public static JavaSootClass getSootClass(String className) {
    ClassType classType = identifierFactory.getClassType(className);
    Optional<JavaSootClass> aClass = view.getClass(classType);
    if (aClass.isPresent()) {
      return aClass.get();
    }
    throw new RuntimeException("Class not found: " + className);
  }

  public static JavaSootMethod getSootMethod(MethodSignature methodSignature) {
    Optional<? extends SootMethod> method = view.getMethod(methodSignature);
    if (method.isPresent()) {
      return (JavaSootMethod) method.get();
    }
    throw new RuntimeException("Method not found: " + methodSignature);
  }

  public static JavaSootField getSootField(FieldSignature fieldSignature) {
    Optional<? extends SootField> field = view.getField(fieldSignature);
    if (field.isPresent()) {
      return (JavaSootField) field.get();
    }
    throw new RuntimeException("Field not found: " + fieldSignature);
  }
}
