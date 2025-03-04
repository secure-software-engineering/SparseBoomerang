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
package typestate;


import wpds.impl.Weight;

import java.util.HashSet;
import java.util.Set;

public class TransitionRepresentationFunction extends Weight {

  private final String rep;

  private static TransitionRepresentationFunction one;

  private static TransitionRepresentationFunction zero;


  private TransitionRepresentationFunction(String rep) {
    this.rep = rep;
  }


  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) return this;
    if (this.equals(one())) return other;
    if (other.equals(zero()) || this.equals(zero())) {
      return zero();
    }
    throw new IllegalStateException("This should not happen!");
     }

  @Override
  public Weight combineWith(Weight other) {
    if (!(other instanceof TransitionRepresentationFunction)) throw new RuntimeException();
    if (this.equals(zero())) return other;
    if (other.equals(zero())) return this;
    if (other.equals(one()) && this.equals(one())) {
      return one();
    }
    TransitionRepresentationFunction func = (TransitionRepresentationFunction) other;

    return extendWith(other);
  }

  public static TransitionRepresentationFunction one() {
    if (one == null) {
        one = new TransitionRepresentationFunction("ONE");
    }
    return one;
  }

  public static TransitionRepresentationFunction zero() {
    if (zero == null) zero = new TransitionRepresentationFunction("ZERO");
    return zero;
  }

  public String toString() {
    return (this==one)? "ONE" : "ZERO";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }


}
