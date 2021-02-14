package boomerang.callgraph;

public interface CallerListener<N, M> {

  M getObservedCallee();

  void onCallerAdded(N callSite, M callee);
}
