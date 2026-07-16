package com.checkout.payment.gateway.filter;

import com.checkout.payment.gateway.model.AcquiringBankResponse;
import com.checkout.payment.gateway.service.AcquiringBankClient;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static com.checkout.payment.gateway.TestUtils.validRequest;
import static com.checkout.payment.gateway.TestUtils.asJsonString;

@SpringBootTest
@AutoConfigureMockMvc
class CorrelationIdFilterTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private AcquiringBankClient acquiringBankClient;

  @Test
  void requestHasCorrelationId() throws Exception {
    when(acquiringBankClient.process(any()))
        .thenReturn(new AcquiringBankResponse(true, "auth-code"));

    var result = mvc.perform(post("/v1/payment")
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
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn().getResponse().getHeader("X-Correlation-Id");

    String id2 = mvc.perform(post("/v1/payment")
            .contentType(MediaType.APPLICATION_JSON)
            .content(asJsonString(validRequest())))
        .andReturn().getResponse().getHeader("X-Correlation-Id");

    assertThat(id1).isNotEqualTo(id2);
  }
}
