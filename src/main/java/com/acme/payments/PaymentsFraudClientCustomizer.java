package com.acme.payments;

import java.time.Duration;

import com.acme.security.FraudClientCustomizer;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.stereotype.Component;

/** Payments wants a tight timeout when talking to the fraud scorer. */
@Component
public class PaymentsFraudClientCustomizer implements FraudClientCustomizer {

    @Override
    public RestTemplateBuilder customize(RestTemplateBuilder builder) {
        return builder.connectTimeout(Duration.ofSeconds(1)).readTimeout(Duration.ofSeconds(2));
    }
}
