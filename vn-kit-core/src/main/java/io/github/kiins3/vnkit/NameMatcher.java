package io.github.kiins3.vnkit;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/** Matches the names people type against province and ward names. */
final class NameMatcher {

  private NameMatcher() {}

  /** A unit type written before a name: "Phường", "P.", "Xã", "TP."... */
  private record Prefix(List<String> words, WardType type) {}

  private static final List<List<String>> PROVINCE_PREFIXES =
      List.of(List.of("thanh", "pho"), List.of("tp"), List.of("tinh"));

  // Longest first. "Thị trấn" no longer exists but still appears in old addresses.
  private static final List<Prefix> WARD_PREFIXES = List.of(
      new Prefix(List.of("dac", "khu"), WardType.SPECIAL_ZONE),
      new Prefix(List.of("thi", "tran"), null),
      new Prefix(List.of("phuong"), WardType.WARD),
      new Prefix(List.of("xa"), WardType.COMMUNE),
      new Prefix(List.of("tt"), null),
      new Prefix(List.of("p"), WardType.WARD),
      new Prefix(List.of("x"), WardType.COMMUNE));

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
    String t = text.trim();
    if (!t.isEmpty() && t.chars().allMatch(Character::isDigit) && t.length() <= 2) {
      return AdminUnits.province(t.length() == 1 ? "0" + t : t);
    }
    List<String> plain = plainWords(VnText.words(t));
    Map<String, Province> index = ProvinceIndex.INDEX;
    Province hit = index.get(String.join("", plain));
    if (hit != null) return Optional.of(hit);
    for (List<String> prefix : PROVINCE_PREFIXES) {
      if (startsWith(plain, prefix)) {
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

  /** How well a name matches: 0 exact with accents, 1 exact without, 2 prefix, 3 substring; +4 if the unit type differs. */
  private record Scored(Ward ward, int tier) {}

  /** A reading of the query: its words with accents, without, and the unit type written before it. */
  private record Reading(String accented, String plain, WardType type) {}

  private record Keys(String accented, String plain) {}

  private static final class WardKeys {
    static final Map<String, Keys> BY_CODE = build();

    private static Map<String, Keys> build() {
      Map<String, Keys> keys = new HashMap<>();
      for (Ward w : AdminUnits.wards()) {
        List<String> words = VnText.words(w.name());
        keys.put(w.code(), new Keys(String.join(" ", words), String.join("", plainWords(words))));
      }
      return Map.copyOf(keys);
    }
  }

  static Optional<Ward> findWard(String name, List<Ward> pool) {
    List<Scored> exact = score(name, pool).stream().filter(s -> s.tier() % 4 <= 1).toList();
    if (exact.isEmpty()) return Optional.empty();
    int best = exact.stream().mapToInt(Scored::tier).min().getAsInt();
    List<Scored> top = exact.stream().filter(s -> s.tier() == best).toList();
    return top.size() == 1 ? Optional.of(top.get(0).ward()) : Optional.empty();
  }

  static List<Ward> searchWards(String query, List<Ward> pool) {
    return score(query, pool).stream()
        .sorted(Comparator.comparingInt(Scored::tier).thenComparing(s -> s.ward().code()))
        .map(Scored::ward)
        .toList();
  }

  private static List<Scored> score(String query, List<Ward> pool) {
    List<Reading> readings = readings(query);
    List<Scored> out = new ArrayList<>();
    if (readings.isEmpty()) return out;
    for (Ward w : pool) {
      Keys k = WardKeys.BY_CODE.get(w.code());
      int best = Integer.MAX_VALUE;
      for (Reading r : readings) {
        int tier = tier(r, k);
        if (tier < 0) continue;
        if (r.type() != null && r.type() != w.type()) tier += 4;
        best = Math.min(best, tier);
      }
      if (best != Integer.MAX_VALUE) out.add(new Scored(w, best));
    }
    return out;
  }

  private static int tier(Reading r, Keys k) {
    if (r.accented().equals(k.accented())) return 0;
    if (r.plain().equals(k.plain())) return 1;
    if (r.plain().length() < 2) return -1;
    if (k.plain().startsWith(r.plain())) return 2;
    if (k.plain().contains(r.plain())) return 3;
    return -1;
  }

  /** The query as written, plus the query without a leading unit type ("Phường", "Xã"...). */
  private static List<Reading> readings(String query) {
    List<String> words = VnText.words(query);
    List<String> plain = plainWords(words);
    List<Reading> out = new ArrayList<>();
    if (words.isEmpty()) return out;
    out.add(new Reading(String.join(" ", words), String.join("", plain), null));
    for (Prefix p : WARD_PREFIXES) {
      if (startsWith(plain, p.words()) && plain.size() > p.words().size()) {
        int n = p.words().size();
        out.add(new Reading(
            String.join(" ", words.subList(n, words.size())),
            String.join("", plain.subList(n, plain.size())),
            p.type()));
        break;
      }
    }
    return out;
  }

  // ---------- Helpers ----------

  private static List<String> plainWords(List<String> words) {
    return words.stream().map(VnText::plain).toList();
  }

  private static boolean startsWith(List<String> words, List<String> prefix) {
    return words.size() >= prefix.size() && words.subList(0, prefix.size()).equals(prefix);
  }
}
