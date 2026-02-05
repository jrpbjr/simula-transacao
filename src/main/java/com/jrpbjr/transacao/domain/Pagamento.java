package com.jrpbjr.transacao.domain;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "pagamento")
public class Pagamento {

    @Id
    private UUID id;

    @Column(nullable = false)
    private Instant criadoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentType tipo;

    @Column(nullable = false)
    private Long correntistaId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal valor;

    // Para PIX: CPF do favorecido (destino) ou “chave pix”
    @Column(length = 50)
    private String destinatario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PaymentStatus status;

    @Column(length = 255)
    private String mensagem;

    protected Pagamento() {}

    public Pagamento(PaymentType tipo, Long correntistaId, BigDecimal valor, String destinatario) {
        this.id = UUID.randomUUID();
        this.criadoEm = Instant.now();
        this.tipo = tipo;
        this.correntistaId = correntistaId;
        this.valor = valor;
        this.destinatario = destinatario;
        this.status = PaymentStatus.PENDING;
    }

    public UUID getId() { return id; }
    public PaymentType getTipo() { return tipo; }
    public Long getCorrentistaId() { return correntistaId; }
    public BigDecimal getValor() { return valor; }
    public String getDestinatario() { return destinatario; }
    public PaymentStatus getStatus() { return status; }
    public String getMensagem() { return mensagem; }

    public void aprovado(String msg) { this.status = PaymentStatus.APPROVED; this.mensagem = msg; }
    public void rejeitado(String msg) { this.status = PaymentStatus.REJECTED; this.mensagem = msg; }
}
