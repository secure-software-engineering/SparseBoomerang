package boomerang.scene.opal

import boomerang.scene.{DeclaredMethod, InvokeExpr, Val}
import org.opalj.tac.{FunctionCall, InstanceFunctionCall, InstanceMethodCall, MethodCall, NonVirtualFunctionCall, NonVirtualMethodCall, Var}

import java.util

class OpalMethodInvokeExpr[+V <: Var[V]](delegate: MethodCall[V], method: OpalMethod) extends InvokeExpr {

  override def getArg(index: Int): Val = getArgs.get(index)

  override def getArgs: util.List[Val] = {
    val result = new util.ArrayList[Val]

    for (param <- delegate.params) {
      result.add(new OpalVal[V](param, method))
    }

    result
  }

  override def isInstanceInvokeExpr: Boolean = delegate.isInstanceOf[InstanceMethodCall[_]]

  override def getBase: Val = {
    if (isInstanceInvokeExpr) {
      return new OpalVal[V](delegate.asInstanceMethodCall.receiver, method)
    }

    throw new RuntimeException("Method call is not an instance invoke expression")
  }

  override def getMethod: DeclaredMethod = {
    val resolvedMethod = OpalClient.resolveMethodRef(delegate.declaringClass, delegate.name, delegate.descriptor)

    if (resolvedMethod.isDefined) {
      return new OpalDeclaredMethod[V](this, resolvedMethod.get)
    }

    throw new RuntimeException("Cannot resolve method " + delegate.name + " from invoke expression")
  }

  override def isSpecialInvokeExpr: Boolean = delegate.astID == NonVirtualMethodCall.ASTID

  override def isStaticInvokeExpr: Boolean = delegate.isStaticMethodCall
}

class OpalFunctionInvokeExpr[+V <: Var[V]](delegate: FunctionCall[V], method: OpalMethod) extends InvokeExpr {

  override def getArg(index: Int): Val = getArgs.get(index)

  override def getArgs: util.List[Val] = {
    val result = new util.ArrayList[Val]

    for (param <- delegate.params) {
      result.add(new OpalVal[V](param, method))
    }

    result
  }

  override def isInstanceInvokeExpr: Boolean = delegate.isInstanceOf[InstanceFunctionCall[_]]

  override def getBase: Val = {
    if (isInstanceInvokeExpr) {
      return new OpalVal[V](delegate.asInstanceFunctionCall.receiver, method)
    }

    throw new RuntimeException("Function call is not an instance invoke expression")
  }

  override def getMethod: DeclaredMethod = {
    val resolvedMethod = OpalClient.resolveMethodRef(delegate.declaringClass, delegate.name, delegate.descriptor)

    if (resolvedMethod.isDefined) {
      new OpalDeclaredMethod[V](this, resolvedMethod.get)
    }

    throw new RuntimeException("Cannot resolve method " + delegate.name + " from invoke expression")
  }

  override def isSpecialInvokeExpr: Boolean = delegate.astID == NonVirtualFunctionCall.ASTID

  override def isStaticInvokeExpr: Boolean = delegate.isStaticFunctionCall

}
