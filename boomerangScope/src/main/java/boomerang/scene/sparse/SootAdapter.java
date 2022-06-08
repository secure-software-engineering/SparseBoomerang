package boomerang.scene.sparse;

import boomerang.scene.Field;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.*;
import soot.SootField;
import soot.SootMethod;
import soot.Unit;
import soot.Value;
import soot.jimple.Stmt;

public class SootAdapter {

  public static Statement asStatement(Unit unit, Method method) {
    return JimpleStatement.create((Stmt) unit, method);
  }

  public static Stmt asStmt(Statement stmt) {
    return ((JimpleStatement) stmt).getDelegate();
  }

  public static Value asValue(Val val) {
    return ((JimpleVal) val).getDelegate();
  }

  public static SootField asField(Val val) {
    Field field = ((JimpleStaticFieldVal) val).field();
    return ((JimpleField) field).getSootField();
  }

  public static SootMethod asSootMethod(Method m) {
    return ((JimpleMethod) m).getDelegate();
  }
}
