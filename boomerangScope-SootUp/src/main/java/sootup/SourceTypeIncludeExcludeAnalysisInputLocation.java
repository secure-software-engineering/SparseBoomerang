package sootup;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import sootup.core.frontend.SootClassSource;
import sootup.core.inputlocation.AnalysisInputLocation;
import sootup.core.model.SourceType;
import sootup.core.transform.BodyInterceptor;
import sootup.core.types.ClassType;
import sootup.core.views.View;

public abstract class SourceTypeIncludeExcludeAnalysisInputLocation
    implements AnalysisInputLocation {

  protected abstract boolean filter(@Nonnull ClassType type);

  protected abstract AnalysisInputLocation getInputLocation();

  @Nonnull
  @Override
  public abstract SourceType getSourceType();

  @Nonnull
  @Override
  public Optional<? extends SootClassSource> getClassSource(
      @Nonnull ClassType type, @Nonnull View view) {
    if (filter(type)) {
      return getInputLocation().getClassSource(type, view);
    }
    return Optional.empty();
  }

  @Nonnull
  @Override
  public Collection<? extends SootClassSource> getClassSources(@Nonnull View view) {
    return getInputLocation().getClassSources(view).stream()
        .filter(type -> filter(type.getClassType()))
        .collect(Collectors.toList());
  }

  @Nonnull
  @Override
  public List<BodyInterceptor> getBodyInterceptors() {
    return getInputLocation().getBodyInterceptors();
  }

  public static class SourceTypeLibraryAnalysisInputLocation
      extends SourceTypeIncludeExcludeAnalysisInputLocation {

    @Nonnull private final SourceTypeIncludeExcludeAnalysisInputLocation inputLocation;
    private final Collection<String> excludes;

    public SourceTypeLibraryAnalysisInputLocation(
        @Nonnull SourceTypeIncludeExcludeAnalysisInputLocation inputLocation,
        Collection<String> excludes) {
      this.inputLocation = inputLocation;
      this.excludes = excludes;
    }

    @Override
    protected AnalysisInputLocation getInputLocation() {
      return inputLocation;
    }

    protected boolean filter(@Nonnull ClassType type) {
      String className = type.getFullyQualifiedName();
      for (String pkg : excludes) {
        if (className.equals(pkg)
            || ((pkg.endsWith(".*") || pkg.endsWith("$*"))
                && className.startsWith(pkg.substring(0, pkg.length() - 1)))) {
          return !inputLocation.filter(type);
        }
      }
      return false;
    }

    @Nonnull
    @Override
    public SourceType getSourceType() {
      return SourceType.Library;
    }
  }

  public static class SourceTypeApplicationAnalysisInputLocation
      extends SourceTypeIncludeExcludeAnalysisInputLocation {
    private final Collection<String> includes;
    @Nonnull private final AnalysisInputLocation inputLocation;

    @Override
    protected AnalysisInputLocation getInputLocation() {
      return inputLocation;
    }

    public SourceTypeApplicationAnalysisInputLocation(
        @Nonnull AnalysisInputLocation inputLocation, Collection<String> includes) {
      super();
      this.inputLocation = inputLocation;
      this.includes = includes;
    }

    protected boolean filter(@Nonnull ClassType type) {
      String className = type.getFullyQualifiedName();
      for (String pkg : includes) {
        if (className.equals(pkg)
            || ((pkg.endsWith(".*") || pkg.endsWith("$*"))
                && className.startsWith(pkg.substring(0, pkg.length() - 1)))) {
          return true;
        }
      }
      return false;
    }

    @Nonnull
    @Override
    public SourceType getSourceType() {
      return SourceType.Application;
    }
  }
}
