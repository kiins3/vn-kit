package io.github.kiins3.vnkit;

import java.util.Optional;

/**
 * A Vietnamese tax code (mã số thuế).
 *
 * <pre>{@code
 * TaxCode.parse("0100109106").map(TaxCode::type);         // MAIN
 * TaxCode.parse("0100109106 - 001").map(TaxCode::value);  // "0100109106-001"
 * TaxCode.parse("0100109107");                            // empty: wrong check digit
 * }</pre>
 *
 * Ten-digit codes, and the first ten digits of thirteen-digit codes, end with a check digit,
 * which is verified. Twelve-digit codes must be well-formed citizen IDs (see {@link CitizenId}).
 */
public final class TaxCode {

  private static final int[] WEIGHTS = {31, 29, 23, 19, 17, 13, 7, 5, 3};

  private final String value;
  private final TaxCodeType type;

  private TaxCode(String value, TaxCodeType type) {
    this.value = value;
    this.type = type;
  }

  /**
   * The tax code, or empty if the text is not a valid one. Spaces are ignored, and the hyphen in
   * a dependent unit's code is optional.
   */
  public static Optional<TaxCode> parse(CharSequence text) {
    if (text == null) return Optional.empty();
    String t = text.toString().replaceAll("\\s+", "");
    if (t.matches("\\d{10}")) {
      return checks(t) ? Optional.of(new TaxCode(t, TaxCodeType.MAIN)) : Optional.empty();
    }
    if (t.matches("\\d{10}-?\\d{3}")) {
      String head = t.substring(0, 10);
      String unit = t.substring(t.length() - 3);
      return checks(head) && !unit.equals("000")
          ? Optional.of(new TaxCode(head + "-" + unit, TaxCodeType.DEPENDENT))
          : Optional.empty();
    }
    if (t.matches("\\d{12}")) {
      return CitizenId.parse(t).map(id -> new TaxCode(t, TaxCodeType.PERSONAL_ID));
    }
    return Optional.empty();
  }

  /** Whether the text is a valid tax code. */
  public static boolean isValid(CharSequence text) {
    return parse(text).isPresent();
  }

  /** Whether the tenth digit matches the first nine. */
  static boolean checks(String tenDigits) {
    int sum = 0;
    for (int i = 0; i < 9; i++) sum += WEIGHTS[i] * (tenDigits.charAt(i) - '0');
    return 10 - sum % 11 == tenDigits.charAt(9) - '0';
  }

  /** The code in its usual form: {@code "0100109106"}, {@code "0100109106-001"} or twelve digits. */
  public String value() {
    return value;
  }

  public TaxCodeType type() {
    return type;
  }

  /** For a dependent unit, the ten-digit code of the taxpayer it depends on. */
  public Optional<TaxCode> parent() {
    return type == TaxCodeType.DEPENDENT
        ? Optional.of(new TaxCode(value.substring(0, 10), TaxCodeType.MAIN))
        : Optional.empty();
  }

  /** For a personal identification number, the citizen ID it is. */
  public Optional<CitizenId> citizenId() {
    return type == TaxCodeType.PERSONAL_ID ? CitizenId.parse(value) : Optional.empty();
  }

  @Override
  public boolean equals(Object o) {
    return o instanceof TaxCode c && c.value.equals(value);
  }

  @Override
  public int hashCode() {
    return value.hashCode();
  }

  @Override
  public String toString() {
    return value;
  }
}
