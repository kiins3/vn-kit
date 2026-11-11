package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.AdminUnits;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** Checks {@link ProvinceCode}. */
public class ProvinceCodeValidator implements ConstraintValidator<ProvinceCode, CharSequence> {

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || AdminUnits.province(value.toString()).isPresent();
  }
}
