package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {

  @ParameterizedTest
  @ValueSource(strings = {
      "0912345678", "091 234 5678", "0912.345.678", "0912-345-678", "+84912345678", "+84 912 345 678",
      "84912345678", "0084912345678", "(+84) 912-345-678", "+84 (0) 912 345 678", " 0912 345 678 "})
  void mobileWrittenInManyWays(String text) {
    PhoneNumber p = PhoneNumber.parse(text).orElseThrow();
    assertThat(p.national()).isEqualTo("0912345678");
    assertThat(p.e164()).isEqualTo("+84912345678");
    assertThat(p.formatted()).isEqualTo("0912 345 678");
    assertThat(p.type()).isEqualTo(PhoneType.MOBILE);
  }

  @ParameterizedTest
  @CsvSource({
      "0961234567, VIETTEL", "0321234567, VIETTEL", "0861234567, VIETTEL", "0951234567, VIETTEL",
      "0911234567, VINAPHONE", "0811234567, VINAPHONE", "0881234567, VINAPHONE",
      "0901234567, MOBIFONE", "0701234567, MOBIFONE", "0891234567, MOBIFONE",
      "0921234567, VIETNAMOBILE", "0521234567, VIETNAMOBILE",
      "0991234567, GMOBILE", "0591234567, GMOBILE",
      "0871234567, ITEL", "0551234567, WINTEL", "0751234567, POTTEL"})
  void carrierOfThePrefix(String text, Carrier carrier) {
    assertThat(PhoneNumber.parse(text)).get().extracting(p -> p.carrier().orElseThrow()).isEqualTo(carrier);
  }

  @Test
  void everyPrefixBelongsToOneCarrier() {
    List<String> all = Arrays.stream(Carrier.values()).flatMap(c -> c.prefixes().stream()).toList();
    assertThat(all).doesNotHaveDuplicates().hasSize(38).allSatisfy(p -> assertThat(p).matches("0[3-9]\\d"));
  }

  @ParameterizedTest
  @ValueSource(strings = {"0711234567", "0511234567", "0311234567", "0801234567", "0601234567", "0101234567"})
  void unassignedMobilePrefixes(String text) {
    assertThat(PhoneNumber.parse(text)).isEmpty();
  }

  @Test
  void fixedLines() {
    PhoneNumber hanoi = PhoneNumber.parse("(024) 3825 1234").orElseThrow();
    assertThat(hanoi.type()).isEqualTo(PhoneType.FIXED);
    assertThat(hanoi.formatted()).isEqualTo("024 3825 1234");
    assertThat(hanoi.carrier()).isEmpty();
    assertThat(PhoneNumber.parse("+84 236 381 2345")).get().extracting(PhoneNumber::formatted).isEqualTo("0236 381 2345");
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {
      "091234567", "09123456789", "912345678", "abc", "0912345678 ext 1", "+1 912 345 678",
      "0912+345678", "++84912345678", "0312345678901", "02438251234 5"})
  void notPhoneNumbers(String text) {
    assertThat(PhoneNumber.parse(text)).isEmpty();
    assertThat(PhoneNumber.isValid(text)).isFalse();
  }

  @ParameterizedTest
  @CsvSource({
      "01688123456, 0388123456",
      "+84 168 812 3456, 0388123456",
      "0123 456 7890, 0834567890",
      "0121 234 5678, 0792345678",
      "0199 123 4567, 0591234567",
      "0912345678, 0912345678"})
  void oldElevenDigitMobileNumbers(String old, String current) {
    assertThat(PhoneNumber.convert(old)).get().extracting(PhoneNumber::national).isEqualTo(current);
  }

  @Test
  void oldNumbersAreOnlyAcceptedWhenConverting() {
    assertThat(PhoneNumber.parse("01688123456")).isEmpty();
    assertThat(PhoneNumber.convert("01001234567")).isEmpty();
    assertThat(PhoneNumber.convert(null)).isEmpty();
  }

  @Test
  void equalWhateverTheFormat() {
    assertThat(PhoneNumber.parse("+84 912 345 678")).isEqualTo(PhoneNumber.parse("0912.345.678"));
    assertThat(PhoneNumber.parse("0912345678").orElseThrow()).hasToString("0912345678");
  }
}
