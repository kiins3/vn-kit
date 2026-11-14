package io.github.kiins3.vnkit;

import io.github.kiins3.vnkit.Names.Prefix;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Vietnam's administrative units as they were before 1 July 2025: 63 provinces, 696 districts and
 * 10,035 wards, communes and towns. Use it to read old addresses; {@link AddressConverter} turns
 * them into current ones.
 *
 * <p>Names are matched as in {@link AdminUnits}. The old data is loaded on first use, separately
 * from the current data.
 */
public final class LegacyUnits {

  private LegacyUnits() {}

  /** The 63 old provinces, ordered by code. */
  public static List<LegacyProvince> provinces() {
    return Data.get().provinces;
  }

  /** The old province with this code. */
  public static Optional<LegacyProvince> province(String code) {
    return Optional.ofNullable(code).map(c -> Data.get().provincesByCode.get(c.trim()));
  }

  /** The old district with this code. */
  public static Optional<LegacyDistrict> district(String code) {
    return Optional.ofNullable(code).map(c -> Data.get().districtsByCode.get(c.trim()));
  }

  /** The old ward, commune or town with this code. */
  public static Optional<LegacyWard> ward(String code) {
    return Optional.ofNullable(code).map(c -> Data.get().wardsByCode.get(c.trim()));
  }

  /** The districts of an old province, ordered by code. */
  public static List<LegacyDistrict> districtsOf(String provinceCode) {
    return Data.get().districtsByProvince.getOrDefault(provinceCode, List.of());
  }

  /** The wards of an old district, ordered by code. */
  public static List<LegacyWard> wardsOf(String districtCode) {
    return Data.get().wardsByDistrict.getOrDefault(districtCode, List.of());
  }

  /**
   * Finds an old province by name, abbreviation or code: {@code "Bình Dương"},
   * {@code "Tỉnh Bà Rịa - Vũng Tàu"}, {@code "BRVT"}, {@code "Thừa Thiên Huế"}.
   */
  public static Optional<LegacyProvince> findProvince(String text) {
    return Optional.ofNullable(text).flatMap(t -> NameMatcher.lookup(t, Data.get().provinceIndex, LegacyUnits::province));
  }

  /**
   * Finds an old district by name inside an old province (a code or a name). Accepts the usual
   * short forms: {@code "Q.1"}, {@code "Q1"}, {@code "Quận 1"}, {@code "H. Củ Chi"}, {@code "TX Kỳ Anh"}.
   * Empty when nothing matches or the name is ambiguous: Hà Tĩnh had both Huyện and Thị xã Kỳ Anh.
   */
  public static Optional<LegacyDistrict> findDistrict(String name, String province) {
    if (name == null || province == null) return Optional.empty();
    return findProvince(province).flatMap(p -> Names.unique(scoreDistricts(name, p.districts())));
  }

  /**
   * Finds an old ward by name, its district and its province. The district may be null when
   * the ward's name is unique in the province.
   */
  public static Optional<LegacyWard> findWard(String name, String district, String province) {
    if (name == null || province == null) return Optional.empty();
    return findProvince(province).flatMap(p -> Names.unique(scoreWards(name, wardPool(p, district))));
  }

  // ---------- Matching ----------

  private static final List<Prefix> DISTRICT_PREFIXES = List.of(
      Prefix.of("thanh pho", "thanh pho"),
      Prefix.of("thi xa", "thi xa"),
      Prefix.of("quan", "quan"),
      Prefix.of("huyen", "huyen"),
      Prefix.of("tp", "thanh pho"),
      Prefix.of("tx", "thi xa"),
      Prefix.of("q", "quan"),
      Prefix.of("h", "huyen"));

  private static final List<Prefix> WARD_PREFIXES = List.of(
      Prefix.of("thi tran", "thi tran"),
      Prefix.of("phuong", "phuong"),
      Prefix.of("xa", "xa"),
      Prefix.of("tt", "thi tran"),
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
      "brvt", "77",
      "thuathienhue", "46");

  static List<Names.Scored<LegacyDistrict>> scoreDistricts(String name, List<LegacyDistrict> pool) {
    Data d = Data.get();
    return Names.score(name, pool, x -> d.keys.get("d" + x.code()), x -> d.types.get("d" + x.code()), DISTRICT_PREFIXES);
  }

  static List<Names.Scored<LegacyWard>> scoreWards(String name, List<LegacyWard> pool) {
    Data d = Data.get();
    return Names.score(name, pool, x -> d.keys.get("w" + x.code()), x -> d.types.get("w" + x.code()), WARD_PREFIXES);
  }

  /** Wards of the districts that match exactly, or of the whole province when the district is missing or unknown. */
  static List<LegacyWard> wardPool(LegacyProvince province, String district) {
    if (district != null && !district.isBlank()) {
      List<LegacyDistrict> districts = Names.exact(scoreDistricts(district, province.districts()));
      if (!districts.isEmpty()) return districts.stream().flatMap(x -> x.wards().stream()).toList();
    }
    return province.districts().stream().flatMap(x -> x.wards().stream()).toList();
  }

  // ---------- Data ----------

  private static final class Data {
    final List<LegacyProvince> provinces;
    final Map<String, LegacyProvince> provincesByCode = new HashMap<>();
    final Map<String, LegacyDistrict> districtsByCode = new HashMap<>();
    final Map<String, LegacyWard> wardsByCode = new HashMap<>();
    final Map<String, List<LegacyDistrict>> districtsByProvince = new LinkedHashMap<>();
    final Map<String, List<LegacyWard>> wardsByDistrict = new LinkedHashMap<>();
    final Map<String, LegacyProvince> provinceIndex = new HashMap<>();
    /** Name keys and type keys, by "d" + district code and "w" + ward code. */
    final Map<String, Names.Key> keys = new HashMap<>();
    final Map<String, String> types = new HashMap<>();

    private Data() {
      List<LegacyProvince> ps = new ArrayList<>();
      Registry.read("legacy_provinces.tsv", 4, f -> ps.add(new LegacyProvince(f[0], f[1], f[2], f[3])));
      provinces = List.copyOf(ps);
      for (LegacyProvince p : provinces) {
        provincesByCode.put(p.code(), p);
        provinceIndex.put(VnText.compact(p.name()), p);
        provinceIndex.put(VnText.compact(p.fullName()), p);
      }
      PROVINCE_ALIASES.forEach((alias, code) -> provinceIndex.put(alias, provincesByCode.get(code)));

      Registry.read("legacy_districts.tsv", 6, f -> {
        LegacyDistrict d = new LegacyDistrict(f[0], f[1], f[2], f[3], f[4], codes(f[5]));
        districtsByCode.put(d.code(), d);
        districtsByProvince.computeIfAbsent(d.provinceCode(), k -> new ArrayList<>()).add(d);
        keys.put("d" + d.code(), Names.key(d.name()));
        types.put("d" + d.code(), Names.typeKey(d.type()));
      });
      Registry.read("legacy_wards.tsv", 7, f -> {
        LegacyWard w = new LegacyWard(f[0], f[1], f[2], f[3], f[4], f[5], codes(f[6]));
        wardsByCode.put(w.code(), w);
        wardsByDistrict.computeIfAbsent(w.districtCode(), k -> new ArrayList<>()).add(w);
        keys.put("w" + w.code(), Names.key(w.name()));
        types.put("w" + w.code(), Names.typeKey(w.type()));
      });
      districtsByProvince.replaceAll((k, v) -> List.copyOf(v));
      wardsByDistrict.replaceAll((k, v) -> List.copyOf(v));
    }

    private static List<String> codes(String field) {
      return field.isEmpty() ? List.of() : Arrays.asList(field.split(","));
    }

    static Data get() {
      return Holder.INSTANCE;
    }

    private static final class Holder {
      static final Data INSTANCE = new Data();
    }
  }
}
