#!/usr/bin/env python3
"""Verify every compiled game JAR against the exact framework used to build it."""

from __future__ import annotations

import json
import subprocess
import sys
import time
from datetime import datetime, timezone
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "server"
COMPILE_REPORT = ROOT / "docs" / "Server_game_split-后端编译台账.json"
RUNTIME_REPORT = ROOT / "docs" / "Server_game_split-后端运行台账.json"
HELPER_DIR = ROOT / "work" / "runtime" / "backend-runtime" / "helper"
FRAMEWORKS = {
    "legacy": SERVER / "gameServer" / "build" / "gameServer.jar",
    "modern": SERVER / "framework-modern" / "build" / "framework-modern.jar",
    "taiwan": SERVER / "framework-taiwan" / "build" / "framework-taiwan.jar",
}

HELPER_SOURCE = r'''
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public final class BackendJarLinkSmoke {
    public static void main(String[] args) throws Exception {
        File gameJar = new File(args[0]);
        List<String> classes = new ArrayList<>();
        try (JarFile jar = new JarFile(gameJar)) {
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                String name = entries.nextElement().getName();
                if (name.endsWith(".class") && !name.equals("module-info.class")) {
                    classes.add(name.substring(0, name.length() - 6).replace('/', '.'));
                }
            }
        }
        Collections.sort(classes);
        int loaded = 0;
        try (URLClassLoader loader = new URLClassLoader(
                new URL[] {gameJar.toURI().toURL()}, BackendJarLinkSmoke.class.getClassLoader())) {
            for (String className : classes) {
                try {
                    Class.forName(className, false, loader);
                    loaded++;
                } catch (Throwable error) {
                    System.err.println(className + " :: " + error.getClass().getName() + " :: " + error.getMessage());
                }
            }
        }
        System.out.println("classes=" + classes.size() + " loaded=" + loaded);
        if (loaded != classes.size()) System.exit(2);
    }
}
'''


def dependency_jars() -> list[Path]:
    return sorted(
        path
        for path in (SERVER / "common" / "lib").rglob("*.jar")
        if not path.name.endswith(("-sources.jar", "-javadoc.jar"))
    )


def prepare_helper() -> None:
    HELPER_DIR.mkdir(parents=True, exist_ok=True)
    source = HELPER_DIR / "BackendJarLinkSmoke.java"
    source.write_text(HELPER_SOURCE, encoding="utf-8")
    result = subprocess.run(
        ["javac", "-encoding", "UTF-8", "-d", str(HELPER_DIR), str(source)],
        text=True,
        capture_output=True,
    )
    if result.returncode:
        raise RuntimeError(result.stderr or result.stdout)


def main() -> int:
    prepare_helper()
    requested = set(sys.argv[1:])
    compile_report = json.loads(COMPILE_REPORT.read_text(encoding="utf-8"))
    dependencies = dependency_jars()
    previous = {}
    if requested and RUNTIME_REPORT.exists():
        previous = {
            item["module"]: item
            for item in json.loads(RUNTIME_REPORT.read_text(encoding="utf-8")).get("modules", [])
        }
    results = []
    for module in compile_report["modules"]:
        name = module["module"]
        if requested and name not in requested:
            if name in previous:
                results.append(previous[name])
            continue
        framework = module.get("framework", "legacy")
        game_jar = Path(module.get("jar") or SERVER / name / "build" / f"{name}.jar")
        framework_jar = FRAMEWORKS[framework]
        started = time.monotonic()
        if not game_jar.is_file() or not framework_jar.is_file():
            result = {
                "module": name,
                "framework": framework,
                "status": "failed",
                "durationSeconds": 0,
                "error": f"missing jar: {game_jar if not game_jar.is_file() else framework_jar}",
            }
        else:
            classpath = ":".join(str(path) for path in [HELPER_DIR, framework_jar, *dependencies])
            try:
                process = subprocess.run(
                    ["java", "-classpath", classpath, "BackendJarLinkSmoke", str(game_jar)],
                    text=True,
                    capture_output=True,
                    timeout=20,
                )
                status = "passed" if process.returncode == 0 else "failed"
                result = {
                    "module": name,
                    "framework": framework,
                    "status": status,
                    "durationSeconds": round(time.monotonic() - started, 3),
                    "result": process.stdout.strip(),
                    "error": process.stderr.strip()[-8000:],
                }
            except subprocess.TimeoutExpired as error:
                result = {
                    "module": name,
                    "framework": framework,
                    "status": "failed",
                    "durationSeconds": round(time.monotonic() - started, 3),
                    "error": f"timeout: {error}",
                }
        results.append(result)
        print(f"{name}: {result['status']}")
    results.sort(key=lambda item: item["module"])

    report = {
        "generatedAt": datetime.now(timezone.utc).isoformat(),
        "scope": "all compiled classes loaded without initialization in an isolated game JVM",
        "modules": results,
        "summary": {
            "total": len(results),
            "passed": sum(item["status"] == "passed" for item in results),
            "failed": sum(item["status"] != "passed" for item in results),
        },
    }
    RUNTIME_REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    return 0 if report["summary"]["failed"] == 0 else 1


if __name__ == "__main__":
    raise SystemExit(main())
