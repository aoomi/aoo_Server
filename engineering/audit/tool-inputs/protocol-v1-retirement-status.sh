#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")/../../.." && pwd)"
exec ruby "$ROOT/engineering/audit/tool-inputs/check_protocol_v1_retirement.rb"
