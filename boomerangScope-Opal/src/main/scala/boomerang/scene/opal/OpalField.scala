package boomerang.scene.opal

import boomerang.scene.Field

class OpalField(delegate: org.opalj.br.Field) extends Field {

  override def isInnerClassField: Boolean = delegate.name.contains("$")

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalField => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toJava

  def getDelegate: org.opalj.br.Field = delegate

}
