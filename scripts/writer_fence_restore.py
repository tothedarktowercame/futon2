#!/usr/bin/env python3
"""Fail-closed writer-fence manifest, journal, and restoration tool.

Capture is read-only. `record` observes a completed park before appending it.
`restore` executes only typed, manifest-bound, currently-valid inverses.
"""

import argparse
import datetime
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys
import tempfile

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
SCHEMA = "wm-writer-fence-restore-v1"


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def digest(value):
    return hashlib.sha256(canonical(value)).hexdigest()


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


def capture(fence_id):
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
    return dict(body, **{"manifest-sha256": digest(body)})


def load_manifest(path):
    value = json.loads(Path(path).read_text())
    supplied = value.pop("manifest-sha256", None)
    if value.get("schema") != SCHEMA or supplied != digest(value):
        raise ValueError("manifest-invalid")
    return dict(value, **{"manifest-sha256": supplied})


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


def load_journal(path):
    if not Path(path).exists(): return []
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
    prior = load_journal(journal_path)
    if any(row.get("target") == identity for row in prior):
        raise ValueError("journal-target-duplicate")
    current = backend.observe(identity, entry)
    if not parked(entry, current): raise ValueError("park-not-observed")
    row = {"schema": SCHEMA, "manifest-sha256": manifest["manifest-sha256"],
           "ordinal": len(prior) + 1, "action": action, "target": identity,
           "recorded-at": datetime.datetime.now(datetime.timezone.utc).isoformat()}
    append_record(journal_path, row)
    return row


def validate_rows(manifest, rows, backend):
    seen = set()
    for index, row in enumerate(rows, 1):
        identity = row.get("target"); entry = manifest["targets"].get(identity)
        if (row.get("schema") != SCHEMA
                or row.get("manifest-sha256") != manifest["manifest-sha256"]
                or row.get("ordinal") != index or identity in seen or not entry
                or row.get("action") != expected_action(entry)
                or not validate_pre(entry)):
            raise ValueError(f"journal-row-invalid:{index}")
        if not parked(entry, backend.observe(identity, entry)):
            raise ValueError(f"journal-action-not-observed:{index}")
        seen.add(identity)


def restore(manifest, rows, backend):
    validate_rows(manifest, rows, backend)
    outcomes = []
    for row in reversed(rows):
        outcome = backend.execute(row["action"], row["target"])
        if not outcome.get("ok"):
            raise RuntimeError("restore-action-failed:" + row["target"])
        outcomes.append({"target": row["target"], "action": row["action"],
                         "outcome": outcome})
    return outcomes


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    cap = sub.add_parser("capture"); cap.add_argument("--fence-id", required=True); cap.add_argument("--manifest", required=True)
    rec = sub.add_parser("record"); rec.add_argument("--manifest", required=True); rec.add_argument("--journal", required=True); rec.add_argument("--action", required=True); rec.add_argument("--target", required=True)
    res = sub.add_parser("restore"); res.add_argument("--manifest", required=True); res.add_argument("--journal", required=True)
    args = parser.parse_args(argv)
    try:
        if args.command == "capture":
            value = capture(args.fence_id); atomic_json(args.manifest, value)
        elif args.command == "record":
            value = record(load_manifest(args.manifest), args.journal,
                           args.action, args.target, LiveBackend())
        else:
            value = restore(load_manifest(args.manifest), load_journal(args.journal), LiveBackend())
        print(json.dumps({"ok": True, "value": value}, indent=2, sort_keys=True)); return 0
    except (OSError, ValueError, RuntimeError) as exc:
        print(json.dumps({"ok": False, "reason": str(exc)}, indent=2, sort_keys=True)); return 1


if __name__ == "__main__": raise SystemExit(main())
