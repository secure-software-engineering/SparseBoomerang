package boomerang.scene.opal

import boomerang.scene.{ControlFlowGraph, Method, Pair, Type, Val}
import org.opalj.br.ReferenceType
import org.opalj.tac.{ArrayLength, Expr, InstanceOf, PrimitiveTypecastExpr, Var}

class OpalVal[+V <: Var[V]](delegate: Expr[V], method: Method, edge: ControlFlowGraph.Edge = null) extends Val(method, edge) {
  
  override def getType: Type = ???

  override def isStatic: Boolean = false

  override def isNewExpr: Boolean = delegate.isNew

  override def getNewExprType: Type = {
    if (isNewExpr) {
      return new OpalType(delegate.asNew.tpe)
    }

    throw new RuntimeException("Value is not a new expression")
  }

  override def asUnbalanced(stmt: ControlFlowGraph.Edge): Val = new OpalVal[V](delegate, method, stmt)

  override def isLocal: Boolean = delegate.isVar

  override def isArrayAllocationVal: Boolean = delegate.isNewArray

  override def isNull: Boolean = delegate.isNullExpr

  override def isStringConstant: Boolean = delegate.isStringConst

  override def getStringValue: String = {
    if (isStringConstant) {
      return delegate.asStringConst.value
    }

    throw new RuntimeException("Value is not a String constant")
  }

  override def isStringBufferOrBuilder: Boolean = {
    val thisType = getType

    thisType.toString.equals("java.lang.String") || thisType.toString.equals("java.lang.StringBuilder") || thisType.toString.equals("java.lang.StringBuffer")
  }

  override def isThrowableAllocationType: Boolean = {
    val thisType = getType

    if (!thisType.isRefType) {
      return false
    }

    val opalType = thisType.asInstanceOf[OpalType].getDelegate
    OpalClient.getClassHierarchy.isSubtypeOf(opalType.asReferenceType, ReferenceType("java/lang/Throwable"))
  }

  override def isCast: Boolean = delegate.astID == PrimitiveTypecastExpr.ASTID

  override def getCastOp: Val = {
    if (isCast) {
      return new OpalVal[V](delegate.asPrimitiveTypeCastExpr.operand, method)
    }

    throw new RuntimeException("Expression is not a cast expression")
  }

  override def isArrayRef: Boolean = ???

  override def isInstanceOfExpr: Boolean = delegate.astID == InstanceOf.ASTID

  override def getInstanceOfOp: Val = {
    if (isInstanceOfExpr) {
      return new OpalVal[V](delegate.asInstanceOf.value, method)
    }

    throw new RuntimeException("Expression is not an instanceOf expression")
  }

  override def isLengthExpr: Boolean = delegate.astID == ArrayLength.ASTID

  override def getLengthOp: Val = {
    if (isLengthExpr) {
      return new OpalVal[V](delegate.asArrayLength, method)
    }

    throw new RuntimeException("Value is not a length expression")
  }

  override def isIntConstant: Boolean = delegate.isIntConst

  override def isClassConstant: Boolean = delegate.isClassConst

  override def getClassConstantType: Type = {
    if (isClassConstant) {
      return new OpalType(delegate.asClassConst.value)
    }

    throw new RuntimeException("Value is not a class constant")
  }

  override def withNewMethod(callee: Method): Val = new OpalVal[V](delegate, callee)

  override def withSecondVal(secondVal: Val) = new OpalDoubleVal[V](delegate, method, secondVal)

  override def isLongConstant: Boolean = delegate.isLongConst

  override def getIntValue: Int = {
    if (isIntConstant) {
      return delegate.asIntConst.value
    }

    throw new RuntimeException("Value is not an integer constant")
  }

  override def getLongValue: Long = {
    if (isLongConstant) {
      return delegate.asLongConst.value
    }

    throw new RuntimeException("Value is not a long constant")
  }

  override def getArrayBase: Pair[Val, Integer] = ???

  override def getVariableName: String = delegate.toString

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalVal[_] => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toString

  def getDelegate: Expr[V] = delegate
}
