package boomerang.scene.jimple;

import boomerang.scene.IfStatement;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.IntConstant;
import sootup.core.jimple.common.constant.NullConstant;
import sootup.core.jimple.common.expr.AbstractConditionExpr;
import sootup.core.jimple.common.expr.JEqExpr;
import sootup.core.jimple.common.expr.JNeExpr;
import sootup.core.jimple.common.stmt.JIfStmt;

public class JimpleIfStatement implements IfStatement {

  private JIfStmt delegate;
  private Method method;

  public JimpleIfStatement(JIfStmt delegate, Method method) {
    this.delegate = delegate;
    this.method = method;
  }

  @Override
  public Statement getTarget() {
    JimpleMethod jm = (JimpleMethod) method;
    // If should have a single target
    return JimpleStatement.create(
        delegate.getTargetStmts(jm.getDelegate().getBody()).get(0), method);
  }

  @Override
  public Evaluation evaluate(Val val) {
    if (delegate.getCondition() instanceof JEqExpr) {
      JEqExpr eqExpr = (JEqExpr) delegate.getCondition();
      Value op1 = eqExpr.getOp1();
      Value op2 = eqExpr.getOp2();
      if ((val.equals(new JimpleVal(op1, method)) && op2.equals(NullConstant.getInstance())
          || (val.equals(new JimpleVal(op2, method)) && op2.equals(NullConstant.getInstance())))) {
        return Evaluation.TRUE;
      }
      if ((val.equals(new JimpleVal(IntConstant.getInstance(0), method))
              && op2.equals(IntConstant.getInstance(0))
          || (val.equals(new JimpleVal(IntConstant.getInstance(1), method))
              && op2.equals(IntConstant.getInstance(1))))) {
        return Evaluation.TRUE;
      }
      if ((val.equals(new JimpleVal(IntConstant.getInstance(1), method))
              && op2.equals(IntConstant.getInstance(0))
          || (val.equals(new JimpleVal(IntConstant.getInstance(0), method))
              && op2.equals(IntConstant.getInstance(1))))) {
        return Evaluation.FALSE;
      }
    }

    if (delegate.getCondition() instanceof JNeExpr) {
      JNeExpr eqExpr = (JNeExpr) delegate.getCondition();
      Value op1 = eqExpr.getOp1();
      Value op2 = eqExpr.getOp2();
      if ((val.equals(new JimpleVal(op1, method)) && op2.equals(NullConstant.getInstance())
          || (val.equals(new JimpleVal(op2, method)) && op2.equals(NullConstant.getInstance())))) {
        return Evaluation.FALSE;
      }
      if ((val.equals(new JimpleVal(IntConstant.getInstance(0), method))
              && op2.equals(IntConstant.getInstance(0))
          || (val.equals(new JimpleVal(IntConstant.getInstance(1), method))
              && op2.equals(IntConstant.getInstance(1))))) {
        return Evaluation.FALSE;
      }
      if ((val.equals(new JimpleVal(IntConstant.getInstance(1), method))
              && op2.equals(IntConstant.getInstance(0))
          || (val.equals(new JimpleVal(IntConstant.getInstance(0), method))
              && op2.equals(IntConstant.getInstance(1))))) {
        return Evaluation.TRUE;
      }
    }
    return Evaluation.UNKOWN;
  }

  @Override
  public boolean uses(Val val) {
    if (delegate.getCondition() instanceof AbstractConditionExpr) {
      AbstractConditionExpr c = delegate.getCondition();
      Value op1 = c.getOp1();
      Value op2 = c.getOp2();
      return val.equals(new JimpleVal(op1, method)) || val.equals(new JimpleVal(op2, method));
    }
    return false;
  }
}
