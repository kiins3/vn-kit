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
 * The value is the code of one of the current provinces or centrally run cities, e.g. {@code "01"}.
 * {@code null} is valid; add {@code @NotNull} or {@code @NotBlank} to require a value.
 */
@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ProvinceCodeValidator.class)
public @interface ProvinceCode {
  String message() default "{io.github.kiins3.vnkit.ProvinceCode.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
