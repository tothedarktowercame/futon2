#!/usr/bin/env python3
"""Run the workspace gate through bounded-test admission and report its receipt."""
import argparse
import json
import os
from pathlib import Path
import re
import shlex
import subprocess
import sys
import time

ROOT = Path(__file__).resolve().parents[1]
DEFAULT_BG = Path("/home/joe/code/futon3c/scripts/bg.py")


def call(bg, args):
    proc = subprocess.run([sys.executable, str(bg), *args],
                          text=True, capture_output=True)
    if proc.returncode:
        raise RuntimeError(proc.stderr.strip() or f"bg.py exit {proc.returncode}")
    try:
        return json.loads(proc.stdout)
    except json.JSONDecodeError as exc:
        raise RuntimeError(f"bg.py returned non-JSON: {proc.stdout!r}") from exc


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--bg", type=Path,
                    default=Path(os.environ.get("FUTON_BG", DEFAULT_BG)))
    ap.add_argument("--command", default="bb -cp . checks/wm_workspace_gate.clj")
    ap.add_argument("--agent", default=os.environ.get("WM_GATE_AGENT", "workspace-gate"))
    ap.add_argument("--label", default="workspace-gate")
    ap.add_argument("--poll-seconds", type=float, default=2.0)
    ap.add_argument("--admission-timeout", type=float, default=2700.0)
    args = ap.parse_args()
    fence_id = os.environ.get("FUTON_WRITER_FENCE_ID")
    fence_evidence = os.environ.get("FUTON_WRITER_FENCE_EVIDENCE")
    if fence_id and not re.fullmatch(r"[A-Za-z0-9._:-]+", fence_id):
        print("workspace-gate-bounded: INVALID_WRITER_FENCE_ID", file=sys.stderr)
        return 125
    command = args.command
    if bool(fence_id) != bool(fence_evidence):
        print("workspace-gate-bounded: WRITER_FENCE_ID_AND_EVIDENCE_REQUIRED_TOGETHER",
              file=sys.stderr)
        return 125
    if fence_id:
        # bg.py/systemd-run does not propagate this submitting shell's local
        # environment. Bind the reviewed identifier into the bounded command.
        command = ("env FUTON_WRITER_FENCE_ID=" + shlex.quote(fence_id)
                   + " FUTON_WRITER_FENCE_EVIDENCE=" + shlex.quote(fence_evidence)
                   + " " + command)
    launch = ["launch-test", command, "--agent", args.agent,
              "--label", args.label, "--dir", str(ROOT), "--window", "measurement"]
    deadline = time.monotonic() + args.admission_timeout
    queued_reported = False
    try:
        while True:
            result = call(args.bg, launch)
            if result.get("ok"):
                job = result["value"]
                break
            if result.get("reason") != "admission-cap":
                print("workspace-gate-bounded: SUBMISSION_FAILED " +
                      json.dumps(result, sort_keys=True), file=sys.stderr)
                return 125
            if not queued_reported:
                print("workspace-gate-bounded: QUEUED " +
                      json.dumps({"active": result.get("active"),
                                  "admission-max": result.get("admission-max")},
                                 sort_keys=True), flush=True)
                queued_reported = True
            if time.monotonic() >= deadline:
                print("workspace-gate-bounded: ADMISSION_TIMEOUT (no verdict)",
                      file=sys.stderr)
                return 124
            time.sleep(args.poll_seconds)

        job_id = job["id"]
        print(f"workspace-gate-bounded: ADMITTED id={job_id}", flush=True)
        while True:
            status = call(args.bg, ["test-status", job_id])
            receipt = status.get("receipt") if status else None
            if receipt:
                output = Path(status["output-file"])
                if output.exists():
                    print(output.read_text(), end="")
                print("workspace-gate-bounded: RECEIPT " +
                      json.dumps({"id": job_id,
                                  "inner-exit": receipt.get("inner-exit"),
                                  "outer-exit": receipt.get("outer-exit"),
                                  "verdict": receipt.get("verdict"),
                                  "reason": receipt.get("reason"),
                                  "resource-status": receipt.get("resource-status"),
                                  "repository-basis-start": receipt.get("repository-basis-start"),
                                  "repository-basis-finish": receipt.get("repository-basis-finish"),
                                  "repository-basis-stable": receipt.get("repository-basis-stable"),
                                  "receipt-file": status.get("receipt-file"),
                                  "resource-receipt": status.get("certificate-resource-file")},
                                 sort_keys=True))
                if receipt.get("outer-exit") != 0:
                    return int(receipt.get("outer-exit", 125))
                return int(receipt.get("inner-exit", 125))
            time.sleep(args.poll_seconds)
    except KeyboardInterrupt:
        print("workspace-gate-bounded: INTERRUPTED exit=130 (no verdict)",
              file=sys.stderr)
        return 130
    except (OSError, RuntimeError) as exc:
        print(f"workspace-gate-bounded: UNAVAILABLE: {exc}", file=sys.stderr)
        return 125


if __name__ == "__main__":
    raise SystemExit(main())
