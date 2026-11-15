package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.CitizenId;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link ValidCitizenId}. */
public class ValidCitizenIdValidator implements ConstraintValidator<ValidCitizenId, CharSequence> {

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || CitizenId.isValid(value);
  }
}
