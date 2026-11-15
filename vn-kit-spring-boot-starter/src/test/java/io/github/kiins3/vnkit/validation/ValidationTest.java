package io.github.kiins3.vnkit.validation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.kiins3.vnkit.PhoneType;
import io.github.kiins3.vnkit.TaxCodeType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.hibernate.validator.HibernateValidator;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

class ValidationTest {

  static final ValidatorFactory EN = factory(Locale.ENGLISH);
  static final ValidatorFactory VI = factory(Locale.forLanguageTag("vi"));

  static ValidatorFactory factory(Locale locale) {
    return Validation.byProvider(HibernateValidator.class).configure().defaultLocale(locale).buildValidatorFactory();
  }

  @AfterAll
  static void close() {
    EN.close();
    VI.close();
  }

  @WardInProvince(ward = "wardCode", province = "provinceCode")
  record AddressForm(@ProvinceCode String provinceCode, @WardCode String wardCode) {}

  /** A plain class with getters and a custom property name. */
  @WardInProvince(ward = "ward", province = "city")
  static class Shipping {
    private final String city;
    private final String ward;

    Shipping(String city, String ward) {
      this.city = city;
      this.ward = ward;
    }

    public String getCity() {
      return city;
    }

    public String getWard() {
      return ward;
    }
  }

  @WardInProvince(ward = "nope")
  record Broken(String provinceCode) {}

  static Set<ConstraintViolation<Object>> validate(Object bean) {
    Validator validator = EN.getValidator();
    return validator.validate(bean);
  }

  @Test
  void validAddress() {
    // Phường Bến Thành is in Hồ Chí Minh City (79).
    assertThat(validate(new AddressForm("79", "26743"))).isEmpty();
    assertThat(validate(new Shipping("79", "26743"))).isEmpty();
  }

  @Test
  void nullsAreLeftToNotNull() {
    assertThat(validate(new AddressForm(null, null))).isEmpty();
    assertThat(validate(new AddressForm("79", null))).isEmpty();
  }

  @Test
  void unknownCodes() {
    assertThat(validate(new AddressForm("00", "99999")))
        .extracting(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
        .containsExactlyInAnyOrder(
            "provinceCode: is not a known province code",
            "wardCode: is not a known ward or commune code");
  }

  @Test
  void wardFromAnotherProvince() {
    // Phường Ba Đình (00004) is in Hà Nội, not Hồ Chí Minh City.
    assertThat(validate(new AddressForm("79", "00004")))
        .extracting(v -> v.getPropertyPath().toString() + ": " + v.getMessage())
        .containsExactly("wardCode: is not in the selected province");
    assertThat(validate(new Shipping("79", "00004")))
        .extracting(v -> v.getPropertyPath().toString())
        .containsExactly("ward");
  }

  @Test
  void vietnameseMessages() {
    Set<ConstraintViolation<AddressForm>> violations = VI.getValidator().validate(new AddressForm("79", "00004"));
    assertThat(violations).extracting(ConstraintViolation::getMessage)
        .containsExactly("không thuộc tỉnh/thành phố đã chọn");
  }

  record Customer(
      @ValidPhoneNumber String phone,
      @ValidPhoneNumber(PhoneType.MOBILE) String mobile,
      @ValidCitizenId String citizenId,
      @ValidTaxCode String taxCode,
      @ValidTaxCode({TaxCodeType.MAIN, TaxCodeType.DEPENDENT}) String companyTaxCode) {}

  static List<String> errors(Object bean) {
    return validate(bean).stream().map(v -> v.getPropertyPath() + ": " + v.getMessage()).sorted().toList();
  }

  @Test
  void validCustomer() {
    assertThat(errors(new Customer("024 3825 1234", "+84 912 345 678", "079090001234", "079090001234", "0100109106-001")))
        .isEmpty();
    assertThat(errors(new Customer(null, null, null, null, null))).isEmpty();
  }

  @Test
  void invalidCustomer() {
    assertThat(errors(new Customer("0711234567", "0912", "003090001234", "0100109107", "abc")))
        .containsExactly(
            "citizenId: is not a valid citizen ID number",
            "companyTaxCode: is not a valid tax code",
            "mobile: is not a valid Vietnamese phone number",
            "phone: is not a valid Vietnamese phone number",
            "taxCode: is not a valid tax code");
  }

  @Test
  void restrictedKinds() {
    // A landline where a mobile is required, a personal ID where a company code is required.
    assertThat(errors(new Customer(null, "024 3825 1234", null, null, "079090001234")))
        .containsExactly("companyTaxCode: is not a valid tax code", "mobile: is not a valid Vietnamese phone number");
  }

  @Test
  void vietnameseMessagesForIdentifiers() {
    assertThat(VI.getValidator().validate(new Customer("1", null, "1", "1", null)))
        .extracting(ConstraintViolation::getMessage)
        .containsExactlyInAnyOrder("không phải số điện thoại hợp lệ", "không phải số căn cước hợp lệ", "không phải mã số thuế hợp lệ");
  }

  @Test
  void typoInPropertyNameFailsLoudly() {
    assertThatThrownBy(() -> validate(new Broken("79")))
        .isInstanceOf(ValidationException.class)
        .hasStackTraceContaining("has no property 'nope'");
  }
}
