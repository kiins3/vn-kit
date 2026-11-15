package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.PhoneNumber;
import io.github.kiins3.vnkit.PhoneType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Checks {@link ValidPhoneNumber}. */
public class ValidPhoneNumberValidator implements ConstraintValidator<ValidPhoneNumber, CharSequence> {

  private Set<PhoneType> types = EnumSet.allOf(PhoneType.class);

  @Override
  public void initialize(ValidPhoneNumber annotation) {
    types = EnumSet.copyOf(List.of(annotation.value()));
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || PhoneNumber.parse(value).filter(p -> types.contains(p.type())).isPresent();
  }
}
