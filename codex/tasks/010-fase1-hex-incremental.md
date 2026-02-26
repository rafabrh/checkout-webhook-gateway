# TASK 010 — FASE 1 (MVP estável + refatoração incremental rumo a Hex/Clean)

Você é um Principal/Staff Backend Engineer Java (padrão banco/fintech).
Gere um PRD **extremamente técnico** para a FASE 1, focado em:
- estabilizar o caminho crítico (checkout -> webhook -> provision -> pairing)
- reduzir risco sem quebrar contratos
- iniciar refatoração incremental para Ports & Adapters (hex) SEM big-bang

## Fontes de verdade (obrigatório)
- codex/CONTEXT.md
- codex/tasks/*.md
- repo: src/main/java/** + configs + migrations + testes

## Restrições duras
- NÃO inventar: se for novo, marcar como **NOVO** e justificar.
- NÃO quebrar endpoints/DTOs existentes (compat primeiro).
- Refatoração incremental: “strangler fig”, passos pequenos, revertíveis.
- Webhook é at-least-once: idempotência e reexecução segura são obrigatórias.
- Não vazar segredos em logs.
- Gerar plano por arquivo/pacote com risco e rollback.

## Saída obrigatória (Markdown, acionável)
1) Objetivo da Fase 1 (em 10 linhas): o que entra e o que NÃO entra.
2) Contratos e compatibilidade:
   - lista de endpoints afetados e garantias de compat
   - quais status codes ficam e quais mudam (se mudar, deve ser compatível)
3) Refatoração incremental para Hex (FASE 1):
   - mapear “core” (use cases) e “infra” (adapters) EXISTENTES hoje
   - definir os 3 primeiros Use Cases (nomes, inputs/outputs, invariantes)
   - definir Ports mínimas (interfaces) SEM reescrever tudo
   - explicar como controllers atuais viram apenas inbound adapters
4) Idempotência/consistência (FASE 1):
   - garantir que provisioning não duplica efeito por orderId/paymentId
   - dedup mínimo possível agora (sem eventId ainda, isso é Fase 2)
   - descrever transições de estado tolerando replay/out-of-order
5) Resiliência mínima (FASE 1):
   - timeouts explícitos WebClient (connect/read/response)
   - retry/backoff somente em falhas transitórias (definir critérios)
6) Segurança mínima (FASE 1):
   - política de acesso do webhook (GET/POST): decisão explícita e coerente com SecurityConfig
   - sanitização de logs (token/query/headers sensíveis)
7) Plano de mudanças por arquivo/pacote:
   - para cada arquivo: mudança, por quê, risco, compatibilidade, rollback
8) Plano de testes (FASE 1):
   - unit tests (lista e cenário)
   - integration tests (com Postgres/Testcontainers quando fizer sentido)
   - manual E2E (passo-a-passo com curl)
9) Checklist de aceitação:
   - “passa/falha” objetivo, com critério objetivo