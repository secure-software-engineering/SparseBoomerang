/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.scene.jimple;

import boomerang.scene.Field;
import boomerang.scene.IfStatement;
import boomerang.scene.InvokeExpr;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.Statement;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Val;
import boomerang.scene.up.SootUpClient;
import com.google.common.base.Joiner;
import java.util.Collection;
import sootup.core.jimple.common.constant.StringConstant;
import sootup.core.jimple.common.expr.AbstractInstanceInvokeExpr;
import sootup.core.jimple.common.expr.JCastExpr;
import sootup.core.jimple.common.expr.JNewExpr;
import sootup.core.jimple.common.expr.JNewMultiArrayExpr;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JCaughtExceptionRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.jimple.common.stmt.*;
import sootup.java.core.JavaSootField;

public class JimpleStatement extends Statement {

  // Wrapper for stmt so we know the method
  private final Stmt delegate;
  private final Method method;

  private JimpleStatement(Stmt delegate, Method m) {
    super(m);
    if (delegate == null) {
      throw new RuntimeException("Invalid, parameter may not be null");
    }
    this.delegate = delegate;
    this.method = m;
  }

  public static Statement create(Stmt delegate, Method m) {
    JimpleStatement jimpleStatement = new JimpleStatement(delegate, m);
    return jimpleStatement;
  }

  @Override
  public String toString() {
    return shortName(delegate);
  }

  private String shortName(Stmt s) {
    if (s.containsInvokeExpr()) {
      String base = "";
      if (s.getInvokeExpr() instanceof AbstractInstanceInvokeExpr) {
        AbstractInstanceInvokeExpr iie = (AbstractInstanceInvokeExpr) s.getInvokeExpr();
        base = iie.getBase().toString() + ".";
      }
      String assign = "";
      if (s instanceof JAssignStmt) {
        assign = ((JAssignStmt) s).getLeftOp() + " = ";
      }
      return assign
          + base
          + s.getInvokeExpr().getMethodSignature().getName()
          + "("
          + Joiner.on(",").join(s.getInvokeExpr().getArgs())
          + ")";
    }
    if (s instanceof JIdentityStmt) {
      return s.toString();
    }
    if (s instanceof JAssignStmt) {
      JAssignStmt assignStmt = (JAssignStmt) s;
      if (assignStmt.getLeftOp() instanceof JInstanceFieldRef) {
        JInstanceFieldRef ifr = (JInstanceFieldRef) assignStmt.getLeftOp();
        return ifr.getBase()
            + "."
            + ifr.getFieldSignature().getName()
            + " = "
            + assignStmt.getRightOp();
      }
      if (assignStmt.getRightOp() instanceof JInstanceFieldRef) {
        JInstanceFieldRef ifr = (JInstanceFieldRef) assignStmt.getRightOp();
        return assignStmt.getLeftOp()
            + " = "
            + ifr.getBase()
            + "."
            + ifr.getFieldSignature().getName();
      }
      if (assignStmt.getRightOp() instanceof JNewExpr) {
        JNewExpr newExpr = (JNewExpr) assignStmt.getRightOp();
        return assignStmt.getLeftOp() + " = new " + newExpr.getType().getClassName();
      }
    }
    return s.toString();
  }

  public boolean containsStaticFieldAccess() {
    if (delegate instanceof JAssignStmt) {
      JAssignStmt assignStmt = (JAssignStmt) delegate;
      return assignStmt.getLeftOp() instanceof JStaticFieldRef
          || assignStmt.getRightOp() instanceof JStaticFieldRef;
    }
    return false;
  }

  public boolean containsInvokeExpr() {
    return delegate.containsInvokeExpr();
  }

  public Field getWrittenField() {
    JAssignStmt as = (JAssignStmt) delegate;
    if (as.getLeftOp() instanceof JStaticFieldRef) {
      JStaticFieldRef staticFieldRef = (JStaticFieldRef) as.getLeftOp();
      JavaSootField sootField = SootUpClient.getInstance().getSootField(staticFieldRef.getFieldSignature());
      return new JimpleField(sootField);
    }

    if (as.getLeftOp() instanceof JArrayRef) {
      return Field.array(getArrayBase().getY());
    }
    JInstanceFieldRef ifr = (JInstanceFieldRef) as.getLeftOp();
    JavaSootField sootField = SootUpClient.getInstance().getSootField(ifr.getFieldSignature());
    return new JimpleField(sootField);
  }

  public boolean isFieldWriteWithBase(Val base) {
    if (isAssign() && isFieldStore()) {
      Pair<Val, Field> instanceFieldRef = getFieldStore();
      return instanceFieldRef.getX().equals(base);
    }
    if (isAssign() && isArrayStore()) {
      Pair<Val, Integer> arrayBase = getArrayBase();
      return arrayBase.getX().equals(base);
    }
    return false;
  }

  public Field getLoadedField() {
    JAssignStmt as = (JAssignStmt) delegate;
    JInstanceFieldRef ifr = (JInstanceFieldRef) as.getRightOp();
    JavaSootField sootField = SootUpClient.getInstance().getSootField(ifr.getFieldSignature());
    return new JimpleField(sootField);
  }

  public boolean isFieldLoadWithBase(Val base) {
    if (isAssign() && isFieldLoad()) {
      return getFieldLoad().getX().equals(base);
    }
    return false;
  }

  @Override
  public boolean isAssign() {
    return delegate instanceof JAssignStmt;
  }

  public Val getLeftOp() {
    assert isAssign();
    JAssignStmt assignStmt = (JAssignStmt) delegate;
    return new JimpleVal(assignStmt.getLeftOp(), method);
  }

  public Val getRightOp() {
    assert isAssign();
    JAssignStmt assignStmt = (JAssignStmt) delegate;
    return new JimpleVal(assignStmt.getRightOp(), method);
  }

  public boolean isInstanceOfStatement(Val fact) {
    if (isAssign()) {
      if (getRightOp().isInstanceOfExpr()) {
        Val instanceOfOp = getRightOp().getInstanceOfOp();
        return instanceOfOp.equals(fact);
      }
    }
    return false;
  }

  public boolean isCast() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getRightOp() instanceof JCastExpr;
  }

  public InvokeExpr getInvokeExpr() {
    return new JimpleInvokeExpr(delegate.getInvokeExpr(), method);
  }

  public boolean isReturnStmt() {
    return delegate instanceof JReturnStmt;
  }

  public boolean isThrowStmt() {
    return delegate instanceof JThrowStmt;
  }

  public boolean isIfStmt() {
    return delegate instanceof JIfStmt;
  }

  public IfStatement getIfStmt() {
    return new JimpleIfStatement((JIfStmt) delegate, method);
  }

  // TODO Rename to getReturnOp();
  public Val getReturnOp() {
    assert isReturnStmt();
    JReturnStmt assignStmt = (JReturnStmt) delegate;
    return new JimpleVal(assignStmt.getOp(), method);
  }

  public boolean isMultiArrayAllocation() {
    return (delegate instanceof JAssignStmt)
        && ((JAssignStmt) delegate).getRightOp() instanceof JNewMultiArrayExpr;
  }

  public boolean isStringAllocation() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getRightOp() instanceof StringConstant;
  }

  public boolean isFieldStore() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getLeftOp() instanceof JInstanceFieldRef;
  }

  public boolean isArrayStore() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getLeftOp() instanceof JArrayRef;
  }

  public boolean isArrayLoad() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getRightOp() instanceof JArrayRef;
  }

  public boolean isFieldLoad() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getRightOp() instanceof JInstanceFieldRef;
  }

  public boolean isIdentityStmt() {
    return delegate instanceof JIdentityStmt;
  }

  public Stmt getDelegate() {
    return delegate;
  }

  public String getShortLabel() {
    if (delegate instanceof JAssignStmt) {
      JAssignStmt assignStmt = (JAssignStmt) delegate;
      if (assignStmt.getRightOp() instanceof JInstanceFieldRef) {
        JInstanceFieldRef fr = (JInstanceFieldRef) assignStmt.getRightOp();
        return assignStmt.getLeftOp()
            + " = "
            + fr.getBase()
            + "."
            + fr.getFieldSignature().getName();
      }
      if (assignStmt.getLeftOp() instanceof JInstanceFieldRef) {
        JInstanceFieldRef fr = (JInstanceFieldRef) assignStmt.getLeftOp();
        return fr.getBase()
            + "."
            + fr.getFieldSignature().getName()
            + " = "
            + assignStmt.getRightOp();
      }
    }
    if (containsInvokeExpr()) {
      InvokeExpr invokeExpr = getInvokeExpr();
      if (invokeExpr.isStaticInvokeExpr()) {
        return (isAssign() ? getLeftOp() + " = " : "")
            + invokeExpr.getMethod()
            + "("
            + invokeExpr.getArgs().toString().replace("[", "").replace("]", "")
            + ")";
      }
      if (invokeExpr.isInstanceInvokeExpr()) {
        return (isAssign() ? getLeftOp() + " = " : "")
            + invokeExpr.getBase()
            + "."
            + invokeExpr.getMethod()
            + "("
            + invokeExpr.getArgs().toString().replace("[", "").replace("]", "")
            + ")";
      }
    }
    return delegate.toString();
  }

  /**
   * This method kills a data-flow at an if-stmt, it is assumed that the propagated "allocation"
   * site is x = null and fact is the propagated aliased variable. (i.e., y after a statement y =
   * x). If the if-stmt checks for if y != null or if y == null, data-flow propagation can be killed
   * when along the true/false branch.
   *
   * @param fact The data-flow value that bypasses the if-stmt
   * @param successor The successor statement of the if-stmt
   * @return true if the Val fact shall be killed
   */
  @Deprecated
  public boolean killAtIfStmt(Val fact, Statement successor) {
    //		IfStmt ifStmt = this.getIfStmt();
    //		if(successor instanceof CallSiteStatement) {
    //          successor = ((CallSiteStatement) successor).getDelegate();
    //		} else if(successor instanceof ReturnSiteStatement) {
    //          successor = ((ReturnSiteStatement) successor).getDelegate();
    //        }
    //		Stmt succ = ((JimpleStatement)successor).getDelegate();
    //		Stmt target = ifStmt.getTarget();
    //
    //		Value condition = ifStmt.getCondition();
    //		if (condition instanceof JEqExpr) {
    //			JEqExpr eqExpr = (JEqExpr) condition;
    //			Value op1 = eqExpr.getOp1();
    //			Value op2 = eqExpr.getOp2();
    //			Val jop1 = new JimpleVal(eqExpr.getOp1(), successor.getMethod());
    //			Val jop2 = new JimpleVal(eqExpr.getOp2(), successor.getMethod());
    //			if (fact instanceof JimpleDoubleVal) {
    //				JimpleDoubleVal valWithFalseVar = (JimpleDoubleVal) fact;
    //				if (jop1.equals(valWithFalseVar.getFalseVariable())) {
    //					if (op2.equals(IntConstant.v(0))) {
    //						if (!succ.equals(target)) {
    //							return true;
    //						}
    //					}
    //				}
    //				if (jop2.equals(valWithFalseVar.getFalseVariable())) {
    //					if (op1.equals(IntConstant.v(0))) {
    //						if (!succ.equals(target)) {
    //							return true;
    //						}
    //					}
    //				}
    //			}
    //			if (op1 instanceof NullConstant) {
    //				if (new JimpleVal(op2,successor.getMethod()).equals(fact)) {
    //					if (!succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			} else if (op2 instanceof NullConstant) {
    //				if (new JimpleVal(op1,successor.getMethod()).equals(fact)) {
    //					if (!succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			}
    //		}
    //		if (condition instanceof JNeExpr) {
    //			JNeExpr eqExpr = (JNeExpr) condition;
    //			Value op1 = eqExpr.getOp1();
    //			Value op2 = eqExpr.getOp2();
    //			if (op1 instanceof NullConstant) {
    //				if (new JimpleVal(op2,successor.getMethod()).equals(fact)) {
    //					if (succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			} else if (op2 instanceof NullConstant) {
    //				if (new JimpleVal(op1,successor.getMethod()).equals(fact)) {
    //					if (succ.equals(target)) {
    //						return true;
    //					}
    //				}
    //			}
    //		}
    return false;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((delegate == null) ? 0 : delegate.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    JimpleStatement other = (JimpleStatement) obj;
    if (delegate == null) {
      return other.delegate == null;
    } else {
      return delegate.equals(other.delegate);
    }
  }

  @Override
  public Pair<Val, Field> getFieldStore() {
    JAssignStmt ins = (JAssignStmt) delegate;
    JInstanceFieldRef val = (JInstanceFieldRef) ins.getLeftOp();
    return new Pair<Val, Field>(
        new JimpleVal(val.getBase(), method),
        new JimpleField(SootUpClient.getInstance().getSootField(val.getFieldSignature())));
  }

  @Override
  public Pair<Val, Field> getFieldLoad() {
    JAssignStmt ins = (JAssignStmt) delegate;
    JInstanceFieldRef val = (JInstanceFieldRef) ins.getRightOp();
    return new Pair<Val, Field>(
        new JimpleVal(val.getBase(), method),
        new JimpleField(SootUpClient.getInstance().getSootField(val.getFieldSignature())));
  }

  @Override
  public boolean isStaticFieldLoad() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getRightOp() instanceof JStaticFieldRef;
  }

  @Override
  public boolean isStaticFieldStore() {
    return delegate instanceof JAssignStmt
        && ((JAssignStmt) delegate).getLeftOp() instanceof JStaticFieldRef;
  }

  @Override
  public StaticFieldVal getStaticField() {
    JStaticFieldRef v;
    if (isStaticFieldLoad()) {
      v = (JStaticFieldRef) ((JAssignStmt) delegate).getRightOp();
    } else if (isStaticFieldStore()) {
      v = (JStaticFieldRef) ((JAssignStmt) delegate).getLeftOp();
    } else {
      throw new RuntimeException("Error");
    }
    return new JimpleStaticFieldVal(
        new JimpleField(SootUpClient.getInstance().getSootField(v.getFieldSignature())), method);
  }

  @Override
  public boolean isPhiStatement() {
    return false;
  }

  @Override
  public Collection<Val> getPhiVals() {
    throw new RuntimeException("Not supported!");
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    if (isArrayLoad()) {
      Val rightOp = getRightOp();
      return rightOp.getArrayBase();
    }
    if (isArrayStore()) {
      Val rightOp = getLeftOp();
      return rightOp.getArrayBase();
    }
    throw new RuntimeException("Dead code");
  }

  @Override
  public int getStartLineNumber() {
    return delegate.getPositionInfo().getStmtPosition().getFirstLine();
  }

  @Override
  public int getStartColumnNumber() {
    return delegate.getPositionInfo().getStmtPosition().getFirstCol();
  }

  @Override
  public int getEndColumnNumber() {
    return delegate.getPositionInfo().getStmtPosition().getLastCol();
  }

  @Override
  public int getEndLineNumber() {
    return delegate.getPositionInfo().getStmtPosition().getLastLine();
  }

  @Override
  public boolean isCatchStmt() {
    return delegate instanceof JIdentityStmt
        && ((JIdentityStmt) delegate).getRightOp() instanceof JCaughtExceptionRef;
  }

  public boolean isUnitializedFieldStatement() {
    System.out.println("isUnitializedFieldStatement not implemented: " + this.toString());
    return false;
    // return delegate.hasTag(BoomerangPretransformer.UNITIALIZED_FIELD_TAG_NAME);
  }
}
