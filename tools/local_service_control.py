#!/usr/bin/env python3
"""Safe, repeatable local service lifecycle control."""

import argparse
import json
import os
import signal
import socket
import subprocess
import sys
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
CATALOG = json.loads((ROOT / "config/local-services.json").read_text())["services"]


def alive(pid):
    try:
        os.kill(pid, 0)
        return True
    except (OSError, ProcessLookupError):
        return False


def identity(pid, expected):
    result = subprocess.run(["ps", "-o", "command=", "-p", str(pid)], capture_output=True, text=True)
    return result.returncode == 0 and expected in result.stdout


def open_port(port):
    try:
        with socket.create_connection(("127.0.0.1", port), timeout=.2):
            return True
    except OSError:
        return False


def read_pid(path):
    try:
        value = path.read_text().strip()
        return int(value) if value.isdigit() else None
    except FileNotFoundError:
        return None


def classpath(module):
    script = f'source "{ROOT}/tools/runtime-java26.sh"; runtime_classpath "{module}"'
    result = subprocess.run(["bash", "-lc", script], cwd=ROOT, capture_output=True, text=True)
    if result.returncode:
        raise RuntimeError(result.stderr.strip() or "runtime classpath resolution failed")
    return result.stdout.strip()


def stop_process(pid, spec, timeout):
    if not alive(pid):
        return
    if not identity(pid, spec["mainClass"]):
        raise RuntimeError(f"PID {pid} does not belong to {spec['mainClass']}")
    os.kill(pid, signal.SIGTERM)
    deadline = time.monotonic() + timeout
    while time.monotonic() < deadline and alive(pid):
        time.sleep(.25)
    if alive(pid):
        raise RuntimeError(f"PID {pid} did not stop gracefully in {timeout}s; process retained")


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("action", choices=("start", "stop", "status"))
    parser.add_argument("service", choices=sorted(CATALOG))
    parser.add_argument("--timeout", type=int, default=90)
    args = parser.parse_args()
    spec = CATALOG[args.service]
    pid_path = ROOT / spec["pidFile"]
    pid = read_pid(pid_path)

    if args.action == "status":
        ok = bool(pid and alive(pid) and identity(pid, spec["mainClass"]))
        print(json.dumps({"service": args.service, "running": ok, "pid": pid if ok else None}))
        return 0 if ok else 3

    if args.action == "stop":
        if pid:
            stop_process(pid, spec, args.timeout)
        pid_path.unlink(missing_ok=True)
        print(f"{args.service} stopped")
        return 0

    if pid and alive(pid):
        if not identity(pid, spec["mainClass"]):
            raise RuntimeError(f"PID file belongs to another process: {pid}")
        print(f"{args.service} already running pid={pid}")
        return 0
    pid_path.unlink(missing_ok=True)
    occupied = [port for port in spec["ports"] if open_port(port)]
    if occupied:
        raise RuntimeError(f"required ports already occupied: {occupied}")
    config_dir = ROOT / spec["configDir"]
    if not config_dir.is_dir():
        raise RuntimeError(f"configuration directory missing: {config_dir}")

    lock = pid_path.with_suffix(".lock")
    try:
        lock.mkdir(parents=True)
    except FileExistsError:
        raise RuntimeError(f"concurrent lifecycle operation: {lock}")
    process = None
    try:
        java_home = os.environ.get("JAVA_HOME", str(ROOT.parent / ".toolchains/jdk-26.0.2.1.jdk/Contents/Home"))
        java = Path(java_home) / "bin/java"
        if not java.is_file():
            raise RuntimeError(f"Java runtime missing: {java}")
        log_path = ROOT / spec["logFile"]
        log_path.parent.mkdir(parents=True, exist_ok=True)
        with log_path.open("a", encoding="utf-8") as log:
            process = subprocess.Popen(
                [str(java), "-Xms512m", "-Xmx2048m", "-cp", classpath(spec["module"]),
                 spec["mainClass"], str(config_dir)], cwd=ROOT, stdout=log,
                stderr=subprocess.STDOUT, start_new_session=True,
            )
        temp = pid_path.with_suffix(".pid.tmp")
        temp.parent.mkdir(parents=True, exist_ok=True)
        temp.write_text(str(process.pid) + "\n")
        temp.replace(pid_path)
        deadline = time.monotonic() + args.timeout
        while time.monotonic() < deadline:
            if process.poll() is not None:
                raise RuntimeError(f"service exited with code {process.returncode}; see {log_path}")
            if all(open_port(port) for port in spec["ports"]):
                print(f"{args.service} started pid={process.pid} ports={spec['ports']}")
                return 0
            time.sleep(.25)
        raise RuntimeError(f"startup timeout; see {log_path}")
    except Exception:
        if process and process.poll() is None:
            os.killpg(process.pid, signal.SIGTERM)
            try:
                process.wait(timeout=10)
            except subprocess.TimeoutExpired:
                pass
        pid_path.unlink(missing_ok=True)
        raise
    finally:
        lock.rmdir()


if __name__ == "__main__":
    try:
        raise SystemExit(main())
    except RuntimeError as error:
        print(error, file=sys.stderr)
        raise SystemExit(2)
