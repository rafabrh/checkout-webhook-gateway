codex exec --model gpt-5.3-codex --sandbox read-only --ask-for-approval never - <<'PROMPT'
Você é um Staff/Principal Backend Engineer Java (padrão banco/fintech: Itaú/Nubank/Mercado Livre/Santander).
Seu objetivo é produzir um PRD extremamente técnico para a **FASE 1** de uma refatoração incremental rumo a Clean/Hexagonal Architecture (Ports & Adapters),
SEM quebrar contratos existentes, SEM big-bang refactor, com passos reversíveis e testáveis.

FONTES DE VERDADE:
- codex/CONTEXT.md
- codex/tasks/*.md (incluindo a task de refatoração hex incremental)
- Repo atual (src/main/java/**, migrations Flyway, configs, testes)

RESTRIÇÕES DURAS:
- Não alterar nenhum arquivo (somente planejar).
- Não inventar endpoints ou mudar paths/DTOs agora. Qualquer mudança de contrato deve ser "FUTURA" e fora do escopo da Fase 1.
- Tudo novo deve ser marcado como **NOVO** e vir com justificativa + impacto.
- A Fase 1 deve ser pequena e mergeável (padrão PR em bigtech).

OBJETIVO DA FASE 1 (escopo fechado):
1) Introduzir camada de Application (Use Cases) para 2 fluxos críticos:
   - CreateCheckout (POST /v1/payments/checkout)
   - ProcessMercadoPagoWebhook (*/v1/payments/mercadopago/notification)
2) Introduzir Ports (interfaces) MÍNIMAS somente quando necessário, sem reescrever o mundo:
   - PaymentGatewayPort (para MercadoPagoClient)
   - PaymentStorePort/OrderStorePort (pode começar como wrapper dos repos JPA existentes)
3) Controllers viram thin: validar request, chamar Use Case, devolver DTO.
4) Garantir zero breaking changes e documentação do “AS-IS vs TO-BE” da fase.

ENTREGÁVEIS (SAÍDA EM MARKDOWN):
## 1) Executive Summary (10-15 linhas)
- Por que a Fase 1 existe
- O que muda e o que NÃO muda
- Critérios de sucesso (build/test + contratos + rollback)

## 2) AS-IS — mapa de dependências REAL
- Para cada fluxo (checkout/webhook), mostre a cadeia atual:
  Controller -> Service -> Repo/WebClient
- Liste pontos de acoplamento e riscos
- Liste transações atuais e side effects

## 3) TO-BE Fase 1 — arquitetura alvo incremental (somente fase 1)
- Diagrama textual (ex.: Controller -> UseCase -> Ports -> Adapters)
- Onde ficam os pacotes novos:
   - com.shkgroups.application (usecases)
   - com.shkgroups.ports (interfaces)
   - com.shkgroups.adapters (implementações: JPA/WebClient)
     (ou ajuste conforme repo existente, mas deixe claro)

## 4) Lista de mudanças por arquivo (cirúrgico)
Para cada item:
- Arquivo atual
- Mudança proposta (bullets)
- Motivação
- Risco
- Estratégia de rollback
- Teste associado

Obrigatório incluir:
- **NOVO** CreateCheckoutUseCase
- **NOVO** ProcessMpWebhookUseCase
- **NOVO** PaymentGatewayPort (+ adapter MercadoPagoClient)
- **NOVO** OrderStorePort/PaymentStorePort (ou justificar não criar nesta fase)

## 5) Estratégia de migração sem quebrar nada
- “Strangler Fig Pattern” aplicado: controllers continuam iguais, por baixo migram pra use cases
- Mantém DTOs e endpoints
- Como validar compatibilidade (contract tests / snapshots / Postman collections)

## 6) Plano de testes Fase 1 (padrão fintech)
- Unit tests (mock ports)
- Integration tests (se possível: Testcontainers + Postgres) para webhook replay e concorrência mínima
- Testes de regressão de contrato (mínimo: status codes + bodies)

## 7) Plano de PRs (mínimo 2 PRs)
PR1: criar estrutura + use cases + controllers thin
PR2: ports + adapters mínimos + testes essenciais
Cada PR: checklist de validação

## 8) Checklist final de execução
- comandos (mvn test)
- rotas validadas
- rollback plan claro

IMPORTANTE:
- Seja brutalmente específico: classes, assinaturas sugeridas, nomes de pacotes, e ordem de implementação.
- Mas não escreva código completo — apenas blueprint acionável.
  PROMPT