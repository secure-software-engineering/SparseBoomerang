package boomerang.scope.sootup;

import boomerang.scope.DataFlowScope;
import boomerang.scope.DeclaredMethod;
import boomerang.scope.Method;
import boomerang.scope.WrappedClass;

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

        return !declaringClass.isApplicationClass();
      }

      public boolean isExcluded(Method method) {
        WrappedClass declaringClass = method.getDeclaringClass();

        return method.isPhantom() || !declaringClass.isApplicationClass();
      }
    };
  }
}
