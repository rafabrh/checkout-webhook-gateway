# CONTEXT — com.shkgroups.agent (Java/Spring Backend)

## Objetivo do sistema
Backend responsável por:
- Criar checkout (Mercado Pago)
- Processar webhook de pagamento (eventos/replays)
- Provisionar e expor fluxo de pairing via Evolution API (QR/instância/sessão)
- Proteger rotas internas (ApiKey) e manter contratos estáveis para o n8n orquestrar

## Regras duras de engenharia
- Não inventar: sempre ler o repo e respeitar o que já existe.
- Compatibilidade: não quebrar endpoints/DTOs existentes sem plano explícito.
- Segurança: nunca vazar segredos em logs/config; validar webhook/headers quando aplicável.
- Confiabilidade: webhook é at-least-once → idempotência e reprocessamento seguro são obrigatórios.
- Observabilidade: tudo deve suportar correlationId/runId (mesmo que simples), logs úteis e erros consistentes.
- Resiliência: timeouts explícitos + retries com backoff para chamadas externas (MP/Evolution), sem loops infinitos.

## Padrões arquiteturais (alvo incremental)
- Modular monolith por contexto: Payments / Orders / Pairing/Provisioning / Security / Config.
- Evolução incremental para Clean/Hex:
  - Use cases (aplicação) chamam ports (interfaces)
  - Adapters implementam ports (WebClient, JPA)
  - Evitar controller chamando infra direto quando fizer sentido.

## Qualidade mínima
- DTOs + validação (Bean Validation) nos boundaries HTTP
- Status codes consistentes + GlobalExceptionHandler
- Testes: unit para regras, integration para fluxos críticos quando aplicável