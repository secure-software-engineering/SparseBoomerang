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
package sync.pds.weights;

import sync.pds.solver.nodes.Node;
import wpds.impl.Weight;

import java.util.Collection;

public class SetDomainRefactor<N, Stmt, Fact> extends Weight {

  private static SetDomainRefactor one;
  private static SetDomainRefactor zero;
  private final String rep;

  private SetDomainRefactor(String rep) {
    this.rep = rep;
  }

  private SetDomainRefactor(Collection<Node<Stmt, Fact>> nodes) {
    this.rep = null;
  }

  public SetDomainRefactor(Node<Stmt, Fact> node) {
    this.rep = null;
  }

  @Override
  public Weight extendWith(Weight other) {
    if (other.equals(one())) {
      return this;
    }
    if (this.equals(one())) {
      return other;
    }
    return zero();
  }

  @Override
  public Weight combineWith(Weight other) {
    if (other.equals(zero())) return this;
    if (this.equals(zero())) return other;
    if (this.equals(one()) || other.equals(one())) return one();

    return zero();
  }

  public static <N extends Weight,Location, Stmt, Fact> SetDomainRefactor<N, Stmt, Fact> one() {
    if (one == null) one = new SetDomainRefactor("<1>");
    return one;
  }

  public static <N extends Weight,Location, Stmt, Fact> SetDomainRefactor<N, Stmt, Fact> zero() {
    if (zero == null) zero = new SetDomainRefactor("<0>");
    return zero;
  }

  @Override
  public String toString() {
   return  (this==one)? "ONE" : "ZERO";
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    SetDomainRefactor other = (SetDomainRefactor) obj;
    if (rep == null) {
      return other.rep == null;
    } else return rep.equals(other.rep);
  }


}
