#!/usr/bin/env python3
"""claude-2 P1a review: generate named wrong implementations of
work_target_belief.clj. Each replacement must match exactly once, so a mutant
cannot silently be the original."""
import pathlib, sys

SRC = pathlib.Path("/home/joe/code/futon2/src/futon2/aif/work_target_belief.clj")
OUT = pathlib.Path(__file__).resolve().parent / "mutants"
orig = SRC.read_text()

MUTANTS = {
    # control 1: uniform doubles instead of the declaration's exact 1/7 ratios
    "M1-uniform-doubles": [(
        "(assoc-in [:belief target] (:masses d))",
        "(assoc-in [:belief target] (zipmap (keys (:masses d)) (repeat (/ 1.0 7))))")],
    # control 2: reconcile-belief-carry style drop of targets not admitted this tick
    "M2-drop-unadmitted": [(
        "previous {:belief {} :lineage {}})",
        "(-> previous (update :belief select-keys (keys (:admitted admission-result)))"
        " (update :lineage select-keys (keys (:admitted admission-result))))"
        " {:belief {} :lineage {}})")],
    # control 4: unreadable / missing-after-genesis predecessor treated as a cold start
    "M3-cold-start-on-unreadable": [
        ("(#{:missing-after-genesis :unreadable} (:status predecessor))", "false"),
        ("(not (#{:present :established-no-snapshots} (:status predecessor)))", "false")],
    # F1 fix: an undeclared endpoint alias admitted as a second belief entity
    "M10-alias-admitted": [(
        "(and (some? id) (not= id target)) :target-alias-undeclared",
        "false :target-alias-undeclared")],
    # control 3: re-admission resets to D
    "M4-readmission-resets": [(
        "(if (contains? retained target)", "(if false")],
    # control 6: registered-not-admitted collapsed into outside-registry
    "M5-adapter-collapse": [(
        "(not seen?) (refusal :registered-not-admitted",
        "(not seen?) (refusal :entity-outside-registry")],
    # control 9: declaration pin not enforced
    "M6-no-declaration-pin": [
        ("(not= declaration-sha256\n                (sha256 (.getBytes ^String text StandardCharsets/UTF_8))))",
         "false)"),
        ("(if (= declaration-sha256 (sha256 bytes))", "(if true")],
    # control 8: any candidate type admitted
    "M7-any-type-admitted": [(
        "(not (#{:advance-mission :advance-ticket} type))", "false")],
    # control 5: half-present target re-introduced when admitted
    "M8-half-present-reintroduced": [(
        "retained (set/union (set (keys (:belief state)))\n                                (set (keys (:lineage state))))",
        "retained (set/difference (set/union (set (keys (:belief state)))\n"
        "                                (set (keys (:lineage state)))) broken)")],
    # control 10: an update lineage is accepted
    "M9-update-lineage-accepted": [(
        "(not= :no-admitted-observations (:updates %))", "false")],
}

OUT.mkdir(parents=True, exist_ok=True)
for name, reps in MUTANTS.items():
    text = orig
    for old, new in reps:
        n = text.count(old)
        if n != 1:
            sys.exit(f"{name}: pattern matched {n} times: {old!r}")
        text = text.replace(old, new)
    (OUT / f"{name}.clj").write_text(text)
    print("wrote", name)
