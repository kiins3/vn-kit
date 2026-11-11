package io.github.kiins3.vnkit.validation;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The ward code in one property belongs to the province code in another, as in an address form
 * where the user picks a province and then a ward:
 *
 * <pre>{@code
 * @WardInProvince(ward = "wardCode", province = "provinceCode")
 * record AddressForm(@ProvinceCode String provinceCode, @WardCode String wardCode, String street) {}
 * }</pre>
 *
 * The check passes when either code is {@code null} or unknown, so it does not repeat the
 * errors of {@link ProvinceCode} and {@link WardCode}. A failure is reported on the ward property.
 */
@Documented
@Target(TYPE)
@Retention(RUNTIME)
@Repeatable(WardInProvince.List.class)
@Constraint(validatedBy = WardInProvinceValidator.class)
public @interface WardInProvince {
  /** Name of the property holding the ward code. */
  String ward() default "wardCode";

  /** Name of the property holding the province code. */
  String province() default "provinceCode";

  String message() default "{io.github.kiins3.vnkit.WardInProvince.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};

  @Documented
  @Target(TYPE)
  @Retention(RUNTIME)
  @interface List {
    WardInProvince[] value();
  }
}
