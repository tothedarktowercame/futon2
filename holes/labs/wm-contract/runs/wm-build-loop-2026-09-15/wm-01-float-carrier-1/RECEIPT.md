# WM-01-float-carrier-1

Author: codex-3. Reviewer: claude-3 (review pending).

## Result

The strengthened FloatCarriedRow enforces duplicate-free support and zero mass
off support. Its unchanged near-normalization bound already implies nonempty
support; `FloatCarriedRow.support_ne_nil` proves this without a new field.
The bound remains exactly `1 / 10^12`, and every pre-existing mass and theorem
statement is unchanged. ProbabilityKernel is unchanged.

`advanceTwiceRow` and `cascadeRow` discharge the two new fields against their
complete 12-constructor support. Their off-support proofs are vacuous because
that support is exhaustive, which satisfies the law. The existing repeat row
is an alias and needs no edit. These are constructed forward-model witnesses;
this packet adds no production observations or composition correspondence claims.

`FloatCarriedRowCorrespondence.retainedRow` represents the seven exact rational
images retained by binding-1. Seven named coordinate theorems preserve those
values. Its support laws and near-normalization are proved, while
`retained_not_exact` proves its sum is not one. This is one represented row,
not a proof of the Clojure reader. Its total remains
`36028797018963969/36028797018963968`, deviation `1/2^55`.

`FloatCarriedRow.toProbabilityKernel` requires the explicit premise
`(r.support.map r.mass).sum = 1`. It casts each rational mass into the reals,
with a list-induction proof that this preserves the sum. The coordinate and
normalization theorems are supplied. There is no coercion, renormalization or
unconditional conversion. Approximate admission alone does not meet the premise.

## Controls and gates

All final runs below have retained raw stdout/stderr and exact argument vectors,
working directories and exits in `logs/*.json`. Commands were not piped.

| Check | Exit | Result |
|---|---:|---|
| Isolated build of the eight affected modules | 0 | All elaborate |
| Canonical build of exactly those eight modules | 0 | Cache brought in line after source commit |
| Positive.lean | 0 | Sparse Fin 3 support [0,1], masses 1/3, 2/3, 0; real off-support outcome 2; unequal exact conversion; retained-row nonempty support |
| Duplicate.lean | 1 expected | `case support_nodup`, `⊢ False`; listed duplicate masses sum to one |
| Hidden.lean | 1 expected | `case mass_eq_zero_of_not_mem.true`, `⊢ False`; positive mass outside support |
| OutsideBound.lean | 1 expected | `case nearNormalised`, `⊢ False`; deviation exactly 2/10^12 |
| RetainedConversion.lean | 1 expected | `case exact_total_one_premise`, `⊢ False`; existing retained carrier supplies every other obligation |
| Axioms.lean | 0 | New/changed carrier and correspondence declarations |
| Row16ForwardModelAxiomCheck | 0 | Rebuilt and executed in both eight-module builds |
| machine-model-test | 0 | 11 tests, 319 assertions, zero failures/errors |
| Python helper syntax | 0 | compile(), no bytecode caches |

Each invalid carrier supplies all other fields. The failed conversion takes the
already constructed retainedRow; only the exact-total premise is unprovable.
The bound and both support laws distinguish the negative cases independently.

Final positive-control and axiom logs contain only propext, Classical.choice
and Quot.sound. The Row16 probe also reports only those axioms for the existing
constructors, composition statements, inheritedExcess and productionPinnedFloatCarried.
No accepted changed declaration uses sorry or a new axiom.

Two development failures are retained explicitly: `isolated-spec` (exit 1)
showed that `exact_mod_cast` alone did not commute the list sum; it was replaced
by an explicit induction. `Positive-attempt1` (exit 1) left a concrete Fin
inequality unsolved; the final decidable proof closes it. Its sorryAx diagnostic
belongs to that failed elaboration, not the accepted control. Build logs replay
nine pre-existing Holes sorry warnings, and an existing unused-simp warning in
MachinePreferenceDistribution. The final axiom reports establish that the
accepted declarations here do not depend on sorryAx.

## Source, environment and cache

The canonical checkout was clean on branch darktower at
`2b22eeef3102b3b48c8c12df66aaded459c36819` before editing. A git archive and a
copied `.lake` provided the isolated workspace. Copy inode separation and all
cache symlink destinations were checked; no mutable link targeted canonical.
Iteration happened only there. The tested bytes were then copied into the three
permitted canonical files and committed without pushing or changing branches.

Only the two known constructors build FloatCarriedRow. The bounded DarkTower
import graph confirms exactly six dependents of MachineModelSpec:
MachineBeliefDistribution, MachineForwardModelWitness, MachineParameters,
MachinePredictiveOutcome, MachinePreferenceDistribution, Row16ForwardModelAxiomCheck.
All elaborate without further source edits. No aggregate/root registration was
needed. The predictive-outcome wrapper's positive and two negative modules do
not depend on MachineModelSpec, so those unaffected negative modes were not run.
The graph evidence and actual wrapper text are retained in `preflight.json`.

The canonical build ran after the source commit with LEAN_NUM_THREADS=1.
`canonical-cache.json` and `logs/canonical-written.stdout` list exactly the 64
files newer than the pre-build marker. All belong to the eight requested
modules; no package or unrelated module outputs were written. The isolated
copy was deleted; `successor-binding.json` records cleanup and the source commit.

## Successor bindings and unchanged runtime

R4-forward-model-float-carried-production-pins-v1 still has its historical
`:admitted` registry record. Its artifact pin was
`02bc5c00a8dc7d5dde2e861d647d805f5023f36eda46a12f4d3ea102d407e34c`;
this edit makes that pin stale. Its Holes subject pin
`4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b`
was already pre-repair. `successor-binding.json` supplies both successor hashes,
the new MachineModelSpec identity and the dependent declarations. It records
a formal witness successor, not renewed acceptance of the historical production
acquisition/composition comparison. The registry is unchanged; codex-28 decides
its update separately.

Binding-1 (`daa263c8`, reviewed `020cba14`) continues to describe historical
`480a666ad2`. Its old source pins remain correct for that subject and do not
describe this new commit. Its pinned rational arithmetic and Positive.lean are
reused and hashed in successor-binding.json; no fresh arithmetic acquisition
or import-closure census was performed.

Runtime machine_model.clj remains SHA-256
`462aae432397e21209c238685d1aa39d040f15b330e2baa5f771d4858eb66b77`.
The unchanged machine-model-test suite passes, including numeric representation
controls. Decimal and binary representation semantics are unchanged.

## Limits

The retained row remains approximate. This packet makes no global entity
encoding, transition arithmetic, composed-error, KL, ordering, serving or
registry claim. No runtime source, existing proof statement, historical receipt,
or unrelated module was edited. No dependency conflict was found. WM-01's
other clauses remain open. Independent review of this packet is pending.

## Exact identities and command index

Mathlib4 commit: `f40c936a64227ba81592a71f3d937e6fbdbf0e4c`.

`Lean (version 4.31.0-rc1, x86_64-unknown-linux-gnu, commit fd009949156901e6cf15b6d9bf1122294b8e697a, Release)`

`Lake version 5.0.0-src+fd00994 (Lean version 4.31.0-rc1)`

| Source | Before SHA-256 | After SHA-256 |
|---|---|---|
| DarkTower/WarMachine/MachineModelSpec.lean | `854ee13082542b52d8f6fe88a72fcfaacaf0878bc3253e31a64c60896d6e8960` | `020af77ed11f0b5407bd6dd22a0a9e6e81ae30132ac41508277c0567500ca80f` |
| DarkTower/WarMachine/MachineForwardModelWitness.lean | `02bc5c00a8dc7d5dde2e861d647d805f5023f36eda46a12f4d3ea102d407e34c` | `62b15fe45ed0cc9eb12eb659b55122c1894cbcb5778b64feec758e965d427389` |
| DarkTower/WarMachine/FloatCarriedRowCorrespondence.lean | `absent (new file)` | `f8e9cdacaa47f2bd4131a41bd2876c5731356cd7b33f2dea3268125360be6e22` |

Exact command argument vectors follow. Each has matching raw stdout/stderr files.

- `Axioms`: exit 0; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/Axioms.lean"]`.
- `Duplicate`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/Duplicate.lean"]`.
- `Hidden`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/Hidden.lean"]`.
- `OutsideBound`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/OutsideBound.lean"]`.
- `Positive-attempt1`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/Positive.lean"]`.
- `Positive`: exit 0; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/Positive.lean"]`.
- `RetainedConversion`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/RetainedConversion.lean"]`.
- `canonical-build`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "build", "DarkTower.WarMachine.MachineModelSpec", "DarkTower.WarMachine.MachineBeliefDistribution", "DarkTower.WarMachine.MachineForwardModelWitness", "DarkTower.WarMachine.MachineParameters", "DarkTower.WarMachine.MachinePredictiveOutcome", "DarkTower.WarMachine.MachinePreferenceDistribution", "DarkTower.WarMachine.Row16ForwardModelAxiomCheck", "DarkTower.WarMachine.FloatCarriedRowCorrespondence"]`.
- `canonical-marker`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["touch", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/canonical-build.marker"]`.
- `canonical-written`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["find", ".lake", "-type", "f", "-newer", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/canonical-build.marker"]`.
- `finish`: exit 0; cwd `/home/joe/code`; argv `["python3", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/finish.py"]`.
- `isolated-build`: exit 0; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "build", "DarkTower.WarMachine.MachineModelSpec", "DarkTower.WarMachine.MachineBeliefDistribution", "DarkTower.WarMachine.MachineForwardModelWitness", "DarkTower.WarMachine.MachineParameters", "DarkTower.WarMachine.MachinePredictiveOutcome", "DarkTower.WarMachine.MachinePreferenceDistribution", "DarkTower.WarMachine.Row16ForwardModelAxiomCheck", "DarkTower.WarMachine.FloatCarriedRowCorrespondence"]`.
- `isolated-spec`: exit 1; cwd `/tmp/wm-float-carrier-u567g4by`; argv `["env", "LEAN_NUM_THREADS=1", "lake", "build", "DarkTower.WarMachine.MachineModelSpec"]`.
- `lake-version`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["lake", "--version"]`.
- `lean-version`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["lean", "--version"]`.
- `publish-source`: exit 0; cwd `/home/joe/code`; argv `["python3", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/publish-source.py"]`.
- `python-syntax`: exit 0; cwd `/home/joe/code`; argv `["python3", "-c", "import pathlib; r=pathlib.Path(\"futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1\"); files=sorted(r.glob(\"*.py\")); [compile(p.read_bytes(),str(p),\"exec\") for p in files]; print(\"PASS\", [p.name for p in files])"]`.
- `runtime-tests`: exit 0; cwd `/home/joe/code/futon2`; argv `["clojure", "-X:test", ":nses", "[futon2.aif.machine-model-test]"]`.
- `setup`: exit 0; cwd `/home/joe/code`; argv `["python3", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-float-carrier-1/setup.py"]`.
- `source-commit`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["git", "show", "--stat", "--oneline", "HEAD"]`.
