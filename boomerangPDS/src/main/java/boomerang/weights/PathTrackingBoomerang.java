/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
package boomerang.weights;

import boomerang.BoomerangOptions;
import boomerang.ForwardQuery;
import boomerang.WeightedBoomerang;
import boomerang.scene.*;
import boomerang.scene.ControlFlowGraph.Edge;
import sync.pds.solver.OneWeightFunctions;
import sync.pds.solver.WeightFunctions;

public abstract class PathTrackingBoomerang extends WeightedBoomerang<DataFlowPathWeight> {

  private OneWeightFunctions<Edge, Val, Field, DataFlowPathWeight> fieldWeights;
  private PathTrackingWeightFunctions callWeights;

  public PathTrackingBoomerang(CallGraph cg, DataFlowScope scope, FrameworkScope frameworkScope) {
    super(cg, scope, frameworkScope);
  }

  public PathTrackingBoomerang(
      CallGraph cg, DataFlowScope scope, BoomerangOptions opt, FrameworkScope frameworkScope) {
    super(cg, scope, opt, frameworkScope);
  }

  @Override
  protected WeightFunctions<Edge, Val, Field, DataFlowPathWeight> getForwardFieldWeights() {
    return getOrCreateFieldWeights();
  }

  @Override
  protected WeightFunctions<Edge, Val, Field, DataFlowPathWeight> getBackwardFieldWeights() {
    return getOrCreateFieldWeights();
  }

  @Override
  protected WeightFunctions<Edge, Val, Edge, DataFlowPathWeight> getBackwardCallWeights() {
    return getOrCreateCallWeights();
  }

  @Override
  protected WeightFunctions<Edge, Val, Edge, DataFlowPathWeight> getForwardCallWeights(
      ForwardQuery sourceQuery) {
    return getOrCreateCallWeights();
  }

  private WeightFunctions<Edge, Val, Field, DataFlowPathWeight> getOrCreateFieldWeights() {
    if (fieldWeights == null) {
      fieldWeights = new OneWeightFunctions<>(DataFlowPathWeight.one());
    }
    return fieldWeights;
  }

  private WeightFunctions<Edge, Val, Edge, DataFlowPathWeight> getOrCreateCallWeights() {
    if (callWeights == null) {
      callWeights =
          new PathTrackingWeightFunctions(
              options.trackDataFlowPath(),
              options.trackPathConditions(),
              options.trackImplicitFlows());
    }
    return callWeights;
  }
}
