package io.github.kiins3.vnkit.web;

import io.github.kiins3.vnkit.AddressConverter;
import io.github.kiins3.vnkit.AdminUnits;
import io.github.kiins3.vnkit.Conversion;
import io.github.kiins3.vnkit.Province;
import io.github.kiins3.vnkit.Ward;
import java.util.List;
import java.util.Optional;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/**
 * Read-only endpoints for address pickers:
 *
 * <ul>
 *   <li>{@code GET /api/vn/provinces} all provinces and centrally run cities
 *   <li>{@code GET /api/vn/provinces/{code}} one province
 *   <li>{@code GET /api/vn/provinces/{code}/wards} its wards, communes and special zones
 *   <li>{@code GET /api/vn/wards/{code}} one ward
 *   <li>{@code GET /api/vn/wards?q=ben thanh&province=79&limit=10} search by name
 *   <li>{@code GET /api/vn/convert?address=P. Dịch Vọng, Q. Cầu Giấy, Hà Nội} convert an old address,
 *       or {@code ?ward=...&district=...&province=...} with separate fields
 * </ul>
 */
@RestController
@RequestMapping("${vnkit.web.base-path:/api/vn}")
public class AdminUnitsController {

  private final VnKitWebProperties properties;

  public AdminUnitsController(VnKitWebProperties properties) {
    this.properties = properties;
  }

  @GetMapping("/provinces")
  public ResponseEntity<List<Province>> provinces() {
    return cached(AdminUnits.provinces());
  }

  @GetMapping("/provinces/{code}")
  public ResponseEntity<Province> province(@PathVariable String code) {
    return cached(AdminUnits.province(code).orElseThrow(() -> notFound("province", code)));
  }

  @GetMapping("/provinces/{code}/wards")
  public ResponseEntity<List<Ward>> wardsOf(@PathVariable String code) {
    Province province = AdminUnits.province(code).orElseThrow(() -> notFound("province", code));
    return cached(province.wards());
  }

  @GetMapping("/wards/{code}")
  public ResponseEntity<Ward> ward(@PathVariable String code) {
    return cached(AdminUnits.ward(code).orElseThrow(() -> notFound("ward", code)));
  }

  @GetMapping("/wards")
  public ResponseEntity<List<Ward>> searchWards(
      @RequestParam String q,
      @RequestParam(required = false) String province,
      @RequestParam(defaultValue = "20") int limit) {
    if (q.isBlank()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "q must not be blank");
    if (province != null && AdminUnits.findProvince(province).isEmpty()) throw notFound("province", province);
    List<Ward> found = province == null ? AdminUnits.searchWards(q) : AdminUnits.searchWards(q, province);
    int max = Math.max(1, Math.min(limit, properties.getMaxResults()));
    return cached(found.subList(0, Math.min(max, found.size())));
  }

  @GetMapping("/convert")
  public ResponseEntity<ConversionView> convert(
      @RequestParam(required = false) String address,
      @RequestParam(required = false) String ward,
      @RequestParam(required = false) String district,
      @RequestParam(required = false) String province) {
    Optional<Conversion> result;
    if (address != null && !address.isBlank()) {
      result = AddressConverter.convert(address);
    } else if (province != null && !province.isBlank()) {
      result = AddressConverter.convert(ward, district, province);
    } else {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Give an address, or a province with a ward and district");
    }
    Conversion c = result.orElseThrow(() ->
        new ResponseStatusException(HttpStatus.NOT_FOUND, "Could not identify the old ward; check the names or add the district"));
    return cached(ConversionView.of(c));
  }

  private <T> ResponseEntity<T> cached(T body) {
    return ResponseEntity.ok().cacheControl(CacheControl.maxAge(properties.getCacheMaxAge()).cachePublic()).body(body);
  }

  private static ResponseStatusException notFound(String what, String code) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "No " + what + " with code " + code);
  }
}
