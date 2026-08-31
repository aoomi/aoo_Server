#!/usr/bin/env python3
import json
import os
import shutil
import subprocess
import tempfile
import time
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
SERVER = ROOT / "server"
DOCS = ROOT / "docs"
LOGS = ROOT / "logs" / "backend-runtime"

HELPER = r'''
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.jar.*;

public class GameJarSmoke {
  public static void main(String[] args) throws Exception {
    File jar = new File(args[0]);
    URLClassLoader loader = new URLClassLoader(new URL[]{jar.toURI().toURL()}, ClassLoader.getSystemClassLoader());
    int loaded = 0;
    List<String> apps = new ArrayList<>();
    List<String> failures = new ArrayList<>();
    try (JarFile jf = new JarFile(jar)) {
      Enumeration<JarEntry> entries = jf.entries();
      while (entries.hasMoreElements()) {
        String n = entries.nextElement().getName();
        if (!n.endsWith(".class") || n.equals("module-info.class") || n.contains("META-INF/versions/")) continue;
        String cn = n.substring(0, n.length() - 6).replace('/', '.');
        try {
          Class<?> c = Class.forName(cn, false, loader);
          loaded++;
          if (cn.endsWith("APP")) {
            try { c.getMethod("main", String[].class); apps.add(cn); }
            catch (NoSuchMethodException ignored) {}
          }
        } catch (Throwable t) {
          failures.add(cn + " :: " + t.getClass().getName() + ": " + String.valueOf(t.getMessage()));
        }
      }
    } finally { loader.close(); }
    System.out.println("LOADED=" + loaded);
    System.out.println("APPS=" + String.join(",", apps));
    for (String f : failures) System.out.println("FAIL=" + f);
    if (!failures.isEmpty() || apps.isEmpty()) System.exit(2);
  }
}
'''

def classpath():
    jars = []
    for base in (SERVER / "gameServer" / "build", SERVER / "common" / "lib", SERVER / "gameServer" / "lib"):
        if base.exists(): jars.extend(str(p) for p in base.rglob("*.jar"))
    classes = [SERVER / "gameServer" / "build" / "classes", SERVER / "common" / "build" / "classes"]
    return os.pathsep.join([str(p) for p in classes if p.exists()] + sorted(set(jars)))

def main():
    LOGS.mkdir(parents=True, exist_ok=True)
    DOCS.mkdir(parents=True, exist_ok=True)
    game_jars = sorted(p for p in SERVER.glob("*/build/*.jar") if p.parent.parent.name not in {"gameServer", "common"})
    cp = classpath()
    results = []
    with tempfile.TemporaryDirectory(prefix="game-runtime-smoke-") as td:
        td = Path(td)
        (td / "GameJarSmoke.java").write_text(HELPER, encoding="utf-8")
        compile_run = subprocess.run(["javac", "-proc:none", "-cp", cp, str(td / "GameJarSmoke.java")], text=True, capture_output=True)
        if compile_run.returncode:
            raise SystemExit("smoke helper compile failed: " + compile_run.stderr)
        run_cp = os.pathsep.join([str(td), cp])
        for index, jar in enumerate(game_jars, 1):
            started = time.time()
            proc = subprocess.run(["java", "-Xms32m", "-Xmx256m", "-cp", run_cp, "GameJarSmoke", str(jar)], text=True, capture_output=True, timeout=60)
            output = (proc.stdout + proc.stderr).strip()
            (LOGS / (jar.stem + ".log")).write_text(output + "\n", encoding="utf-8")
            loaded = next((x.split("=", 1)[1] for x in output.splitlines() if x.startswith("LOADED=")), "0")
            apps = next((x.split("=", 1)[1] for x in output.splitlines() if x.startswith("APPS=")), "")
            failures = [x[5:] for x in output.splitlines() if x.startswith("FAIL=")]
            results.append({"game": jar.stem, "jar": str(jar), "status": "passed" if proc.returncode == 0 else "failed", "classCount": int(loaded), "entrypoints": [x for x in apps.split(",") if x], "failures": failures, "seconds": round(time.time() - started, 3)})
            print(f"[{index}/{len(game_jars)}] {jar.stem}: {results[-1]['status']}", flush=True)

    passed = sum(r["status"] == "passed" for r in results)
    report = {"checkedAt": time.strftime("%Y-%m-%d %H:%M:%S"), "total": len(results), "passed": passed, "failed": len(results)-passed, "scope": "isolated JVM class-load and executable APP entrypoint discovery", "fullStartup": "requires configured MySQL, Redis, RocketMQ and game registration data", "results": results}
    (DOCS / "Server_game_split-后端运行台账.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    lines = ["# Server_game_split 后端运行台账", "", f"- 检查时间：{report['checkedAt']}", f"- 游戏总数：{len(results)}", f"- 运行门禁通过：{passed}", f"- 失败：{len(results)-passed}", "- 门禁定义：每个游戏在独立 JVM 类加载器中加载全部 class，并确认存在可执行 APP.main 入口。", "- 完整联网启动：依赖 MySQL、Redis、RocketMQ 及数据库游戏注册数据，不等同于本地单 JAR 门禁。", "", "| 游戏 | 状态 | 类数量 | 启动入口 | 失败原因 |", "|---|---:|---:|---|---|"]
    for r in results:
        reason = "；".join(r["failures"][:2]).replace("|", "\\|")
        lines.append(f"| {r['game']} | {r['status']} | {r['classCount']} | {', '.join(r['entrypoints'])} | {reason} |")
    (DOCS / "Server_game_split-后端运行台账.md").write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(json.dumps({"total": len(results), "passed": passed, "failed": len(results)-passed}, ensure_ascii=False))
    return 0 if passed == len(results) else 1

if __name__ == "__main__":
    raise SystemExit(main())
