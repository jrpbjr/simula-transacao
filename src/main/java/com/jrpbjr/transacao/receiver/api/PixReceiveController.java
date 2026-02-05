package com.jrpbjr.transacao.receiver.api;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pix")
public class PixReceiveController {

    @PostMapping("/receive")
    public ReceivePixResponse receive(@RequestBody ReceivePixRequest req) {
        // Simula crédito no recebedor
        if (req.amount() == null || req.amount().signum() <= 0) {
            return new ReceivePixResponse(false, "Valor inválido");
        }
        return new ReceivePixResponse(true, "Recebedor " + req.receiverKey() + " creditado com " + req.amount());
    }

    public record ReceivePixRequest(String receiverKey, BigDecimal amount, String fromCpf) {}
    public record ReceivePixResponse(boolean received, String message) {}
}
