package test;

import static test.AbstractTestingFramework.getJavaVersion;

import boomerang.framework.soot.SootFrameworkScope;
import boomerang.framework.soot.jimple.BoomerangPretransformer;
import boomerang.framework.sootup.BoomerangPreInterceptor;
import boomerang.framework.sootup.SootUpFrameworkScope;
import boomerang.scene.FrameworkScope;
import com.google.common.collect.Lists;
import jakarta.annotation.Nonnull;
import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;
import soot.*;
import soot.jimple.Jimple;
import soot.jimple.JimpleBody;
import soot.options.Options;
import sootup.SourceTypeIncludeExcludeAnalysisInputLocation;
import sootup.callgraph.CallGraphAlgorithm;
import sootup.callgraph.ClassHierarchyAnalysisAlgorithm;
import sootup.core.frontend.BodySource;
import sootup.core.frontend.ResolveException;
import sootup.core.graph.MutableBlockStmtGraph;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.inputlocation.EagerInputLocation;
import sootup.core.jimple.basic.NoPositionInformation;
import sootup.core.model.ClassModifier;
import sootup.core.model.MethodModifier;
import sootup.core.model.SootClassMember;
import sootup.core.model.SourceType;
import sootup.core.signatures.MethodSignature;
import sootup.core.signatures.PackageName;
import sootup.core.transform.BodyInterceptor;
import sootup.interceptors.BytecodeBodyInterceptors;
import sootup.java.bytecode.frontend.inputlocation.DefaultRuntimeAnalysisInputLocation;
import sootup.java.bytecode.frontend.inputlocation.JavaClassPathAnalysisInputLocation;
import sootup.java.core.JavaSootClass;
import sootup.java.core.JavaSootMethod;
import sootup.java.core.OverridingJavaClassSource;
import sootup.java.core.types.JavaClassType;
import sootup.java.core.views.JavaView;
import sootup.jimple.frontend.JimpleStringAnalysisInputLocation;
import sootup.jimple.frontend.JimpleView;

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

    System.out.println("framework:soot");

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
      eps.add(methodByName);

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
                }
              });

      // analyze
      PackManager.v()
          .getPack("wjtp")
          .add(transform); // whole programm, jimple, user-defined transformations
      PackManager.v().getPack("cg").apply(); // call graph package
      PackManager.v().getPack("wjtp").apply();
    }

    System.out.println("classes: " + Scene.v().getClasses().size());
    Scene.v().getClasses().stream()
        .sorted(Comparator.comparing(SootClass::toString))
        .forEach(System.out::println);

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
      String customEntrypointMethodName,
      List<String> includedPackages,
      List<String> excludedPackages) {

    System.out.println("framework:sootup");

    // configure interceptors
    // TODO: check if the interceptor needs a reset in between runs
    List<BodyInterceptor> bodyInterceptors =
        new ArrayList<>(BytecodeBodyInterceptors.Default.getBodyInterceptors());
    bodyInterceptors.add(new BoomerangPreInterceptor());

    // configure AnalysisInputLocations
    List<AnalysisInputLocation> inputLocations = new ArrayList<>();

    DefaultRuntimeAnalysisInputLocation runtimeInputLocation =
        new DefaultRuntimeAnalysisInputLocation();
    if (includedPackages.isEmpty() && excludedPackages.isEmpty()) {
      inputLocations.add(runtimeInputLocation);
    } else {
      SourceTypeIncludeExcludeAnalysisInputLocation.SourceTypeApplicationAnalysisInputLocation
          sourceTypeApplicationAnalysisInputLocationRuntime =
              new SourceTypeIncludeExcludeAnalysisInputLocation
                  .SourceTypeApplicationAnalysisInputLocation(
                  runtimeInputLocation, includedPackages);
      SourceTypeIncludeExcludeAnalysisInputLocation.SourceTypeLibraryAnalysisInputLocation
          sourceTypeLibraryAnalysisInputLocationRuntime =
              new SourceTypeIncludeExcludeAnalysisInputLocation
                  .SourceTypeLibraryAnalysisInputLocation(
                  sourceTypeApplicationAnalysisInputLocationRuntime, excludedPackages);
      inputLocations.add(sourceTypeApplicationAnalysisInputLocationRuntime);
      inputLocations.add(sourceTypeLibraryAnalysisInputLocationRuntime);
    }

    JavaClassPathAnalysisInputLocation classPathInputLocation =
        new JavaClassPathAnalysisInputLocation(pathStr, SourceType.Application, bodyInterceptors);

    if (true /*includedPackages.isEmpty() && excludedPackages.isEmpty() */) {
      inputLocations.add(classPathInputLocation);
    } else {
      SourceTypeIncludeExcludeAnalysisInputLocation.SourceTypeApplicationAnalysisInputLocation
          sourceTypeApplicationAnalysisInputLocation =
              new SourceTypeIncludeExcludeAnalysisInputLocation
                  .SourceTypeApplicationAnalysisInputLocation(
                  classPathInputLocation, includedPackages);

      SourceTypeIncludeExcludeAnalysisInputLocation.SourceTypeLibraryAnalysisInputLocation
          sourceTypeLibraryAnalysisInputLocation =
              new SourceTypeIncludeExcludeAnalysisInputLocation
                  .SourceTypeLibraryAnalysisInputLocation(
                  sourceTypeApplicationAnalysisInputLocation, excludedPackages);
      inputLocations.add(sourceTypeApplicationAnalysisInputLocation);
      inputLocations.add(sourceTypeLibraryAnalysisInputLocation);
    }

    /*
    // before: figure out if included/excluded was intended as: && or ||
                new ScopedAnalysisInputLocation.AllowlistingScopedAnalysisInputLocation(
                    classPathInputLocation, includedPackages),
                new ScopedAnalysisInputLocation.DenylistingScopedAnalysisInputLocation(
                    classPathInputLocation, excludedPackages))
     */

    JavaView javaView;
    sootup.callgraph.CallGraph cg;
    List<MethodSignature> entypointSignatures = Lists.newArrayList();

    if (customEntrypointMethodName == null) {

      javaView = new JavaView(inputLocations);
      // collect entrypoints
      for (JavaSootClass sootClass : javaView.getClasses().collect(Collectors.toList())) {
        String scStr = sootClass.toString();
        if (scStr.equals(className) || (scStr.contains(className + "$"))) {
          sootClass.getMethods().stream()
              .map(SootClassMember::getSignature)
              .forEach(entypointSignatures::add);
        }
      }

    } else {

      // build dummy entrypoint class
      String jimpleClassStr =
          "class dummyClass\n"
              + "{\n"
              + "    public static void main(java.lang.String[])\n"
              + "    {\n"
              + "        "
              + className
              + " dummyObj;\n"
              + "        java.lang.String[] l0;\n"
              + "        l0 := @parameter0: java.lang.String[];\n"
              + "        dummyObj = new "
              + className
              + ";\n"
              + "        virtualinvoke dummyObj.<"
              + className
              + ": void "
              + customEntrypointMethodName
              + "()>();\n"
              + "        return;\n"
              + "    }\n"
              + "}";

      JavaClassType dummyClassType = new JavaClassType("dummyClass", new PackageName(""));
      JimpleStringAnalysisInputLocation jimpleStringAnalysisInputLocation =
          new JimpleStringAnalysisInputLocation(
              jimpleClassStr, SourceType.Application, Collections.emptyList());
      JimpleView jimpleView = new JimpleView(jimpleStringAnalysisInputLocation);
      Optional<sootup.core.model.SootClass> aClass = jimpleView.getClass(dummyClassType);

      assert aClass.isPresent();
      sootup.core.model.SootClass sootClass = aClass.get();

      MethodSignature methodSignature =
          jimpleView
              .getIdentifierFactory()
              .parseMethodSignature("<dummyClass: void main(java.lang.String[])>");
      BodySource bodySource =
          new BodySource() {
            @Nonnull
            @Override
            public sootup.core.model.Body resolveBody(@Nonnull Iterable<MethodModifier> iterable)
                throws ResolveException, IOException {
              return sootup.core.model.Body.builder(
                      new MutableBlockStmtGraph(
                          sootClass
                              .getMethod(methodSignature.getSubSignature())
                              .get()
                              .getBody()
                              .getStmtGraph()))
                  .setMethodSignature(methodSignature)
                  .build();
            }

            @Override
            public Object resolveAnnotationsDefaultValue() {
              return null;
            }

            @Nonnull
            @Override
            public MethodSignature getSignature() {
              return methodSignature;
            }
          };
      JavaSootMethod method =
          new JavaSootMethod(
              bodySource,
              methodSignature,
              EnumSet.of(MethodModifier.PUBLIC, MethodModifier.STATIC),
              Collections.emptySet(),
              Collections.emptyList(),
              NoPositionInformation.getInstance());

      OverridingJavaClassSource dummyClassSource =
          new OverridingJavaClassSource(
              new EagerInputLocation(),
              Paths.get("/in-memory"),
              dummyClassType,
              null,
              Collections.emptySet(),
              null,
              Collections.emptySet(),
              Collections.singleton(method),
              NoPositionInformation.getInstance(),
              EnumSet.of(ClassModifier.PUBLIC),
              Collections.emptyList(),
              Collections.emptyList(),
              Collections.emptyList());

      inputLocations.add(
          new EagerInputLocation(
              Collections.singletonMap(dummyClassType, dummyClassSource), SourceType.Application));
      javaView = new JavaView(inputLocations);

      MethodSignature dummyEntrypoint =
          javaView
              .getIdentifierFactory()
              .parseMethodSignature("<dummyClass: void main(java.lang.String[])>");
      assert javaView.getMethod(dummyEntrypoint).isPresent();

      entypointSignatures.add(dummyEntrypoint);
    }

    System.out.println(
        "classes: "
            + javaView
                .getClasses()
                .count()); // soot has 1911 for boomerang.guided.DemandDrivenGuidedAnalysisTest

    javaView
        .getClasses()
        .sorted(Comparator.comparing(sootup.core.model.SootClass::toString))
        .forEach(System.out::println);
    System.out.println();

    // initialize CallGraphAlgorithm
    // TODO: use spark when available
    CallGraphAlgorithm cga =
        customEntrypointMethodName == null
            ? new ClassHierarchyAnalysisAlgorithm(javaView)
            : new ClassHierarchyAnalysisAlgorithm(javaView);
    cg = cga.initialize(entypointSignatures);

    return new SootUpFrameworkScope(javaView, cg, entypointSignatures);
  }
}
