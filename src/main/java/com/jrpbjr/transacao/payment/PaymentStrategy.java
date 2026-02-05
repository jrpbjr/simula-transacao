package com.jrpbjr.transacao.payment;

import com.jrpbjr.transacao.domain.Pagamento;

public interface PaymentStrategy {
    boolean supports(com.jrpbjr.transacao.domain.PaymentType type);
    PaymentResult pay(Pagamento pagamento);
}