package boomerang.framework.soot;

import boomerang.framework.soot.jimple.JimpleField;
import boomerang.framework.soot.jimple.JimpleMethod;
import boomerang.framework.soot.jimple.JimpleStaticFieldVal;
import boomerang.framework.soot.jimple.JimpleVal;
import boomerang.scene.*;
import java.util.stream.Stream;
import soot.SootMethod;
import soot.jimple.IntConstant;

public class SootFrameworkFactory implements FrameworkScopeFactory {

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
}
