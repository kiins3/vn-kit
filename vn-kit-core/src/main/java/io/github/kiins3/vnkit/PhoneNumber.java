package io.github.kiins3.vnkit;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * A Vietnamese phone number, mobile or fixed line.
 *
 * <pre>{@code
 * PhoneNumber.parse("+84 912-345-678").map(PhoneNumber::national);   // "0912345678"
 * PhoneNumber.parse("0951234567").flatMap(PhoneNumber::carrier);     // VIETTEL
 * PhoneNumber.convert("01688123456").map(PhoneNumber::national);     // "0388123456"
 * }</pre>
 *
 * Spaces, dots, hyphens, slashes and brackets are ignored, and the country code may be written
 * as {@code +84}, {@code 84} or {@code 0084}. A mobile number must start with a prefix that has
 * been assigned to a network (see {@link Carrier}); a fixed-line number must have eleven digits
 * and an area code starting {@code 02}.
 */
public final class PhoneNumber {

  private static final Map<String, Carrier> CARRIERS = new HashMap<>();

  static {
    for (Carrier c : Carrier.values()) for (String p : c.prefixes()) CARRIERS.put(p, c);
  }

  /** Mobile prefixes from before the switch to ten digits in 2018. */
  private static final Map<String, String> OLD_PREFIXES = Map.ofEntries(
      // Viettel
      Map.entry("0162", "032"), Map.entry("0163", "033"), Map.entry("0164", "034"), Map.entry("0165", "035"),
      Map.entry("0166", "036"), Map.entry("0167", "037"), Map.entry("0168", "038"), Map.entry("0169", "039"),
      // MobiFone
      Map.entry("0120", "070"), Map.entry("0121", "079"), Map.entry("0122", "077"), Map.entry("0126", "076"),
      Map.entry("0128", "078"),
      // VinaPhone
      Map.entry("0123", "083"), Map.entry("0124", "084"), Map.entry("0125", "085"), Map.entry("0127", "081"),
      Map.entry("0129", "082"),
      // Vietnamobile, Gmobile
      Map.entry("0186", "056"), Map.entry("0188", "058"), Map.entry("0199", "059"));

  private final String national;
  private final PhoneType type;

  private PhoneNumber(String national, PhoneType type) {
    this.national = national;
    this.type = type;
  }

  /** The number, or empty if the text is not a valid Vietnamese phone number. */
  public static Optional<PhoneNumber> parse(CharSequence text) {
    String d = digits(text);
    if (d == null) return Optional.empty();
    if (d.length() == 10 && CARRIERS.containsKey(d.substring(0, 3))) return Optional.of(new PhoneNumber(d, PhoneType.MOBILE));
    if (d.length() == 11 && d.startsWith("02")) return Optional.of(new PhoneNumber(d, PhoneType.FIXED));
    return Optional.empty();
  }

  /** Whether the text is a valid Vietnamese phone number. */
  public static boolean isValid(CharSequence text) {
    return parse(text).isPresent();
  }

  /**
   * Like {@link #parse}, but also accepts an eleven-digit mobile number from before September
   * 2018 and returns it with its current prefix: {@code 0168 812 3456} becomes
   * {@code 0388 123 456}. Useful when cleaning old customer data.
   */
  public static Optional<PhoneNumber> convert(CharSequence text) {
    Optional<PhoneNumber> current = parse(text);
    if (current.isPresent()) return current;
    String d = digits(text);
    if (d == null || d.length() != 11) return Optional.empty();
    String prefix = OLD_PREFIXES.get(d.substring(0, 4));
    return prefix == null ? Optional.empty() : parse(prefix + d.substring(4));
  }

  /** Mobile or fixed line. */
  public PhoneType type() {
    return type;
  }

  /** Digits as dialled inside Vietnam: {@code "0912345678"}. */
  public String national() {
    return national;
  }

  /** International form for SMS gateways and APIs: {@code "+84912345678"}. */
  public String e164() {
    return "+84" + national.substring(1);
  }

  /** Grouped for display: {@code "0912 345 678"}, {@code "024 3825 1234"}, {@code "0236 381 2345"}. */
  public String formatted() {
    String n = national;
    if (type == PhoneType.FIXED && (n.startsWith("024") || n.startsWith("028"))) {
      return n.substring(0, 3) + " " + n.substring(3, 7) + " " + n.substring(7);
    }
    return n.substring(0, 4) + " " + n.substring(4, 7) + " " + n.substring(7);
  }

  /** The network the prefix was assigned to; empty for fixed lines. */
  public Optional<Carrier> carrier() {
    return type == PhoneType.MOBILE ? Optional.of(CARRIERS.get(national.substring(0, 3))) : Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof PhoneNumber p && p.national.equals(national);
  }

  @Override
  public int hashCode() {
    return national.hashCode();
  }

  @Override
  public String toString() {
    return national;
  }

  /** The national digits with a leading zero, or null if the text has anything but digits and separators. */
  private static String digits(CharSequence text) {
    if (text == null) return null;
    StringBuilder sb = new StringBuilder();
    boolean plus = false;
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c >= '0' && c <= '9') sb.append(c);
      else if (c == '+' && !plus && sb.length() == 0) plus = true;
      else if (" .-/()".indexOf(c) < 0 && !Character.isWhitespace(c) && !Character.isSpaceChar(c)) return null;
    }
    String d = sb.toString();
    if (plus) {
      if (!d.startsWith("84")) return null;
      d = d.substring(2);
    } else if (d.startsWith("0084")) {
      d = d.substring(4);
    } else if (d.startsWith("84") && (d.length() == 11 || d.length() == 12)) {
      d = d.substring(2);
    } else {
      return d.startsWith("0") ? d : null;
    }
    return d.startsWith("0") ? d : "0" + d; // "+84 (0) 912..." keeps its zero
  }
}
