package boomerang.scene.opal
import boomerang.scene.{AllocVal, Type, Val, WrappedClass}
import org.opalj.br.ObjectType

class OpalType(delegate: org.opalj.br.Type) extends Type {

  override def isNullType: Boolean = false

  override def isRefType: Boolean = delegate.isReferenceType

  override def isArrayType: Boolean = delegate.isArrayType

  override def getArrayBaseType: Type = new OpalType(delegate.asArrayType)

  override def getWrappedClass: WrappedClass = new OpalWrappedClass(OpalClient.getClassFileForType(delegate.asObjectType))

  override def doesCastFail(targetValType: Type, target: Val): Boolean = {
    if (!isRefType || !targetValType.isRefType) {
      return false
    }

    val sourceType = delegate.asReferenceType
    val targetType = targetValType.asInstanceOf[OpalType].getDelegate.asReferenceType

    target match {
      case allocVal: AllocVal if allocVal.getAllocVal.isNewExpr => OpalClient.getClassHierarchy.isSubtypeOf(sourceType, targetType)

      case _ => OpalClient.getClassHierarchy.isSubtypeOf(sourceType, targetType)
    }
  }

  override def isSubtypeOf(otherType: String): Boolean = OpalClient.getClassHierarchy.isSubtypeOf(delegate.asObjectType, ObjectType(otherType))

  override def isBooleanType: Boolean = delegate.isBooleanType

  override def hashCode(): Int = 31 * delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalType => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toJava

  def getDelegate: org.opalj.br.Type = delegate
}
