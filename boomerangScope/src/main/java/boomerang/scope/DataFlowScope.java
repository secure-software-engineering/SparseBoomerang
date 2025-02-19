package boomerang.scope;

public interface DataFlowScope {

  DataFlowScope INCLUDE_ALL =
      new DataFlowScope() {
        @Override
        public boolean isExcluded(DeclaredMethod method) {
          return false;
        }

        @Override
        public boolean isExcluded(Method method) {
          return false;
        }
      };

  boolean isExcluded(DeclaredMethod method);

  boolean isExcluded(Method method);
}
