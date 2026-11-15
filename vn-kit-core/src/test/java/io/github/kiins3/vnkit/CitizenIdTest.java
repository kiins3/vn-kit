package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class CitizenIdTest {

  @Test
  void readsWhatTheNumberEncodes() {
    CitizenId id = CitizenId.parse("079090001234").orElseThrow();
    assertThat(id.number()).isEqualTo("079090001234");
    assertThat(id.birthProvince().name()).isEqualTo("Hồ Chí Minh");
    assertThat(id.gender()).isEqualTo(Gender.MALE);
    assertThat(id.birthYear()).isEqualTo(1990);
  }

  @ParameterizedTest
  @CsvSource({
      "001305012345, Hà Nội, FEMALE, 2005",
      "038185012345, Thanh Hóa, FEMALE, 1985",
      "096200012345, Cà Mau, MALE, 2000",
      "027099012345, Bắc Ninh, MALE, 1999"})
  void genderAndCentury(String number, String province, Gender gender, int year) {
    CitizenId id = CitizenId.parse(number).orElseThrow();
    assertThat(id.birthProvince().name()).isEqualTo(province);
    assertThat(id.gender()).isEqualTo(gender);
    assertThat(id.birthYear()).isEqualTo(year);
  }

  @Test
  void oldProvinceAndTheCurrentOne() {
    // 074 was Bình Dương, now part of Hồ Chí Minh City.
    CitizenId id = CitizenId.parse("074190012345").orElseThrow();
    assertThat(id.birthProvince().name()).isEqualTo("Bình Dương");
    assertThat(id.province().code()).isEqualTo("79");
  }

  @Test
  void birthYearCannotBeInTheFuture() {
    assertThat(CitizenId.parse("079299001234", 2026)).isEmpty();
    assertThat(CitizenId.parse("079299001234", 2099)).isPresent();
    assertThat(CitizenId.parse("079226001234", 2026)).get().extracting(CitizenId::birthYear).isEqualTo(2026);
  }

  @Test
  void maskedInLogs() {
    CitizenId id = CitizenId.parse("079 090 001 234").orElseThrow();
    assertThat(id.masked()).isEqualTo("079*******34");
    assertThat(id).hasToString("079*******34");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "003090001234", // no province 03
      "179090001234", "07909000123", "0790900012345", "07909000123a", "079-090-001234", "123456789"})
  void notCitizenIds(String text) {
    assertThat(CitizenId.parse(text)).isEmpty();
    assertThat(CitizenId.isValid(text)).isFalse();
  }

  @Test
  void everyOldProvinceCodeIsAccepted() {
    for (LegacyProvince p : LegacyUnits.provinces()) {
      assertThat(CitizenId.parse("0" + p.code() + "090001234")).get().extracting(CitizenId::birthProvince).isEqualTo(p);
    }
  }
}
