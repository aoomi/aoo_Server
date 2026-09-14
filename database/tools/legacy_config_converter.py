#!/usr/bin/env python3
"""Deterministically convert immutable 2.22 JSON/DDL sources to typed staging assets.

The converter never publishes a game. Ambiguous family/provider/field mappings are
preserved losslessly and marked REVIEW_REQUIRED, so migration cannot silently guess.
All output is constrained to database/target or database/.work.
"""

import argparse
import csv
import hashlib
import json
import re
from collections import Counter, defaultdict
from pathlib import Path
from typing import Any, Dict, Iterable, List, Optional, Tuple


DATABASE_DIR = Path(__file__).resolve().parents[1]
SERVER_ROOT = DATABASE_DIR.parent
DEFAULT_SOURCE = SERVER_ROOT / "server/LegacyCommon/conf/jsonData"
DEFAULT_OUTPUT = DATABASE_DIR / "target/legacy-2.22-structured"

REGION_ALIASES = {
    "all": "GLOBAL", "anhui": "CN-34", "chongqing": "CN-50",
    "fujian": "CN-35", "fujiang": "CN-35", "gansu": "CN-62",
    "guangdong": "CN-44", "guangxi": "CN-45", "guizhou": "CN-52",
    "haerbin": "CN-23-01", "hainan": "CN-46", "hebei": "CN-13",
    "heilongjiang": "CN-23", "henan": "CN-41", "henna": "CN-41",
    "hubei": "CN-42", "hunan": "CN-43", "jiangsu": "CN-32",
    "jiangxi": "CN-36", "jilin": "CN-22", "liaoning": "CN-21",
    "neimenggu": "CN-15", "ningxia": "CN-64", "riben": "JP",
    "shandong": "CN-37", "shanghai": "CN-31", "shanxi": "CN-14",
    "shanxisheng": "CN-61", "sichaun": "CN-51", "sichuan": "CN-51",
    "taiwan": "CN-71", "tianjin": "CN-12", "xinjiang": "CN-65",
    "xizang": "CN-54", "yindu": "IN", "yunnan": "CN-53",
    "zhejiang": "CN-33", "zichuan": "CN-37-03-02",
    "sc": "CN-51", "cd": "CN-51-01", "yongzhou": "CN-43-11",
}

CATEGORY_FAMILY = {
    "mahjong": ("MAHJONG", "MAHJONG_UNCLASSIFIED"),
    "poker": ("POKER", "POKER_UNCLASSIFIED"),
}

JSON_TARGETS = {
    "gamelist": {
        "id": "aoo_game_catalog.game_id",
        "gameName": "aoo_game_catalog.game_code",
        "gameType": "aoo_game_catalog.category_code/family_code",
        "region": "aoo_game_region.region_code",
        "isOpen": "aoo_game_catalog.status (requires provider validation)",
        "sortNum": "aoo_game_region.priority",
        "imgUrl": "aoo_game_release.catalog_snapshot.imageUri",
    },
    "gamecreate": {
        "ID": "aoo_create_ui_option.ordinal/source id",
        "GameName": "aoo_game_catalog.game_code lookup",
        "Title": "aoo_create_ui_option.title",
        "Key": "aoo_create_ui_option.option_key",
        "ToggleType": "aoo_create_ui_option.control_type",
        "ToggleCount": "aoo_create_ui_choice count validation",
        "ShowIndexs": "aoo_create_ui_option.default_value",
        "ToggleDesc": "aoo_create_ui_choice.display_name",
        "Display": "aoo_create_ui_option.visible_expression",
        "AtRow": "aoo_create_ui_option.visible_expression.legacyAtRow",
        "AtRowEx": "aoo_create_ui_option.visible_expression.legacyAtRowEx",
        "Spacing": "aoo_create_ui_option.visible_expression.legacySpacing",
        "IsShowHelp": "aoo_create_ui_option.visible_expression.showHelp",
        "isWanFa": "aoo_create_ui_option.visible_expression.isPlayRule",
    },
    "roomcost": {
        "id": "source_primary_key",
        "selectCity": "aoo_region_alias.source_region_code (requires city crosswalk)",
        "roomid": "source_business_key",
        "GameType": "aoo_game_catalog.game_code lookup",
        "SetCount": "aoo_room_cost_policy.round_count",
        "PeopleMin": "aoo_room_cost_policy.player_count lower bound",
        "PeopleMax": "aoo_room_cost_policy.player_count upper bound",
        "AaCostCount": "aoo_room_cost_policy.cost_minor[AA]",
        "WinCostCount": "aoo_room_cost_policy.cost_minor[WINNER]",
        "CostCount": "aoo_room_cost_policy.cost_minor[OWNER]",
        "ClubAaCostCount": "aoo_room_cost_policy.club_cost_minor[AA]",
        "ClubWinCostCount": "aoo_room_cost_policy.club_cost_minor[WINNER]",
        "ClubCostCount": "aoo_room_cost_policy.cost_minor[CLUB]",
        "UnionCostCount": "aoo_room_cost_policy.cost_minor[UNION]",
        "Sign": "migration validation flag",
    },
    "gamehelp": {
        "id": "aoo_game_help_content.block_ordinal/source id",
        "gameName": "aoo_game_catalog.game_code lookup",
        "isTitle": "aoo_game_help_content.block_type",
        "img": "aoo_game_help_content.image_uri",
        "desc": "aoo_game_help_content.title/body",
        "fontSize": "aoo_game_release.ui_snapshot.legacyFontSize",
        "fontColor": "aoo_game_release.ui_snapshot.legacyFontColor",
    },
}


def canonical_json(value: Any) -> bytes:
    return json.dumps(value, ensure_ascii=False, sort_keys=True, separators=(",", ":")).encode("utf-8")


def digest_value(value: Any) -> str:
    return hashlib.sha256(canonical_json(value)).hexdigest()


def digest_file(path: Path) -> str:
    digest = hashlib.sha256()
    with path.open("rb") as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b""):
            digest.update(chunk)
    return digest.hexdigest()


def require_output_boundary(path: Path) -> Path:
    resolved = path.resolve()
    allowed = [(DATABASE_DIR / "target").resolve(), (DATABASE_DIR / ".work").resolve()]
    if not any(resolved == root or root in resolved.parents for root in allowed):
        raise ValueError("output must stay under database/target or database/.work")
    return resolved


def load_json(path: Path) -> Dict[str, Dict[str, Any]]:
    with path.open("r", encoding="utf-8") as stream:
        value = json.load(stream)
    if not isinstance(value, dict):
        raise ValueError("{} must have an object root".format(path))
    return value


def write_jsonl(path: Path, records: Iterable[Dict[str, Any]]) -> Tuple[int, str]:
    count = 0
    digest = hashlib.sha256()
    with path.open("w", encoding="utf-8", newline="\n") as stream:
        for record in records:
            payload = canonical_json(record) + b"\n"
            stream.write(payload.decode("utf-8"))
            digest.update(payload)
            count += 1
    return count, digest.hexdigest()


def split_csv(value: Any) -> List[str]:
    if value is None:
        return []
    return [item.strip() for item in str(value).split(",") if item.strip()]


def parse_indices(value: Any) -> List[int]:
    result = []
    for raw in split_csv(value):
        try:
            result.append(int(raw))
        except ValueError:
            continue
    return result


def game_code(value: Any) -> str:
    return str(value or "").strip().lower()


def normalize_catalog(gamelist: Dict[str, Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], Dict[str, Dict[str, Any]], List[Dict[str, Any]]]:
    records = []
    by_code = {}
    issues = []
    for source_key, row in sorted(gamelist.items(), key=lambda item: int(item[1].get("id", item[0]))):
        code = game_code(row.get("gameName"))
        category, family = CATEGORY_FAMILY.get(str(row.get("gameType", "")).lower(), ("OTHER", "OTHER_UNCLASSIFIED"))
        source_region = str(row.get("region", "")).strip().lower()
        canonical_region = REGION_ALIASES.get(source_region)
        source_enabled = int(row.get("isOpen", 0) or 0) == 1
        record = {
            "gameId": int(row["id"]),
            "gameCode": code,
            "displayName": code,
            "categoryCode": category,
            "familyCode": family,
            "providerKey": "legacy.catalog.{}".format(code),
            "regionCode": canonical_region,
            "legacyRegion": source_region,
            "priority": int(row.get("sortNum", 100) or 100),
            "imageUri": row.get("imgUrl"),
            "sourceEnabled": source_enabled,
            "targetStatus": "DRAFT",
            "publicationBlockers": ["NATIVE_PROVIDER_NOT_VERIFIED", "FAMILY_CLASSIFICATION_UNVERIFIED"],
            "sourcePrimaryKey": str(source_key),
        }
        record["contentHash"] = digest_value(record)
        records.append(record)
        by_code[code] = record
        if not canonical_region:
            issues.append({"type": "MISSING_REGION_MAPPING", "object": "gamelist", "key": source_key, "value": source_region})
    return records, by_code, issues


def normalize_ui(gamecreate: Dict[str, Dict[str, Any]], catalog: Dict[str, Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    pair_counts = Counter((game_code(row.get("GameName")), str(row.get("Key", "")).strip()) for row in gamecreate.values())
    records = []
    issues = []
    for source_key, row in sorted(gamecreate.items(), key=lambda item: int(item[1].get("ID", item[0]))):
        code = game_code(row.get("GameName"))
        raw_key = str(row.get("Key", "")).strip()
        source_id = int(row.get("ID", source_key))
        duplicate = pair_counts[(code, raw_key)] > 1
        normalized_key = raw_key if not duplicate else "{}__legacy_{}".format(raw_key or "option", source_id)
        choice_labels = split_csv(row.get("ToggleDesc"))
        toggle_count = int(row.get("ToggleCount", len(choice_labels)) or 0)
        defaults = parse_indices(row.get("ShowIndexs"))
        out_of_range = [index for index in defaults if index < 0 or index > toggle_count]
        catalog_row = catalog.get(code)
        record = {
            "sourceId": source_id,
            "gameCode": code,
            "gameId": catalog_row["gameId"] if catalog_row else None,
            "playVersion": "legacy-2.22",
            "optionKey": normalized_key,
            "legacyOptionKey": raw_key,
            "controlType": "RADIO" if int(row.get("ToggleType", 0) or 0) == 0 else "CHECKBOX",
            "title": str(row.get("Title", "")).replace("/s", " ").strip(),
            "defaultSourceIndices": defaults,
            "required": int(row.get("ToggleType", 0) or 0) == 0,
            "choices": [{"choiceKey": "legacy_{}".format(index), "wireValue": index, "displayName": label, "ordinal": index} for index, label in enumerate(choice_labels, 1)],
            "visibleExpression": {
                "display": int(row.get("Display", 1) or 0) == 1,
                "legacyAtRow": split_csv(row.get("AtRow")),
                "legacyAtRowEx": split_csv(row.get("AtRowEx")),
                "legacySpacing": split_csv(row.get("Spacing")),
                "showHelp": int(row.get("IsShowHelp", 0) or 0) == 1,
                "isPlayRule": int(row.get("isWanFa", 0) or 0) == 1,
            },
            "mappingStatus": "REVIEW_REQUIRED" if duplicate or not catalog_row or out_of_range else "STRUCTURALLY_VALID",
        }
        record["contentHash"] = digest_value(record)
        records.append(record)
        if duplicate:
            issues.append({"type": "DUPLICATE_UI_OPTION_KEY", "object": code, "key": raw_key, "sourceId": source_id})
        if not catalog_row:
            issues.append({"type": "UI_GAME_NOT_IN_CATALOG", "object": code, "key": raw_key, "sourceId": source_id})
        if toggle_count != len(choice_labels):
            issues.append({"type": "UI_CHOICE_COUNT_MISMATCH", "object": code, "key": raw_key, "sourceId": source_id, "expected": toggle_count, "actual": len(choice_labels)})
        if out_of_range:
            issues.append({"type": "UI_DEFAULT_OUT_OF_RANGE", "object": code, "key": raw_key, "sourceId": source_id, "values": out_of_range})
    return records, issues


def normalize_room_cost(roomcost: Dict[str, Dict[str, Any]], catalog: Dict[str, Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    records = []
    issues = []
    payer_fields = (
        ("AA", "AaCostCount", "ClubAaCostCount"),
        ("WINNER", "WinCostCount", "ClubWinCostCount"),
        ("OWNER", "CostCount", None),
        ("CLUB", "ClubCostCount", None),
        ("UNION", "UnionCostCount", None),
    )
    upper_catalog = {code.upper(): row for code, row in catalog.items()}
    for source_key, row in sorted(roomcost.items()):
        code = str(row.get("GameType", "")).strip()
        catalog_row = upper_catalog.get(code.upper())
        for player_count in range(int(row.get("PeopleMin", 0) or 0), int(row.get("PeopleMax", 0) or 0) + 1):
            for payer_mode, cost_field, club_cost_field in payer_fields:
                record = {
                    "sourceId": str(row.get("id", source_key)),
                    "sourceRoomId": str(row.get("roomid", "")),
                    "legacyCityCode": str(row.get("selectCity", "")),
                    "gameCode": code.lower(),
                    "gameId": catalog_row["gameId"] if catalog_row else None,
                    "playVersion": "legacy-2.22",
                    "regionCode": catalog_row["regionCode"] if catalog_row else None,
                    "roundCount": int(row.get("SetCount", 0) or 0),
                    "playerCount": player_count,
                    "payerMode": payer_mode,
                    "currencyCode": "ROOM_CARD",
                    "costMinor": int(row.get(cost_field, 0) or 0),
                    "clubCostMinor": int(row.get(club_cost_field, 0) or 0) if club_cost_field else None,
                    "sourceSign": int(row.get("Sign", 0) or 0),
                    "mappingStatus": "REVIEW_REQUIRED" if not catalog_row else "STRUCTURALLY_VALID",
                }
                record["contentHash"] = digest_value(record)
                records.append(record)
        if not catalog_row:
            issues.append({"type": "ROOM_COST_GAME_NOT_IN_CATALOG", "object": code, "key": source_key})
    return records, issues


def normalize_help(gamehelp: Dict[str, Dict[str, Any]], catalog: Dict[str, Dict[str, Any]]) -> Tuple[List[Dict[str, Any]], List[Dict[str, Any]]]:
    records = []
    issues = []
    ordinal_by_game = defaultdict(int)
    for source_key, row in sorted(gamehelp.items(), key=lambda item: int(item[1].get("id", item[0]))):
        code = game_code(row.get("gameName"))
        ordinal_by_game[code] += 1
        catalog_row = catalog.get(code)
        is_title = int(row.get("isTitle", 0) or 0) == 1
        image = row.get("img")
        record = {
            "sourceId": int(row.get("id", source_key)),
            "gameCode": code,
            "gameId": catalog_row["gameId"] if catalog_row else None,
            "playVersion": "legacy-2.22",
            "localeCode": "zh-CN",
            "blockOrdinal": ordinal_by_game[code],
            "blockType": "TITLE" if is_title else ("IMAGE" if image and str(image).lower() != "null" else "PARAGRAPH"),
            "title": row.get("desc") if is_title else None,
            "body": None if is_title else row.get("desc"),
            "imageUri": None if image is None or str(image).lower() == "null" else image,
            "legacyStyle": {"fontSize": row.get("fontSize"), "fontColor": row.get("fontColor")},
            "mappingStatus": "REVIEW_REQUIRED" if not catalog_row else "STRUCTURALLY_VALID",
        }
        record["contentHash"] = digest_value(record)
        records.append(record)
        if not catalog_row:
            issues.append({"type": "HELP_GAME_NOT_IN_CATALOG", "object": code, "key": source_key})
    return records, issues


def classify_legacy_table(table_name: str) -> Tuple[str, List[str]]:
    name = table_name.lower()
    if re.search(r"gametype|hutype|gamesettings|public_gamelist|category", name):
        return "GAME_CATALOG_RULE", ["aoo_game_catalog", "aoo_play_version", "aoo_room_rule_definition"]
    if re.search(r"area|city|region|gps", name):
        return "REGION", ["aoo_region", "aoo_game_region"]
    if re.search(r"room.?config|template", name):
        return "ROOM_TEMPLATE", ["aoo_room_template", "aoo_room_template_release_lock"]
    if re.search(r"game.?room|player.?game.?room", name):
        return "ROOM_RUNTIME", ["aoo_room_rule_lock", "aoo_room_snapshot", "aoo_room_event"]
    if re.search(r"play.?back|play.?game|rank|award.?record|hu.?reward", name):
        return "RESULT_REPLAY", ["aoo_settlement", "perspective_replay_event"]
    if re.search(r"roomcard|recharge|money|tixian|caiwu|fencheng|goods|pay|currency", name):
        return "ASSET", ["aoo_currency_balance", "aoo_ledger"]
    if re.search(r"club|family|guild|union", name):
        return "CLUB", ["aoo_club_member", "aoo_room_template"]
    if re.search(r"account|player|login|session", name):
        return "PLAYER", ["aoo_session"]
    return "ARCHIVE_REVIEW", ["aoo_legacy_record_envelope"]


def extract_ddl_fields(sql_path: Path) -> Iterable[Dict[str, Any]]:
    current_table: Optional[str] = None
    create_pattern = re.compile(r"^CREATE TABLE(?: IF NOT EXISTS)?\s+[`\"]?([^`\"\s(]+)", re.I)
    column_pattern = re.compile(r"^\s*`([^`]+)`\s+([^,]+)")
    with sql_path.open("r", encoding="utf-8", errors="replace") as stream:
        for line in stream:
            create_match = create_pattern.match(line)
            if create_match:
                current_table = create_match.group(1)
                continue
            if current_table:
                if line.lstrip().startswith(")"):
                    current_table = None
                    continue
                column_match = column_pattern.match(line)
                if column_match:
                    domain, targets = classify_legacy_table(current_table)
                    column = column_match.group(1)
                    yield {
                        "sourceFile": sql_path.name,
                        "sourceTable": current_table,
                        "sourceColumn": column,
                        "sourceDefinition": column_match.group(2).strip(),
                        "domain": domain,
                        "canonicalTargets": targets,
                        "losslessLanding": "aoo_legacy_record_envelope.payload_json.{}".format(column),
                        "mappingStatus": "REVIEW_REQUIRED" if domain == "ARCHIVE_REVIEW" else "DOMAIN_MAPPED_FIELD_REVIEW_REQUIRED",
                    }


def build_field_mapping_rows(source_objects: Dict[str, Dict[str, Any]], ddl_fields: List[Dict[str, Any]]) -> List[Dict[str, Any]]:
    rows = []
    for object_name, records in sorted(source_objects.items()):
        fields = sorted({str(field) for record in records.values() for field in record.keys()})
        targets = JSON_TARGETS[object_name]
        for field in fields:
            rows.append({
                "domain": object_name.upper(), "sourceSystem": "legacy-2.22-json",
                "sourceObject": "{}.json".format(object_name), "sourceField": field,
                "target": targets.get(field, "aoo_legacy_record_envelope.payload_json.{}".format(field)),
                "transform": "typed-normalization" if field in targets else "lossless-preserve",
                "nullPolicy": "QUARANTINE", "validation": "source field retained and record SHA-256 verified",
                "status": "MAPPED" if field in targets else "REVIEW_REQUIRED",
            })
    for field in ddl_fields:
        rows.append({
            "domain": field["domain"], "sourceSystem": "legacy-2.22-sql",
            "sourceObject": "{}:{}".format(field["sourceFile"], field["sourceTable"]),
            "sourceField": field["sourceColumn"], "target": ";".join(field["canonicalTargets"]),
            "transform": field["losslessLanding"], "nullPolicy": "PRESERVE",
            "validation": field["sourceDefinition"], "status": field["mappingStatus"],
        })
    return rows


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--source", type=Path, default=DEFAULT_SOURCE)
    parser.add_argument("--output", type=Path, default=DEFAULT_OUTPUT)
    parser.add_argument("--original", type=Path, default=DATABASE_DIR / "original")
    args = parser.parse_args()

    output = require_output_boundary(args.output)
    output.mkdir(parents=True, exist_ok=True)
    source_files = {name: args.source / "{}.json".format(name) for name in ("gamelist", "gamecreate", "roomcost", "gamehelp")}
    for path in source_files.values():
        if not path.is_file():
            raise FileNotFoundError(path)

    source_objects = {name: load_json(path) for name, path in source_files.items()}
    catalog, catalog_by_code, issues = normalize_catalog(source_objects["gamelist"])
    ui, ui_issues = normalize_ui(source_objects["gamecreate"], catalog_by_code)
    costs, cost_issues = normalize_room_cost(source_objects["roomcost"], catalog_by_code)
    helps, help_issues = normalize_help(source_objects["gamehelp"], catalog_by_code)
    issues.extend(ui_issues + cost_issues + help_issues)

    outputs = {}
    for filename, records in (
        ("game_catalog.jsonl", catalog),
        ("create_ui_options.jsonl", ui),
        ("room_cost_policies.jsonl", costs),
        ("game_help_blocks.jsonl", helps),
        ("data_quality_issues.jsonl", sorted(issues, key=lambda item: canonical_json(item))),
    ):
        count, file_hash = write_jsonl(output / filename, records)
        outputs[filename] = {"recordCount": count, "sha256": file_hash}

    ddl_fields = []
    original_sources = []
    for sql_path in sorted(args.original.glob("*.sql")):
        fields = list(extract_ddl_fields(sql_path))
        ddl_fields.extend(fields)
        original_sources.append({"path": str(sql_path.relative_to(SERVER_ROOT)), "sha256": digest_file(sql_path), "fieldCount": len(fields)})
    count, file_hash = write_jsonl(output / "legacy_ddl_dictionary.jsonl", ddl_fields)
    outputs["legacy_ddl_dictionary.jsonl"] = {"recordCount": count, "sha256": file_hash}

    mapping_rows = build_field_mapping_rows(source_objects, ddl_fields)
    mapping_path = output / "legacy_field_mapping.csv"
    fieldnames = ["domain", "sourceSystem", "sourceObject", "sourceField", "target", "transform", "nullPolicy", "validation", "status"]
    with mapping_path.open("w", encoding="utf-8", newline="") as stream:
        writer = csv.DictWriter(stream, fieldnames=fieldnames)
        writer.writeheader()
        writer.writerows(mapping_rows)
    outputs[mapping_path.name] = {"recordCount": len(mapping_rows), "sha256": digest_file(mapping_path)}

    catalog_codes = set(catalog_by_code)
    coverage = {
        "catalogGameCount": len(catalog),
        "legacyEnabledCount": sum(1 for row in catalog if row["sourceEnabled"]),
        "canonicalRegionCount": len({row["regionCode"] for row in catalog if row["regionCode"]}),
        "missingRegionCount": sum(1 for row in catalog if not row["regionCode"]),
        "uiRowCount": len(ui),
        "uiGameCount": len({row["gameCode"] for row in ui}),
        "uiGamesMissingCatalog": sorted({row["gameCode"] for row in ui if row["gameCode"] not in catalog_codes}),
        "roomCostSourceCount": len(source_objects["roomcost"]),
        "roomCostPolicyCount": len(costs),
        "roomCostGamesMissingCatalog": sorted({row["gameCode"] for row in costs if row["gameId"] is None}),
        "helpRowCount": len(helps),
        "helpGameCount": len({row["gameCode"] for row in helps}),
        "helpGamesMissingCatalog": sorted({row["gameCode"] for row in helps if row["gameCode"] not in catalog_codes}),
        "dataQualityIssueCount": len(issues),
        "allCatalogRowsDraft": all(row["targetStatus"] == "DRAFT" for row in catalog),
        "publicationBlockedUntilProviderAndComponentValidation": True,
    }
    coverage_path = output / "coverage.json"
    coverage_path.write_bytes(json.dumps(coverage, ensure_ascii=False, sort_keys=True, indent=2).encode("utf-8") + b"\n")
    outputs[coverage_path.name] = {"recordCount": 1, "sha256": digest_file(coverage_path)}

    manifest = {
        "schemaVersion": 1,
        "conversionPolicy": "lossless staging; no automatic publication",
        "source": [
            {"path": str(path.relative_to(SERVER_ROOT)), "sha256": digest_file(path), "recordCount": len(source_objects[name])}
            for name, path in sorted(source_files.items())
        ] + original_sources,
        "outputs": outputs,
        "coverage": coverage,
    }
    manifest["manifestContentHash"] = digest_value(manifest)
    manifest_path = output / "manifest.json"
    manifest_path.write_bytes(json.dumps(manifest, ensure_ascii=False, sort_keys=True, indent=2).encode("utf-8") + b"\n")
    print(json.dumps({"output": str(output), "manifestSha256": digest_file(manifest_path), "coverage": coverage}, ensure_ascii=False, sort_keys=True))
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
