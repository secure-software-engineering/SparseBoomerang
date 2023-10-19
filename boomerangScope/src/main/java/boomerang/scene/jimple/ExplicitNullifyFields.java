package boomerang.scene.jimple;

import boomerang.scene.up.SootUpClient;
import com.google.common.collect.Sets;
import java.util.List;
import java.util.Set;
import sootup.core.jimple.basic.StmtPositionInfo;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.stmt.JAssignStmt;
import sootup.core.jimple.common.stmt.Stmt;
import sootup.core.types.ClassType;
import sootup.core.types.ReferenceType;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootField;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.views.JavaView;

public class ExplicitNullifyFields {
  public static void apply(JavaView view) {
    for (JavaSootClass c : view.getClasses()) {
      for (JavaSootMethod m : c.getMethods()) {
        if (m.hasBody() && m.isConstructor()) {
          apply(view, m);
        }
      }
    }
  }

  private static void apply(JavaView view, JavaSootMethod cons) {
    ClassType declaringClassType = cons.getDeclaringClassType();
    JavaSootClass javaSootClass = view.getClass(declaringClassType).get();
    Set<? extends JavaSootField> fields1 = javaSootClass.getFields();
    List<Stmt> stmts = cons.getBody().getStmts();
    Set<JavaSootField> fieldsDefinedInMethod = getFieldsDefinedInMethod(cons);
    for (JavaSootField f : fields1) {
      if (fieldsDefinedInMethod.contains(f)) continue;
      if (f.isStatic()) continue;
      if (f.isFinal()) continue;
      if (f.getType() instanceof ReferenceType) {
        stmts.add(
            0,
            new JAssignStmt(
                new JInstanceFieldRef(cons.getBody().getThisLocal(), f.getSignature()),
                NullConstant.getInstance(),
                StmtPositionInfo.createNoStmtPositionInfo()));
      }
    }
  }

  private static Set<JavaSootField> getFieldsDefinedInMethod(JavaSootMethod cons) {
    Set<JavaSootField> res = Sets.newHashSet();
    for (Stmt u : cons.getBody().getStmts()) {
      if (u instanceof JAssignStmt) {
        JAssignStmt as = (JAssignStmt) u;
        Value left = as.getLeftOp();
        if (left instanceof JInstanceFieldRef) {
          JInstanceFieldRef ifr = (JInstanceFieldRef) left;
          res.add(SootUpClient.getInstance().getSootField(ifr.getFieldSignature()));
        }
      }
    }
    return res;
  }
}
