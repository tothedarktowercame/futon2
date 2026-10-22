#!/usr/bin/env python3
"""Freeze a completed Agency job and its result; re-read without redispatch.

Usage: wm_freeze_agent_result.py ROOT PHASE RESULT PREDECESSOR
Paths RESULT and PREDECESSOR are relative to ROOT. ROOT/PHASE-dispatch.json
must contain the accepted job ID. Run with one writer per root.
This captures evidence, not a semantic acceptance or production close.
"""
import argparse
import hashlib
import json
from pathlib import Path
import urllib.request


def digest(raw):
    return hashlib.sha256(raw).hexdigest()


def encoded(value):
    return (json.dumps(value, sort_keys=True, indent=2) + "\n").encode()


def write_once(path, raw):
    if path.exists():
        if path.read_bytes() != raw:
            raise ValueError(f"Saved artifact changed: {path}")
        return
    with path.open("xb") as out:
        out.write(raw)


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("root", type=Path)
    parser.add_argument("phase")
    parser.add_argument("result")
    parser.add_argument("predecessor")
    args = parser.parse_args()
    root = args.root
    snapshot = root / f"{args.phase}-snapshot.json"
    if snapshot.exists():
        record = json.loads(snapshot.read_bytes())
        for item in record["artifacts"]:
            if digest((root / item["path"]).read_bytes()) != item["sha256"]:
                raise ValueError(f"Saved input changed: {item['path']}")
        print(json.dumps({"status": "restored", "snapshot": str(snapshot),
                          "sha256": digest(snapshot.read_bytes())}))
        return
    dispatch_path = root / f"{args.phase}-dispatch.json"
    dispatch_raw = dispatch_path.read_bytes()
    job_id = json.loads(dispatch_raw)["job-id"]
    # Reconnect by recorded job ID. This tool has no dispatch endpoint.
    url = f"http://localhost:7070/api/alpha/invoke/jobs/{job_id}"
    with urllib.request.urlopen(url, timeout=20) as response:
        job_raw = response.read()
    job = json.loads(job_raw)["job"]
    if job.get("state") != "done" or job.get("job-id") != job_id:
        raise ValueError(f"Job not completed: {job_id} state={job.get('state')}")
    result_path = root / args.result
    result_raw = result_path.read_bytes()
    json.loads(result_raw)  # Result is structured, not a partial JSON write.
    job_path = root / f"{args.phase}-terminal.json"
    write_once(job_path, job_raw)
    paths = [dispatch_path, result_path, job_path, root / args.predecessor]
    record = {"schema": "wm/frozen-agent-result-v1", "phase": args.phase,
              "job_id": job_id, "agent": job.get("agent-id"),
              "semantically_accepted": False,
              "artifacts": [{"path": str(p.relative_to(root)),
                             "sha256": digest(p.read_bytes())} for p in paths]}
    if result_path.read_bytes() != result_raw:
        raise ValueError("Result changed while being captured")
    write_once(snapshot, encoded(record))
    print(json.dumps({"status": "captured", "snapshot": str(snapshot),
                      "sha256": digest(snapshot.read_bytes())}))


if __name__ == "__main__":
    main()
