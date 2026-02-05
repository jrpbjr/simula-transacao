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
## 🧪 Testes no Postman
🔧 Variáveis de Ambiente (opcional)
- Crie um Environment no Postman:

- baseUrl = http://localhost:8080
---
## 1️⃣ Consultar saldo do correntista
- GET {{baseUrl}}/api/accounts/1
- Resposta:
{
"id": 1,
"cpf": "111.111.111-01",
"nome": "Correntista 01",
"saldo": 200.00
}
---
## 2️⃣ Pagamento via PIX (com idempotência)
- POST {{baseUrl}}/api/payments
- Body:
{
"type": "PIX",
"correntistaId": 1,
"amount": 50.00,
"pixReceiverKey": "222.222.222-01",
"idempotencyKey": "pix-ord-0004"
}
Resposta:
{
"id": "75ced8b8-b378-454c-8e46-baa16ce7d180",
"type": "PIX",
"status": "APPROVED",
"amount": 50.00,
"message": "PIX OK: Crédito aplicado para CPF 222.222.222-01"

}

##📌 Reenviar a mesma requisição com o mesmo idempotencyKey não gera novo débito.
---
## 3️⃣ Pagamento com Cartão de Crédito
- POST {{baseUrl}}/api/payments
- Body:
{
"type": "CREDIT_CARD",
"correntistaId": 1,
"amount": 50.00
}

- Resposta:
{
"id": "013b9988-63fa-44db-9bd0-b2ca0e7aa008",
"type": "CREDIT_CARD",
"status": "APPROVED",
"amount": 50.00,
"message": "Cartão aprovado (simulado)"
}
---
## 4️⃣ Pagamento via Boleto

- POST {{baseUrl}}/api/payments
- Body:
{
"type": "BOLETO",
"correntistaId": 1,
"amount": 50.00
} 

- Resposta:
{
"id": "f4386785-cd7f-40e6-955a-2b0450367ec3",
"type": "BOLETO",
"status": "APPROVED",
"amount": 50.00,
"message": "Boleto gerado: 34191.79001 01043.510047 91020.150008 5 12340000010000 (simulado)"
}
---
## ✅ Validação pós-pagamento
- Consulte novamente o saldo:

- GET {{baseUrl}}/api/accounts/1

- E valida a atualização conforme as regras de débito de cada estratégia.
---
## 📦 Estrutura do Projeto

