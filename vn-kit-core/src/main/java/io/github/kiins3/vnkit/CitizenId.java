package io.github.kiins3.vnkit;

import java.time.Year;
import java.time.ZoneId;
import java.util.Optional;

/**
 * A twelve-digit personal identification number (số định danh cá nhân), the number on the
 * căn cước and căn cước công dân cards. Since 1 July 2025 it is also an individual's tax code.
 *
 * <pre>{@code
 * CitizenId id = CitizenId.parse("079090001234").orElseThrow();
 * id.birthProvince().name();   // "Hồ Chí Minh"
 * id.gender();                 // MALE
 * id.birthYear();              // 1990
 * id.masked();                 // "079*******34"
 * }</pre>
 *
 * The number encodes where the birth was registered (digits 1-3), gender and century (digit 4)
 * and the last two digits of the birth year (digits 5-6). The province code comes from the list
 * of 63 provinces used before the 2025 reform; codes of current provinces are a subset of it.
 * Only the structure can be checked, not whether a number was actually issued. The nine-digit
 * chứng minh nhân dân numbers are not accepted.
 */
public final class CitizenId {

  private static final ZoneId VIETNAM = ZoneId.of("Asia/Ho_Chi_Minh");

  private final String number;
  private final LegacyProvince birthProvince;
  private final Gender gender;
  private final int birthYear;

  private CitizenId(String number, LegacyProvince birthProvince, Gender gender, int birthYear) {
    this.number = number;
    this.birthProvince = birthProvince;
    this.gender = gender;
    this.birthYear = birthYear;
  }

  /** The number, or empty if the text is not a well-formed citizen ID. Spaces are ignored. */
  public static Optional<CitizenId> parse(CharSequence text) {
    return parse(text, Year.now(VIETNAM).getValue());
  }

  /** Whether the text is a well-formed citizen ID. */
  public static boolean isValid(CharSequence text) {
    return parse(text).isPresent();
  }

  static Optional<CitizenId> parse(CharSequence text, int currentYear) {
    if (text == null) return Optional.empty();
    String n = text.toString().replaceAll("\\s+", "");
    if (!n.matches("0\\d{11}")) return Optional.empty();
    Optional<LegacyProvince> province = LegacyUnits.province(n.substring(1, 3));
    if (province.isEmpty()) return Optional.empty();
    int century = n.charAt(3) - '0';
    int year = 1900 + century / 2 * 100 + Integer.parseInt(n.substring(4, 6));
    if (year > currentYear) return Optional.empty();
    return Optional.of(new CitizenId(n, province.get(), century % 2 == 0 ? Gender.MALE : Gender.FEMALE, year));
  }

  /** The twelve digits. */
  public String number() {
    return number;
  }

  /** The province where the birth was registered, as it was before the 2025 reform. */
  public LegacyProvince birthProvince() {
    return birthProvince;
  }

  /** The current province that the birth province is now part of. */
  public Province province() {
    return birthProvince.newProvince();
  }

  public Gender gender() {
    return gender;
  }

  public int birthYear() {
    return birthYear;
  }

  /** For logs and screens: {@code "079*******34"}. */
  public String masked() {
    return number.substring(0, 3) + "*******" + number.substring(10);
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof CitizenId c && c.number.equals(number);
  }

  @Override
  public int hashCode() {
    return number.hashCode();
  }

  /** The masked number, so IDs do not end up in logs by accident; {@link #number()} has the digits. */
  @Override
  public String toString() {
    return masked();
  }
}
