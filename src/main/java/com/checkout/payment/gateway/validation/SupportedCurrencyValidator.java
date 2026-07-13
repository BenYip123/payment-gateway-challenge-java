package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.validation.annotations.SupportedCurrency;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.util.Set;

public class SupportedCurrencyValidator implements ConstraintValidator<SupportedCurrency, String> {

  // hardcoded currencies for the purpose of this challenge
  private static final Set<String> SUPPORTED = Set.of("USD", "GBP", "EUR");

  @Override
  public boolean isValid(String value, ConstraintValidatorContext context) {
    if (value == null) return true;
    return SUPPORTED.contains(value);
  }
}
