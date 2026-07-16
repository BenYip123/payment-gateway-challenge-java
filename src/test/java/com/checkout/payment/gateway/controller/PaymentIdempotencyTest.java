package com.checkout.payment.gateway.controller;

import static com.checkout.payment.gateway.TestUtils.asJsonString;
import static com.checkout.payment.gateway.TestUtils.validRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.service.AcquiringBankClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIdempotencyTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Test
  void duplicateIdempotencyKeyReturnsSamePayment() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    String key = "test-idempotency-key-123";

    var result1 = mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn();

    var result2 = mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", key)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn();

    // Bank should only have been called once
    verify(acquiringBankClient, times(1)).process(any());

    // Second response returns the same body
    String body1 = result1.getResponse().getContentAsString();
    String body2 = result2.getResponse().getContentAsString();
    assertThat(body1).isEqualTo(body2);
  }

  @Test
  void differentIdempotencyKeysBothCallBank() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", "key-1")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())));

    mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", "key-2")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())));

    verify(acquiringBankClient, times(2)).process(any());
  }

  @Test
  void responseContainsIdempotencyKeyHeader() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", "my-key")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andExpect(status().isCreated())
        .andExpect(header().string("Idempotency-Key", "my-key"));
  }

  @Test
  void missingIdempotencyKeyReturns400() throws Exception {
    mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andExpect(status().isBadRequest());
  }
}
