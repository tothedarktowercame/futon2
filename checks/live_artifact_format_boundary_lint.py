#!/usr/bin/env python3
"""Lint live-paper generators for unproved values at format/aggregate boundaries.

This is deliberately a bounded contract lint, not a Python/Clojure theorem
prover.  Each rule names (a) a source shape that can coerce absence and (b) the
source-level proof which retires it.  Unsupported data-flow is reported as a
finding until the generator adds an explicit validation or reconciliation.
"""
from __future__ import annotations

import argparse
import json
import re
import sys
from pathlib import Path

P4NG = Path("/home/joe/code/p4ng")
GEN = P4NG / "empirics-futon"

GENERATORS = {
    "live-topology": GEN / "gen_live_topology.bb",
    "lane-campaign": GEN / "gen_lane_campaign_table.bb",
    "model-coverage": GEN / "gen_model_coverage.py",
    "q-interface": GEN / "gen_q_interface_table.bb",
    "variable-situation": GEN / "gen_variable_situation_table.bb",
    "war-room": GEN / "gen_war_room_tetrahedron.bb",
    "defect-tally": GEN / "make_defect_tally_figure.py",
    "workflow-report": GEN / "gen_workflow_report.bb",
}

# A rule remains a finding while its unsafe carrier exists and its explicit
# proof does not.  Proofs are intentionally concrete: a vague `validate`
# function elsewhere in a file does not prove a particular rendered value.
RULES = [
    {"generator": "live-topology", "id": "classification-counts-before-format",
     "unsafe": r"classification-counts\s+:transition",
     "proof": r"FORMAT-PROOF\s+classification-counts|every\?\s+some\?[^\n]*classification-counts",
     "detail": "classification counts reach %d without required-key/non-nil proof"},
    {"generator": "lane-campaign", "id": "blank-active-identities",
     "unsafe": r"active\?\s+\(every\?\s+some\?\s+values\)",
     "proof": r"FORMAT-PROOF\s+active-identities|str/blank\?[^\n]*(holding|job-id)",
     "detail": "non-nil permits blank holding/job values to render as empty cells"},
    {"generator": "model-coverage", "id": "empty-population-to-zero",
     "unsafe": r"rows\s*=\s*reg(?:\[\"rows\"\]|\.get\(\"rows\"\))",
     "proof": r"FORMAT-PROOF\s+nonempty-rows|if\s+not\s+isinstance\(rows,\s*list\)\s+or\s+not\s+rows|if\s+not\s+rows\s*:|assert\s+rows\b",
     "detail": "empty authoritative rows render a plausible all-zero table"},
    {"generator": "q-interface", "id": "missing-as-of-to-empty",
     "unsafe": r"\(tex\s+\(:as-of\s+data\)\)",
     "proof": r"FORMAT-PROOF\s+as-of|\(string\?\s+\(:as-of\s+data\)\)",
     "detail": "nil :as-of is stringified into a blank publication stamp"},
    {"generator": "variable-situation", "id": "unreconciled-cell-population",
     "unsafe": r"cell\s+\(fn\s+\[c\s+p\]",
     "proof": r"FORMAT-PROOF\s+cell-population|cell-total|row accounting broken",
     "detail": "unknown/missing pointer status disappears from both columns"},
    {"generator": "war-room", "id": "default-zero-and-null-metrics",
     "unsafe": r"\(get\s+st\s+:repaired\s+0\)",
     "proof": r"FORMAT-PROOF\s+war-room-metrics|repair-status-population-valid",
     "detail": "missing categories default to zero and unchecked workflow numerics reach %d"},
    {"generator": "workflow-report", "id": "missing-lane-to-idle",
     "unsafe": r"\(get\s+holdings\s+lane\)",
     "proof": r"FORMAT-PROOF\s+four-lane-population|required-lanes[^\n]*set",
     "detail": "missing registry lane becomes idle/open=0 in the generated report"},
]


def line_for(text: str, match: re.Match[str]) -> int:
    return text.count("\n", 0, match.start()) + 1


def lint() -> dict:
    findings, unavailable = [], []
    for name, path in GENERATORS.items():
        try:
            text = path.read_text()
        except OSError as exc:
            unavailable.append({"generator": name, "path": str(path), "error": str(exc)})
            continue
        for rule in (r for r in RULES if r["generator"] == name):
            unsafe = re.search(rule["unsafe"], text, re.S)
            proved = re.search(rule["proof"], text, re.S | re.I)
            if unsafe and not proved:
                findings.append({"generator": name, "path": str(path),
                                 "line": line_for(text, unsafe),
                                 "finding": rule["id"], "detail": rule["detail"],
                                 "proof-required": rule["proof"]})
    return {"schema": "live-artifact-format-boundary-lint/v1",
            "generators": len(GENERATORS), "findings": findings,
            "finding-count": len(findings), "unavailable": unavailable,
            "clean-generators": sorted(set(GENERATORS) -
                                       {f["generator"] for f in findings} -
                                       {u["generator"] for u in unavailable}),
            "limits": [
                "bounded to the generators declared in README-live-artifacts.md",
                "recognises explicit source proofs, not arbitrary interprocedural data flow",
                "a proof hidden behind a helper needs a FORMAT-PROOF marker plus its own test",
                "categorical reconciliation and expected populations remain generator-specific",
            ]}


def format_probe(text: str) -> list[str]:
    """Small falsifier for the core same-scope format proof."""
    findings = []
    for match in re.finditer(r'\(format\s+"[^"]*%d[^"]*"\s+\(:([\w-]+)\s+\w+\)\)', text):
        key = match.group(1)
        prefix = text[max(0, match.start() - 300):match.start()]
        proof = re.search(rf'\((?:assert|when-not)\s+\(some\?\s+\(:{re.escape(key)}\s+\w+\)\)', prefix)
        if not proof:
            findings.append(key)
    return findings


def negative_control() -> int:
    unsafe = '(format "%d" (:count row))'
    safe = '(assert (some? (:count row)))\n(format "%d" (:count row))'
    result = {"unsafe-flagged": format_probe(unsafe) == ["count"],
              "proved-safe-not-flagged": format_probe(safe) == []}
    passed = all(result.values())
    print(json.dumps(result, sort_keys=True))
    print("live-artifact-format-boundary-lint:", "PASS" if passed else "FAIL",
          "negative-control exit-convention=0-pass/2-control-slipped")
    return 0 if passed else 2


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--negative-control", action="store_true")
    args = parser.parse_args()
    if args.negative_control:
        return negative_control()
    result = lint()
    print(json.dumps(result, indent=2, sort_keys=True))
    if result["unavailable"]:
        print("live-artifact-format-boundary-lint: UNAVAILABLE", file=sys.stderr)
        return 2
    # Findings are the expected live state until the seven owners repair them.
    print(f"live-artifact-format-boundary-lint: FINDINGS {result['finding-count']}")
    return 1 if result["findings"] else 0


if __name__ == "__main__":
    raise SystemExit(main())
