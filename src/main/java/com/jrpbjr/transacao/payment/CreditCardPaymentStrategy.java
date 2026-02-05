package com.jrpbjr.transacao.payment;

import com.jrpbjr.transacao.domain.Pagamento;
import com.jrpbjr.transacao.domain.PaymentType;
import com.jrpbjr.transacao.repository.CorrentistaRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.stereotype.Component;

@Component
public class CreditCardPaymentStrategy implements PaymentStrategy {

    private final CorrentistaRepository correntistaRepo;

    public CreditCardPaymentStrategy(CorrentistaRepository correntistaRepo) {
        this.correntistaRepo = correntistaRepo;
    }

    @Override
    public boolean supports(PaymentType type) {
        return type == PaymentType.CREDIT_CARD;
    }

    @Override
    @Transactional
    public PaymentResult pay(Pagamento pagamento) {
        var pagador = correntistaRepo.findById(pagamento.getCorrentistaId())
                .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

        // regra: debita do saldo (simulação)
        pagador.debitar(pagamento.getValor());
        correntistaRepo.save(pagador);

        // aqui você poderia “chamar adquirente” (simulado)
        return new PaymentResult(true, "Cartão aprovado (simulado)");
    }
}

