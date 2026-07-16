package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.AcquiringBankRequest;
import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class PaymentGatewayService {

  private static final Logger LOG = LoggerFactory.getLogger(PaymentGatewayService.class);

  private final PaymentsRepository paymentsRepository;
  private final AcquiringBankClient acquiringBankClient;
  private final Validator validator;
  private final MeterRegistry registry;

  public PaymentGatewayService(PaymentsRepository paymentsRepository, AcquiringBankClient acquiringBankClient, Validator validator, MeterRegistry registry) {
    this.paymentsRepository = paymentsRepository;
    this.acquiringBankClient = acquiringBankClient;
    this.validator = validator;
    this.registry = registry;
  }

  public PostPaymentResponse getPaymentById(UUID id) {
    LOG.debug("Requesting access to payment with ID {}", id);
    return paymentsRepository.get(id)
        .orElseThrow(() -> new PaymentNotFoundException("Payment with ID " + id + " not found"));
  }

  public PostPaymentResponse processPayment(PostPaymentRequest request) {
    // Validate annotations using Bean Validation API
    Set<ConstraintViolation<PostPaymentRequest>> violations = validator.validate(request);

    if (!violations.isEmpty()) {
      LOG.warn("Payment request validation failed: {}", violations.stream()
          .map(ConstraintViolation::getMessage)
          .collect(Collectors.toList()));
      Counter.builder("payment.outcomes")
          .tag("status", PaymentStatus.REJECTED.name().toLowerCase())
          .register(registry)
          .increment();
      return createRejectedResponse(violations);
    }

    AcquiringBankRequest bankRequest = buildAcquiringBankRequest(request);
    AcquiringBankResponse bankResponse = acquiringBankClient.process(bankRequest);
    PaymentStatus status = mapAcquiringBankResponse(bankResponse);
    PostPaymentResponse response = createAndPersist(request, status);

    Counter.builder("payment.outcomes")
        .tag("status", status.name().toLowerCase())
        .register(registry)
        .increment();

    return response;
  }

  private AcquiringBankRequest buildAcquiringBankRequest(PostPaymentRequest request) {
    return new AcquiringBankRequest(
        request.getCardNumber(),
        request.getExpiryDate(),
        request.getCurrency(),
        request.getAmount(),
        request.getCvv()
    );
  }

  private PaymentStatus mapAcquiringBankResponse(AcquiringBankResponse response) {
    return response.isAuthorized() ? PaymentStatus.AUTHORIZED : PaymentStatus.DECLINED;
  }

  private PostPaymentResponse createAndPersist(PostPaymentRequest request, PaymentStatus status) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setId(UUID.randomUUID());
    response.setStatus(status);
    response.setCardNumberLastFour(request.getCardNumberLastFour());
    response.setExpiryMonth(request.getExpiryMonth());
    response.setExpiryYear(request.getExpiryYear());
    response.setCurrency(request.getCurrency());
    response.setAmount(request.getAmount());
    paymentsRepository.add(response);
    LOG.info("Payment {} processed with status {}", response.getId(), status);
    return response;
  }

  private PostPaymentResponse createRejectedResponse(Set<ConstraintViolation<PostPaymentRequest>> violations) {
    PostPaymentResponse response = new PostPaymentResponse();
    response.setStatus(PaymentStatus.REJECTED);
    response.setErrors(violations.stream()
        .map(ConstraintViolation::getMessage)
        .collect(Collectors.toList()));

    return response;
  }

}

