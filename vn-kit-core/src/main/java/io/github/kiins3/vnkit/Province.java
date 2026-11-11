package io.github.kiins3.vnkit;

import java.util.List;

/**
 * A province-level unit: one of the 34 provinces and centrally run cities.
 *
 * @param code two-digit code from the national list of administrative units, e.g. {@code "01"}
 * @param name short name, e.g. {@code "Hà Nội"}
 * @param nameEn English short name, e.g. {@code "Hanoi"}
 * @param fullName name with the unit type, e.g. {@code "Thành phố Hà Nội"}
 * @param fullNameEn English name with the unit type, e.g. {@code "Hanoi City"}
 * @param codeName ASCII slug, e.g. {@code "ha_noi"}
 * @param type centrally run city or province
 * @param postalPrefixes two-digit postal code prefixes used in the province
 */
public record Province(
    String code,
    String name,
    String nameEn,
    String fullName,
    String fullNameEn,
    String codeName,
    ProvinceType type,
    List<String> postalPrefixes) {

  public Province {
    postalPrefixes = List.copyOf(postalPrefixes);
  }

  /** The wards, communes and special zones of this province, ordered by code. */
  public List<Ward> wards() {
    return AdminUnits.wardsOf(code);
  }
}
