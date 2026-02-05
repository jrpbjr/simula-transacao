package com.jrpbjr.transacao.receiver.api;

import com.jrpbjr.transacao.repository.CorrentistaRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;

@RestController
@RequestMapping("/api/pix")
public class PixReceiveController {

    private final CorrentistaRepository repo;

    public PixReceiveController(CorrentistaRepository repo) {
        this.repo = repo;
    }

    @PostMapping("/receive")
    public ReceivePixResponse receive(@RequestBody ReceivePixRequest req) {
        if (req.amount() == null || req.amount().signum() <= 0) {
            return new ReceivePixResponse(false, "Valor inválido");
        }
        if (req.receiverKey() == null || req.receiverKey().isBlank()) {
            return new ReceivePixResponse(false, "ReceiverKey inválida");
        }

        // receiverKey = CPF (simples)
        var receiver = repo.findByCpf(req.receiverKey())
                .orElse(null);

        if (receiver == null) {
            return new ReceivePixResponse(false, "Destinatário não encontrado");
        }

        // crédito (optimistic lock via @Version)
        int attempts = 0;
        while (true) {
            try {
                receiver.creditar(req.amount());
                repo.save(receiver);
                return new ReceivePixResponse(true, "Crédito aplicado para CPF " + req.receiverKey());
            } catch (OptimisticLockException e) {
                attempts++;
                if (attempts >= 3) throw e;
                try { Thread.sleep(30L * attempts); } catch (InterruptedException ignored) {}
            }
        }
    }

    public record ReceivePixRequest(String receiverKey, BigDecimal amount, String fromCpf) {}
    public record ReceivePixResponse(boolean received, String message) {}
}
