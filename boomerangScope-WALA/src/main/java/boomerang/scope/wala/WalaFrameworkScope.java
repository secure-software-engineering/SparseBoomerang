package boomerang.scope.wala;

import boomerang.scope.CallGraph;
import boomerang.scope.DataFlowScope;
import boomerang.scope.Field;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import boomerang.scope.StaticFieldVal;
import boomerang.scope.Val;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public class WalaFrameworkScope implements FrameworkScope {

  @Override
  public Val getTrueValue(Method m) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public Val getFalseValue(Method m) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public Stream<Method> handleStaticFieldInitializers(Val fact) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public StaticFieldVal newStaticFieldVal(Field field, Method m) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Nonnull
  @Override
  public Method resolveMethod(String signatureStr) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public CallGraph getCallGraph() {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public void updateDataFlowScope(DataFlowScope dataFlowScope) {}

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    throw new UnsupportedOperationException("implement me!");
  }
}
