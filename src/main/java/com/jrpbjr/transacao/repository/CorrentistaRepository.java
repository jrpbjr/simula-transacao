package com.jrpbjr.transacao.repository;

import com.jrpbjr.transacao.domain.Correntista;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CorrentistaRepository extends JpaRepository<Correntista, Long> {
    Optional<Correntista> findByCpf(String cpf);
}