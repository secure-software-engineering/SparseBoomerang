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

import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Pair;
import boomerang.scene.StaticFieldVal;
import boomerang.scene.Type;
import boomerang.scene.Val;
import javafx.scene.Scene;
import sootup.core.IdentifierFactory;
import sootup.core.jimple.basic.Local;
import sootup.core.jimple.basic.Value;
import sootup.core.jimple.common.constant.*;
import sootup.core.jimple.common.expr.*;
import sootup.core.jimple.common.ref.JArrayRef;
import sootup.core.jimple.common.ref.JInstanceFieldRef;
import sootup.core.jimple.common.ref.JStaticFieldRef;
import sootup.core.typehierarchy.TypeHierarchy;
import sootup.core.typehierarchy.ViewTypeHierarchy;
import sootup.core.types.ClassType;
import sootup.core.types.NullType;
import sootup.java.core.JavaIdentifierFactory;
import sootup.java.core.JavaProject;

public class JimpleVal extends Val {
  private final Value v;

  public JimpleVal(Value v, Method m) {
    this(v, m, null);
  }

  protected JimpleVal(Value v, Method m, Edge unbalanced) {
    super(m, unbalanced);
    if (v == null) throw new RuntimeException("Value must not be null!");
    this.v = v;
  }

  public JimpleType getType() {
    return v == null ? new JimpleType(NullType.getInstance()) : new JimpleType(v.getType());
  }

  public Method m() {
    return m;
  }

  @Override
  public String toString() {
    return v.toString()
        + " ("
        + m.getDeclaringClass()
        + "."
        + m
        + ")"
        + (isUnbalanced() ? " unbalanaced " + unbalancedStmt : "");
  }

  public boolean isStatic() {
    return false;
  }

  public boolean isNewExpr() {
    return v instanceof JNewExpr;
  }

  public Type getNewExprType() {
    return new JimpleType(((JNewExpr) v).getType());
  }

  public Val asUnbalanced(Edge stmt) {
    return new JimpleVal(v, m, stmt);
  }

  public boolean isLocal() {
    return v instanceof Local;
  }

  public boolean isArrayAllocationVal() {
    if (v instanceof JNewArrayExpr) {
      JNewArrayExpr expr = (JNewArrayExpr) v;
      // TODO Performance issue?!
      //            return expr.getBaseType() instanceof RefType;
      return true;
    } else if (v instanceof JNewMultiArrayExpr) {
      return true;
    }
    return false;
  }

  public boolean isNull() {
    return v instanceof NullConstant;
  }

  public boolean isStringConstant() {
    return v instanceof StringConstant;
  }

  public String getStringValue() {
    return ((StringConstant) v).getValue();
  }

  public boolean isStringBufferOrBuilder() {
    Type type = getType();
    return type.toString().equals("java.lang.String")
        || type.toString().equals("java.lang.StringBuilder")
        || type.toString().equals("java.lang.StringBuffer");
  }

  public boolean isThrowableAllocationType() {
    IdentifierFactory factory = JavaIdentifierFactory.getInstance();
    ClassType classType = factory.getClassType("java.lang.Throwable");
    JavaProject project = new JavaProject();
    TypeHierarchy hierarchy = new ViewTypeHierarchy(View);
    return Scene.v()
        .getOrMakeFastHierarchy()
        .canStoreType(getType().getDelegate(), Scene.v().getType("java.lang.Throwable"));
  }

  public boolean isCast() {
    return v instanceof JCastExpr;
  }

  public Val getCastOp() {
    JCastExpr cast = (JCastExpr) v;
    return new JimpleVal(cast.getOp(), m);
  }

  public boolean isInstanceFieldRef() {
    return v instanceof JInstanceFieldRef;
  }

  public boolean isStaticFieldRef() {
    return v instanceof JStaticFieldRef;
  }

  public StaticFieldVal getStaticField() {
    JStaticFieldRef val = (JStaticFieldRef) v;
    return new JimpleStaticFieldVal(new JimpleField(val.getFieldSignature()), m);
  }

  public boolean isArrayRef() {
    return v instanceof JArrayRef;
  }

  @Override
  public Pair<Val, Integer> getArrayBase() {
    return new Pair<>(
        new JimpleVal(((JArrayRef) v).getBase(), m),
        ((JArrayRef) v).getIndex() instanceof IntConstant
            ? ((IntConstant) ((JArrayRef) v).getIndex()).getValue()
            : -1);
  }

  public boolean isInstanceOfExpr() {
    return v instanceof JInstanceOfExpr;
  }

  public Val getInstanceOfOp() {
    JInstanceOfExpr val = (JInstanceOfExpr) v;
    return new JimpleVal(val.getOp(), m);
  }

  public boolean isLengthExpr() {
    return v instanceof JLengthExpr;
  }

  public Val getLengthOp() {
    JLengthExpr val = (JLengthExpr) v;
    return new JimpleVal(val.getOp(), m);
  }

  public boolean isIntConstant() {
    return v instanceof IntConstant;
  }

  public int getIntValue() {
    return ((IntConstant) v).getValue();
  }

  public boolean isLongConstant() {
    return v instanceof LongConstant;
  }

  public long getLongValue() {
    return ((LongConstant) v).getValue();
  }

  public boolean isClassConstant() {
    return v instanceof ClassConstant;
  }

  public Type getClassConstantType() {
    return new JimpleType(((ClassConstant) v).getType());
  }

  @Override
  public Val withNewMethod(Method callee) {
    throw new RuntimeException("Only allowed for static fields");
  }

  @Override
  public Val withSecondVal(Val leftOp) {
    return new JimpleDoubleVal(v, m, leftOp);
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = super.hashCode();
    result = prime * result + ((v == null) ? 0 : v.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (!super.equals(obj)) return false;
    if (getClass() != obj.getClass()) return false;
    JimpleVal other = (JimpleVal) obj;
    if (v == null) {
      if (other.v != null) return false;
    } else if (!v.equals(other.v)) return false;
    return true;
  }

  public Value getDelegate() {
    return v;
  }

  @Override
  public String getVariableName() {
    return v.toString();
  }
}
