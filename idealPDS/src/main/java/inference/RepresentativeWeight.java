package inference;
/**
 * ***************************************************************************** Copyright (c) 2018
 * Fraunhofer IEM, Paderborn, Germany. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * <p>SPDX-License-Identifier: EPL-2.0
 *
 * <p>Contributors: Johannes Spaeth - initial API and implementation
 * *****************************************************************************
 */
import javax.annotation.Nonnull;
import wpds.impl.Weight;

public class RepresentativeWeight extends Weight {

  @Nonnull private final String rep;
  private static RepresentativeWeight one;
  private static RepresentativeWeight zero;

  private RepresentativeWeight(@Nonnull String rep) {
    this.rep = rep;
  }

  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) return this;
    if (this.equals(one())) return other;
    if (other.equals(zero()) || this.equals(zero())) {
      return zero();
    }
    throw new IllegalStateException("this can not happen!");
  }

  @Override
  public Weight combineWith(Weight other) {
    return extendWith(other);
  }

  public static <W extends Weight> W one() {
    if (one == null) one = new RepresentativeWeight("ONE");
    return (W) one;
  }

  public static <W extends Weight> W zero() {
    if (zero == null) zero = new RepresentativeWeight("ZERO");
    return (W) zero;
  }

  public String toString() {
    return this.rep;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + rep.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    RepresentativeWeight other = (RepresentativeWeight) obj;
    return rep.equals(other.rep);
  }
}
