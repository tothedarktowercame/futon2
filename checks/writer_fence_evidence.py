#!/usr/bin/env python3
"""Read-only evidence bundle for the War Machine writer fence.

Exit 0 = FENCE-VERIFIABLE, 1 = FENCE-BREACH, 3 = FENCE-INDETERMINATE,
2 = a self-test escaped. This command never parks or resumes anything.
"""

import argparse
import datetime
import hashlib
import json
import os
import subprocess
import sys
import tempfile


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
ATTESTATION_SCHEMA = "wm-writer-fence-attestation-v1"
MAX_ATTESTATION_SECONDS = 2 * 60 * 60
EXPECTED_ACKNOWLEDGERS = {
    "operator": "joe",
    "dispatch-coordinator": "claude-20",
    "publisher": "claude-1",
    "sessions": ["wm-nouns", "wm-verbs", "wm-organization", "wm-evidence"],
}


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
      (require 'futon3c.apm.semantic-progress-watchdog)
      (into {{}}
        (for [id [{ids}]
              :let [s (futon3c.apm.durable-coordinator/status {json.dumps(REGISTRY)} id)
                    w (get-in s [:durable-state :regulator/quiescence-witness])]]
          [id {{:present? (some? s)
                :enabled? (get-in s [:registration :coordinator/enabled?])
                :lifecycle (get-in s [:registration :coordinator/lifecycle])
                :runtime-scheduler-present? (some? (:runtime s))
                :watchdog-scheduler-present?
                (boolean
                 (futon3c.apm.semantic-progress-watchdog/running?
                  (str "semantic-progress:" id)))
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
        if not isinstance(row, dict):
            findings.append({"component": "coordinator", "id": identity,
                             "reason": "status-missing"})
            continue
        terminal_complete = (row.get("durable-status") == "complete"
                             and row.get("tick-claim") is None
                             and row.get("runtime-scheduler-present?") is False)
        if terminal_complete:
            expected = {"present?": True,
                        "runtime-scheduler-present?": False,
                        "watchdog-scheduler-present?": False,
                        "durable-status": "complete", "tick-claim": None}
        else:
            expected = {
                "present?": True, "enabled?": False,
                "runtime-scheduler-present?": False,
                "watchdog-scheduler-present?": False,
                "durable-status": "stopped", "tick-claim": None,
                "witness-type": "durable-quiescence-witness",
                "witness-coordinator": identity, "witness-tick-claim": None,
            }
        mismatches = {key: {"expected": value, "observed": row.get(key)}
                      for key, value in expected.items() if row.get(key) != value}
        if not terminal_complete and row.get("lifecycle") == "running":
            mismatches["lifecycle"] = {"expected": "not-running", "observed": "running"}
        if (not terminal_complete
                and row.get("witness-epoch") != row.get("durable-epoch")):
            mismatches["witness-epoch"] = {"expected": row.get("durable-epoch"),
                                            "observed": row.get("witness-epoch")}
        if mismatches:
            findings.append({"component": "coordinator", "id": identity,
                             "reason": "not-durably-quiescent", "detail": mismatches})
    return findings


def observed_findings(state, include_c292=True):
    findings = coordinator_findings(state)
    for identity, row in state["unit-state"].items():
        if row.get("ActiveState") != "inactive":
            findings.append({"component": "systemd-unit", "id": identity,
                             "reason": "active", "detail": row})
    if state["writable-handles"]:
        findings.append({"component": "writable-handles", "reason": "observed",
                         "detail": state["writable-handles"]})
    if (include_c292 and
            (state["c292"]["exit"] != 0
             or state["c292"]["report"].get("verdict") != "QUIESCENT")):
        findings.append({"component": "c292", "reason": "not-quiescent",
                         "detail": state["c292"]})
    return findings


def iso_instant(value):
    if not isinstance(value, str):
        return None
    try:
        return datetime.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError:
        return None


def load_attestations(path, fence_id, now=None):
    now = now or datetime.datetime.now(datetime.timezone.utc)
    if path is None:
        return {"status": "absent", "required": list(ATTESTATIONS)}
    try:
        with open(path, encoding="utf-8") as handle:
            value = json.load(handle)
    except (OSError, json.JSONDecodeError) as failure:
        return {"status": "unavailable", "reason": str(failure),
                "required": list(ATTESTATIONS)}
    issued = iso_instant(value.get("issued-at"))
    expires = iso_instant(value.get("expires-at"))
    acknowledgers = value.get("acknowledged-by")
    writers = value.get("writer-population")
    intended = value.get("intended-state")
    problems = []
    if value.get("schema") != ATTESTATION_SCHEMA:
        problems.append("schema-invalid")
    if not fence_id or value.get("fence-id") != fence_id:
        problems.append("fence-id-mismatch")
    if issued is None or expires is None:
        problems.append("attestation-interval-invalid")
    elif not (issued <= now <= expires):
        problems.append("attestation-not-current")
    elif (expires - issued).total_seconds() > MAX_ATTESTATION_SECONDS:
        problems.append("attestation-window-too-wide")
    if acknowledgers != EXPECTED_ACKNOWLEDGERS:
        problems.append("acknowledgers-invalid")
    if not isinstance(acknowledgers, dict) or not isinstance(acknowledgers.get("sessions"), list):
        problems.append("session-acknowledgers-not-enumerated")
    expected_writers = {"coordinators": list(COORDINATORS), "units": list(UNITS)}
    if writers != expected_writers:
        problems.append("writer-population-mismatch")
    expected_state = {
        "coordinators": {
            "jit-queue:jit-m94A03-retry-v3": "terminal-complete-watchdog-stopped",
            "jit-queue:jit-all-open-v2": "durably-stopped",
            "ftriangle-live-smoke-v1": "durably-stopped",
        },
        "units": "inactive", "writable-handles": "none", "c292": "QUIESCENT"}
    if intended != expected_state:
        problems.append("intended-state-mismatch")
    encoded = json.dumps(value, sort_keys=True, separators=(",", ":")).encode()
    return {"status": "complete" if not problems else "invalid",
            "required": list(ATTESTATIONS), "problems": problems,
            "content-sha256": hashlib.sha256(encoded).hexdigest(), "value": value}


def evaluate(first, second, attestations, fence_id=None, interval=None,
             writer_state_only=False):
    first_findings = observed_findings(first, not writer_state_only)
    second_findings = observed_findings(second, not writer_state_only)
    comparable_first = ({key: value for key, value in first.items() if key != "c292"}
                        if writer_state_only else first)
    comparable_second = ({key: value for key, value in second.items() if key != "c292"}
                         if writer_state_only else second)
    classification = {
        "fence-id": fence_id,
        "observed": {"start": first, "finish": second},
        "attested": attestations,
        "observation-scope": ("parked-writer-population-post-click"
                              if writer_state_only else "full-pre-click-fence"),
        "unverifiable": [
            "future-manual-or-editor-writer-start",
            "future-agent-or-publisher-dispatch",
            "writer-with-no-current-writable-handle",
            "unenumerated-in-jvm-writer",
            "cross-authority-aba",
        ],
    }
    if first_findings or second_findings:
        return 1, {"verdict": "FENCE-BREACH", "fence-id": fence_id,
                   "observation-interval": interval, "classification": classification,
                   "findings": {"start": first_findings, "finish": second_findings}}
    if comparable_first != comparable_second:
        return 3, {"verdict": "FENCE-INDETERMINATE", "fence-id": fence_id,
                   "observation-interval": interval,
                   "reason": "observed-state-moved", "classification": classification}
    if attestations.get("status") != "complete":
        return 3, {"verdict": "FENCE-INDETERMINATE", "fence-id": fence_id,
                   "observation-interval": interval,
                   "reason": "attestations-not-complete", "classification": classification}
    return 0, {"verdict": ("WRITERS-STILL-PARKED" if writer_state_only
                            else "FENCE-VERIFIABLE"), "fence-id": fence_id,
               "observation-interval": interval, "classification": classification}


def fixture():
    witness = lambda identity: {
        "present?": True, "enabled?": False, "lifecycle": "draining",
        "runtime-scheduler-present?": False,
        "watchdog-scheduler-present?": False, "durable-status": "stopped",
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
    complete = {"status": "complete", "value": {"fence-id": "fixture-fence"}}
    cases = [("verifiable", base, base, complete, 0),
             ("unattested", base, base, {"status": "absent"}, 3)]
    breached = json.loads(json.dumps(base))
    breached["unit-state"][UNITS[0]]["ActiveState"] = "active"
    cases.append(("active-unit", breached, breached, complete, 1))
    moved = json.loads(json.dumps(base))
    moved["unit-state"][UNITS[0]]["InvocationID"] = "new"
    cases.append(("state-moved", base, moved, complete, 3))
    terminal = json.loads(json.dumps(base))
    identity = COORDINATORS[0]
    terminal["coordinator-state"]["coordinators"][identity] = {
        "present?": True, "enabled?": True, "lifecycle": "running",
        "runtime-scheduler-present?": False,
        "watchdog-scheduler-present?": False,
        "durable-status": "complete", "tick-claim": None,
    }
    cases.append(("terminal-watchdog-parked", terminal, terminal, complete, 0))
    terminal_live_watchdog = json.loads(json.dumps(terminal))
    terminal_live_watchdog["coordinator-state"]["coordinators"][identity][
        "watchdog-scheduler-present?"] = True
    cases.append(("terminal-watchdog-live", terminal_live_watchdog,
                  terminal_live_watchdog, complete, 1))
    escaped = []
    for name, first, second, attest, expected in cases:
        actual, report = evaluate(first, second, attest, "fixture-fence")
        print("CONTROL", name, "expected", expected, "actual", actual, report["verdict"])
        if actual != expected:
            escaped.append(name)
    post_click = json.loads(json.dumps(base))
    post_click["c292"] = {"exit": 1, "report": {"verdict": "NOT-QUIESCENT",
                                                   "findings": ["authorised-output"]}}
    actual, report = evaluate(post_click, post_click, complete, "fixture-fence",
                              writer_state_only=True)
    print("CONTROL post-click-scope expected 0 actual", actual, report["verdict"])
    if actual != 0 or report["verdict"] != "WRITERS-STILL-PARKED":
        escaped.append("post-click-scope")
    post_click["unit-state"][UNITS[0]]["ActiveState"] = "active"
    actual, report = evaluate(post_click, post_click, complete, "fixture-fence",
                              writer_state_only=True)
    print("CONTROL post-click-active-writer expected 1 actual", actual, report["verdict"])
    if actual != 1:
        escaped.append("post-click-active-writer")
    now = datetime.datetime.now(datetime.timezone.utc)
    value = {
        "schema": ATTESTATION_SCHEMA, "fence-id": "fixture-fence",
        "issued-at": (now - datetime.timedelta(minutes=1)).isoformat(),
        "expires-at": (now + datetime.timedelta(minutes=1)).isoformat(),
        "acknowledged-by": EXPECTED_ACKNOWLEDGERS,
        "writer-population": {"coordinators": list(COORDINATORS), "units": list(UNITS)},
        "intended-state": {
            "coordinators": {
                "jit-queue:jit-m94A03-retry-v3":
                "terminal-complete-watchdog-stopped",
                "jit-queue:jit-all-open-v2": "durably-stopped",
                "ftriangle-live-smoke-v1": "durably-stopped",
            },
            "units": "inactive", "writable-handles": "none",
            "c292": "QUIESCENT"},
    }
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8") as handle:
        json.dump(value, handle)
        handle.flush()
        controls = {
            "bound-attestation": load_attestations(handle.name, "fixture-fence", now),
            "foreign-attestation": load_attestations(handle.name, "other-fence", now),
            "stale-attestation": load_attestations(
                handle.name, "fixture-fence", now + datetime.timedelta(hours=1)),
        }
    for name, result in controls.items():
        expected = "complete" if name == "bound-attestation" else "invalid"
        print("CONTROL", name, "expected", expected, "actual", result["status"])
        if result["status"] != expected:
            escaped.append(name)
    value["acknowledged-by"] = dict(EXPECTED_ACKNOWLEDGERS, sessions=["wm-nouns"])
    with tempfile.NamedTemporaryFile(mode="w", encoding="utf-8") as handle:
        json.dump(value, handle); handle.flush()
        result = load_attestations(handle.name, "fixture-fence", now)
    print("CONTROL incomplete-session-population expected invalid actual", result["status"])
    if result["status"] != "invalid":
        escaped.append("incomplete-session-population")
    return 2 if escaped else 0


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--fence-id", help="identity of the observed fence window")
    parser.add_argument("--attestations", help="structured JSON attestation bound to --fence-id")
    parser.add_argument("--self-test", action="store_true")
    parser.add_argument("--writer-state-only", action="store_true",
                        help="post-click observation; does not claim clean repositories/jobs")
    args = parser.parse_args()
    if args.self_test:
        return self_test()
    if bool(args.fence_id) != bool(args.attestations):
        report = {"verdict": "FENCE-INDETERMINATE", "fence-id": args.fence_id,
                  "reason": "fence-id-and-attestations-required-together"}
        print(json.dumps(report, indent=2, sort_keys=True))
        return 3
    started = datetime.datetime.now(datetime.timezone.utc)
    attestations = load_attestations(args.attestations, args.fence_id, started)
    try:
        first = snapshot()
        second = snapshot()
        finished = datetime.datetime.now(datetime.timezone.utc)
        interval = {"started-at": started.isoformat(), "finished-at": finished.isoformat()}
        code, report = evaluate(first, second, attestations, args.fence_id, interval,
                                args.writer_state_only)
    except Exception as failure:
        code, report = 3, {"verdict": "FENCE-INDETERMINATE", "fence-id": args.fence_id,
                           "reason": "observation-unavailable", "detail": str(failure),
                           "classification": {"observed": "unavailable",
                                              "attested": attestations,
                                              "unverifiable": list(ATTESTATIONS)}}
    print(json.dumps(report, indent=2, sort_keys=True))
    return code


if __name__ == "__main__":
    sys.exit(main())
