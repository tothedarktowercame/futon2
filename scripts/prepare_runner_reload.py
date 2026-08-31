#!/usr/bin/env python3
"""Fail-closed preparation for Joe's serving-JVM runner reload; never reloads."""

import json
import os
import subprocess
import sys

ROOT = "/home/joe/code/futon2"
FUTON3C = "/home/joe/code/futon3c"
SOURCE = ROOT + "/src/futon2/aif/full_loop_runner.clj"
BG = FUTON3C + "/scripts/bg.py"
RELOAD_COMMAND = (
    "cd /home/joe/code/futon3c && clojure -M:dev-admin load-file " + SOURCE
)


def run(argv, cwd=ROOT):
    return subprocess.run(argv, cwd=cwd, text=True, capture_output=True)


def check(name, passed, **evidence):
    return {"name": name, "pass": bool(passed), "evidence": evidence}


def git(*args):
    return run(["git", *args])


def latest_clean_receipt(head):
    listed = run(["python3", BG, "test-list"])
    try:
        records = json.loads(listed.stdout)
    except json.JSONDecodeError:
        return None, "bounded-receipts-unreadable"
    commands = {"clojure -T:build ci", "cd /home/joe/code/futon2 && make ci"}
    candidates = [r for r in records if r.get("cmd") in commands and r.get("receipt")]
    candidates.sort(key=lambda r: r["receipt"].get("finished-at", ""), reverse=True)
    for record in candidates:
        receipt = record["receipt"]
        start = receipt.get("repository-basis-start") or {}
        finish = receipt.get("repository-basis-finish") or {}
        if (receipt.get("outer-exit") == 0 and receipt.get("verdict") == "pass" and
                receipt.get("repository-basis-stable") is True and
                start.get("head") == head == finish.get("head") and
                start.get("dirty") is False and finish.get("dirty") is False):
            return record, None
    return None, "no-clean-bounded-receipt-for-current-commit"


def main():
    checks = []
    canonical = os.path.realpath(SOURCE)
    checks.append(check("canonical-source", canonical == SOURCE and os.path.isfile(SOURCE),
                        expected=SOURCE, observed=canonical))
    branch = git("branch", "--show-current").stdout.strip()
    # This repository's canonical successor to the historical 'master' name is main.
    checks.append(check("canonical-branch", branch == "main", expected="main", observed=branch))
    head_result = git("rev-parse", "HEAD")
    head = head_result.stdout.strip()
    checks.append(check("commit-readable", head_result.returncode == 0, head=head))
    status = git("status", "--porcelain")
    clean = status.returncode == 0 and not status.stdout.strip()
    checks.append(check("repository-clean", clean,
                        findings=status.stdout.strip().splitlines()))
    probe = run(["clojure", "-M", "-e",
                 "(require 'futon2.aif.full-loop-runner) (println :runner-resolvable)"], ROOT)
    checks.append(check("namespace-resolvable", probe.returncode == 0 and
                        ":runner-resolvable" in probe.stdout,
                        exit=probe.returncode,
                        summary=(probe.stderr or probe.stdout).strip().splitlines()[-1:]))
    receipt, receipt_reason = latest_clean_receipt(head)
    checks.append(check("tested-commit", receipt is not None,
                        head=head, receipt=(receipt or {}).get("id"), reason=receipt_reason))
    ready = all(item["pass"] for item in checks)
    report = {"reload-preflight": "READY" if ready else "REFUSED",
              "checks": checks,
              "reload-command": RELOAD_COMMAND if ready else None,
              "withheld-command": None if ready else RELOAD_COMMAND,
              "side-effects": "none; serving JVM was not reloaded"}
    print(json.dumps(report, indent=2, sort_keys=True))
    print("runner-reload-preflight:", report["reload-preflight"],
          "exit-convention=0-ready/1-refused")
    return 0 if ready else 1


if __name__ == "__main__":
    sys.exit(main())
