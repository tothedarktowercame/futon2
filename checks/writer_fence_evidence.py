#!/usr/bin/env python3
"""Read-only evidence bundle for the War Machine writer fence.

Exit 0 = FENCE-VERIFIABLE, 1 = FENCE-BREACH, 3 = FENCE-INDETERMINATE,
2 = a self-test escaped. This command never parks or resumes anything.
"""

import argparse
import hashlib
import json
import os
import subprocess
import sys


FUTON3C = "/home/joe/code/futon3c"
PROOF_EVAL = FUTON3C + "/scripts/proof-eval.sh"
REGISTRY = FUTON3C + "/data/apm-coordinators/registry.edn"
QUIESCENCE = "/home/joe/code/futon2/checks/quiescence_check.py"
REPOS = (
    "/home/joe/code/futon2", "/home/joe/code/futon3c",
    "/home/joe/code/mathlib4", "/home/joe/code/p4ng", "/home/joe/code/futon3",
)
COORDINATORS = (
    "jit-queue:jit-m94A03-retry-v3",
    "jit-queue:jit-all-open-v2",
    "ftriangle-live-smoke-v1",
)
UNITS = (
    "apm-campaign-babysit-jit-all-open-v2.service",
    "apm-watchdog.timer", "apm-watchdog.service", "apm-closer.service",
    "apm-axiom-audit.timer", "apm-axiom-audit.service",
    "futon-pattern-index.timer", "futon-pattern-index.service",
)
ATTESTATIONS = (
    "operator-no-workspace-write",
    "dispatch-frozen",
    "publisher-paused",
    "sessions-reconciled",
    "coordinators-not-resumed-before-release",
)


def run(argv, *, stdin=None, cwd=None):
    return subprocess.run(argv, input=stdin, cwd=cwd, capture_output=True, text=True)


def edn_json(text):
    converted = run([
        "bb", "-e",
        "(require '[clojure.edn :as e] '[cheshire.core :as j]) "
        "(print (j/generate-string (e/read-string (slurp *in*))))",
    ], stdin=text)
    if converted.returncode:
        raise RuntimeError("coordinator-result-unparseable: " + converted.stderr.strip())
    return json.loads(converted.stdout)


def coordinator_state():
    ids = " ".join(json.dumps(x) for x in COORDINATORS)
    form = f'''(do
      (require 'futon3c.apm.durable-coordinator)
      (into {{}}
        (for [id [{ids}]
              :let [s (futon3c.apm.durable-coordinator/status {json.dumps(REGISTRY)} id)
                    w (get-in s [:durable-state :regulator/quiescence-witness])]]
          [id {{:present? (some? s)
                :enabled? (get-in s [:registration :coordinator/enabled?])
                :lifecycle (get-in s [:registration :coordinator/lifecycle])
                :runtime-scheduler-present? (some? (:runtime s))
                :durable-status (get-in s [:durable-state :regulator/status])
                :durable-epoch (get-in s [:durable-state :regulator/epoch])
                :tick-claim (:tick-claim s)
                :witness-type (:state/type w)
                :witness-coordinator (:coordinator/id w)
                :witness-epoch (:regulator/epoch w)
                :witness-tick-claim (:tick-claim w)
                :witnessed-at (:witnessed-at w)}}])))'''
    observed = run([PROOF_EVAL, "-"], stdin=form, cwd=FUTON3C)
    if observed.returncode:
        raise RuntimeError("serving-jvm-status-unavailable: " + observed.stderr.strip())
    envelope = edn_json(observed.stdout)
    if envelope.get("ok") is not True or not isinstance(envelope.get("value"), dict):
        raise RuntimeError("serving-jvm-status-invalid")
    with open(REGISTRY, "rb") as handle:
        digest = hashlib.sha256(handle.read()).hexdigest()
    return {"registry-sha256": digest, "coordinators": envelope["value"]}


def unit_state():
    argv = ["systemctl", "--user", "show", *UNITS, "--no-pager",
            "-p", "Id", "-p", "ActiveState", "-p", "SubState",
            "-p", "InvocationID", "-p", "NextElapseUSecRealtime"]
    observed = run(argv)
    if observed.returncode:
        raise RuntimeError("systemd-state-unavailable: " + observed.stderr.strip())
    rows, row = {}, {}
    for line in observed.stdout.splitlines() + [""]:
        if not line:
            if row.get("Id"):
                rows[row["Id"]] = row
            row = {}
        elif "=" in line:
            key, value = line.split("=", 1)
            row[key] = value
    missing = sorted(set(UNITS) - set(rows))
    if missing:
        raise RuntimeError("systemd-units-missing: " + ",".join(missing))
    return rows


def writable_handles():
    roots = tuple(path + "/" for path in REPOS)
    found = []
    for pid in (name for name in os.listdir("/proc") if name.isdigit()):
        try:
            fds = os.listdir(f"/proc/{pid}/fd")
        except OSError:
            continue
        for fd in fds:
            try:
                path = os.readlink(f"/proc/{pid}/fd/{fd}")
                if not path.startswith(roots):
                    continue
                with open(f"/proc/{pid}/fdinfo/{fd}") as info:
                    flags_line = next(x for x in info if x.startswith("flags:"))
                flags = int(flags_line.split()[1], 8)
                if flags & 3 not in (1, 2):
                    continue
                with open(f"/proc/{pid}/cmdline", "rb") as command:
                    cmd = command.read().replace(b"\0", b" ").decode(errors="replace")
                found.append({"pid": int(pid), "fd": int(fd), "flags": oct(flags),
                              "path": path, "command": cmd})
            except (OSError, StopIteration, ValueError):
                # A process/fd disappearing during enumeration is not evidence
                # of a surviving writable handle. The outer state sandwich and
                # attestations constrain the interval; this remains a scan.
                continue
    return sorted(found, key=lambda x: (x["pid"], x["fd"]))


def quiescence_state():
    observed = run(["python3", QUIESCENCE])
    try:
        report = json.loads(observed.stdout)
    except json.JSONDecodeError as failure:
        raise RuntimeError("c292-output-unparseable") from failure
    return {"exit": observed.returncode, "report": report}


def snapshot():
    return {
        "coordinator-state": coordinator_state(),
        "unit-state": unit_state(),
        "writable-handles": writable_handles(),
        "c292": quiescence_state(),
    }


def coordinator_findings(state):
    findings = []
    rows = state["coordinator-state"]["coordinators"]
    for identity in COORDINATORS:
        row = rows.get(identity)
        expected = {
            "present?": True, "enabled?": False,
            "runtime-scheduler-present?": False,
            "durable-status": "stopped", "tick-claim": None,
            "witness-type": "durable-quiescence-witness",
            "witness-coordinator": identity, "witness-tick-claim": None,
        }
        if not isinstance(row, dict):
            findings.append({"component": "coordinator", "id": identity,
                             "reason": "status-missing"})
            continue
        mismatches = {key: {"expected": value, "observed": row.get(key)}
                      for key, value in expected.items() if row.get(key) != value}
        if row.get("lifecycle") == "running":
            mismatches["lifecycle"] = {"expected": "not-running", "observed": "running"}
        if row.get("witness-epoch") != row.get("durable-epoch"):
            mismatches["witness-epoch"] = {"expected": row.get("durable-epoch"),
                                            "observed": row.get("witness-epoch")}
        if mismatches:
            findings.append({"component": "coordinator", "id": identity,
                             "reason": "not-durably-quiescent", "detail": mismatches})
    return findings


def observed_findings(state):
    findings = coordinator_findings(state)
    for identity, row in state["unit-state"].items():
        if row.get("ActiveState") != "inactive":
            findings.append({"component": "systemd-unit", "id": identity,
                             "reason": "active", "detail": row})
    if state["writable-handles"]:
        findings.append({"component": "writable-handles", "reason": "observed",
                         "detail": state["writable-handles"]})
    if state["c292"]["exit"] != 0 or state["c292"]["report"].get("verdict") != "QUIESCENT":
        findings.append({"component": "c292", "reason": "not-quiescent",
                         "detail": state["c292"]})
    return findings


def load_attestations(path):
    if path is None:
        return {"status": "absent", "required": list(ATTESTATIONS)}
    try:
        with open(path, encoding="utf-8") as handle:
            value = json.load(handle)
    except (OSError, json.JSONDecodeError) as failure:
        return {"status": "unavailable", "reason": str(failure),
                "required": list(ATTESTATIONS)}
    missing = [key for key in ATTESTATIONS if value.get(key) is not True]
    return {"status": "complete" if not missing else "incomplete",
            "required": list(ATTESTATIONS), "missing": missing, "value": value}


def evaluate(first, second, attestations):
    first_findings = observed_findings(first)
    second_findings = observed_findings(second)
    classification = {
        "observed": {"start": first, "finish": second},
        "attested": attestations,
        "unverifiable": [
            "future-manual-or-editor-writer-start",
            "future-agent-or-publisher-dispatch",
            "writer-with-no-current-writable-handle",
            "unenumerated-in-jvm-writer",
            "cross-authority-aba",
        ],
    }
    if first_findings or second_findings:
        return 1, {"verdict": "FENCE-BREACH", "classification": classification,
                   "findings": {"start": first_findings, "finish": second_findings}}
    if first != second:
        return 3, {"verdict": "FENCE-INDETERMINATE",
                   "reason": "observed-state-moved", "classification": classification}
    if attestations.get("status") != "complete":
        return 3, {"verdict": "FENCE-INDETERMINATE",
                   "reason": "attestations-not-complete", "classification": classification}
    return 0, {"verdict": "FENCE-VERIFIABLE", "classification": classification}


def fixture():
    witness = lambda identity: {
        "present?": True, "enabled?": False, "lifecycle": "draining",
        "runtime-scheduler-present?": False, "durable-status": "stopped",
        "durable-epoch": 7, "tick-claim": None,
        "witness-type": "durable-quiescence-witness",
        "witness-coordinator": identity, "witness-epoch": 7,
        "witness-tick-claim": None, "witnessed-at": "fixture",
    }
    return {
        "coordinator-state": {"registry-sha256": "fixture",
                              "coordinators": {x: witness(x) for x in COORDINATORS}},
        "unit-state": {x: {"Id": x, "ActiveState": "inactive", "SubState": "dead",
                            "InvocationID": ""} for x in UNITS},
        "writable-handles": [],
        "c292": {"exit": 0, "report": {"verdict": "QUIESCENT"}},
    }


def self_test():
    base = fixture()
    complete = {"status": "complete"}
    cases = [("verifiable", base, base, complete, 0),
             ("unattested", base, base, {"status": "absent"}, 3)]
    breached = json.loads(json.dumps(base))
    breached["unit-state"][UNITS[0]]["ActiveState"] = "active"
    cases.append(("active-unit", breached, breached, complete, 1))
    moved = json.loads(json.dumps(base))
    moved["unit-state"][UNITS[0]]["InvocationID"] = "new"
    cases.append(("state-moved", base, moved, complete, 3))
    escaped = []
    for name, first, second, attest, expected in cases:
        actual, report = evaluate(first, second, attest)
        print("CONTROL", name, "expected", expected, "actual", actual, report["verdict"])
        if actual != expected:
            escaped.append(name)
    return 2 if escaped else 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--attestations", help="JSON object with the five required true attestations")
    parser.add_argument("--self-test", action="store_true")
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    attestations = load_attestations(args.attestations)
    try:
        first = snapshot()
        second = snapshot()
        code, report = evaluate(first, second, attestations)
    except Exception as failure:
        code, report = 3, {"verdict": "FENCE-INDETERMINATE",
                           "reason": "observation-unavailable", "detail": str(failure),
                           "classification": {"observed": "unavailable",
                                              "attested": attestations,
                                              "unverifiable": list(ATTESTATIONS)}}
    print(json.dumps(report, indent=2, sort_keys=True))
    return code


if __name__ == "__main__":
    sys.exit(main())
