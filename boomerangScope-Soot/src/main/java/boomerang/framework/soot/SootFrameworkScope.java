package boomerang.framework.soot;

import boomerang.framework.soot.jimple.*;
import boomerang.scene.*;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import soot.*;
import soot.jimple.IntConstant;

public class SootFrameworkScope implements FrameworkScope {

  @Nonnull private final SootCallGraph sootCallGraph;

  public SootFrameworkScope() {
    sootCallGraph = new SootCallGraph();
    getEntrypoints().forEach(sootCallGraph::addEntryPoint);
  }

  @Override
  public List<Method> getEntrypoints() {
    return Scene.v().getEntryPoints().stream().map(JimpleMethod::of).collect(Collectors.toList());
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
  public Method getMethod(String signatureStr) {
    return JimpleMethod.of(Scene.v().getMethod(signatureStr));
  }

  @Override
  public CallGraph getCallGraph() {
    return sootCallGraph;
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return SootDataFlowScopeUtil.make(Scene.v());
  }

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    return SootDataFlowScopeUtil.excludeComplex();
  }
}
