package boomerang.scene;

import java.util.stream.Stream;
import javax.annotation.Nonnull;

public interface FrameworkScope {

  CallGraph getCallGraph();

  DataFlowScope getDataFlowScope();

  Val getTrueValue(Method m);

  Val getFalseValue(Method m);

  Stream<Method> handleStaticFieldInitializers(Val fact);

  StaticFieldVal newStaticFieldVal(Field field, Method m);

  // TODO Refactor: Only used in tests
  void updateDataFlowScope(DataFlowScope dataFlowScope);

  // TODO Refactor: Only used in tests
  @Nonnull
  Method resolveMethod(String signatureStr);

  // TODO Refactor: Only used in tests
  DataFlowScope createDataFlowScopeWithoutComplex();
}
