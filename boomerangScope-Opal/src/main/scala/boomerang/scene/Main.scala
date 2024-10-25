package boomerang.scene

import boomerang.scene.opal.{OpalCallGraph, OpalClient}
import com.typesafe.config.ConfigValueFactory
import org.opalj.br.analyses.Project
import org.opalj.br.analyses.cg.InitialEntryPointsKey
import org.opalj.log.{DevNullLogger, GlobalLogContext, OPALLogger}
import org.opalj.tac.cg.CHACallGraphKey

import java.io.File

object Main {

  def main(args: Array[String]): Unit = {
    OPALLogger.updateLogger(GlobalLogContext, DevNullLogger)

    val project = Project(new File("C:\\Users\\Sven\\Documents\\CogniCrypt\\Opal\\MultipleCalls\\Main.jar"))
    var config = project.config

    val key = s"${InitialEntryPointsKey.ConfigKeyPrefix}entryPoints"
    val currentValues = project.config.getList(key).unwrapped()
    val allMethods = project.allMethodsWithBody

    allMethods.foreach(method => {
      val configValue = new java.util.HashMap[String, String]
      configValue.put("declaringClass", method.classFile.thisType.toJava)
      configValue.put("name", method.name)

      currentValues.add(ConfigValueFactory.fromMap(configValue))
      config = config.withValue(key, ConfigValueFactory.fromIterable(currentValues))
    })

    config = config.withValue(
      s"${InitialEntryPointsKey.ConfigKeyPrefix}analysis",
      ConfigValueFactory.fromAnyRef("org.opalj.br.analyses.cg.ConfigurationEntryPointsFinder")
    )

    val newProject = Project.recreate(project, config)
    val callGraph = newProject.get(CHACallGraphKey)
    val entryPoints = newProject.get(InitialEntryPointsKey)
    println("Entry Points: " + entryPoints.size)

    callGraph.reachableMethods().foreach(method => {
      println(method.method)
    })

    OpalClient.init(newProject)

    val opalCallGraph = new OpalCallGraph(callGraph, entryPoints.toSet)
    opalCallGraph.getEdges.forEach(edge => {
      println(edge)
    })
  }
}
