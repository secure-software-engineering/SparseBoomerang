package boomerang.soot;

import boomerang.callgraph.ObservableICFG;
import boomerang.controlflowgraph.ObservableControlFlowGraph;
import boomerang.controlflowgraph.PredecessorListener;
import boomerang.scene.*;
import boomerang.solver.BackwardBoomerangSolver;
import boomerang.soot.jimple.JimpleField;
import boomerang.soot.jimple.JimpleMethod;
import boomerang.soot.jimple.JimpleStaticFieldVal;
import boomerang.soot.jimple.JimpleVal;
import soot.SootMethod;
import soot.jimple.IntConstant;
import sync.pds.solver.nodes.Node;

import java.util.stream.Stream;

public class SootFrameworkFactory implements ScopeFactory {


    @Override
    public Val getTrueValue(Method m) {
        return new JimpleVal(IntConstant.v(1), m);
    }

    @Override
    public Val getFalseValue(Method m) {
        return  new JimpleVal(IntConstant.v(0), m);
    }

    @Override
    public Stream<Method> handleStaticFieldInitializers(Val fact) {
        JimpleStaticFieldVal val = ((JimpleStaticFieldVal) fact);
        return ((JimpleField) val.field()).getSootField().getDeclaringClass().getMethods().stream().filter(SootMethod::hasActiveBody).map(JimpleMethod::of);
    }

    @Override
    public StaticFieldVal newStaticFieldVal(Field field, Method m) {
        return new JimpleStaticFieldVal( (JimpleField) field, m);
    }


}
