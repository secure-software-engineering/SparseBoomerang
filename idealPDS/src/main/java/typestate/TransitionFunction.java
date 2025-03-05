package typestate;

import java.util.Collection;
import javax.annotation.Nonnull;
import typestate.finiteautomata.ITransition;
import wpds.impl.Weight;

public interface TransitionFunction extends Weight {
  @Nonnull
  Collection<ITransition> values();

  @Nonnull
  Weight extendWith(@Nonnull Weight other);

  @Nonnull
  Weight combineWith(@Nonnull Weight other);
}
