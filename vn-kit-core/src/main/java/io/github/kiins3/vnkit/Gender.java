package io.github.kiins3.vnkit;

/** Gender as encoded in a citizen ID number. */
public enum Gender {
  MALE("Nam"),
  FEMALE("Nữ");

  private final String label;

  Gender(String label) {
    this.label = label;
  }

  /** Vietnamese label: {@code "Nam"} or {@code "Nữ"}. */
  public String label() {
    return label;
  }
}
