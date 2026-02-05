package com.jrpbjr.transacao.payment;


import com.jrpbjr.transacao.domain.PaymentType;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PaymentStrategyResolver {

    private final List<PaymentStrategy> strategies;

    public PaymentStrategyResolver(List<PaymentStrategy> strategies) {
        this.strategies = strategies;
    }

    public PaymentStrategy resolve(PaymentType type) {
        return strategies.stream()
                .filter(s -> s.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Pagamento não suportado: " + type));
    }
}