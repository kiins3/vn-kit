package io.github.kiins3.vnkit;

/**
 * A commune-level unit: a ward (phường), commune (xã) or special zone (đặc khu). Since July 2025
 * these sit directly under a province; there is no district level any more.
 *
 * @param code five-digit code from the national list of administrative units, e.g. {@code "00004"}
 * @param provinceCode code of the province it belongs to
 * @param name short name, e.g. {@code "Ba Đình"}
 * @param nameEn name without accents, e.g. {@code "Ba Dinh"}
 * @param fullName name with the unit type, e.g. {@code "Phường Ba Đình"}
 * @param fullNameEn English name with the unit type, e.g. {@code "Ba Dinh Ward"}
 * @param codeName ASCII slug, e.g. {@code "ba_dinh"}
 * @param type ward, commune or special zone
 * @param postalCode five-digit postal code
 */
public record Ward(
    String code,
    String provinceCode,
    String name,
    String nameEn,
    String fullName,
    String fullNameEn,
    String codeName,
    WardType type,
    String postalCode) {

  /** The province this unit belongs to. */
  public Province province() {
    return AdminUnits.province(provinceCode).orElseThrow();
  }
}
