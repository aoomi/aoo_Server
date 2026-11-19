#!/usr/bin/env python3
"""Read-only evidence collector for task 89.

It deliberately reports source facts instead of assigning a pass status: a route,
button, table, or test hit is only one vertex in an end-to-end reachability graph.
"""
from __future__ import annotations

import argparse
import json
import os
import re
from pathlib import Path


TEXT_SUFFIXES = {".java", ".ts", ".tsx", ".vue", ".json", ".prefab", ".scene", ".sql", ".md"}
ROUTE = re.compile(r'(?:createContext\(|new URL\(|(?:get|post|put|delete|mutate|request)\()[^\n]{0,80}?["\'](/(?:api/)?v\d+/[^"\'?# ]+)')
TABLE = re.compile(r'(?i)\b(?:from|join|update|into|table)\s+`?([a-z][a-z0-9_]{2,})`?')
BUTTON = re.compile(r'(?i)\b(?:btn[_A-Za-z0-9/-]*|onClick|@click|clickEvents)\b')
SENDPACK = re.compile(r'\bSendPack\s*\(')


def files(root: Path):
    ignored = {"target", "node_modules", "build", "library", "temp", "backup", ".git"}
    for top in ("Client", "Server", "Admin"):
        base = root / top
        if not base.exists():
            continue
        for current, directories, names in os.walk(base):
            directories[:] = [name for name in directories if name.lower() not in ignored]
            for name in names:
                path = Path(current) / name
                if path.suffix.lower() not in TEXT_SUFFIXES:
                    continue
                try:
                    if path.stat().st_size > 2_000_000:
                        continue
                except OSError:
                    continue
                yield path


def scan(root: Path) -> dict[str, object]:
    result: dict[str, object] = {"root": str(root), "files": 0, "routes": [], "tables": [], "ui_event_files": [], "sendPack_files": []}
    routes, tables, ui_files, send_files = set(), set(), set(), set()
    for path in files(root):
        result["files"] = int(result["files"]) + 1
        try:
            text = path.read_text(encoding="utf-8", errors="ignore")
        except OSError:
            continue
        rel = str(path.relative_to(root))
        for match in ROUTE.finditer(text):
            routes.add((match.group(1), rel))
        for match in TABLE.finditer(text):
            tables.add((match.group(1).lower(), rel))
        if BUTTON.search(text):
            ui_files.add(rel)
        if SENDPACK.search(text):
            send_files.add(rel)
    result.update(routes=sorted(routes), tables=sorted(tables), ui_event_files=sorted(ui_files), sendPack_files=sorted(send_files))
    return result


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("root", type=Path)
    parser.add_argument("--legacy", type=Path)
    args = parser.parse_args()
    payload = {"current": scan(args.root.resolve())}
    if args.legacy:
        payload["legacy"] = scan(args.legacy.resolve())
    print(json.dumps(payload, ensure_ascii=False, indent=2))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
