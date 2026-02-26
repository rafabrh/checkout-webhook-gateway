#!/usr/bin/env bash
set -euo pipefail

MODEL="${MODEL:-gpt-5.3-codex}"
REASONING="${REASONING:-medium}"            # low|medium|high|xhigh (se suportado)
PLAN_APPROVAL="${PLAN_APPROVAL:-never}"
EXEC_APPROVAL="${EXEC_APPROVAL:-untrusted}"

ROOT_DIR="${ROOT_DIR:-$(git rev-parse --show-toplevel 2>/dev/null || pwd)}"
TASK_DIR="${TASK_DIR:-$ROOT_DIR/codex/tasks}"
PRD_DIR="${PRD_DIR:-$ROOT_DIR/codex/prd}"
GATE_FILE="${GATE_FILE:-$ROOT_DIR/codex/GATE.md}"
CONTEXT_FILE="${CONTEXT_FILE:-$ROOT_DIR/codex/CONTEXT.md}"
PLAN_PROMPT_FILE="${PLAN_PROMPT_FILE:-$ROOT_DIR/codex/PLAN_PROMPT.md}"
EXEC_PROMPT_FILE="${EXEC_PROMPT_FILE:-$ROOT_DIR/codex/EXECUTE_PROMPT.md}"

mkdir -p "$PRD_DIR"

die() { echo "⛔ $*" >&2; exit 1; }
latest_prd() { ls -1t "$PRD_DIR"/PRD_*.md 2>/dev/null | head -n 1 || true; }

CODEX_HELP=""
load_codex_help() { [[ -n "$CODEX_HELP" ]] || CODEX_HELP="$(codex exec --help 2>/dev/null || true)"; }
supports() { load_codex_help; grep -q -- "$1" <<<"$CODEX_HELP"; }

require_plan_files() {
  [[ -f "$CONTEXT_FILE" ]] || die "Faltando $CONTEXT_FILE"
  [[ -f "$GATE_FILE" ]] || die "Faltando $GATE_FILE"
  [[ -f "$PLAN_PROMPT_FILE" ]] || die "Faltando $PLAN_PROMPT_FILE"
  [[ -d "$TASK_DIR" ]] || die "Faltando diretório $TASK_DIR"
  compgen -G "$TASK_DIR/*.md" >/dev/null || die "Nenhuma task encontrada em $TASK_DIR/*.md"
}
require_exec_files() {
  require_plan_files
  [[ -f "$EXEC_PROMPT_FILE" ]] || die "Faltando $EXEC_PROMPT_FILE"
}

gate_status() { grep -E '^STATUS:\s*' "$GATE_FILE" | head -n 1 | sed 's/STATUS:\s*//'; }
set_gate() {
  local value="$1"
  if grep -qE '^STATUS:\s*' "$GATE_FILE"; then
    perl -0777 -pe "s/^STATUS:\s*\w+/STATUS: $value/m" -i "$GATE_FILE"
  else
    printf "STATUS: %s\n" "$value" | cat - "$GATE_FILE" > "$GATE_FILE.tmp" && mv "$GATE_FILE.tmp" "$GATE_FILE"
  fi
}

codex_exec() {
  local sandbox="$1"
  local approval="$2"
  local input_file="$3"

  local -a args
  args+=(exec --model "$MODEL")

  if supports "--sandbox"; then args+=(--sandbox "$sandbox"); fi
  if supports "--ask-for-approval"; then args+=(--ask-for-approval "$approval"); fi
  if supports "--config"; then args+=(--config "model_reasoning_effort=\"$REASONING\""); fi
  if supports "--ephemeral"; then args+=(--ephemeral); fi

  args+=(-)
  codex "${args[@]}" < "$input_file"
}

generate_prd() {
  local out="$PRD_DIR/PRD_$(date +%Y%m%d_%H%M%S).md"
  local tmp="$out.tmp"
  echo "🧠 Gerando PRD (read-only) -> $out"

  # escreve em tmp pra não gerar PRD vazio
  if ! codex_exec "read-only" "$PLAN_APPROVAL" "$PLAN_PROMPT_FILE" | tee "$tmp" ; then
    rm -f "$tmp"
    die "Falha no codex exec (provável stream disconnected / auth / rede)."
  fi

  if [[ ! -s "$tmp" ]]; then
    rm -f "$tmp"
    die "PRD veio vazio. Isso é quase sempre stream disconnected / backend instável. Tente: codex logout/login e rode novamente."
  fi

  mv "$tmp" "$out"
  echo ""
  echo "✅ PRD gerado: $out"
  echo "➡️ Revise e rode: ./codex/ralph-loop.sh approve"
}

render_exec_prompt_to_tmp() {
  local prd_path="$1"
  local tmp="$PRD_DIR/.execute_prompt_tmp.md"
  sed "s|{{PRD_PATH}}|$prd_path|g" "$EXEC_PROMPT_FILE" > "$tmp"
  echo "$tmp"
}

execute_prd() {
  local prd_path="$1"
  [[ -f "$prd_path" ]] || die "PRD não encontrado: $prd_path"
  echo "🛠️ Executando PRD (workspace-write) -> $prd_path"
  local tmp; tmp="$(render_exec_prompt_to_tmp "$prd_path")"
  codex_exec "workspace-write" "$EXEC_APPROVAL" "$tmp"
}

usage() {
  cat <<EOF
Uso:
  ./codex/ralph-loop.sh plan
  ./codex/ralph-loop.sh approve
  ./codex/ralph-loop.sh exec [PRD]
  ./codex/ralph-loop.sh loop

Env vars:
  MODEL=$MODEL
  REASONING=$REASONING
  PLAN_APPROVAL=$PLAN_APPROVAL
  EXEC_APPROVAL=$EXEC_APPROVAL
EOF
}

cmd_plan() { require_plan_files; set_gate "DRAFT"; generate_prd; }
cmd_approve() { require_plan_files; set_gate "APPROVED"; echo "✅ Gate aprovado."; }
cmd_exec() {
  require_exec_files
  local status; status="$(gate_status || true)"
  [[ "$status" == "APPROVED" ]] || die "Gate não aprovado (status=$status). Rode: ./codex/ralph-loop.sh approve"

  local prd_path="${1:-}"
  [[ -n "$prd_path" ]] || prd_path="$(latest_prd)"
  [[ -n "$prd_path" ]] || die "Nenhum PRD encontrado em $PRD_DIR"

  execute_prd "$prd_path"
}
cmd_loop() {
  require_plan_files
  cmd_plan
  local prd_path; prd_path="$(latest_prd)"
  [[ -n "$prd_path" ]] || die "Falha ao localizar PRD gerado"
  echo ""
  echo "⏸️ Loop pausado aguardando aprovação humana."
  echo "   1) Revise: $prd_path"
  echo "   2) Aprovando: ./codex/ralph-loop.sh approve"
  echo "   3) Executando: ./codex/ralph-loop.sh exec $prd_path"
}

main() {
  cd "$ROOT_DIR"
  local cmd="${1:-}"; shift || true
  case "$cmd" in
    plan) cmd_plan ;;
    approve) cmd_approve ;;
    exec) cmd_exec "${1:-}" ;;
    loop) cmd_loop ;;
    ""|-h|--help|help) usage ;;
    *) die "Comando desconhecido: $cmd" ;;
  esac
}
main "$@"
