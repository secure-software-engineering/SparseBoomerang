package test;

import static test.AbstractTestingFramework.getJavaVersion;

import boomerang.framework.soot.SootFrameworkScope;
import boomerang.framework.soot.jimple.BoomerangPretransformer;
import boomerang.framework.sootup.BoomerangPreInterceptor;
import boomerang.framework.sootup.SootUpFrameworkScope;
import boomerang.scene.FrameworkScope;
import com.google.common.collect.Lists;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import soot.*;
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

  public static FrameworkScope init(String cpStr, String cls) {
    return init(cpStr, cls, Collections.emptyList(), Collections.emptyList());
  }

  public static FrameworkScope init(
      String cpStr, String cls, List<String> includedPackages, List<String> excludedPackages) {

    // TODO: ms: currently: switch here to test desired framework - refactor e.g. into parameterized
    // tests!
    return getSootFrameworkScope(cpStr, cls, includedPackages, excludedPackages);
  }

  private static FrameworkScope getSootFrameworkScope(
      String pathStr,
      String className,
      List<String> includedPackages,
      List<String> excludedPackages) {

    G.v().reset();

    String CG = "cha"; // sometimes the test used "spark"
    Options.v().set_whole_program(true);
    Options.v().setPhaseOption("cg." + CG, "on");
    Options.v().setPhaseOption("cg." + CG, "verbose:true");
    Options.v().set_output_format(Options.output_format_none);
    Options.v().set_no_bodies_for_excluded(true);
    Options.v().set_allow_phantom_refs(true);
    if(!includedPackages.isEmpty()) {
      Options.v().set_include(includedPackages); // [ms] not all tests had this option
    }
    Options.v().setPhaseOption("jb", "use-original-names:true");
    Options.v().set_keep_line_number(true);

    // JAVA VERSION 8
    if (getJavaVersion() < 9) {
      Options.v().set_prepend_classpath(true);
      Options.v().set_soot_classpath(pathStr);

      System.out.println("ms: setup <= java8: " + pathStr);

    }
    // JAVA VERSION 9
    else if (getJavaVersion() >= 9) {
      System.out.println("ms: setup >= java9: " + pathStr);
      Options.v().set_soot_classpath("VIRTUAL_FS_FOR_JDK" + File.pathSeparator + pathStr);
    }

    Options.v()
        .set_process_dir(
            Lists.newArrayList(
                Paths.get(pathStr)
                    .toAbsolutePath()
                    .toString())); // some have: Arrays.asList(classPath.split(":")) - sometimes not
                                   // even set (AbstractTestingFramework.java)

    Options.v().setPhaseOption("jb.sils", "enabled:false");
    Options.v().setPhaseOption("jb", "use-original-names:true");

    if(!excludedPackages.isEmpty()) {
      Options.v().set_exclude(excludedPackages);
    }

    Scene.v().loadNecessaryClasses();
    // collect entrypoints
    List<SootMethod> eps = Lists.newArrayList();
    int classCount = 0;
    for (SootClass sootClass : Scene.v().getClasses()) {
      classCount++;
      String scStr = sootClass.toString();
      if (scStr.equals(className) || (scStr.contains(className + "$"))) {
        sootClass.setApplicationClass();
        eps.addAll(sootClass.getMethods());
      }
    }
    Scene.v().setEntryPoints(eps);
    System.out.println("classCount: " + classCount);

    assert !eps.isEmpty();

    PackManager.v().runPacks();
    BoomerangPretransformer.v().reset();
    BoomerangPretransformer.v().apply();

    return new SootFrameworkScope();
  }

  private static FrameworkScope getSootUpFrameworkScope(
      String pathStr,
      String className,
      List<String> includedPackages,
      List<String> excludedPackages) {

    // configure interceptors
    List<BodyInterceptor> bodyInterceptors =
        new ArrayList<>(BytecodeBodyInterceptors.Default.getBodyInterceptors());
    bodyInterceptors.add(
        new BoomerangPreInterceptor()); // TODO: check if the interceptor needs a reset in between
    // runs?

    // configure AnalysisInputLocations

    JavaClassPathAnalysisInputLocation classPathInputLocation =
        new JavaClassPathAnalysisInputLocation(pathStr, SourceType.Application, bodyInterceptors);
    List<AnalysisInputLocation> inputLocations =
        List.of(
            new JrtFileSystemAnalysisInputLocation(),
            // FIXME: figure out if included/excluded was intended as: && or ||
            new ScopedAnalysisInputLocation.AllowlistingScopedAnalysisInputLocation(
                classPathInputLocation, includedPackages),
            new ScopedAnalysisInputLocation.DenylistingScopedAnalysisInputLocation(
                classPathInputLocation, excludedPackages));
    JavaView javaView = new JavaView(inputLocations);

    sootup.callgraph.CallGraph cg;
    List<MethodSignature> entypointSignatures;
    if (className != null) {
      // collect entrypoints
      List<JavaSootMethod> eps = Lists.newArrayList();
      for (JavaSootClass sootClass : javaView.getClasses()) {
        String scStr = sootClass.toString();
        if (scStr.equals(className) || (scStr.contains(className + "$"))) {
          eps.addAll(sootClass.getMethods());
        }
      }

      // initialize CallGraphAlgorithm
      entypointSignatures =
          eps.stream().map(SootClassMember::getSignature).collect(Collectors.toList());
      ClassHierarchyAnalysisAlgorithm cha = new ClassHierarchyAnalysisAlgorithm(javaView);
      cg = cha.initialize(entypointSignatures);

    } else {
      cg = null; // TODO: irgh!
      throw new UnsupportedOperationException("ms: check if this is wanted/valid");
    }

    return new SootUpFrameworkScope(javaView, cg, entypointSignatures);
  }
}
