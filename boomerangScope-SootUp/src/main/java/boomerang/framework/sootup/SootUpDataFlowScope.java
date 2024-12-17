package boomerang.framework.sootup;

import boomerang.scene.DataFlowScope;
import boomerang.scene.DeclaredMethod;
import boomerang.scene.Method;
import boomerang.scene.WrappedClass;

public class SootUpDataFlowScope {

  /**
   * Default DataFlowScope that excludes native methods and methods from third-party libraries
   *
   * @return the dataflow scope
   */
  public static DataFlowScope make() {
    return new DataFlowScope() {

      @Override
      public boolean isExcluded(DeclaredMethod method) {
        WrappedClass declaringClass = method.getDeclaringClass();

        return method.isNative() || !declaringClass.isApplicationClass();
      }

      public boolean isExcluded(Method method) {
        WrappedClass declaringClass = method.getDeclaringClass();

        return method.isNative() || !declaringClass.isApplicationClass();
      }
    };
  }
}
