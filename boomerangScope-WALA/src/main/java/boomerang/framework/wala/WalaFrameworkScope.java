package boomerang.framework.wala;

import boomerang.scene.*;
import java.util.List;
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
  public Method getMethod(String signatureStr) {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public CallGraph buildCallGraph() {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    throw new UnsupportedOperationException("implement me!");
  }

  @Override
  public List<Method> getEntrypoints() {
    throw new UnsupportedOperationException("implement me!");
  }
}
