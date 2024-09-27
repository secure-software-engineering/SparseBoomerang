package boomerang.scene.opal

import boomerang.scene.{Method, Val, ValWithFalseVariable}
import org.opalj.tac.{Expr, Var}

class OpalDoubleVal[+V <: Var[V]](delegate: Expr[V], method: Method, falseVal: Val) extends OpalVal(delegate, method) with ValWithFalseVariable{

  override def getFalseVariable: Val = falseVal

  override def hashCode(): Int = 31 * super.hashCode() + 31 * falseVal.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalDoubleVal[_] => super.equals(other) && falseVal.equals(other.getFalseVariable)
    case _ => false
  }

  override def toString: String = "FalseVal: " + falseVal + " from " + super.toString
}
