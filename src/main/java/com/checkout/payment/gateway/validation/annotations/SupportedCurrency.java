package com.checkout.payment.gateway.validation.annotations;

import com.checkout.payment.gateway.validation.SupportedCurrencyValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Documented
@Constraint(validatedBy = SupportedCurrencyValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface SupportedCurrency {
  String message() default "Currency is not supported";
  Class<?>[] groups() default {};
  Class<? extends Payload>[] payload() default {};
}
