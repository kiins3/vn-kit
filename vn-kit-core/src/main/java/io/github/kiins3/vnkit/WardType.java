package io.github.kiins3.vnkit;

/** Kind of commune-level unit. */
public enum WardType {
  /** Phường (urban ward). */
  WARD("Phường"),
  /** Xã (rural commune). */
  COMMUNE("Xã"),
  /** Đặc khu (special zone, mostly islands). */
  SPECIAL_ZONE("Đặc khu");

  private final String label;

  WardType(String label) {
    this.label = label;
  }

  /** The Vietnamese name of the unit type, as used in full names. */
  public String label() {
    return label;
  }
}
