#!/usr/bin/env python3
"""Read-only War Machine operator-run readiness report."""

import datetime
import json
import os
import subprocess
import sys
import urllib.request

ROOT = "/home/joe/code/futon2"
FUTON3 = "/home/joe/code/futon3"
BG = "/home/joe/code/futon3c/scripts/bg.py"
ROSTER = "http://localhost:7070/api/alpha/agents"


def run(argv, cwd=ROOT):
    return subprocess.run(argv, cwd=cwd, text=True, capture_output=True)


def result(name, passed, **evidence):
    return {"name": name, "pass": bool(passed), "evidence": evidence}


def instant(value):
    return datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))


def repository_state(path):
    head = run(["git", "rev-parse", "HEAD"], path)
    committed = run(["git", "show", "-s", "--format=%cI", "HEAD"], path)
    dirty = run(["git", "status", "--porcelain", "--untracked-files=no"], path)
    return {"head": head.stdout.strip(), "committed-at": committed.stdout.strip(),
            "tracked-dirty": bool(dirty.stdout.strip()),
            "readable": head.returncode == committed.returncode == dirty.returncode == 0}


def latest_suite(records, command, repo):
    candidates = [r for r in records if r.get("cmd") == command and r.get("receipt")]
    if not candidates:
        return result(repo + "-suite", False, reason="no-terminal-bounded-receipt", command=command)
    record = max(candidates, key=lambda r: r["receipt"].get("finished-at", ""))
    state = repository_state(ROOT if repo == "futon2" else FUTON3)
    receipt = record["receipt"]
    fresh = (state["readable"] and not state["tracked-dirty"] and
             instant(receipt["finished-at"]) >= instant(state["committed-at"]))
    passed = receipt.get("outer-exit") == 0 and receipt.get("verdict") == "pass" and fresh
    return result(repo + "-suite", passed, receipt=record["id"],
                  verdict=receipt.get("verdict"), outer_exit=receipt.get("outer-exit"),
                  finished_at=receipt.get("finished-at"), repository=state, fresh=fresh)


def roster_readiness(negative=False):
    try:
        with urllib.request.urlopen(ROSTER, timeout=3) as response:
            body = json.load(response)
        agents = body.get("agents", {})
    except Exception as error:
        return result("reviewer-availability", False, reason="roster-unavailable", error=str(error)), None
    def available(name):
        entry = agents.get(name) or {}
        return entry.get("status") == "idle" and entry.get("invoke-ready?") is True
    default = "codex-7"
    preferred = ["codex-1", "codex-8", "codex-12", "codex-20"]
    chosen = next((name for name in preferred if available(name)), None)
    if negative:
        chosen = None
    return result("reviewer-availability", chosen is not None,
                  roster_reachable=True, default_reviewer=default,
                  default_present=default in agents, default_available=available(default),
                  selected=chosen, candidates={name: available(name) for name in preferred},
                  negative_control=negative), chosen


def main():
    negative = "--negative-reviewer" in sys.argv[1:]
    checks = []

    reviewer, chosen = roster_readiness(negative)
    checks.append(reviewer)

    gate = run(["bb", "-cp", ".", "checks/wm_workspace_gate.clj"])
    checks.append(result("workspace-gate", gate.returncode == 0, exit=gate.returncode,
                         summary=next((line for line in gate.stdout.splitlines()
                                       if line.startswith("wm-workspace-gate: SUMMARY")), None)))

    authority = run(["bb", "checks/contract_authority_current.clj"])
    checks.append(result("contract-freshness", authority.returncode == 0, exit=authority.returncode,
                         summary=authority.stdout.strip().splitlines()[-1] if authority.stdout.strip() else None))

    schema = run(["clojure", "-M", "-m", "checks.trace-schema-compatibility"])
    checks.append(result("trace-schema-v20-readback", schema.returncode == 0, exit=schema.returncode,
                         summary=schema.stdout.strip().splitlines()[-1] if schema.stdout.strip() else None))

    listed = run(["python3", BG, "test-list"])
    try:
        records = json.loads(listed.stdout)
    except json.JSONDecodeError:
        records = []
    checks.append(latest_suite(records, "clojure -T:build ci", "futon2"))
    checks.append(latest_suite(records, "clojure -X:test", "futon3"))

    health_result = run(["python3", BG, "test-health"])
    try:
        health = json.loads(health_result.stdout)
        config = health["current-configuration"]
        active = sum((r.get("systemd", {}).get("ActiveState") in ("active", "activating"))
                     for r in records)
        capacity = active < config["admission-max"]
        checks.append(result("bounded-service-capacity", capacity,
                             active=active, admission_max=config["admission-max"],
                             tasks_max=config["tasks-max"], slice_tasks_max=config["slice-tasks-max"],
                             configuration_hash=config["configuration-hash"]))
    except (json.JSONDecodeError, KeyError, TypeError):
        checks.append(result("bounded-service-capacity", False, reason="health-unreadable",
                             exit=health_result.returncode))

    dry = run(["make", "-n", "certify-run", "RUN_ID=readiness-probe"])
    checks.append(result("certify-run-command", dry.returncode == 0, exit=dry.returncode,
                         command="make certify-run RUN_ID=<uuid-from-TickRunRecord>"))

    ready = all(item["pass"] for item in checks)
    command = (f"clojure -M:wm-full-loop once --reviewer {chosen}" if chosen else None)
    report = {"readiness": "READY" if ready else "NOT-READY", "checks": checks,
              "operator-command": command, "side-effects": "none; no tick and no dispatch"}
    print(json.dumps(report, indent=2, sort_keys=True))
    print("run-readiness:", report["readiness"],
          "exit-convention=0-ready/1-not-ready/2-negative-control-slipped")
    if negative:
        reviewer_failed = not next(c for c in checks if c["name"] == "reviewer-availability")["pass"]
        if not ready and reviewer_failed:
            print("run-readiness: PASS negative reviewer mutation rejected")
            return 0
        print("run-readiness: FAIL negative reviewer mutation slipped")
        return 2
    return 0 if ready else 1


if __name__ == "__main__":
    sys.exit(main())
