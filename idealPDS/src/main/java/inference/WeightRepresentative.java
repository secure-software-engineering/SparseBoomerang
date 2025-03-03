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

import wpds.impl.Weight;

public class WeightRepresentative extends Weight {

  private static WeightRepresentative zero;
  private static WeightRepresentative one;

  private WeightRepresentative() {
  }

  @Override
  public Weight extendWith(Weight other) {
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

  @Override
  public Weight combineWith(Weight other) {
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
    return ( this == one ) ? "ONE" : "ZERO";
  }

}
