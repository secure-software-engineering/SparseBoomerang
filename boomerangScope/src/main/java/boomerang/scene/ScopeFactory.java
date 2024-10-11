package boomerang.scene;

import java.util.stream.Stream;

public interface ScopeFactory {
  Val getTrueValue(Method m);

  Val getFalseValue(Method m);

  Stream<Method> handleStaticFieldInitializers(Val fact);

  StaticFieldVal newStaticFieldVal(Field field, Method m);
}
