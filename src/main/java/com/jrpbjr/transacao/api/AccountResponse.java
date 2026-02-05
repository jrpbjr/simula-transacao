package com.jrpbjr.transacao.api;

import java.math.BigDecimal;

public record AccountResponse(
        Long id,
        String cpf,
        String nome,
        BigDecimal saldo
) {}
