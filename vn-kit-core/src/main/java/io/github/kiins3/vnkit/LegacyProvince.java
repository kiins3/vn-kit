package io.github.kiins3.vnkit;

import java.util.List;

/**
 * A province as it was before 1 July 2025, when there were 63. Codes are from the old national
 * list and can differ from current codes for the same name: the old Bắc Ninh was {@code "27"},
 * the current one is {@code "24"}.
 *
 * @param code two-digit code before the reform
 * @param name short name, e.g. {@code "Bình Dương"}
 * @param fullName name with the unit type, e.g. {@code "Tỉnh Bình Dương"}
 * @param newProvinceCode code of the current province it became part of
 */
public record LegacyProvince(String code, String name, String fullName, String newProvinceCode) {

  /** The current province it became part of. Provinces were merged whole, never split. */
  public Province newProvince() {
    return AdminUnits.province(newProvinceCode).orElseThrow();
  }

  /** Its districts, ordered by code. */
  public List<LegacyDistrict> districts() {
    return LegacyUnits.districtsOf(code);
  }
}
