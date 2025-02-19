package boomerang.scope;

public interface DataFlowScope {

  boolean isExcluded(DeclaredMethod method);

  boolean isExcluded(Method method);
}
