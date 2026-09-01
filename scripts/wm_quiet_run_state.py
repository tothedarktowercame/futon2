#!/usr/bin/env python3
"""Receipt-consuming state machine for the War Machine quiet run.

It never parks, reloads, clicks, restores, or releases external writers.  It
validates observations, appends a hash-chained transition receipt, and emits
release authority only after restoration evidence is complete.
"""

import argparse
import datetime as dt
import hashlib
import json
import os
from pathlib import Path
import subprocess
import sys

ROOT = Path(__file__).resolve().parent.parent
if str(ROOT) not in sys.path: sys.path.insert(0, str(ROOT))
from scripts import writer_fence_restore as restoration
from checks import writer_fence_evidence as fence_authority

SCHEMA = "wm-quiet-run-state-v1"
ORDER = ["initial", "quiescence", "fence-held", "tested-commit",
         "reload-recorded", "click-issued", "click-terminal", "certified",
         "restored", "released"]
MAX_FENCE_RECEIPT_AGE = 300


def parking_specification():
    writers = []
    for identity in fence_authority.COORDINATORS:
        terminal = identity == restoration.TERMINAL_ID
        writers.append({
            "id": identity, "kind": "coordinator",
            "park-action": ("semantic-watchdog-stop" if terminal
                            else "durable-coordinator-stop"),
            "operator-command": (
                "cd /home/joe/code/futon3c && scripts/proof-eval.sh "
                f"'(do (require (quote futon3c.apm.semantic-progress-watchdog)) "
                f"(futon3c.apm.semantic-progress-watchdog/stop! \"semantic-progress:{identity}\"))'"
                if terminal else
                "cd /home/joe/code/futon3c && scripts/proof-eval.sh "
                f"'(do (require (quote futon3c.apm.durable-coordinator)) "
                "(futon3c.apm.durable-coordinator/stop! "
                f"\"{restoration.REGISTRY}\" \"{identity}\"))'"),
            "required-observation": (
                "durable-complete; regulator absent; tick claim absent; watchdog absent"
                if terminal else
                "durable-stopped; disabled; regulator/watchdog/tick absent; quiescence witness present")})
    for identity in fence_authority.UNITS:
        writers.append({"id": identity, "kind": "systemd-unit",
                        "park-action": "systemctl-user-stop",
                        "operator-command": f"systemctl --user stop {identity}",
                        "required-observation":
                        "inactive with identical state across the fence observation interval"})
    return {"schema": "wm-quiet-run-parking-spec-v1",
            "coordinator-count": len(fence_authority.COORDINATORS),
            "systemd-unit-count": len(fence_authority.UNITS),
            "writers": writers,
            "acknowledgers": fence_authority.EXPECTED_ACKNOWLEDGERS,
            "entry-receipts": ["QUIESCENT", "FENCE-VERIFIABLE"],
            "must-remain-running": ["futon3c-zone.service"]}


def parking_request(fence_id):
    spec = parking_specification()
    return {"schema": "wm-quiet-run-parking-request-v1", "fence-id": fence_id,
            "specification-sha256": digest(spec), "specification": spec,
            "request": (
                f"Joe — request writer fence {fence_id}. Keep futon3c-zone.service running. "
                f"Park the {spec['coordinator-count']} named coordinator writers and "
                f"the {spec['systemd-unit-count']} named systemd units exactly as rendered; "
                "hold the named acknowledgers until the state machine emits FENCE-RELEASE. "
                "Entry is established only by QUIESCENT followed by FENCE-VERIFIABLE.")}


def canonical(value):
    return json.dumps(value, sort_keys=True, separators=(",", ":")).encode()


def digest(value):
    return hashlib.sha256(canonical(value)).hexdigest()


def instant(value):
    if not isinstance(value, str): return None
    try: return dt.datetime.fromisoformat(value.replace("Z", "+00:00"))
    except ValueError: return None


def now(): return dt.datetime.now(dt.timezone.utc)


def json_file(path):
    text = Path(path).read_text()
    value, end = json.JSONDecoder().raw_decode(text.lstrip())
    require(not text.lstrip()[end:].strip(), "evidence-trailing-content")
    return value


def edn_file(path):
    result = subprocess.run(
        ["bb", "-e", "(require '[clojure.edn :as e] '[cheshire.core :as j]) "
         "(print (j/generate-string (e/read-string (slurp *in*))))"],
        input=Path(path).read_text(), text=True, capture_output=True)
    if result.returncode: raise ValueError("certificate-unreadable")
    return json.loads(result.stdout)


def file_ref(path):
    data = Path(path).read_bytes()
    return {"path": str(Path(path).resolve()), "sha256": hashlib.sha256(data).hexdigest()}


def persist_observation(ledger, state, value):
    directory = Path(str(ledger) + ".evidence")
    directory.mkdir(mode=0o700, parents=True, exist_ok=True)
    path = directory / f"{state}.json"
    temporary = path.with_suffix(".json.tmp")
    temporary.write_text(json.dumps(value, indent=2, sort_keys=True) + "\n")
    os.replace(temporary, path)
    return str(path)


def observe_quiescence():
    result = subprocess.run(["python3", str(ROOT / "checks/quiescence_check.py")],
                            capture_output=True, text=True)
    try: value = json.loads(result.stdout)
    except json.JSONDecodeError as exc: raise ValueError("quiescence-producer-unreadable") from exc
    require(result.returncode == 0 and value.get("verdict") == "QUIESCENT",
            "quiescence-not-proven")
    return value


def observe_fence(fence_id, attestations):
    result = subprocess.run(
        ["python3", str(ROOT / "checks/writer_fence_evidence.py"),
         "--fence-id", fence_id, "--attestations", attestations],
        capture_output=True, text=True)
    try: value = json.loads(result.stdout)
    except json.JSONDecodeError as exc: raise ValueError("fence-producer-unreadable") from exc
    require(result.returncode == 0 and value.get("verdict") == "FENCE-VERIFIABLE",
            "fence-not-verifiable")
    return value


def load_ledger(path):
    rows = []
    if not Path(path).exists(): raise ValueError("state-ledger-missing")
    for number, line in enumerate(Path(path).read_text().splitlines(), 1):
        try: row = json.loads(line)
        except json.JSONDecodeError as exc: raise ValueError(f"state-ledger-invalid:{number}") from exc
        body = dict(row); supplied = body.pop("receipt-sha256", None)
        if body.get("schema") != SCHEMA or supplied != digest(body):
            raise ValueError(f"state-ledger-integrity-failure:{number}")
        expected_previous = rows[-1]["receipt-sha256"] if rows else None
        if body.get("previous-receipt-sha256") != expected_previous:
            raise ValueError(f"state-ledger-chain-failure:{number}")
        expected_state = ORDER[number - 1] if number <= len(ORDER) else None
        if body.get("state") != expected_state:
            raise ValueError(f"state-ledger-history-invalid:{number}")
        if rows and body.get("fence-id") != rows[0].get("fence-id"):
            raise ValueError(f"state-ledger-fence-id-changed:{number}")
        for ref in body.get("evidence", []):
            evidence_path = Path(ref.get("path", ""))
            try: actual = hashlib.sha256(evidence_path.read_bytes()).hexdigest()
            except OSError as exc:
                raise ValueError(f"state-ledger-evidence-unavailable:{number}") from exc
            if actual != ref.get("sha256"):
                raise ValueError(f"state-ledger-evidence-changed:{number}")
        rows.append(row)
    if not rows: raise ValueError("state-ledger-empty")
    return rows


def append(path, body):
    row = dict(body, **{"receipt-sha256": digest(body)})
    with open(path, "a", encoding="utf-8") as handle:
        handle.write(json.dumps(row, sort_keys=True) + "\n")
        handle.flush(); os.fsync(handle.fileno())
    return row


def require(condition, reason):
    if not condition: raise ValueError(reason)


def receipt_ok(receipt, name):
    require(receipt.get("outer-exit") == 0, name + "-outer-failed")
    require(receipt.get("verdict") == "pass", name + "-verdict-not-pass")
    require(receipt.get("resource-status") == "clean", name + "-resource-not-clean")
    require(receipt.get("repository-basis-stable") is True, name + "-basis-not-stable")
    require(receipt.get("repository-basis-start", {}).get("dirty") is False,
            name + "-basis-start-dirty")
    require(receipt.get("repository-basis-finish", {}).get("dirty") is False,
            name + "-basis-finish-dirty")


BG = Path("/home/joe/code/futon3c/scripts/bg.py")


def bounded_job(job_id):
    """Resolve a bounded receipt through its durable producer registry.

    A caller-authored JSON file is not a bounded-job receipt.  The registry
    record, systemd unit, and on-disk receipt must independently agree.
    """
    require(isinstance(job_id, str) and job_id, "bounded-job-id-required")
    result = subprocess.run(["python3", str(BG), "test-status", job_id],
                            capture_output=True, text=True)
    require(result.returncode == 0, "bounded-job-status-unavailable")
    try: record = json.loads(result.stdout)
    except json.JSONDecodeError as exc:
        raise ValueError("bounded-job-status-unreadable") from exc
    require(isinstance(record, dict) and record.get("id") == job_id,
            "bounded-job-not-in-producer-registry")
    require(record.get("unit") and record.get("receipt-file"),
            "bounded-job-producer-identity-incomplete")
    require(record.get("systemd", {}).get("ActiveState") == "inactive",
            "bounded-job-not-terminal")
    require(isinstance(record.get("receipt"), dict), "bounded-job-receipt-unavailable")
    require(json_file(record["receipt-file"]) == record["receipt"],
            "bounded-job-receipt-registry-mismatch")
    return record


def evidence_quiescent(args, context):
    value = observe_quiescence()
    path = persist_observation(args.ledger, "quiescence", value)
    return [file_ref(path)], {"quiescence": "QUIESCENT",
                              "producer": "checks/quiescence_check.py"}


def evidence_fence(args, context):
    require(args.attestations, "fence-attestations-required")
    value = observe_fence(context["fence-id"], args.attestations)
    path = persist_observation(args.ledger, "fence-held", value)
    att = json_file(args.attestations)
    require(value.get("fence-id") == context["fence-id"] == att.get("fence-id"),
            "fence-id-mismatch")
    finished = instant(value.get("observation-interval", {}).get("finished-at"))
    expires = instant(att.get("expires-at")); current = now()
    require(finished is not None and 0 <= (current - finished).total_seconds()
            <= MAX_FENCE_RECEIPT_AGE, "fence-receipt-stale")
    require(expires is not None and current < expires, "fence-attestation-expired")
    return [file_ref(path), file_ref(args.attestations)], {
        "fence-observed-at": finished.isoformat(), "attestation-expires-at": expires.isoformat(),
        "coverage": "bounded-through-tested-commit-only",
        "producer": "checks/writer_fence_evidence.py"}


def evidence_tested(args, context):
    require(len(args.job_id) == 3, "exactly-three-bounded-job-ids-required")
    records = [bounded_job(x) for x in args.job_id]
    require(all(x.get("agent-id") == context["fence-id"] for x in records),
            "tested-attempt-identity-mismatch")
    by_command = {str(x.get("receipt", {}).get("command")): x for x in records}
    require(set(by_command) == {"make workspace-gate", "clojure -T:build ci",
                                "clojure -X:test"},
            "tested-command-population-mismatch")
    gate_record = by_command["make workspace-gate"]
    gate = gate_record["receipt"]; receipt_ok(gate, "workspace-gate")
    require("workspace-gate" in str(gate.get("command", "")),
            "workspace-gate-command-identity-mismatch")
    suites = [by_command[x]["receipt"] for x in
              ("clojure -T:build ci", "clojure -X:test")]
    for index, receipt in enumerate(suites): receipt_ok(receipt, f"suite-{index + 1}")
    futon2 = by_command["clojure -T:build ci"]["receipt"]
    gate_head = gate.get("repository-basis-finish", {}).get("head")
    require(gate_head and gate_head == futon2.get("repository-basis-finish", {}).get("head"),
            "tested-commit-mismatch")
    fence_path = context.get("fence-evidence")
    attestation_path = context.get("fence-attestations")
    require(fence_path and attestation_path, "recorded-fence-evidence-absent")
    fence = json_file(fence_path); att = json_file(attestation_path)
    require(fence.get("verdict") == "FENCE-VERIFIABLE", "tested-fence-not-verifiable")
    observed = instant(fence.get("observation-interval", {}).get("finished-at"))
    # The start instant comes from systemd, not from receipt JSON supplied by
    # the presenter.  It is monotonic on the same boot as the producer.
    gate_start_mono = gate_record.get("systemd", {}).get("ExecMainStartTimestampMonotonic")
    require(str(gate_start_mono or "").isdigit() and int(gate_start_mono) > 0,
            "gate-start-substrate-unavailable")
    gate_finish = instant(gate.get("finished-at"))
    expires = instant(att.get("expires-at"))
    require(all((observed, gate_finish, expires)), "tested-interval-unreadable")
    # Current ingestion time is machine-measured.  A presenter cannot freshen
    # a day-old fence by editing a gate receipt's started-at field.
    age = (now() - observed).total_seconds()
    require(0 <= age <= MAX_FENCE_RECEIPT_AGE, "fence-receipt-expired-at-ingestion")
    require(gate_finish <= expires, "attestation-expired-before-gate-finished")
    refs = [file_ref(fence_path), file_ref(attestation_path)] + [
        file_ref(x["receipt-file"]) for x in records]
    return refs, {"tested-commit": gate_head,
                  "tested-attempt": context["fence-id"],
                  "bounded-job-ids": sorted(args.job_id),
                  "fence-receipt-age-at-ingestion-seconds": age,
                  "attestation-coverage": "ends-with-bounded-tested-phase"}


def evidence_reload(args, context):
    report = json_file(args.evidence)
    require(report.get("readiness") == "READY", "reload-readiness-not-ready")
    serving = [x for x in report.get("checks", []) if x.get("name") == "serving-runner-code"]
    require(len(serving) == 1 and serving[0].get("pass") is True,
            "loaded-code-identity-not-verified")
    return [file_ref(args.evidence)], {"attestation-coverage": "not-claimed-after-tested-phase"}


def evidence_click_issued(args, context):
    value = json_file(args.evidence)
    click_id = value.get("click-id"); require(isinstance(click_id, str) and click_id, "click-id-absent")
    require(instant(value.get("started-at")) is not None, "click-start-time-absent")
    return [file_ref(args.evidence)], {"click-id": click_id,
                                      "attestation-coverage": "not-claimed"}


def evidence_click_terminal(args, context):
    value = json_file(args.evidence)
    require(value.get("schema") == "wm-click-resource-v1", "click-receipt-schema-invalid")
    require(value.get("click-id") == context.get("click-id"), "terminal-click-id-mismatch")
    require(value.get("run-id"), "terminal-run-id-absent")
    require(value.get("terminal-outcome"), "terminal-outcome-absent")
    require(value.get("resource-status") in ("clean", "dirty"), "terminal-resource-unavailable")
    return [file_ref(args.evidence)], {"run-id": value["run-id"],
                                      "terminal-outcome": value["terminal-outcome"],
                                      "attestation-coverage": "not-claimed"}


def evidence_certified(args, context):
    value = edn_file(args.evidence)
    require(value.get("verdict") == "pass", "certificate-not-pass")
    require(value.get("run/id") == context.get("run-id"), "certificate-run-id-mismatch")
    return [file_ref(args.evidence)], {"certificate": str(Path(args.evidence).resolve()),
                                      "attestation-coverage": "not-claimed"}


def active_manifest_targets(manifest):
    result = set()
    for identity, entry in manifest.get("targets", {}).items():
        pre = entry.get("pre-state", {})
        if entry.get("class") in ("terminal-watchdog", "running-coordinator"):
            result.add(identity)
        elif entry.get("class") == "systemd-unit" and pre.get("ActiveState") in ("active", "activating"):
            result.add(identity)
    return result


def evidence_restored(args, context):
    result = json_file(args.evidence)
    require(result.get("ok") is True, "restoration-command-not-successful")
    key = restoration.read_key(args.key_file)
    manifest = restoration.load_manifest(args.manifest, key, context["fence-id"])
    journal = restoration.load_journal(args.journal)
    restoration.validate_rows(manifest, journal)
    outcomes = restoration.load_outcomes(args.outcomes, manifest, journal)
    expected = active_manifest_targets(manifest)
    journal_targets = {x.get("target") for x in journal}
    outcome_targets = {x.get("target") for x in outcomes if x.get("status") == "restored"}
    require(journal_targets == expected, "park-journal-target-population-incomplete")
    require(len(journal_targets) == len(journal), "park-journal-target-duplicate")
    require(outcome_targets == journal_targets, "restoration-outcome-population-incomplete")
    backend = restoration.LiveBackend()
    for identity in sorted(journal_targets):
        entry = manifest["targets"][identity]
        require(restoration.restored(entry, backend.observe(identity, entry)),
                "restoration-live-state-mismatch:" + identity)
    refs = [file_ref(x) for x in (args.evidence, args.manifest, args.journal, args.outcomes)]
    return refs, {"restored-targets": sorted(outcome_targets),
                  "attestation-coverage": "not-claimed"}


VALIDATORS = {"quiescence": evidence_quiescent, "fence-held": evidence_fence,
              "tested-commit": evidence_tested, "reload-recorded": evidence_reload,
              "click-issued": evidence_click_issued, "click-terminal": evidence_click_terminal,
              "certified": evidence_certified, "restored": evidence_restored}


def context(rows):
    result = {"fence-id": rows[0]["fence-id"]}
    for row in rows:
        result.update(row.get("facts", {}))
        if row.get("state") == "fence-held" and len(row.get("evidence", [])) == 2:
            result["fence-evidence"] = row["evidence"][0]["path"]
            result["fence-attestations"] = row["evidence"][1]["path"]
    return result


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    init = sub.add_parser("init"); init.add_argument("--ledger", required=True); init.add_argument("--fence-id", required=True)
    advance = sub.add_parser("advance"); advance.add_argument("--ledger", required=True); advance.add_argument("--to", required=True, choices=ORDER[1:]); advance.add_argument("--evidence"); advance.add_argument("--attestations"); advance.add_argument("--fence-evidence"); advance.add_argument("--job-id", action="append", default=[]); advance.add_argument("--manifest"); advance.add_argument("--journal"); advance.add_argument("--outcomes"); advance.add_argument("--key-file")
    status = sub.add_parser("status"); status.add_argument("--ledger", required=True)
    parking = sub.add_parser("parking-request"); parking.add_argument("--fence-id", required=True)
    args = parser.parse_args(argv)
    try:
        if args.command == "parking-request":
            print(json.dumps(parking_request(args.fence_id), indent=2, sort_keys=True))
            return 0
        if args.command == "init":
            require(not Path(args.ledger).exists(), "state-ledger-already-exists")
            body = {"schema": SCHEMA, "previous-receipt-sha256": None,
                    "state": "initial", "fence-id": args.fence_id,
                    "transitioned-at": now().isoformat(), "evidence": [],
                    "facts": {"parking-specification-sha256":
                              digest(parking_specification())}}
            row = append(args.ledger, body)
        elif args.command == "status":
            rows = load_ledger(args.ledger); row = rows[-1]
        else:
            rows = load_ledger(args.ledger); current = rows[-1]["state"]
            require(ORDER.index(args.to) == ORDER.index(current) + 1,
                    f"transition-not-next:{current}->{args.to}")
            if args.to == "released":
                require(current == "restored", "release-before-restoration")
                refs, facts = [], {"release-authority": "FENCE-RELEASE",
                                   "attestation-coverage": "not-claimed"}
            else:
                if args.to not in ("quiescence", "fence-held", "tested-commit"):
                    require(args.evidence, "transition-evidence-required")
                refs, facts = VALIDATORS[args.to](args, context(rows))
            body = {"schema": SCHEMA,
                    "previous-receipt-sha256": rows[-1]["receipt-sha256"],
                    "state": args.to, "fence-id": rows[0]["fence-id"],
                    "transitioned-at": now().isoformat(), "evidence": refs, "facts": facts}
            row = append(args.ledger, body)
        print(json.dumps({"ok": True, "state": row["state"], "receipt": row,
                          "operator-note": ("FENCE-RELEASE" if row["state"] == "released" else None)}, indent=2, sort_keys=True))
        return 0
    except (OSError, ValueError, KeyError, json.JSONDecodeError) as exc:
        print(json.dumps({"ok": False, "reason": str(exc)}, sort_keys=True)); return 1


if __name__ == "__main__": raise SystemExit(main())
