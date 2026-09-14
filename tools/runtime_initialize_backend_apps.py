#!/usr/bin/env python3
"""Initialize every game APP class in an isolated JVM without invoking main()."""

from __future__ import annotations

import json
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "server"
PREFLIGHT = ROOT / "docs" / "Server_game_split-后端启动预检台账.json"
OUTPUT = ROOT / "docs" / "Server_game_split-后端APP初始化台账.json"
HELPER = ROOT / "logs" / "backend-runtime" / "app-init-helper"
FRAMEWORKS = {
    "legacy": SERVER / "gameServer" / "build" / "gameServer.jar",
    "modern": SERVER / "framework-modern" / "build" / "framework-modern.jar",
    "taiwan": SERVER / "framework-taiwan" / "build" / "framework-taiwan.jar",
}
SOURCE = r'''
public final class BackendAppInitSmoke {
    public static void main(String[] args) throws Exception {
        Class<?> app = Class.forName(args[0], true, Thread.currentThread().getContextClassLoader());
        System.out.println("initialized=" + app.getName());
    }
}
'''


def dependencies() -> list[Path]:
    return sorted(
        path for path in (SERVER / "common" / "lib").rglob("*.jar")
        if not path.name.endswith(("-sources.jar", "-javadoc.jar"))
    )


def prepare() -> None:
    HELPER.mkdir(parents=True, exist_ok=True)
    source = HELPER / "BackendAppInitSmoke.java"
    source.write_text(SOURCE, encoding="utf-8")
    result = subprocess.run(["javac", "-d", str(HELPER), str(source)], text=True, capture_output=True)
    if result.returncode:
        raise RuntimeError(result.stderr or result.stdout)


def main() -> int:
    prepare()
    requested = set(sys.argv[1:])
    preflight = json.loads(PREFLIGHT.read_text(encoding="utf-8"))
    libs = dependencies()
    previous = {}
    if requested and OUTPUT.exists():
        previous = {
            item["module"]: item
            for item in json.loads(OUTPUT.read_text(encoding="utf-8")).get("modules", [])
        }
    modules = []
    for item in preflight["modules"]:
        if requested and item["module"] not in requested:
            if item["module"] in previous:
                modules.append(previous[item["module"]])
            continue
        started = time.monotonic()
        framework = item["framework"]
        app = item["entryPoints"][0] if len(item.get("entryPoints", [])) == 1 else ""
        jar = Path(item["jar"])
        error = ""
        stdout = ""
        if not app:
            error = "missing unique APP entry point"
        else:
            classpath = ":".join(str(path) for path in [HELPER, FRAMEWORKS[framework], *libs, jar])
            try:
                process = subprocess.run(
                    ["java", "-classpath", classpath, "BackendAppInitSmoke", app],
                    text=True,
                    capture_output=True,
                    timeout=15,
                )
                stdout = process.stdout.strip()
                if process.returncode:
                    error = process.stderr.strip()[-8000:] or f"exit code {process.returncode}"
            except subprocess.TimeoutExpired as exception:
                error = f"timeout: {exception}"
        result = {
            "module": item["module"],
            "framework": framework,
            "app": app,
            "status": "passed" if not error else "failed",
            "durationSeconds": round(time.monotonic() - started, 3),
            "result": stdout,
            "error": error,
        }
        modules.append(result)
        print(f"{item['module']}: {result['status']}")
    modules.sort(key=lambda item: item["module"])
    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "scope": "APP class initialization in an isolated JVM; main() is not invoked",
        "modules": modules,
        "summary": {
            "total": len(modules),
            "passed": sum(item["status"] == "passed" for item in modules),
            "failed": sum(item["status"] != "passed" for item in modules),
        },
    }
    OUTPUT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0 if report["summary"]["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
