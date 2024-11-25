package test;

import static test.AbstractTestingFramework.getJavaVersion;

import boomerang.framework.soot.SootFrameworkScope;
import boomerang.framework.soot.jimple.BoomerangPretransformer;
import boomerang.framework.sootup.BoomerangPreInterceptor;
import boomerang.framework.sootup.SootUpFrameworkScope;
import boomerang.scene.FrameworkScope;
import com.google.common.collect.Lists;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;
import sootup.ScopedAnalysisInputLocation;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SootClassMember;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.transform.BodyInterceptor;
import sootup.java.bytecode.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.bytecode.inputlocation.JrtFileSystemAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.interceptors.BytecodeBodyInterceptors;
import sootup.java.core.views.JavaView;

// TODO: refactor as parameterized test -> update to junit 5
public class FrameworkScopeFactory {

  // FIXME: adapt to be used by Guided...Tests and CustomFlowTests --> no need to create a
  // in-memory-entrypoint! --> uses processdir
  // see
  // https://github.com/secure-software-engineering/SparseBoomerang/blob/4c491929237d869f6efdd9fcadbd065c1729610a/boomerangPDS/src/test/java/boomerang/guided/DemandDrivenGuidedAnalysisTest.java#L463
  public static FrameworkScope init(String classPath, String fqClassName) {
    return init(classPath, fqClassName, null, Collections.emptyList(), Collections.emptyList());
  }

  public static FrameworkScope init(
      String classPath,
      String fqClassName,
      String customEntrypointMethodName,
      List<String> includedPackages,
      List<String> excludedPackages) {

    // TODO: ms: currently: switch here to test desired framework - refactor e.g. into parameterized
    // tests!
    return getSootFrameworkScope(
        classPath, fqClassName, customEntrypointMethodName, includedPackages, excludedPackages);
  }

  private static FrameworkScope getSootFrameworkScope(
      String pathStr,
      String className,
      String customEntrypointMethodName,
      List<String> includedPackages,
      List<String> excludedPackages) {

    List<SootMethod> eps = Lists.newArrayList();
    SootMethod sootTestMethod = null;
    int classCount = 0;

    G.v().reset();

    Options.v().set_whole_program(true);
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);

    if (!includedPackages.isEmpty()) {
      Options.v().set_include(includedPackages);
    }

    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_keep_line_number(true);

    Options.v().setPhaseOption("jb.sils", "enabled:false");
    Options.v().setPhaseOption("jb", "use-original-names:true");

    if (!excludedPackages.isEmpty()) {
      Options.v().set_exclude(excludedPackages);
    }

    if (customEntrypointMethodName == null) {
      Options.v().setPhaseOption("cg.cha", "on");
      Options.v().setPhaseOption("cg.cha", "verbose:true");

      Options.v().set_prepend_classpath(true);
      List<String> processDir = Collections.singletonList(pathStr);
      Options.v().set_process_dir(processDir);

      Scene.v().loadNecessaryClasses();

      for (SootClass sootClass : Scene.v().getClasses()) {
        classCount++;
        String scStr = sootClass.toString();
        if (scStr.equals(className) || (scStr.startsWith(className + "$"))) {
          sootClass.setApplicationClass();
          eps.addAll(sootClass.getMethods());
        }
      }
      if (eps.isEmpty()) {
        throw new IllegalStateException(
            "No entrypoints given/found in " + classCount + " classes.");
      }
      Scene.v().setEntryPoints(eps);

      PackManager.v().runPacks();
      BoomerangPretransformer.v().reset();
      BoomerangPretransformer.v().apply();

    } else {
      Options.v().setPhaseOption("cg.spark", "on");
      Options.v().setPhaseOption("cg.spark", "verbose:true");

      // which runtime library needs to be configured
      if (getJavaVersion() < 9) {
        Options.v().set_prepend_classpath(true);
        Options.v().set_soot_classpath(pathStr);
      } else if (getJavaVersion() >= 9) {
        Options.v().set_soot_classpath("VIRTUAL_FS_FOR_JDK" + File.pathSeparator + pathStr);
      }

      // create entrypoint class/method
      SootClass sootTestCaseClass = Scene.v().forceResolve(className, SootClass.BODIES);
      for (SootMethod m : sootTestCaseClass.getMethods()) {
        if (m.getName().equals(customEntrypointMethodName)) {
          sootTestMethod = m;
          break;
        }
      }
      if (sootTestMethod == null) {
        throw new RuntimeException(
            "The method with name "
                + customEntrypointMethodName
                + " was not found in the Soot Scene.");
      }
      sootTestMethod.getDeclaringClass().setApplicationClass();

      String targetClass = getTargetClass(sootTestMethod, className);

      Scene.v().addBasicClass(targetClass, SootClass.BODIES);
      Scene.v().loadNecessaryClasses();

      SootClass c = Scene.v().forceResolve(targetClass, SootClass.BODIES);
      if (c != null) {
        c.setApplicationClass();
      }

      SootMethod methodByName = c.getMethodByName("main");
      for (SootMethod m : sootTestCaseClass.getMethods()) {
        if (m.isStaticInitializer()) {
          eps.add(m);
        }
      }

      // collect entrypoints
      for (SootClass inner : Scene.v().getClasses()) {
        classCount++;
        if (inner.getName().contains(sootTestCaseClass.getName())) {
          inner.setApplicationClass();
          for (SootMethod m : inner.getMethods()) {
            if (m.isStaticInitializer()) {
              eps.add(m);
            }
          }
        }
      }
      eps.add(methodByName);

      if (eps.isEmpty()) {
        throw new IllegalStateException(
            "No entrypoints given/found in " + classCount + " classes.");
      }
      Scene.v().setEntryPoints(eps);

      Transform transform =
          new Transform(
              "wjtp.ifds",
              new SceneTransformer() {
                @Override
                protected void internalTransform(String phaseName, Map<String, String> options) {
                  BoomerangPretransformer.v().reset();
                  BoomerangPretransformer.v().apply();
                  /* TODO: [ms] still needed?
                  callGraph = new SootCallGraph();
                  dataFlowScope = getDataFlowScope();
                  analyzeWithCallGraph();*/
                }
              });

      // analyze
      PackManager.v()
          .getPack("wjtp")
          .add(transform); // whole programm, jimple, user-defined transformations
      PackManager.v().getPack("cg").apply(); // call graph package
      PackManager.v().getPack("wjtp").apply();
    }

    return new SootFrameworkScope();
  }

  private static String getTargetClass(SootMethod sootTestMethod, String testCaseClassName) {
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
    RefType testCaseType = RefType.v(testCaseClassName);
    Local loc = Jimple.v().newLocal("l0", paramType);
    body.getLocals().add(loc);
    body.getUnits().add(Jimple.v().newIdentityStmt(loc, Jimple.v().newParameterRef(paramType, 0)));
    Local allocatedTestObj = Jimple.v().newLocal("dummyObj", testCaseType);
    body.getLocals().add(allocatedTestObj);
    body.getUnits()
        .add(Jimple.v().newAssignStmt(allocatedTestObj, Jimple.v().newNewExpr(testCaseType)));
    body.getUnits()
        .add(
            Jimple.v()
                .newInvokeStmt(
                    Jimple.v().newVirtualInvokeExpr(allocatedTestObj, sootTestMethod.makeRef())));
    body.getUnits().add(Jimple.v().newReturnVoidStmt());

    Scene.v().addClass(sootClass);
    body.validate();
    return sootClass.toString();
  }

  /** SootUp Framework setup TODO: [ms] refactor me! */
  private static FrameworkScope getSootUpFrameworkScope(
      String pathStr,
      String className,
      String methodName,
      String applicationDir,
      List<String> includedPackages,
      List<String> excludedPackages) {

    // configure interceptors
    List<BodyInterceptor> bodyInterceptors =
        new ArrayList<>(BytecodeBodyInterceptors.Default.getBodyInterceptors());
    bodyInterceptors.add(
        new BoomerangPreInterceptor()); // TODO: check if the interceptor needs a reset in between
    // runs?

    // configure AnalysisInputLocations
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();

    if (applicationDir != null) {
      JavaClassPathAnalysisInputLocation processDirInputLocation =
          new JavaClassPathAnalysisInputLocation(pathStr, SourceType.Application, bodyInterceptors);
    }

    JavaClassPathAnalysisInputLocation classPathInputLocation =
        new JavaClassPathAnalysisInputLocation(pathStr, SourceType.Application, bodyInterceptors);
    inputLocations.addAll(
        List.of(
            new JrtFileSystemAnalysisInputLocation(),
            // FIXME: figure out if included/excluded was intended as: && or ||
            new ScopedAnalysisInputLocation.AllowlistingScopedAnalysisInputLocation(
                classPathInputLocation, includedPackages),
            new ScopedAnalysisInputLocation.DenylistingScopedAnalysisInputLocation(
                classPathInputLocation, excludedPackages)));
    JavaView javaView = new JavaView(inputLocations);

    sootup.callgraph.CallGraph cg;
    List<MethodSignature> entypointSignatures;
    List<JavaSootMethod> eps = Lists.newArrayList();

    if (methodName != null) {
      // build entrypoint

      /*
      String targetClassAsJimpleStr = ""; print jimple via soot and adapt to a template
      new JimpleStringAnalysisInputLocation() // currently in sootup:develop  branch
      */
      throw new UnsupportedOperationException("implement me!");

    } else {
      // collect entrypoints
      for (JavaSootClass sootClass : javaView.getClasses()) {
        String scStr = sootClass.toString();
        if (scStr.equals(className) || (scStr.contains(className + "$"))) {
          eps.addAll(sootClass.getMethods());
        }
      }
    }

    // initialize CallGraphAlgorithm
    entypointSignatures =
        eps.stream().map(SootClassMember::getSignature).collect(Collectors.toList());
    ClassHierarchyAnalysisAlgorithm cha = new ClassHierarchyAnalysisAlgorithm(javaView);
    cg = cha.initialize(entypointSignatures);

    return new SootUpFrameworkScope(javaView, cg, entypointSignatures);
  }
}
