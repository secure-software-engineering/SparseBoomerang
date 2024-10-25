package boomerang.scene.opal

import boomerang.scene.Field

case class OpalField(delegate: org.opalj.br.Field) extends Field {

  override def isInnerClassField: Boolean = delegate.name.contains("$")

  override def toString: String = delegate.toJava
}
