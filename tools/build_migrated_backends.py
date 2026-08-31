#!/usr/bin/env python3
"""Build the migrated Eclipse/Ant backends without requiring Ant."""

from __future__ import annotations

import json
import os
import shutil
import subprocess
import sys
import time
import zipfile
from pathlib import Path


ROOT = Path(os.environ.get('AOO_MIGRATION_SOURCE_ROOT', Path(__file__).resolve().parents[1]))
SERVER = ROOT / 'server'
MODERN = ROOT / 'backend-2024' / 'sources'
LOG_ROOT = ROOT / 'logs' / 'backend-build'
REPORT = ROOT / 'docs' / 'Server_game_split-后端编译台账.json'
PUBLIC = {'common', 'commdef', 'gameHall', 'gameServer', 'logs'}
TAIWAN = {'TWDDZ', 'TWDLE', 'TWDZPK', 'TWERDDZ', 'TWFQPLS', 'TWSZD', 'TWTWMJ'}


def java_files(*roots: Path) -> list[Path]:
    result: list[Path] = []
    for root in roots:
        if root.is_dir():
            result.extend(sorted(root.rglob('*.java')))
    return result


def classpath() -> str:
    archived = SERVER / 'common' / 'lib' / 'archived'
    jars = sorted(path for path in (SERVER / 'common' / 'lib').rglob('*.jar')
                  if archived not in path.parents
                  and not path.name.endswith(('-sources.jar', '-javadoc.jar')))
    return ':'.join(str(path) for path in jars)


def compile_sources(name: str, sources: list[Path], output: Path, cp: str) -> dict[str, object]:
    started = time.time()
    log = LOG_ROOT / f'{name}.log'
    output.mkdir(parents=True, exist_ok=True)
    args_file = LOG_ROOT / f'{name}.sources'
    args_file.write_text('\n'.join(f'"{path}"' for path in sources) + '\n', encoding='utf-8')
    command = [
        'javac', '-J-Xmx6g',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.processing=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.code=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.comp=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.file=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.jvm=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.main=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.model=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.parser=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.tree=ALL-UNNAMED',
        '-J--add-opens=jdk.compiler/com.sun.tools.javac.util=ALL-UNNAMED',
        '-encoding', 'UTF-8', '-source', '8', '-target', '8',
        '-cp', cp, '-d', str(output), f'@{args_file}',
    ]
    process = subprocess.run(command, text=True, stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    log.write_text(process.stdout, encoding='utf-8')
    return {
        'module': name,
        'status': 'passed' if process.returncode == 0 else 'failed',
        'sourceCount': len(sources),
        'exitCode': process.returncode,
        'durationSeconds': round(time.time() - started, 2),
        'log': str(log.relative_to(ROOT)),
    }


def make_jar(classes: Path, output: Path) -> None:
    output.parent.mkdir(parents=True, exist_ok=True)
    with zipfile.ZipFile(output, 'w', zipfile.ZIP_DEFLATED, allowZip64=True) as archive:
        for path in sorted(classes.rglob('*')):
            if path.is_file():
                archive.write(path, path.relative_to(classes))


def main() -> None:
    requested = set(sys.argv[1:])
    LOG_ROOT.mkdir(parents=True, exist_ok=True)
    core_build = SERVER / 'gameServer' / 'build'
    core_classes = core_build / 'classes'
    if core_classes.exists():
        shutil.rmtree(core_classes)
    core_sources = java_files(
        SERVER / 'common' / 'src', SERVER / 'commdef' / 'src', SERVER / 'gameServer' / 'src'
    )
    core = compile_sources('gameServer-core', core_sources, core_classes, classpath())
    modern_build = SERVER / 'framework-modern' / 'build'
    modern_classes = modern_build / 'classes'
    if modern_classes.exists():
        shutil.rmtree(modern_classes)
    modern_sources = java_files(MODERN / 'common' / 'src', MODERN / 'commdef' / 'src', MODERN / 'gameServer' / 'src')
    modern_cp = ':'.join(filter(None, [
        classpath(),
        str(MODERN / 'common' / 'lib' / 'apache-commons' / 'commons-compress-1.9.jar'),
        str(MODERN / 'common' / 'lib' / 'ip2region-2.6.4.jar'),
    ]))
    modern = compile_sources('framework-modern', modern_sources, modern_classes, modern_cp)
    modern_jar = modern_build / 'framework-modern.jar'
    if modern['status'] == 'passed':
        make_jar(modern_classes, modern_jar)

    modules: list[dict[str, object]] = []
    if core['status'] == 'passed':
        core_jar = core_build / 'gameServer.jar'
        make_jar(core_classes, core_jar)
        game_cp = f"{core_jar}:{classpath()}"
        for module in sorted(SERVER.iterdir(), key=lambda path: path.name):
            if not module.is_dir() or module.name in PUBLIC or module.name in TAIWAN or not (module / 'src').is_dir():
                continue
            if requested and module.name not in requested:
                continue
            sources = java_files(module / 'src')
            classes = module / 'build' / 'classes'
            if classes.exists():
                shutil.rmtree(classes)
            result = compile_sources(f'{module.name}.legacy', sources, classes, game_cp)
            framework = 'legacy'
            if result['status'] != 'passed' and modern['status'] == 'passed':
                if classes.exists():
                    shutil.rmtree(classes)
                modern_game_cp = f'{modern_jar}:{modern_cp}'
                result = compile_sources(f'{module.name}.modern', sources, classes, modern_game_cp)
                framework = 'modern'
            result['module'] = module.name
            result['framework'] = framework
            if result['status'] == 'passed':
                make_jar(classes, module / 'build' / f'{module.name}.jar')
            modules.append(result)
            print(json.dumps(result, ensure_ascii=False), flush=True)
    if requested and REPORT.exists():
        previous = json.loads(REPORT.read_text(encoding='utf-8'))
        merged = {
            item['module']: item
            for item in previous.get('modules', [])
            if isinstance(item, dict) and item.get('module')
        }
        merged.update({item['module']: item for item in modules})
        modules = sorted(merged.values(), key=lambda item: item['module'])
    report = {
        'core': core,
        'frameworks': {'legacy': core, 'modern': modern},
        'moduleCount': len(modules),
        'passed': sum(item['status'] == 'passed' for item in modules),
        'failed': sum(item['status'] == 'failed' for item in modules),
        'modules': modules,
    }
    REPORT.write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps({
        'core': core['status'], 'moduleCount': report['moduleCount'],
        'passed': report['passed'], 'failed': report['failed'],
    }, ensure_ascii=False), flush=True)


if __name__ == '__main__':
    main()
