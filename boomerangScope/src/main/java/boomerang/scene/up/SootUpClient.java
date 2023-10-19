package boomerang.scene.up;

import java.util.Optional;

import sootup.core.model.SootField;
import sootup.core.model.SootMethod;
import sootup.core.signatures.FieldSignature;
import sootup.core.signatures.MethodSignature;
import sootup.core.types.ClassType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

/** View needs to be provided by the client. */
public class SootUpClient {

  private JavaView view;

  private JavaIdentifierFactory identifierFactory;

  private SootUpClient(JavaView view, JavaIdentifierFactory idFactory) {
    this.view = view;
    this.identifierFactory = idFactory;
  }

  private static SootUpClient INSTANCE;

  public static SootUpClient getInstance(JavaView view, JavaIdentifierFactory idFactory){
    if(INSTANCE == null){
      INSTANCE = new SootUpClient(view, idFactory);
    }
    return INSTANCE;
  }

  public static SootUpClient getInstance(){
    if(INSTANCE==null){
      throw new RuntimeException("Client hasn't been initialized");
    }
    return INSTANCE;
  }

  public JavaView getView() {
    return view;
  }

  public JavaIdentifierFactory getIdentifierFactory() {
    return identifierFactory;
  }

  // TODO: move these to util
  public JavaSootClass getSootClass(String className) {
    ClassType classType = this.identifierFactory.getClassType(className);
    Optional<JavaSootClass> aClass = view.getClass(classType);
    if (aClass.isPresent()) {
      return aClass.get();
    }
    throw new RuntimeException("Class not found: " + className);
  }

  public JavaSootMethod getSootMethod(MethodSignature methodSignature) {
    Optional<? extends SootMethod> method = view.getMethod(methodSignature);
    if (method.isPresent()) {
      return (JavaSootMethod) method.get();
    }
    throw new RuntimeException("Method not found: " + methodSignature);
  }

  public JavaSootField getSootField(FieldSignature fieldSignature) {
    Optional<? extends SootField> field = view.getField(fieldSignature);
    if (field.isPresent()) {
      return (JavaSootField) field.get();
    }
    throw new RuntimeException("Field not found: " + fieldSignature);
  }
}
