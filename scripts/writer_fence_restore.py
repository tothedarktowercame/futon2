#!/usr/bin/env python3
"""Fail-closed writer-fence manifest, journal, and restoration tool.

Capture is read-only. `record` observes a completed park before appending it.
`restore` executes only typed, manifest-bound, currently-valid inverses.
"""

import argparse
import datetime
import hashlib
import hmac
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile
import stat

FUTON3C = Path("/home/joe/code/futon3c")
REGISTRY = FUTON3C / "data/apm-coordinators/registry.edn"
PROOF_EVAL = FUTON3C / "scripts/proof-eval.sh"
TERMINAL_ID = "jit-queue:jit-m94A03-retry-v3"
RUNNING_IDS = ("jit-queue:jit-all-open-v2", "ftriangle-live-smoke-v1")
COORDINATORS = (TERMINAL_ID,) + RUNNING_IDS
UNITS = (
    "apm-campaign-babysit-jit-all-open-v2.service",
    "apm-watchdog.timer", "apm-watchdog.service", "apm-closer.service",
    "apm-axiom-audit.timer", "apm-axiom-audit.service",
    "futon-pattern-index.timer", "futon-pattern-index.service",
)
SCHEMA = "wm-writer-fence-restore-v2"
DEFAULT_KEY = Path(os.environ.get(
    "FUTON_WRITER_FENCE_KEY",
    "/home/joe/.config/futon/writer-fence-restore.key"))


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def authenticate(value, key):
    return hmac.new(key, canonical(value), hashlib.sha256).hexdigest()


def read_key(path):
    path = Path(path)
    fd = os.open(path, os.O_RDONLY | getattr(os, "O_NOFOLLOW", 0))
    try:
        metadata = os.fstat(fd)
        if not stat.S_ISREG(metadata.st_mode):
            raise ValueError("manifest-key-not-regular-file")
        if metadata.st_uid != os.geteuid():
            raise ValueError("manifest-key-owner-mismatch")
        if stat.S_IMODE(metadata.st_mode) & 0o077:
            raise ValueError("manifest-key-not-owner-only")
        with os.fdopen(fd, "rb") as handle:
            fd = None
            key = handle.read().strip()
    finally:
        if fd is not None: os.close(fd)
    if len(key) < 32:
        raise ValueError("manifest-key-too-short")
    return key


def atomic_json(path, value):
    path = Path(path)
    path.parent.mkdir(parents=True, exist_ok=True)
    fd, tmp = tempfile.mkstemp(prefix=path.name + ".", dir=path.parent)
    try:
        with os.fdopen(fd, "w", encoding="utf-8") as handle:
            json.dump(value, handle, indent=2, sort_keys=True)
            handle.write("\n"); handle.flush(); os.fsync(handle.fileno())
        os.replace(tmp, path)
    finally:
        if os.path.exists(tmp): os.unlink(tmp)


def proof(form):
    result = subprocess.run([str(PROOF_EVAL), "-"], input=form, text=True,
                            capture_output=True, cwd=FUTON3C)
    if result.returncode:
        raise RuntimeError("serving-jvm-observation-unavailable: " + result.stderr.strip())
    converted = subprocess.run(
        ["bb", "-e", "(require '[clojure.edn :as e] '[cheshire.core :as j]) "
         "(print (j/generate-string (e/read-string (slurp *in*))))"],
        input=result.stdout, text=True, capture_output=True)
    value = json.loads(converted.stdout)
    if not value.get("ok"): raise RuntimeError("serving-jvm-observation-invalid")
    return value["value"]


def observe_coordinator(identity):
    form = f'''(do
      (require 'futon3c.apm.durable-coordinator)
      (require 'futon3c.apm.semantic-progress-watchdog)
      (let [s (futon3c.apm.durable-coordinator/status {json.dumps(str(REGISTRY))} {json.dumps(identity)})]
        {{:present? (some? s)
          :enabled? (get-in s [:registration :coordinator/enabled?])
          :lifecycle (get-in s [:registration :coordinator/lifecycle])
          :durable-status (get-in s [:durable-state :regulator/status])
          :tick-claim (:tick-claim s)
          :quiescence-witness (get-in s [:durable-state :regulator/quiescence-witness])
          :runtime-scheduler-present? (some? (:runtime s))
          :watchdog-scheduler-present? (boolean
            (futon3c.apm.semantic-progress-watchdog/running?
              (str "semantic-progress:" {json.dumps(identity)})))}}))'''
    return proof(form)


def observe_unit(identity):
    result = subprocess.run(
        ["systemctl", "--user", "show", identity, "--no-pager",
         "-p", "Id", "-p", "LoadState", "-p", "UnitFileState",
         "-p", "ActiveState", "-p", "SubState"], text=True,
        capture_output=True)
    if result.returncode: raise RuntimeError("unit-observation-unavailable:" + identity)
    return dict(line.split("=", 1) for line in result.stdout.splitlines() if "=" in line)


def capture(fence_id, key):
    targets = {}
    for identity in COORDINATORS:
        observed = observe_coordinator(identity)
        expected_class = "terminal-watchdog" if identity == TERMINAL_ID else "running-coordinator"
        targets[identity] = {"kind": "coordinator", "class": expected_class,
                             "pre-state": observed}
    for identity in UNITS:
        targets[identity] = {"kind": "unit", "class": "systemd-unit",
                             "pre-state": observe_unit(identity)}
    body = {"schema": SCHEMA, "fence-id": fence_id,
            "captured-at": datetime.datetime.now(datetime.timezone.utc).isoformat(),
            "targets": targets}
    return dict(body, **{"manifest-hmac-sha256": authenticate(body, key)})


def validate_manifest(value, key, expected_fence_id):
    value = dict(value)
    copy = dict(value)
    supplied = copy.pop("manifest-hmac-sha256", None)
    if (copy.get("schema") != SCHEMA
            or not hmac.compare_digest(supplied or "", authenticate(copy, key))):
        raise ValueError("manifest-authentication-invalid")
    if copy.get("fence-id") != expected_fence_id:
        raise ValueError("manifest-fence-id-mismatch")
    targets = copy.get("targets")
    if not isinstance(targets, dict) or not targets:
        raise ValueError("NOTHING-RECORDED:manifest-zero-targets")
    for identity, entry in targets.items():
        required = ("terminal-watchdog" if identity == TERMINAL_ID else
                    "running-coordinator" if identity in RUNNING_IDS else
                    "systemd-unit" if identity in UNITS else None)
        if required is None or entry.get("class") != required:
            raise ValueError("manifest-target-class-invalid:" + identity)
    return value


def load_manifest(path, key, expected_fence_id):
    return validate_manifest(json.loads(Path(path).read_text()), key,
                             expected_fence_id)


def expected_action(entry):
    if entry["class"] == "terminal-watchdog": return "rearm-terminal-coordinator"
    if entry["class"] == "running-coordinator": return "resume-coordinator"
    if entry["class"] == "systemd-unit": return "start-unit"
    raise ValueError("target-class-unknown")


def validate_pre(entry):
    pre = entry["pre-state"]
    if entry["class"] == "terminal-watchdog":
        return (pre.get("durable-status") == "complete"
                and not pre.get("runtime-scheduler-present?")
                and pre.get("watchdog-scheduler-present?") is True)
    if entry["class"] == "running-coordinator":
        return pre.get("durable-status") == "running" and pre.get("enabled?") is True
    if entry["class"] == "systemd-unit":
        return pre.get("ActiveState") in ("active", "activating")
    return False


def parked(entry, current):
    if entry["class"] == "terminal-watchdog":
        return (current.get("durable-status") == "complete"
                and current.get("tick-claim") is None
                and not current.get("runtime-scheduler-present?")
                and not current.get("watchdog-scheduler-present?"))
    if entry["class"] == "running-coordinator":
        witness = current.get("quiescence-witness")
        return (current.get("durable-status") == "stopped"
                and current.get("enabled?") is False
                and current.get("tick-claim") is None
                and not current.get("runtime-scheduler-present?")
                and not current.get("watchdog-scheduler-present?")
                and isinstance(witness, dict))
    if entry["class"] == "systemd-unit":
        return current.get("ActiveState") == "inactive"
    return False


def load_journal(path, missing_ok=False):
    if not Path(path).exists():
        if missing_ok: return []
        raise ValueError("NOTHING-RECORDED:journal-missing")
    rows = []
    for number, line in enumerate(Path(path).read_text().splitlines(), 1):
        try: rows.append(json.loads(line))
        except json.JSONDecodeError as exc: raise ValueError(f"journal-invalid-line:{number}") from exc
    return rows


def append_record(path, row):
    fd = os.open(path, os.O_WRONLY | os.O_CREAT | os.O_APPEND, 0o600)
    try:
        os.write(fd, canonical(row) + b"\n"); os.fsync(fd)
    finally: os.close(fd)


class LiveBackend:
    def observe(self, identity, entry):
        return observe_coordinator(identity) if entry["kind"] == "coordinator" else observe_unit(identity)

    def execute(self, action, identity):
        if action == "start-unit":
            result = subprocess.run(["systemctl", "--user", "start", identity])
            return {"ok": result.returncode == 0, "exit": result.returncode}
        verb = "start-registered!" if action == "rearm-terminal-coordinator" else "resume!"
        form = ("(do (require 'futon3c.apm.durable-coordinator) "
                f"(futon3c.apm.durable-coordinator/{verb} "
                f"{json.dumps(str(REGISTRY))} {json.dumps(identity)}))")
        return proof(form)


def record(manifest, journal_path, action, identity, backend):
    entry = manifest["targets"].get(identity)
    if not entry or expected_action(entry) != action or not validate_pre(entry):
        raise ValueError("record-action-or-prestate-mismatch")
    prior = load_journal(journal_path, missing_ok=True)
    if any(row.get("target") == identity for row in prior):
        raise ValueError("journal-target-duplicate")
    current = backend.observe(identity, entry)
    if not parked(entry, current): raise ValueError("park-not-observed")
    row = {"schema": SCHEMA, "fence-id": manifest["fence-id"],
           "manifest-hmac-sha256": manifest["manifest-hmac-sha256"],
           "ordinal": len(prior) + 1, "action": action, "target": identity,
           "recorded-at": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    append_record(journal_path, row)
    return row


def validate_rows(manifest, rows):
    seen = set()
    for index, row in enumerate(rows, 1):
        identity = row.get("target"); entry = manifest["targets"].get(identity)
        if (row.get("schema") != SCHEMA
                or row.get("fence-id") != manifest["fence-id"]
                or row.get("manifest-hmac-sha256") != manifest["manifest-hmac-sha256"]
                or row.get("ordinal") != index or identity in seen or not entry
                or row.get("action") != expected_action(entry)
                or not validate_pre(entry)):
            raise ValueError(f"journal-row-invalid:{index}")
        seen.add(identity)


def restored(entry, current):
    if entry["class"] == "terminal-watchdog":
        return (current.get("durable-status") == "complete"
                and not current.get("runtime-scheduler-present?")
                and current.get("watchdog-scheduler-present?") is True)
    if entry["class"] == "running-coordinator":
        return (current.get("durable-status") == "running"
                and current.get("enabled?") is True)
    if entry["class"] == "systemd-unit":
        return current.get("ActiveState") in ("active", "activating")
    return False


def load_outcomes(path, manifest, rows):
    outcomes = load_journal(path, missing_ok=True)
    expected_ordinals = list(range(len(rows), len(rows) - len(outcomes), -1))
    if [row.get("ordinal") for row in outcomes] != expected_ordinals:
        raise ValueError("restore-outcomes-not-reverse-prefix")
    for outcome in outcomes:
        source = rows[outcome["ordinal"] - 1]
        if (outcome.get("schema") != SCHEMA
                or outcome.get("fence-id") != manifest["fence-id"]
                or outcome.get("manifest-hmac-sha256") != manifest["manifest-hmac-sha256"]
                or outcome.get("target") != source["target"]
                or outcome.get("action") != source["action"]
                or outcome.get("status") != "restored"):
            raise ValueError("restore-outcome-invalid")
    return outcomes


def load_attempts(path, manifest, rows):
    attempts = load_journal(path, missing_ok=True)
    expected_ordinals = list(range(len(rows), len(rows) - len(attempts), -1))
    if [row.get("ordinal") for row in attempts] != expected_ordinals:
        raise ValueError("restore-attempts-not-reverse-prefix")
    for attempt in attempts:
        source = rows[attempt["ordinal"] - 1]
        if (attempt.get("schema") != SCHEMA
                or attempt.get("fence-id") != manifest["fence-id"]
                or attempt.get("manifest-hmac-sha256") != manifest["manifest-hmac-sha256"]
                or attempt.get("target") != source["target"]
                or attempt.get("action") != source["action"]
                or attempt.get("status") != "inverse-attempt-recorded"):
            raise ValueError("restore-attempt-invalid")
    return attempts


def outcome_record(manifest, row, reconciliation=None):
    value = {"schema": SCHEMA, "fence-id": manifest["fence-id"],
             "manifest-hmac-sha256": manifest["manifest-hmac-sha256"],
             "ordinal": row["ordinal"], "target": row["target"],
             "action": row["action"], "status": "restored",
             "recorded-at": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    if reconciliation:
        value["reconciliation"] = reconciliation
    return value


def restore(manifest, rows, backend, outcomes_path):
    if not rows: raise ValueError("NOTHING-RECORDED")
    validate_rows(manifest, rows)
    prior = load_outcomes(outcomes_path, manifest, rows)
    attempts_path = str(outcomes_path) + ".attempts.jsonl"
    attempts = load_attempts(attempts_path, manifest, rows)
    completed = {row["ordinal"] for row in prior}
    attempted = {row["ordinal"] for row in attempts}
    outcomes, reconciled = [], []
    for row in reversed(rows):
        entry = manifest["targets"][row["target"]]
        if row["ordinal"] in completed:
            if not restored(entry, backend.observe(row["target"], entry)):
                raise ValueError("restored-outcome-current-state-mismatch:" + row["target"])
            continue
        # This observation is deliberately adjacent to the inverse.  Earlier
        # validation never substitutes for compare-before-act.
        current = backend.observe(row["target"], entry)
        if restored(entry, current) and row["ordinal"] in attempted:
            recorded = outcome_record(
                manifest, row, "observed-restored-outcome-record-missing")
            append_record(outcomes_path, recorded)
            outcomes.append(recorded); reconciled.append(row["target"])
            continue
        if restored(entry, current):
            raise ValueError("restored-state-without-inverse-attempt:" + row["target"])
        if not parked(entry, current):
            raise ValueError("journal-action-not-observed:" + str(row["ordinal"]))
        if row["ordinal"] not in attempted:
            attempt = {"schema": SCHEMA, "fence-id": manifest["fence-id"],
                       "manifest-hmac-sha256": manifest["manifest-hmac-sha256"],
                       "ordinal": row["ordinal"], "target": row["target"],
                       "action": row["action"], "status": "inverse-attempt-recorded",
                       "recorded-at": datetime.datetime.now(datetime.timezone.utc).isoformat()}
            append_record(attempts_path, attempt)
            attempted.add(row["ordinal"])
        outcome = backend.execute(row["action"], row["target"])
        if not outcome.get("ok"):
            raise RuntimeError("restore-action-failed:" + row["target"])
        if not restored(entry, backend.observe(row["target"], entry)):
            raise RuntimeError("restore-postcondition-unconfirmed:" + row["target"])
        recorded = outcome_record(manifest, row)
        append_record(outcomes_path, recorded)
        outcomes.append(recorded)
    return {"outcomes": outcomes, "reconciled-missing-outcomes": reconciled,
            "assurance": "final-state-observed",
            "residual-limitation":
            "compare-before-act-narrows-race-but-does-not-prove-event-freedom"}


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    cap = sub.add_parser("capture"); cap.add_argument("--manifest", required=True)
    rec = sub.add_parser("record"); rec.add_argument("--manifest", required=True); rec.add_argument("--journal", required=True); rec.add_argument("--action", required=True); rec.add_argument("--target", required=True)
    res = sub.add_parser("restore"); res.add_argument("--manifest", required=True); res.add_argument("--journal", required=True); res.add_argument("--outcomes", required=True)
    for command in (cap, rec, res):
        command.add_argument("--fence-id", required=True)
        command.add_argument("--key-file", default=str(DEFAULT_KEY))
    args = parser.parse_args(argv)
    try:
        key = read_key(args.key_file)
        if args.command == "capture":
            value = capture(args.fence_id, key); atomic_json(args.manifest, value)
        elif args.command == "record":
            value = record(load_manifest(args.manifest, key, args.fence_id), args.journal,
                           args.action, args.target, LiveBackend())
        else:
            value = restore(load_manifest(args.manifest, key, args.fence_id),
                            load_journal(args.journal), LiveBackend(), args.outcomes)
        print(json.dumps({"ok": True, "value": value}, indent=2, sort_keys=True)); return 0
    except (OSError, ValueError, RuntimeError) as exc:
        print(json.dumps({"ok": False, "reason": str(exc)}, indent=2, sort_keys=True)); return 1


if __name__ == "__main__": raise SystemExit(main())
