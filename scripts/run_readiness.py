#!/usr/bin/env python3
"""Read-only War Machine operator-run readiness report."""

import hashlib
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


def result(name, passed, blocker_kind, resolution_kind, **evidence):
    return {"name": name, "pass": bool(passed),
            "blocker-kind": None if passed else blocker_kind,
            "resolution-kind": None if passed else resolution_kind,
            "evidence": evidence}


def readiness_instruction(checks):
    blocked = [item for item in checks if not item["pass"]]
    if not blocked:
        return "ready"
    if any(item.get("resolution-kind") == "operator-action" for item in blocked):
        return "needs-you"
    return "waiting"


def repository_state(path):
    head = run(["git", "rev-parse", "HEAD"], path)
    tree = run(["git", "rev-parse", "HEAD^{tree}"], path)
    tracked = subprocess.run(["git", "diff", "HEAD", "--"], cwd=path,
                             stdout=subprocess.PIPE, stderr=subprocess.PIPE)
    dirty = run(["git", "status", "--porcelain"], path)
    readable = head.returncode == tree.returncode == tracked.returncode == dirty.returncode == 0
    return {"head": head.stdout.strip(), "tree-sha": tree.stdout.strip(),
            "tracked-diff-sha256": hashlib.sha256(tracked.stdout).hexdigest(),
            "dirty": bool(dirty.stdout.strip()), "readable": readable}


def receipt_matches_tree(receipt, state):
    started = receipt.get("repository-basis-start") or {}
    finished = receipt.get("repository-basis-finish") or {}
    stable = receipt.get("repository-basis-stable") is True
    match = (state.get("readable") and not state.get("dirty") and stable and
             started.get("readable") and finished.get("readable") and
             started.get("dirty") is False and finished.get("dirty") is False and
             started.get("tree-sha") == state.get("tree-sha") == finished.get("tree-sha") and
             started.get("tracked-diff-sha256") == state.get("tracked-diff-sha256") ==
             finished.get("tracked-diff-sha256"))
    if not started or not finished:
        reason = "receipt-missing-tested-tree"
    elif state.get("dirty"):
        reason = "current-tree-dirty"
    elif not stable:
        reason = "tested-tree-changed-during-run"
    elif not match:
        reason = "tested-tree-differs"
    else:
        reason = None
    return bool(match), reason


def latest_suite(records, command, repo):
    equivalent = {command}
    if repo == "futon2":
        equivalent.add("cd /home/joe/code/futon2 && make ci")
    candidates = [r for r in records if r.get("cmd") in equivalent and r.get("receipt")]
    if not candidates:
        return result(repo + "-suite", False, "unverified", "self-clearing",
                      reason="no-terminal-bounded-receipt", command=command)
    record = max(candidates, key=lambda r: r["receipt"].get("finished-at", ""))
    state = repository_state(ROOT if repo == "futon2" else FUTON3)
    receipt = record["receipt"]
    fresh, freshness_reason = receipt_matches_tree(receipt, state)
    passed = receipt.get("outer-exit") == 0 and receipt.get("verdict") == "pass" and fresh
    return result(repo + "-suite", passed, "unverified", "self-clearing", receipt=record["id"],
                  verdict=receipt.get("verdict"), outer_exit=receipt.get("outer-exit"),
                  finished_at=receipt.get("finished-at"), repository=state, fresh=fresh,
                  freshness_reason=freshness_reason,
                  tested_tree=receipt.get("repository-basis-start"))


def tree_control():
    empty = hashlib.sha256(b"").hexdigest()
    state = {"readable": True, "dirty": False, "tree-sha": "tree-current",
             "tracked-diff-sha256": empty}
    basis = {"readable": True, "dirty": False, "tracked-dirty": False,
             "tree-sha": "tree-current", "tracked-diff-sha256": empty}
    old_same = {"finished-at": "2000-01-01T00:00:00+00:00",
                "repository-basis-start": basis, "repository-basis-finish": basis,
                "repository-basis-stable": True}
    recent_other = dict(old_same)
    other = dict(basis, **{"tree-sha": "tree-other"})
    recent_other.update({"finished-at": "2999-01-01T00:00:00+00:00",
                         "repository-basis-start": other,
                         "repository-basis-finish": other})
    old_ok, _ = receipt_matches_tree(old_same, state)
    other_ok, other_reason = receipt_matches_tree(recent_other, state)
    report = {"old-identical-tree-accepted": old_ok,
              "recent-different-tree-rejected": not other_ok,
              "different-tree-reason": other_reason}
    print(json.dumps(report, indent=2, sort_keys=True))
    passed = old_ok and not other_ok and other_reason == "tested-tree-differs"
    print("run-readiness-tree-control:", "PASS" if passed else "FAIL",
          "exit-convention=0-pass/2-control-slipped")
    return 0 if passed else 2


def resolution_control():
    checks = [result("dirty-tree", False, "unverified", "self-clearing"),
              result("missing-reviewer", False, "unavailable", "operator-action")]
    instruction = readiness_instruction(checks)
    passed = instruction == "needs-you"
    print(json.dumps({"checks": checks, "instruction": instruction}, indent=2, sort_keys=True))
    print("run-readiness-resolution-control:", "PASS" if passed else "FAIL",
          "operator action remains loud beside wait blockers",
          "exit-convention=0-pass/2-control-slipped")
    return 0 if passed else 2


def roster_readiness(negative=False):
    try:
        with urllib.request.urlopen(ROSTER, timeout=3) as response:
            body = json.load(response)
        agents = body.get("agents", {})
    except Exception as error:
        return result("reviewer-availability", False, "unavailable", "operator-action",
                      reason="roster-unavailable", error=str(error)), None
    def available(name):
        entry = agents.get(name) or {}
        return entry.get("status") == "idle" and entry.get("invoke-ready?") is True
    default = "codex-7"
    preferred = ["codex-1", "codex-8", "codex-12", "codex-20"]
    chosen = next((name for name in preferred if available(name)), None)
    if negative:
        chosen = None
    return result("reviewer-availability", chosen is not None, "unavailable", "operator-action",
                  roster_reachable=True, default_reviewer=default,
                  default_present=default in agents, default_available=available(default),
                  selected=chosen, candidates={name: available(name) for name in preferred},
                  negative_control=negative), chosen


def main():
    if "--tree-control" in sys.argv[1:]:
        return tree_control()
    if "--resolution-control" in sys.argv[1:]:
        return resolution_control()
    negative = "--negative-reviewer" in sys.argv[1:]
    checks = []

    reviewer, chosen = roster_readiness(negative)
    checks.append(reviewer)

    gate = run(["bb", "-cp", ".", "checks/wm_workspace_gate.clj"])
    checks.append(result("workspace-gate", gate.returncode == 0, "unverified", "self-clearing", exit=gate.returncode,
                         summary=next((line for line in gate.stdout.splitlines()
                                       if line.startswith("wm-workspace-gate: SUMMARY")), None)))

    authority = run(["bb", "checks/contract_authority_current.clj"])
    checks.append(result("contract-freshness", authority.returncode == 0, "unverified", "self-clearing", exit=authority.returncode,
                         summary=authority.stdout.strip().splitlines()[-1] if authority.stdout.strip() else None))

    schema = run(["clojure", "-M", "-m", "checks.trace-schema-compatibility"])
    checks.append(result("trace-schema-v20-readback", schema.returncode == 0, "unverified", "self-clearing", exit=schema.returncode,
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
        checks.append(result("bounded-service-capacity", capacity, "unavailable", "self-clearing",
                             active=active, admission_max=config["admission-max"],
                             tasks_max=config["tasks-max"], slice_tasks_max=config["slice-tasks-max"],
                             configuration_hash=config["configuration-hash"]))
    except (json.JSONDecodeError, KeyError, TypeError):
        checks.append(result("bounded-service-capacity", False, "unavailable", "self-clearing", reason="health-unreadable",
                             exit=health_result.returncode))

    dry = run(["make", "-n", "certify-run", "RUN_ID=readiness-probe"])
    checks.append(result("certify-run-command", dry.returncode == 0, "unverified", "self-clearing", exit=dry.returncode,
                         command="make certify-run RUN_ID=<uuid-from-TickRunRecord>"))

    ready = all(item["pass"] for item in checks)
    instruction = readiness_instruction(checks)
    command = (f"clojure -M:wm-full-loop once --reviewer {chosen}" if chosen else None)
    report = {"readiness": "READY" if ready else "NOT-READY", "instruction": instruction,
              "message": ("run the printed command" if ready else
                          "operator action required, then re-run" if instruction == "needs-you" else
                          "wait for in-flight work to settle, then re-run"), "checks": checks,
              "operator-command": command, "side-effects": "none; no tick and no dispatch"}
    print(json.dumps(report, indent=2, sort_keys=True))
    print("run-readiness:", report["readiness"], "(" + instruction + ")",
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
