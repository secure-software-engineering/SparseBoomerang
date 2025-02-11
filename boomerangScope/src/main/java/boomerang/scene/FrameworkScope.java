package boomerang.scene;

import java.util.List;
import java.util.stream.Stream;
import javax.annotation.Nonnull;

public interface FrameworkScope {

  List<Method> getEntrypoints();

  Val getTrueValue(Method m);

  Val getFalseValue(Method m);

  Stream<Method> handleStaticFieldInitializers(Val fact);

  StaticFieldVal newStaticFieldVal(Field field, Method m);

  // TODO: [ms] maybe refactor it - currently its only used in testcases
  @Nonnull
  Method getMethod(String signatureStr);

  // TODO: [ms] maybe refactor it - currently its only used in testcases
  CallGraph getCallGraph();

  // TODO: [ms] maybe refactor it - currently its only used in testcases
  DataFlowScope getDataFlowScope();

  // TODO: [ms] maybe refactor it - currently its only used in testcases
  DataFlowScope createDataFlowScopeWithoutComplex();
}
