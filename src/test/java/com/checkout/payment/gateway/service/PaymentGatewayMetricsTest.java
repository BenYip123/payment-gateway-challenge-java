package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.TestUtils.validRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import io.micrometer.core.instrument.MeterRegistry;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest
class PaymentGatewayMetricsTest {

  @Autowired
  private PaymentGatewayService paymentGatewayService;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Autowired
  private MeterRegistry meterRegistry;

  private String idempotencyKey;

  @BeforeEach
  void setUp() {
    idempotencyKey = UUID.randomUUID().toString();
  }

  @Test
  void whenBankAuthorizesThenRecordsMetric() {
    PostPaymentRequest request = validRequest();

    AcquiringBankResponse bankResponse = new AcquiringBankResponse(
        true,
        "test-auth-code"
    );

    when(acquiringBankClient.process(any())).thenReturn(bankResponse);

    paymentGatewayService.processPayment(request, idempotencyKey);

    assertThat(meterRegistry.get("payment.outcomes")
            .tag("status", "authorized").counter().count()).isOne();
  }

  @Test
  void whenBankDeclinesThenRecordsMetric() {
    PostPaymentRequest request = validRequest();

    AcquiringBankResponse bankResponse = new AcquiringBankResponse(
        false,
        ""
    );

    when(acquiringBankClient.process(any())).thenReturn(bankResponse);

    paymentGatewayService.processPayment(request, idempotencyKey);

    assertThat(meterRegistry.get("payment.outcomes")
            .tag("status", "declined").counter().count()).isOne();
  }

  @Test
  void whenValidationFailsThenRecordsRejectedMetric() {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("bad");
    request.setCurrency("XYZ");
    request.setAmount(-1);
    request.setCvv("");

    paymentGatewayService.processPayment(request, idempotencyKey);

    assertThat(meterRegistry.get("payment.outcomes")
            .tag("status", "rejected").counter().count()).isOne();
  }
}
