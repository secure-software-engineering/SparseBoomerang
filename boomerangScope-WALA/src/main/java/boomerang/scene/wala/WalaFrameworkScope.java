package boomerang.scene.wala;

import boomerang.scene.*;
import java.util.stream.Stream;

public class WalaFrameworkScope implements FrameworkScopeFactory {
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
}
