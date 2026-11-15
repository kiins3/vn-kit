package io.github.kiins3.vnkit.validation;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.RECORD_COMPONENT;
import static java.lang.annotation.ElementType.TYPE_USE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import io.github.kiins3.vnkit.CitizenId;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

/**
 * The value is a well-formed twelve-digit citizen ID (căn cước, CCCD) as accepted by
 * {@link CitizenId#parse}: a known province code, a birth year that is not in the future.
 * {@code null} is valid; add {@code @NotBlank} to require a value.
 */
@Documented
@Target({FIELD, PARAMETER, RECORD_COMPONENT, TYPE_USE})
@Retention(RUNTIME)
@Constraint(validatedBy = ValidCitizenIdValidator.class)
public @interface ValidCitizenId {
  String message() default "{io.github.kiins3.vnkit.ValidCitizenId.message}";

  Class<?>[] groups() default {};

  Class<? extends Payload>[] payload() default {};
}
