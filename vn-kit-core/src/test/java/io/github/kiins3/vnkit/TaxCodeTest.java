package io.github.kiins3.vnkit;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class TaxCodeTest {

  @ParameterizedTest
  @ValueSource(strings = {"0100109106", "0300588569", "0101248141", " 0100 109 106 "})
  void tenDigitsWithTheirCheckDigit(String text) {
    TaxCode code = TaxCode.parse(text).orElseThrow();
    assertThat(code.type()).isEqualTo(TaxCodeType.MAIN);
    assertThat(code.parent()).isEmpty();
    assertThat(code.citizenId()).isEmpty();
  }

  @ParameterizedTest
  @ValueSource(strings = {"0100109107", "0300588560", "0101248140", "0000000000"})
  void wrongCheckDigit(String text) {
    assertThat(TaxCode.parse(text)).isEmpty();
  }

  @Test
  void atMostOneCheckDigitFitsAnyNineDigits() {
    for (int head = 0; head < 1_000_000_000; head += 7_919_993) {
      String nine = String.format("%09d", head);
      int fits = 0;
      for (int d = 0; d <= 9; d++) if (TaxCode.checks(nine + d)) fits++;
      assertThat(fits).as(nine).isLessThanOrEqualTo(1);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"0100109106-001", "0100109106001", "0100109106 - 001"})
  void dependentUnits(String text) {
    TaxCode code = TaxCode.parse(text).orElseThrow();
    assertThat(code.type()).isEqualTo(TaxCodeType.DEPENDENT);
    assertThat(code.value()).isEqualTo("0100109106-001");
    assertThat(code.parent()).get().extracting(TaxCode::value).isEqualTo("0100109106");
  }

  @Test
  void dependentUnitNeedsAValidParentAndANumber() {
    assertThat(TaxCode.parse("0100109107-001")).isEmpty();
    assertThat(TaxCode.parse("0100109106-000")).isEmpty();
  }

  @Test
  void personalIdNumberSince2025() {
    TaxCode code = TaxCode.parse("079090001234").orElseThrow();
    assertThat(code.type()).isEqualTo(TaxCodeType.PERSONAL_ID);
    assertThat(code.citizenId()).get().extracting(CitizenId::birthYear).isEqualTo(1990);
    assertThat(TaxCode.parse("003090001234")).isEmpty();
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"010010910", "01001091061", "0100109106-01", "0100109106-0001", "010010910a", "0100109106--001"})
  void notTaxCodes(String text) {
    assertThat(TaxCode.parse(text)).isEmpty();
    assertThat(TaxCode.isValid(text)).isFalse();
  }

  @Test
  void equalWhateverTheFormat() {
    assertThat(TaxCode.parse("0100109106001")).isEqualTo(TaxCode.parse("0100109106-001"));
    assertThat(TaxCode.parse("0100109106").orElseThrow()).hasToString("0100109106");
  }
}
