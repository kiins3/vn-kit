package io.github.kiins3.vnkit;

import java.util.List;
import java.util.Optional;

/**
 * Vietnam's administrative units after the July 2025 reform: 34 provinces and centrally run
 * cities, each split directly into wards, communes and special zones (3,321 in total).
 *
 * <pre>{@code
 * AdminUnits.province("01");                        // Thành phố Hà Nội
 * AdminUnits.findProvince("tp.hcm");                // Thành phố Hồ Chí Minh
 * AdminUnits.findWard("P. Ba Dinh", "Ha Noi");      // Phường Ba Đình
 * AdminUnits.searchWards("ben thanh");              // every unit whose name matches
 * }</pre>
 *
 * Names are matched without regard to case, punctuation or accents, but an exact accented match
 * wins: {@code "Sơn Hạ"} and {@code "Sơn Hà"} are different communes. The data is loaded once,
 * on first use, and is immutable and safe to share between threads.
 */
public final class AdminUnits {

  private AdminUnits() {}

  /** All provinces and centrally run cities, ordered by code. */
  public static List<Province> provinces() {
    return Registry.get().provinces;
  }

  /** All wards, communes and special zones, ordered by code. */
  public static List<Ward> wards() {
    return Registry.get().wards;
  }

  /** The province with this two-digit code, e.g. {@code "79"}. */
  public static Optional<Province> province(String code) {
    return Optional.ofNullable(code).map(c -> Registry.get().provincesByCode.get(c.trim()));
  }

  /** The ward, commune or special zone with this five-digit code, e.g. {@code "26743"}. */
  public static Optional<Ward> ward(String code) {
    return Optional.ofNullable(code).map(c -> Registry.get().wardsByCode.get(c.trim()));
  }

  /** The wards, communes and special zones of a province, ordered by code; empty for an unknown code. */
  public static List<Ward> wardsOf(String provinceCode) {
    return Registry.get().wardsByProvince.getOrDefault(provinceCode, List.of());
  }

  /**
   * Finds a province by how people write it: with or without accents or the unit type, in
   * English, by a common abbreviation or by code. {@code "Hà Nội"}, {@code "ha noi"},
   * {@code "TP. Hà Nội"}, {@code "Hanoi"}, {@code "HN"} and {@code "01"} all give Hà Nội.
   */
  public static Optional<Province> findProvince(String text) {
    return Optional.ofNullable(text).flatMap(NameMatcher::findProvince);
  }

  /**
   * Finds a ward, commune or special zone by name inside a province. The province may be a
   * code or a name as accepted by {@link #findProvince}. Returns empty when nothing matches
   * or when the name is ambiguous, e.g. {@code "Son Ha"} in Quảng Ngãi matches both Sơn Hà
   * and Sơn Hạ; {@link #searchWards(String, String)} lists every candidate.
   */
  public static Optional<Ward> findWard(String name, String province) {
    if (name == null || province == null) return Optional.empty();
    return findProvince(province).flatMap(p -> NameMatcher.findWard(name, p.wards()));
  }

  /** Wards, communes and special zones anywhere in the country whose name matches, best matches first. */
  public static List<Ward> searchWards(String query) {
    return query == null ? List.of() : NameMatcher.searchWards(query, wards());
  }

  /** Like {@link #searchWards(String)}, limited to one province (a code or a name). */
  public static List<Ward> searchWards(String query, String province) {
    if (query == null || province == null) return List.of();
    return findProvince(province).map(p -> NameMatcher.searchWards(query, p.wards())).orElse(List.of());
  }

  /** Release of the bundled dataset, e.g. {@code "v5.2.0"}. */
  public static String datasetVersion() {
    return Registry.get().datasetVersion;
  }

  /** The most recent legal act reflected in the bundled data, e.g. {@code "388/NQ-UBTVQH16"}. */
  public static String latestDecree() {
    return Registry.get().latestDecree;
  }
}
