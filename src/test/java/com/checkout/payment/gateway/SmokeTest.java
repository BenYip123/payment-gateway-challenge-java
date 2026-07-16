package com.checkout.payment.gateway;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SmokeTest {

  @Autowired
  private MockMvc mvc;

  @Test
  void appStartsAndHealthEndpointResponds() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk());
  }

  @Test
  void getPaymentReturns404ForUnknownId() throws Exception {
    mvc.perform(get("/v1/payment/00000000-0000-0000-0000-000000000000"))
        .andExpect(status().isNotFound());
  }

  @Test
  void getPaymentReturns400ForInvalidId() throws Exception {
    mvc.perform(get("/v1/payment/not-a-uuid"))
        .andExpect(status().isBadRequest());
  }
}
