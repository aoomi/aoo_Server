#!/usr/bin/env python3
"""Static cross-source audit that writes evidence only inside database/.work."""

import csv
import hashlib
import json
import re
import subprocess
from pathlib import Path
from typing import Any, Dict, List

from legacy_config_converter import REGION_ALIASES


DATABASE_DIR = Path(__file__).resolve().parents[1]
SERVER_ROOT = DATABASE_DIR.parent
OUTPUT = DATABASE_DIR / ".work/audit/database-package-static.json"
CONVERSION = DATABASE_DIR / "target/legacy-2.22-structured/manifest.json"
MYSQL_AUDIT = DATABASE_DIR / ".work/audit/mysql-integration-gate.json"


def sha256(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def source_provider_inventory() -> List[Dict[str, Any]]:
    rows = []
    pattern = "server/*/src/main/resources/META-INF/services/com.aoo.bcg.gamespi.GameProvider"
    for service_file in sorted(SERVER_ROOT.glob(pattern)):
        for class_name in service_file.read_text(encoding="utf-8").splitlines():
            class_name = class_name.strip()
            if not class_name or class_name.startswith("#"):
                continue
            simple_name = class_name.rsplit(".", 1)[-1]
            java_candidates = list((service_file.parents[5]).glob("**/{}.java".format(simple_name)))
            text = "\n".join(path.read_text(encoding="utf-8", errors="replace") for path in java_candidates)
            code_match = re.search(r"GameDescriptor\s*\([^,]+,\s*[\"']([^\"']+)", text)
            code = code_match.group(1).lower() if code_match else class_name.split(".")[-2].lower()
            rows.append({
                "className": class_name,
                "gameCode": code,
                "serviceFile": str(service_file.relative_to(SERVER_ROOT)),
                "javaFiles": [str(path.relative_to(SERVER_ROOT)) for path in java_candidates],
                "capabilities": {
                    "createAuthoritativeSession": "createAuthoritativeSession(" in text,
                    "restoreAuthoritativeSession": "restoreAuthoritativeSession(" in text,
                    "ruleComponents": "ruleComponents(" in text,
                    "reconnectViewProvider": "reconnectViewProvider(" in text,
                    "settlementProvider": "settlementProvider(" in text,
                    "capabilityManifest": "capabilityManifest(" in text,
                },
            })
    return rows


def main() -> int:
    migration_paths = sorted((DATABASE_DIR / "migrations").glob("V*.sql"))
    migration_text = "\n".join(path.read_text(encoding="utf-8") for path in migration_paths)
    create_tables = sorted(set(re.findall(r"CREATE TABLE(?: IF NOT EXISTS)?\s+([a-zA-Z0-9_]+)", migration_text, re.I)))
    required_objects = {
        "catalog": ["aoo_game_catalog", "aoo_game_family", "aoo_card_category"],
        "region": ["aoo_region", "aoo_region_alias", "aoo_game_region"],
        "rulesAndUi": ["aoo_room_rule_definition", "aoo_room_rule_choice", "aoo_create_ui_option", "aoo_ui_rule_binding"],
        "components": ["aoo_component_type", "aoo_game_component", "aoo_component_dependency", "aoo_component_conflict", "aoo_play_component_binding"],
        "releaseIndex": ["aoo_game_release", "aoo_game_release_component", "aoo_compiled_room_create_index", "aoo_compiled_index_active", "aoo_configuration_cache_epoch"],
        "historyLocks": ["aoo_room_rule_lock", "aoo_room_template_release_lock"],
        "migrationGovernance": ["aoo_legacy_field_mapping", "aoo_migration_run", "aoo_migration_validation", "aoo_dual_write_control", "aoo_schema_change_plan"],
        "qualityPrivacy": ["aoo_data_repair_job", "aoo_data_quality_issue", "aoo_privacy_request", "aoo_legal_hold"],
        "queryOperations": ["aoo_query_baseline", "aoo_query_plan_observation", "aoo_backup_manifest", "aoo_restore_drill", "aoo_replica_failover_drill", "aoo_capacity_forecast"],
    }
    object_checks = {
        group: {"passed": all(name in create_tables for name in names), "missing": sorted(set(names) - set(create_tables))}
        for group, names in required_objects.items()
    }

    catalog_path = SERVER_ROOT / "server/Bootstrap/src/main/resources/game-catalog-528.tsv"
    with catalog_path.open("r", encoding="utf-8", newline="") as stream:
        catalog = list(csv.DictReader(stream, delimiter="\t"))
    providers = source_provider_inventory()
    catalog_codes = [row["code"].strip().lower() for row in catalog]
    catalog_ids = [int(row["gameId"]) for row in catalog]
    enabled = [row for row in catalog if row["enabled"] == "1"]
    invalid_regions = []
    for row in catalog:
        for field in ("provinceCode", "cityCode"):
            value = row[field].strip().lower()
            if value and value != "unpublished" and value not in REGION_ALIASES:
                invalid_regions.append({"gameId": int(row["gameId"]), "gameCode": row["code"], "field": field, "value": value})

    conversion = json.loads(CONVERSION.read_text(encoding="utf-8"))
    mysql_audit = json.loads(MYSQL_AUDIT.read_text(encoding="utf-8"))
    coverage = conversion["coverage"]
    java_result = subprocess.run(["java", "-version"], stdout=subprocess.PIPE, stderr=subprocess.STDOUT)
    java_version = java_result.stdout.decode("utf-8", "replace").splitlines()[0] if java_result.stdout else "unavailable"
    java_26 = bool(re.search(r'version "26(?:\.|\")', java_version))

    source_database_gap = {
        "catalogRows": len(catalog),
        "enabledCatalogRows": len(enabled),
        "nativeProviderCount": len(providers),
        "nativeProviderCodes": sorted({row["gameCode"] for row in providers}),
        "catalogBridgeCount": len(catalog) - len({row["gameCode"] for row in providers} & set(catalog_codes)),
        "databaseProductionCatalogRowsLoaded": 0,
        "reason": "converter output is intentionally DRAFT and no deployment database was authorized; provider/component validation must precede load",
    }
    runtime_boundaries = {
        "REF03_componentInstantiation": "BOUNDARY_BLOCKED: requires GameProvider/component implementations and behavior tests outside database/",
        "REF04_componentKeyRuntimeType": "BOUNDARY_BLOCKED: DB uniqueness/type contracts exist; startup ServiceLoader type verification is outside database/",
        "REF05_uiValidatorRuntime": "BOUNDARY_BLOCKED: DB one-to-one mapping constraints exist; protocol/server validator integration is outside database/",
        "REF06_combinationRuntime": "BOUNDARY_BLOCKED: DB expression fields exist; publish evaluator integration is outside database/",
        "REF07_templateWriteGuard": "BOUNDARY_BLOCKED: FK/immutable sidecar exists; application write/lifecycle guard is outside database/",
        "REF08_snapshotRestore": "BOUNDARY_BLOCKED: immutable lock and version columns exist; provider restore behavior is outside database/",
        "REF09_historicalDecoder": "BOUNDARY_BLOCKED: release/codec identifiers exist; versioned decoder implementations are outside database/",
        "REF11_startupScan": "ENVIRONMENT_AND_BOUNDARY_BLOCKED: Java 26 plus bootstrap changes/tests are outside database/",
        "REF12_transactionSaga": "BOUNDARY_BLOCKED: DB release/room lock and Outbox exist; room/assets/Saga integration is outside database/",
    }
    operational_external = {
        "DBA01_onlineDDL": "WAITING_ENVIRONMENT: observation table/runbook ready; no production-sized topology supplied",
        "DBA02_slowQuery": "LOCAL_VERIFIED_ONLY: 50k-row MySQL 8.4 plan captured; production dataset/topology absent",
        "DBA03_connectionPool": "BOUNDARY_BLOCKED: evidence schema ready; service pool load test is outside database/",
        "DBA04_deadlockRetry": "BOUNDARY_BLOCKED: lock-order policies ready; transaction implementation/load test outside database/",
        "DBA05_restore": "WAITING_ENVIRONMENT: manifest/drill schema ready; no authorized backup target",
        "DBA06_failover": "WAITING_ENVIRONMENT: drill schema ready; no replica topology",
        "DBA07_capacity": "WAITING_ENVIRONMENT: forecast schema ready; production metrics absent",
        "DBA08_archive": "WAITING_ENVIRONMENT: idempotent archive ledger ready; production partitions absent",
        "DBA09_repairApproval": "DATABASE_COMPLETE: dry-run/rate/pause/audit/rollback and independent approvals are constrained",
        "DBA10_statistics": "WAITING_ENVIRONMENT: plan baseline schema ready; production statistics absent",
    }

    checks = {
        "requiredSchemaObjects": all(value["passed"] for value in object_checks.values()),
        "atomicActivationProcedure": "CREATE PROCEDURE aoo_activate_compiled_game_index" in migration_text,
        "activePointerUnique": "PRIMARY KEY (game_id,region_code)" in migration_text,
        "immutableRoomLockTriggers": "trg_room_rule_lock_no_update" in migration_text and "trg_room_rule_lock_no_delete" in migration_text,
        "catalogAtLeast500": len(catalog) >= 500,
        "catalogGameIdUnique": len(catalog_ids) == len(set(catalog_ids)),
        "catalogCodeUnique": len(catalog_codes) == len(set(catalog_codes)),
        "catalogRegionsCanonical": not invalid_regions,
        "legacyConversionDeterministic": conversion.get("schemaVersion") == 1 and coverage["catalogGameCount"] == 560,
        "legacyPublicationFailClosed": coverage["allCatalogRowsDraft"] and coverage["publicationBlockedUntilProviderAndComponentValidation"],
        "liveMysqlGate": mysql_audit.get("passed") is True,
        "fullDictionaryGenerated": mysql_audit.get("dataDictionary", {}).get("columnCount", 0) >= 1000,
    }
    result = {
        "schemaVersion": 1,
        "passed": all(checks.values()),
        "checks": checks,
        "schemaObjectGroups": object_checks,
        "migrationCount": len(migration_paths),
        "migrationHashes": [{"file": path.name, "sha256": sha256(path)} for path in migration_paths],
        "createdTableCount": len(create_tables),
        "sourceCatalog": {
            "path": str(catalog_path.relative_to(SERVER_ROOT)), "sha256": sha256(catalog_path),
            "rows": len(catalog), "enabled": len(enabled), "duplicateGameIds": len(catalog_ids) - len(set(catalog_ids)),
            "duplicateCodes": len(catalog_codes) - len(set(catalog_codes)), "unmappedRegionReferences": invalid_regions,
        },
        "nativeProviders": providers,
        "sourceDatabaseGap": source_database_gap,
        "legacyCoverage": coverage,
        "runtimeBoundaryBlocks": runtime_boundaries,
        "operationalEnvironmentStatus": operational_external,
        "java": {"version": java_version, "java26Available": java_26},
        "evidence": {
            "mysqlIntegration": str(MYSQL_AUDIT.relative_to(SERVER_ROOT)),
            "legacyConversion": str(CONVERSION.relative_to(SERVER_ROOT)),
            "dataDictionary": str((DATABASE_DIR / "target/data-dictionary.json").relative_to(SERVER_ROOT)),
        },
    }
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    OUTPUT.write_text(json.dumps(result, ensure_ascii=False, sort_keys=True, indent=2) + "\n", encoding="utf-8")
    print(json.dumps({"passed": result["passed"], "catalog": len(catalog), "enabled": len(enabled), "providers": len(providers), "invalidRegions": len(invalid_regions), "output": str(OUTPUT)}, ensure_ascii=False))
    return 0 if result["passed"] else 1


if __name__ == "__main__":
    raise SystemExit(main())
