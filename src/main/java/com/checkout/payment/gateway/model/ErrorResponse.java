package com.checkout.payment.gateway.model;

import java.util.List;

public class ErrorResponse {
  // use a list of Strings to store multiple error messages (e.g. validation errors)
  private final List<String> messages;

  public ErrorResponse(List<String> messages) {
    this.messages = messages;
  }

  public List<String> getMessages() {
    return messages;
  }

  @Override
  public String toString() {
    return "ErrorResponse{" +
        "messages='" + messages + '\'' +
        '}';
  }
}
