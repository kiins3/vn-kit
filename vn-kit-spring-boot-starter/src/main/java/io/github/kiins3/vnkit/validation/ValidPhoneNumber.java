package io.github.kiins3.vnkit.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.kiins3.vnkit.PhoneNumber;
import io.github.kiins3.vnkit.PhoneType;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value is a Vietnamese phone number as accepted by {@link PhoneNumber#parse}, e.g.
 * {@code "0912 345 678"} or {@code "+84912345678"}. Use {@code @ValidPhoneNumber(PhoneType.MOBILE)}
 * to accept mobile numbers only. {@code null} is valid; add {@code @NotBlank} to require a value.
 *
 * <p>Formatting is accepted as typed, so store {@code PhoneNumber.parse(value).get().national()}
 * rather than the raw value.
 */
@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidPhoneNumberValidator.class)
public @interface ValidPhoneNumber {
  /** Which kinds of number to accept; both by default. */
  PhoneType[] value() default {PhoneType.MOBILE, PhoneType.FIXED};

  String message() default "{io.github.kiins3.vnkit.ValidPhoneNumber.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
