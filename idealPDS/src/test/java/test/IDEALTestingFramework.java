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
package test;

import boomerang.WeightedForwardQuery;
import boomerang.debugger.Debugger;
import boomerang.options.BoomerangOptions;
import boomerang.results.ForwardBoomerangResults;
import boomerang.scene.*;
import boomerang.scene.CallGraph.Edge;
import boomerang.solver.Strategies;
import com.google.common.collect.Lists;
import ideal.*;
import java.util.*;
import java.util.Map.Entry;
import sync.pds.solver.WeightFunctions;
import test.ExpectedResults.InternalState;
import test.core.selfrunning.ImprecisionException;
import typestate.TransitionFunction;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;

public abstract class IDEALTestingFramework extends AbstractTestingFramework {
  private static final boolean FAIL_ON_IMPRECISE = false;
  protected StoreIDEALResultHandler<TransitionFunction> resultHandler =
      new StoreIDEALResultHandler<>();

  @Override
  protected void initializeWithEntryPoint() {
    frameworkScope =
        FrameworkScopeFactory.init(
            buildClassPath(),
            getTestCaseClassName(),
            testMethodName.getMethodName(),
            getIncludedPackagesList(),
            getExludedPackageList());
  }

  @Override
  protected void analyze() {
    analyze(
        frameworkScope.resolveMethod(
            "<" + getTestCaseClassName() + ": void " + testMethodName.getMethodName() + "()>"));
  }

  protected abstract TypeStateMachineWeightFunctions getStateMachine();

  protected IDEALAnalysis<TransitionFunction> createAnalysis() {
    return new IDEALAnalysis<>(
        new IDEALAnalysisDefinition<TransitionFunction>() {

          @Override
          public Collection<WeightedForwardQuery<TransitionFunction>> generate(
              ControlFlowGraph.Edge stmt) {
            return IDEALTestingFramework.this.getStateMachine().generateSeed(stmt);
          }

          @Override
          public WeightFunctions<
                  ControlFlowGraph.Edge, Val, ControlFlowGraph.Edge, TransitionFunction>
              weightFunctions() {
            return IDEALTestingFramework.this.getStateMachine();
          }

          @Override
          public Debugger<TransitionFunction> debugger(IDEALSeedSolver<TransitionFunction> solver) {
            return
            /*
             * VISUALIZATION ? new IDEVizDebugger<>(new File(
             * ideVizFile.getAbsolutePath().replace(".json", " " + solver.getSeed() + ".json")),
             * callGraph) :
             */
            new Debugger<>();
          }

          @Override
          public IDEALResultHandler<TransitionFunction> getResultHandler() {
            return resultHandler;
          }

          @Override
          public BoomerangOptions boomerangOptions() {
            return BoomerangOptions.builder()
                .withStaticFieldStrategy(Strategies.StaticFieldStrategy.FLOW_SENSITIVE)
                .enableAllowMultipleQueries(true)
                .build();
          }

          @Override
          public FrameworkScope getFrameworkFactory() {
            return frameworkScope;
          }
        });
  }

  protected void analyze(Method m) {
    Set<Assertion> expectedResults = parseExpectedQueryResults(m);
    TestingResultReporter testingResultReporter = new TestingResultReporter(expectedResults);

    Map<WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>>
        seedToSolvers = executeAnalysis();
    for (Entry<
            WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>>
        e : seedToSolvers.entrySet()) {
      testingResultReporter.onSeedFinished(e.getKey().asNode(), e.getValue());
    }
    List<Assertion> unsound = Lists.newLinkedList();
    List<Assertion> imprecise = Lists.newLinkedList();
    for (Assertion r : expectedResults) {
      if (r instanceof ShouldNotBeAnalyzed) {
        throw new RuntimeException(r.toString());
      }
    }
    for (Assertion r : expectedResults) {
      if (!r.isSatisfied()) {
        unsound.add(r);
      }
    }
    for (Assertion r : expectedResults) {
      if (r.isImprecise()) {
        imprecise.add(r);
      }
    }
    if (!unsound.isEmpty()) throw new RuntimeException("Unsound results: " + unsound);
    if (!imprecise.isEmpty() && FAIL_ON_IMPRECISE) {
      throw new ImprecisionException("Imprecise results: " + imprecise);
    }
  }

  protected Map<
          WeightedForwardQuery<TransitionFunction>, ForwardBoomerangResults<TransitionFunction>>
      executeAnalysis() {
    IDEALTestingFramework.this.createAnalysis().run();
    return resultHandler.getResults();
  }

  private Set<Assertion> parseExpectedQueryResults(Method sootTestMethod) {
    Set<Assertion> results = new HashSet<>();
    parseExpectedQueryResults(sootTestMethod, results, new HashSet<>());
    return results;
  }

  private void parseExpectedQueryResults(Method m, Set<Assertion> queries, Set<Method> visited) {
    if (visited.contains(m)) return;
    visited.add(m);

    for (Statement stmt : m.getStatements()) {
      if (!(stmt.containsInvokeExpr())) continue;
      for (Edge callSite : frameworkScope.getCallGraph().edgesOutOf(stmt)) {
        parseExpectedQueryResults(callSite.tgt(), queries, visited);
      }
      boomerang.scene.InvokeExpr invokeExpr = stmt.getInvokeExpr();
      String invocationName = invokeExpr.getMethod().getName();
      if (invocationName.equals("shouldNotBeAnalyzed")) {
        queries.add(new ShouldNotBeAnalyzed(stmt));
      }
      if (!invocationName.startsWith("mayBeIn") && !invocationName.startsWith("mustBeIn")) continue;
      Val val = invokeExpr.getArg(0);
      if (invocationName.startsWith("mayBeIn")) {
        if (invocationName.contains("Error"))
          queries.add(new MayBe(stmt, val, InternalState.ERROR));
        else queries.add(new MayBe(stmt, val, InternalState.ACCEPTING));
      } else if (invocationName.startsWith("mustBeIn")) {
        if (invocationName.contains("Error"))
          queries.add(new MustBe(stmt, val, InternalState.ERROR));
        else queries.add(new MustBe(stmt, val, InternalState.ACCEPTING));
      }
    }
  }

  /**
   * The methods parameter describes the variable that a query is issued for. Note: We misuse
   * the @Deprecated annotation to highlight the method in the Code.
   */
  protected static void mayBeInErrorState(Object variable) {}

  protected static void mustBeInErrorState(Object variable) {}

  protected static void mayBeInAcceptingState(Object variable) {}

  protected static void mustBeInAcceptingState(Object variable) {}

  protected static void shouldNotBeAnalyzed() {}

  /**
   * This method can be used in test cases to create branching. It is not optimized away.
   *
   * @return
   */
  protected boolean staticallyUnknown() {
    return true;
  }
}
