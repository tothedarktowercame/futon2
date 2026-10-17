#!/usr/bin/env python3
"""Read-only War Machine operator-run readiness report."""

import hashlib
import json
import os
import re
import subprocess
import sys
import urllib.request

ROOT = "/home/joe/code/futon2"
FUTON3 = "/home/joe/code/futon3"
WORKSPACE_REPOSITORIES = {
    "futon2": ROOT,
    "mathlib4": "/home/joe/code/mathlib4",
    "p4ng": "/home/joe/code/p4ng",
    "futon3": FUTON3,
    "futon3c": "/home/joe/code/futon3c",
}
BG = "/home/joe/code/futon3c/scripts/bg.py"
PROOF_EVAL = "/home/joe/code/futon3c/scripts/proof-eval.sh"
ROSTER = "http://localhost:7070/api/alpha/agents"
WARRANT_INDEX = "/home/joe/code/storage/test-registry/futon2-suite/index.json"
REQUIRED_WARRANTS = [
    "futon2.aif.cascade-shape-g-test",
    "futon2.aif.loop-node-exercise-test",
    "futon2.aif.reading-policy-preference-test",
    "futon2.aif.reading-policy-precision-test",
    "futon2.aif.full-loop-runtime-test",
    "futon2.aif.wm.family-selection-test",
    "futon2.aif.wm.reading-family-decision-test",
]
WM_CAST = {"author": "zai-2", "reviewer": "zai-6",
           "repair-reviewer": "zai-7"}


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


def parse_workspace_provenance(output_file):
    prefix = "wm-workspace-gate: PROVENANCE "
    if not output_file:
        return None
    try:
        with open(output_file, encoding="utf-8") as stream:
            line = next((line for line in stream if line.startswith(prefix)), None)
        value = json.loads(line[len(prefix):]) if line else None
        return value if isinstance(value, list) else None
    except (OSError, json.JSONDecodeError):
        return None


def workspace_basis_matches(rows, states):
    if not rows:
        return False, "receipt-missing-workspace-basis", None
    by_name = {str(row.get("repository")): row for row in rows}
    if set(by_name) != set(WORKSPACE_REPOSITORIES):
        return False, "receipt-workspace-population-differs", None
    for name in WORKSPACE_REPOSITORIES:
        row = by_name[name]
        current = states[name]
        matches = (row.get("readable?") is True and
                   current.get("readable") and
                   row.get("dirty?") == current.get("dirty") and
                   row.get("git-sha") == current.get("head") and
                   row.get("tree-sha") == current.get("tree-sha") and
                   row.get("tracked-diff-sha256") ==
                   current.get("tracked-diff-sha256"))
        if not matches:
            return False, "workspace-basis-differs", name
    return True, None, None


def latest_workspace_gate(records):
    command = "bb -cp . checks/wm_workspace_gate.clj"
    candidates = [record for record in records
                  if record.get("cmd") == command
                  and record.get("dir") == ROOT
                  and record.get("receipt")]
    if not candidates:
        return result("workspace-gate", False, "unverified", "self-clearing",
                      reason="no-terminal-bounded-receipt", command=command)
    record = max(candidates,
                 key=lambda item: item["receipt"].get("finished-at", ""))
    receipt = record["receipt"]
    futon2_state = repository_state(ROOT)
    wrapper_fresh, wrapper_reason = receipt_matches_tree(receipt, futon2_state)
    workspace_basis = parse_workspace_provenance(record.get("output-file"))
    states = {name: repository_state(path)
              for name, path in WORKSPACE_REPOSITORIES.items()}
    workspace_fresh, workspace_reason, changed_repository = \
        workspace_basis_matches(workspace_basis, states)
    passed = (receipt.get("outer-exit") == 0 and
              receipt.get("inner-exit") == 0 and
              receipt.get("verdict") == "pass" and
              receipt.get("resource-status") == "clean" and
              wrapper_fresh and workspace_fresh)
    reason = (None if passed else workspace_reason or wrapper_reason or
              "workspace-gate-verdict-failed")
    return result(
        "workspace-gate", passed, "unverified", "self-clearing",
        reason=reason, receipt=record.get("id"),
        finished_at=receipt.get("finished-at"),
        verdict=receipt.get("verdict"), inner_exit=receipt.get("inner-exit"),
        outer_exit=receipt.get("outer-exit"),
        resource_status=receipt.get("resource-status"),
        wrapper_basis_fresh=wrapper_fresh,
        wrapper_freshness_reason=wrapper_reason,
        workspace_basis_fresh=workspace_fresh,
        changed_repository=changed_repository,
        tested_workspace=workspace_basis,
        current_workspace=states)


def serving_code_observation():
    form = """(let [v (resolve 'futon3c.wm.code-identity/status)
  s (when v (v)) i (:identity s)]
  (str "WMCI|" (if v (name (:availability s)) "unavailable")
       "|" (or (:content-sha256 i) "") "|" (or (:disk-sha256 i) "")
       "|" (or (some-> i :current-status name) "") "|" (or (:path i) "")
       "|" (or (some-> s :reason name) "namespace-not-loaded")))"""
    observed = run(["bash", PROOF_EVAL, form], "/home/joe/code/futon3c")
    match = re.search(r':value "WMCI\|([^\"]*)"', observed.stdout)
    if observed.returncode != 0 or not match:
        return {"availability": "unavailable", "reason": "serving-query-failed",
                "query-exit": observed.returncode}
    fields = match.group(1).split("|")
    if len(fields) != 6:
        return {"availability": "unavailable", "reason": "serving-response-malformed"}
    availability, loaded_sha, disk_sha, current_status, path, reason = fields
    return {"availability": availability, "content-sha256": loaded_sha or None,
            "disk-sha256": disk_sha or None, "current-status": current_status or None,
            "path": path or None, "reason": reason or None}


def serving_code_result(tested_tree, observed=None):
    observed = observed or serving_code_observation()
    if observed.get("availability") != "available":
        return result("serving-runner-code", False, "unavailable", "operator-action",
                      reason=observed.get("reason"), loaded=observed)
    matched = (observed.get("current-status") == "current" and
               observed.get("content-sha256") and
               observed.get("content-sha256") == observed.get("disk-sha256"))
    reason = None if matched else "loaded-code-does-not-match-disk"
    return result("serving-runner-code", matched, "unverified", "operator-action",
                  reason=reason, loaded=observed,
                  warranted_commit=(tested_tree or {}).get("head"))


def class_emission_observation():
    """Exercise the loaded reading-policy scorer, not a fixture/parser."""
    form = """(let [v (resolve 'futon2.aif.cascade-shape-g/class-emission-readiness)
  r (when v (v))]
  (str "WMCLASS|" (if v (name (:status r)) "unavailable")
       "|" (or (some-> r :model-kind name) "")
       "|" (boolean (:consumed-in-g? r))
       "|" (or (some-> r :scorer-version name) "")
       "|" (or (some-> r :reason name) "function-not-loaded")))"""
    observed = run(["bash", PROOF_EVAL, form], "/home/joe/code/futon3c")
    match = re.search(r':value "WMCLASS\|([^\"]*)"', observed.stdout)
    if observed.returncode != 0 or not match:
        return {"status": "unavailable", "reason": "serving-query-failed",
                "query-exit": observed.returncode}
    fields = match.group(1).split("|")
    if len(fields) != 5:
        return {"status": "unavailable", "reason": "serving-response-malformed"}
    status, model_kind, consumed, scorer_version, reason = fields
    return {"status": status, "model-kind": model_kind or None,
            "consumed-in-g": consumed == "true",
            "scorer-version": scorer_version or None,
            "reason": reason or None}


def class_emission_result(observed=None):
    observed = observed or class_emission_observation()
    passed = (observed.get("status") == "ready" and
              observed.get("model-kind") == "class-emission" and
              observed.get("consumed-in-g") is True)
    return result("reading-policy-class-emission", passed,
                  "predictable-cannot-repair", "self-clearing",
                  reason=None if passed else observed.get("reason") or
                  "class-emission-not-consumed-by-loaded-scorer",
                  loaded=observed)


def focused_warrant_result():
    """Check the affected test closures without rerunning them.

    The registry re-hashes the files actually loaded by each successful test
    run.  This supersedes the old all-repository receipt for click readiness:
    an unrelated commit or untracked lab receipt cannot invalidate tests whose
    recorded closure is unchanged.
    """
    try:
        with open(WARRANT_INDEX, encoding="utf-8") as stream:
            index = json.load(stream)
    except (OSError, json.JSONDecodeError) as error:
        return result("focused-test-warrants", False, "unverified", "self-clearing",
                      reason="warrant-index-unreadable", error=str(error))
    checks = {}
    for namespace in REQUIRED_WARRANTS:
        entry = index.get(namespace) or {}
        entry_id = entry.get("entry-id")
        base = entry.get("git-head")
        if not entry_id or not base:
            checks[namespace] = {"warrant?": False, "reason": "warrant-absent"}
            continue
        changed = run(["git", "diff", "--name-only", base, "--"], ROOT)
        body = json.dumps({"entry-id": entry_id, "repo-root": ROOT,
                           "changed-paths": changed.stdout.splitlines()}).encode()
        request = urllib.request.Request(
            "http://localhost:7070/api/alpha/test-registry/check", data=body,
            headers={"Content-Type": "application/json"})
        try:
            with urllib.request.urlopen(request, timeout=30) as response:
                answer = json.load(response)
            check = answer.get("check", answer)
            checks[namespace] = {"warrant?": check.get("warrant?") is True,
                                 "reason": check.get("reason"),
                                 "entry-id": entry_id}
        except Exception as error:
            checks[namespace] = {"warrant?": False,
                                 "reason": "registry-check-unavailable",
                                 "error": str(error), "entry-id": entry_id}
    passed = all(check.get("warrant?") is True for check in checks.values())
    return result("focused-test-warrants", passed, "unverified", "self-clearing",
                  reason=None if passed else "one-or-more-warrants-stale",
                  namespaces=checks)


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


def workspace_gate_receipt_control():
    empty = hashlib.sha256(b"").hexdigest()
    states = {name: {"head": "head-" + name, "tree-sha": "tree-" + name,
                     "tracked-diff-sha256": empty, "dirty": False,
                     "readable": True}
              for name in WORKSPACE_REPOSITORIES}
    rows = [{"repository": name, "path": WORKSPACE_REPOSITORIES[name],
             "git-sha": state["head"], "tree-sha": state["tree-sha"],
             "tracked-diff-sha256": state["tracked-diff-sha256"],
             "dirty?": False, "readable?": True}
            for name, state in states.items()]
    exact, _, _ = workspace_basis_matches(rows, states)
    changed = {name: dict(state) for name, state in states.items()}
    changed["p4ng"]["tree-sha"] = "different-tree"
    stale, reason, repository = workspace_basis_matches(rows, changed)
    report = {"exact-basis-accepted": exact,
              "different-basis-rejected": not stale,
              "reason": reason, "repository": repository}
    passed = (exact and not stale and reason == "workspace-basis-differs" and
              repository == "p4ng")
    print(json.dumps(report, indent=2, sort_keys=True))
    print("run-readiness-workspace-receipt-control:",
          "PASS" if passed else "FAIL",
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


def serving_code_control():
    check = serving_code_result(
        {"head": "tested-commit"},
        {"availability": "available", "content-sha256": "loaded",
         "disk-sha256": "different", "current-status": "stale",
         "path": "src/futon2/aif/full_loop_runner.clj"})
    passed = (not check["pass"] and
              check["evidence"]["reason"] == "loaded-code-does-not-match-disk")
    print(json.dumps(check, indent=2, sort_keys=True))
    print("run-readiness-serving-code-control:", "PASS" if passed else "FAIL",
          "mismatched loaded commit rejected exit-convention=0-pass/2-control-slipped")
    return 0 if passed else 2


def class_emission_control():
    check = class_emission_result(
        {"status": "ready", "model-kind": "class-emission",
         "consumed-in-g": False, "scorer-version": "forged"})
    passed = (not check["pass"] and
              check["evidence"]["reason"] ==
              "class-emission-not-consumed-by-loaded-scorer")
    print(json.dumps(check, indent=2, sort_keys=True))
    print("run-readiness-class-emission-control:",
          "PASS" if passed else "FAIL",
          "unconsumed class receipt rejected exit-convention=0-pass/2-control-slipped")
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
    cast = dict(WM_CAST)
    chosen = cast["reviewer"] if all(available(name) for name in cast.values()) else None
    if negative:
        chosen = None
    return result("wm-cast-availability", chosen is not None, "unavailable", "operator-action",
                  roster_reachable=True, cast=cast,
                  selected_reviewer=chosen,
                  candidates={name: available(name) for name in cast.values()},
                  negative_control=negative), chosen


def main():
    if "--tree-control" in sys.argv[1:]:
        return tree_control()
    if "--resolution-control" in sys.argv[1:]:
        return resolution_control()
    if "--serving-code-control" in sys.argv[1:]:
        return serving_code_control()
    if "--class-emission-control" in sys.argv[1:]:
        return class_emission_control()
    if "--workspace-gate-receipt-control" in sys.argv[1:]:
        return workspace_gate_receipt_control()
    negative = "--negative-reviewer" in sys.argv[1:]
    checks = []

    reviewer, chosen = roster_readiness(negative)
    checks.append(reviewer)

    authority = run(["bb", "checks/contract_authority_current.clj"])
    checks.append(result("contract-freshness", authority.returncode == 0, "unverified", "self-clearing", exit=authority.returncode,
                         summary=authority.stdout.strip().splitlines()[-1] if authority.stdout.strip() else None))

    # Preflight checks the current writer contract, not the multi-gigabyte
    # historical reader census. The complete census is a maintenance check;
    # rereading it several times here made a one-click readiness probe take
    # minutes or exhaust the JVM before a click was even issued.
    schema = run(["clojure", "-M", "-m", "checks.trace-schema-compatibility",
                  "--negative"])
    checks.append(result("trace-schema-v20-readback", schema.returncode == 0, "unverified", "self-clearing", exit=schema.returncode,
                         summary=schema.stdout.strip().splitlines()[-1] if schema.stdout.strip() else None))

    listed = run(["python3", BG, "test-list"])
    try:
        records = json.loads(listed.stdout)
    except json.JSONDecodeError:
        records = []
    # The all-repository receipts are maintenance evidence, not a launch
    # prerequisite.  Click readiness consumes closure-scoped test warrants,
    # so unrelated repository activity cannot force an all-suite rerun.
    checks.append(focused_warrant_result())
    checks.append(serving_code_result({"head": repository_state(ROOT).get("head")}))
    checks.append(class_emission_result())

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
    # C213: `clojure -M:wm-full-loop ...` as a process is RETIRED for clicks
    # (CLAUDE.md, M-omni-wm-runner 2026-07-26).  Clicks run in-process in the
    # serving JVM; a fresh runtime JVM from this repo is the thing forbidden.
    command = (
        "bb -cp . checks/wm_click_resource_observer.clj "
        "holes/labs/wm-contract/wm-click-resource-YYYYMMDDTHHMMSS.receipt.json "
        f"{chosen}"
        if chosen else None)
    report = {"readiness": "READY" if ready else "NOT-READY", "instruction": instruction,
              "message": ("run the printed command" if ready else
                          "operator action required, then re-run" if instruction == "needs-you" else
                          "wait for in-flight work to settle, then re-run"), "checks": checks,
              "operator-command": command, "side-effects": "none; no tick and no dispatch"}
    print(json.dumps(report, indent=2, sort_keys=True))
    print("run-readiness:", report["readiness"], "(" + instruction + ")",
          "exit-convention=0-ready/1-not-ready/2-negative-control-slipped")
    if negative:
        reviewer_failed = not next(c for c in checks if c["name"] == "wm-cast-availability")["pass"]
        if not ready and reviewer_failed:
            print("run-readiness: PASS negative reviewer mutation rejected")
            return 0
        print("run-readiness: FAIL negative reviewer mutation slipped")
        return 2
    return 0 if ready else 1


if __name__ == "__main__":
    sys.exit(main())
