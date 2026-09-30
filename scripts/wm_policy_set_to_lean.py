#!/usr/bin/env python3
"""Generate the reading half of a CascadeSpec example from the published JSON."""
import argparse, hashlib, json
from pathlib import Path


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("target")
    ap.add_argument("--json", default="/var/www/zone.hyperreal.enterprises/2026-09-30-what-a-cascade-is.json")
    ap.add_argument("--output", default="../mathlib4/DarkTower/WarMachine/CascadeSpecExamples.lean")
    ap.add_argument("--script-commit", required=True)
    args = ap.parse_args()
    raw = Path(args.json).read_bytes()
    data = json.loads(raw)
    target = next((t for t in data["targets"] if t["target"] == args.target), None)
    if target is None:
        raise SystemExit(f"target not found: {args.target}")
    patterns = sorted({p for f in target["fragments"] for p in f["refs"]} |
                      {u["pattern"] for p in target["policies"] for u in p["units"]})
    number = {p: i for i, p in enumerate(patterns)}
    table = "\n".join(f"  {i} = {p}" for p, i in number.items())
    fragments = []
    for f in target["fragments"]:  # retain empty fragments: analysis->cascades skips them later
        refs = ", ".join(f"⟨{number[p]}, by decide⟩" for p in f["refs"])
        fragments.append(f"    ⟨[{refs}]⟩")
    body = f'''import DarkTower.WarMachine.CascadeSpec

/-! GENERATED FILE — DO NOT EDIT.
Source: {args.json}
Source SHA-256: {hashlib.sha256(raw).hexdigest()}
Generator: futon2/scripts/wm_policy_set_to_lean.py
Generator commit: {args.script_commit}
Target: {args.target}

Pattern numbering:
{table}
-/

namespace DarkTower.WarMachine.CascadeSpecExamples
open DarkTower.WarMachine.CascadeSpec

def realLibrary : Library := {{{", ".join(map(str, range(len(patterns))))}}}

/-- Empty source fragments are retained; `supportedUnits` performs the same
skip as `analysis->cascades`. -/
def realReading : Reading realLibrary :=
  ⟨[
{",\n".join(fragments)}
  ]⟩

def counts (c : Cascade realLibrary) : Nat × Nat × Nat :=
  (c.units.card, c.precedes.card, c.overlap.card)

theorem real_alternatives_counts :
    (readingCascades .alternatives realReading).map counts = [(3, 2, 0)] := by
  simp +decide [readingCascades, readingFactsB, rawReadingCascades, realReading,
    realLibrary, supportedUnits, fragmentUnitsAux, unitsAt, unitsAtAux, alternatives,
    cascadeOfUnits?, counts, directedEdges, adjacentFragments, overlapPairs, readingFragment]

theorem real_overlap_counts :
    (readingCascades .overlap realReading).map counts = [(3, 2, 0)] := by
  simp +decide [readingCascades, readingFactsB, rawReadingCascades, realReading,
    realLibrary, supportedUnits, fragmentUnitsAux, unitsAt, unitsAtAux, cascadeOfUnits?,
    counts, directedEdges, adjacentFragments, overlapPairs, readingFragment]

theorem real_reading_reported_count : (allReadingCascades realReading).length = 2 := by
  have ha := congrArg List.length real_alternatives_counts
  have ho := congrArg List.length real_overlap_counts
  simpa [allReadingCascades] using congrArg₂ (· + ·) ha ho

theorem real_reading_structurally_distinct_count :
    (structuralDedup (allReadingCascades realReading)).length = 1 := by
  simp +decide [allReadingCascades, readingCascades, readingFactsB, rawReadingCascades,
    realReading, realLibrary, supportedUnits, fragmentUnitsAux, unitsAt, unitsAtAux,
    alternatives, cascadeOfUnits?, structuralDedup, structurallyDifferent,
    Cascade.structuralIdentity, directedEdges, adjacentFragments, overlapPairs, readingFragment]

#print axioms real_alternatives_counts
#print axioms real_overlap_counts
#print axioms real_reading_reported_count
#print axioms real_reading_structurally_distinct_count

end DarkTower.WarMachine.CascadeSpecExamples
'''
    Path(args.output).write_text(body)


if __name__ == "__main__":
    main()
