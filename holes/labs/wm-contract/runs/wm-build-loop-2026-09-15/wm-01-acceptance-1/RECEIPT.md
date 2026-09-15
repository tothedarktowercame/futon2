# WM-01-acceptance-1 — independent repair acceptance

**Verdict: ACCEPT, limited to the repaired carrier laws and constructor changes
in mathlib4 `480a666ad27c18477b4b9df86e11862be3930d50`.**

Author of acceptance: codex-3, independent of repair author codex-27.
Reviewer of this receipt: claude-3 (review still pending).
Commission: `invoke-1789507948054-21249-18a18e5d`, governing dispatch
`invoke-1789507763981-21248-b8bddfbb`.
Dispatch SHA256: `f61d3606031660fa231975a0a746fc5df5671b9316f36b70826f337d6b5728ae`.
Cascade SHA256: `27841342cef13fcf6f7b9f6434f0f349f57abf73d7b6d0f0f42bf95ebe717495`.
Both supplied hashes were independently verified before execution.

## Why accept

The repaired structures enforce the intended laws without replacing them by
weaker predicates. All 38 changed modules freshly elaborate. Valid sparse
kernels, exact rational tables and zero precision are accepted; nine separate
negative controls supply every field and fail on the intended obligation.
The 65 changed declarations selected in axiom-targets.json, including private
constructors, depend on no axioms outside propext, Classical.choice and
Quot.sound. The field proofs and composition code were read, not accepted on
build success alone. Caveats about intentional rejected artifacts and existing
holes are explicit below.

## Existing evidence and historical identity

Searched futon2/holes/labs/wm-contract and p4ng/wm-walkthroughs for the repair
SHA and kernel/repair acceptance records, plus /tmp/codex27-kernel*.
No complete independent acceptance receipt was located in that search scope.
The plan-clearance report is an unexecuted proposal. The join-6 review records
a hold on that earlier proposed review, not its execution. Codex-27's source
response and checklist ratification explicitly distinguish author validation
from missing independent acceptance.

Contrary to the dispatcher's earlier filesystem observation, the author's old
logs **are present now**. The final build log ends “Build completed successfully
(8645 jobs). EXIT: 0”; the source audit and before/after axiom logs also exist.
Their identities are retained in existing-evidence.json. They remain author
evidence. This receipt's new executions are not a recovered historical receipt.

Canonical mathlib4 was darktower at
`2b22eeef3102b3b48c8c12df66aaded459c36819`, a descendant of the subject.
The three later changed files are CascadeEFE.lean, CascadeEFEPolicies.lean and
GOverCascades.lean. All 38 subject repair files and lean-toolchain/lakefile.lean/
lake-manifest.json match the canonical source byte-for-byte. A recursive source
import walk of the repair roots covers 2460 Mathlib/DarkTower modules; all
located source bytes match, and none of the three later modules is in this
closure. This is cache-compatibility evidence, not acceptance of the descendant.

## Isolation and commands

A git archive of the exact subject was extracted into the fresh temporary
workspace recorded in workspace.txt. The canonical .lake was **copied** with
`cp -a --reflink=auto`, including packages and prebuilt Mathlib; there were no
hard links or mutable symlinks into the canonical checkout. The only cache
symlink resolved within the isolated copy. Cached products for all 38 repair
modules (304 files) were removed **in the copy** to force their fresh compilation.
No Lake or Lean command ran in the canonical checkout. No checkout, source,
proof statement, test, checklist or dependency was changed there.

Each repair module was compiled serially, in dependency order, with:

```text
lake env lean -j 1 -o <isolated>/.lake/build/lib/lean/<module>.olean <module>.lean
```

`LEAN_NUM_THREADS=1` was supplied. These are direct Lean module builds under
Lake's isolated environment, not a claim to have run a whole-repository
`lake build`. Compatible unaffected oleans were reused from the copy. The
recorded LEAN_PATH names only isolated package/build directories and the shared
installed Lean toolchain, never canonical mathlib4/.lake.

Every exact argv, cwd, exit status and duration is in build-receipts.json,
control-receipts.json, axiom-receipts.json and rejected-axiom-receipt.json.
Raw stdout/stderr are under logs/. Helper scripts retain the acquisition and
execution procedures. No gate output was piped.

Toolchain, as actually executed:

```text
Lean (version 4.31.0-rc1, x86_64-unknown-linux-gnu, commit fd009949156901e6cf15b6d9bf1122294b8e697a, Release)
Lake version 5.0.0-src+fd00994 (Lean version 4.31.0-rc1)
```

| Check | Invocations | Exit status |
|---|---:|---|
| Lean/Lake versions and isolated LEAN_PATH | 3 | all 0 |
| Fresh builds of changed modules, including committed guarded negatives | 38 | all 0 |
| Independent valid-control file | 1 | 0 |
| Independent unguarded invalid controls | 9 | all 1, intended rejection |
| Changed-declaration axiom probes | 37 files / 65 declarations | all 0 |
| Axioms of deliberately rejected committed definitions | 1 / 3 declarations | 0; sorryAx flagged below |

No .clj/.el helper was written, so clj-kondo/check-parens are inapplicable.
All helper Python sources were syntax-compiled without creating output files.

## Findings by obligation

### 1. Duplicate-free support — ACCEPT

Holes.lean:7020, exact source:

```lean
support_nodup : ∀ s, (support s).Nodup
```

This constrains each row's actual support list. It does not deduplicate the
list, alter masses or constrain a different surrogate. Duplicate unit support
[(), ()] with mass 1/2 is normalized under list summation, so normalization
alone would accept the defect. The repaired field rejects it.

Valid control: sparseKernel has support [s] and exact mass 1 at s, zero elsewhere.
InvalidKernel-support_nodup supplies nonnegativity, exact normalization and the
off-support field; its only outstanding false proof is support_nodup. The
corresponding ExactTable control independently checks the rational carrier.

### 2. Zero mass outside support — ACCEPT

Holes.lean:7021:

```lean
mass_eq_zero_of_not_mem : ∀ s o, o ∉ support s → mass s o = 0
```

This quantifies over every outcome, including outcomes absent from the list.
The valid sparse Bool kernel and sparseExact table have a real excluded outcome
(true at the false row) and prove its mass is zero. The invalid controls retain
support [false], supported mass 1 and hidden true mass 100: normalization,
nonnegativity and Nodup still hold, while this field fails.

### 3. Exact tables and dependent constructors — ACCEPT at source/proof scope

Holes.lean:7016–7021 requires the actual support/mass plus all four proofs:

```lean
support : S → List O
mass : S → O → ℝ
nonnegative : ∀ s o, 0 ≤ mass s o
normalised : ∀ s, ((support s).map (mass s)).sum = 1
support_nodup : ∀ s, (support s).Nodup
mass_eq_zero_of_not_mem : ∀ s o, o ∉ support s → mass s o = 0
```

LocalPreferenceModule.lean:9–15 independently requires the rational equivalents:

```lean
support : List O
mass : O → ℚ
nonnegative : ∀ o, 0 ≤ mass o
normalised : (support.map mass).sum = 1
support_nodup : support.Nodup
mass_eq_zero_of_not_mem : ∀ o, o ∉ support → mass o = 0
```

ExactTable.kernel forwards Nodup directly, transports off-support zero through
the rational-to-real cast, and proves exact normalization by commuting cast
with the list sum. It does not apply a tolerance or renormalize a row.
Valid controls include exact thirds (1/3, 2/3) and sparseExact; they prove the
cast preserves a supported third and the actual off-support zero.

Independent nonnegative negatives use masses (-1, 2) over Bool, so the row
still sums exactly to 1 and the other fields hold. Normalization negatives
use singleton mass 1/2, preserving all other obligations. Both kernel and
ExactTable variants fail on their respective intended fields.

Read all 38 file diffs (retained as repair.diff). The constructor changes fall
into these meaningful groups:

- Exhaustive finite enumerations prove Nodup and discharge off-support by
  enumerating the codomain (Status.all, Theta.all, Unit and small inductives).
  In these particular full-domain instances the off-support antecedent is
  impossible. That is lawful instance-level vacuity, not a replacement of the
  general field with True or a restricted outcome quantifier. These fixtures
  alone would not test hidden mass; the sparse Bool controls do.
- Partial/vertex-tagged witnesses now return zero outside their declared
  support, by a membership test or explicit pattern matching. Supported masses
  remain the declared values; they are not renormalized. This includes the
  predictive/preference/parameter witness changes and MachineQWitness's
  evidence-vertex restriction.
- F10 seed, machineC and groundedPrediction discharge the actual finite
  vertex/disposition cases. constantConditional forwards both source proofs.
- MachineQ's two composition constructors inherit Nodup through the declared
  support equalities. An empty state list contradicts its supplied normalized
  belief; the nonempty case obtains the source row's Nodup proof. Each
  off-support proof makes every summand zero using the actual transition or
  observation kernel's law. MachinePredictiveOutcome does the analogous
  transfer through supportExact and its terminal-state sum.

No new explicit axiom or executable sorry was added by the repair diff.
The 65 declaration axiom checks cover the changed accepted constructors and
related changed statements (inventory in axiom-targets.json). No one of these
uses sorryAx. This does not re-admit historical runtime witnesses by filename.

### 4. Nonnegative precision, with zero allowed — ACCEPT

Holes.lean:7048–7050, 7086 and 7094:

```lean
structure NonnegativeReal where
  value : ℝ
  nonnegative : 0 ≤ value
abbrev PrecisionMap := Channel → NonnegativeReal
def variationalFreeEnergy (precision : PrecisionMap) (error : Channel → ℝ) : VariationalFreeEnergyValue :=
```

NonnegativeReal/PrecisionMap already existed; the repair makes the VFE consumer
require PrecisionMap rather than a signed Channel → ℝ function. Its new
variationalFreeEnergy_nonnegative theorem proves each weighted square,
accumulated sum, division and final factor nonnegative. Error remains signed.

Valid controls explicitly construct zeroPrecision and twoPrecision. The zero
case proves VFE = 0 for every error map, ruling out accidental strict positivity.
The negative control attempts weight -2 with the required proof field and fails
on precision_nonnegative. Existing precision witness statement edits pass the
same nonnegative records to the newly typed consumer; they do not change the
reference values.

## Negative outcomes: exact quoted diagnostics

Each independent negative supplies all data and all four kernel/table fields
(or both NonnegativeReal fields). There are no omitted unrelated fields,
unknown names, syntax failures or artificial `fail` tactics. Tactics discharge
the valid obligations and reduce the intended invalid goal to False. The error
case tags below are emitted by Lean; filename labels alone are not the evidence.

### InvalidExactTable-mass_eq_zero_of_not_mem.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidExactTable-mass_eq_zero_of_not_mem.lean:6:44: error: unsolved goals
case mass_eq_zero_of_not_mem.true
h : True
⊢ False
```

### InvalidExactTable-nonnegative.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidExactTable-nonnegative.lean:6:44: error: unsolved goals
case nonnegative.false
⊢ False
```

### InvalidExactTable-normalised.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidExactTable-normalised.lean:6:44: error: unsolved goals
case normalised
⊢ False
```

### InvalidExactTable-support_nodup.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidExactTable-support_nodup.lean:6:44: error: unsolved goals
case support_nodup
⊢ False
```

### InvalidKernel-mass_eq_zero_of_not_mem.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidKernel-mass_eq_zero_of_not_mem.lean:6:52: error: unsolved goals
case mass_eq_zero_of_not_mem.true
s : Unit
h : True
⊢ False
```

### InvalidKernel-nonnegative.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidKernel-nonnegative.lean:6:52: error: unsolved goals
case nonnegative.false
s : Unit
⊢ False
```

### InvalidKernel-normalised.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidKernel-normalised.lean:6:52: error: unsolved goals
case normalised
s : Unit
⊢ False
```

### InvalidKernel-support_nodup.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidKernel-support_nodup.lean:6:52: error: unsolved goals
case support_nodup
s : Unit
⊢ False
```

### InvalidPrecision.lean — exit 1

```text
/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-acceptance-1/InvalidPrecision.lean:6:40: error: unsolved goals
case precision_nonnegative
channel : Channel
⊢ False
```

The committed ProbabilityKernelRepairNegative.lean separately rebuilt with
exit 0: its three #guard_msgs blocks matched their expected rejection text.
Source inspection confirms every kernel field is supplied; the failing fields
are support_nodup, mass_eq_zero_of_not_mem and the nonnegative precision proof.
The independent named-case controls above strengthen the diagnostic attribution.

## Axiom reports and explicit sorryAx flags

The 65 changed-declaration reports are in logs/Axioms-*.stdout, generated by
actual `#print axioms` commands elaborated for the resolved constant names,
including private names. The helper also rejects any axiom outside the allowed
set. All 37 probe invocations exit 0; all reported sets are subsets of
{propext, Classical.choice, Quot.sound}. The eight valid-control definitions/
theorems printed in logs/Valid.stdout also use only that set.

**sorryAx is present in the three deliberately rejected definitions exported
by the committed negative-test module:** duplicateKernel, hiddenMassKernel,
negativePrecisionCall. logs/RejectedAxioms.stdout retains those actual reports.
#guard_msgs validates their failing diagnostics but does not erase Lean's
placeholder proof from those rejected constants. These are explicitly invalid
test artifacts and must not be counted as admitted probability witnesses.
None of the 65 checked accepted declarations depends on them or on sorryAx.
This acceptance is not a statement that every constant in the test module is
sound without admissions.

The fresh Holes compilation also reports nine pre-existing `declaration uses
sorry` warnings at lines 157, 999, 7403, 7426, 7437, 7965, 7968, 7971 and 7974.
The repair did not introduce those admissions, and no selected repaired law or
constructor depends on them. Their presence is flagged, not silently converted
to whole-module or whole-machine acceptance.

## Limits

This is the missing independent acceptance of the carrier repair, subject to
claude-3's review of this receipt. It does not close WM-01. It does not validate
Clojure numeric correspondence, runtime floats, model meanings, semantic
observation authority, production caller bindings, existing witness admission
lineage, or current descendant/serving behavior. It neither audits all Holes
proofs nor re-proves the reused Mathlib/package substrate. Cache/toolchain
compatibility and the checked source closure are stated explicitly above.
No source repair, production action, new dispatch, or checkbox edit occurred.

## Exact reviewed source hashes

The complete import-name inventory and subject/current comparison are in
source-manifest.json; control-source-manifest.json pins every review Lean file.
These SHA256 hashes refer to the 38 reviewed files at the historical subject:

| File | SHA256 |
|---|---|
| `DarkTower/WarMachine/AmbiguityBridge.lean` | `61509396b64295c3b1ea49de8d53ce2342ee790172428b814364ea21f3efa99f` |
| `DarkTower/WarMachine/AmbiguityWitness.lean` | `4e7c1b713636470de8c1a2c4193ea27ee5730c83d0e758814fe3bd20f29a9ec7` |
| `DarkTower/WarMachine/BeliefUpdateFalsifier.lean` | `699cec1ef9b5b54cda407ad7f6f485f8950dc0784fc41515a7ffcb8de3dcc9d9` |
| `DarkTower/WarMachine/ExpectedFreeEnergyWitness.lean` | `90d283e5366f48be055000d46943c26cf18a3a37728adb8078a2852a3bc82c2a` |
| `DarkTower/WarMachine/ExpectedInformationGainWitness.lean` | `248864464b047fcabcc78c493a9798e69f4789a137b0600e36893c232ebbbd7b` |
| `DarkTower/WarMachine/F10RuledCarrier.lean` | `6127e93b35c17b46d502b2b024fdace8caf2b7c20b6a4eed9b0718e1e235853c` |
| `DarkTower/WarMachine/GenerativeModelNegative.lean` | `f7d1149b7cbcac56a95cfa146d28ae48c08d8581a5d02b16525e30bf14eb997e` |
| `DarkTower/WarMachine/GenerativeModelWitness.lean` | `6eff415b3c9791d81ba2556c2f9a2f5d62d48fe16501c03d2c22befc2f41af6c` |
| `DarkTower/WarMachine/Holes.lean` | `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3` |
| `DarkTower/WarMachine/LocalPreferenceModule.lean` | `228b0fffac5097facea4a486fe14b6e84f743b749697c9a67de6fc5e60c66eae` |
| `DarkTower/WarMachine/MachineBeliefDistribution.lean` | `0071f10e6447a459a4a9f5ac7e94062145a3d962a6009a0177287a73d6d4e79a` |
| `DarkTower/WarMachine/MachineParameters.lean` | `157d979114ec9eb14e7fc9d97442905dd9342dae3f523657882a0f2a487d6d3f` |
| `DarkTower/WarMachine/MachinePredictiveOutcome.lean` | `dd3205bf71108542ff77b0b773502e47dec0a915d89a839beac9f58c3876062d` |
| `DarkTower/WarMachine/MachinePreferenceDistribution.lean` | `4ca7357d7a2156b66c89b121fd75afc15b07023dfbc7697f16808f1363984a6a` |
| `DarkTower/WarMachine/MachineQ.lean` | `a52826e1a16eee2ecfd74492a75d73284cd32e2b0427272fb3e92fbc15b417e6` |
| `DarkTower/WarMachine/MachineQWitness.lean` | `a0daf44404c55c316ade678988aeb8d40f85be88e4a14b97a5d5632fc895c5aa` |
| `DarkTower/WarMachine/MachineTransition.lean` | `08ad7a9e90bc7455874fea333a26b5de413a55aae792eea54c1b7b7bf878337a` |
| `DarkTower/WarMachine/ObservationKernelWitness.lean` | `99d67b8c594a6063864355436622bf45ebf3ee32b1cc424a301e40a9e8d7be7d` |
| `DarkTower/WarMachine/ParameterPosteriorKernelOutcomeNegative.lean` | `f5d9291f2868190e0257c457f08f9a6176f266652dcfc14bb0a7ddc24ebed5a9` |
| `DarkTower/WarMachine/ParameterPosteriorKernelPriorNegative.lean` | `81c82cc904c23588b89514c62454b1a985733fe47dab360d696c351fa667cec9` |
| `DarkTower/WarMachine/ParameterPosteriorKernelWitness.lean` | `8512366eb8246c3d383539fdcab75ff8efb0b1b49c29ab4bd7e992c479e15892` |
| `DarkTower/WarMachine/ParameterPriorKernelHabitNegative.lean` | `296ccec7c569c0dcf89ab32d6c1ee38870ee448ace91ce7e3c7178b164ea8fec` |
| `DarkTower/WarMachine/ParameterPriorKernelOutcomeNegative.lean` | `748a726a9092b1efa4688c03613390d9bf322368cdb814fa7a8032e1151dbe0e` |
| `DarkTower/WarMachine/ParameterPriorKernelWitness.lean` | `1ac981f4ac29cf5867717c6d412c243459ca673803d16941d7c17cf3b6fa83c7` |
| `DarkTower/WarMachine/PolicyPriorKernelNegative.lean` | `89448450f84537261a7ee62d5e2cbc1474b0faf0537e1fc761d8c516cc95e9b0` |
| `DarkTower/WarMachine/PolicyPriorKernelWitness.lean` | `172436cbae2ba145f37ea64f30eb184cffb4f27d791ee2bd182d79e2c905c7c4` |
| `DarkTower/WarMachine/PrecisionWitness.lean` | `5f10a87fccb75c69ca41ba2b126b9460a63fdbe4797c880913b6f3fe7b2e971b` |
| `DarkTower/WarMachine/PredictiveOutcomeKernelUnconditionalNegative.lean` | `2b2005173fd4f3b789073372b8c4eeebfd1379215cb00ded3b77f62b3da545b0` |
| `DarkTower/WarMachine/PredictiveOutcomeKernelWitness.lean` | `b38429046b74af1ed580c5fbfddda984d49d81f8705b5568f74a783494c05632` |
| `DarkTower/WarMachine/PredictiveOutcomeRiskWitness.lean` | `5336e34b3fe09fcb5f8e6a6bf9bc506c1f77186ee4f8544828a000df1ff8b7b4` |
| `DarkTower/WarMachine/PreferenceDistributionConditioningNegative.lean` | `4a5e9fb8dc582b535b78e3aebff30e12f1bcfb37939589302494658afd8373d6` |
| `DarkTower/WarMachine/PreferenceDistributionWitness.lean` | `f32a05143e2c19e05b896e0e1bdb53e0b59bfe248401d2c9fffa58ce942cbe87` |
| `DarkTower/WarMachine/PreferenceRiskSeparation.lean` | `869c69fd258ed4b4e585a0ab7f7c83fee11c6f50afd390a83b4c680507fba640` |
| `DarkTower/WarMachine/PreferenceRiskWitness.lean` | `33e80d6499b9221b1306cd25bde8bec3411da2c8893f16cb8aded7715f1a8b40` |
| `DarkTower/WarMachine/ProbabilityKernelRepairNegative.lean` | `0de6a98797ccfee74afda5328572b200cf5172051801c1cf1db55b866ac4b701` |
| `DarkTower/WarMachine/TransitionKernelUncontrolledNegative.lean` | `a0dd96892d781b484178fa29bd29f4819a2d64915399bed383465428a09e3f8e` |
| `DarkTower/WarMachine/TransitionKernelWitness.lean` | `0611cb7f8d441cd45f3b5f8e93f15cfa63ff021de67f8caec91053d78af4f58b` |
| `DarkTower/WarMachine/VariationalFreeEnergyWitness.lean` | `d8f43f4ffeb0147dfffc268556e06742391338218249f65fc7941c4d3adf0249` |

## Cleanup and final identity check

The isolated workspace (11G) was removed after all executions.
cleanup-receipt.json records the deletion, a distinct-inode cache-copy check,
and verification that canonical HEAD and all 38 source hashes remained unchanged.
Raw logs and review controls are retained here.

The optional `git diff --cached --check` returned 2 solely for the raw
repair.diff context lines (a single space is the unified-diff empty-context
marker). That byte-for-byte source evidence is retained unmodified; raw output
is in logs/git-diff-check.stdout. All requested Lean checks have the statuses
reported above.
