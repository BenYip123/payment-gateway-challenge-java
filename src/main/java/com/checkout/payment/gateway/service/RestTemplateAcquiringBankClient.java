package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.AcquiringBankRequest;
import com.checkout.payment.gateway.model.AcquiringBankResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

@Service
public class RestTemplateAcquiringBankClient implements AcquiringBankClient {

  private final RestTemplate restTemplate;
  private final String url;

  public RestTemplateAcquiringBankClient(
      RestTemplate restTemplate,
      @Value("${acquiring-bank.url:http://localhost:8080}") String url) { // use http://localhost:8080 if acquiring bank not set to connect to simulator
    this.restTemplate = restTemplate;
    this.url = url;
  }

  @Override
  public AcquiringBankResponse process(AcquiringBankRequest request) {
    ResponseEntity<AcquiringBankResponse> response =
        restTemplate.postForEntity(url + "/payments", request, AcquiringBankResponse.class);
    if (response.getStatusCode() == HttpStatus.SERVICE_UNAVAILABLE) {
      throw new HttpServerErrorException(HttpStatus.SERVICE_UNAVAILABLE);
    }
    return response.getBody();
  }
}

