package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import java.util.Collection;
import java.util.Collections;

public class ArrayContainerCollectionManager implements IDemandDrivenGuidedManager {

  @Override
  public Collection<Query> onForwardFlow(ForwardQuery query, Edge dataFlowEdge, Val dataFlowVal) {
    Statement targetStmt = dataFlowEdge.getStart();
    // Any statement of type someVariable[..] = rightOp
    if (targetStmt.isAssign() && targetStmt.getLeftOp().isArrayRef()) {
      // If propagated fact also matches "someVariable"
      if (targetStmt.getLeftOp().getArrayBase().getX().equals(dataFlowVal)) {
        // Do start a new backward query for rightOp
        return Collections.singleton(BackwardQuery.make(dataFlowEdge, targetStmt.getRightOp()));
      }
    }
    return Collections.emptySet();
  }

  @Override
  public Collection<Query> onBackwardFlow(BackwardQuery query, Edge dataFlowEdge, Val dataFlowVal) {
    return Collections.emptySet();
  }
}
