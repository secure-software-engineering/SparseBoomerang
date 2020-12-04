package boomerang.flowfunction;

import boomerang.ForwardQuery;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Method;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Collection;
import wpds.interfaces.State;

public interface IForwardFlowFunction {
  Collection<Val> returnFlow(Method callee, Statement returnStmt, Val returnedVal);

  Collection<Val> callFlow(Statement callSite, Val factAtCallSite, Method callee);

  Collection<State> normalFlow(ForwardQuery query, Edge edge, Val fact);

  Collection<State> callToReturn(ForwardQuery query, Edge edge, Val fact);
}
