package com.acme.payments;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.UUID;

import com.acme.security.FraudDecision;
import com.acme.security.FraudScreeningService;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PaymentController {

    public record PaymentRequest(BigDecimal amount, String currency) { }

    public record PaymentResponse(String id, String merchant, String status, int fraudScore) { }

    private final FraudScreeningService fraud;

    public PaymentController(FraudScreeningService fraud) {
        this.fraud = fraud;
    }

    /** Business operation: authorize a payment for the (signature-authenticated) merchant. */
    @PostMapping("/payments")
    public PaymentResponse authorize(@RequestBody PaymentRequest request, Principal merchant) {
        FraudDecision decision = fraud.screen(merchant.getName(), request.amount(), request.currency());
        return new PaymentResponse(UUID.randomUUID().toString(), merchant.getName(),
                decision.approved() ? "AUTHORIZED" : "DECLINED", decision.score());
    }
}
