package io.github.kiins3.vnkit;

import io.github.kiins3.vnkit.Names.Prefix;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Matches the names people type against current province and ward names. */
final class NameMatcher {

  private NameMatcher() {}

  static final List<List<String>> PROVINCE_PREFIXES =
      List.of(List.of("thanh", "pho"), List.of("tp"), List.of("tinh"));

  // Longest first. "Thị trấn" no longer exists but still appears in old addresses.
  private static final List<Prefix> WARD_PREFIXES = List.of(
      Prefix.of("dac khu", "dac khu"),
      Prefix.of("thi tran", null),
      Prefix.of("phuong", "phuong"),
      Prefix.of("xa", "xa"),
      Prefix.of("tt", null),
      Prefix.of("p", "phuong"),
      Prefix.of("f", "phuong"),
      Prefix.of("x", "xa"));

  private static final Map<String, String> PROVINCE_ALIASES = Map.of(
      "hn", "01",
      "hcm", "79",
      "tphcm", "79",
      "hcmc", "79",
      "saigon", "79",
      "sg", "79",
      "thuathienhue", "46");

  // ---------- Provinces ----------

  static Optional<Province> findProvince(String text) {
    return lookup(text, ProvinceIndex.INDEX, AdminUnits::province);
  }

  /** Finds a unit in a compact-name index, by code, by its name, or by its name without a leading type. */
  static <T> Optional<T> lookup(String text, Map<String, T> index, java.util.function.Function<String, Optional<T>> byCode) {
    String t = text.trim();
    if (!t.isEmpty() && t.length() <= 2 && t.chars().allMatch(Character::isDigit)) {
      return byCode.apply(t.length() == 1 ? "0" + t : t);
    }
    List<String> plain = VnText.words(t).stream().map(VnText::plain).toList();
    T hit = index.get(String.join("", plain));
    if (hit != null) return Optional.of(hit);
    for (List<String> prefix : PROVINCE_PREFIXES) {
      if (plain.size() > prefix.size() && plain.subList(0, prefix.size()).equals(prefix)) {
        hit = index.get(String.join("", plain.subList(prefix.size(), plain.size())));
        if (hit != null) return Optional.of(hit);
      }
    }
    return Optional.empty();
  }

  private static final class ProvinceIndex {
    static final Map<String, Province> INDEX = build();

    private static Map<String, Province> build() {
      Map<String, Province> index = new HashMap<>();
      for (Province p : AdminUnits.provinces()) {
        for (String s : List.of(p.name(), p.nameEn(), p.fullName(), p.fullNameEn(), p.codeName())) {
          index.put(VnText.compact(s), p);
        }
      }
      PROVINCE_ALIASES.forEach((alias, code) -> AdminUnits.province(code).ifPresent(p -> index.put(alias, p)));
      return Map.copyOf(index);
    }
  }

  // ---------- Wards ----------

  private static final class WardKeys {
    static final Map<String, Names.Key> BY_CODE = build();

    private static Map<String, Names.Key> build() {
      Map<String, Names.Key> keys = new HashMap<>();
      for (Ward w : AdminUnits.wards()) keys.put(w.code(), Names.key(w.name()));
      return Map.copyOf(keys);
    }
  }

  private static final Map<WardType, String> TYPE_KEYS = Map.of(
      WardType.WARD, "phuong", WardType.COMMUNE, "xa", WardType.SPECIAL_ZONE, "dac khu");

  static List<Names.Scored<Ward>> scoreWards(String query, List<Ward> pool) {
    return Names.score(query, pool, w -> WardKeys.BY_CODE.get(w.code()), w -> TYPE_KEYS.get(w.type()), WARD_PREFIXES);
  }

  static Optional<Ward> findWard(String name, List<Ward> pool) {
    return Names.unique(scoreWards(name, pool));
  }

  static List<Ward> searchWards(String query, List<Ward> pool) {
    return Names.ranked(scoreWards(query, pool));
  }
}
