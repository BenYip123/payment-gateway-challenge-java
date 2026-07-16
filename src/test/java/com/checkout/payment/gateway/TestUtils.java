package com.checkout.payment.gateway;

import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class TestUtils {

  public static PostPaymentRequest validRequest() {
    PostPaymentRequest request = new PostPaymentRequest();
    request.setCardNumber("4242405343248871");
    request.setExpiryMonth(12);
    request.setExpiryYear(2027);
    request.setCurrency("GBP");
    request.setAmount(1050);
    request.setCvv("123");
    return request;
  }

  public static String asJsonString(Object obj) {
    try {
      return new ObjectMapper().writeValueAsString(obj);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}