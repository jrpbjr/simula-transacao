package com.jrpbjr.transacao.service;

import com.jrpbjr.transacao.api.CreatePaymentRequest;
import com.jrpbjr.transacao.api.PaymentResponse;
import com.jrpbjr.transacao.domain.Pagamento;
import com.jrpbjr.transacao.domain.PaymentStatus;
import com.jrpbjr.transacao.domain.PaymentType;
import com.jrpbjr.transacao.integration.PixReceiverClient;
import com.jrpbjr.transacao.payment.PaymentStrategyResolver;
import com.jrpbjr.transacao.repository.CorrentistaRepository;
import com.jrpbjr.transacao.repository.PagamentoRepository;
import jakarta.persistence.OptimisticLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class PaymentAppService {

    private static final int MAX_RETRIES = 3;

    private final PagamentoRepository pagamentoRepo;
    private final CorrentistaRepository correntistaRepo;
    private final PixReceiverClient receiverClient;
    private final PaymentStrategyResolver resolver;

    public PaymentAppService(PagamentoRepository pagamentoRepo,
                             CorrentistaRepository correntistaRepo,
                             PixReceiverClient receiverClient,
                             PaymentStrategyResolver resolver) {
        this.pagamentoRepo = pagamentoRepo;
        this.correntistaRepo = correntistaRepo;
        this.receiverClient = receiverClient;
        this.resolver = resolver;
    }

    /**
     * Regras:
     * - PIX exige idempotencyKey
     * - Se idempotencyKey já existir: devolve o mesmo pagamento (não debita e não chama Feign)
     * - Se for novo: cria pagamento PENDING (TX curta)
     *   - Cartão/Boleto: processa via Strategy (debita saldo) e finaliza
     *   - PIX: debita (TX curta) -> chama receiver (fora TX) -> aprova ou compensa/estorna
     */
    public PaymentResponse createAndProcess(CreatePaymentRequest req) {

        //  Idempotência (somente PIX)
        if (req.type() == PaymentType.PIX) {
            if (req.idempotencyKey() == null || req.idempotencyKey().isBlank()) {
                throw new IllegalArgumentException("PIX exige idempotencyKey");
            }

            var existing = pagamentoRepo.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) {
                // Já foi processado (APPROVED/REJECTED/PENDING) -> devolve o mesmo resultado
                return toResponse(existing.get());
            }
        }

        // Cria pagamento e/ou aplica débito (TX curta)
        UUID pagamentoId = applyDebitWithRetry(req);

        var pagamento = pagamentoRepo.findById(pagamentoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado"));

        // Se já saiu REJECTED (ex: PIX sem receiverKey), devolve
        if (pagamento.getStatus() != PaymentStatus.PENDING) {
            return toResponse(pagamento);
        }

        // Cartão/Boleto: processa via Strategy (debita saldo)
        if (req.type() == PaymentType.CREDIT_CARD || req.type() == PaymentType.BOLETO) {

            var strategy = resolver.resolve(req.type());
            var result = strategy.pay(pagamento);

            if (result.success()) {
                // marca para auditoria/consistência (opcional, mas útil)
                markDebitAppliedIfNeeded(pagamentoId);
                finalizeApproved(pagamentoId, result.message());
            } else {
                // cartão/boleto: aqui não precisa refundTx (você pode criar finalizeRejected simples),
                // mas mantendo o fluxo consistente:
                finalizeRejectedWithRefund(pagamentoId, result.message());
            }

            return toResponse(pagamentoRepo.findById(pagamentoId).orElseThrow());
        }

        // ===== 4) PIX: chama receiver (fora de TX) =====
        try {
            var pagador = correntistaRepo.findById(req.correntistaId())
                    .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

            var resp = receiverClient.receive(
                    new PixReceiverClient.ReceivePixRequest(
                            req.pixReceiverKey(),
                            req.amount(),
                            pagador.getCpf()
                    )
            );

            if (resp.received()) {
                finalizeApproved(pagamentoId, "PIX OK: " + resp.message());
            } else {
                finalizeRejectedWithRefund(pagamentoId, "Receiver recusou: " + resp.message());
            }
        } catch (Exception ex) {
            finalizeRejectedWithRefund(pagamentoId, "Falha ao chamar receiver: " + ex.getMessage());
        }

        return toResponse(pagamentoRepo.findById(pagamentoId).orElseThrow());
    }

    // =========================
    // Helpers
    // =========================

    private PaymentResponse toResponse(Pagamento p) {
        return new PaymentResponse(
                p.getId(),
                p.getTipo(),
                p.getStatus(),
                p.getValor(),
                p.getMensagem()
        );
    }

    // -------------------------
    // TX curta: cria pagamento / debita PIX
    // -------------------------

    private UUID applyDebitWithRetry(CreatePaymentRequest req) {
        int attempt = 0;
        while (true) {
            try {
                return applyDebitTx(req);
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) throw e;
                sleepBackoff(attempt);
            }
        }
    }

    /**
     * TX curta:
     * - (PIX) re-checa idempotencyKey (proteção contra corrida)
     * - cria Pagamento PENDING
     * - valida PIX receiverKey
     * - debita saldo SOMENTE para PIX aqui (cartão/boleto debitam na Strategy)
     * - marca debitApplied no PIX
     */
    @Transactional
    protected UUID applyDebitTx(CreatePaymentRequest req) {

        // Proteção extra contra corrida (PIX)
        if (req.type() == PaymentType.PIX && req.idempotencyKey() != null && !req.idempotencyKey().isBlank()) {
            var existing = pagamentoRepo.findByIdempotencyKey(req.idempotencyKey());
            if (existing.isPresent()) return existing.get().getId();
        }

        var pagamento = new Pagamento(
                req.type(),
                req.correntistaId(),
                req.amount(),
                req.pixReceiverKey(),
                req.idempotencyKey()
        );
        pagamentoRepo.save(pagamento);

        // Cartão/Boleto: não debita aqui -> será debitado na Strategy
        if (req.type() != PaymentType.PIX) {
            return pagamento.getId();
        }

        // PIX: valida receiverKey
        if (req.pixReceiverKey() == null || req.pixReceiverKey().isBlank()) {
            pagamento.rejeitado("PIX precisa de chave/CPF do destinatário");
            pagamentoRepo.save(pagamento);
            return pagamento.getId();
        }

        // PIX: debita saldo
        var correntista = correntistaRepo.findById(req.correntistaId())
                .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

        correntista.debitar(req.amount());
        correntistaRepo.save(correntista);

        pagamento.markDebitApplied();
        pagamentoRepo.save(pagamento);

        return pagamento.getId();
    }

    // -------------------------
    //
    // -------------------------

    @Transactional
    protected void finalizeApproved(UUID pagamentoId, String msg) {
        var pagamento = pagamentoRepo.findById(pagamentoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado"));

        if (pagamento.getStatus() != PaymentStatus.PENDING) return;

        pagamento.aprovado(msg);
        pagamentoRepo.save(pagamento);
    }

    private void finalizeRejectedWithRefund(UUID pagamentoId, String reason) {
        int attempt = 0;
        while (true) {
            try {
                refundTx(pagamentoId, reason);
                return;
            } catch (OptimisticLockException e) {
                attempt++;
                if (attempt >= MAX_RETRIES) throw e;
                sleepBackoff(attempt);
            }
        }
    }

    @Transactional
    protected void refundTx(UUID pagamentoId, String reason) {
        var pagamento = pagamentoRepo.findById(pagamentoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado"));

        if (pagamento.getStatus() != PaymentStatus.PENDING) return;

        // refund só faz sentido para PIX (aqui)
        if (pagamento.isDebitApplied() && pagamento.getTipo() == PaymentType.PIX) {
            var correntista = correntistaRepo.findById(pagamento.getCorrentistaId())
                    .orElseThrow(() -> new IllegalArgumentException("Correntista não encontrado"));

            correntista.creditar(pagamento.getValor());
            correntistaRepo.save(correntista);
        }

        pagamento.rejeitado("Operação rejeitada: " + reason);
        pagamentoRepo.save(pagamento);
    }

    /**
     * Se cartão/boleto debitou via Strategy e você quer refletir isso no Pagamento,
     * marca debitApplied=true.
     *
     * (Opcional, mas ajuda auditoria e futuro refund/estornos)
     */
    @Transactional
    protected void markDebitAppliedIfNeeded(UUID pagamentoId) {
        var pagamento = pagamentoRepo.findById(pagamentoId)
                .orElseThrow(() -> new IllegalStateException("Pagamento não encontrado"));

        if (pagamento.getStatus() != PaymentStatus.PENDING) return;

        if (!pagamento.isDebitApplied()) {
            pagamento.markDebitApplied();
            pagamentoRepo.save(pagamento);
        }
    }

    private void sleepBackoff(int attempt) {
        try {
            Thread.sleep(30L * attempt);
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
    }
}