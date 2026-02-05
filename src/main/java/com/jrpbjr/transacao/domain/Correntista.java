package com.jrpbjr.transacao.domain;


import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Column;
import jakarta.persistence.GenerationType;

import java.math.BigDecimal;

@Entity
@Table(name = "correntista")
public class Correntista {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 20)
    private String cpf;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal saldo = BigDecimal.ZERO;

    protected Correntista() {}

    public Correntista(String cpf, String nome, BigDecimal saldo) {
        this.cpf = cpf;
        this.nome = nome;
        this.saldo = saldo == null ? BigDecimal.ZERO : saldo;
    }

    public Long getId() { return id; }
    public String getCpf() { return cpf; }
    public String getNome() { return nome; }
    public BigDecimal getSaldo() { return saldo; }

    public void debitar(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Valor inválido");
        if (saldo.compareTo(valor) < 0) throw new IllegalStateException("Saldo insuficiente");
        saldo = saldo.subtract(valor);
    }

    public void creditar(BigDecimal valor) {
        if (valor == null || valor.signum() <= 0) throw new IllegalArgumentException("Valor inválido");
        saldo = saldo.add(valor);
    }
}
