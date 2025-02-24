package test.setup;

import boomerang.scope.DataFlowScope;
import boomerang.scope.FrameworkScope;
import boomerang.scope.Method;
import boomerang.scope.soot.BoomerangPretransformer;
import boomerang.scope.soot.SootFrameworkScope;
import boomerang.scope.soot.jimple.JimpleMethod;
import soot.ArrayType;
import soot.G;
import soot.Local;
import soot.Modifier;
import soot.PackManager;
import soot.RefType;
import soot.Scene;
import soot.SootClass;
import soot.SootMethod;
import soot.Type;
import soot.VoidType;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SootTestSetup implements TestSetup {

    private SootMethod testMethod;

    @Override
    public void initialize(String classPath, String testClassName, String testMethodName, List<String> includedPackages, List<String> excludedPackages) {
        G.reset();

        Options.v().set_whole_program(true);
        Options.v().set_output_format(Options.output_format_none);
        Options.v().set_no_bodies_for_excluded(true);
        Options.v().set_allow_phantom_refs(true);
        Options.v().set_keep_line_number(true);

        Options.v().set_soot_classpath("VIRTUAL_FS_FOR_JDK" + File.pathSeparator + classPath);

        Options.v().setPhaseOption("jb.sils", "enabled:false");
        Options.v().setPhaseOption("jb", "use-original-names:true");

        Options.v().setPhaseOption("cg.cha", "on");
        Options.v().setPhaseOption("cg.cha", "verbose:true");
        Options.v().setPhaseOption("cg.cha", "all-reachable:true");

        Options.v().set_exclude(excludedPackages);
        Options.v().set_include(includedPackages);

        SootClass sootTestCaseClass = Scene.v().forceResolve(testClassName, SootClass.BODIES);
        sootTestCaseClass.setApplicationClass();
        testMethod = sootTestCaseClass.getMethod("void " + testMethodName + "()");

        if (testMethod == null) {
            throw new RuntimeException("Could not load testMethod " + testMethodName);
        }

        //SootClass targetClass = getTargetClass(testClassName, testMethod);
        //targetClass.setApplicationClass();

        //Scene.v().addBasicClass(targetClass.toString(), SootClass.BODIES);
        Scene.v().loadNecessaryClasses();

        //SootMethod methodByName = targetClass.getMethodByName("main");
        List<SootMethod> entryPoints = new ArrayList<>();

        for (SootMethod m : sootTestCaseClass.getMethods()) {
            if (m.isStaticInitializer()) {
                entryPoints.add(m);
            }
        }

        for (SootClass inner : Scene.v().getClasses()) {
            if (inner.getName().contains(sootTestCaseClass.getName())) {
                inner.setApplicationClass();

                for (SootMethod m : inner.getMethods()) {
                    if (m.isStaticInitializer()) {
                        entryPoints.add(m);
                    }
                }
            }
        }

        entryPoints.add(testMethod);
        Scene.v().setEntryPoints(entryPoints);
    }

    @Override
    public Method getTestMethod() {
        return JimpleMethod.of(testMethod);
    }

    @Override
    public FrameworkScope createFrameworkScope(DataFlowScope dataFlowScope) {
        PackManager.v().getPack("cg").apply();

        BoomerangPretransformer.v().reset();
        BoomerangPretransformer.v().apply();

        return new SootFrameworkScope(
                Scene.v(),
                Scene.v().getCallGraph(),
                Scene.v().getEntryPoints(),
                dataFlowScope);
    }

    private SootClass getTargetClass(String className, SootMethod sootTestMethod) {
        SootClass sootClass = new SootClass("dummyClass");
        Type paramType = ArrayType.v(RefType.v("java.lang.String"), 1);
        SootMethod mainMethod =
                new SootMethod(
                        "main",
                        Collections.singletonList(paramType),
                        VoidType.v(),
                        Modifier.PUBLIC | Modifier.STATIC);
        sootClass.addMethod(mainMethod);
        JimpleBody body = Jimple.v().newBody(mainMethod);
        mainMethod.setActiveBody(body);
        RefType testCaseType = RefType.v(className);
        Local loc = Jimple.v().newLocal("l0", paramType);
        body.getLocals().add(loc);
        body.getUnits()
                .add(Jimple.v().newIdentityStmt(loc, Jimple.v().newParameterRef(paramType, 0)));
        Local allocatedTestObj = Jimple.v().newLocal("dummyObj", testCaseType);
        body.getLocals().add(allocatedTestObj);
        body.getUnits()
                .add(
                        Jimple.v()
                                .newAssignStmt(
                                        allocatedTestObj, Jimple.v().newNewExpr(testCaseType)));
        body.getUnits()
                .add(
                        Jimple.v()
                                .newInvokeStmt(
                                        Jimple.v()
                                                .newVirtualInvokeExpr(
                                                        allocatedTestObj,
                                                        sootTestMethod.makeRef())));
        body.getUnits().add(Jimple.v().newReturnVoidStmt());

        Scene.v().addClass(sootClass);
        body.validate();
        return sootClass;
    }
}
