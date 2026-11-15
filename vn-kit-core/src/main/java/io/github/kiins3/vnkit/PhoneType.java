package io.github.kiins3.vnkit;

/** Kind of a Vietnamese phone number. */
public enum PhoneType {
  /** Ten digits, e.g. {@code 0912 345 678}. */
  MOBILE,
  /** Eleven digits with an area code starting {@code 02}, e.g. {@code 024 3825 1234}. */
  FIXED
}
