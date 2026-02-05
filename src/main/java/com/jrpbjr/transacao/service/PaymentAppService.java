package com.jrpbjr.transacao.service;

import com.jrpbjr.transacao.api.CreatePaymentRequest;
import com.jrpbjr.transacao.api.PaymentResponse;
import com.jrpbjr.transacao.domain.Pagamento;
import com.jrpbjr.transacao.payment.PaymentStrategyResolver;
import com.jrpbjr.transacao.repository.PagamentoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PaymentAppService {

    private final PagamentoRepository pagamentoRepo;
    private final PaymentStrategyResolver resolver;

    public PaymentAppService(PagamentoRepository pagamentoRepo, PaymentStrategyResolver resolver) {
        this.pagamentoRepo = pagamentoRepo;
        this.resolver = resolver;
    }

    @Transactional
    public PaymentResponse createAndProcess(CreatePaymentRequest req) {
        var pagamento = new Pagamento(
                req.type(),
                req.correntistaId(),
                req.amount(),
                req.pixReceiverKey()
        );

        pagamentoRepo.save(pagamento);

        var strategy = resolver.resolve(req.type());
        var result = strategy.pay(pagamento);

        if (result.success()) pagamento.aprovado(result.message());
        else pagamento.rejeitado(result.message());

        pagamentoRepo.save(pagamento);

        return new PaymentResponse(
                pagamento.getId(),
                pagamento.getTipo(),
                pagamento.getStatus(),
                pagamento.getValor(),
                pagamento.getMensagem()
        );
    }
}