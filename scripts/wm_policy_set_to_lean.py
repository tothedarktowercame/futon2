#!/usr/bin/env python3
"""Generate a CascadeSpec example from one target in the published JSON."""
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
    retractions = [p for p in target["policies"] if p["kind"] == "retraction"]
    graph_rows = {}
    for policy in retractions:
        for edge in policy["edges"]:
            key = tuple(sorted((edge["from"], edge["to"])))
            direction = None if edge["kind"] == "overlap" else (edge["from"], edge["to"])
            prior = graph_rows.get(key)
            if prior is not None and prior != direction:
                raise SystemExit(f"inconsistent graph direction for {key}")
            graph_rows[key] = direction
    patterns = sorted({p for f in target["fragments"] for p in f["refs"]} |
                      {p for edge in graph_rows for p in edge})
    number = {p: i for i, p in enumerate(patterns)}
    table = "\n".join(f"  {i} = {p}" for p, i in number.items())
    fragments = []
    for f in target["fragments"]:  # retain empty fragments: analysis->cascades skips them later
        refs = ", ".join(f"⟨{number[p]}, by decide⟩" for p in f["refs"])
        fragments.append(f"    ⟨[{refs}]⟩")
    graph_edges = []
    for (left, right), direction in sorted(graph_rows.items()):
        authored = "none" if direction is None else f"some ({number[direction[0]]}, {number[direction[1]]})"
        graph_edges.append(
            f"    ⟨{number[left]}, {number[right]}, by decide, (1 : ℚ), {authored}, by simp⟩")

    retraction_defs = []
    retraction_names = []
    axiom_names = ["real_reading_structurally_distinct_count"]
    for ri, policy in enumerate(retractions):
        name = f"realRetraction{ri + 1}"
        retraction_names.append(name)
        unit_names = {}
        unit_defs = []
        for ui, unit in enumerate(policy["units"]):
            uname = f"{name}Unit{ui}"
            unit_names[unit["id"]] = uname
            pattern = number.get(unit["pattern"])
            if pattern is None:
                # Deliberately emit an impossible membership proof: the planted
                # outside-library control must be rejected by Lean.
                pattern = max(number.values(), default=-1) + 1
            unit_defs.append(
                f"def {uname} : Unit realLibrary :=\n"
                f"  ⟨{ui}, {pattern}, by decide, none⟩")
        overlap_terms = []
        precedes_terms = []
        for edge in policy["edges"]:
            a, b = unit_names[edge["from"]], unit_names[edge["to"]]
            if edge["kind"] == "overlap":
                ai = next(i for i, u in enumerate(policy["units"]) if u["id"] == edge["from"])
                bi = next(i for i, u in enumerate(policy["units"]) if u["id"] == edge["to"])
                if bi < ai:
                    a, b = b, a
                overlap_terms.append(f"⟨{a}, {b}, by decide⟩")
            else:
                precedes_terms.append(f"({a}, {b})")
        units = ", ".join(unit_names[u["id"]] for u in policy["units"])
        overlaps = ", ".join(overlap_terms)
        precedes = ", ".join(precedes_terms)
        defs = "\n\n".join(unit_defs)
        retraction_defs.append(f'''{defs}

def {name} : Cascade realLibrary where
  units := {{{units}}}
  nonempty := by simp
  precedes := {{{precedes}}}
  overlap := {{{overlaps}}}
  labelsInLibrary := fun u _ => u.inLibrary
  precedesEndpoints := by simp
  overlapEndpoints := by simp
  rank := fun u => u.id
  precedesForward := by simp [{", ".join(unit_names.values())}]

theorem {name}_contains_usable_seeds :
    ∀ seed ∈ realReading.usableSeeds realGraph, {name}.containsPattern seed := by
  simp [Reading.usableSeeds, Reading.citedPatterns, PatternGraph.incident,
    Cascade.containsPattern, realReading, realGraph, {name},
    {", ".join(unit_names.values())}]

theorem {name}_precedes_authored :
    ∀ e ∈ {name}.precedes, authoredGraphEdge realGraph e.1.pattern e.2.pattern := by
  simp [{name}]

theorem {name}_overlap_unauthored :
    ∀ e ∈ {name}.overlap, overlapGraphEdge realGraph e.left.pattern e.right.pattern := by
  simp [overlapGraphEdge, realGraph, {name}, {", ".join(unit_names.values())}]''')
        axiom_names.extend([
            f"{name}_contains_usable_seeds",
            f"{name}_precedes_authored",
            f"{name}_overlap_unauthored",
        ])
    body = f'''import DarkTower.WarMachine.CascadeSpec

/-! GENERATED FILE — DO NOT EDIT.
Source: {args.json}
Source SHA-256: {hashlib.sha256(raw).hexdigest()}
Generator: futon2/scripts/wm_policy_set_to_lean.py
Generator commit: {args.script_commit}
Target: {args.target}
Theorems using `native_decide`: `real_reading_structurally_distinct_count`,
`real_policy_members_count`.

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

/-- The export records graph kind but not numeric weight. Weight is set to one;
none of the provenance theorems observes it. -/
def realGraph : PatternGraph realLibrary where
  edges := {{
{",\n".join(graph_edges)}
  }}
  endpointsInLibrary := by simp [realLibrary]

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
    alternatives, cascadeOfUnits?, directedEdges, adjacentFragments, overlapPairs,
    readingFragment]
  rw [structuralDedup.eq_def]
  native_decide

{"\n\n".join(retraction_defs)}

def realRetractions : List (Cascade realLibrary) :=
  [{", ".join(retraction_names)}]

theorem real_policy_members_count :
    (structuralDedup (allReadingCascades realReading ++ realRetractions)).length = 4 := by
  simp +decide [allReadingCascades, readingCascades, readingFactsB, rawReadingCascades,
    realReading, realLibrary, supportedUnits, fragmentUnitsAux, unitsAt, unitsAtAux,
    alternatives, cascadeOfUnits?, directedEdges, adjacentFragments, overlapPairs,
    readingFragment, realRetractions]
  rw [structuralDedup.eq_def]
  native_decide

#print axioms real_alternatives_counts
#print axioms real_overlap_counts
#print axioms real_reading_reported_count
#print axioms real_reading_structurally_distinct_count
{"\n".join(f"#print axioms {n}" for n in axiom_names[1:])}
#print axioms real_policy_members_count

end DarkTower.WarMachine.CascadeSpecExamples
'''
    Path(args.output).write_text(body)


if __name__ == "__main__":
    main()
