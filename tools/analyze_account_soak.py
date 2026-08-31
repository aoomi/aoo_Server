#!/usr/bin/env python3
"""Strict acceptance gate for account-server soak metrics."""
import argparse, csv, json, statistics
from pathlib import Path

parser=argparse.ArgumentParser()
parser.add_argument("metrics",type=Path)
parser.add_argument("--target-seconds",type=int,default=86400)
parser.add_argument("--sample-seconds",type=int,default=60)
parser.add_argument("--max-rss-growth-percent",type=float,default=25.0)
parser.add_argument("--max-thread-growth",type=int,default=20)
parser.add_argument("--max-native-growth-percent",type=float,default=25.0)
parser.add_argument("--max-open-file-growth",type=int,default=100)
parser.add_argument("--output",type=Path)
args=parser.parse_args()

with args.metrics.open(newline="",encoding="utf-8") as source:
    rows=list(csv.DictReader(source))
if len(rows)<2: raise SystemExit("FAILED: insufficient soak samples")

elapsed=[int(row["elapsed_seconds"]) for row in rows]
rss=[int(row["rss_kib"]) for row in rows]
threads=[int(row["threads"]) for row in rows]
native=[int(row.get("native_committed_kib",0) or 0) for row in rows]
open_files=[int(row.get("open_files",0) or 0) for row in rows]
failures=[]
if elapsed[-1]<args.target_seconds: failures.append(f"duration {elapsed[-1]} < {args.target_seconds}")
max_gap=max(b-a for a,b in zip(elapsed,elapsed[1:]))
if max_gap>args.sample_seconds*2+5: failures.append(f"sample gap {max_gap}s")
for index,row in enumerate(rows,2):
    if row["pid_alive"]!="1" or row["port_open"]!="1" or row["probe_ok"]!="1":
        failures.append(f"runtime failure at csv line {index}")
    if row.get("dependency_ports_ok","1")!="1": failures.append(f"dependency failure at csv line {index}")
    status=int(row["http_status"])
    if status and not 200<=status<400: failures.append(f"HTTP {status} at csv line {index}")

window=max(3,len(rows)//10)
rss_start=statistics.median(rss[:window]); rss_end=statistics.median(rss[-window:])
rss_growth=(rss_end-rss_start)*100.0/max(rss_start,1)
thread_growth=int(statistics.median(threads[-window:])-statistics.median(threads[:window]))
native_start=statistics.median(native[:window]); native_end=statistics.median(native[-window:])
native_growth=(native_end-native_start)*100.0/max(native_start,1)
open_file_growth=int(statistics.median(open_files[-window:])-statistics.median(open_files[:window]))
if rss_growth>args.max_rss_growth_percent: failures.append(f"RSS growth {rss_growth:.2f}%")
if thread_growth>args.max_thread_growth: failures.append(f"thread growth {thread_growth}")
if native_start and native_growth>args.max_native_growth_percent: failures.append(f"native memory growth {native_growth:.2f}%")
if open_file_growth>args.max_open_file_growth: failures.append(f"open file growth {open_file_growth}")

result={"status":"FAILED" if failures else "PASSED","samples":len(rows),
        "elapsedSeconds":elapsed[-1],"maxSampleGapSeconds":max_gap,
        "rssStartKiB":rss_start,"rssEndKiB":rss_end,"rssGrowthPercent":round(rss_growth,2),
        "nativeStartKiB":native_start,"nativeEndKiB":native_end,
        "nativeGrowthPercent":round(native_growth,2),"threadGrowth":thread_growth,
        "openFileGrowth":open_file_growth,"failures":failures}
rendered=json.dumps(result,ensure_ascii=False,indent=2)
if args.output:
    args.output.parent.mkdir(parents=True,exist_ok=True); args.output.write_text(rendered+"\n",encoding="utf-8")
print(rendered)
raise SystemExit(1 if failures else 0)
