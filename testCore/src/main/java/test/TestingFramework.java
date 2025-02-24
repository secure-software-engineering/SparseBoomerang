package test;

import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import org.junit.Assert;
import test.setup.SootTestSetup;
import test.setup.TestSetup;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

public class TestingFramework {

    private final TestSetup testSetup;

    protected TestingFramework() {
        this.testSetup = createTestSetup();
    }

    private TestSetup createTestSetup() {
        // TODO Parameterize
        return new SootTestSetup();
    }

    public FrameworkScope getFrameworkScope(String targetClassName, String targetMethodName) {
        return getFrameworkScope(targetClassName, targetMethodName, DataFlowScope.EXCLUDE_PHANTOM_CLASSES);
    }

    public FrameworkScope getFrameworkScope(String targetClassName, String targetMethodName, DataFlowScope dataFlowScope) {
        String classPath = buildClassPath();
        testSetup.initialize(classPath, targetClassName, targetMethodName, getIncludedPackages(), getExcludedPackages());

        return testSetup.createFrameworkScope(dataFlowScope);
    }

    public Method getTestMethod() {
        Method testMethod = testSetup.getTestMethod();

        if (testMethod == null) {
            throw new IllegalStateException("Test method not initialized. Call 'getFrameworkScope' first");
        }

        return testMethod;
    }

    public void assertResults(Collection<Assertion> assertions) {
        assertResults(assertions, true);
    }

    public void assertResults(Collection<Assertion> assertions, boolean failOnImprecise) {
        Collection<Assertion> unsound = new HashSet<>();
        Collection<Assertion> imprecise = new HashSet<>();

        for (Assertion r : assertions) {
            if (r.isUnsound()) {
                unsound.add(r);
            }
        }

        for (Assertion r : assertions) {
            if (r.isImprecise()) {
                imprecise.add(r);
            }
        }

        if (!unsound.isEmpty()) {
            Assert.fail("Unsound results:\n- " + unsound.stream().map(Assertion::getAssertedMessage).collect(Collectors.joining("\n- ")));
        }

        if (!imprecise.isEmpty() && failOnImprecise) {
            Assert.fail("Imprecise results:\n- " + imprecise.stream().map(Assertion::getAssertedMessage).collect(Collectors.joining("\n- ")));
        }
    }

    protected String buildClassPath() {
        String userDir = System.getProperty("user.dir");
        String javaHome = System.getProperty("java.home");
        if (javaHome == null || javaHome.isEmpty()) {
            throw new RuntimeException("Could not get property java.home!");
        }

        return userDir + "/target/test-classes";
    }

    protected List<String> getIncludedPackages() {
        return Collections.emptyList();
    }

    protected List<String> getExcludedPackages() {
        return Collections.emptyList();
    }
}
