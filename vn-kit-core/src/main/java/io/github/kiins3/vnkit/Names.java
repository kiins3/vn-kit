package io.github.kiins3.vnkit;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Matching of names as people type them against unit names, shared by current and old units.
 *
 * <p>A query is read twice: as written, and without a leading unit type ("Phường", "Q.", "Xã"...).
 * Each unit gets a tier for each reading: 0 same words with accents, 1 same without accents,
 * 2 its name starts with the query, 3 its name contains it; plus 4 when the query names another
 * unit type. A unit keeps its best tier.
 */
final class Names {

  private Names() {}

  /** A unit type written before a name, and the type key it stands for (null: any type). */
  record Prefix(List<String> words, String type) {
    static Prefix of(String words, String type) {
      return new Prefix(List.of(words.split(" ")), type);
    }
  }

  /** A unit name ready for comparison. */
  record Key(String accented, String plain) {}

  record Scored<T>(T item, int tier) {}

  private record Reading(String accented, String plain, String type) {}

  /** Short prefixes that people write glued to a number: "Q1", "P12", "F7". */
  private static final Set<String> GLUED = Set.of("q", "p", "f", "quan", "phuong");
  private static final Pattern GLUED_NUMBER = Pattern.compile("([a-z]+)(\\d+)");

  /** Words of a name or query: accents kept, numbers without leading zeros, "q1" split into "q 1". */
  static List<String> words(String text) {
    List<String> out = new ArrayList<>();
    for (String w : VnText.words(text)) {
      Matcher m = GLUED_NUMBER.matcher(w);
      if (m.matches() && GLUED.contains(m.group(1))) {
        out.add(m.group(1));
        out.add(stripZeros(m.group(2)));
      } else {
        out.add(isDigits(w) ? stripZeros(w) : w);
      }
    }
    return out;
  }

  static Key key(String name) {
    List<String> words = words(name);
    return new Key(String.join(" ", words), plainJoined(words, 0));
  }

  /** The key of a unit type label: "Thị trấn" gives "thi tran". */
  static String typeKey(String label) {
    return String.join(" ", words(label).stream().map(VnText::plain).toList());
  }

  static <T> List<Scored<T>> score(
      String query, Collection<T> pool, Function<T, Key> keyOf, Function<T, String> typeOf, List<Prefix> prefixes) {
    List<Reading> readings = readings(query, prefixes);
    List<Scored<T>> out = new ArrayList<>();
    if (readings.isEmpty()) return out;
    for (T item : pool) {
      Key k = keyOf.apply(item);
      int best = Integer.MAX_VALUE;
      for (Reading r : readings) {
        int tier = tier(r, k);
        if (tier < 0) continue;
        if (r.type() != null && !r.type().equals(typeOf.apply(item))) tier += 4;
        best = Math.min(best, tier);
      }
      if (best != Integer.MAX_VALUE) out.add(new Scored<>(item, best));
    }
    return out;
  }

  /** The items that match exactly (tier 0 or 1, either unit type) at the best tier found. */
  static <T> List<T> exact(List<Scored<T>> scored) {
    List<Scored<T>> exact = scored.stream().filter(s -> s.tier() % 4 <= 1).toList();
    if (exact.isEmpty()) return List.of();
    int best = exact.stream().mapToInt(Scored::tier).min().getAsInt();
    return exact.stream().filter(s -> s.tier() == best).map(Scored::item).toList();
  }

  /** The one exact match, or empty when there is none or more than one. */
  static <T> Optional<T> unique(List<Scored<T>> scored) {
    List<T> exact = exact(scored);
    return exact.size() == 1 ? Optional.of(exact.get(0)) : Optional.empty();
  }

  /** All matches, best first; ties keep the pool's order. */
  static <T> List<T> ranked(List<Scored<T>> scored) {
    return scored.stream().sorted(Comparator.comparingInt(Scored::tier)).map(Scored::item).toList();
  }

  private static int tier(Reading r, Key k) {
    if (r.accented().equals(k.accented())) return 0;
    if (r.plain().equals(k.plain())) return 1;
    if (r.plain().length() < 2) return -1;
    if (k.plain().startsWith(r.plain())) return 2;
    if (k.plain().contains(r.plain())) return 3;
    return -1;
  }

  private static List<Reading> readings(String query, List<Prefix> prefixes) {
    List<String> words = words(query);
    List<Reading> out = new ArrayList<>();
    if (words.isEmpty()) return out;
    out.add(new Reading(String.join(" ", words), plainJoined(words, 0), null));
    List<String> plain = words.stream().map(VnText::plain).toList();
    for (Prefix p : prefixes) {
      int n = p.words().size();
      if (plain.size() > n && plain.subList(0, n).equals(p.words())) {
        out.add(new Reading(String.join(" ", words.subList(n, words.size())), plainJoined(words, n), p.type()));
        break;
      }
    }
    return out;
  }

  private static String plainJoined(List<String> words, int from) {
    StringBuilder out = new StringBuilder();
    for (String w : words.subList(from, words.size())) out.append(VnText.plain(w));
    return out.toString();
  }

  private static boolean isDigits(String s) {
    return !s.isEmpty() && s.chars().allMatch(Character::isDigit);
  }

  private static String stripZeros(String digits) {
    String s = digits.replaceFirst("^0+(?=\\d)", "");
    return s;
  }
}
