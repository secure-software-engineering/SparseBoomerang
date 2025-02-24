package typestate.tests;

import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.KeyStoreStateMachine;
import typestate.targets.KeyStoreLong;

import java.security.KeyStoreException;

public class KeyStoreLongTest extends IDEALTestingFramework {

    private final String target = KeyStoreLong.class.getName();

    @Override
    protected TypeStateMachineWeightFunctions getStateMachine() {
        return new KeyStoreStateMachine();
    }

    @Test
    public void test1() {
        analyze(target, testName.getMethodName(), 1, 1);
    }

    @Test
    public void test2() throws KeyStoreException {
        analyze(target, testName.getMethodName(), 1, 1);
    }

    @Test
    public void test3() {
        analyze(target, testName.getMethodName(), 1, 1);
    }

    @Test
    public void test4() {
        analyze(target, testName.getMethodName(), 2, 1);
    }

    @Test
    public void catchClause() {
        analyze(target, testName.getMethodName(), 1, 1);
    }
}
