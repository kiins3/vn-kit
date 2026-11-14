package io.github.kiins3.vnkit;

import java.util.List;

/**
 * A ward, commune or town (thị trấn) before 1 July 2025, and the current units it became.
 *
 * @param code five-digit code before the reform
 * @param districtCode code of its old district
 * @param provinceCode code of its old province
 * @param name short name, e.g. {@code "Dịch Vọng"} or {@code "1"} for Phường 1
 * @param fullName name with the unit type, e.g. {@code "Phường Dịch Vọng"}
 * @param type {@code "Phường"}, {@code "Xã"} or {@code "Thị trấn"}
 * @param newWardCodes codes of the current units it became part of. Most old wards went whole
 *     into one; a divided ward lists every unit that received part of it, the one that received
 *     most of it first.
 */
public record LegacyWard(
    String code,
    String districtCode,
    String provinceCode,
    String name,
    String fullName,
    String type,
    List<String> newWardCodes) {

  public LegacyWard {
    newWardCodes = List.copyOf(newWardCodes);
  }

  /** Whether it was split between several current units. */
  public boolean divided() {
    return newWardCodes.size() > 1;
  }

  /** The current units it became part of, the main one first. */
  public List<Ward> newWards() {
    return newWardCodes.stream().map(c -> AdminUnits.ward(c).orElseThrow()).toList();
  }

  /** Its old district. */
  public LegacyDistrict district() {
    return LegacyUnits.district(districtCode).orElseThrow();
  }

  /** Its old province. */
  public LegacyProvince province() {
    return LegacyUnits.province(provinceCode).orElseThrow();
  }
}
