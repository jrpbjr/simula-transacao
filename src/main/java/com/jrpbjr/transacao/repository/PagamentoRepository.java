package com.jrpbjr.transacao.repository;

import com.jrpbjr.transacao.domain.Pagamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PagamentoRepository extends JpaRepository<Pagamento, UUID> { }