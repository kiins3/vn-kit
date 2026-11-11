package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.AdminUnits;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link WardCode}. */
public class WardCodeValidator implements ConstraintValidator<WardCode, CharSequence> {

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || AdminUnits.ward(value.toString()).isPresent();
  }
}
