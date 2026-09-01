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

POPULATION_BOUNDARY = {
    "boundary/type": "declared-not-derived",
    "subject": "live-artifact-format-catalogue",
    "pinned": "named-live-generators-and-recognised-source-proof-shapes",
    "not-pinned": "all-publication-generators-and-lossy-aggregation-boundaries",
    "derivation-status": "not-exactly-derivable",
    "reason": "semantic-and-interprocedural-format-boundaries-are-not-a-syntactic-population",
}

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
     "proof-kind": "clojure-map-vocabulary-before-format",
     "detail": "classification counts reach %d without required-key/non-nil proof"},
    {"generator": "lane-campaign", "id": "blank-active-identities",
     "unsafe": r"active\?\s+\(every\?\s+some\?\s+values\)",
     "proof": r"str/blank\?[^\n]*(holding|job-id)",
     "detail": "non-nil permits blank holding/job values to render as empty cells"},
    {"generator": "model-coverage", "id": "empty-population-to-zero",
     "unsafe": r"rows\s*=\s*reg(?:\[\"rows\"\]|\.get\(\"rows\"\))",
     "proof": r"if\s+not\s+isinstance\(rows,\s*list\)\s+or\s+not\s+rows|if\s+not\s+rows\s*:|assert\s+rows\b",
     "detail": "empty authoritative rows render a plausible all-zero table"},
    {"generator": "q-interface", "id": "missing-as-of-to-empty",
     "unsafe": r"\(tex\s+\(:as-of\s+data\)\)",
     "proof": r"\(string\?\s+\(:as-of\s+data\)\)",
     "detail": "nil :as-of is stringified into a blank publication stamp"},
    {"generator": "variable-situation", "id": "unreconciled-cell-population",
     "unsafe": r"cell\s+\(fn\s+\[c\s+p\]",
     "proof": r"cell-total|row accounting broken",
     "detail": "unknown/missing pointer status disappears from both columns"},
    {"generator": "war-room", "id": "default-zero-and-null-metrics",
     "unsafe": r"\(get\s+st\s+:repaired\s+0\)",
     "proof-kind": "clojure-membership-before-aggregation",
     "collection": "insts", "key": "status",
     "detail": "missing categories default to zero and unchecked workflow numerics reach %d"},
    {"generator": "workflow-report", "id": "missing-lane-to-idle",
     "unsafe": r"\(get\s+holdings\s+lane\)",
     "proof": r"required-lanes[^\n]*set|when-not\s+\(=\s+\(set\s+lanes\)\s+registry-lanes\)",
     "detail": "missing registry lane becomes idle/open=0 in the generated report"},
]


def line_for(text: str, match: re.Match[str]) -> int:
    return text.count("\n", 0, match.start()) + 1


def without_comments(text: str) -> str:
    """Remove line comments so declarations of proof cannot satisfy the lint."""
    return "\n".join(line.split(";", 1)[0] for line in text.splitlines())


def balanced_form(text: str, start: int) -> str | None:
    """Return one balanced Clojure form, ignoring strings sufficiently for lint input."""
    depth, quoted, escaped = 0, False, False
    for pos in range(start, len(text)):
        char = text[pos]
        if quoted:
            if escaped:
                escaped = False
            elif char == "\\":
                escaped = True
            elif char == '"':
                quoted = False
            continue
        if char == '"':
            quoted = True
        elif char == "(":
            depth += 1
        elif char == ")":
            depth -= 1
            if depth == 0:
                return text[start:pos + 1]
    return None


def clojure_membership_before_aggregation(text: str, collection: str, key: str) -> bool:
    """Recognise a bounded executable categorical-population proof.

    The exact collection/key later aggregated must first be traversed, every
    value must be checked against a literal finite set, and rejection must be
    loud in the same doseq form. This intentionally does not infer helper or
    interprocedural validation.
    """
    code = without_comments(text)
    aggregate = re.search(
        rf"\(frequencies\s+\(map\s+:{re.escape(key)}\s+{re.escape(collection)}\)\)",
        code,
    )
    if not aggregate:
        return False
    prefix = code[:aggregate.start()]
    starts = list(re.finditer(
        rf"\(doseq\s+\[([\w-]+)\s+{re.escape(collection)}\]", prefix
    ))
    for start_match in reversed(starts):
        form = balanced_form(prefix, start_match.start())
        if not form:
            continue
        var = re.escape(start_match.group(1))
        membership = re.search(
            rf"\(when-not\s+\(\#\{{[^}}]+\}}\s+\(:{re.escape(key)}\s+{var}\)\)",
            form,
            re.S,
        )
        loud = re.search(r"\((?:die|throw)\b", form)
        if membership and loud:
            return True
    return False


def rule_proved(rule: dict, text: str) -> bool:
    if rule.get("proof-kind") == "clojure-map-vocabulary-before-format":
        code = without_comments(text)
        format_pos = code.find("(get-in org [:classification-counts :transition])")
        if format_pos < 0:
            return False
        prefix = code[:format_pos]
        # Bounded recognition of an alias for the exact source map followed by
        # presence, numeric type, and closed-vocabulary rejection. All four
        # failures must be loud before the formatter.
        required = [
            r"\[counts\s+\(:classification-counts\s+org\)\]",
            r"\(when-not\s+\(map\?\s+counts\)",
            r"\(when-not\s+\(every\?\s+some\?\s+\(map\s+counts\s+classification-keys\)\)",
            r"\(when-not\s+\(every\?\s+integer\?\s+\(map\s+counts\s+classification-keys\)\)",
            r"\(remove\s+\(set\s+classification-keys\)\s+\(keys\s+counts\)\)",
        ]
        return all(re.search(pattern, prefix, re.S) for pattern in required) and \
            len(re.findall(r"\(System/exit\s+1\)", prefix)) >= 4
    if rule.get("proof-kind") == "clojure-membership-before-aggregation":
        return clojure_membership_before_aggregation(
            text, rule["collection"], rule["key"]
        )
    return bool(re.search(rule["proof"], without_comments(text), re.S | re.I))


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
            proved = rule_proved(rule, text)
            if unsafe and not proved:
                findings.append({"generator": name, "path": str(path),
                                 "line": line_for(text, unsafe),
                                 "finding": rule["id"], "detail": rule["detail"],
                                 "proof-required": (rule.get("proof") or
                                                    rule.get("proof-kind"))})
    return {"schema": "live-artifact-format-boundary-lint/v1",
            "population-boundary": POPULATION_BOUNDARY,
            "generators": len(GENERATORS), "findings": findings,
            "finding-count": len(findings), "unavailable": unavailable,
            "clean-generators": sorted(set(GENERATORS) -
                                       {f["generator"] for f in findings} -
                                       {u["generator"] for u in unavailable}),
            "limits": [
                "bounded to the generators declared in README-live-artifacts.md",
                "recognises explicit source proofs, not arbitrary interprocedural data flow",
                "comments and FORMAT-PROOF markers are never accepted as proof",
                "a proof hidden behind a helper remains review-required until this lint learns its executable shape",
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
    marker_only = ';; FORMAT-PROOF count\n(format "%d" (:count row))'
    safe = '(assert (some? (:count row)))\n(format "%d" (:count row))'
    result = {"unsafe-flagged": format_probe(unsafe) == ["count"],
              "marker-only-flagged": format_probe(marker_only) == ["count"],
              "proved-safe-not-flagged": format_probe(safe) == []}
    unproved_population = """
(def insts source)
;; repair-status-population-valid
(def st (frequencies (map :status insts)))
(get st :repaired 0)
"""
    proved_population = """
(def insts source)
(doseq [i insts]
  (when-not (#{:repaired :partial :open} (:status i))
    (die "unknown status")))
(def st (frequencies (map :status insts)))
(get st :repaired 0)
"""
    wrong_collection = """
(doseq [i other-insts]
  (when-not (#{:repaired :partial :open} (:status i)) (die "bad")))
(def st (frequencies (map :status insts)))
(get st :repaired 0)
"""
    topology_marker_only = """
;; FORMAT-PROOF classification-counts
(get-in org [:classification-counts :transition])
"""
    result.update({
        "population-marker-only-flagged":
            not clojure_membership_before_aggregation(unproved_population, "insts", "status"),
        "membership-proof-accepted":
            clojure_membership_before_aggregation(proved_population, "insts", "status"),
        "wrong-collection-proof-rejected":
            not clojure_membership_before_aggregation(wrong_collection, "insts", "status"),
        "current-war-room-proof-accepted":
            clojure_membership_before_aggregation(
                GENERATORS["war-room"].read_text(), "insts", "status"),
        "current-live-topology-proof-accepted":
            rule_proved(RULES[0], GENERATORS["live-topology"].read_text()),
        "topology-marker-only-rejected":
            not rule_proved(RULES[0], topology_marker_only),
    })
    passed = all(result.values())
    print(json.dumps(result, sort_keys=True))
    print("live-artifact-format-boundary-lint:", "PASS" if passed else "FAIL",
          "negative-control exit-convention=0-pass/2-control-slipped")
    return 0 if passed else 2


def main() -> int:
    parser = argparse.ArgumentParser()
    parser.add_argument("--negative-control", action="store_true")
    parser.add_argument("--report", action="store_true",
                        help="report findings but fail only when the lint is unavailable")
    args = parser.parse_args()
    if args.negative_control:
        return negative_control()
    result = lint()
    print(json.dumps(result, indent=2, sort_keys=True))
    if result["unavailable"]:
        print("live-artifact-format-boundary-lint: UNAVAILABLE", file=sys.stderr)
        return 2
    # Findings are report-only in the workspace gate until their owners repair
    # them. Direct invocation remains red so the population cannot be mistaken
    # for extinction.
    mode = "REPORT" if args.report else "FINDINGS"
    print(f"live-artifact-format-boundary-lint: {mode} findings={result['finding-count']}")
    # Exit 3 is a truthful census verdict: the instrument ran, its self-test is
    # separately blocking, and findings remain. Exit 0 continues to mean clean.
    return (3 if result["findings"] else 0) if args.report else (1 if result["findings"] else 0)


if __name__ == "__main__":
    raise SystemExit(main())
