package io.github.kiins3.vnkit;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts addresses written before 1 July 2025 (province, district, ward) to the current units
 * (province, ward).
 *
 * <pre>{@code
 * AddressConverter.convert("12 Trần Thái Tông, P. Dịch Vọng, Q. Cầu Giấy, Hà Nội")
 *     .map(Conversion::address);
 * // "12 Trần Thái Tông, Phường Cầu Giấy, Thành phố Hà Nội"
 *
 * AddressConverter.convertProvince("Bình Dương");   // Thành phố Hồ Chí Minh
 * }</pre>
 *
 * Every method returns empty rather than guess when the old unit cannot be identified, for
 * example when a ward name occurs in two districts and the district is missing. When an old ward
 * was split between several current units, the result says so; see {@link Conversion#exact()}.
 */
public final class AddressConverter {

  private AddressConverter() {}

  /** The current province an old province became part of: {@code "Bà Rịa - Vũng Tàu"} gives Hồ Chí Minh. */
  public static Optional<Province> convertProvince(String oldProvince) {
    return LegacyUnits.findProvince(oldProvince).map(LegacyProvince::newProvince);
  }

  /** Converts an old ward by its code before the reform. */
  public static Optional<Conversion> convertCode(String oldWardCode) {
    return LegacyUnits.ward(oldWardCode).map(w -> of("", w));
  }

  /**
   * Converts an old address given as separate fields, as stored in most databases. The district
   * may be null when the ward's name is unique in the province. The ward may be null only for
   * the island districts that had no wards (Côn Đảo, Lý Sơn, Bạch Long Vĩ, Cồn Cỏ, Hoàng Sa).
   */
  public static Optional<Conversion> convert(String ward, String district, String province) {
    if (province == null) return Optional.empty();
    Optional<LegacyProvince> p = LegacyUnits.findProvince(province);
    if (p.isEmpty()) return Optional.empty();
    if (ward != null && !ward.isBlank()) {
      Optional<Conversion> c = findWard(ward, district, p.get()).map(w -> of("", w));
      if (c.isPresent()) return c;
    }
    return island(district, p.get(), "");
  }

  /**
   * Converts an old address written on one line, parts separated by commas:
   * {@code "[house number and street,] ward, [district,] province[, Việt Nam]"}. Common short
   * forms are understood: {@code "P.12, Q.Tân Bình, TP.HCM"}, {@code "F.7, Q3"}, {@code "TT Đông Anh"}.
   * Whatever comes before the ward is kept as the street.
   */
  public static Optional<Conversion> convert(String address) {
    if (address == null) return Optional.empty();
    List<String> parts = parts(address);
    if (parts.size() < 2) return Optional.empty();
    Optional<LegacyProvince> found = LegacyUnits.findProvince(parts.get(parts.size() - 1));
    if (found.isEmpty()) return Optional.empty();
    LegacyProvince province = found.get();
    List<String> rest = parts.subList(0, parts.size() - 1);
    int n = rest.size();

    // ward, district, province
    if (n >= 2 && !Names.exact(LegacyUnits.scoreDistricts(rest.get(n - 1), province.districts())).isEmpty()) {
      Optional<Conversion> c = withStreet(rest.subList(0, n - 2), rest.get(n - 2), rest.get(n - 1), province);
      if (c.isPresent()) return c;
    }
    // island district, province
    Optional<Conversion> c = island(rest.get(n - 1), province, String.join(", ", rest.subList(0, n - 1)));
    if (c.isPresent()) return c;
    // ward, province
    return withStreet(rest.subList(0, n - 1), rest.get(n - 1), null, province);
  }

  // ---------- Helpers ----------

  private static Conversion of(String street, LegacyWard w) {
    return new Conversion(street, w.province(), w.district(), w, w.newWards());
  }

  private static Optional<LegacyWard> findWard(String ward, String district, LegacyProvince province) {
    return Names.unique(LegacyUnits.scoreWards(ward, LegacyUnits.wardPool(province, district)));
  }

  private static Optional<Conversion> island(String district, LegacyProvince province, String street) {
    if (district == null || district.isBlank()) return Optional.empty();
    return Names.unique(LegacyUnits.scoreDistricts(district, province.districts()))
        .filter(d -> !d.newWardCodes().isEmpty())
        .map(d -> new Conversion(street, province, d, null,
            d.newWardCodes().stream().map(code -> AdminUnits.ward(code).orElseThrow()).toList()));
  }

  /** Resolves the ward part; if it does not match as a whole, tries it without a leading street. */
  private static Optional<Conversion> withStreet(List<String> streetParts, String wardPart, String district, LegacyProvince province) {
    List<String> street = new ArrayList<>(streetParts);
    Optional<LegacyWard> w = findWard(wardPart, district, province);
    if (w.isEmpty()) {
      // "123 Lê Lợi P. Bến Thành": the street and the ward in one part.
      List<Integer> starts = wardStarts(wardPart);
      for (int i = starts.size() - 1; i >= 0 && w.isEmpty(); i--) {
        int at = starts.get(i);
        w = findWard(wardPart.substring(at), district, province);
        if (w.isPresent()) street.add(wardPart.substring(0, at).replaceAll("[\\s,;-]+$", ""));
      }
    }
    String s = String.join(", ", street.stream().filter(x -> !x.isBlank()).toList());
    return w.map(ward -> of(s, ward));
  }

  private static final Pattern WARD_START =
      Pattern.compile("(?<![\\p{L}\\d])(phuong|thi tran|xa|p\\.|tt\\.|f\\.|p(?=\\d)|f(?=\\d)|p |tt |x\\.)");

  /** Where a ward type ("Phường", "P.", "Xã", "TT"...) begins inside a part. */
  private static List<Integer> wardStarts(String part) {
    String plain = VnText.removeAccents(part).toLowerCase(Locale.ROOT);
    List<Integer> starts = new ArrayList<>();
    if (plain.length() != part.length()) return starts; // positions would not line up
    Matcher m = WARD_START.matcher(plain);
    while (m.find()) if (m.start() > 0) starts.add(m.start());
    return starts;
  }

  private static final Set<String> COUNTRY = Set.of("vietnam", "vn", "vietnamese");

  private static List<String> parts(String address) {
    String nfc = Normalizer.normalize(address, Normalizer.Form.NFC);
    List<String> parts = new ArrayList<>();
    for (String p : nfc.split("[,;\\n]")) {
      String t = p.trim();
      if (!t.isEmpty()) parts.add(t);
    }
    while (!parts.isEmpty() && COUNTRY.contains(VnText.compact(parts.get(parts.size() - 1)))) {
      parts.remove(parts.size() - 1);
    }
    return parts;
  }
}
