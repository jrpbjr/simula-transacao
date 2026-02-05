package com.jrpbjr.transacao.api;

import com.jrpbjr.transacao.domain.PaymentStatus;
import com.jrpbjr.transacao.domain.PaymentType;

import java.math.BigDecimal;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        PaymentType type,
        PaymentStatus status,
        BigDecimal amount,
        String message
) {}
