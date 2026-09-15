# WM-01-binding-1 — row-7 rebinding

Author: codex-3. Reviewer: claude-3 (review pending).

**Verdict: the constructed exact-rational correspondence passes. The retained
production row is numerically admitted but cannot instantiate the unchanged
exact kernel.** No source change or new formal relation is needed for these
bounded conclusions. WM-01 remains open beyond this packet.

## Subjects and evidence

The Lean subject is `480a666ad27c18477b4b9df86e11862be3930d50`.
`BINDING.json` binds the exact theorem names, source identities, reader input
and output, toolchain, commands, controls and reused acceptance evidence.
`source-identities.json` records all 2420 reachable repository source modules,
with each SHA-256 and direct imports. Each matches the repaired commit bytes.
Canonical HEAD is recorded separately; none of its three later changed modules
occurs in this closure. Package revisions match the unchanged lake manifest,
and tracked package Lean/config sources have no diff. Lean/Std imports are
bound by the recorded toolchain. This does not accept the later descendant.

The historical `:tree-sha` 4750f9fa17eb09bb3d70c0ae12e4256cf3dcf5ec is a
commit, an ancestor 91 commits before the subject. Its row-7 receipt cannot
transfer by filename. Holes and MachineBeliefDistribution changed; the latter
added the duplicate-free and zero-off-support proofs. MachineModelSpec also
changed before the repair (FloatCarriedRow addition), so its hash is included.
We do not instantiate FloatCarriedRow or claim its independent acceptance here.

The accepted fresh MachineBeliefDistribution build and selectedKernel axiom
receipt are reused from acceptance-1 (3fc00fe1, 1f373e4b; review 9fcf1af1).
They were not rerun. New concrete controls execute read-only against the
compatible canonical cache without `-o`. `find .lake -newer <pre-run marker>`
returned no entries both before and after all Lean runs. No canonical build,
checkout change, source edit, historical receipt edit or serving action occurred.

### Direct carrier/source pins

| File | SHA-256 |
|---|---|
| DarkTower/WarMachine/Holes.lean | `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3` |
| DarkTower/WarMachine/MachineBeliefState.lean | `da2c648860c1a4ee48f58106c5f7fc60a5150d914bc2089a2440eea801a7521e` |
| DarkTower/WarMachine/MachineModelSpec.lean | `854ee13082542b52d8f6fe88a72fcfaacaf0878bc3253e31a64c60896d6e8960` |
| DarkTower/WarMachine/MachineBeliefDistribution.lean | `0071f10e6447a459a4a9f5ac7e94062145a3d962a6009a0177287a73d6d4e79a` |
| src/futon2/aif/machine_belief.clj | `38993c823ccbd9dd10b6daa51902de4ae9bc7d08feaaa826c26a3bc2f332908b` |
| src/futon2/aif/machine_model.clj | `462aae432397e21209c238685d1aa39d040f15b330e2baa5f771d4858eb66b77` |

## Binding and premises

State coordinates, in order: spawned, refined, strengthened, addressed,
falsified, foreclosed, reopened. The single association is Lean entity `0`
with runtime string `arxana/stack/futon-v1/leaf/2/2`; this is an explicit binding
assumption, not a global string-to-Nat encoding. Both concrete contexts use
model id `wm-production`, revision `2026-09-12-row-7`. The Lean theorem carries
these strings as metadata and does not prove their meaning or acquisition provenance.

The unchanged construction requires `hn : ∀ s, 0 ≤ posterior s` and
`h₁ : Normalised posterior`; the latter means exactly
`(Status.all.map posterior).sum = 1` over real numbers.
`selectedPosterior_conserved` additionally requires
`hp : stored context.entity = some posterior`.

`Positive.lean` proves these premises for the unequal exact row k/28,
k=1..7, defines the corresponding concrete stored belief at entity 0, and
instantiates selectedKernel, selectedKernel_normalised,
selectedKernel_coordinate (each of seven named coordinates), and
selectedPosterior_conserved. The unchanged runtime reader returns the identical
rational coordinates and exact admission. `runtime.edn` retains full input,
context, output and admission; `coordinates.tsv` connects those values to the
Lean definitions. This is an executed finite correspondence control, not a
formal verification of the Clojure reader or a new production observation.

## Actual retained production row

The input is form 0, `:mu-post`, selected entity, from
`data/wm-trace/wm-trace-2026-09-04.edn`, SHA-256
`f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`.
The runtime control checks the bytes before reading. This trace is untracked;
future reproduction requires those same external bytes. The extracted row and
exact binary64 representations are retained here. The bootstrapped May row is
excluded: the required subject is the carried September posterior.

Python independently converts each original double with `Fraction(float)` and
cross-checks the hexadecimal binary64 value. The exact values appear in
`independent-arithmetic.json`. Their sum is
`36028797018963969/36028797018963968`; deviation is `1/2^55`.
The unchanged reader reports representation `:ieee-floating`, admission
`:float-carried`, criterion `:absolute-row-sum` v1.1 with bound `1/10^12`,
and `:exactly-normalized? false`. All masses remain unchanged.

Lean positively proves `production_not_Normalised : ¬ Normalised prodPosterior`
for these exact rational images of the seven doubles. A separate attempt to
supply the contrary premise fails specifically on `Normalised_premise`.
No normalization, tolerance change or weakening of the kernel was used.

## Distinguishing controls and axioms

| Control | Result | Evidence |
|---|---|---|
| Current pinned source identities + independent arithmetic | exit 0 | logs/prepare.stdout |
| Historical Holes and MachineBeliefDistribution blobs | exit 1, expected | `REJECT stale-source-hash` in logs/stale.stdout; uses the same identity checker as the positive check |
| Unchanged runtime reader, exact and carried rows | exit 0 | logs/runtime.stdout; runtime.edn |
| Exact rational Lean construction, seven coordinates, conservation; production nonnormalization proof | exit 0 | logs/positive.stdout |
| Swapped spawned/refined masses (sum still one) | exit 1, expected | `error: unsolved goals`, `case spawned_coordinate_equation`, `case refined_coordinate_equation`, each `⊢ False` in logs/swapped.stdout |
| Attempted exact production normalization | exit 1, expected | `error: unsolved goals`, `case Normalised_premise`, `⊢ False` in logs/normalisation.stdout |

The positive control reports axioms for all 13 new premise/coordinate/conservation
and nonnormalization theorems plus the three previously unreported dependency
theorems selectedPosterior_conserved, selectedKernel_coordinate and
selectedKernel_normalised. Every report contains only propext, Classical.choice
and Quot.sound. No sorryAx or extra axiom appears. selectedKernel itself uses
the accepted prior axiom receipt. Failed controls are failed elaborations, not
accepted declarations or admitted witnesses.

## Exactly what is rebound

- The symbolic row-7 selection/coordinate/conservation claim is now bound to
  the repaired source under explicit nonnegativity, exact normalization and
  selected-entity premises, with all these premises discharged for the
  constructed exact fixture.
- The carried production input is bound to the current unchanged reader and
  its approximate numeric admission, with independent exact arithmetic.
- The historical “production matches Lean reference” is floating coordinate
  equality. It cannot be rebound as exact-kernel correspondence: its required
  Normalised premise is false. The original receipt is retained unchanged.

No claim covers every runtime input, global entity encoding, model identity
beyond the explicit context, acquisition completeness, trace durability, other
witnesses, or current production execution. The 38-module repair audit and its
admission census were not repeated. Further numeric correspondence beyond the
executed exact fixture remains outside this packet.

## Commands and gates

All commands run without pipelines. `run.py` retains stdout, stderr, cwd,
argument vector and exit status under `logs/`. `lean-runs.py` sets
`LEAN_NUM_THREADS=1`; each Lean run uses `-j 1` and no output option.
Clojure lint and parens gates pass. Python syntax is checked with `compile()`
without writing caches. Receipt helper execution is reproducible from these
scripts; run the runtime control before prepare.py and Lean controls.

- `cache-after`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["find", ".lake", "-newer", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/cache-before.marker"]`.
- `cache-before`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["find", ".lake", "-newer", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/cache-before.marker"]`.
- `kondo`: exit 0; cwd `/home/joe/code/futon2`; argv `["clj-kondo", "--lint", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/runtime-control.clj"]`.
- `lake-version`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["lake", "--version"]`.
- `lean-version`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["lean", "--version"]`.
- `marker`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["touch", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/cache-before.marker"]`.
- `normalisation`: exit 1; cwd `/home/joe/code/mathlib4`; argv `["lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/FailedNormalisation.lean"]`.
- `parens`: exit 0; cwd `/home/joe/code/futon2`; argv `["emacs", "-Q", "--batch", "-l", "/home/joe/code/futon4/dev/check-parens.el", "--eval", "(arxana-check-parens-cli)", "--", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/runtime-control.clj"]`.
- `positive`: exit 0; cwd `/home/joe/code/mathlib4`; argv `["lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/Positive.lean"]`.
- `prepare`: exit 0; cwd `/home/joe/code`; argv `["python3", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/prepare.py"]`.
- `python-syntax`: exit 0; cwd `/home/joe/code`; argv `["python3", "-c", "import pathlib; r=pathlib.Path(\"futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1\"); files=sorted(r.glob(\"*.py\")); [compile(p.read_bytes(),str(p),\"exec\") for p in files]; print(\"PASS\", [p.name for p in files])"]`.
- `runtime`: exit 0; cwd `/home/joe/code/futon2`; argv `["clojure", "-M", "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/runtime-control.clj", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1"]`.
- `stale`: exit 1; cwd `/home/joe/code`; argv `["python3", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/prepare.py", "--stale"]`.
- `swapped`: exit 1; cwd `/home/joe/code/mathlib4`; argv `["lake", "env", "lean", "-j", "1", "/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-binding-1/Swapped.lean"]`.
