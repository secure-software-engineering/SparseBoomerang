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

public class WeightRepresentative implements Weight {

  private static WeightRepresentative zero;
  private static WeightRepresentative one;

  private WeightRepresentative() {}

  @Nonnull
  @Override
  public Weight extendWith(@Nonnull Weight other) {
    if (other.equals(one())) {
      return this;
    }
    if (this.equals(one())) {
      return other;
    }
    if (other.equals(zero()) || this.equals(zero())) {
      return zero();
    }
    throw new IllegalStateException("This should not happen!");
  }

  @Nonnull
  @Override
  public Weight combineWith(@Nonnull Weight other) {
    return extendWith(other);
  }

  public static <W extends Weight> W one() {
    if (one == null) {
      one = new WeightRepresentative();
    }
    return (W) one;
  }

  public static <W extends Weight> W zero() {
    if (zero == null) {
      zero = new WeightRepresentative();
    }
    return (W) zero;
  }

  public String toString() {
    return (this == one) ? "ONE" : "ZERO";
  }
}
