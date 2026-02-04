# 💳 Sistema de Pagamentos Distribuído (PIX, Cartão e Boleto)

Este projeto demonstra a implementação de um **sistema de pagamentos distribuído**, desenvolvido com **Spring Boot (Java 21)**, utilizando **PostgreSQL**, **Docker**, **Feign Client** e padrões de arquitetura modernos como **Strategy**, **Idempotência**, **Optimistic Locking** e **Compensação (SAGA simplificada)**.

O objetivo é simular um cenário realista de **transferência via PIX entre dois serviços independentes** (Pagador e Recebedor), garantindo **consistência, segurança e tolerância a falhas**.

---

## 🧱 Arquitetura Geral

- **payer-service (8080)**  
  Serviço responsável por:
  - Gerenciar correntistas pagadores
  - Processar pagamentos (PIX, Cartão, Boleto)
  - Debitar saldo
  - Aplicar idempotência
  - Executar compensação em caso de falha

- **receiver-service (8081)**  
  Serviço responsável por:
  - Gerenciar correntistas recebedores
  - Receber e creditar PIX
  - Garantir concorrência segura no saldo

- **PostgreSQL**
  - Banco independente para cada serviço
  - Dados inicializados automaticamente via `data.sql`

---

## 🧠 Principais Conceitos Aplicados

### ✅ Strategy Pattern
Cada tipo de pagamento possui sua própria estratégia:
- `PixPaymentStrategy`
- `CreditCardPaymentStrategy`
- `BoletoPaymentStrategy`

Isso elimina `if/else` extensos e facilita a adição de novos meios de pagamento.

---

### 🔁 Idempotência (PIX)
- Implementada via `idempotencyKey`
- Garante que **a mesma requisição não seja processada mais de uma vez**
- Evita:
  - Débitos duplicados
  - Chamadas repetidas ao serviço recebedor

---

### 🔐 Concorrência com Optimistic Lock
- Uso de `@Version` na entidade `Correntista`
- Evita inconsistências em operações simultâneas de débito/crédito
- Retry automático em caso de conflito de versão

---

### 🔄 Compensação (SAGA simplificada)
- Se o débito for realizado mas o crédito falhar:
  - O sistema executa **estorno automático**
- Garante **consistência eventual** entre os serviços

---

## 🛠️ Tecnologias Utilizadas

- Java 21
- Spring Boot
- Spring Data JPA
- Spring Cloud OpenFeign
- PostgreSQL
- Docker & Docker Compose
- Maven
- REST APIs

---

## 📦 Estrutura do Projeto

