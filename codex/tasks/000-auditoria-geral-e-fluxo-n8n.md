# TASK 000 — Auditoria Master (Backend + n8n) com PRD executável

## Objetivo (resultado esperado)
Gerar um PRD que eu consiga executar por fases (MVP -> hardening -> observabilidade/segurança),
com foco em backend Java/Spring (padrões de bancos/fintechs) e integração n8n.

## O que auditar no backend (sem inventar)
1) **Mapa do sistema**: módulos/pacotes, responsabilidades, fluxos (checkout -> webhook -> pairing).
2) **Contratos HTTP**: endpoints, DTOs, validações, status codes, compatibilidade.
3) **Consistência/Idempotência**: webhook at-least-once, dedup por paymentId/eventId, transições de status.
4) **Resiliência**: timeouts, retries/backoff, tratamento de falhas de rede (MP/Evolution).
5) **Segurança**: ApiKey, proteção de rotas internas, validação webhook, logs sem segredos.
6) **Observabilidade**: correlationId/runId, logs estruturados, métricas mínimas, taxonomy de erro.
7) **Testabilidade**: unit vs integration; o que mockar; onde Testcontainers faz sentido.

## O que projetar do n8n (fluxo final)
- Descrever o workflow step-by-step: nodes, decisões, retries, error path/DLQ.
- Definir payload mínimo entre nodes (correlationId/runId, orderId, paymentId, planId, token).
- Garantir reexecução segura (sem duplicar efeitos).
- Segurança no n8n: credentials e segredos, nunca hardcoded.

## Entregáveis obrigatórios no PRD
- “AS-IS vs TO-BE” (e o quanto o projeto está perto/longe de hex/clean)
- Lista de mudanças por arquivo/pacote (com risco e rollback)
- Plano de testes por fase
- Roadmap em 3 fases (o que entra em cada uma)
- Arquitetura utilizada e qual se encaixa melhor pra esse fluxo