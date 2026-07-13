package com.checkout.payment.gateway.model;

import java.util.List;

public class ErrorResponse {
  // use a list of Strings to store multiple error messages (e.g. validation errors)
  private final List<String> message;

  public ErrorResponse(List<String> messages) {
    this.message = messages;
  }

  public List<String> getMessage() {
    return message;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "message='" + message + '\'' +
        '}';
  }
}
