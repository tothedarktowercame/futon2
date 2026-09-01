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

try:
    from scripts import writer_fence_restore as restoration
except ModuleNotFoundError:
    import writer_fence_restore as restoration

SCHEMA = "wm-quiet-run-state-v1"
ORDER = ["initial", "quiescence", "fence-held", "tested-commit",
         "reload-recorded", "click-issued", "click-terminal", "certified",
         "restored", "released"]
MAX_FENCE_RECEIPT_AGE = 300


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
    value, _ = json.JSONDecoder().raw_decode(text.lstrip())
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


def evidence_quiescent(args, context):
    value = json_file(args.evidence)
    require(value.get("verdict") == "QUIESCENT", "quiescence-not-proven")
    return [file_ref(args.evidence)], {"quiescence": "QUIESCENT"}


def evidence_fence(args, context):
    value = json_file(args.evidence); att = json_file(args.attestations)
    require(value.get("verdict") == "FENCE-VERIFIABLE", "fence-not-verifiable")
    require(value.get("fence-id") == context["fence-id"] == att.get("fence-id"),
            "fence-id-mismatch")
    finished = instant(value.get("observation-interval", {}).get("finished-at"))
    expires = instant(att.get("expires-at")); current = now()
    require(finished is not None and 0 <= (current - finished).total_seconds()
            <= MAX_FENCE_RECEIPT_AGE, "fence-receipt-stale")
    require(expires is not None and current < expires, "fence-attestation-expired")
    return [file_ref(args.evidence), file_ref(args.attestations)], {
        "fence-observed-at": finished.isoformat(), "attestation-expires-at": expires.isoformat(),
        "coverage": "bounded-through-tested-commit-only"}


def evidence_tested(args, context):
    gate = json_file(args.evidence); receipt_ok(gate, "workspace-gate")
    require("workspace-gate" in str(gate.get("command", "")),
            "workspace-gate-command-identity-mismatch")
    suites = [json_file(path) for path in args.suite_receipt]
    require(len(suites) == 2, "exactly-two-suite-receipts-required")
    for index, receipt in enumerate(suites): receipt_ok(receipt, f"suite-{index + 1}")
    require({str(x.get("command")) for x in suites} ==
            {"clojure -T:build ci", "clojure -X:test"},
            "suite-command-population-mismatch")
    fence = json_file(args.fence_evidence); att = json_file(args.attestations)
    require(fence.get("verdict") == "FENCE-VERIFIABLE", "tested-fence-not-verifiable")
    observed = instant(fence.get("observation-interval", {}).get("finished-at"))
    gate_start = instant(gate.get("started-at")); gate_finish = instant(gate.get("finished-at"))
    expires = instant(att.get("expires-at"))
    require(all((observed, gate_start, gate_finish, expires)), "tested-interval-unreadable")
    age = (gate_start - observed).total_seconds()
    require(0 <= age <= MAX_FENCE_RECEIPT_AGE,
            "fence-receipt-expired-before-gate-start")
    require(gate_finish <= expires, "attestation-expired-before-gate-finished")
    refs = [file_ref(args.evidence), file_ref(args.fence_evidence),
            file_ref(args.attestations)] + [file_ref(x) for x in args.suite_receipt]
    return refs, {"tested-commit": gate.get("repository-basis-finish", {}).get("head"),
                  "fence-receipt-age-at-gate-start-seconds": age,
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
    refs = [file_ref(x) for x in (args.evidence, args.manifest, args.journal, args.outcomes)]
    return refs, {"restored-targets": sorted(outcome_targets),
                  "attestation-coverage": "not-claimed"}


VALIDATORS = {"quiescence": evidence_quiescent, "fence-held": evidence_fence,
              "tested-commit": evidence_tested, "reload-recorded": evidence_reload,
              "click-issued": evidence_click_issued, "click-terminal": evidence_click_terminal,
              "certified": evidence_certified, "restored": evidence_restored}


def context(rows):
    result = {"fence-id": rows[0]["fence-id"]}
    for row in rows: result.update(row.get("facts", {}))
    return result


def main(argv=None):
    parser = argparse.ArgumentParser()
    sub = parser.add_subparsers(dest="command", required=True)
    init = sub.add_parser("init"); init.add_argument("--ledger", required=True); init.add_argument("--fence-id", required=True)
    advance = sub.add_parser("advance"); advance.add_argument("--ledger", required=True); advance.add_argument("--to", required=True, choices=ORDER[1:]); advance.add_argument("--evidence"); advance.add_argument("--attestations"); advance.add_argument("--fence-evidence"); advance.add_argument("--suite-receipt", action="append", default=[]); advance.add_argument("--manifest"); advance.add_argument("--journal"); advance.add_argument("--outcomes"); advance.add_argument("--key-file")
    status = sub.add_parser("status"); status.add_argument("--ledger", required=True)
    args = parser.parse_args(argv)
    try:
        if args.command == "init":
            require(not Path(args.ledger).exists(), "state-ledger-already-exists")
            body = {"schema": SCHEMA, "previous-receipt-sha256": None,
                    "state": "initial", "fence-id": args.fence_id,
                    "transitioned-at": now().isoformat(), "evidence": [], "facts": {}}
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
