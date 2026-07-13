package com.checkout.payment.gateway.controller;

import com.checkout.payment.gateway.enums.PaymentStatus;
import com.checkout.payment.gateway.model.PostPaymentRequest;
import com.checkout.payment.gateway.model.PostPaymentResponse;
import com.checkout.payment.gateway.service.PaymentGatewayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController("api")
@Tag(name = "Payments", description = "Payment gateway operations")
public class PaymentGatewayController {

  private final PaymentGatewayService paymentGatewayService;

  public PaymentGatewayController(PaymentGatewayService paymentGatewayService) {
    this.paymentGatewayService = paymentGatewayService;
  }

  @GetMapping("/v1/payment/{id}")
  @Operation(summary = "Get payment details", description = "Retrieve a payment by its ID")
  @ApiResponse(responseCode = "200", description = "Payment found")
  @ApiResponse(responseCode = "404", description = "Payment not found")
  public ResponseEntity<PostPaymentResponse> getPostPaymentEventById(@PathVariable UUID id) {
    return new ResponseEntity<>(paymentGatewayService.getPaymentById(id), HttpStatus.OK);
  }

  @PostMapping("/v1/payment")
  @Operation(summary = "Process a payment", description = "Submit a card payment for processing")
  @ApiResponse(responseCode = "201", description = "Payment processed successfully")
  @ApiResponse(responseCode = "200", description = "Payment rejected due to validation")
  @ApiResponse(responseCode = "502", description = "Bank service unavailable")
  public ResponseEntity<PostPaymentResponse> processPayment(@RequestBody PostPaymentRequest request) {
    PostPaymentResponse response = paymentGatewayService.processPayment(request);

    // when Payment status is rejected (usually from validation check), we return a 200
    HttpStatus status = response.getStatus() == PaymentStatus.REJECTED
        ? HttpStatus.OK
        : HttpStatus.CREATED;
    return new ResponseEntity<>(response, status);
  }

}
