package boomerang.weights;

import javax.annotation.Nonnull;
import wpds.impl.Weight;

public class MinDistanceWeight implements Weight {

  private static MinDistanceWeight one;
  private static MinDistanceWeight zero;

  private Integer minDistance = -1;
  private String rep;

  private MinDistanceWeight(String rep) {
    this.rep = rep;
  }

  public MinDistanceWeight(Integer minDistance) {
    this.minDistance = minDistance;
  }

  @Nonnull
  @Override
  public Weight extendWith(@Nonnull Weight o) {
    if (!(o instanceof MinDistanceWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    MinDistanceWeight other = (MinDistanceWeight) o;
    if (other.equals(one())) return this;
    if (this.equals(one())) return other;
    Integer newDistance = minDistance + other.minDistance;
    return new MinDistanceWeight(newDistance);
  }

  @Nonnull
  @Override
  public Weight combineWith(@Nonnull Weight o) {
    if (!(o instanceof MinDistanceWeight))
      throw new RuntimeException("Cannot extend to different types of weight!");
    MinDistanceWeight other = (MinDistanceWeight) o;
    if (other.equals(one())) return this;
    if (this.equals(one())) return other;
    return new MinDistanceWeight(Math.min(other.minDistance, minDistance));
  }

  public static MinDistanceWeight one() {
    if (one == null) one = new MinDistanceWeight("ONE");
    return one;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((minDistance == null) ? 0 : minDistance.hashCode());
    result = prime * result + ((rep == null) ? 0 : rep.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    MinDistanceWeight other = (MinDistanceWeight) obj;
    if (minDistance == null) {
      if (other.minDistance != null) return false;
    } else if (!minDistance.equals(other.minDistance)) return false;
    if (rep == null) {
      return other.rep == null;
    } else return rep.equals(other.rep);
  }

  @Override
  public String toString() {
    return this.equals(one()) ? "ONE " : " Distance: " + minDistance;
  }

  public Integer getMinDistance() {
    return minDistance;
  }
}
