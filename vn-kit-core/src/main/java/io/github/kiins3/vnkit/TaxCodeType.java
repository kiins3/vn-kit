package io.github.kiins3.vnkit;

/** Kind of a Vietnamese tax code (mã số thuế). */
public enum TaxCodeType {
  /** Ten digits: a company or organization, or a person or household registered before 1 July 2025. */
  MAIN,
  /** Thirteen digits, {@code 0100109106-001}: a branch, office or other unit that depends on a ten-digit taxpayer. */
  DEPENDENT,
  /** Twelve digits: a personal identification number, the tax code of individuals and households since 1 July 2025. */
  PERSONAL_ID
}
