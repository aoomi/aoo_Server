#!/usr/bin/env python3
"""2.22 isolation gate. Read-only scanner; JSON output is deterministic."""
from __future__ import annotations

import argparse, hashlib, json, os, re, sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
DEFAULT_LEGACY = ROOT.parent / "Test"
SCOPES = ("Client", "Server", "Admin", ".github")
SKIP_PARTS = {"node_modules", "library", "temp", "dist", "target", "build", ".git", ".cache", "logs", ".work", "work", ".runtime"}
TEXT_EXT = {".ts", ".tsx", ".js", ".mjs", ".cjs", ".vue", ".java", ".kt", ".xml", ".gradle", ".properties", ".yml", ".yaml", ".json", ".toml", ".ini", ".cfg", ".conf", ".sh", ".rb", ".py", ".sql", ".php", ".html", ".prefab", ".scene", ".meta", ".asset", ".md", ".txt"}
PRODUCTION_SEGMENTS = {"src", "assets", "server", "config", "deploy", "database", "build-templates", "settings", ".github"}
EVIDENCE_SEGMENTS = {"docs", "migration", "migrations", "original", "backups", "reference", "development", "tests", "test", "tools", "scripts"}
SCENE_PROFILE_PATH = "Client/profiles/v2/packages/scene.json"
RETIRED_SCENE_PROFILE_UUIDS = {
  "3842e73c-4567-42eb-b5b6-7a1e499f42cd",
  "40995e53-6714-4fb1-8d39-6e25f4c0aa90",
  "e293c14b-a711-44ce-83f0-89a1e70e6361",
  "ef7520f3-b8be-4b69-a3c3-205dd36c5b42",
  "1ebde2bc-327d-4f89-ad64-60fdf976469c",
  "2e1d38db-1cf9-41e2-88af-6a182561a07b",
}

RULES = {
  "legacy-path": re.compile(r"(?i)(?:/Users/[^\s'\"]+/Code/Game/BCG/Test\b|(?:^|[/'\"=:\s])\.\.?/Test(?:/|\b)|\bTest/QH[_/-]|\bQH_DFMJ\b)"),
  "legacy-version": re.compile(r"(?i)(?:(?:legacy|旧版|旧包|迁移|baseline|archive|reference)[^\n]{0,80}(?<!\d)(?:2\.2\.2|2\.22)(?!\d)|(?<!\d)(?:2\.2\.2|2\.22)(?!\d)[^\n]{0,80}(?:legacy|旧版|旧包|迁移|baseline|archive|reference))"),
  "legacy-package": re.compile(r"(?i)\b(?:com\.qh(?:\.[\w$-]+)+|qh[_-](?:client|server|game|hall|dfmj|jytf)\b)"),
  "legacy-entry": re.compile(r"(?i)(?:(?:https?|wss?)://[^\s'\"<>]*(?:qh[_./-]|legacy|/test/|\.php\b)[^\s'\"<>]*|(?:/[\w.-]+)+\.php\b)"),
  "dynamic-loader": re.compile(r"(?i)\b(?:require|import)\s*\(\s*(?!['\"])[^)]+\)"),
  "copy-bridge": re.compile(r"(?i)\b(?:cp|rsync|ditto|copy|xcopy|robocopy)\b[^\n]*(?:Test|2\.22|2\.2\.2|QH[_/-])"),
  "runtime-fallback": re.compile(r"(?i)\b(?:fallbackTo|orElse|tryLegacy|legacyFallback)\b[^\n]{0,160}(?:2\.22|QH[_/-]|Test/|legacy)|\blegacy[^\n]{0,100}\b(?:endpoint|baseUrl|websocket|classpath|bundle|external|subgame)\b"),
  "build-dependency": re.compile(r"(?i)(?:<artifactId>[^<]*(?:legacy|qh)[^<]*</artifactId>|(?:classpath|implementation|api|runtimeOnly|module)[^\n]*(?:2\.22|qh[_-]|aoo-legacy|legacy-kernel))"),
  "database-direct": re.compile(r"(?i)\b(?:from|join|update|insert\s+into|delete\s+from)\s+[`\"]?(?:qh_|legacy_|t_(?:user|player|room|club|game|account)\b)[\w$]*"),
  "legacy-brand": re.compile(r"(?i)(?<![a-z0-9])qh(?=\b|[_./:\-])|情怀"),
  "legacy-interface": re.compile(r"(?i)(?:[\"']/(?:api/)?(?:v\d+/)?legacy(?:/|[\"'])|[\"']/(?:qh|old)(?:[-_/][\w.-]+)+(?:[\"']|\?))"),
}

COVERAGE = (
  "client", "server", "admin", "build-and-ci", "scene", "prefab", "resource",
  "dynamic-path", "2.22", "legacy-runtime", "native-prefab", "qh/QH/sentiment",
  "legacy-interface", "legacy-uuid", "legacy-dependency", "duplicate-entry",
)

def ignored(path: Path) -> bool:
    rel = path.relative_to(ROOT)
    if any(part in SKIP_PARTS for part in rel.parts): return True
    return any("backup-" in part.lower() or part.startswith("archived-") for part in rel.parts)

def classification(rel: Path) -> str:
    parts = {p.lower() for p in rel.parts}
    if rel.suffix.lower() in {".md", ".txt"}: return "readonly-evidence"
    posix=rel.as_posix().lower()
    if "/build-audit/" in "/"+posix: return "localized-generated-resource"
    if "client/assets/common/code/runtime/compatibilityapp/" in posix: return "isolated-compatibility"
    if posix.endswith("server/config/large-file-declarations.json"): return "readonly-evidence"
    if posix.endswith("deprecatedentrypointblocklist.java") or posix.endswith("legacyprotocolusage.java"): return "isolated-compatibility"
    if any(x in posix for x in ("server/server/legacycommon/", "server/server/legacygamehall/", "server/server/legacyaccountserver/")): return "isolated-compatibility"
    if parts & EVIDENCE_SEGMENTS: return "readonly-evidence"
    if parts & PRODUCTION_SEGMENTS: return "production-reachable"
    return "configuration-or-build"

def fingerprint(rule: str, path: str, line: int, excerpt: str) -> str:
    normalized = re.sub(r"\s+", " ", excerpt.strip())
    return hashlib.sha256(f"{rule}\0{path}\0{line}\0{normalized}".encode()).hexdigest()[:20]

def finding(rule, rel, line, excerpt, kind=None):
    cls = kind or classification(Path(rel))
    return {"id": fingerprint(rule, rel, line, excerpt), "rule": rule, "path": rel,
            "line": line, "classification": cls, "excerpt": excerpt.strip()[:300]}

def rule_classification(rule: str, rel: str, lines, index: int):
    if rule != "legacy-interface": return None
    context="\n".join(lines[max(0,index-2):min(len(lines),index+3)])
    deny=re.compile(r"(?i)\b(?:410|retired|disabled|forbidden|deny|block(?:ed|list)?)\b")
    if deny.search(context) or rel.endswith("/HttpRoutePolicy.java"):
        return "isolated-compatibility"
    return None

def scan_symlinks():
    out=[]
    for scope in SCOPES:
        base=ROOT/scope
        if not base.exists(): continue
        for d, dirs, files in os.walk(base, followlinks=False):
            dp=Path(d); dirs[:] = [x for x in dirs if x not in SKIP_PARTS and "backup-" not in x.lower()]
            for name in dirs+files:
                p=dp/name
                if p.is_symlink():
                    rel=p.relative_to(ROOT).as_posix(); target=os.readlink(p)
                    out.append(finding("symlink", rel, 0, target, "production-reachable" if classification(Path(rel)) != "readonly-evidence" else "readonly-evidence"))
    return out

def scan_native_prefab_duplicates():
    """Block byte-identical Native prefab aliases that remain production reachable."""
    base=ROOT/"Client"/"assets"; groups={}
    if not base.exists(): return []
    for p in base.rglob("*.prefab"):
        if ignored(p): continue
        try: digest=hashlib.sha256(p.read_bytes()).hexdigest()
        except OSError: continue
        groups.setdefault(digest, []).append(p)
    out=[]
    for paths in groups.values():
        if len(paths) < 2 or not any("native" in {x.lower() for x in p.parts} for p in paths): continue
        rels=sorted(p.relative_to(ROOT).as_posix() for p in paths)
        excerpt="byte-identical prefab aliases: " + ", ".join(rels)
        for rel in rels:
            out.append(finding("native-prefab-duplicate", rel, 0, excerpt, "production-reachable"))
    return out

def scan_duplicate_entries():
    """Detect duplicate literal entry registration inside one production source file."""
    out=[]
    rx=re.compile(r"(?i)(?:createContext|addRoute|registerRoute|route|endpoint)\s*\(\s*[\"']([^\"']+)[\"']")
    for scope in SCOPES:
        base=ROOT/scope
        if not base.exists(): continue
        for p in base.rglob("*"):
            if not p.is_file() or ignored(p) or p.suffix.lower() not in {".ts", ".js", ".mjs", ".vue", ".java", ".kt"}: continue
            try: lines=p.read_text(errors="replace").splitlines()
            except OSError: continue
            seen={}
            for no,line in enumerate(lines,1):
                for entry in rx.findall(line):
                    if entry in seen:
                        rel=p.relative_to(ROOT).as_posix()
                        out.append(finding("duplicate-entry",rel,no,f"{entry} first registered at line {seen[entry]}",classification(Path(rel))))
                    else: seen[entry]=no
    return out

def legacy_uuids(legacy: Path):
    uuids=set()
    if not legacy.exists(): return uuids, []
    candidates = [legacy/"QH_DFMJ"/x/"assets" for x in ("client-unified-3.8.6","client-unified","client-next-3.8.8","client")]
    candidates += [legacy/"QH_JYTF_client"/"TF_Client"/"assets"]
    candidates += list((legacy/"TF_Game").glob("Client*/assets")) if (legacy/"TF_Game").exists() else []
    sources=[p for p in candidates if p.is_dir()]
    rx=re.compile(r"(?im)(?:^uuid:\s*|[\"']uuid[\"']\s*:\s*[\"'])([0-9a-f-]{22,36})")
    for source_root in sources:
      for d, dirs, files in os.walk(source_root):
        dirs[:] = [x for x in dirs if x not in SKIP_PARTS]
        for n in files:
            if not n.endswith(".meta"): continue
            p=Path(d)/n
            source=p.with_suffix("")
            if source.suffix.lower() not in {".prefab", ".scene"}: continue
            try:
                if p.stat().st_size <= 262144:
                    m=rx.search(p.read_text(errors="replace"))
                    if m: uuids.add(m.group(1).lower())
            except OSError: pass
    return uuids, [str(p) for p in sources]

def local_asset_uuids():
    out=set(); rx=re.compile(r'(?im)(?:^uuid:\s*|[\"\']uuid[\"\']\s*:\s*[\"\'])([0-9a-f-]{22,36})')
    base=ROOT/"Client"/"assets"
    if not base.exists(): return out
    for d, dirs, files in os.walk(base):
        dirs[:] = [x for x in dirs if x not in SKIP_PARTS]
        for n in files:
            if not n.endswith('.meta'): continue
            try:
                m=rx.search((Path(d)/n).read_text(errors='replace'))
                if m: out.add(m.group(1).lower())
            except OSError: pass
    return out

def scan_files(uuids):
    out=[]; scanned=0
    local_uuids=local_asset_uuids() if uuids else set()
    uuid_rx = re.compile(r"[0-9a-f]{8}(?:-[0-9a-f]{4}){3}-[0-9a-f]{12}|[0-9a-f]{22,32}", re.I)
    for scope in SCOPES:
        base=ROOT/scope
        if not base.exists(): continue
        for d, dirs, files in os.walk(base):
            dirs[:] = [x for x in dirs if x not in SKIP_PARTS and "backup-" not in x.lower() and not x.startswith("archived-")]
            for name in files:
                p=Path(d)/name
                if ignored(p) or (p.suffix.lower() not in TEXT_EXT and name not in {"pom.xml", "Dockerfile", "Makefile"}): continue
                try:
                    if p.stat().st_size > 5_000_000: continue
                    text=p.read_text(encoding="utf-8", errors="replace")
                except OSError: continue
                scanned += 1; rel=p.relative_to(ROOT).as_posix()
                lines=text.splitlines()
                for no, line in enumerate(lines, 1):
                    if rel == SCENE_PROFILE_PATH:
                        for value in uuid_rx.findall(line):
                            if value.lower() in RETIRED_SCENE_PROFILE_UUIDS:
                                out.append(finding("retired-scene-profile-uuid", rel, no, line, "configuration-or-build"))
                    for rule, rx in RULES.items():
                        if rule == "dynamic-loader" and p.suffix.lower() not in {".js", ".mjs", ".cjs", ".ts", ".tsx", ".vue"}: continue
                        if rule == "copy-bridge" and p.suffix.lower() not in {".sh", ".rb", ".py", ".yml", ".yaml", ".json"}: continue
                        if rx.search(line): out.append(finding(rule, rel, no, line, rule_classification(rule, rel, lines, no-1)))
                    if uuids and p.suffix.lower() in {".prefab", ".scene", ".asset", ".json"}:
                        for value in uuid_rx.findall(line):
                            if value.lower() in uuids:
                                kind="localized-generated-resource" if value.lower() in local_uuids else None
                                out.append(finding("legacy-uuid", rel, no, line, kind))
    return scanned, out

def main():
    ap=argparse.ArgumentParser(); ap.add_argument("--legacy-root", type=Path, default=DEFAULT_LEGACY)
    ap.add_argument("--allowlist", type=Path, default=Path(__file__).with_name("allowlist.json"))
    ap.add_argument("--output", type=Path); ap.add_argument("--no-legacy-uuid", action="store_true")
    args=ap.parse_args()
    allow=json.loads(args.allowlist.read_text()) if args.allowlist.exists() else {"entries":[]}
    allowed={x["id"]:x for x in allow.get("entries",[])}
    uuids, uuid_sources=(set(), []) if args.no_legacy_uuid else legacy_uuids(args.legacy_root)
    scanned, findings=scan_files(uuids); findings.extend(scan_symlinks())
    findings.extend(scan_native_prefab_duplicates()); findings.extend(scan_duplicate_entries())
    findings={f["id"]:f for f in findings}; findings=sorted(findings.values(),key=lambda x:(x["path"],x["line"],x["rule"]))
    for f in findings:
        a=allowed.get(f["id"])
        f["allowed"] = bool(a and f["classification"] == "readonly-evidence" and a.get("reason"))
    blocking=[f for f in findings if not f["allowed"] and f["classification"] in {"production-reachable", "configuration-or-build"}]
    unreviewed=[f for f in findings if not f["allowed"] and f["classification"] == "readonly-evidence"]
    full_mode=not args.no_legacy_uuid
    signable=full_mode and args.legacy_root.exists() and bool(uuid_sources) and bool(uuids) and not blocking
    report={"schemaVersion":2,"root":str(ROOT),"legacyRoot":str(args.legacy_root),"legacyRootPresent":args.legacy_root.exists(),"uuidSourceRoots":uuid_sources,"coverage":list(COVERAGE),
      "summary":{"mode":"full" if full_mode else "text-only","filesScanned":scanned,"legacyUuids":len(uuids),"findings":len(findings),"allowedEvidence":sum(f["allowed"] for f in findings),"unreviewedEvidence":len(unreviewed),"blocking":len(blocking),"passed":not blocking,"signable":signable},
      "policy":"Only exact, reasoned read-only documentation/migration evidence may be allowed; every production/configuration/build finding blocks.","findings":findings}
    payload=json.dumps(report,ensure_ascii=False,indent=2)+"\n"
    if args.output: args.output.parent.mkdir(parents=True,exist_ok=True); args.output.write_text(payload)
    print(json.dumps(report["summary"],ensure_ascii=False)); return 0 if not blocking else 2
if __name__ == "__main__": sys.exit(main())
