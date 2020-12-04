package boomerang.flowfunction;

import boomerang.scene.DeclaredMethod;

public class FlowFunctionUtils {
  public static boolean isSystemArrayCopy(DeclaredMethod method) {
    return method.getName().equals("arraycopy")
        && method.getDeclaringClass().getName().equals("java.lang.System");
  }
}
