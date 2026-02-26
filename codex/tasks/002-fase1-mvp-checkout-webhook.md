# TASK 002 — Fase 1 (MVP Happy Path): Webhook GET/POST + Token + Checkout sem TX longa + Testes P0

## Objetivo
Estabilizar o caminho feliz (checkout -> pagamento -> webhook -> decisão) com correções mínimas de operação,
SEM mudar endpoints/DTOs/paths e SEM implementar hardening nível fintech.

## Escopo IN (obrigatório)
### A) Webhook Mercado Pago (P0)
1) Endpoint existente deve aceitar GET e POST:
- Path: `/v1/payments/mercadopago/notification`
- Manter o mesmo handler/controller (sem endpoint novo)

2) Security deve permitir GET e POST como `permitAll` para esse path:
- A segurança real fica na validação do token no backend

3) Token obrigatório no backend:
- Validar query param `token` contra `app.mercadopago.webhookToken`
- Token inválido -> retornar resposta compatível:
  - `decision=IGNORE`
  - `reason=invalid_token`
- NÃO logar token em claro (sanitizar)

4) Não quebrar quando `external_reference` / `orderId` vier ausente:
- Se MP não trouxer `external_reference`/`orderId`, NÃO tentar persistir `payments` com `order_id=null`
- Resposta compatível:
  - `decision=IGNORE`
  - `reason=no_external_reference`

### B) Checkout (P0/P1 mínimo)
1) Remover chamada do Mercado Pago de dentro de TX:
- Hoje o `@Transactional` cobre chamada externa (WebClient.block)
- Refatorar para:
  - TX curta para criar order e commit
  - chamada MP fora da TX
  - se MP falhar: aplicar compensação simples e explícita (ex.: marcar order como CANCELED ou manter CREATED com reason/log)
- NÃO mudar response DTO: `CheckoutCreateResponse`

### C) Testes P0 (mínimo aceitável)
1) Contract tests (MockMvc) para:
- `POST /v1/payments/checkout` (status/body)
- `GET|POST /v1/payments/mercadopago/notification` com token inválido e com token válido

2) Teste de replay básico do webhook:
- mesmo `paymentId` processado 2x não deve duplicar efeito (idempotência atual)
- (se Testcontainers for pesado, usar o mínimo possível; mas precisa pelo menos validar “não quebra”)

## Escopo OUT (explicitamente fora)
- Dedup por `eventId`
- Validação de assinatura oficial do MP
- Observabilidade completa (correlationId/runId)
- Refatoração Hex completa do projeto
- Mudanças em provisioning/pairing (ficam para fase posterior)

## Regras duras
- Não quebrar contratos existentes (paths/DTOs/semântica principal)
- Não vazar segredos em logs
- Manter PRs pequenos, rollback com git revert