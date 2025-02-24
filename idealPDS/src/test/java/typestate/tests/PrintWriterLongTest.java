package typestate.tests;

import org.junit.Test;
import test.IDEALTestingFramework;
import typestate.finiteautomata.TypeStateMachineWeightFunctions;
import typestate.impl.statemachines.PrintWriterStateMachine;
import typestate.targets.PrintWriterLong;

import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class PrintWriterLongTest extends IDEALTestingFramework {

    private final String target = PrintWriterLong.class.getName();

    @Override
    protected List<String> getIncludedPackages() {
        return List.of("java.io.PrintWriter");
    }

    @Override
    protected TypeStateMachineWeightFunctions getStateMachine() {
        return new PrintWriterStateMachine();
    }

    @Test
    public void test1() throws FileNotFoundException {
        analyze(target, testName.getMethodName(), 1, 1);
    }
}
