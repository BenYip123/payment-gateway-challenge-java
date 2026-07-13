package com.checkout.payment.gateway.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class AcquiringBankResponse {

  private Boolean authorized;
  @JsonProperty("authorization_code")
  private String authorizationCode;

  public AcquiringBankResponse (Boolean authorized, String authorizationCode) {
    this.authorized = authorized;
    this.authorizationCode = authorizationCode;
  }

  public Boolean isAuthorized() {
    return authorized;
  }
}
