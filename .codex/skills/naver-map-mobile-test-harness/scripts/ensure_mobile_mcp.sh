#!/bin/bash
set -euo pipefail

if ! command -v codex >/dev/null 2>&1; then
  echo "codex CLI is required but was not found on PATH." >&2
  exit 1
fi

if codex mcp get mobile-mcp >/dev/null 2>&1; then
  echo "mobile-mcp is already configured for Codex."
  exit 0
fi

if ! command -v npx >/dev/null 2>&1; then
  echo "npx is required to install mobile-mcp but was not found on PATH." >&2
  exit 1
fi

codex mcp add mobile-mcp --env MOBILEMCP_DISABLE_TELEMETRY=1 -- npx -y @mobilenext/mobile-mcp@latest
echo "mobile-mcp has been added to Codex."
