#!/usr/bin/env python3
"""Remove retired AssetDB camera cache entries from Creator's scene profile."""
from __future__ import annotations

import json
import os
import tempfile
from pathlib import Path

from gate import RETIRED_SCENE_PROFILE_UUIDS, ROOT, SCENE_PROFILE_PATH


def sanitize(path: Path) -> int:
    data = json.loads(path.read_text(encoding="utf-8"))
    infos = data.get("camera-infos")
    uuids = data.get("camera-uuids")
    if not isinstance(infos, dict) or not isinstance(uuids, list):
        raise ValueError("unsupported Creator scene profile schema")

    removed = sum(uuid in infos for uuid in RETIRED_SCENE_PROFILE_UUIDS)
    for uuid in RETIRED_SCENE_PROFILE_UUIDS:
        infos.pop(uuid, None)
    data["camera-uuids"] = [uuid for uuid in uuids if uuid not in RETIRED_SCENE_PROFILE_UUIDS]

    if len(data["camera-uuids"]) != len(set(data["camera-uuids"])):
        raise ValueError("duplicate camera UUIDs remain")
    if set(infos) != set(data["camera-uuids"]):
        raise ValueError("camera-infos and camera-uuids are inconsistent")

    payload = json.dumps(data, ensure_ascii=False, indent=2) + "\n"
    if payload != path.read_text(encoding="utf-8"):
        fd, temporary = tempfile.mkstemp(prefix=path.name + ".", dir=path.parent)
        try:
            with os.fdopen(fd, "w", encoding="utf-8") as stream:
                stream.write(payload)
            os.replace(temporary, path)
        finally:
            if os.path.exists(temporary):
                os.unlink(temporary)
    return removed


if __name__ == "__main__":
    target = ROOT / SCENE_PROFILE_PATH
    count = sanitize(target)
    print(f"scene profile sanitized: removed={count}, path={target}")
