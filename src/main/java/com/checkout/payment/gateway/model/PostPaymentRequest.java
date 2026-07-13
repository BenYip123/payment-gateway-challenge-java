package com.checkout.payment.gateway.model;

import com.checkout.payment.gateway.validation.annotations.SupportedCurrency;
import com.checkout.payment.gateway.validation.annotations.ValidExpiryDate;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.io.Serializable;

@ValidExpiryDate
public class PostPaymentRequest implements Serializable {

  @JsonProperty("card_number")
  @NotBlank
  @Size(min = 14, max = 19, message = "Card number must be 14-19 numeric characters")
  @Pattern(regexp = "\\d+", message = "Card number must only contain numeric characters")
  private String cardNumber;

  @JsonProperty("expiry_month")
  @NotNull
  @Min(value = 1, message = "Expiry month must be between 1 and 12")
  @Max(value = 12, message = "Expiry month must be between 1 and 12")
  private int expiryMonth;

  @JsonProperty("expiry_year")
  @NotNull
  private int expiryYear;

  @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be 3 letters")
  @NotBlank
  @SupportedCurrency
  private String currency;

  @NotNull
  @Positive(message = "Amount must be a positive integer")
  private int amount;

  @NotBlank
  @Pattern(regexp = "\\d{3,4}", message = "CVV must be 3-4 numeric characters")
  private String cvv;

  public String getCardNumber() {
    return cardNumber;
  }

  public void setCardNumber(String cardNumber) {
    this.cardNumber = cardNumber;
  }

  public int getExpiryMonth() {
    return expiryMonth;
  }

  public void setExpiryMonth(int expiryMonth) {
    this.expiryMonth = expiryMonth;
  }

  public int getExpiryYear() {
    return expiryYear;
  }

  public void setExpiryYear(int expiryYear) {
    this.expiryYear = expiryYear;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public int getAmount() {
    return amount;
  }

  public void setAmount(int amount) {
    this.amount = amount;
  }

  public String getCvv() {
    return cvv;
  }

  public void setCvv(String cvv) {
    this.cvv = cvv;
  }

  @JsonProperty("expiry_date")
  public String getExpiryDate() {
    return String.format("%d/%d", expiryMonth, expiryYear);
  }

  public String getCardNumberLastFour() {
    if (cardNumber == null || cardNumber.length() < 4) {
      return cardNumber;
    }
    return cardNumber.substring(cardNumber.length() - 4);
  }

  @Override
  public String toString() {
    return "PostPaymentRequest{" +
        "cardNumberLastFour=" + getCardNumberLastFour() +
        ", expiryMonth=" + expiryMonth +
        ", expiryYear=" + expiryYear +
        ", currency='" + currency + '\'' +
        ", amount=" + amount +
        ", cvv=" + cvv +
        '}';
  }
}
