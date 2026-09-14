#!/usr/bin/env python3
import argparse
import datetime as dt
import json
import os
import re
import signal
import socket
import subprocess
import time
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
PREFLIGHT = ROOT / "docs/Server_game_split-后端启动预检台账.json"
REPORT = ROOT / "docs/Server_game_split-后端真实启动台账.json"
LOG_DIR = ROOT / "logs/backend-runtime/real-start"
FRAMEWORKS = {
    "legacy": ROOT / "server/gameServer/build/gameServer.jar",
    "modern": ROOT / "server/framework-modern/build/framework-modern.jar",
    "taiwan": ROOT / "server/framework-taiwan/build/framework-taiwan.jar",
}


def port_open(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=0.2):
            return True
    except OSError:
        return False


def game_type(module):
    source_root = ROOT / "server" / module / "src"
    for path in source_root.rglob("*APP.java"):
        text = path.read_text(encoding="utf-8", errors="ignore")
        match = re.search(r"\bgameTypeId\s*=\s*(-?\d+)\s*;", text)
        if match:
            return int(match.group(1))
    return None


def common_jars():
    # Production startup resolves only Maven reactor artifacts. Quarantined
    # 2.22 jars under reference/legacy-2.22 are never placed on the classpath.
    return []


def write_report(results):
    payload = {
        "generatedAt": dt.datetime.now().astimezone().isoformat(),
        "scope": "698 backend modules: isolated real main() startup with WebSocket and HTTP port checks",
        "modules": results,
        "summary": {
            "total": len(results),
            "passed": sum(item["status"] == "passed" for item in results),
            "failed": sum(item["status"] == "failed" for item in results),
            "skipped": sum(item["status"] == "skipped" for item in results),
        },
    }
    temp = REPORT.with_suffix(".json.tmp")
    temp.write_text(json.dumps(payload, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    temp.replace(REPORT)


def error_summary(log_path):
    text = log_path.read_text(encoding="utf-8", errors="ignore")
    lines = [
        line.strip() for line in text.splitlines()
        if re.search(r"exception|error|failed|失败|错误", line, re.I)
    ]
    return " | ".join(lines[-3:])[:1200] or "startup exited or timed out before both ports opened"


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--limit", type=int)
    parser.add_argument("--timeout", type=int, default=30)
    parser.add_argument("--resume", action="store_true")
    parser.add_argument("--retry-failed", action="store_true")
    parser.add_argument("--modules", nargs="*")
    args = parser.parse_args()

    modules = json.loads(PREFLIGHT.read_text(encoding="utf-8"))["modules"]
    previous = {}
    requested = set(args.modules or [])
    if (args.resume or requested) and REPORT.exists():
        previous = {
            item["module"]: item
            for item in json.loads(REPORT.read_text(encoding="utf-8")).get("modules", [])
        }
    results = []
    completed_this_run = 0
    jars = common_jars()
    LOG_DIR.mkdir(parents=True, exist_ok=True)

    for spec in modules:
        module = spec["module"]
        if requested and module not in requested:
            if module in previous:
                results.append(previous[module])
            continue
        if not requested and module in previous and not (
            args.retry_failed and previous[module]["status"] == "failed"
        ):
            results.append(previous[module])
            continue
        if args.limit is not None and completed_this_run >= args.limit:
            break
        completed_this_run += 1
        started = time.monotonic()
        gt = game_type(module)
        app = spec["entryPoints"][0] if spec.get("entryPoints") else ""
        framework = spec["framework"]
        result = {
            "module": module,
            "framework": framework,
            "app": app,
            "gameTypeId": gt,
            "status": "failed",
            "durationSeconds": 0,
            "result": "",
            "error": "",
        }
        if gt is None or not app or framework not in FRAMEWORKS:
            result["status"] = "skipped"
            result["error"] = "missing gameTypeId, APP entry, or framework mapping"
            results.append(result)
            write_report(results)
            continue

        client_port, http_port, node_port = 19996, 19886, 19997
        prepare = subprocess.run([
            "python3", str(ROOT / "tools/prepare_backend_smoke_sandbox.py"),
            "--game", module, "--game-type", str(gt),
            "--client-port", str(client_port), "--http-port", str(http_port),
            "--node-port", str(node_port),
            "--environment", "test", "--apply",
        ], cwd=ROOT, capture_output=True, text=True)
        if prepare.returncode:
            result["error"] = ("sandbox prepare failed: " + prepare.stderr)[-1200:]
            results.append(result)
            write_report(results)
            continue

        module_jar = Path(spec["jar"])
        classpath = os.pathsep.join(map(str, [FRAMEWORKS[framework], module_jar] + jars))
        config = ROOT / f"work/local-runtime/conf-smoke-{module.lower()}"
        log_path = LOG_DIR / f"{module}.log"
        with log_path.open("w", encoding="utf-8") as log:
            process = subprocess.Popen([
                "java", "--add-opens=java.base/java.lang=ALL-UNNAMED",
                "--add-opens=java.base/java.nio=ALL-UNNAMED",
                "-Xms128m", "-Xmx768m", "-cp", classpath, app, str(config),
            ], cwd=ROOT, stdout=log, stderr=subprocess.STDOUT, start_new_session=True)
            deadline = time.monotonic() + args.timeout
            passed = False
            while time.monotonic() < deadline:
                if process.poll() is not None:
                    break
                network_ready = port_open(client_port)
                if framework != "modern":
                    network_ready = network_ready and port_open(http_port)
                if network_ready:
                    passed = True
                    break
                time.sleep(0.25)
            if process.poll() is None:
                os.killpg(process.pid, signal.SIGTERM)
                try:
                    process.wait(timeout=5)
                except subprocess.TimeoutExpired:
                    os.killpg(process.pid, signal.SIGKILL)
                    process.wait()

        result["durationSeconds"] = round(time.monotonic() - started, 3)
        if passed:
            result["status"] = "passed"
            result["result"] = (
                f"WebSocket={client_port}, HTTP={http_port}"
                if framework != "modern"
                else f"WebSocket={client_port}, HTTP=not implemented by modern source framework"
            )
        else:
            result["error"] = error_summary(log_path)
        results.append(result)
        write_report(results)
        print(f"{module}|{result['status']}|{result['durationSeconds']}s", flush=True)

    write_report(results)


if __name__ == "__main__":
    main()
