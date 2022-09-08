package boomerang.controlflowgraph;

import boomerang.ForwardQuery;
import boomerang.scene.ControlFlowGraph;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleMethod;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.sparse.SparseAliasingCFG;
import boomerang.scene.sparse.SparseCFGCache;
import boomerang.solver.ForwardBoomerangSolver;
import java.util.Collection;
import soot.SootMethod;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.Node;
import wpds.interfaces.State;

/**
 * To replace the anonymous impl in ForwardSolver, so that we can access the Edge fiel of the outer
 * method
 */
public class ForwardSolverSuccessorListener extends SuccessorListener {

  private final ForwardQuery query;
  private final ControlFlowGraph.Edge curr;
  private final Val value;
  private final Method method;
  private final Node<ControlFlowGraph.Edge, Val> node;
  private final org.slf4j.Logger
      LOGGER; // doesn't look good but this class also shouldn't exist alone
  private final ForwardBoomerangSolver owner;

  public ForwardSolverSuccessorListener(
      ControlFlowGraph.Edge curr,
      ForwardQuery query,
      Val value,
      Method method,
      Node<ControlFlowGraph.Edge, Val> node,
      org.slf4j.Logger LOGGER,
      ForwardBoomerangSolver owner) {
    super(curr.getTarget());
    this.query = query;
    this.curr = curr;
    this.value = value;
    this.method = method;
    this.node = node;
    this.LOGGER = LOGGER;
    this.owner = owner;
  }

  public ControlFlowGraph.Edge getEdge() {
    return curr;
  }

  @Override
  public void getSuccessor(Statement succ) {
    if (query.getType().isNullType()
        && curr.getStart().isIfStmt()
        && curr.getStart().killAtIfStmt(value, succ)) {
      return;
    }

    if (!method.getLocals().contains(value) && !value.isStatic()) {
      return;
    }
    if (curr.getTarget().containsInvokeExpr()
        && (curr.getTarget().isParameter(value) || value.isStatic())) {
      owner.callFlow(
          method,
          node,
          new ControlFlowGraph.Edge(curr.getTarget(), succ),
          curr.getTarget().getInvokeExpr());
    } else {
      owner.checkForFieldOverwrite(curr, value);
      // TODO compute for the actual next stmt, but propagate to sparse next stmt.
      /*    if (value.toString().contains("main")) {
        // SparseAliasingCFG sparseCFG = getSparseCFG(method, curr.getStart(), value);
        // Set<Unit> successors =
        //    sparseCFG.getGraph().successors(SootAdapter.asStmt(curr.getTarget()));
        // Unit next = successors.iterator().next();
        Collection<State> out =
            owner.computeNormalFlow(
                method, new ControlFlowGraph.Edge(curr.getTarget(), succ), value);
        for (State s : out) {
          LOGGER.trace("{}: {} -> {}", s, node, owner.getQuery());
          owner.propagate(node, s);
        }
      } else {*/
      Collection<State> out =
          owner.computeNormalFlow(method, new ControlFlowGraph.Edge(curr.getTarget(), succ), value);
      //      if (method.getName().contains("main")) {
      //        for (State s : out) {
      //          if (s instanceof Node) {
      //            Node node = (Node) s;
      //            Val fact = (Val) node.fact();
      //            SparseAliasingCFG sparseCFG = getSparseCFG(method, curr.getTarget(), fact);
      //            List<Unit> nextUses =
      // sparseCFG.getNextUses(SootAdapter.asStmt(curr.getTarget()));
      //            for (Unit nextUse : nextUses) {
      //              Statement start = curr.getTarget();
      //              Statement target = SootAdapter.asStatement(nextUse, method);
      //              ControlFlowGraph.Edge newEdge = new ControlFlowGraph.Edge(start, target);
      //              State state = new Node<ControlFlowGraph.Edge, Val>(newEdge, fact);
      //              owner.propagate(node, state);
      //            }
      //          } else {
      //            LOGGER.trace("{}: {} -> {}", s, node, owner.getQuery());
      //            owner.propagate(node, s);
      //          }
      //        }
      //      } else {
      for (State s : out) {
        LOGGER.trace("{}: {} -> {}", s, node, owner.getQuery());
        owner.propagate(node, s);
      }
      //      }
      // }
    }
  }

  private SparseAliasingCFG getSparseCFG(Method method, Statement stmt, Val currentVal) {
    SootMethod sootMethod = ((JimpleMethod) method).getDelegate();
    Stmt sootStmt = ((JimpleStatement) stmt).getDelegate();
    SparseCFGCache sparseCFGCache =
        SparseCFGCache.getInstance(owner.getOptions().getSparsificationStrategy(), true);
    SparseAliasingCFG sparseCFG =
        sparseCFGCache.getSparseCFGForForwardPropagation(sootMethod, sootStmt, currentVal);
    return sparseCFG;
  }
}
