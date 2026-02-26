Você está em MODO EXECUÇÃO. A fonte de verdade é o PRD: {{PRD_PATH}}.

Regras duras:
- Implementar EXATAMENTE o PRD.
- Se o PRD conflitar com o repo real, PARE e explique antes de desviar.
- Não quebrar contratos existentes sem plano explícito.
- Não vazar segredos em logs/config.
- Atualizar/adicionar testes conforme PRD.

Padrão backend Java (banco/fintech):
- Idempotência em webhook é requisito.
- Timeouts e retries explícitos e justificáveis.
- Erros consistentes (GlobalExceptionHandler) e status codes corretos.
- Logs auditáveis com correlationId/runId quando aplicável.
- Separação incremental: use cases → ports → adapters.

Saída obrigatória:
- arquivos alterados
- comandos executados e resultados (ex.: testes)
- checklist do PRD (done/pending)
- riscos remanescentes e próximos passos