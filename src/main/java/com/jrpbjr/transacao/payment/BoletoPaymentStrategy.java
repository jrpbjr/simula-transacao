package com.jrpbjr.transacao.payment;

import com.jrpbjr.transacao.domain.Pagamento;
import com.jrpbjr.transacao.domain.PaymentType;
import com.jrpbjr.transacao.repository.CorrentistaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

@Component
public class BoletoPaymentStrategy implements PaymentStrategy {

    private final CorrentistaRepository correntistaRepo;

    public BoletoPaymentStrategy(CorrentistaRepository correntistaRepo) {
        this.correntistaRepo = correntistaRepo;
    }

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.BOLETO;
    }

    @Override
    @Transactional
    public PaymentResult pay(Pagamento pagamento) {
        var pagador = correntistaRepo.findById(pagamento.getCorrentistaId())
                .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

        pagador.debitar(pagamento.getValor());
        correntistaRepo.save(pagador);

        return new PaymentResult(true,
                "Boleto gerado: 34191.79001 01043.510047 91020.150008 5 12340000010000 (simulado)");
    }
}