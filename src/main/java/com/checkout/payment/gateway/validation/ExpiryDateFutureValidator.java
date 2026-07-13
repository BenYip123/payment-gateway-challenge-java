package com.checkout.payment.gateway.validation;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.validation.annotations.ValidExpiryDate;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.DateTimeException;
import java.time.YearMonth;

public class ExpiryDateFutureValidator implements ConstraintValidator<ValidExpiryDate, PostPaymentRequest> {

  @Override
  public boolean isValid(PostPaymentRequest value, ConstraintValidatorContext context) {
    if (value == null) return true;

    try {
      int year = value.getExpiryYear();
      int month = value.getExpiryMonth();

      if (month < 1 || month > 12) return true; // let @Min/@Max annotations handle this
      return YearMonth.of(year, month).isAfter(YearMonth.now());
    } catch (DateTimeException e) {
      return true;
    }
  }
}
