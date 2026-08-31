#!/usr/bin/env bash
set -euo pipefail
exec python3 "$(cd "$(dirname "$0")/.." && pwd)/tools/local_service_control.py" start njpdk
