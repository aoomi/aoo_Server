#!/usr/bin/env python3
"""Loopback, throttled first-entry bundle download measurement."""
import functools, http.server, json, shutil, tempfile, threading, time, tracemalloc, urllib.request
from pathlib import Path

ROOT=Path(__file__).resolve().parents[1];BUILD=ROOT.parent/"Client/build/web-desktop";OUT=ROOT/"docs/generated/large09-download-peak.json"
RATE=2*1024*1024;BUNDLES=("common-prefab","lobby-prefab")
class Handler(http.server.SimpleHTTPRequestHandler):
    def log_message(self,*_): pass
    def copyfile(self,source,output):
        while True:
            chunk=source.read(64*1024)
            if not chunk:return
            output.write(chunk);output.flush();time.sleep(len(chunk)/RATE)

def main():
    if not BUILD.is_dir(): raise SystemExit("Client web build missing")
    server=http.server.ThreadingHTTPServer(("127.0.0.1",0),functools.partial(Handler,directory=str(BUILD)))
    thread=threading.Thread(target=server.serve_forever,daemon=True);thread.start();started=time.monotonic();tracemalloc.start();downloaded=0;files=0
    with tempfile.TemporaryDirectory(prefix="aoo-download-peak-") as temp:
        destination=Path(temp)
        for bundle in BUNDLES:
            source=BUILD/"assets"/bundle
            for path in sorted(source.rglob("*")):
                if not path.is_file():continue
                relative=path.relative_to(BUILD);target=destination/relative;target.parent.mkdir(parents=True,exist_ok=True)
                with urllib.request.urlopen(f"http://127.0.0.1:{server.server_port}/{relative.as_posix()}",timeout=30) as response,target.open("wb") as output:
                    while True:
                        chunk=response.read(64*1024)
                        if not chunk:break
                        output.write(chunk);downloaded+=len(chunk)
                files+=1
        disk_bytes=sum(path.stat().st_size for path in destination.rglob("*") if path.is_file())
    _current,peak=tracemalloc.get_traced_memory();tracemalloc.stop();elapsed=time.monotonic()-started;server.shutdown();server.server_close()
    checks={"actualHttpTransfer":downloaded>0,"contentIntegrity":disk_bytes==downloaded,"streamingMemoryPeakUnder32MiB":peak<=32*1024*1024,"timeoutUnder30Seconds":elapsed<=30,"cdnCacheHeadersVerified":False,"retryAndResumeVerified":False,"diskCapacityPreflight":False}
    report={"schemaVersion":1,"task":"LARGE09","passed":all(checks.values()),"scenario":{"bundles":BUNDLES,"perConnectionBytesPerSecond":RATE,"transport":"loopback HTTP throttled response","files":files},"metrics":{"downloadedBytes":downloaded,"diskBytes":disk_bytes,"elapsedSeconds":round(elapsed,3),"peakPythonBytes":peak,"averageBytesPerSecond":round(downloaded/elapsed)},"checks":checks,"blockers":["No CDN deployment/cache-header endpoint is configured","No resumable range/retry test or pre-download disk capacity contract exists"]}
    OUT.write_text(json.dumps(report,ensure_ascii=False,indent=2)+"\n");print(f"LARGE09 FAIL bytes={downloaded} seconds={elapsed:.3f}");return 1
if __name__=="__main__":raise SystemExit(main())
