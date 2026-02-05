package com.jrpbjr.transacao.payment;

import com.jrpbjr.transacao.domain.Pagamento;
import com.jrpbjr.transacao.domain.PaymentType;
import com.jrpbjr.transacao.integration.PixReceiverClient;
import com.jrpbjr.transacao.repository.CorrentistaRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PixPaymentStrategy implements PaymentStrategy {

    private final CorrentistaRepository correntistaRepo;
    private final PixReceiverClient receiverClient;

    public PixPaymentStrategy(CorrentistaRepository correntistaRepo, PixReceiverClient receiverClient) {
        this.correntistaRepo = correntistaRepo;
        this.receiverClient = receiverClient;
    }

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.PIX;
    }

    @Override
    @Transactional
    public PaymentResult pay(Pagamento pagamento) {
        if (pagamento.getDestinatario() == null || pagamento.getDestinatario().isBlank()) {
            return new PaymentResult(false, "PIX precisa de chave/CPF do destinatário");
        }

        var pagador = correntistaRepo.findById(pagamento.getCorrentistaId())
                .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

        // debita local
        pagador.debitar(pagamento.getValor());
        correntistaRepo.save(pagador);

        // simula recebedor em outro serviço
        var resp = receiverClient.receive(
                new PixReceiverClient.ReceivePixRequest(
                        pagamento.getDestinatario(),
                        pagamento.getValor(),
                        pagador.getCpf()
                )
        );

        if (!resp.received()) {
            // Em projeto real: compensação/saga (recreditar ou reverter)
            return new PaymentResult(false, "Falha ao creditar recebedor: " + resp.message());
        }

        return new PaymentResult(true, "PIX enviado com sucesso: " + resp.message());
    }
}
