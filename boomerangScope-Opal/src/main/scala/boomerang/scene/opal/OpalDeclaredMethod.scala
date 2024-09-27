package boomerang.scene.opal

import boomerang.scene.{DeclaredMethod, InvokeExpr, WrappedClass}
import org.opalj.br.Method
import org.opalj.tac.Var

class OpalDeclaredMethod[+V <: Var[V]](invokeExpr: InvokeExpr, delegate: Method) extends DeclaredMethod(invokeExpr) {

  override def isNative: Boolean = delegate.isNative

  override def getSubSignature: String = delegate.signature.toString

  override def getName: String = delegate.name

  override def isStatic: Boolean = delegate.isStatic

  override def isConstructor: Boolean = delegate.isStatic

  override def getSignature: String = delegate.fullyQualifiedSignature

  override def getDeclaringClass: WrappedClass = {
    val declaredMethod = OpalClient.getDeclaredMethod(delegate)
    new OpalWrappedClass(OpalClient.getClassFileForType(declaredMethod.declaringClassType))
  }

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalDeclaredMethod[_] => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toJava

  def getDelegate: Method = delegate

}
