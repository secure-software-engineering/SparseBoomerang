package boomerang.scene.opal

import boomerang.scene.{Method, Type, WrappedClass}
import org.opalj.br.ClassFile

import java.util

class OpalWrappedClass(delegate: ClassFile) extends WrappedClass {

  override def getMethods: util.Set[Method] = {
    val methods = new util.HashSet[Method]

    for (method <- delegate.methods) {
      if (method.body.isDefined) {
        methods.add(new OpalMethod(method))
      }
    }

    methods
  }

  override def hasSuperclass: Boolean = delegate.superclassType.isDefined

  override def getSuperclass: WrappedClass = {
    val superClass = OpalClient.getClassFileForType(delegate.superclassType.get)

    new OpalWrappedClass(superClass)
  }

  override def getType: Type = new OpalType(delegate.thisType)

  override def isApplicationClass: Boolean = OpalClient.isApplicationClass(delegate)

  override def getFullyQualifiedName: String = delegate.classSignature.get.toString()

  override def getName: String = delegate.classSignature.get.toString()

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalWrappedClass => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toString()

  override def getDelegate: AnyRef = delegate
}
