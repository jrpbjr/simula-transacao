package com.jrpbjr.transacao.service;

import com.jrpbjr.transacao.api.AccountResponse;
import com.jrpbjr.transacao.repository.CorrentistaRepository;
import org.springframework.stereotype.Service;

@Service
public class AccountService {

    private final CorrentistaRepository repo;

    public AccountService(CorrentistaRepository repo) {
        this.repo = repo;
    }

    public AccountResponse findById(Long id) {
        var c = repo.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));
        return new AccountResponse(c.getId(), c.getCpf(), c.getNome(), c.getSaldo());
    }
}