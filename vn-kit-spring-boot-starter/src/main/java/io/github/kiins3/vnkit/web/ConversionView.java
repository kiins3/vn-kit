package io.github.kiins3.vnkit.web;

import io.github.kiins3.vnkit.Conversion;
import io.github.kiins3.vnkit.LegacyWard;
import io.github.kiins3.vnkit.Province;
import io.github.kiins3.vnkit.Ward;
import java.util.List;

/**
 * JSON form of a {@link Conversion}.
 *
 * @param address the address in the current form
 * @param oldAddress the address as it was, with full unit names
 * @param street what came before the ward in the input
 * @param ward the current ward; for a divided old ward, the one that got most of it
 * @param province the current province
 * @param exact whether the old ward went whole into {@code ward}
 * @param candidates every current ward the old ward went into, the main one first
 * @param oldWardCode code of the old ward, or null for an island district
 * @param oldDistrictCode code of the old district
 * @param oldProvinceCode code of the old province
 */
public record ConversionView(
    String address,
    String oldAddress,
    String street,
    Ward ward,
    Province province,
    boolean exact,
    List<Ward> candidates,
    String oldWardCode,
    String oldDistrictCode,
    String oldProvinceCode) {

  static ConversionView of(Conversion c) {
    return new ConversionView(
        c.address(), c.oldAddress(), c.street(), c.ward(), c.province(), c.exact(), c.candidates(),
        c.oldWard().map(LegacyWard::code).orElse(null), c.oldDistrict().code(), c.oldProvince().code());
  }
}
