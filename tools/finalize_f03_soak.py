#!/usr/bin/env python3
"""Finalize F03 soak evidence by combining resource and protocol workload gates."""

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path


ROUND_RE = re.compile(r"round=(\d+) (START|PASS|FAIL)(?:\s|$)")


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("metrics", type=Path)
    parser.add_argument("workload", type=Path)
    parser.add_argument("--target-seconds", type=int, default=86400)
    parser.add_argument("--workload-interval-seconds", type=int, default=300)
    parser.add_argument("--deep-workload", type=Path)
    parser.add_argument("--deep-workload-interval-seconds", type=int, default=1800)
    parser.add_argument("--output", type=Path)
    args = parser.parse_args()

    analyzer = Path(__file__).with_name("analyze_account_soak.py")
    process = subprocess.run(
        [sys.executable, str(analyzer), str(args.metrics),
         "--target-seconds", str(args.target_seconds)],
        check=False, capture_output=True, text=True,
    )
    try:
        resource_report = json.loads(process.stdout)
    except json.JSONDecodeError:
        resource_report = {
            "status": "FAILED",
            "failures": ["resource analyzer did not return JSON"],
            "stdout": process.stdout,
            "stderr": process.stderr,
        }

    starts = set()
    passes = set()
    failures = set()
    if args.workload.exists():
        for line in args.workload.read_text(encoding="utf-8", errors="replace").splitlines():
            match = ROUND_RE.search(line)
            if not match:
                continue
            round_no = int(match.group(1))
            state = match.group(2)
            if state == "START":
                starts.add(round_no)
            elif state == "PASS":
                passes.add(round_no)
            else:
                failures.add(round_no)

    # The companion workload can be attached after the primary sampler has
    # started. Allow at most two missing intervals for startup/finalization;
    # every round that did start must still finish successfully.
    expected_minimum = max(
        1, args.target_seconds // args.workload_interval_seconds - 2
    )
    workload_failures = []
    if failures:
        workload_failures.append(f"failed workload rounds: {sorted(failures)}")
    incomplete = starts - passes
    if incomplete:
        workload_failures.append(f"incomplete workload rounds: {sorted(incomplete)}")
    if len(passes) < expected_minimum:
        workload_failures.append(
            f"workload rounds {len(passes)} below required minimum {expected_minimum}"
        )

    deep_report = None
    if args.deep_workload:
        deep_starts = set()
        deep_passes = set()
        deep_failures = set()
        if args.deep_workload.exists():
            for line in args.deep_workload.read_text(
                encoding="utf-8", errors="replace"
            ).splitlines():
                match = ROUND_RE.search(line)
                if not match:
                    continue
                round_no = int(match.group(1))
                state = match.group(2)
                if state == "START":
                    deep_starts.add(round_no)
                elif state == "PASS":
                    deep_passes.add(round_no)
                else:
                    deep_failures.add(round_no)
        deep_minimum = max(
            1,
            args.target_seconds // args.deep_workload_interval_seconds - 2,
        )
        if deep_failures:
            workload_failures.append(
                f"failed deep workload rounds: {sorted(deep_failures)}"
            )
        deep_incomplete = deep_starts - deep_passes
        if deep_incomplete:
            workload_failures.append(
                f"incomplete deep workload rounds: {sorted(deep_incomplete)}"
            )
        if len(deep_passes) < deep_minimum:
            workload_failures.append(
                f"deep workload rounds {len(deep_passes)} below required minimum {deep_minimum}"
            )
        deep_report = {
            "intervalSeconds": args.deep_workload_interval_seconds,
            "startedRounds": len(deep_starts),
            "passedRounds": len(deep_passes),
            "failedRounds": sorted(deep_failures),
            "minimumRequiredRounds": deep_minimum,
        }

    all_failures = list(resource_report.get("failures", [])) + workload_failures
    report = {
        "status": "PASSED" if not all_failures and process.returncode == 0 else "FAILED",
        "targetSeconds": args.target_seconds,
        "resource": resource_report,
        "workload": {
            "intervalSeconds": args.workload_interval_seconds,
            "startedRounds": len(starts),
            "passedRounds": len(passes),
            "failedRounds": sorted(failures),
            "minimumRequiredRounds": expected_minimum,
        },
        "deepWorkload": deep_report,
        "failures": all_failures,
    }
    rendered = json.dumps(report, ensure_ascii=False, indent=2) + "\n"
    if args.output:
        args.output.parent.mkdir(parents=True, exist_ok=True)
        args.output.write_text(rendered, encoding="utf-8")
    print(rendered, end="")
    return 0 if report["status"] == "PASSED" else 1


if __name__ == "__main__":
    raise SystemExit(main())
