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
package inference;

import static inference.WeightRepresentative.one;
import static inference.WeightRepresentative.zero;

import boomerang.scope.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.annotation.Nonnull;
import wpds.impl.Weight;

public class InferenceWeight extends Weight {

  @Nonnull private final Set<Method> invokedMethods;

  private InferenceWeight(Set<Method> res) {
    this.invokedMethods = res;
  }

  public InferenceWeight(Method m) {
    this.invokedMethods = Collections.singleton(m);
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
    InferenceWeight func = (InferenceWeight) other;
    Set<Method> res = new HashSet<>(invokedMethods);
    res.addAll(func.invokedMethods);
    return new InferenceWeight(res);
  }

  @Override
  public Weight combineWith(Weight other) {
    return extendWith(other);
  }

  public String toString() {
    return "{Func:" + invokedMethods.toString() + "}";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + invokedMethods.hashCode();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    InferenceWeight other = (InferenceWeight) obj;
    return invokedMethods.equals(other.invokedMethods);
  }
}
