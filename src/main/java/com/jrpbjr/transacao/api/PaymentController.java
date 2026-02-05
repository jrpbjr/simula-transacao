package com.jrpbjr.transacao.api;

import com.jrpbjr.transacao.service.PaymentAppService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentAppService service;

    public PaymentController(PaymentAppService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<PaymentResponse> pay(@Valid @RequestBody CreatePaymentRequest request) {
        return ResponseEntity.ok(service.createAndProcess(request));
    }
}
