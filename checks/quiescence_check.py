#!/usr/bin/env python3
"""Composite, state-sandwiched quiet-window check.

Exit 0 = quiescent, 1 = observed non-quiescence, 3 = unavailable/moving
observation, 2 = a self-test mutation escaped.
"""

import argparse
import hashlib
import json
import subprocess
import sys


REPOS = [
    "/home/joe/code/futon2",
    "/home/joe/code/futon3c",
    "/home/joe/code/mathlib4",
    "/home/joe/code/p4ng",
    "/home/joe/code/futon3",
]
REGISTRY = "/home/joe/code/futon2/holes/labs/wm-contract/lane-registry.edn"
BG = "/home/joe/code/futon3c/scripts/bg.py"
LANES = {"wm-nouns", "wm-verbs", "wm-organization", "wm-evidence"}


def run(argv, *, stdin=None):
    return subprocess.run(argv, input=stdin, capture_output=True, text=True)


def repo_state(path):
    head = run(["git", "-C", path, "rev-parse", "HEAD"])
    status = run(["git", "-C", path, "status", "--porcelain=v1"])
    if head.returncode or status.returncode:
        raise RuntimeError("repository-unavailable: " + path)
    return {"head": head.stdout.strip(), "porcelain": status.stdout}


def edn_to_json(text):
    converted = run(
        ["bb", "-e",
         "(require '[clojure.edn :as edn] '[cheshire.core :as json]) "
         "(print (json/generate-string (edn/read-string (slurp *in*))))"],
        stdin=text,
    )
    if converted.returncode:
        raise RuntimeError("edn-conversion-failed: " + converted.stderr.strip())
    return json.loads(converted.stdout)


def lane_state():
    check = run(["bb", "checks/lane_registry_check.clj"])
    rows = []
    for line in check.stdout.splitlines():
        if line.startswith("{:lane "):
            rows.append(edn_to_json(line))
    registry_bytes = open(REGISTRY, "rb").read()
    return {
        "validator-exit": check.returncode,
        "rows": rows,
        "registry-sha256": hashlib.sha256(registry_bytes).hexdigest(),
    }


def ordinary_state():
    listed = run(["python3", BG, "list"])
    if listed.returncode:
        raise RuntimeError("ordinary-job-list-unavailable")
    parsed = edn_to_json(listed.stdout)
    if not parsed.get("ok") or not isinstance(parsed.get("value"), list):
        raise RuntimeError("ordinary-job-list-malformed")
    active = []
    for row in parsed["value"]:
        if row.get("alive?") is True or row.get("status") in ("running", "starting"):
            active.append(str(row.get("id")))
    return sorted(active)


def bounded_state():
    listed = run(["python3", BG, "test-list"])
    if listed.returncode:
        raise RuntimeError("bounded-job-list-unavailable")
    rows = json.loads(listed.stdout)
    return sorted(
        str(row.get("id")) for row in rows
        if row.get("systemd", {}).get("ActiveState") in ("active", "activating")
    )


def live_snapshot():
    return {
        "repos": {path: repo_state(path) for path in REPOS},
        "lanes": lane_state(),
        "ordinary-active": ordinary_state(),
        "bounded-active": bounded_state(),
    }


def findings(snapshot):
    found = []
    for path, state in snapshot["repos"].items():
        if state["porcelain"]:
            found.append({"condition": "clean-tree", "repo": path,
                          "detail": state["porcelain"].splitlines()})
    lanes = snapshot["lanes"]
    if lanes["validator-exit"] != 0:
        found.append({"condition": "valid-lane-registry",
                      "exit": lanes["validator-exit"]})
    reported = {str(row.get("lane", "")).removeprefix(":") for row in lanes["rows"]}
    if reported != LANES:
        found.append({"condition": "four-explicit-lanes",
                      "reported": sorted(reported)})
    for row in lanes["rows"]:
        state = str(row.get("state", "")).removeprefix(":")
        if state != "idle":
            found.append({"condition": "lane-idle", "lane": row.get("lane"),
                          "state": row.get("state"), "holding": row.get("holding")})
    if snapshot["ordinary-active"]:
        found.append({"condition": "no-active-ordinary-jobs",
                      "jobs": snapshot["ordinary-active"]})
    if snapshot["bounded-active"]:
        found.append({"condition": "no-active-bounded-jobs",
                      "jobs": snapshot["bounded-active"]})
    return found


def evaluate(first, second):
    if first != second:
        return 3, {"verdict": "UNAVAILABLE", "reason": "state-changed-during-observation",
                   "start": first, "finish": second}
    found = findings(first)
    if found:
        return 1, {"verdict": "NOT-QUIESCENT", "findings": found}
    return 0, {"verdict": "QUIESCENT", "conditions": {
        "clean-repositories": len(REPOS), "idle-lanes": 4,
        "ordinary-active": 0, "bounded-active": 0,
        "state-sandwich-stable": True}}


def clean_fixture():
    return {
        "repos": {path: {"head": "fixture-head", "porcelain": ""} for path in REPOS},
        "lanes": {"validator-exit": 0, "registry-sha256": "fixture-registry",
                  "rows": [{"lane": lane, "state": "idle", "holding": None}
                           for lane in sorted(LANES)]},
        "ordinary-active": [], "bounded-active": [],
    }


def self_test():
    base = clean_fixture()
    cases = []
    cases.append(("clean", base, base, 0))
    dirty = json.loads(json.dumps(base)); dirty["repos"][REPOS[0]]["porcelain"] = "?? x\n"
    cases.append(("dirty-tree", dirty, dirty, 1))
    holding = json.loads(json.dumps(base)); holding["lanes"]["rows"][0].update(
        {"state": "holding", "holding": "C292"})
    cases.append(("active-holding", holding, holding, 1))
    stale = json.loads(json.dumps(base)); stale["lanes"]["validator-exit"] = 1
    stale["lanes"]["rows"][0].update({"state": "stale-holding", "holding": "C-old"})
    cases.append(("stale-holding", stale, stale, 1))
    ordinary = json.loads(json.dumps(base)); ordinary["ordinary-active"] = ["bg-live"]
    cases.append(("ordinary-active", ordinary, ordinary, 1))
    bounded = json.loads(json.dumps(base)); bounded["bounded-active"] = ["bounded-live"]
    cases.append(("bounded-active", bounded, bounded, 1))
    moved = json.loads(json.dumps(base)); moved["repos"][REPOS[0]]["head"] = "later-head"
    cases.append(("state-moved", base, moved, 3))
    escaped = []
    for name, first, second, expected in cases:
        actual, report = evaluate(first, second)
        print("CONTROL", name, "expected", expected, "actual", actual, report["verdict"])
        if actual != expected:
            escaped.append(name)
    return 2 if escaped else 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    try:
        first = live_snapshot()
        second = live_snapshot()
        code, report = evaluate(first, second)
    except Exception as failure:
        code, report = 3, {"verdict": "UNAVAILABLE", "reason": str(failure)}
    print(json.dumps(report, indent=2, sort_keys=True))
    return code


if __name__ == "__main__":
    sys.exit(main())
