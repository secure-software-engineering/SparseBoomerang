package boomerang.scene.opal

import boomerang.scene.{ControlFlowGraph, Method, Pair, Type, Val}
import org.opalj.br.{ObjectType, ReferenceType}
import org.opalj.tac.{ArrayLength, Const, DUVar, DVar, DefSites, Expr, FunctionCall, InstanceOf, IntConst, LongConst, New, PrimitiveTypecastExpr, StringConst, UVar}
import org.opalj.value.ValueInformation

class OpalVal(val delegate: Expr[DUVar[ValueInformation]], method: OpalMethod, unbalanced: ControlFlowGraph.Edge = null) extends Val(method, unbalanced) {
  
  override def getType: Type = delegate match {
    case local : UVar[ValueInformation] =>
      if (local.value.isReferenceValue) {
        OpalType(local.value.asReferenceValue.asReferenceType, local.value.asReferenceValue.isNull.isYes)
      } else if (local.value.isPrimitiveValue) {
        OpalType(local.value.asPrimitiveValue.primitiveType)
      } else if (local.value.isVoid) {
        OpalType(ObjectType.Void)
      } else if (local.value.isArrayValue.isYes) {
        OpalType(ObjectType.Array)
      } else {
        throw new RuntimeException("Could not determine type " + local.value)
      }
    case local: DUVar[ValueInformation] =>
      if (local.value.isReferenceValue) {
        OpalType(local.value.asReferenceValue.asReferenceType, local.value.asReferenceValue.isNull.isYes)
      } else if (local.value.isPrimitiveValue) {
        OpalType(local.value.asPrimitiveValue.primitiveType)
      } else if (local.value.isVoid) {
        OpalType(ObjectType.Void)
      } else if (local.value.isArrayValue.isYes) {
        OpalType(ObjectType.Array)
      } else {
        throw new RuntimeException("Could not determine type " + local.value)
      }
    case const: Const => OpalType(const.tpe)
    case newExpr: New => OpalType(newExpr.tpe)
    case functionCall: FunctionCall[_] => OpalType(functionCall.descriptor.returnType)
    case _ => throw new RuntimeException("Type not implemented yet")
  }

  override def isStatic: Boolean = false

  override def isNewExpr: Boolean = delegate.isNew

  override def getNewExprType: Type = {
    if (isNewExpr) {
      return OpalType(delegate.asNew.tpe)
    }

    throw new RuntimeException("Value is not a new expression")
  }

  override def asUnbalanced(stmt: ControlFlowGraph.Edge): Val = new OpalVal(delegate, method, stmt)

  override def isLocal: Boolean = delegate.isVar

  override def isArrayAllocationVal: Boolean = delegate.isNewArray

  // TODO Deal with multiple arrays
  override def getArrayAllocationSize: Val = {
    if (isArrayAllocationVal) {
      return new OpalVal(delegate.asNewArray.counts.head, method)
    }

    throw new RuntimeException("Value is not an array allocation expression")
  }

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

    thisType.toString.equals("java/lang/String") || thisType.toString.equals("java/lang/StringBuilder") || thisType.toString.equals("java/lang/StringBuffer")
  }

  override def isThrowableAllocationType: Boolean = {
    val thisType = getType

    if (!thisType.isRefType) {
      return false
    }

    val opalType = thisType.asInstanceOf[OpalType].delegate
    OpalClient.getClassHierarchy.isSubtypeOf(opalType.asReferenceType, ReferenceType("java/lang/Throwable"))
  }

  override def isCast: Boolean = delegate.astID == PrimitiveTypecastExpr.ASTID

  override def getCastOp: Val = {
    if (isCast) {
      return new OpalVal(delegate.asPrimitiveTypeCastExpr.operand, method)
    }

    throw new RuntimeException("Expression is not a cast expression")
  }

  override def isArrayRef: Boolean = delegate match {
    case ref: UVar[ValueInformation] => ref.value.isArrayValue.isYes
    case _ => false
  }

  override def isInstanceOfExpr: Boolean = delegate.astID == InstanceOf.ASTID

  override def getInstanceOfOp: Val = {
    if (isInstanceOfExpr) {
      return new OpalVal(delegate.asInstanceOf.value, method)
    }

    throw new RuntimeException("Expression is not an instanceOf expression")
  }

  override def isLengthExpr: Boolean = delegate.astID == ArrayLength.ASTID

  override def getLengthOp: Val = {
    if (isLengthExpr) {
      return new OpalVal(delegate.asArrayLength, method)
    }

    throw new RuntimeException("Value is not a length expression")
  }

  override def isIntConstant: Boolean = delegate.isIntConst

  override def isClassConstant: Boolean = delegate.isClassConst

  override def getClassConstantType: Type = {
    if (isClassConstant) {
      return OpalType(delegate.asClassConst.value)
    }

    throw new RuntimeException("Value is not a class constant")
  }

  override def withNewMethod(callee: Method): Val = new OpalVal(delegate, callee.asInstanceOf[OpalMethod])

  override def withSecondVal(secondVal: Val) = new OpalDoubleVal(delegate, method, secondVal)

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

  override def getArrayBase: Pair[Val, Integer] = {
    if (isArrayRef) {
      // TODO
    }

    throw new RuntimeException("Value is not an array ref")
  }

  override def getVariableName: String = delegate.toString

  override def hashCode(): Int = delegate match {
    case uVar: UVar[_] =>
      // UVars should reference their DVar to keep the comparisons consistent
      val tac = OpalClient.getTacForMethod(method.delegate)
      val defStmt = tac.stmts(uVar.definedBy.head)

      if (!defStmt.isAssignment) {
        return 31 * super.hashCode() + delegate.hashCode()
      }

      val targetVar = defStmt.asAssignment.targetVar
      31 * super.hashCode() + targetVar.hashCode()
    case _ => 31 * super.hashCode() + delegate.hashCode()
  }

  private def canEqual(a: Any): Boolean = a.isInstanceOf[OpalVal]

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalVal =>
      // DVar does not implement a proper equals() method, so we have to compare the hash codes
      this.delegate match {
        case _: DVar[_] if other.delegate.isInstanceOf[DVar[_]] =>
          super.equals(other) && this.delegate.hashCode() == other.delegate.hashCode()
        case uVar: UVar[_] if other.delegate.isInstanceOf[DVar[_]] =>
          val tac = OpalClient.getTacForMethod(method.delegate)
          val defStmt = tac.stmts(uVar.definedBy.head)

          if (!defStmt.isAssignment) {
            return false
          }

          val targetVar = defStmt.asAssignment.targetVar
          val otherVar = other.delegate.asInstanceOf[DVar[ValueInformation]]
          super.equals(other) && targetVar.hashCode() == otherVar.hashCode()
        case dVar: DVar[_] if other.delegate.isInstanceOf[UVar[_]] =>
          val otherVar = other.delegate.asInstanceOf[UVar[ValueInformation]]
          val tac = OpalClient.getTacForMethod(method.delegate)
          val defStmt = tac.stmts(otherVar.definedBy.head)

          if (!defStmt.isAssignment) {
            return false
          }

          val targetVar = defStmt.asAssignment.targetVar
          super.equals(other) && dVar.hashCode() == targetVar.hashCode()
        case _ =>
          other.canEqual(this) && super.equals(other) && this.delegate == other.delegate
      }
    case _ => false
  }

  override def toString: String = {
    delegate match {
      case _: DVar[_] => "var" // TODO Add origin
      case uVar: UVar[_] => "var" + uVar.definedBy.head
      case stringConst: StringConst => "\"" + stringConst.value + "\""
      case intConst: IntConst => intConst.value.toString
      case longConst: LongConst => longConst.value.toString
      case _ => delegate.toString
    }
  }
}
