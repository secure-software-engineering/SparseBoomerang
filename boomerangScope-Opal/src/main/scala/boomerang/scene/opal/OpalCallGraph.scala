package boomerang.scene.opal

import boomerang.scene.CallGraph.Edge
import org.opalj.br.{DefinedMethod, Method, MultipleDefinedMethods, VirtualDeclaredMethod}
import org.opalj.tac.cg.CallGraph

class OpalCallGraph(callGraph: CallGraph, entryPoints: Set[Method]) extends boomerang.scene.CallGraph {

  callGraph.reachableMethods().foreach(method => {
    method.method match {
      case definedMethod: DefinedMethod => addEdgesFromMethod(definedMethod)
      // TODO Should this case be considered?
      // case definedMethods: MultipleDefinedMethods =>
      //   definedMethods.foreachDefinedMethod(m => addEdgesFromMethod(m))
      case _ =>
    }
  })

  private def addEdgesFromMethod(method: DefinedMethod): Unit = {
    // TODO move TAC to parameters or use method wrappers with TAC
    val tacCode = OpalClient.getTacForMethod(method.definedMethod)

    tacCode.stmts.foreach(stmt => {
      val srcStatement = new OpalStatement(stmt, OpalMethod(method.definedMethod))

      if (srcStatement.containsInvokeExpr()) {
        val callees = callGraph.directCalleesOf(method, stmt.pc)

        callees.foreach(callee => {
          callee.method match {
            case definedMethod: DefinedMethod =>
              val targetMethod = OpalMethod(definedMethod.definedMethod)

              addEdge(new Edge(srcStatement, targetMethod))
            case virtualMethod: VirtualDeclaredMethod =>
              val targetMethod = OpalPhantomMethod(virtualMethod.declaringClassType, virtualMethod.name, virtualMethod.descriptor, srcStatement.getInvokeExpr.isStaticInvokeExpr)

              addEdge(new Edge(srcStatement, targetMethod))
            case definedMethods: MultipleDefinedMethods =>
              definedMethods.foreachDefinedMethod(method => {
                val targetMethod = OpalMethod(method)

                addEdge(new Edge(srcStatement, targetMethod))
              })
          }
        })
      }
    })
  }

  entryPoints.foreach(entryPoint => {
    if (entryPoint.body.isDefined) {
      addEntryPoint(OpalMethod(entryPoint))
    }
  })
}
