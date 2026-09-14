#!/usr/bin/env python3
import argparse
import json
import os
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "work" / "python"))

from pymongo import MongoClient


def main() -> int:
    parser = argparse.ArgumentParser(description="Verify current and legacy account IDs never collide")
    parser.add_argument("--output", required=True)
    parser.add_argument("--mongo-uri", default=os.getenv("AOO_MONGODB_URI", "mongodb://127.0.0.1:27017"))
    args = parser.parse_args()

    client = MongoClient(args.mongo_uri, serverSelectionTimeoutMS=5000)
    current = {int(value) for value in client.clark_mongodb.DbTagAccountInfo.distinct("_id")}
    legacy = {int(value) for value in client.Account01_aoo.tagaccountinfos.distinct("_id")}
    sequence_doc = client.clark_mongodb.DbTagDBDataKey.find_one({"_id": 1}) or {}
    legacy_sequence_doc = client.Account01_aoo.tagdbdatakeys.find_one({"_id": 1}) or {}
    sequence = int(sequence_doc.get("AccountID", 0))
    legacy_sequence = int(legacy_sequence_doc.get("AccountID", 0))
    overlap = sorted(current & legacy)
    failures = []
    if overlap:
        failures.append(f"current and legacy account IDs overlap: {overlap[:20]}")
    if sequence < max(current, default=0):
        failures.append("current sequence is behind the largest current account ID")
    if sequence <= max(legacy_sequence, max(legacy, default=0)):
        failures.append("current sequence has not advanced beyond the legacy account ceiling")

    result = {
        "status": "PASSED" if not failures else "FAILED",
        "currentCount": len(current),
        "currentMin": min(current, default=None),
        "currentMax": max(current, default=None),
        "currentSequence": sequence,
        "legacyCount": len(legacy),
        "legacyMin": min(legacy, default=None),
        "legacyMax": max(legacy, default=None),
        "legacySequence": legacy_sequence,
        "overlapCount": len(overlap),
        "failures": failures,
    }
    output = Path(args.output)
    output.parent.mkdir(parents=True, exist_ok=True)
    output.write_text(json.dumps(result, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(json.dumps(result, ensure_ascii=False, indent=2))
    return 0 if not failures else 1


if __name__ == "__main__":
    raise SystemExit(main())
