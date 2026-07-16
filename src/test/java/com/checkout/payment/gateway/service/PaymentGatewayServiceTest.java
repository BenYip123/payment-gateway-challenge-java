package com.checkout.payment.gateway.service;

import static com.checkout.payment.gateway.TestUtils.validRequest;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.exception.PaymentNotFoundException;
import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.web.client.HttpServerErrorException;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayServiceTest {

  @Autowired
  private PaymentGatewayService paymentGatewayService;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Autowired
  private PaymentsRepository paymentsRepository;

  private String idempotencyKey;

  @BeforeEach
  void setUp() {
    idempotencyKey = UUID.randomUUID().toString();
  }

  @Test
  void whenBankAuthorizesThenAuthorized() {
    PostPaymentRequest request = validRequest();

    AcquiringBankResponse bankResponse = new AcquiringBankResponse(
        true,
        "test-auth-code"
    );

    when(acquiringBankClient.process(any())).thenReturn(bankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);

    assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
    assertThat(response.getId()).isNotNull();
    assertThat(paymentsRepository.get(response.getId())).isPresent();
  }

  @Test
  void whenBankDeclinesThenDeclined() {
    PostPaymentRequest request = validRequest();

    AcquiringBankResponse bankResponse = new AcquiringBankResponse(
        false,
        "test-auth-code"
    );

    when(acquiringBankClient.process(any())).thenReturn(bankResponse);

    PostPaymentResponse response = paymentGatewayService.processPayment(request, idempotencyKey);

    assertThat(response.getStatus()).isEqualTo(PaymentStatus.DECLINED);
    assertThat(response.getId()).isNotNull();
    assertThat(paymentsRepository.get(response.getId())).isPresent();
  }

  @Test
  void whenBankUnavailableThenThrows() {
    PostPaymentRequest request = validRequest();

    when(acquiringBankClient.process(any())).thenThrow(new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE));

    assertThatThrownBy(() -> paymentGatewayService.processPayment(request, idempotencyKey))
        .isInstanceOf(HttpServerErrorException.class);
  }

  @Test
  void whenPaymentExistsThenReturnsIt() {
    PostPaymentResponse existing = new PostPaymentResponse();
    UUID id = UUID.randomUUID();
    existing.setId(id);
    existing.setStatus(PaymentStatus.AUTHORIZED);
    existing.setAmount(1050);
    existing.setCurrency("GBP");

    paymentsRepository.add(existing);

    PostPaymentResponse response = paymentGatewayService.getPaymentById(id);

    assertThat(response.getId()).isEqualTo(id);
    assertThat(response.getStatus()).isEqualTo(PaymentStatus.AUTHORIZED);
  }

  @Test
  void whenPaymentDoesNotExistThenThrows() {
    UUID id = UUID.randomUUID();

    assertThatThrownBy(() -> paymentGatewayService.getPaymentById(id))
        .isInstanceOf(PaymentNotFoundException.class)
        .hasMessage("Payment with ID " + id + " not found");
  }
}
