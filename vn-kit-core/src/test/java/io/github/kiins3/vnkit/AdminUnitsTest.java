package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminUnitsTest {

  @Nested
  class Data {

    @Test
    void hasThe34ProvincesAnd3321WardsOfTheReform() {
      assertThat(AdminUnits.provinces()).hasSize(34);
      assertThat(AdminUnits.wards()).hasSize(3321);
    }

    @Test
    void hasNineCentrallyRunCities() {
      Set<String> cities = AdminUnits.provinces().stream()
          .filter(p -> p.type() == ProvinceType.CITY)
          .map(Province::name)
          .collect(Collectors.toSet());
      assertThat(cities).containsExactlyInAnyOrder(
          "Hà Nội", "Hải Phòng", "Huế", "Đà Nẵng", "Hồ Chí Minh", "Cần Thơ",
          "Đồng Nai", "Quảng Ninh", "Bắc Ninh");
    }

    @Test
    void codesAreWellFormedAndUnique() {
      assertThat(AdminUnits.provinces()).allSatisfy(p -> assertThat(p.code()).matches("\\d{2}"));
      assertThat(AdminUnits.wards()).allSatisfy(w -> {
        assertThat(w.code()).matches("\\d{5}");
        assertThat(w.postalCode()).matches("\\d{5}");
      });
      assertThat(AdminUnits.wards().stream().map(Ward::code).distinct()).hasSize(3321);
    }

    @Test
    void everyWardBelongsToAProvinceAndStartsWithItsType() {
      assertThat(AdminUnits.wards()).allSatisfy(w -> {
        assertThat(w.province().code()).isEqualTo(w.provinceCode());
        assertThat(w.fullName()).isEqualTo(w.type().label() + " " + w.name());
      });
      int total = AdminUnits.provinces().stream().mapToInt(p -> p.wards().size()).sum();
      assertThat(total).isEqualTo(3321);
    }

    @Test
    void listsAreReadOnly() {
      assertThatThrownBy(() -> AdminUnits.provinces().clear()).isInstanceOf(UnsupportedOperationException.class);
      assertThatThrownBy(() -> AdminUnits.wardsOf("01").clear()).isInstanceOf(UnsupportedOperationException.class);
    }

    @Test
    void knowsWhichReleaseItShips() {
      assertThat(AdminUnits.datasetVersion()).startsWith("v");
      assertThat(AdminUnits.latestDecree()).contains("/");
    }
  }

  @Nested
  class Lookup {

    @Test
    void byCode() {
      assertThat(AdminUnits.province("01")).get().extracting(Province::fullName).isEqualTo("Thành phố Hà Nội");
      assertThat(AdminUnits.ward("26743")).get().extracting(Ward::fullName).isEqualTo("Phường Bến Thành");
      assertThat(AdminUnits.ward("26743").get().province().name()).isEqualTo("Hồ Chí Minh");
    }

    @Test
    void unknownOrNullCodes() {
      assertThat(AdminUnits.province("00")).isEmpty();
      assertThat(AdminUnits.province(null)).isEmpty();
      assertThat(AdminUnits.ward("99999")).isEmpty();
      assertThat(AdminUnits.wardsOf("00")).isEmpty();
    }
  }

  @Nested
  class FindProvince {

    @ParameterizedTest
    @ValueSource(strings = {
        "Hồ Chí Minh", "ho chi minh", "HO CHI MINH", "Thành phố Hồ Chí Minh", "TP. Hồ Chí Minh",
        "tp.hcm", "TPHCM", "HCM", "Sài Gòn", "Ho Chi Minh City", "79", "  79 "})
    void hoChiMinhCity(String text) {
      assertThat(AdminUnits.findProvince(text)).get().extracting(Province::code).isEqualTo("79");
    }

    @ParameterizedTest
    @ValueSource(strings = {"Hà Nội", "ha noi", "Hanoi", "HN", "TP Hà Nội", "1", "01"})
    void hanoi(String text) {
      assertThat(AdminUnits.findProvince(text)).get().extracting(Province::code).isEqualTo("01");
    }

    @Test
    void ignoresHowToneMarksArePlaced() {
      assertThat(AdminUnits.findProvince("Thanh Hóa")).get().extracting(Province::name).isEqualTo("Thanh Hoá");
      assertThat(AdminUnits.findProvince("Tỉnh Khánh Hòa")).get().extracting(Province::name).isEqualTo("Khánh Hoà");
    }

    @Test
    void oldNameOfHue() {
      assertThat(AdminUnits.findProvince("Thừa Thiên Huế")).get().extracting(Province::code).isEqualTo("46");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   ", "Atlantis", "Quận 1", "999"})
    void nothingForUnknownText(String text) {
      assertThat(AdminUnits.findProvince(text)).isEmpty();
    }
  }

  @Nested
  class FindWard {

    @ParameterizedTest
    @ValueSource(strings = {"Ba Đình", "ba dinh", "Phường Ba Đình", "P. Ba Đình", "p.ba dinh", "PHUONG BA DINH"})
    void writtenInManyWays(String name) {
      assertThat(AdminUnits.findWard(name, "Hà Nội")).get().extracting(Ward::code).isEqualTo("00004");
    }

    @Test
    void sameNameInAnotherProvince() {
      assertThat(AdminUnits.findWard("Ba Đình", "Thanh Hoá")).get().extracting(Ward::fullName).isEqualTo("Xã Ba Đình");
    }

    @Test
    void specialZones() {
      assertThat(AdminUnits.findWard("Phú Quốc", "An Giang")).get().extracting(Ward::type).isEqualTo(WardType.SPECIAL_ZONE);
      assertThat(AdminUnits.findWard("đặc khu côn đảo", "tp.hcm")).get().extracting(Ward::code).isEqualTo("26732");
    }

    @Test
    void newStyleToneMarksMatchOldStyleData() {
      assertThat(AdminUnits.findWard("Hòa Khánh", "Đà Nẵng")).get().extracting(Ward::name).isEqualTo("Hoà Khánh");
    }

    @Test
    void accentsDecideWhenTwoNamesDifferOnlyByThem() {
      assertThat(AdminUnits.findWard("Sơn Hà", "Quảng Ngãi")).get().extracting(Ward::code).isEqualTo("21289");
      assertThat(AdminUnits.findWard("Sơn Hạ", "Quảng Ngãi")).get().extracting(Ward::code).isEqualTo("21292");
      assertThat(AdminUnits.findWard("son ha", "Quảng Ngãi")).isEmpty();
      assertThat(AdminUnits.searchWards("son ha", "Quảng Ngãi"))
          .extracting(Ward::code).startsWith("21289", "21292");
    }

    @Test
    void unitTypeDecidesWhenAccentsAreMissing() {
      // Đồng Tháp has Phường Mỹ Tho and Xã Mỹ Thọ.
      assertThat(AdminUnits.findWard("phuong my tho", "Đồng Tháp")).get().extracting(Ward::code).isEqualTo("28261");
      assertThat(AdminUnits.findWard("xa my tho", "Đồng Tháp")).get().extracting(Ward::code).isEqualTo("30076");
      assertThat(AdminUnits.findWard("my tho", "Đồng Tháp")).isEmpty();
    }

    @Test
    void nameThatStartsLikeAUnitType() {
      // "Xa" here is part of the name, not "Xã".
      Ward ward = AdminUnits.wards().stream().filter(w -> VnText.removeAccents(w.name()).startsWith("Xa ")).findFirst().orElseThrow();
      assertThat(AdminUnits.findWard(ward.name(), ward.provinceCode())).contains(ward);
    }

    @Test
    void nothingWhenUnknown() {
      assertThat(AdminUnits.findWard("Ba Đình", "Atlantis")).isEmpty();
      assertThat(AdminUnits.findWard("Không Có", "Hà Nội")).isEmpty();
      assertThat(AdminUnits.findWard(null, "Hà Nội")).isEmpty();
      assertThat(AdminUnits.findWard("Ba Đình", null)).isEmpty();
    }
  }

  @Nested
  class Search {

    @Test
    void exactMatchesComeFirst() {
      assertThat(AdminUnits.searchWards("ben thanh")).first().extracting(Ward::code).isEqualTo("26743");
    }

    @Test
    void sameNameAcrossTheCountry() {
      assertThat(AdminUnits.searchWards("Hoà Bình"))
          .extracting(Ward::code)
          .startsWith("04795", "11533", "27211", "29830", "31891");
    }

    @Test
    void prefixAndSubstring() {
      assertThat(AdminUnits.searchWards("hoan kiem", "01")).extracting(Ward::code).containsExactly("00070");
      assertThat(AdminUnits.searchWards("kiem", "Hà Nội")).extracting(Ward::name).contains("Hoàn Kiếm");
    }

    @Test
    void scopedToAProvince() {
      assertThat(AdminUnits.searchWards("Hoà Bình", "Hồ Chí Minh")).extracting(Ward::provinceCode).containsOnly("79");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "  ", ".,", "a"})
    void nothingForEmptyOrOneLetterQueries(String query) {
      assertThat(AdminUnits.searchWards(query)).isEmpty();
    }
  }
}
