# WM-01-admission-1 — bounded R4 successor, publication blocked

Author: codex-4. Reviewer: claude-3; review of this successor subject is pending.

## Author verdict

The editable R4 fragment now contains only a **proposed**, formal-only successor.
Scoped preparation and binding controls pass. **Not admitted; not globally published;
required gates are not all green.** The global merger refuses the out-of-scope R17
pin. The existing merger control suite and both required witness wrappers also fail,
as detailed below. This commit is the scoped artifact/refusal handoff authorized by
the dispatch, not a completion or deployment claim.

Authority: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-admission-1.md`, SHA-256
`42b8442d5c363edcbf2fad23679a5e5b36710c4e4cb925150350fad6e031bcdc`.
Starting futon2 commit: `43cf8563014cda89de8a6ea0d52b68f0c6ebeff4`.

## Identity and retirement

`R4-forward-model-float-carried-production-pins-v1`
→ `R4-forward-model-float-carried-formal-support-v2`.

Chose **removal from the editable fragment**, preserving the historical entry verbatim
below and in `old-entry.edn`, and the entire original fragment in `old-fragment.edn`.
Demoting the old admission to a proposal would misrepresent a withdrawn production
claim as work awaiting admission. No retired status was invented. Wrapper metadata
is unchanged. The old verification and review were not copied to the successor.

The generated global registry still contains the old historical admission because
validation refuses before any write. Its old verified-binding header is **not a current
gate result**. Removal is effective in the editable inventory and isolated generated
inventory only; global retirement/publication remains blocked. No global registry was
hand-edited or assembled while excluding R17.

## Formal successor and verification preparation

`successor-subject.edn` contains the exact immutable subject. The claim is constructed
formal witness and support laws only. It refers to the unchanged-name theorem
`productionPinnedFloatCarried` as a theorem about constructed rows, without inheriting
the historical claim of production acquisition or independent runtime composition.
Support laws are discharged by the constructors against `MachineModelSpec`.

`verification-preparation.edn` binds that subject to current source pins and the already
accepted formal controls from mathlib4 `f40c936a64`, futon2 author `6f494738` and reviewer
`e99c08c4`. It is a preparation record, **not a freshly executed Lean verification receipt**.
It pins the accepted control invocations/transcripts for the reviewer's inspection.
No qualifying subject-bound verification/review pair has been asserted or manufactured.
The predecessor's formal review is evidence about its own packet, not approval of this
new registry subject. Promotion is outside this dispatch.

The retained approximate row totals `1 + 1/2^55`; its support laws and admitted bound do
not establish exact normalization. There are no fresh observations or production
composition correspondence claims.

| Source | Current SHA-256 |
|---|---|
| MachineForwardModelWitness.lean | `62b15fe45ed0cc9eb12eb659b55122c1894cbcb5778b64feec758e965d427389` |
| MachineModelSpec.lean | `020af77ed11f0b5407bd6dd22a0a9e6e81ae30132ac41508277c0567500ca80f` |
| FloatCarriedRowCorrespondence.lean | `f8e9cdacaa47f2bd4131a41bd2876c5731356cd7b33f2dea3268125360be6e22` |
| Holes.lean | `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3` |

## What the canonical validator actually checked

- Historical unmodified R4 fragment, scoped merge: exit 1,
  `:node-witness-pin-mismatch` on its actual MachineForwardModelWitness pin.
  Existing `resolve!` separately rejects both actual stale R4 locators, including Holes.
- Proposed successor, scoped merge: exit 0 with `:pending`, seeded against the original
  global registry for identity preservation. Scoped `--check` roundtrip: exit 0.
  `scoped-registry.edn` is this isolated output, **not a global publication**.
- Proposed validation checks shape/authority and reads available paths, but does not
  enforce hash equality or review admission. Therefore `new-pins` separately calls the
  existing `resolve!` over every successor/preparation locator. All resolve.
- Reusing the old ID for the changed subject: the canonical scoped merger rejects with
  `:node-witness-id-revision-required`, exit 1, against the original admitted registry.
- Tampering the new artifact pin: existing `resolve!` rejects with
  `:node-witness-pin-mismatch`, exit 1. This is explicitly a direct pin check,
  not a claim that proposed-mode merge enforces admission checks.
- Borrowing the old review: existing `receipt!` rejects the new subject with
  `:node-witness-subject-mismatch`, detail `:review`, exit 1. No fake verification
  or review was added to make this control reach the intended check.
- Full canonical `--check` and default regeneration both exit 1 before writing.

## Exact global blocker

I independently scanned **56 fragments / 212 pinned locator occurrences**. Before:
exactly three stale occurrences (R17 Holes; R4 Holes; R4 MachineForwardModelWitness).
After: exactly one stale occurrence, in `checks/witness-fragments/DirichletConcentrations.edn`:

- ID: `R17-dirichlet-accumulation-ieee-residuals-v1`.
- Subject: `mathlib4/DarkTower/WarMachine/Holes.lean`, declaration
  `DarkTower.WarMachine.Holes.DirichletConcentrations`.
- Pinned: `4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b`.
- Actual: `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3`.
- Refusal: `:node-witness-pin-mismatch`.

The complete inventories and raw global refusals are retained in `logs/`. R17 was not edited.

## Additional failed gates and bounded diagnosis

1. `bb -cp scripts:test test/witnesses/merge_witnesses_control.bb`: **exit 1** at
   `:genuine-deterministic-merge`. Its first block selects machineObservation,
   machinePrecision and machineChannelPredictionError, not this R4 fragment. Both
   of its merges succeed, but its assertion requires all 24 entries to be proposed;
   these entries are admitted. Its test and selected fragments match starting HEAD.
   The later `:stale-bytes`, `:missing-declaration`, `:nil-receipt`, `:missing-readback`
   and `:id-reuse` blocks are **not reached**. The separate R4 controls above do not
   turn this suite green. No test or admission was changed to route around the failure.
2. Both required wrapper negative commands: **exit 2**, printing `mutation slipped`.
   A separate call to the existing positive-receipt validator returns `:pass? false`,
   failures `[:positive-source-drift]`; elaboration, axioms, fixture and toolchain checks
   pass. `positive-source-drift-detail.stdout` identifies the stale declaration slice.
   Direct negative Lean fixtures, invoked serially from `/home/joe/code/mathlib4`, both
   exit **0**. The negatives still reject, but their wrapper gates remain failed.
   The positive receipt is outside scope and was not changed.
3. My initial direct Lean diagnosis used `lake -d` from futon2 and reported incompatible
   headers. That invocation selected the wrong launch toolchain; it is **not evidence
   of a broken canonical cache**. Those failed attempts remain in the logs. Correcting
   the diagnostic working directory (the same one the wrapper uses) gives the two
   passing direct fixture results above. No cache rebuild or source change was made.
4. Initial clj-kondo rejected the new control's namespace/file mismatch. Corrected the
   namespace; final lint and final check-parens both exit 0. The initial failure is retained.
5. The first final-artifact diagnostic had a delimiter typo in its inline form and exited
   1 before execution. The corrected invocation passes; both attempts are retained.

## Before and after

| Artifact | Before SHA-256 | After SHA-256 |
|---|---|---|
| PredictiveOutcomeKernel.edn | `7f9d4d4654264276c7c937617eea98271d7607e05246606d7c032bdf5fa1fb56` | `3860a42e62b32c5f9a737df223537c850c9e16f3dc33ffe54d39c7268596bba7` |
| witness-registry.edn | `5f153fc1c13bf3a6327729f70d37f873dbe9c3cb11f46be616ab4f485bf2666b` | `5f153fc1c13bf3a6327729f70d37f873dbe9c3cb11f46be616ab4f485bf2666b` |

Historical lexical entry SHA-256: `32c8c8a6edca417e34ae186dbb9eddb4c8b88b60dbcbe195d20a3db1f4ea22c5`.
The source commit's fragment bytes match `old-fragment.edn`; the extracted old entry
parses identically; wrapper metadata is unchanged; the new subject matches the fragment.
Raw logs are explicitly retained despite the repository's general `logs/` ignore rule.

## Remaining WM-01 clauses / return to owner

This packet does not close WM-01. Remaining: actual subject-bound verification and
independent review/admission for the proposed identity; global publication after the
R17 conflict is resolved; the other dependent proof/consumer bindings and independent
repair acceptance; shared State/Action/Outcome/model-revision and numeric semantics;
production correspondence and joined consumer evidence; numerical operations and their
error semantics. No serving action, click, push, checkbox edit or new worker task occurred.

Owner question: how should the out-of-scope R17 retirement, stale merger-suite status
expectation and predictive positive-receipt source drift be assigned? This receipt
reports them; it does not elect or start their successors.

## Historical entry (verbatim)

```edn
{:schema :wm/node-witness-v1,
   :retained-evidence
   {:readback
    {:repo "futon2",
     :path
     "holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn",
     :sha256
     "88559ad404088f2087212febe8ab24385dfefefe130288a12e6ddbd14da504f7"},
    :generator
    {:repo "futon2",
     :path
     "holes/labs/wm-contract/runs/row-6-predictive-outcome/generate_forward_witness.clj",
     :sha256
     "eaa27949d53c45ee6b8400affba7fbadae4f58309bac57cf595d8cb605f023cf"},
    :negative-controls
    {:repo "futon2",
     :path
     "holes/labs/wm-contract/runs/row-6-predictive-outcome/readback.edn",
     :sha256
     "88559ad404088f2087212febe8ab24385dfefefe130288a12e6ddbd14da504f7"}},
   :verification
   {:status :verified,
    :receipt
    {:repo "futon2",
     :path
     "holes/labs/wm-contract/runs/row-16-forward-model-2026-09-12/verification-receipts.edn",
     :sha256
     "cf383515274dba2b24612380724fb9b6f57bfb28d11890f253db493e127af289",
     :selector
     [:records "R4-forward-model-float-carried-production-pins-v1"]}},
   :subject-artifact
   {:repo "mathlib4",
    :path "DarkTower/WarMachine/Holes.lean",
    :sha256
    "4dc0a76b9999d09b2ab49c932117e5b7dcfec523e5e61735b3a84191229cd02b",
    :declaration "DarkTower.WarMachine.Holes.PredictiveOutcomeKernel"},
   :scope :production-pins-float-carried-v1.1,
   :node :R4,
   :evidence-limit
   "Contract-v1.1 FloatCarriedRow measurement at these production pins only. It does not claim the production bytes are an exactly normalized PredictiveOutcomeKernel and does not perform renormalization.",
   :as-of "2026-09-12",
   :review
   {:status :verified,
    :receipt
    {:repo "futon2",
     :path
     "holes/labs/wm-contract/runs/row-16-forward-model-2026-09-12/review-receipts.edn",
     :sha256
     "82726e60a0c25f2a4e31da0e6e334c9f08ae1a8753a654f191721888c62cb06b",
     :selector
     [:records "R4-forward-model-float-carried-production-pins-v1"]}},
   :status :admitted,
   :id "R4-forward-model-float-carried-production-pins-v1",
   :kind :lean-witness,
   :polarity :supports,
   :declaration "PredictiveOutcomeKernel",
   :artifact
   {:repo "mathlib4",
    :path "DarkTower/WarMachine/MachineForwardModelWitness.lean",
    :sha256
    "02bc5c00a8dc7d5dde2e861d647d805f5023f36eda46a12f4d3ea102d407e34c",
    :module "DarkTower.WarMachine.MachineForwardModelWitness",
    :declaration
    "DarkTower.WarMachine.MachineForwardModelWitness.productionPinnedFloatCarried"},
   :quantity :Q-o-pi,
   :claim
   "At the unchanged row-9 production pins, three policy-conditioned outcome rows match independent exact positional-A composition within measured IEEE deltas and satisfy FloatCarriedRow.nearNormalised; exact ProbabilityKernel normalization is deferred to row 14's reviewed model instance.",
   :equation :forward-model}
```

## Exact retained command index

Every invocation below retains its exit and unfiltered output. A diagnostic process exit
0 does not override a printed validation failure; expected negative exits are explained
above. Python orchestration stopped on unexpected gates; remaining independent gates
were invoked separately without changing those failed results.

### source-head

- Command: `git rev-parse HEAD`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: source identity.
- Raw output: [stdout](logs/source-head.stdout), [stderr](logs/source-head.stderr); [invocation](logs/source-head.json).

### inventory-before

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj inventory`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: all pinned locators in all fragments.
- Raw output: [stdout](logs/inventory-before.stdout), [stderr](logs/inventory-before.stderr); [invocation](logs/inventory-before.json).

### old-scoped-merge

- Command: `bb scripts/merge_witnesses.bb --fragments /tmp/wm-r4-old-9txj5atf/fragments --output /tmp/wm-r4-old-9txj5atf/registry.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: unaltered historical R4 fragment only.
- Raw output: [stdout](logs/old-scoped-merge.stdout), [stderr](logs/old-scoped-merge.stderr); [invocation](logs/old-scoped-merge.json).

### old-pins

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj old-pins`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: existing resolve! on both actual stale R4 locators.
- Raw output: [stdout](logs/old-pins.stdout), [stderr](logs/old-pins.stderr); [invocation](logs/old-pins.json).

### prepare

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj prepare`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: replace one fragment entry; prepare formal successor subject.
- Raw output: [stdout](logs/prepare.stdout), [stderr](logs/prepare.stderr); [invocation](logs/prepare.json).

### new-pins

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj new-pins`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: existing resolve! on successor and preparation locators.
- Raw output: [stdout](logs/new-pins.stdout), [stderr](logs/new-pins.stderr); [invocation](logs/new-pins.json).

### new-scoped-merge

- Command: `bb scripts/merge_witnesses.bb --fragments /tmp/wm-r4-new-qvbdjejd/fragments --output /tmp/wm-r4-new-qvbdjejd/registry.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: one proposed fragment only; NOT global publication.
- Raw output: [stdout](logs/new-scoped-merge.stdout), [stderr](logs/new-scoped-merge.stderr); [invocation](logs/new-scoped-merge.json).

### new-scoped-check

- Command: `bb scripts/merge_witnesses.bb --fragments /tmp/wm-r4-new-qvbdjejd/fragments --output /tmp/wm-r4-new-qvbdjejd/registry.edn --check`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: isolated generated registry roundtrip.
- Raw output: [stdout](logs/new-scoped-check.stdout), [stderr](logs/new-scoped-check.stderr); [invocation](logs/new-scoped-check.json).

### prepare-reuse-id

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj reuse-id /tmp/wm-r4-new-qvbdjejd/fragments`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: temporary changed-subject fixture.
- Raw output: [stdout](logs/prepare-reuse-id.stdout), [stderr](logs/prepare-reuse-id.stderr); [invocation](logs/prepare-reuse-id.json).

### reuse-id

- Command: `bb scripts/merge_witnesses.bb --fragments /tmp/wm-r4-new-qvbdjejd/fragments --output /tmp/wm-r4-new-qvbdjejd/registry.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: canonical merger against old admitted identity.
- Raw output: [stdout](logs/reuse-id.stdout), [stderr](logs/reuse-id.stderr); [invocation](logs/reuse-id.json).

### tamper-pin

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj tamper-pin`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: existing resolve! directly; proposals do not enforce pins.
- Raw output: [stdout](logs/tamper-pin.stdout), [stderr](logs/tamper-pin.stderr); [invocation](logs/tamper-pin.json).

### borrow-review

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj borrow-review`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: existing receipt! directly on old review and new subject.
- Raw output: [stdout](logs/borrow-review.stdout), [stderr](logs/borrow-review.stderr); [invocation](logs/borrow-review.json).

### global-check

- Command: `bb scripts/merge_witnesses.bb --check`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: full canonical registry validation.
- Raw output: [stdout](logs/global-check.stdout), [stderr](logs/global-check.stderr); [invocation](logs/global-check.json).

### global-regenerate

- Command: `bb scripts/merge_witnesses.bb`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: attempt full canonical publication; expect refusal before write.
- Raw output: [stdout](logs/global-regenerate.stdout), [stderr](logs/global-regenerate.stderr); [invocation](logs/global-regenerate.json).

### inventory-after

- Command: `bb -cp scripts holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj inventory`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: all pinned locators in all fragments after R4 replacement.
- Raw output: [stdout](logs/inventory-after.stdout), [stderr](logs/inventory-after.stderr); [invocation](logs/inventory-after.json).

### merger-controls

- Command: `bb -cp scripts:test test/witnesses/merge_witnesses_control.bb`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: existing merger control suite.
- Raw output: [stdout](logs/merger-controls.stdout), [stderr](logs/merger-controls.stderr); [invocation](logs/merger-controls.json).

### negative-unconditional

- Command: `bb checks/predictive_outcome_kernel_witness.clj --negative-unconditional`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **2**. Scope: affected wrapper negative mode; serial; canonical mathlib4.
- Raw output: [stdout](logs/negative-unconditional.stdout), [stderr](logs/negative-unconditional.stderr); [invocation](logs/negative-unconditional.json).

### negative-softmax

- Command: `bb checks/predictive_outcome_kernel_witness.clj --negative-softmax`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **2**. Scope: affected wrapper softmax negative; serial.
- Raw output: [stdout](logs/negative-softmax.stdout), [stderr](logs/negative-softmax.stderr); [invocation](logs/negative-softmax.json).

### kondo

- Command: `clj-kondo --lint holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **3**. Scope: new Clojure control.
- Raw output: [stdout](logs/kondo.stdout), [stderr](logs/kondo.stderr); [invocation](logs/kondo.json).

### parens

- Command: `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj checks/witness-fragments/PredictiveOutcomeKernel.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: control and fragment parentheses.
- Raw output: [stdout](logs/parens.stdout), [stderr](logs/parens.stderr); [invocation](logs/parens.json).

### positive-receipt-diagnostic

- Command: `bb -cp . -e '(require (quote [checks.positive-proof-receipt :as r]) (quote [clojure.edn :as e])) (prn (r/validate (e/read-string (slurp "holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn"))))'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: diagnose wrapper positive receipt; printed pass? is authoritative, exit only means diagnostic completed.
- Raw output: [stdout](logs/positive-receipt-diagnostic.stdout), [stderr](logs/positive-receipt-diagnostic.stderr); [invocation](logs/positive-receipt-diagnostic.json).

### lean-negative-unconditional

- Command: `lake -d /home/joe/code/mathlib4 env lean -j 1 /home/joe/code/mathlib4/DarkTower/WarMachine/PredictiveOutcomeKernelUnconditionalNegative.lean`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: direct negative elaboration to diagnose wrapper failure; not a replacement wrapper gate.
- Raw output: [stdout](logs/lean-negative-unconditional.stdout), [stderr](logs/lean-negative-unconditional.stderr); [invocation](logs/lean-negative-unconditional.json).

### lean-negative-softmax

- Command: `lake -d /home/joe/code/mathlib4 env lean -j 1 /home/joe/code/mathlib4/DarkTower/WarMachine/PredictiveOutcomeKernelSoftmaxNegative.lean`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: direct negative elaboration to diagnose wrapper failure; not a replacement wrapper gate.
- Raw output: [stdout](logs/lean-negative-softmax.stdout), [stderr](logs/lean-negative-softmax.stderr); [invocation](logs/lean-negative-softmax.json).

### kondo-final

- Command: `clj-kondo --lint holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: final control namespace matches filename.
- Raw output: [stdout](logs/kondo-final.stdout), [stderr](logs/kondo-final.stderr); [invocation](logs/kondo-final.json).

### parens-final

- Command: `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/controls.clj checks/witness-fragments/PredictiveOutcomeKernel.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: final control and fragment parentheses.
- Raw output: [stdout](logs/parens-final.stdout), [stderr](logs/parens-final.stderr); [invocation](logs/parens-final.json).

### baseline-test-inputs

- Command: `git diff --exit-code 43cf8563 -- test/witnesses/merge_witnesses_control.bb checks/witness-fragments/machineObservation.edn checks/witness-fragments/machinePrecision.edn checks/witness-fragments/machineChannelPredictionError.edn`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: test and selected fragments unchanged from starting HEAD.
- Raw output: [stdout](logs/baseline-test-inputs.stdout), [stderr](logs/baseline-test-inputs.stderr); [invocation](logs/baseline-test-inputs.json).

### python-syntax

- Command: `python3 -B -c 'from pathlib import Path; p=Path("holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/run.py"); compile(p.read_bytes(),str(p),"exec"); print("PASS")'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: runner syntax without bytecode writes.
- Raw output: [stdout](logs/python-syntax.stdout), [stderr](logs/python-syntax.stderr); [invocation](logs/python-syntax.json).

### canonical-lean-negative-unconditional

- Command: `lake env lean -j 1 DarkTower/WarMachine/PredictiveOutcomeKernelUnconditionalNegative.lean`
- Cwd: `/home/joe/code/mathlib4`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: diagnostic in exact wrapper cwd to select canonical toolchain; not a substitute wrapper gate.
- Raw output: [stdout](logs/canonical-lean-negative-unconditional.stdout), [stderr](logs/canonical-lean-negative-unconditional.stderr); [invocation](logs/canonical-lean-negative-unconditional.json).

### canonical-lean-negative-softmax

- Command: `lake env lean -j 1 DarkTower/WarMachine/PredictiveOutcomeKernelSoftmaxNegative.lean`
- Cwd: `/home/joe/code/mathlib4`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: diagnostic in exact wrapper cwd to select canonical toolchain; not a substitute wrapper gate.
- Raw output: [stdout](logs/canonical-lean-negative-softmax.stdout), [stderr](logs/canonical-lean-negative-softmax.stderr); [invocation](logs/canonical-lean-negative-softmax.json).

### positive-source-drift-detail

- Command: `bb -cp . -e '(require (quote [checks.positive-proof-receipt :as r]) (quote [clojure.edn :as e])) (let [x (e/read-string (slurp "holes/labs/wm-contract/predictive-outcome-kernel-positive-receipt.edn"))] (doseq [[old new] (map vector (:source-basis x) (r/live-source-basis x)) [a b] (map vector (:declarations old) (:declarations new)) :when (not= a b)] (prn {:path (:path old) :old a :actual b})))'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: read-only identification of stale positive receipt declaration hashes.
- Raw output: [stdout](logs/positive-source-drift-detail.stdout), [stderr](logs/positive-source-drift-detail.stderr); [invocation](logs/positive-source-drift-detail.json).

### final-artifact-check

- Command: `bb -cp scripts -e '(require (quote [witnesses.node-witness :as n])) (let [p "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/" old (n/read-edn (slurp (str p "old-fragment.edn"))) new (n/read-edn (slurp "checks/witness-fragments/PredictiveOutcomeKernel.edn"))) (assert (= (first (:node-witnesses old)) (n/read-edn (slurp (str p "old-entry.edn")))) ) (assert (= (dissoc old :node-witnesses) (dissoc new :node-witnesses))) (assert (= [(n/subject (first (:node-witnesses new)))] [(n/read-edn (slurp (str p "successor-subject.edn")))])) (println "PASS: old entry preserved, wrapper unchanged, successor subject matches"))'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **1**. Scope: historical entry, unchanged wrapper metadata and exact successor subject.
- Raw output: [stdout](logs/final-artifact-check.stdout), [stderr](logs/final-artifact-check.stderr); [invocation](logs/final-artifact-check.json).

### final-artifact-check-corrected

- Command: `bb -cp scripts -e '(require (quote [witnesses.node-witness :as n])) (let [p "holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/" old (n/read-edn (slurp (str p "old-fragment.edn"))) new (n/read-edn (slurp "checks/witness-fragments/PredictiveOutcomeKernel.edn"))] (assert (= (first (:node-witnesses old)) (n/read-edn (slurp (str p "old-entry.edn")))) ) (assert (= (dissoc old :node-witnesses) (dissoc new :node-witnesses))) (assert (= [(n/subject (first (:node-witnesses new)))] [(n/read-edn (slurp (str p "successor-subject.edn")))])) (println "PASS: old entry preserved, wrapper unchanged, successor subject matches"))'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: historical entry, unchanged wrapper metadata and exact successor subject.
- Raw output: [stdout](logs/final-artifact-check-corrected.stdout), [stderr](logs/final-artifact-check-corrected.stderr); [invocation](logs/final-artifact-check-corrected.json).

### python-syntax-final

- Command: `python3 -B -c 'from pathlib import Path; ps=sorted(Path("/home/joe/code/futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1").glob("*.py")); [compile(p.read_bytes(),str(p),"exec") for p in ps]; print("PASS",[p.name for p in ps])'`
- Cwd: `/home/joe/code/futon2`; environment: `LEAN_NUM_THREADS=1`.
- Exit: **0**. Scope: final Python scripts syntax without bytecode writes.
- Raw output: [stdout](logs/python-syntax-final.stdout), [stderr](logs/python-syntax-final.stderr); [invocation](logs/python-syntax-final.json).

## Staging checks

`git diff --check` before staging exited 0. `git diff --cached --check` after
staging exited 2 solely for final blank lines in the retained raw stderr files
`logs/final-artifact-check.stderr` and `logs/merger-controls.stderr`. Those bytes
are preserved as evidence rather than edited. Explicit staged-path enumeration
confirmed only the authorized fragment and this packet; the registry is excluded.
