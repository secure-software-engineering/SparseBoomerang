package boomerang.framework.soot;

import boomerang.framework.soot.jimple.*;
import boomerang.scene.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import soot.*;
import soot.jimple.IntConstant;

public class SootFrameworkScope implements FrameworkScope {

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
  public CallGraph buildCallGraph() {
    SootCallGraph sootCallGraph = new SootCallGraph();
    getEntrypoints().forEach(sootCallGraph::addEntryPoint);
    return sootCallGraph;
  }

  @Override
  public DataFlowScope getDataFlowScope() {
    return SootDataFlowScopeUtil.make(Scene.v());
  }

  @Override
  public void executeFramework() {

    SceneTransformer sceneTransformer =
        new SceneTransformer() {
          protected void internalTransform(
              String phaseName, @SuppressWarnings("rawtypes") Map options) {
            BoomerangPretransformer.v().reset();
            BoomerangPretransformer.v().apply();
          }
        };

    Transform transform =
        new Transform("wjtp.ifds", sceneTransformer); // TODO: maybe adapt the phaseName
    PackManager.v()
        .getPack("wjtp")
        .add(transform); // whole programm, jimple, user-defined transformations

    PackManager.v().getPack("cg").apply(); // call graph package
    PackManager.v().getPack("wjtp").apply();
  }

  @Override
  public DataFlowScope createDataFlowScopeWithoutComplex() {
    return SootDataFlowScopeUtil.excludeComplex(Scene.v());
  }
}
