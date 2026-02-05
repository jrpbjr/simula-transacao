package com.jrpbjr.transacao;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableFeignClients
public class SimulaTransacaoApplication {

    public static void main(String[] args) {
        SpringApplication.run(SimulaTransacaoApplication.class, args);
    }

}
