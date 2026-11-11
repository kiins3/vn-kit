package io.github.kiins3.vnkit.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value is the code of a current ward, commune or special zone, e.g. {@code "00004"}.
 * {@code null} is valid; add {@code @NotNull} or {@code @NotBlank} to require a value. To check
 * that it lies in the province chosen next to it, put {@link WardInProvince} on the class.
 */
@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = WardCodeValidator.class)
public @interface WardCode {
  String message() default "{io.github.kiins3.vnkit.WardCode.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
