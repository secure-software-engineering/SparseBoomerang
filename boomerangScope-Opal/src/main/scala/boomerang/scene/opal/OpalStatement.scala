package boomerang.scene.opal

import boomerang.scene.{Field, IfStatement, InvokeExpr, Pair, Statement, StaticFieldVal, Val}
import org.opalj.tac.{PrimitiveTypecastExpr, Stmt, Var}

import java.util

class OpalStatement[+V <: Var[V]](delegate: Stmt[V], m: OpalMethod) extends Statement(m) {

  override def containsStaticFieldAccess(): Boolean = isStaticFieldLoad || isStaticFieldStore

  override def containsInvokeExpr(): Boolean = isAssign && delegate.asAssignment.expr.isFunctionCall || delegate.isMethodCall

  override def getWrittenField: Field = {
    if (isFieldStore) {
      val resolvedField = OpalClient.resolveFieldStore(delegate.asPutField)
      return new OpalField(resolvedField.get)
    }

    if (isStaticFieldStore) {
      val resolvedField = OpalClient.resolveFieldStore(delegate.asPutStatic)
      return new OpalField(resolvedField.get)
    }

    if (isArrayStore) {
      // TODO
      //val resolvedField = OpalClient.resolveFieldStore(delegate.asArrayStore)
      //return new OpalField(resolvedField.get)
    }

    throw new RuntimeException("Statement is not a field store operation")
  }

  override def isFieldWriteWithBase(base: Val): Boolean = {
    if (isAssign && isFieldStore) {
      return getFieldStore.getX.equals(base)
    }

    if (isAssign && isArrayStore) {
      return getArrayBase.getX.equals(base)
    }

    false
  }

  override def getLoadedField: Field = {
    // TODO Also array?
    if (isFieldLoad) {
      val resolvedField = OpalClient.resolveFieldLoad(delegate.asAssignment.expr.asFieldRead)
      return new OpalField(resolvedField.get)
    }

    throw new RuntimeException("Statement is not a field load operation")
  }

  override def isFieldLoadWithBase(base: Val): Boolean = {
    // TODO Also array?
    if (isAssign && isFieldLoad) {
      return getFieldLoad.getX.equals(base)
    }

    false
  }

  override def isAssign: Boolean = delegate.isAssignment

  override def getLeftOp: Val = {
    if (isAssign) {
      return new OpalVal[V](delegate.asAssignment.targetVar, m)
    }

    throw new RuntimeException("Statement is not an assignment")
  }

  override def getRightOp: Val = {
    if (isAssign) {
      return new OpalVal[V](delegate.asAssignment.expr, m)
    }

    throw new RuntimeException("Statement is not an assignment")
  }

  override def isInstanceOfStatement(fact: Val): Boolean = {
    if (isAssign) {
      if (getRightOp.isInstanceOfExpr) {
        val insOf = getRightOp.getInstanceOfOp

        return insOf.equals(fact)
      }
    }

    false
  }

  override def isCast: Boolean = {
    // Primitive type casts
    if (isAssign) {
      val assignExpr = delegate.asAssignment.expr

      if (assignExpr.astID == PrimitiveTypecastExpr.ASTID) {
        return true
      }
    }

    // Class casts
    delegate.isCheckcast
  }

  override def isPhiStatement: Boolean = false

  override def getInvokeExpr: InvokeExpr = {
    if (containsInvokeExpr()) {
      if (isAssign) {
        return new OpalFunctionInvokeExpr[V](delegate.asAssignment.expr.asFunctionCall, m)
      } else {
        return new OpalMethodInvokeExpr[V](delegate.asMethodCall, m)
      }
    }

    throw new RuntimeException("Statement does not contain an invoke expression")
  }

  override def isReturnStmt: Boolean = delegate.isReturnValue

  override def isThrowStmt: Boolean = delegate.isThrow

  override def isIfStmt: Boolean = delegate.isIf

  override def getIfStmt: IfStatement = {
    if (isIfStmt) {
      return new OpalIfStatement[V](delegate.asIf, m)
    }

    throw new RuntimeException("Statement is not an if-statement")
  }

  override def getReturnOp: Val = {
    if (isReturnStmt) {
      return new OpalVal[V](delegate.asReturnValue.expr, m)
    }

    throw new RuntimeException("Statement is not a return statement")
  }

  override def isMultiArrayAllocation: Boolean = ???

  override def isStringAllocation: Boolean = {
    if (isAssign) {
      return delegate.asAssignment.expr.isStringConst
    }

    throw new RuntimeException("Statement is not an allocation statement")
  }

  override def isFieldStore: Boolean = {
    if (!delegate.isPutField) {
      return false
    }

    val field = OpalClient.resolveFieldStore(delegate.asPutField)
    field.isDefined
  }

  override def isArrayStore: Boolean = delegate.isArrayStore

  override def isArrayLoad: Boolean = {
    if (!isAssign) {
      return false
    }

    delegate.asAssignment.expr.isArrayLoad
  }

  override def isFieldLoad: Boolean = {
    if (!delegate.isAssignment) {
      return false
    }

    if (!delegate.asAssignment.expr.isGetField) {
      return false
    }

    val field = OpalClient.resolveFieldLoad(delegate.asAssignment.expr.asGetField)
    field.isDefined
  }

  override def isIdentityStmt: Boolean = false

  override def getFieldStore: Pair[Val, Field] = {
    if (isFieldStore) {
      val resolvedField = OpalClient.resolveFieldStore(delegate.asPutField).get
      val ref = delegate.asPutField.objRef

      return new Pair(new OpalVal[V](ref, m), new OpalField(resolvedField))
    }

    throw new RuntimeException("Statement is not a field store operation")
  }

  override def getFieldLoad: Pair[Val, Field] = {
    if (isFieldLoad) {
      val resolvedField = OpalClient.resolveFieldLoad(delegate.asAssignment.expr.asFieldRead).get
      val ref = delegate.asAssignment.expr.asGetField.objRef

      return new Pair(new OpalVal[V](ref, m), new OpalField(resolvedField))
    }

    throw new RuntimeException("Statement is not a field load operation")
  }

  override def isStaticFieldLoad: Boolean = {
    if (!delegate.isAssignment) {
      return false
    }

    if (!delegate.asAssignment.expr.isGetStatic) {
      return false
    }

    val field = OpalClient.resolveFieldLoad(delegate.asAssignment.expr.asGetStatic)
    field.isDefined
  }

  override def isStaticFieldStore: Boolean = {
    if (!delegate.isPutStatic) {
      return false
    }

    val field = OpalClient.resolveFieldStore(delegate.asPutStatic)
    field.isDefined
  }

  override def getStaticField: StaticFieldVal = {
    if (isStaticFieldLoad) {
      val resolvedField = OpalClient.resolveFieldLoad(delegate.asAssignment.expr.asGetStatic)
      val staticField = new OpalField(resolvedField.get)

      return new OpalStaticFieldVal(staticField, m)
    }

    if (isStaticFieldStore) {
      val resolvedField = OpalClient.resolveFieldStore(delegate.asPutStatic)
      val staticField = new OpalField(resolvedField.get)

      return new OpalStaticFieldVal(staticField, m)
    }

    throw new RuntimeException("Statement is neither a static field load or store operation")
  }

  override def killAtIfStmt(fact: Val, successor: Statement): Boolean = false

  override def getPhiVals: util.Collection[Val] = throw new RuntimeException("Not supported")

  override def getArrayBase: Pair[Val, Integer] = {
    if (isArrayLoad) {
      val rightOp = getRightOp
      return rightOp.getArrayBase
    }

    if (isArrayLoad) {
      // TODO
    }

    throw new RuntimeException("Statement has no array base")
  }

  override def getStartLineNumber: Int = m.getDelegate.body.get.lineNumber(delegate.pc).getOrElse(-1)

  override def getStartColumnNumber: Int = getStartLineNumber()

  override def getEndLineNumber: Int = getStartLineNumber()

  override def getEndColumnNumber: Int = getStartLineNumber()

  override def isCatchStmt: Boolean = delegate.isCaughtException

  override def hashCode(): Int = 31 + delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalStatement[_] => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toString

  def getDelegate: Stmt[V] = delegate
}
