package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import io.micrometer.core.instrument.MeterRegistry;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

import static com.checkout.payment.gateway.TestUtils.validRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@SpringBootTest
class PaymentGatewayMetricsTest {

  @Autowired
  private PaymentGatewayService paymentGatewayService;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Autowired
  private MeterRegistry meterRegistry;

  @Test
  void whenBankAuthorizesThenRecordsMetric() {
    PostPaymentRequest request = validRequest();

    AcquiringBankResponse bankResponse = new AcquiringBankResponse(
        true,
        "test-auth-code"
    );

    when(acquiringBankClient.process(any())).thenReturn(bankResponse);

    paymentGatewayService.processPayment(request);

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

    paymentGatewayService.processPayment(request);

    assertThat(meterRegistry.get("payment.outcomes")
            .tag("status", "declined").counter().count()).isOne();
  }

  @Test
  void whenValidationFailsThenRecordsRejectedMetric() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("bad");
    request.setExpiryMonth(13);
    request.setExpiryYear(2027);
    request.setCurrency("XYZ");
    request.setAmount(-1);
    request.setCvv("");

    paymentGatewayService.processPayment(request);

    assertThat(meterRegistry.get("payment.outcomes")
            .tag("status", "rejected").counter().count()).isOne();
  }
}
