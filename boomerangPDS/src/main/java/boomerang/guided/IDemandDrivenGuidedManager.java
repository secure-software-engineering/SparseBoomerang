package boomerang.guided;

import boomerang.BackwardQuery;
import boomerang.Boomerang;
import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.Query;
import boomerang.QueryGraph;
import boomerang.guided.Specification.Parameter;
import boomerang.guided.Specification.QueryDirection;
import boomerang.guided.Specification.QuerySelector;
import boomerang.guided.Specification.SootMethodWithSelector;
import boomerang.results.AbstractBoomerangResults.Context;
import boomerang.results.BackwardBoomerangResults;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.AllocVal;
import boomerang.scene.ControlFlowGraph.Edge;
import boomerang.scene.DataFlowScope;
import boomerang.scene.Method;
import boomerang.scene.SootDataFlowScope;
import boomerang.scene.Statement;
import boomerang.scene.Val;
import boomerang.scene.jimple.JimpleStatement;
import boomerang.scene.jimple.SootCallGraph;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.collect.Table;
import com.google.common.collect.Table.Cell;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import soot.Scene;
import soot.jimple.Stmt;
import sync.pds.solver.nodes.Node;
import wpds.impl.Weight.NoWeight;

public interface IDemandDrivenGuidedManager {
  Collection<Query> onForwardFlow(ForwardQuery query, Edge dataFlowEdge, Val dataFlowVal);
  Collection<Query> onBackwardFlow(BackwardQuery query, Edge dataFlowEdge, Val dataFlowVal);
}
