package boomerang.scene.opal

import boomerang.scene.{ControlFlowGraph, Method, Statement, Val, WrappedClass}
import org.opalj.tac.{DUVar, InstanceFunctionCall, InstanceMethodCall, UVar}
import org.opalj.value.ValueInformation

import scala.jdk.CollectionConverters._
import java.util

class OpalMethod(delegate: org.opalj.br.Method) extends Method {

  if (delegate.body.isEmpty) {
    throw new RuntimeException("Trying to build Opal method without body present")
  }

  private var localCache: Option[util.Set[Val]] = None
  private var parameterLocalCache: Option[util.List[Val]] = None

  private val cfg = new OpalControlFlowGraph(this)

  override def isStaticInitializer: Boolean = delegate.isStaticInitializer

  override def isParameterLocal(value: Val): Boolean = {
    // if (value.isStatic) return false

    val parameterLocals = getParameterLocals
    parameterLocals.contains(value)
  }

  override def isThisLocal(fact: Val): Boolean = {
    if (isStatic) return false

    val thisLocal = isThisLocalDefined
    if (thisLocal.isDefined) {
      return thisLocal.get.equals(fact)
    }

    // TODO This might not be enough
    false
  }

  override def getThisLocal: Val = {
    if (!isStatic) {
      val thisLocal = isThisLocalDefined
      if (thisLocal.isDefined) {
        return thisLocal.get
      }

      // TODO Replace corresponding places
      throw new RuntimeException("this local is not used in method. Use #isThisLocal for comparisons")
    }

    throw new RuntimeException("Static method does not have a 'this' local")
  }

  private def isThisLocalDefined: Option[Val] = {
    val locals = getLocals
    for (local <- locals.asScala) {
      val opalVal = local.asInstanceOf[OpalVal[DUVar[ValueInformation]]]
      val valDelegate = opalVal.getDelegate

      if (valDelegate.isInstanceOf[UVar[_]]) {
        if (valDelegate.asVar.definedBy.head == -1) {
          return Some(local)
        }
      }
    }

    None
  }

  override def getLocals: util.Set[Val] = {
    if (localCache.isEmpty) {
      localCache = Some(new util.HashSet[Val]())

      val tac = OpalClient.getTacForMethod(delegate)

      for (stmt <- tac.stmts) {
        if (stmt.isMethodCall) {
          // Extract the base
          if (stmt.isInstanceOf[InstanceMethodCall[_]]) {
            val opalVal = new OpalVal[DUVar[ValueInformation]](stmt.asInstanceMethodCall.receiver, this)
            localCache.get.add(opalVal)
          }

          // Parameters of method calls
          for (param <- stmt.asMethodCall.params) {
            if (param.isVar) {
              localCache.get.add(new OpalVal[DUVar[ValueInformation]](param.asVar, this))
            }
          }
        }

        if (stmt.isAssignment) {
          // Target variable
          val targetVar = stmt.asAssignment.targetVar
          localCache.get.add(new OpalVal[DUVar[ValueInformation]](targetVar, this))

          if (stmt.asAssignment.expr.isFunctionCall) {
            // Extract the base
            if (stmt.asAssignment.expr.isInstanceOf[InstanceFunctionCall[_]]) {
              val opalVal = new OpalVal[DUVar[ValueInformation]](stmt.asAssignment.expr.asInstanceFunctionCall.receiver, this)
              localCache.get.add(opalVal)
            }

            // Parameters of function call
            for (param <- stmt.asAssignment.expr.asFunctionCall.params) {
              if (param.isVar) {
                localCache.get.add(new OpalVal[DUVar[ValueInformation]](param.asVar, this))
              }
            }
          }
        }
      }
    }

    localCache.get
  }

  override def getParameterLocals: util.List[Val] = {
    if (parameterLocalCache.isEmpty) {
      parameterLocalCache = Some(new util.ArrayList[Val]())

      val locals = getLocals
      for (local <- locals.asScala) {
        if (local.isLocal) {
          val opalVal = local.asInstanceOf[OpalVal[DUVar[ValueInformation]]]
          val valDelegate = opalVal.getDelegate

          if (valDelegate.isInstanceOf[UVar[_]]) {
            if (valDelegate.asVar.definedBy.head < 0) {
              parameterLocalCache.get.add(local)
            }
          }
        }
      }
    }

    parameterLocalCache.get
  }

  override def isStatic: Boolean = delegate.isStatic

  override def isNative: Boolean = delegate.isNative

  override def getStatements: util.List[Statement] = cfg.getStatements

  override def getDeclaringClass: WrappedClass = {
    val declaredMethod = OpalClient.getDeclaredMethod(delegate)
    new OpalWrappedClass(OpalClient.getClassFileForType(declaredMethod.declaringClassType))
  }

  override def getControlFlowGraph: ControlFlowGraph = cfg

  override def getSubSignature: String = delegate.signature.toString

  override def getName: String = delegate.name

  override def isConstructor: Boolean = delegate.isConstructor

  override def isPublic: Boolean = delegate.isPublic

  override def hashCode(): Int = 31 + delegate.hashCode()

  override def equals(obj: Any): Boolean = obj match {
    case other: OpalMethod => this.delegate == other.getDelegate
    case _ => false
  }

  override def toString: String = delegate.toJava

  def getDelegate: org.opalj.br.Method = delegate

}
