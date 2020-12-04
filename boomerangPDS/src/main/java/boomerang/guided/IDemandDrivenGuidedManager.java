package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.Val;
import java.util.Collection;

public interface IDemandDrivenGuidedManager {
  Collection<Query> onForwardFlow(ForwardQuery query, Edge dataFlowEdge, Val dataFlowVal);

  Collection<Query> onBackwardFlow(BackwardQuery query, Edge dataFlowEdge, Val dataFlowVal);
}
