package wpds.impl;

import javax.annotation.Nonnull;

public class NoWeight extends Weight {

    static private final NoWeight INSTANCE = new NoWeight();

    private NoWeight(){
        /* Singleton */
    }

    @Nonnull
    public static NoWeight getInstance(){
      return INSTANCE;
    }

    @Override
    public Weight extendWith(@Nonnull Weight other) {
        return other;
    }

    @Override
    public Weight combineWith(@Nonnull Weight other) {
        return other;
    }

    @Override
    public String toString() {
        return "";
    }
}
