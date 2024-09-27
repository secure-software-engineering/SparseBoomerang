package boomerang.scene.opal

import boomerang.scene.CallGraph.Edge
import org.opalj.br.Method
import org.opalj.tac.cg.CallGraph

class OpalCallGraph(callGraph: CallGraph, entryPoints: Set[Method]) extends boomerang.scene.CallGraph {

  for (reachableMethod <- callGraph.reachableMethods()) {
    if (reachableMethod.method.hasSingleDefinedMethod) {
      val method = reachableMethod.method.definedMethod

      if (method.body.isDefined) {
        val tacCode = OpalClient.getTacForMethod(method)
        val calleeMap = callGraph.calleesOf(reachableMethod.method).toMap

        for (stmt <- tacCode.stmts) {
          val srcStatement = new OpalStatement(stmt, new OpalMethod(reachableMethod.method.definedMethod))

          if (srcStatement.containsInvokeExpr()) {
            val callees = calleeMap(stmt.pc)

            for (callee <- callees) {
              if (callee.method.hasSingleDefinedMethod) {
                val target = callee.method

                if (target.definedMethod.body.isDefined) {
                  val targetMethod = new OpalMethod(target.definedMethod)

                  val edge = new Edge(srcStatement, targetMethod)
                  addEdge(edge)
                  // TODO Test for multiple edges
                  println("Added edge " + srcStatement + " -> " + targetMethod)
                }
              }
            }
          }
        }
      }
    }
  }

  for (entryPoint <- entryPoints) {
    if (entryPoint.body.isDefined) {
      val method = new OpalMethod(entryPoint)

      addEntryPoint(method)
    }
  }
}
