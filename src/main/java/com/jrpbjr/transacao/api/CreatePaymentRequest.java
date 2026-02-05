package com.jrpbjr.transacao.api;

import com.jrpbjr.transacao.domain.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record CreatePaymentRequest(
        @NotNull PaymentType type,
        @NotNull Long correntistaId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        // usado para PIX (cpf/chave do destinatário); pode ser null em boleto/cartão
        String pixReceiverKey,
        @jakarta.validation.constraints.Size(min = 8, max = 100)
                String idempotencyKey
) {}