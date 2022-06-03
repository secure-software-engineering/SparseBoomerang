package boomerang.scene.sparse;

import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.jimple.JimpleStatement;
import soot.Unit;
import soot.jimple.Stmt;

public class SootAdapter {

    public static Statement asStatement(Unit unit, Method method) {
        return JimpleStatement.create((Stmt) unit, method);
    }

    public static Stmt asStmt(Statement stmt) {
        return ((JimpleStatement) stmt).getDelegate();
    }

}
