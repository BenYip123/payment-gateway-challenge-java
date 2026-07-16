package com.checkout.payment.gateway.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.repository.PaymentsRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static com.checkout.payment.gateway.TestUtils.validRequest;
import static com.checkout.payment.gateway.TestUtils.asJsonString;

/**
 * Integration tests that require the mountebank simulator to be running on port 8080.
 * Run with: docker compose up -d bank_simulator
 */
@SpringBootTest
@AutoConfigureMockMvc
class PaymentGatewayIntegrationTest {

  @Autowired
  private MockMvc mvc;

  @Autowired
  PaymentsRepository paymentsRepository;

  @Test
  void whenValidOddCardThenAuthorized() throws Exception {
    PostPaymentRequest request = validRequest();

    MvcResult result = mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value(PaymentStatus.AUTHORIZED.getName()))
        .andReturn();

    String id = new ObjectMapper().readTree(result.getResponse().getContentAsString())
        .get("id").asText();
    assertThat(paymentsRepository.get(UUID.fromString(id))).isPresent();
  }

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

    String id = new ObjectMapper().readTree(result.getResponse().getContentAsString())
        .get("id").asText();
    assertThat(paymentsRepository.get(UUID.fromString(id))).isPresent();
  }

  @Test
  void whenZeroCardThen502() throws Exception {
    PostPaymentRequest request = validRequest();
    request.setCardNumber("4242405343248870");

    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(request)))
        .andExpect(status().isBadGateway());
  }
}
