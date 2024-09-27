package boomerang.scene.opal

import boomerang.scene.{ControlFlowGraph, Statement}
import com.google.common.collect.{HashMultimap, Multimap}

import java.util

class OpalControlFlowGraph(method: OpalMethod) extends ControlFlowGraph {

  private var cacheBuilt = false

  private val startPointCache: util.List[Statement] = new util.ArrayList[Statement]
  private val endPointCache: util.List[Statement] = new util.ArrayList[Statement]
  private val predsOfCache: Multimap[Statement, Statement] = HashMultimap.create()
  private val succsOfCache: Multimap[Statement, Statement] = HashMultimap.create()
  private val statements: util.List[Statement] = new util.ArrayList[Statement]()

  override def getStartPoints: util.Collection[Statement] = {
    buildCache()
    startPointCache
  }

  override def getEndPoints: util.Collection[Statement] = {
    buildCache()
    endPointCache
  }

  override def getSuccsOf(curr: Statement): util.Collection[Statement] = {
    buildCache()
    succsOfCache.get(curr)
  }

  override def getPredsOf(curr: Statement): util.Collection[Statement] = {
    buildCache()
    predsOfCache.get(curr)
  }

  override def getStatements: util.List[Statement] = {
    buildCache()
    statements
  }

  private def buildCache(): Unit = {
    if (cacheBuilt) return

    val tac = OpalClient.getTacForMethod(method.getDelegate)

    val headPc = tac.cfg.startBlock.startPC
    val head = tac.stmts(headPc)
    val headStatement = new OpalStatement(head, method)
    startPointCache.add(headStatement)

    // TODO Deal with tails
    val tail = tac.cfg.normalReturnNode.predecessors.tail

    for (stmt <- tac.stmts) {
      val statement = new OpalStatement(stmt, method)
      statements.add(statement)

      val stmtPc = tac.pcToIndex(stmt.pc)
      for (successorPc <- tac.cfg.successors(stmtPc)) {
        val successor = tac.stmts(successorPc)
        val successorStatement = new OpalStatement(successor, method)

        succsOfCache.put(statement, successorStatement)
      }

      for (predecessorPc <- tac.cfg.predecessors(stmtPc)) {
        val predecessor = tac.stmts(predecessorPc)
        val predecessorStatement = new OpalStatement(predecessor, method)

        predsOfCache.put(statement, predecessorStatement)
      }
    }

    cacheBuilt = true
  }
}
