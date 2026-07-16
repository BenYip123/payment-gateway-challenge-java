package com.checkout.payment.gateway.filter;

import static com.checkout.payment.gateway.TestUtils.asJsonString;
import static com.checkout.payment.gateway.TestUtils.validRequest;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.service.AcquiringBankClient;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  private String idempotencyKey;

  @BeforeEach
  void setUp() {
    idempotencyKey = UUID.randomUUID().toString();
  }

  @Test
  void requestHasCorrelationId() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    var result = mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn();

    String correlationId = result.getResponse().getHeader("X-Correlation-Id");
    assertThat(correlationId).isNotBlank();
  }

  @Test
  void correlationIdsAreDifferentForEachRequest() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    String id1 = mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn().getResponse().getHeader("X-Correlation-Id");

    String id2 = mvc.perform(post("/v1/payment")
            .header("Idempotency-Key", idempotencyKey)
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn().getResponse().getHeader("X-Correlation-Id");

    assertThat(id1).isNotEqualTo(id2);
  }
}
