package com.jrpbjr.transacao.integration;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.math.BigDecimal;

@FeignClient(name = "pixReceiverClient", url = "${pix.receiver.base-url}")
public interface PixReceiverClient {

    @PostMapping("/api/pix/receive")
    ReceivePixResponse receive(@RequestBody ReceivePixRequest request);

    record ReceivePixRequest(String receiverKey, BigDecimal amount, String fromCpf) {}
    record ReceivePixResponse(boolean received, String message) {}
}
