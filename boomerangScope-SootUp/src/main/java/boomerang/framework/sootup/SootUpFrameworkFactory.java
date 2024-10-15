package boomerang.framework.sootup;

import boomerang.scene.*;
import java.util.stream.Stream;
import javax.annotation.Nonnull;
import sootup.core.types.ClassType;
import sootup.java.core.views.JavaView;

public class SootUpFrameworkFactory implements FrameworkScopeFactory {

  @Nonnull private final JavaView view;

  public SootUpFrameworkFactory(@Nonnull JavaView view) {
    this.view = view;
  }

  @Override
  @Nonnull
  public Val getTrueValue(Method m) {
    return new JimpleUpVal(
        sootup.core.jimple.common.constant.IntConstant.getInstance(1), (JimpleUpMethod) m);
  }

  @Override
  @Nonnull
  public Val getFalseValue(Method m) {
    return new JimpleUpVal(
        sootup.core.jimple.common.constant.IntConstant.getInstance(0), (JimpleUpMethod) m);
  }

  @Override
  @Nonnull
  public Stream<Method> handleStaticFieldInitializers(Val fact) {
    JimpleUpStaticFieldVal val = ((JimpleUpStaticFieldVal) fact);
    ClassType declaringClassType =
        ((JimpleUpField) val.field()).getDelegate().getDeclaringClassType();

    return view.getClass(declaringClassType).get().getMethods().stream()
        .filter(sootup.core.model.SootMethod::hasBody)
        .map(JimpleUpMethod::of);
  }

  @Override
  @Nonnull
  public StaticFieldVal newStaticFieldVal(Field field, Method m) {
    return new JimpleUpStaticFieldVal((JimpleUpField) field, m);
  }
}
