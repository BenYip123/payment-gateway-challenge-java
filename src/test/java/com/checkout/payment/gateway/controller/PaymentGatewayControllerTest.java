package com.checkout.payment.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import java.util.UUID;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayControllerTest {

  @Autowired
  private MockMvc mvc;
  @Autowired
  PaymentsRepository paymentsRepository;

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

  // requires simulator to be running
  @Test
  void whenValidOddCardThenAuthorized() throws Exception {
    PostPaymentRequest request = validRequest();

    MvcResult result = mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andReturn();

    // check the payment has been stored
    String id = new ObjectMapper().readTree(result.getResponse().getContentAsString())
        .get("id").asText();
    assertThat(paymentsRepository.get(UUID.fromString(id))).isPresent();
  }

  // requires simulator to be running
  @Test
  void whenEvenCardThenDeclined() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4242405343248872");

    MvcResult result = mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.DECLINED.getName()))
        .andReturn();

    // check the payment has been stored
    String id = new ObjectMapper().readTree(result.getResponse().getContentAsString())
        .get("id").asText();
    assertThat(paymentsRepository.get(UUID.fromString(id))).isPresent();
  }

  // requires simulator to be running
  @Test
  void whenZeroCardThen502() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4242405343248870");

    // when card number ends with zero, the simulator returns a 503 without doing anything else
    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isBadGateway());
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



  private static PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4242405343248871");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }

  private static String asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
