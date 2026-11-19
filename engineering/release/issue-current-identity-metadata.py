#!/usr/bin/env python3
"""Current32 source issuance only: explicit files, no database or activation."""
import argparse
import copy
import hashlib
import json
import os
from pathlib import Path
import runpy
import sys
from datetime import datetime, timezone
import zipfile

sys.dont_write_bytecode = True


class IssuanceRejected(Exception):
    pass


def require(condition, code):
    if not condition:
        raise IssuanceRejected(code)


def sha(raw):
    return hashlib.sha256(raw).hexdigest()


def serialize(value):
    return (json.dumps(value, ensure_ascii=False, sort_keys=True, indent=2) + "\n").encode()


def contained(root, relative):
    logical_path(relative)
    path = (root / relative).resolve()
    require(path.is_relative_to(root.resolve()), "PATH_OUTSIDE_PROJECT")
    return path


def logical_path(value):
    require(isinstance(value, str) and value and "\0" not in value and
            not Path(value).is_absolute() and ".." not in Path(value).parts,
            "RELATIVE_PATH_REQUIRED")
    return value


def read_ref(root, ref):
    require(set(ref) == {"path", "sha256"}, "INPUT_REFERENCE_FIELDS")
    raw = contained(root, ref["path"]).read_bytes()
    require(sha(raw) == ref["sha256"], "INPUT_SHA_MISMATCH")
    return json.loads(raw)


def current32(document, key):
    rows = document[key]
    require(isinstance(rows, list) and len(rows) == 32, "CURRENT32_COUNT")
    require(all(isinstance(r, dict) and type(r.get("gameId")) is int and r["gameId"] > 0 for r in rows), "CURRENT32_GAME_ID_TYPE")
    require(len({r["gameId"] for r in rows}) == 32, "CURRENT32_DUPLICATE_GAME_ID")
    return {r["gameId"]: r for r in rows}


def derive(root, packet, now):
    require(packet["mode"] in {"PREPARE_CURRENT32", "ISSUE_CURRENT32"}, "EXPLICIT_MODE_REQUIRED")
    require(packet["contractVersion"] == "common-room-v1", "RULE_CONTRACT_VERSION_DRIFT")
    refs = packet["materials"]
    implementation = packet["hashImplementation"]
    require(implementation["path"] == "server/database/bootstrap/initialize_empty_database.py", "HASH_IMPLEMENTATION_PATH")
    implementation_path = contained(root, implementation["path"])
    require(sha(implementation_path.read_bytes()) == implementation["sha256"], "HASH_IMPLEMENTATION_SHA")
    # Import only the frozen module definitions; never invoke its database CLI.
    functions = runpy.run_path(str(implementation_path), run_name="aoo_current32_hash_contract")
    hash_json = functions["hash_json"]
    baseline = read_ref(root, refs["baseline"])
    require(baseline["manifestKind"] == "runtime-metadata-canonical", "BASELINE_KIND")
    require(baseline["manifestStatus"] == "SIGNED_CANONICAL_INPUT", "BASELINE_STATUS")
    old_rows = current32(baseline, "rows")
    identity_doc = read_ref(root, refs["identities"])
    identities = current32(identity_doc, "plays")
    bindings = current32(read_ref(root, refs["bindings"]), "rows")
    require(old_rows.keys() == identities.keys() == bindings.keys(), "CURRENT32_MEMBERSHIP_DRIFT")
    for game_id, identity in identities.items():
        require(identity["gameCode"] == old_rows[game_id]["gameCode"], "BUSINESS_CODE_DRIFT")
        require(identity["regionCode"] == old_rows[game_id]["regionCode"], "REGION_DRIFT")
        require(bindings[game_id]["gameCode"] == identity["gameCode"], "BINDING_CODE_DRIFT")
    source_doc = read_ref(root, refs["sourceMap"])
    locations = source_doc["sourceLocationMap"]
    require(len({r["priorLogicalPath"] for r in locations}) == len(locations), "SOURCE_LOCATOR_DUPLICATE")
    by_old = {r["priorLogicalPath"]: r for r in locations}
    source_digests = {}
    for record in locations:
        logical_path(record["priorLogicalPath"])
        logical_path(record["logicalPath"])
        actual = contained(root, record["physicalPath"]).read_bytes()
        require(sha(actual) == record["sha256"], "COMPONENT_SOURCE_DRIFT")
        require(record["logicalPath"] not in source_digests, "CURRENT_LOGICAL_PATH_DUPLICATE")
        source_digests[record["logicalPath"]] = record["sha256"]
    workbook_receipt = read_ref(root, refs["workbooks"])["rows"]
    books = {r["path"]: r for r in workbook_receipt}
    require(len(books) == len(workbook_receipt), "WORKBOOK_RECEIPT_DUPLICATE")
    identity_indexes = {r["gameId"]: i for i, r in enumerate(identity_doc["plays"])}
    rows = []
    for game_id, old in old_rows.items():
        identity = identities[game_id]
        binding = bindings[game_id]
        require(binding["playVersion"] == identity["playVersion"] and
                binding["family"] == identity["family"] and
                binding["regionCode"] == identity["regionCode"], "BINDING_IDENTITY_DRIFT")
        records = []
        for evidence in old["providerBinding"]["implementationEvidence"]:
            logical = evidence.split("#", 1)[0]
            locator = by_old[logical]
            records.append({"path": locator["logicalPath"], "sha256": locator["sha256"]})
        artifact = binding["artifact"]
        artifact_path = contained(root, artifact["path"])
        require(sha(artifact_path.read_bytes()) == artifact["sha256"], "PROVIDER_ARTIFACT_DRIFT")
        with zipfile.ZipFile(artifact_path) as archive:
            require(binding["providerClass"].replace(".", "/") + ".class" in archive.namelist(), "PROVIDER_CLASS_NOT_IN_ARTIFACT")
            if "provider" in identity:
                native_factory = identity["provider"]
                require(isinstance(native_factory, str) and bool(native_factory.strip()), "NATIVE_AUTHORITY_FACTORY_REQUIRED")
                require(binding.get("pokerFactory") == native_factory, "FACTORY_IDENTITY_DRIFT")
                spi = archive.read("META-INF/services/com.aoo.bcg.poker.PokerFamilyProviderFactory").decode().splitlines()
                require([s.strip() for s in spi if s.strip() and not s.lstrip().startswith("#")] == [native_factory], "FACTORY_SPI_NOT_UNIQUE")
            else:
                require("pokerFactory" not in binding, "NON_NATIVE_FACTORY_NOT_AUTHORIZED")
        records.append({"path": artifact["path"], "sha256": artifact["sha256"]})
        source_digests[artifact["path"]] = artifact["sha256"]
        workbook = contained(root, identity["workbookPath"])
        require(sha(workbook.read_bytes()) == books[identity["workbookPath"]]["sha256"], "WORKBOOK_BYTES_DRIFT")
        with zipfile.ZipFile(workbook) as archive:
            members = [{"member": name, "sha256": sha(archive.read(name))}
                       for name in sorted(archive.namelist())
                       if not name.endswith("/") and name != "docProps/core.xml"]
        normalized_book = sha(json.dumps(members, ensure_ascii=False, separators=(",", ":")).encode())
        rule_hash = hash_json(identity)
        component_hash = hash_json(records)
        revision = hash_json({"components": records, "workbook": {"path": identity["workbookPath"], "normalizedHash": normalized_book}})
        source_binding = {"gameId": game_id, "gameCode": identity["gameCode"],
                          "providerRoute": binding["providerRoute"], "providerClass": binding["providerClass"],
                          "artifactVersion": binding["artifactVersion"], "ruleContentHash": rule_hash,
                          "normalizedWorkbookHash": normalized_book, "componentContentHash": component_hash,
                          "sourceRevision": revision, "contractVersion": packet["contractVersion"]}
        row = copy.deepcopy(old)
        row.update({key: identity[key] for key in ("gameCode", "playVersion", "family", "regionCode")})
        row.update({"sourceHash": hash_json(source_binding), "ruleContentHash": rule_hash,
                    "componentContentHash": component_hash, "sourceRevision": revision,
                    "artifactVersion": binding["artifactVersion"], "contractVersion": packet["contractVersion"],
                    "providerBinding": {"route": binding["providerRoute"], "providerClass": binding["providerClass"],
                                        "implementationEvidence": [r["path"] + "#sha256=" + r["sha256"] for r in records]},
                    "runtimeMetadataEvidence": binding["runtimeMetadataEvidence"],
                    "sourceArtifact": {"normalizedIdentitySource": refs["identities"]["path"] + "#plays[" + str(identity_indexes[game_id]) + "]",
                                       "normalizedComponentSource": "SHA-256 over ordered current source and artifact path+hash records",
                                       "normalizedContentHash": rule_hash,
                                       "normalizedWorkbookSource": identity["workbookPath"] + "#normalized-zip-member-sha256-excluding-docProps/core.xml",
                                       "normalizedWorkbookHash": normalized_book},
                    "issuanceReason": "Explicit current32 source issuance; no business identity or rules upgrade"})
        rows.append(row)
    signed = packet["mode"] == "ISSUE_CURRENT32"
    if signed:
        material = {k: v for k, v in packet.items() if k != "materials"}
        material["materials"] = {k: v for k, v in refs.items() if k not in {"approval", "lease"}}
        approval = read_ref(root, refs["approval"])
        require(approval["status"] == "ACCEPTED_SOURCE_ONLY" and approval["materialSha256"] == hash_json(material), "SOURCE_APPROVAL_MISMATCH")
        require(approval["issuer"] == packet["issuer"] and approval["edition"] == packet["edition"], "SOURCE_APPROVAL_OWNER_EDITION")
        literals_doc = read_ref(root, refs["literals"])
        literals = current32(literals_doc, "rows")
        require(literals.keys() == identities.keys() and literals_doc["status"] == "REGISTERED", "LITERAL_REGISTRATION")
        require(literals_doc["issuer"] == packet["issuer"] and literals_doc["edition"] == packet["edition"], "LITERAL_OWNER_EDITION")
        for field in ("releaseId", "indexGeneration"):
            values = [r[field] for r in literals.values()]
            require(all(type(v) is int and v > 0 for v in values) and len(set(values)) == 32, "LITERAL_ID_COLLISION")
        require(all(type(r["releaseVersion"]) is int and r["releaseVersion"] > 0 for r in literals.values()), "LITERAL_RELEASE_VERSION")
        preimage = read_ref(root, refs["currentPreimage"])
        current = current32(preimage, "rows")
        require(current.keys() == identities.keys(), "CURRENT_PREIMAGE_MEMBERSHIP")
        for game_id, current_row in current.items():
            require(current_row["gameCode"] == identities[game_id]["gameCode"], "CURRENT_PREIMAGE_CODE")
        preflight = read_ref(root, refs["preflight"])
        require(preflight["literalSha256"] == refs["literals"]["sha256"] and
                preflight["currentPreimageSha256"] == refs["currentPreimage"]["sha256"], "COLLISION_RECEIPT_BINDING")
        require(preflight["counts"] == [0] * 96, "COLLISION_NOT_CLEAR")
        expected_pairs = [literals[game_id] for game_id in sorted(literals)]
        require(preflight["pairs"] == expected_pairs, "COLLISION_PAIR_DRIFT")
        checked = datetime.fromisoformat(preflight["checkedAt"])
        expires = datetime.fromisoformat(preflight["expiresAt"])
        require(checked <= now < expires and (expires - checked).total_seconds() <= 7200, "COLLISION_RECEIPT_EXPIRED")
        lease = read_ref(root, refs["lease"])
        require(lease["state"] == "HELD" and lease["owner"] == packet["issuer"] and
                lease["edition"] == packet["edition"] and lease["literalSha256"] == refs["literals"]["sha256"], "ISSUANCE_LEASE_BINDING")
        acquired = datetime.fromisoformat(lease["acquiredAt"])
        expiry = datetime.fromisoformat(lease["expiresAt"])
        require(acquired <= now < expiry and (expiry - acquired).total_seconds() <= 7200, "ISSUANCE_LEASE_EXPIRED")
        for row in rows:
            values = literals[row["gameId"]]
            row["releaseId"], row["indexGeneration"] = values["releaseId"], values["indexGeneration"]
            row["admission"] = "ACCEPTED"
    else:
        for row in rows:
            row["admission"] = "PENDING_ISSUANCE"
    runtime = copy.deepcopy(baseline)
    runtime.update({"manifestStatus": "SIGNED_CANONICAL_INPUT" if signed else "UNSIGNED_PREPARATION",
                    "consumable": signed, "manifestVersion": packet["runtimeManifestVersion"],
                    "releaseIssuer": packet["issuer"], "releaseEdition": packet["edition"], "rows": rows,
                    "supersedes": {"sha256": refs["baseline"]["sha256"], "historicalReferenceOnly": True},
                    "sourceDigests": source_digests})
    runtime["sourceContract"].update({"releaseIssuer": packet["issuer"], "releaseEdition": packet["edition"]})
    runtime_sha = sha(serialize(runtime))
    evidence_doc = read_ref(root, refs["evidence"])
    evidence = []
    for item in evidence_doc["rows"]:
        logical_path(item["path"])
        require(sha(contained(root, item["physicalPath"]).read_bytes()) == item["sha256"], "CONTRACT_EVIDENCE_DRIFT")
        evidence.append({key: item[key] for key in ("path", "sha256", "assertion")})
    evidence.append({"path": logical_path(packet["runtimeManifestLogicalPath"]), "sha256": runtime_sha,
                     "assertion": "current source-backed runtime manifest"})
    digest_input = "\n".join(["contractVersion=" + packet["contractVersion"], "runtimeManifestSha=" + runtime_sha] +
                            [r["path"] + "\0" + r["sha256"] for r in evidence])
    contract = {"manifestKind": "canonical-identity-current-contract", "manifestVersion": packet["contractManifestVersion"],
                "status": "SIGNED_CANONICAL_INPUT" if signed else "UNSIGNED_PREPARATION",
                "issuer": packet["issuer"], "releaseEdition": packet["edition"], "contractVersion": packet["contractVersion"],
                "sourceHash": sha(digest_input.encode()),
                "sourceHashAlgorithm": "SHA-256(contractVersion + accepted runtime manifest digest + explicit current RoomSnapshot/Runtime evidence digests)",
                "noFallbacks": True, "noHistoricalCompatibilityPromotion": True, "evidence": evidence,
                "rows": [{key: r[key] for key in ("gameId", "gameCode", "contractVersion", "sourceHash", "releaseId", "indexGeneration")} for r in rows]}
    return {"kind": "current32-source-issuance", "activationAllowed": False, "businessIdentityChanged": False,
            "runtimeManifest": runtime, "runtimeManifestSha256": runtime_sha, "currentContract": contract,
            "currentContractSha256": sha(serialize(contract)),
            "literalRegistration": read_ref(root, refs["literals"]) if signed else None,
            "sourceDataGates": packet["sourceDataGates"], "sourceOnly": True}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--project-root", required=True)
    parser.add_argument("--input", required=True)
    parser.add_argument("--input-sha256", required=True)
    parser.add_argument("--output", required=True)
    args = parser.parse_args()
    root = Path(args.project_root).resolve()
    raw = Path(args.input).read_bytes()
    require(sha(raw) == args.input_sha256, "PACKET_SHA_MISMATCH")
    packet = json.loads(raw)
    output = Path(args.output).resolve()
    require(output.is_relative_to(root / "docs/05-当前状态/evidence") and output.parent.is_dir(), "OUTPUT_EVIDENCE_ONLY")
    require(not output.exists(), "OUTPUT_ALREADY_EXISTS")
    result = derive(root, packet, datetime.now(timezone.utc))
    # Exclusive creation: never overwrite a live manifest, old artifact or evidence.
    handle = output.open("xb")
    try:
        with handle:
            handle.write(serialize(result))
            handle.flush()
            os.fsync(handle.fileno())
    except OSError:
        output.unlink()
        raise
    print(json.dumps({"ok": True, "rows": 32, "outputSha256": sha(output.read_bytes()), "activationAllowed": False}))


if __name__ == "__main__":
    try:
        main()
    except (IssuanceRejected, OSError, ValueError, KeyError, TypeError, zipfile.BadZipFile) as error:
        print(json.dumps({"ok": False, "code": str(error) if isinstance(error, IssuanceRejected) else type(error).__name__}), file=sys.stderr)
        sys.exit(1)
