package boomerang.scene

import boomerang.scene.opal.{OpalCallGraph, OpalClient}
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.tac.cg.CHACallGraphKey

import scala.jdk.CollectionConverters._
import java.io.File

object Main {

  def main(args: Array[String]): Unit = {
    val project = Project(new File("C:\\Users\\Sven\\Documents\\CogniCrypt\\Opal\\IfStmt\\IfStmt.jar"))
    val callGraph = project.get(CHACallGraphKey)
    val entryPoints = project.get(InitialEntryPointsKey)

    OpalClient.init(project)

    val opalCallGraph = new OpalCallGraph(callGraph, entryPoints.toSet)

    val edges = opalCallGraph.getEdges
    edges.forEach(x => {
      val start = x.src()
      val target = x.tgt()

      if (!target.isStatic) {
        //val thisLocal = target.getThisLocal
      }
      val locals = target.getParameterLocals

      for (stmt <- target.getStatements.asScala) {
        if (stmt.isAssign) {
          println(stmt.getRightOp.isLengthExpr)
        }

        println("--")
      }
    })
  }
}
