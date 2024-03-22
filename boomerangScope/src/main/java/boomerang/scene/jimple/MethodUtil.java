package boomerang.scene.jimple;

import sootup.java.core.JavaSootMethod;

public class MethodUtil {
  protected static final String CONSTRUCTOR_NAME = "<init>";
  protected static final String STATIC_INITIALIZER_NAME = "<clinit>";

  public static boolean isConstructor(JavaSootMethod m) {
    return m.getSignature().getName().equals(CONSTRUCTOR_NAME);
  }

  public static boolean isStaticInitializer(JavaSootMethod m) {
    return m.getSignature().getName().equals(STATIC_INITIALIZER_NAME);
  }
}
