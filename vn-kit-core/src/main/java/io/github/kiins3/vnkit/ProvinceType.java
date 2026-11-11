package io.github.kiins3.vnkit;

/** Kind of province-level unit. */
public enum ProvinceType {
  /** Thành phố trực thuộc trung ương (centrally run city). */
  CITY("Thành phố"),
  /** Tỉnh. */
  PROVINCE("Tỉnh");

  private final String label;

  ProvinceType(String label) {
    this.label = label;
  }

  /** The Vietnamese name of the unit type, as used in full names. */
  public String label() {
    return label;
  }
}
