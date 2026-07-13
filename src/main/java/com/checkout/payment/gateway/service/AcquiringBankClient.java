package com.checkout.payment.gateway.service;

import com.checkout.payment.gateway.model.AcquiringBankRequest;
import com.checkout.payment.gateway.model.AcquiringBankResponse;

public interface AcquiringBankClient {
  AcquiringBankResponse process(AcquiringBankRequest request);
}
