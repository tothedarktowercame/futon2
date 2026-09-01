#!/usr/bin/env python3
"""Bounded lint for acceptance over an absent or empty verification subject.

This is intentionally a catalogue of acceptance boundaries, not a claim that
regular expressions prove arbitrary Python or Clojure data flow.  Each rule
names the subject which must exist and the executable proof required before the
site may report success.  Problem accumulators are not subjects: an empty
`problems` collection is a legitimate success only after the subject proof.
"""
from __future__ import annotations

import argparse
import json
import re
import subprocess
import sys
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]

RULES = [
    {"id": "strict-contract-declarations", "path": "checks/contract_lint.clj",
     "accept": r"strict-pass\?", "proof": r"\(empty\?\s+decls\).*zero-declarations",
     "subject": "contract declarations"},
    {"id": "operational-certificate-route", "path": "checks/wm_operational_certificate.clj",
     "accept": r":verdict\s+verdict", "proof": r"route-present\?\s+\(seq\s+route\)",
     "subject": "traversed route"},
    {"id": "lean-positive-source", "path": "checks/lean_positive_witness.clj",
     "accept": r":pass\?\s+\(and", "proof": r"re-find\s+declaration-pattern",
     "subject": "positive Lean declarations"},
    {"id": "restore-manifest-targets", "path": "scripts/writer_fence_restore.py",
     "accept": r"def restore\(", "proof": r"not isinstance\(targets, dict\) or not targets",
     "subject": "restoration targets"},
    {"id": "restore-journal", "path": "scripts/writer_fence_restore.py",
     "accept": r"def restore\(", "proof": r"NOTHING-RECORDED:journal-(?:missing|empty)",
     "subject": "restoration journal"},
    {"id": "fence-receipt-observations", "path": "writer_fence_capability.clj",
     "accept": r"held\?\s+\(empty\?\s+problems\)",
     "proof": r"(?:prior|receipt)-observations-(?:absent|empty)|observation-population-(?:absent|empty)",
     "subject": "prior receipt observations"},
    {"id": "fence-live-observations", "path": "writer_fence_capability.clj",
     "accept": r"held\?\s+\(empty\?\s+problems\)",
     "proof": r"live-observations-(?:absent|empty)|live-observation-population-(?:absent|empty)",
     "subject": "fresh live observations"},
]

# Real pre-repair revisions.  These controls demonstrate reach against history,
# rather than merely against synthetic strings shaped after the implementation.
HISTORICAL = [
    ("2642a98^", "checks/contract_lint.clj", "strict-contract-declarations"),
    ("fbd204c^", "checks/wm_operational_certificate.clj", "historical-incomplete-certificate"),
    ("6591647^", "checks/ambiguity_witness.clj", "historical-empty-lean-baseline"),
    ("adfa555^", "scripts/writer_fence_restore.py", "historical-empty-restoration"),
    ("30dd5b1", "writer_fence_capability.clj", "fence-receipt-observations"),
]


def proved(rule: dict, text: str) -> bool:
    return bool(re.search(rule["proof"], text, re.S))


def scan() -> dict:
    findings, unavailable = [], []
    for rule in RULES:
        path = ROOT / rule["path"]
        try:
            text = path.read_text()
        except OSError as exc:
            unavailable.append({"path": rule["path"], "error": str(exc)})
            continue
        if re.search(rule["accept"], text, re.S) and not proved(rule, text):
            findings.append({k: rule[k] for k in ("id", "path", "subject")})
    return {"schema": "empty-subject-acceptance-lint/v1", "rules": len(RULES),
            "finding-count": len(findings), "findings": findings,
            "unavailable": unavailable,
            "limits": [
                "bounded to registered acceptance boundaries",
                "does not equate an empty problem accumulator with an empty subject",
                "helper-hidden or novel proof shapes remain unverified until registered",
            ]}


def generic_probe(language: str, text: str) -> bool:
    """Return true when a small candidate has vacuous acceptance."""
    if language == "python":
        danger = bool(re.search(r"\ball\s*\(|\.get\([^\n,]+,\s*True\)", text))
        reject = bool(re.search(r"if\s+not\s+\w+\s*:\s*(?:raise|return\s+False)", text))
    else:
        danger = bool(re.search(r"\(every\?|\(get\s+[^\n]+\s+true\)", text))
        reject = bool(re.search(r"\(and\s+\(seq\s+\w+\)|\(when-not\s+\(seq\s+\w+\)", text))
    return danger and not reject


def historical_controls() -> dict:
    observed = []
    for rev, path, identifier in HISTORICAL:
        p = subprocess.run(["git", "show", f"{rev}:{path}"], cwd=ROOT,
                           text=True, capture_output=True)
        if p.returncode:
            observed.append({"id": identifier, "caught": False, "error": p.stderr.strip()})
            continue
        if identifier in {r["id"] for r in RULES}:
            rule = next(r for r in RULES if r["id"] == identifier)
            caught = not proved(rule, p.stdout)
        elif identifier == "historical-incomplete-certificate":
            caught = "traceWritten" not in p.stdout or "execution-complete?" not in p.stdout
        else:
            # The historical wrappers/restorer accepted an elaborator/no-op
            # without establishing a substantive subject.
            caught = not bool(re.search(r"declaration-pattern|NOTHING-RECORDED", p.stdout))
        observed.append({"id": identifier, "revision": rev, "path": path, "caught": caught})
    probes = {
        "python-all-empty-flagged": generic_probe("python", "return all(ok(x) for x in rows)"),
        "python-explicit-empty-rejection-clean": not generic_probe(
            "python", "if not rows: raise ValueError('empty')\nreturn all(ok(x) for x in rows)"),
        "clojure-every-empty-flagged": generic_probe("clojure", "(every? ok? rows)"),
        "clojure-explicit-nonempty-clean": not generic_probe(
            "clojure", "(and (seq rows) (every? ok? rows))"),
        "truthy-default-flagged": generic_probe("python", "return row.get('verified', True)"),
    }
    passed = all(x["caught"] for x in observed) and all(probes.values())
    out = {"historical": observed, "probes": probes, "pass": passed}
    print(json.dumps(out, indent=2, sort_keys=True))
    return 0 if passed else 2


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--report", action="store_true")
    ap.add_argument("--self-test", action="store_true")
    args = ap.parse_args()
    if args.self_test:
        return historical_controls()
    result = scan()
    print(json.dumps(result, indent=2, sort_keys=True))
    if result["unavailable"]:
        print("empty-subject-acceptance-lint: UNAVAILABLE", file=sys.stderr)
        return 2
    mode = "REPORT" if args.report else "FINDINGS"
    print(f"empty-subject-acceptance-lint: {mode} findings={result['finding-count']}")
    return 0 if args.report else (1 if result["findings"] else 0)


if __name__ == "__main__":
    raise SystemExit(main())
