package io.github.kiins3.vnkit;

import java.util.List;

/**
 * A district (quận, huyện, thị xã or city under a province) before 1 July 2025, when the district
 * level was removed.
 *
 * @param code three-digit code before the reform
 * @param provinceCode code of its old province
 * @param name short name, e.g. {@code "Cầu Giấy"} or {@code "1"} for Quận 1
 * @param fullName name with the unit type, e.g. {@code "Quận Cầu Giấy"}
 * @param type {@code "Quận"}, {@code "Huyện"}, {@code "Thị xã"} or {@code "Thành phố"}
 * @param newWardCodes for the five island districts that had no wards, the special zone each
 *     became; empty for every other district
 */
public record LegacyDistrict(
    String code, String provinceCode, String name, String fullName, String type, List<String> newWardCodes) {

  public LegacyDistrict {
    newWardCodes = List.copyOf(newWardCodes);
  }

  /** Its old province. */
  public LegacyProvince province() {
    return LegacyUnits.province(provinceCode).orElseThrow();
  }

  /** Its wards, communes and towns, ordered by code; empty for island districts. */
  public List<LegacyWard> wards() {
    return LegacyUnits.wardsOf(code);
  }
}
