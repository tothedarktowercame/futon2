# Proposal: a resolved node witness in the existing declaration registry

Date: 2026-09-12. Status: DISCOVERY / PROPOSAL, for claude-15 review and
Joe/steward disposition. This is the Box 3 instrument lane, not F11 execution.
No registry, generator, generated section, runtime, or canonical Lean change is
made by this packet. All paths below are relative to `/home/joe/code`.

## Definition proposed for review

A node is **witnessed** only when its preceding three rungs resolve, its
FUNDAMENTALS cap permits the step, and at least one exact node–equation–
declaration binding has an admissible, pinned, claim-bearing witness in the
existing witness registry. Admissibility requires a typed artifact, a resolved
subject and quantity, a specific claim with scope and polarity, and retained
kind-specific verification evidence at the claimed pins. A file's existence,
a self-declared `:state :witnessed`, a source theorem name, or a test fixture
alone is insufficient. Report the witnessed binding and scope, not merely the
node's highest rung: this is existential coverage, not verification of all
node equations, all inputs, or production behavior.

This deliberately permits a witnessed **counterexample**, with `:polarity
:refutes` and its exact claim. It must never print that a refuted general claim
was established. An absent candidate leaves the rung unchanged; a claimed
admissible witness that is malformed, dangling, mismatched or unsupported
refuses generation. Explicit proposals awaiting evidence are not readiness
claims and must be shown as pending, never counted.

## What the current instrument actually reads

- `p4ng/empirics-futon/gen_rnode_dossiers.py:319` derives the first three
  rungs; its declaration join uses the holes contract and **Holes.lean only**.
  Its later declared-readiness path (`:readiness` / `:licence`) checks a
  resolving pointer and caps, but does not validate a typed node witness.
  That path must not remain an alternative way to assert witnessed.
- `futon2/checks/witness-registry.edn:1` is generated. The editable authority
  is `checks/witness-fragments/`, merged by
  `futon2/scripts/merge_witnesses.bb:28`. The merge rejects duplicate
  declaration ownership. Keep this one coverage accounting (V5).
- `futon2/checks/witness-fragments/ObservationVector.edn:1` already carries
  a positive-proof receipt, negative controls, and explicit evidence limits.
  Extend this convention; do not create a separately maintained node ledger.
  ObservationVector is not the R2 equation's declared machineObservation.
- A real commissioning shape is
  `futon2/holes/labs/wm-contract/runs/RUN4-fold-wiring-gate-2026-09-12/commissioning.edn:1`:
  mechanism hashes, induced cases, observed outcomes, test results and scope.
  Its `:state :witnessed` does not by itself identify an R-node equation.
- A real replay shape is
  `futon3c/test/futon3c/apm/live_job_driver_test.clj:1944`:
  acceptance-ordering provenance supplies source paths and hashes. The
  associated retained capture and assertion must be resolved too. A hash of
  a mutable live path is provenance, not a guarantee that those original
  bytes remain retrievable today. No live queue files were read for this note.

## Carrier and authority

Add optional `:node-witnesses` to the **owning declaration fragment**; preserve
all existing top-level witness fields and the generated vector format. An
entry contains schema version, node, equation, quantity, declaration, witness
kind, artifact locator, claim sentence, polarity, scope, as-of date, and
verification locator/status. The artifact locator contains repo, repo-relative
path, SHA-256 and a precise selector (qualified declaration, EDN record/case,
or replay record/assertion). Module plus declaration is a selector, not a
substitute for a revision/byte pin. Verification records must pin relevant
source/import or runtime/checker context, not just their own output text.

The fragment's `:witnesses` must include the bound declaration. Multiple
witnesses belong in that fragment; do not add a second owner. An optional
human-readable claim ID is unique within the registry and immutable in
meaning; changing the claim or pins creates a new revision. The generation
input digest includes the registry and every resolved dependency.

The prose subject correspondence needs steward review: a sentence mentioning
“R2” cannot prove that an unrelated theorem is about observation. Require a
review locator resolving the exact typed subject, claim, artifact and pins;
validate the binding mechanically as well. Review does not replace artifact
verification. Pending records may retain missing evidence as explicit nil,
but admitted records must have none of these missing fields.

Kind-specific evidence:

| Kind | Required resolution and retained evidence | Maximum claim here |
| --- | --- | --- |
| `:lean-witness` | Qualified theorem at pinned module; successful checker receipt tied to that module, dependency/toolchain authority and theorem/axiom checks; reviewed connection to the bound quantity | The exact formal proposition under its recorded assumptions |
| `:commissioning` | Pinned commissioning record; exact induced case, production entry point and mechanism pins; expected and observed outcomes and executed result; resolve cited transcript/receipt dependencies | The named induced behavior at those pins |
| `:replay-pin` | Retained original capture at its hash, exact capture selector, pinned replay assertion/checker and successful executed replay receipt; source identity retained | The asserted behavior on that captured input |

A replay fixture with only a source hash is pending if the capture is missing.
A commissioning record cannot borrow the credibility of the Lean kind, or
vice versa. Historical evidence is evaluated at its historical pins; as-of
is the observation date, not a freshness guarantee for current production.

## Proposed generator behavior and refusal commissioning

1. Load fragments through the existing merge/schema workflow, not an additional
   ledger. Check schema version, unique IDs, owning declaration and all typed
   fields. Resolve the node roster and the exact equation's declaration and
   quantity; do not combine one equation's type with another's formula.
2. Resolve repo-relative paths against explicit repository authorities; reject
   traversal and resolution outside those roots. Read each artifact once,
   check its hash, then check selectors against those same bytes. Line ranges
   must be in bounds, but a line in a comment is not a Lean declaration. Use
   the pinned declaration census/checker receipt, not substring matching.
3. Follow and pin verification/review dependencies, including retained capture
   bytes for replay. Reject unsupported kind, missing proof/check result,
   failed result, mismatched source context, or a mismatched semantic subject.
   Never convert a nil checker exit to success.
4. Derive witnessed only after the preceding rungs and cap check. Existing
   declared readiness may reference this derived claim, but a bare licence
   pointer cannot raise readiness above the available typed evidence. Higher
   rungs retain their separate obligations; this packet adds none of them.
5. Resolve all claims before publishing output. On refusal retain the prior
   output unchanged and return a nonzero result naming the claim and cause.
   Consumers must not mistake a stale output for a successful regeneration.

Before implementation is accepted, commission this resolution gate using a
retained, actual-shape positive witness and mutations of that same record:
missing file, one-byte artifact change, selector in a comment, incorrect
node/equation/declaration, nil or failed checker receipt, changed dependency
pin, missing replay capture, and commissioning expected-outcome substituted
for observed outcome. Every false admission claim must refuse with a specific
reason. Also exercise pending evidence, partial coverage and a FUNDAMENTALS
cap: these must not promote the node. A bare readiness licence must fail to
bypass those checks. These are **proposed controls**, not executed tests of a
generator change in this packet.

## R2 worked example: exact candidate, with two unresolved prerequisites

The binding is `:observe / :o / :R2 / machineObservation` at
`futon2/holes/labs/wm-contract/aif-equations.edn:74`.
The declaration is at
`mathlib4/DarkTower/WarMachine/MachineObservation.lean:58`;
its reference theorem is at
`mathlib4/DarkTower/WarMachine/MachineObservationWitness.lean:20`.
The proposed owning fragment is `checks/witness-fragments/machineObservation.edn`
(which this packet does not create). Its new field would contain this entry:

```edn
{:schema :wm/node-witness-v1
 :id "R2-observe-empty-fourteen-v1"
 :status :proposed
 :node :R2 :equation :observe :quantity :o
 :declaration "machineObservation"
 :kind :lean-witness
 :artifact
 {:repo "mathlib4"
  :path "DarkTower/WarMachine/MachineObservationWitness.lean"
  :sha256 "2498ed198408a6ef991de1da6d2cd43dfccac612629bcacaabc8d90b9ef5a86e"
  :module "DarkTower.WarMachine.MachineObservationWitness"
  :declaration "DarkTower.WarMachine.MachineObservationWitness.emptyObservationHasFourteenZeros"
  :lines [20 23]}
 :subject-artifact
 {:repo "mathlib4" :path "DarkTower/WarMachine/MachineObservation.lean"
  :sha256 "f864610a2d794b57fa18d8b3b457faf7c426015f4c01ec5c989df292adbb10a0"
  :declaration "DarkTower.WarMachine.MachineObservation.machineObservation"}
 :claim "For zero-valued inputs and absent variants, the R2 reference model's machineObservation numeric projection has fourteen zero coordinates in Channel.all order."
 :polarity :supports
 :scope :reference-model-empty-input
 :verification {:status :unresolved-at-pin :receipt nil}
 :review {:status :proposed :receipt nil}
 :as-of "2026-09-12"}
```

This is an exact **pending** example, not a fabricated admitted record.
The F8 handoff at
`futon2/holes/labs/wm-contract/runs/F8-observe/codex-handoff-summary.md:25`
reports successful direct Lean, build and axiom checks and identifies source
commit `169662b19653d20c76274bdce4845ab414da10cf`. Its witness-file hash is
`81a8d33df84db80cc0716d6a8fb673aa453f9774072a12336b39328af1a88710`,
which differs from the proposed current pin. The current census at
`futon2/holes/labs/wm-contract/runs/F8-observe/lean-state-join-check.edn:514`
records the current witness hash but has nil typecheck exit. Neither licenses
claiming successful verification of the current bytes. The old receipt may
be usable with its historical dependency closure; that is a separate check,
not permission to attach it to the current file. No Lean check was run here.

There is also a lower-rung resolution gap: machineObservation is absent from
Holes.lean and holes-contract.json. Under the inspected generator's derivation,
R2 stops at **named (1)**, not formula-transcribed (3). The separate F8 census
resolves the declaration in its actual module; the dossier generator does not
consume that resolution yet. Do not bypass this by declaring rung 4.

Exact transition: with current inputs **1 → 1**, pending witness shown. After
separately reviewed multi-module type/formula resolution, and after supplying
verified-at-pin and reviewed-subject receipts, this entry could license
**3 → 4**, provided the FUNDAMENTALS cap permits it. It cannot license 5.
This conditional example is the honest limit of the inspected evidence.

Exact refusal: change only the candidate's status to admitted with the nil
verification receipt retained. Generation must stop with
`node-witness-verification-missing` for `R2-observe-empty-fourteen-v1`.
After a real receipt is supplied, changing its theorem selector to
`emptyObservationHasFifteenZeros` must stop with
`node-witness-declaration-missing`; a file/line existing must not rescue it.

## Boundaries and recorded inspection

Witnessed does not answer the FUNDAMENTALS inhabitant question and does not
license constructed, wired, validated or run-correlated. A reference theorem
is about its model. A bounded commissioning case is about its induced case.
Neither supplies the continuous run's correlation evidence. Preserve
`:lean-attestation :suspended-vocabulary` unchanged (V4); this proposal does
not restore suspended runtime language. Preserve the existing cap even if a
witness is available and report the blocked transition explicitly.

Inspected source SHA-256 pins (file contents, not a claim of clean repositories):

| Source | SHA-256 |
| --- | --- |
| p4ng/empirics-futon/gen_rnode_dossiers.py | 22e1ff9ee891bf8296af6ea6489a61995f0ed418c397ee942a7e3ecc2fee4707 |
| futon2/checks/witness-registry.edn | b399c6afd964569fa5ee785eea68e8b7d633ed6d88f256c4f5e119f5c94d6a69 |
| futon2/scripts/merge_witnesses.bb | 82bd265b042a6767128991e499d78cd3f3a72c841bf28dcf1952d0f434df1783 |
| futon2/holes/labs/wm-contract/aif-equations.edn | 51b928561f6d8c13336266f48561295011d3220090c4d8a97b0ebb4cdcb3e99f |
| futon2/holes/labs/wm-contract/FUNDAMENTALS.edn | 169d611af2ce66a23531e00baaea9eba4e8bdf59ee63e28a3444bad11c0a0efa |
| futon2/holes/labs/wm-contract/runs/F8-observe/codex-handoff-summary.md | 9035b64c5408dc2e117176616276bf5d02b89ba1a9da82b01225d60b389e5e19 |
| futon2/holes/labs/wm-contract/runs/F8-observe/lean-state-join-check.edn | 838937ca19d5df68e4a635b386e3d377ce934756ac06cfc14c52899d2bea5121 |

Validation for this note: source/line pointers and listed current hashes
checked read-only; historical module hashes obtained with git show; proposed
EDN parsed; git diff whitespace check. No generator import/execution (it has
output side effects), merge/regeneration, witness execution, live calls or
registry edits. Existing unrelated worktree changes are outside this commit.
