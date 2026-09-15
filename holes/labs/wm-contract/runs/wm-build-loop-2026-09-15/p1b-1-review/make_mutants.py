#!/usr/bin/env python3
"""claude-2 P1b-1 review: named wrong implementations of work_target_store.clj.
A replacement must match exactly once unless marked 'all' (then at least once),
so no mutant can silently be the original. EXPECT says whether the committed
tests should kill it; 'survive' marks a known reach limit, reported as such."""
import pathlib, sys

SRC = pathlib.Path("/home/joe/code/futon2/src/futon2/aif/work_target_store.clj")
OUT = pathlib.Path(__file__).resolve().parent / "mutants"
orig = SRC.read_text()

MUTANTS = {
    "S01-missing-head-reads-established": ("kill", [
        ("{:status (cond established :damaged", "{:status (cond established :established-no-snapshots")]),
    "S02-no-stale-predecessor": ("kill", [
        ("(not= expected-head (:head current)) {:status :stale-predecessor",
         "false {:status :stale-predecessor")]),
    "S03-no-idempotent-lookup": ("kill", [
        ("(when (= (:id operation) (get-in entry [:snapshot :operation :id])) entry)",
         "(when false entry)")]),
    "S04-intent-not-compared": ("kill", [
        ("(if (= intent (get-in previous [:snapshot :committed-intent-sha256]))", "(if true")]),
    "S05-non-atomic-move": ("survive", [
        ("StandardCopyOption/ATOMIC_MOVE", "StandardCopyOption/REPLACE_EXISTING", "all")]),
    "S06-no-snapshot-dir-fsync": ("kill", [
        ('(force-directory! (path store "snapshots"))', "nil")]),
    "S07-refs-must-equal-head": ("kill", [
        ("(> (:seq ref) (get-in current [:head :seq])) {:status :reference-refused :reason :ahead-of-head}",
         "(not= (:seq ref) (get-in current [:head :seq])) {:status :reference-refused :reason :ahead-of-head}")]),
    "S08-commit-skips-validator": ("kill", [
        ("(let [_ (payload-valid! store payload)", "(let [_ nil")]),
    "S09-prepared-artifacts-adopted": ("kill", [
        ("pending? (or (seq extras) (seq preps) (> (count snaps) (:seq h)))", "pending? false")]),
    "S10-incomplete-init-reads-established": ("kill", [
        ("(and init (empty? snaps)) :initialization-incomplete",
         "(and init (empty? snaps)) :established-no-snapshots")]),
    "S11-orphans-read-as-pristine": ("kill", [
        ("{:status (if (or (seq records) (seq snaps) (seq preps)) :pending-recovery :model-not-established)",
         "{:status (if false :pending-recovery :model-not-established)")]),
    "S12-filelock-only-no-mutex": ("kill", [
        ("(locking monitor\n    (with-open", "(do monitor\n    (with-open")]),
    "S13-no-previous-hash-check": ("kill", [
        ("(require! (= (:previous-sha256 s) (:snapshot-sha256 prev)) :hash-mismatch)", "nil")]),
    "S14-duplicate-op-in-chain-allowed": ("survive", [
        ("(require! (not (contains? ids (get-in s [:operation :id]))) :duplicate-operation-id)", "nil")]),
    "S15-empty-head-envelope-unchecked": ("survive", [
        ("(require! (= h (empty-head g gh)) :head-envelope)", "nil")]),
    # Reverts the review fix: temp preparations parsed as authority again.
    "S17-temp-parsed-as-authority": ("kill", [
        ('prep? #(str/ends-with? (fname %) ".tmp")', "prep? (constantly false)")]),
    "S16-no-expected-head-check": ("kill", [
        ("(require! (= (:expected-head s) prev) :expected-head-mismatch)", "nil")]),
}

OUT.mkdir(parents=True, exist_ok=True)
for name, (expect, reps) in MUTANTS.items():
    text = orig
    for rep in reps:
        old, new = rep[0], rep[1]
        n = text.count(old)
        if (len(rep) == 3 and n < 1) or (len(rep) == 2 and n != 1):
            sys.exit(f"{name}: pattern matched {n} times: {old!r}")
        text = text.replace(old, new)
    (OUT / f"{name}.clj").write_text(text)
    (OUT / f"{name}.expect").write_text(expect)
    print("wrote", name, expect)
