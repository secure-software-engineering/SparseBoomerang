package boomerang.scene.opal

import boomerang.scene.{IfStatement, Statement, Val}
import org.opalj.tac.{DUVar, If, Var}
import org.opalj.value.ValueInformation

class OpalIfStatement[+V <: Var[V]](delegate: If[V], method: OpalMethod) extends IfStatement {

  override def getTarget: Statement = {
    val tac = OpalClient.getTacForMethod(method.getDelegate)
    val target = delegate.targetStmt

    new OpalStatement[DUVar[ValueInformation]](tac.stmts(target), method)
  }

  override def evaluate(otherVal: Val): IfStatement.Evaluation = IfStatement.Evaluation.UNKOWN

  override def uses(otherVal: Val): Boolean = {
    val left = new OpalVal(delegate.left, method)
    val right = new OpalVal(delegate.right, method)

    otherVal.equals(left) || otherVal.equals(right)
  }

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalStatement[_] => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toString()

  def getDelegate: If[V] = delegate

}
