# TASK 001 — Fase 1 (MVP): Checkout TX curta + Webhook GET/POST + Testes P0

## Objetivo
Aplicar correções mínimas e seguras para estabilizar o caminho crítico **checkout → pagamento → webhook → decisão**, mantendo contratos e sem refactor suicida.

## Escopo (IN) — Fase 1
### A) Webhook Mercado Pago
1) **Aceitar GET e POST** no endpoint existente:
- Path: `/v1/payments/mercadopago/notification`
- Manter o handler atual (sem endpoint novo).
- **Security** deve permitir GET e POST como `permitAll` para esse path, e a segurança real fica no **token validation** (no backend).

2) **Token obrigatório no backend**
- Validar `token` (query param) contra `app.mercadopago.webhookToken` (ou config equivalente já existente).
- Se inválido: retornar resposta compatível com o contrato atual, com `decision=IGNORE` e `reason=invalid_token`.
- **Não logar token em claro**.

3) **Não quebrar por external_reference ausente**
- Quando MP não trouxer `external_reference`/`orderId`:
  - **NÃO tentar inserir em `payments` com `order_id=null`** (isso hoje pode explodir NOT NULL).
  - Responder `decision=IGNORE`, `reason=no_external_reference` (ou reason equivalente), e não gerar side effects no banco.

4) **Idempotência atual deve continuar**
- Preservar unicidade `payments.payment_id`.
- Preservar lock pessimista (`findByPaymentIdForUpdate`) e comportamento de replay sem duplicar efeitos.

### B) Checkout (Create Checkout)
1) **Remover chamada externa do Mercado Pago de dentro da transação**
- Hoje existe risco: `@Transactional` cobrindo chamada WebClient/block para MP.
- Refatorar para:
  - TX curta para criar `Order` (status `CREATED`) e commit.
  - Chamada ao MP fora da TX.
  - Após sucesso do MP: atualizar dados necessários no banco (se houver update).
  - Se MP falhar: registrar estado consistente:
    - **Opção preferida**: marcar `Order.status=CANCELED` (ou estado equivalente existente) em uma TX curta.
    - Não vazar tokens/segredos em logs/exceptions.

2) **Contrato HTTP não muda**
- Manter path `/v1/payments/checkout`
- Manter DTOs: `CheckoutCreateRequest` e `CheckoutCreateResponse`
- Manter semântica do retorno no caminho feliz (checkoutUrl/messageText).

### C) Testes P0 (mínimo obrigatório)
1) Contract tests (MockMvc):
- `/v1/payments/checkout` com payload válido → 200 e JSON com `orderId` e `checkoutUrl`
- `/v1/payments/mercadopago/notification` token inválido → 200 com `decision=IGNORE` e `reason=invalid_token`

2) Replay/idempotência (teste de integração ou service test com banco real):
- Duas execuções do webhook para o mesmo `paymentId` não podem criar duas linhas em `payments`.
- Se não for viável Testcontainers agora, pelo menos um teste cobrindo o caminho que antes tentava `orderId=null` (garantir que não escreve no banco).

## Escopo (OUT) — Não fazer agora
- Dedup por `eventId` / tabela `payment_events`
- Clean/Hex completo no projeto inteiro
- Observabilidade (correlationId/runId) avançada
- Retries/backoff e 503 retry-friendly (fica para fase 2 hardening)
- Refatorar provisioning/pairing

## Arquivos prováveis a mudar (baseado no repo atual)
- `src/main/java/com/shkgroups/security/SecurityConfig.java`
- `src/main/java/com/shkgroups/payments/api/PaymentsController.java`
- `src/main/java/com/shkgroups/payments/CheckoutService.java`
- `src/main/java/com/shkgroups/payments/MercadoPagoWebhookService.java`
- `src/main/java/com/shkgroups/payments/MercadoPagoPaymentTxService.java`
- testes em `src/test/java/...` (criar se não existir)

## Regras duras
- Não quebrar contratos existentes (paths/DTOs)
- Não expor segredos em log (token, accessToken, notificationUrl com token)
- Mudanças pequenas, PR-friendly, com rollback simples (`git revert`)

## Critérios de aceite
- `mvn test` verde
- Checkout: chamada ao MP não ocorre mais dentro da TX (TX curta)
- Webhook: GET e POST passam pela mesma lógica, token validado, e **orderId nulo não quebra DB**
- Replay: não duplica `payments` para mesmo `paymentId`
MD