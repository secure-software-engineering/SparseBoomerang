package boomerang.scope.soot;

import boomerang.scope.CallGraph;
import boomerang.scope.DataFlowScope;
import boomerang.scope.Field;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import boomerang.scope.StaticFieldVal;
import boomerang.scope.Val;
import boomerang.scope.soot.jimple.JimpleField;
import boomerang.scope.soot.jimple.JimpleMethod;
import boomerang.scope.soot.jimple.JimpleStaticFieldVal;
import boomerang.scope.soot.jimple.JimpleVal;
import java.util.Collection;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import soot.Scene;
import soot.SootMethod;
import soot.jimple.IntConstant;

public class SootFrameworkScope implements FrameworkScope {

  protected final Scene scene;
  protected final SootCallGraph sootCallGraph;
  protected DataFlowScope dataFlowScope;

  public SootFrameworkScope(
      Scene scene,
      soot.jimple.toolkits.callgraph.CallGraph callGraph,
      Collection<SootMethod> entryPoints,
      DataFlowScope dataFlowScope) {
    this.scene = scene;

    this.sootCallGraph = new SootCallGraph(callGraph, entryPoints);
    this.dataFlowScope = dataFlowScope;
  }

  @Override
  public Val getTrueValue(Method m) {
    return new JimpleVal(IntConstant.v(1), m);
  }

  @Override
  public Val getFalseValue(Method m) {
    return new JimpleVal(IntConstant.v(0), m);
  }

  @Override
  public Stream<Method> handleStaticFieldInitializers(Val fact) {
    JimpleStaticFieldVal val = ((JimpleStaticFieldVal) fact);
    return ((JimpleField) val.field())
        .getSootField().getDeclaringClass().getMethods().stream()
            .filter(SootMethod::hasActiveBody)
            .map(JimpleMethod::of);
  }

  @Override
  public StaticFieldVal newStaticFieldVal(Field field, Method m) {
    return new JimpleStaticFieldVal((JimpleField) field, m);
  }

  @Nonnull
  @Override
  public Method resolveMethod(String signatureStr) {
    return JimpleMethod.of(scene.getMethod(signatureStr));
  }

  @Override
  public CallGraph getCallGraph() {
    return sootCallGraph;
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return dataFlowScope;
  }

  @Override
  public void updateDataFlowScope(DataFlowScope dataFlowScope) {
    this.dataFlowScope = dataFlowScope;
  }

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    return SootDataFlowScopeUtil.excludeComplex();
  }
}
