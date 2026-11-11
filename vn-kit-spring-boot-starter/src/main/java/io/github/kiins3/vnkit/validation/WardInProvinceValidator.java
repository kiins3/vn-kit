package io.github.kiins3.vnkit.validation;

import io.github.kiins3.vnkit.AdminUnits;
import io.github.kiins3.vnkit.Ward;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import jakarta.validation.ValidationException;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.RecordComponent;
import java.util.Optional;

/** Checks {@link WardInProvince}. */
public class WardInProvinceValidator implements ConstraintValidator<WardInProvince, Object> {

  private String wardProperty;
  private String provinceProperty;

  @Override
  public void initialize(WardInProvince annotation) {
    wardProperty = annotation.ward();
    provinceProperty = annotation.province();
  }

  @Override
  public boolean isValid(Object bean, ConstraintValidatorContext context) {
    if (bean == null) return true;
    Object wardCode = read(bean, wardProperty);
    Object provinceCode = read(bean, provinceProperty);
    if (wardCode == null || provinceCode == null) return true;

    Optional<Ward> ward = AdminUnits.ward(wardCode.toString());
    if (ward.isEmpty() || AdminUnits.province(provinceCode.toString()).isEmpty()) return true;
    if (ward.get().provinceCode().equals(provinceCode.toString().trim())) return true;

    context.disableDefaultConstraintViolation();
    context.buildConstraintViolationWithTemplate(context.getDefaultConstraintMessageTemplate())
        .addPropertyNode(wardProperty)
        .addConstraintViolation();
    return false;
  }

  /** Reads a property from a record component, a getter or a field. */
  static Object read(Object bean, String property) {
    Class<?> type = bean.getClass();
    try {
      if (type.isRecord()) {
        for (RecordComponent c : type.getRecordComponents()) {
          if (c.getName().equals(property)) return invoke(c.getAccessor(), bean);
        }
      }
      String suffix = Character.toUpperCase(property.charAt(0)) + property.substring(1);
      for (String name : new String[] {"get" + suffix, property}) {
        try {
          return invoke(type.getMethod(name), bean);
        } catch (NoSuchMethodException ignored) {
          // try the next form
        }
      }
      for (Class<?> c = type; c != null && c != Object.class; c = c.getSuperclass()) {
        try {
          Field f = c.getDeclaredField(property);
          f.setAccessible(true);
          return f.get(bean);
        } catch (NoSuchFieldException ignored) {
          // look in the superclass
        }
      }
    } catch (ReflectiveOperationException | RuntimeException e) {
      throw new ValidationException("Cannot read property '" + property + "' of " + type.getName(), e);
    }
    throw new ValidationException(
        "@WardInProvince: " + type.getName() + " has no property '" + property + "'");
  }

  private static Object invoke(Method m, Object bean) throws ReflectiveOperationException {
    m.setAccessible(true);
    return m.invoke(bean);
  }
}
