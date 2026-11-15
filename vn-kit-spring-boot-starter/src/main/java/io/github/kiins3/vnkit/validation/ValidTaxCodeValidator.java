package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.TaxCode;
import io.github.kiins3.vnkit.TaxCodeType;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/** Checks {@link ValidTaxCode}. */
public class ValidTaxCodeValidator implements ConstraintValidator<ValidTaxCode, CharSequence> {

  private Set<TaxCodeType> types = EnumSet.allOf(TaxCodeType.class);

  @Override
  public void initialize(ValidTaxCode annotation) {
    types = EnumSet.copyOf(List.of(annotation.value()));
  }

  @Override
  public boolean isValid(CharSequence value, ConstraintValidatorContext context) {
    return value == null || TaxCode.parse(value).filter(c -> types.contains(c.type())).isPresent();
  }
}
