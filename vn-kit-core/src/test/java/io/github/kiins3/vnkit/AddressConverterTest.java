package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class AddressConverterTest {

  @Nested
  class LegacyData {

    @Test
    void hasThe63ProvincesBeforeTheReform() {
      assertThat(LegacyUnits.provinces()).hasSize(63);
      assertThat(LegacyUnits.provinces().stream().map(LegacyProvince::newProvinceCode).collect(Collectors.toSet()))
          .hasSize(34);
    }

    @Test
    void everyOldWardBecameACurrentUnit() {
      int wards = 0;
      Set<String> reached = new java.util.HashSet<>();
      for (LegacyProvince p : LegacyUnits.provinces()) {
        for (LegacyDistrict d : p.districts()) {
          assertThat(d.province()).isEqualTo(p);
          reached.addAll(d.newWardCodes());
          for (LegacyWard w : d.wards()) {
            wards++;
            assertThat(w.newWards()).isNotEmpty().allSatisfy(n -> assertThat(n.provinceCode()).isEqualTo(p.newProvinceCode()));
            reached.addAll(w.newWardCodes());
          }
        }
      }
      assertThat(wards).isEqualTo(10035);
      assertThat(reached).hasSize(AdminUnits.wards().size());
    }

    @Test
    void everyOldWardIsFoundByItsOwnAddress() {
      for (LegacyProvince p : LegacyUnits.provinces()) {
        for (LegacyDistrict d : p.districts()) {
          for (LegacyWard w : d.wards()) {
            assertThat(AddressConverter.convert(w.fullName(), d.fullName(), p.fullName()))
                .as(w.fullName() + ", " + d.fullName() + ", " + p.fullName())
                .get().extracting(c -> c.oldWard().orElseThrow()).isEqualTo(w);
            assertThat(AddressConverter.convert(w.fullName() + ", " + d.fullName() + ", " + p.fullName()))
                .as("one line: " + w.fullName())
                .get().extracting(c -> c.oldWard().orElseThrow()).isEqualTo(w);
          }
        }
      }
    }

    @Test
    void islandDistrictsHadNoWards() {
      assertThat(LegacyUnits.provinces().stream().flatMap(p -> p.districts().stream())
              .filter(d -> d.wards().isEmpty()).map(LegacyDistrict::name))
          .containsExactlyInAnyOrder("Bạch Long Vĩ", "Cồn Cỏ", "Hoàng Sa", "Lý Sơn", "Côn Đảo");
    }

    @Test
    void codesOfTheSameNameCanChange() {
      assertThat(LegacyUnits.findProvince("Bắc Ninh")).get().extracting(LegacyProvince::code).isEqualTo("27");
      assertThat(AdminUnits.findProvince("Bắc Ninh")).get().extracting(Province::code).isEqualTo("24");
    }

    @Test
    void districtsWrittenShort() {
      assertThat(LegacyUnits.findDistrict("Q.1", "TP.HCM")).get().extracting(LegacyDistrict::fullName).isEqualTo("Quận 1");
      assertThat(LegacyUnits.findDistrict("Q1", "hcm")).get().extracting(LegacyDistrict::fullName).isEqualTo("Quận 1");
      assertThat(LegacyUnits.findDistrict("H. Củ Chi", "Hồ Chí Minh")).get().extracting(LegacyDistrict::type).isEqualTo("Huyện");
    }

    @Test
    void districtNamesSharedByTwoTypes() {
      // Hà Tĩnh had Huyện Kỳ Anh and Thị xã Kỳ Anh.
      assertThat(LegacyUnits.findDistrict("Kỳ Anh", "Hà Tĩnh")).isEmpty();
      assertThat(LegacyUnits.findDistrict("TX Kỳ Anh", "Hà Tĩnh")).get().extracting(LegacyDistrict::code).isEqualTo("449");
      assertThat(LegacyUnits.findDistrict("Huyện Kỳ Anh", "Hà Tĩnh")).get().extracting(LegacyDistrict::code).isEqualTo("447");
    }
  }

  @Nested
  class Provinces {

    @ParameterizedTest
    @CsvSource({
        "Bình Dương, 79",
        "Tỉnh Bà Rịa - Vũng Tàu, 79",
        "BRVT, 79",
        "Hà Giang, 08",
        "Quảng Nam, 48",
        "Thừa Thiên Huế, 46",
        "Hà Nội, 01",
        "tinh hoa binh, 25",
        "Bắc Ninh, 24",
        "Bắc Giang, 24"})
    void mergedInto(String old, String current) {
      assertThat(AddressConverter.convertProvince(old)).get().extracting(Province::code).isEqualTo(current);
    }

    @Test
    void unknown() {
      assertThat(AddressConverter.convertProvince("Atlantis")).isEmpty();
      assertThat(AddressConverter.convertProvince(null)).isEmpty();
    }
  }

  @Nested
  class Fields {

    @Test
    void wardThatWentWhole() {
      Conversion c = AddressConverter.convert("Phường 12", "Quận Tân Bình", "TP. Hồ Chí Minh").orElseThrow();
      assertThat(c.exact()).isTrue();
      assertThat(c.ward().fullName()).isEqualTo("Phường Bảy Hiền");
      assertThat(c.province().code()).isEqualTo("79");
      assertThat(c.oldWard()).get().extracting(LegacyWard::code).isEqualTo("26971");
      assertThat(c.address()).isEqualTo("Phường Bảy Hiền, Thành phố Hồ Chí Minh");
    }

    @Test
    void dividedWard() {
      Conversion c = AddressConverter.convert("Dịch Vọng", "Cầu Giấy", "Hà Nội").orElseThrow();
      assertThat(c.exact()).isFalse();
      assertThat(c.ward().fullName()).isEqualTo("Phường Cầu Giấy");
      assertThat(c.candidates()).extracting(Ward::fullName).containsExactly("Phường Cầu Giấy", "Phường Nghĩa Đô");
      assertThat(c.toString()).contains("divided: 2 candidates");
    }

    @Test
    void numberedWardsNeedTheirDistrict() {
      // Ten districts of Hồ Chí Minh City had a Phường 1.
      assertThat(AddressConverter.convert("Phường 1", null, "TP.HCM")).isEmpty();
      assertThat(AddressConverter.convert("Phường 01", "Quận 3", "TP.HCM")).get()
          .extracting(c -> c.ward().fullName()).isEqualTo("Phường Bàn Cờ");
    }

    @Test
    void districtIsOptionalWhenTheWardNameIsUnique() {
      assertThat(AddressConverter.convert("Phường Bến Thành", null, "Hồ Chí Minh")).get()
          .extracting(c -> c.ward().code()).isEqualTo("26743");
    }

    @Test
    void ambiguousDistrictIsSettledByTheWard() {
      // Kỳ Trinh was in Thị xã Kỳ Anh, not Huyện Kỳ Anh.
      assertThat(AddressConverter.convert("Kỳ Trinh", "Kỳ Anh", "Hà Tĩnh")).get()
          .extracting(c -> c.oldDistrict().code()).isEqualTo("449");
    }

    @Test
    void islandDistrictBecameASpecialZone() {
      Conversion c = AddressConverter.convert(null, "Huyện Côn Đảo", "Bà Rịa - Vũng Tàu").orElseThrow();
      assertThat(c.ward().fullName()).isEqualTo("Đặc khu Côn Đảo");
      assertThat(c.oldWard()).isEmpty();
      assertThat(c.province().code()).isEqualTo("79");
    }

    @Test
    void townsUnderTheirOldType() {
      Conversion c = AddressConverter.convert("TT Đông Anh", "Đông Anh", "Hà Nội").orElseThrow();
      assertThat(c.oldWard()).get().extracting(LegacyWard::type).isEqualTo("Thị trấn");
      assertThat(c.candidates()).extracting(Ward::fullName).contains("Xã Đông Anh");
    }

    @Test
    void byOldCode() {
      assertThat(AddressConverter.convertCode("26971")).get().extracting(c -> c.ward().fullName()).isEqualTo("Phường Bảy Hiền");
      assertThat(AddressConverter.convertCode("99999")).isEmpty();
    }

    @Test
    void nothingRatherThanAGuess() {
      assertThat(AddressConverter.convert("Không Có", "Cầu Giấy", "Hà Nội")).isEmpty();
      assertThat(AddressConverter.convert("Dịch Vọng", "Cầu Giấy", "Atlantis")).isEmpty();
      assertThat(AddressConverter.convert(null, "Cầu Giấy", "Hà Nội")).isEmpty();
      assertThat(AddressConverter.convert("Dịch Vọng", "Cầu Giấy", null)).isEmpty();
    }
  }

  @Nested
  class OneLine {

    @ParameterizedTest
    @ValueSource(strings = {
        "Phường 12, Quận Tân Bình, Thành phố Hồ Chí Minh",
        "P.12, Q.Tân Bình, TP.HCM",
        "p12, q tan binh, tphcm",
        "F12, Tân Bình, Sài Gòn",
        "Phường 12, Quận Tân Bình, TP. Hồ Chí Minh, Việt Nam"})
    void writtenInManyWays(String address) {
      assertThat(AddressConverter.convert(address)).get().extracting(c -> c.ward().fullName()).isEqualTo("Phường Bảy Hiền");
    }

    @Test
    void keepsTheStreet() {
      Conversion c = AddressConverter.convert("Số 12 ngõ 3, Trần Thái Tông, P. Dịch Vọng, Q. Cầu Giấy, Hà Nội").orElseThrow();
      assertThat(c.street()).isEqualTo("Số 12 ngõ 3, Trần Thái Tông");
      assertThat(c.address()).isEqualTo("Số 12 ngõ 3, Trần Thái Tông, Phường Cầu Giấy, Thành phố Hà Nội");
      assertThat(c.oldAddress()).isEqualTo("Số 12 ngõ 3, Trần Thái Tông, Phường Dịch Vọng, Quận Cầu Giấy, Thành phố Hà Nội");
    }

    @Test
    void streetAndWardInOnePart() {
      Conversion c = AddressConverter.convert("135 Lê Lợi P. Bến Thành, Quận 1, TP.HCM").orElseThrow();
      assertThat(c.street()).isEqualTo("135 Lê Lợi");
      assertThat(c.ward().code()).isEqualTo("26743");
    }

    @Test
    void withoutDistrict() {
      assertThat(AddressConverter.convert("135 Lê Lợi, Phường Bến Thành, TP.HCM")).get()
          .extracting(Conversion::address).isEqualTo("135 Lê Lợi, Phường Bến Thành, Thành phố Hồ Chí Minh");
    }

    @Test
    void oldProvinceMergedIntoAnother() {
      Conversion c = AddressConverter.convert("Phường Hiệp Thành, Thủ Dầu Một, Bình Dương").orElseThrow();
      assertThat(c.province().name()).isEqualTo("Hồ Chí Minh");
      assertThat(c.exact()).isFalse();
      assertThat(c.candidates()).extracting(Ward::name).containsExactlyInAnyOrder("Phú Lợi", "Thủ Dầu Một");
    }

    @Test
    void islandDistrict() {
      assertThat(AddressConverter.convert("Huyện Lý Sơn, Quảng Ngãi")).get()
          .extracting(c -> c.ward().fullName()).isEqualTo("Đặc khu Lý Sơn");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "Hà Nội", "Không Có, Hà Nội", "Phường 1, TP.HCM", "Phường 12, Quận Tân Bình, Atlantis"})
    void nothingRatherThanAGuess(String address) {
      assertThat(AddressConverter.convert(address)).isEmpty();
    }
  }
}
