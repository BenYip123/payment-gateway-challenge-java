package com.checkout.payment.gateway.exception;

import com.checkout.payment.gateway.model.ErrorResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.client.HttpServerErrorException;
import java.util.List;

@ControllerAdvice
public class CommonExceptionHandler {

  private static final Logger LOG = LoggerFactory.getLogger(CommonExceptionHandler.class);

  @ExceptionHandler(PaymentNotFoundException.class)
  public ResponseEntity<ErrorResponse> handlePaymentNotFound(PaymentNotFoundException ex) {
    LOG.error("Payment not found: {}", ex.getMessage(), ex);
    return new ResponseEntity<>(new ErrorResponse(List.of("Payment not found")),
        HttpStatus.NOT_FOUND);
  }

  @ExceptionHandler(HttpServerErrorException.class)
  public ResponseEntity<ErrorResponse> handleServiceUnavailable(HttpServerErrorException ex) {
    LOG.error("Bank service unavailable", ex);
    return new ResponseEntity<>(new ErrorResponse(List.of("Service unavailable")),
        HttpStatus.BAD_GATEWAY);
  }
}
