package com.checkout.payment.gateway.controller;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.checkout.payment.gateway.service.AcquiringBankClient;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static com.checkout.payment.gateway.TestUtils.validRequest;
import static com.checkout.payment.gateway.TestUtils.asJsonString;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Test
  void whenPaymentWithIdExistThenCorrectPaymentIsReturned() throws Exception {
    PostPaymentResponse payment = new PostPaymentResponse();
    payment.setId(UUID.randomUUID());
    payment.setAmount(10);
    payment.setCurrency("GBP");
    payment.setStatus(PaymentStatus.AUTHORIZED);
    payment.setExpiryMonth(12);
    payment.setExpiryYear(2024);
    payment.setCardNumberLastFour("4321");

    paymentsRepository.add(payment);

    mvc.perform(MockMvcRequestBuilders.get("/v1/payment/" + payment.getId()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(payment.getStatus().getName()))
        .andExpect(jsonPath("$.card_number_last_four").value(payment.getCardNumberLastFour()))
        .andExpect(jsonPath("$.expiry_month").value(payment.getExpiryMonth()))
        .andExpect(jsonPath("$.expiry_year").value(payment.getExpiryYear()))
        .andExpect(jsonPath("$.currency").value(payment.getCurrency()))
        .andExpect(jsonPath("$.amount").value(payment.getAmount()));
  }

  @Test
  void whenPaymentWithIdDoesNotExistThen404IsReturned() throws Exception {
    mvc.perform(MockMvcRequestBuilders.get("/v1/payment/" + UUID.randomUUID()))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.message[0]").value("Payment not found"));
  }

  @Test
  void whenCurrencyIsNotSupportedThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCurrency("HKD");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Currency is not supported")));
  }

  @Test
  void whenExpiryDateIsPastThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setExpiryYear(2020);

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Expiry date must be in the future")));
  }

  @Test
  void whenAFieldIsMissingThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCvv("");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("must not be blank")));
  }

  @Test
  void whenCardNumberWrongSizeAndLettersThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("123abc");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Card number must be 14-19 numeric characters")))
        .andExpect(jsonPath("$.errors").value(hasItem("Card number must only contain numeric characters")));
  }

  @Test
  void whenExpiryMonthIsInvalidThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setExpiryMonth(13);

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Expiry month must be between 1 and 12")));
  }

  @Test
  void whenCurrencyIsInvalidThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCurrency("A23");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Currency must be 3 letters")));
  }

  @Test
  void whenAmountIsNegativeThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setAmount(-1000);

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("Amount must be a positive integer")));
  }

  @Test
  void whenCVVIsInvalidThenRejected() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCvv("ABC");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value(PaymentStatus.REJECTED.getName()))
        .andExpect(jsonPath("$.errors").value(hasItem("CVV must be 3-4 numeric characters")));
  }

  // Verify response has the expected field types
  @Test
  void postPaymentResponseContainsOnlyRequiredFields() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    PostPaymentRequest request = validRequest();

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").isString())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andExpect(jsonPath("$.card_number_last_four").isString())
        .andExpect(jsonPath("$.expiry_month").isNumber())
        .andExpect(jsonPath("$.expiry_year").isNumber())
        .andExpect(jsonPath("$.currency").isString())
        .andExpect(jsonPath("$.amount").isNumber());
  }
}
