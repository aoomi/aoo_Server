#!/usr/bin/env bash
set -euo pipefail
exec python3 "$(cd "$(dirname "$0")/../../.." && pwd)/engineering/local-dev/tool-inputs/local_service_control.py" stop njpdk
