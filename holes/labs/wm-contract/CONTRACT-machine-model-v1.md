# MachineModelSpec v1 — Wave 0 contract

2026-09-12. Implemented at `src/futon2/aif/machine_model.clj` and
`mathlib4/DarkTower/WarMachine/MachineModelSpec.lean`. Authority: claude-15
reviewer adoption, bell `invoke-1789237525807-20463-e7497b3e`, following
`SPEC-fundamentals-build-2026-09-12.md` (`4d6670aa`) and
`RULINGS-walkthrough-2026-09-12.md:222` (Item 5). All paths below are relative
to `/home/joe/code` unless explicitly repository-relative.

This is structural admission of an explicit model contract. It is not a
machine instance commissioned against production, a correct-dynamics theorem,
a full-node certificate, or permission to start a qualifying run. No tick
consumer has been changed. Phase-1 owners must use the explicit refusal APIs;
this namespace cannot prevent code that does not call it from inventing defaults.

## Adopted bindings (commission text verbatim)

> R1 STATE: entity-indexed categorical over the seven statuses
>    (MachineBeliefState.lean:25 carrier). Constructor APIs take an
>    explicit entity context; multi-entity policies require a
>    separately typed joint construction; summing/averaging entity
>    rows is a typed refusal, never a fallback.

Source: `mathlib4/DarkTower/WarMachine/MachineBeliefState.lean:25` and
`futon2/src/futon2/aif/belief.clj:36`. Runtime accepts `:belief :mode
:single-entity`, validates the entire contextual posterior and rejects any
other combination mode or policy entity. Lean binds the contextual kernel to
the stored posterior and requires every admitted policy to have that entity.
Other entities may remain in storage; their posteriors are not combined.

> R2 OUTCOME: one C on the tagged sum per aif-equations.edn:217.
>    Support derived from the gated authority
>    (full_loop_cohort.clj:31-33 via ruled_outcome_c.clj), with the
>    12-vs-14 correction trail RECORDED from source in the contract
>    doc — pin what the authority yields today, cite the older ruling
>    text, do not hard-code either count. The evidence vertex stays
>    :unruled :carrier :owed exactly as ruled_outcome_c.clj:36 has
>    it: the contract represents that as a named open obligation
>    with a refusal on any consumer that needs it, not a filled-in
>    vocabulary.

`futon2/holes/labs/wm-contract/aif-equations.edn`, choice `:outcome-domain`,
retains the older ruling's fourteen terminal dispositions/nine named zeros.
`futon2/src/futon2/aif/full_loop_cohort.clj:26` currently declares fourteen
attempt-close outcomes, including two historical verification outcomes.
`futon2/src/futon2/aif/ruled_outcome_c.clj:22` explicitly excludes
`:historical-verification-awaiting-validation` and
`:historical-verification-refused` from the disposition domain; its
`disposition-outcomes` is set difference, not a copied number. Thus the
**observed source-derived counts today are 14 − 2 = 12 dispositions,
five positive seed entries and seven named zeros**. This was checked by
loading these namespaces in a separate read-only process. The contract
computes its tagged support from that authority each time, never hard-codes
12 or 14, and rejects an instance from a different authority snapshot.

`ruled_outcome_c.clj:36` retains the evidence vocabulary as owed. V1's available
kernel support is the organization region of the tagged sum, with the missing
evidence region explicitly open; this must not be described as completed full
C. `require-outcome-vocabulary` refuses evidence and any unavailable/named-empty
region. The Lean contract requires organization-tagged supported outcomes and
carries `EvidenceVocabulary.owed`; its evidence consumer returns false.
Future support evolution requires reviewed corresponding contract changes,
not silently dropping the open obligation. No C weights are constructed here.

> R3 B/A AUTHORITY: every A and B carried by the contract bears an
>    explicit :authority field — :declared-prior or
>    :observed-estimate — and consumers can read it. No numeric
>    dynamics may enter with :observed-estimate unless backed by a
>    measurement pointer; declared priors are legitimate content but
>    must say so. Identity-B (belief.clj:214) is representable only
>    as :declared-prior with its own name, never silently.

Source: `futon2/src/futon2/aif/belief.clj:214` names identity B;
`mathlib4/DarkTower/WarMachine/Holes.lean:7037` names the controlled carrier.
Every kernel has `:name`, `:authority`, and complete `:rows`. Observed kernels
also require a local `:measurement {:path ... :sha256 ...}`. Admission checks
file existence, byte hash, strict single-form EDN and a record with
`:schema :wm/kernel-measurement-v1`, matching `:kernel/name`, exact `:rows`, and
`:outcome :measured`. A digest of an unrelated file cannot support the kernel.
These checks bind the supplied record; they do not authenticate the measurer or
prove that the record truthfully describes an experiment. Independent review
and production matching remain Phase-1 obligations. Identity B is recognized
from its actual rows and cannot be relabelled observed, even with a matching
measurement record. Lean's Authority type distinguishes the two cases and
requires an explicit declared authority for identity B.

> R4 THETA: contract v1 is explicit finite support over registered
>    machine-model parameter hypotheses, each with its real
>    likelihood and a recorded prior. Continuous Dirichlet parameter
>    uncertainty is OUT of contract v1, recorded in the contract doc
>    as a named exclusion requiring a new probability representation
>    (Holes.lean:7015 finite ProbabilityKernel cannot carry it).

Source: `mathlib4/DarkTower/WarMachine/Holes.lean:7015` and parameter carriers
at `:7026` and `:7030`. Each hypothesis has id/revision, a full likelihood
kernel over the same state/outcome domain, and a registration path/hash. The
strict EDN registration must carry `:schema :wm/parameter-hypothesis-v1`, the
same id/revision and the exact likelihood. The prior has exactly those
hypothesis IDs as its support and sums to one. A registration reference is an
external authored artifact, not a second mutable registry maintained here.
Lean requires a finite, nonempty, duplicate-free support, named registrations,
likelihood kernels and a normalized prior. Continuous Dirichlet uncertainty
is the named exclusion `:continuous-dirichlet` (and any non-v1 parameter kind
is refused); a concentration-normalized mean is not its posterior. Supporting
it requires a new probability representation, not an epsilon or covert binning.

## Serialized contract and refusals

The required keys are `:schema`, `:model`, `:context`, `:state-support`,
`:belief`, `:actions`, `:policies`, `:outcome`, `:A`, `:B`, `:D`,
`:policy-prior`, `:parameters`, `:observation-encoding`, `:semantics`.
Model, policy, cascade, parameter hypothesis and encoding identities carry
nonempty ids/revisions. Policies carry complete action sequences, contextual
entity identity, cascade identity/revision and unique node identities. This is
identity/payload admission, not a fold's acyclicity or semantic-warrant check.
A's row indices are states; B's are `[state action]`. D is a complete state
row. Finite support is explicit, nonempty, duplicate-free and matches all row
keys. Every mass is finite/nonnegative and every row sums exactly to one;
ratios are supported and no automatic normalization/tolerance is introduced.
All zero-mass keys remain present. Impossible conditioning is declared a typed
refusal; no posterior constructor is implemented in Wave 0.

`validate` returns `{:ok true :schema :wm/machine-model-v1
:open-obligations #{:evidence-vocabulary}}`, or `{:ok false :refusal
{:kind ... :path ...}}`. Refusals include missing fields/support/identity,
duplicate support, nonfinite/negative mass, unnormalized rows, mismatched
kernel input/output support, absent/undeclared/mismatched authority, missing
or changed measurement artifacts, mismatched hypothesis registration,
entity averaging, joint construction required, altered outcome authority,
encoding mismatch, undeclared actions, prohibited defaults and excluded
parameter representation. `require-producer` refuses missing or non-callable
producers: it cannot substitute identity B, uniform belief, point-mass Q or
Gaussian predictions. Legitimate explicit degenerate model distributions are
allowed; missing producers are never filled by them.

## Executable example and validation

`futon2/test/futon2/aif/machine_model_test.clj` provides `example` and
`with-example`. This constructs an explicitly **declared reference prior**:
entity `mission/example`, seven-status uniform posterior/D, action `:inspect`,
policy/cascade identities, named identity B, organization-tagged deterministic
A, and one registered parameter hypothesis whose actual likelihood is A.
The registration is a temporary pinned EDN artifact; it is not a production
registry write or empirical claim. The complete example validates, while an
evidence-vocabulary request is refused.

Reproduce in futon2:

```sh
clojure -M:test -e '(require (quote [futon2.aif.machine-model :as m]) (quote [futon2.aif.machine-model-test :as t])) (t/with-example #(prn (m/validate %)))'
clojure -X:test :nses '[futon2.aif.machine-model-test]'
```

The Lean module states the typed structural obligations over existing
`GenerativeModel`, `ProbabilityKernel` and `machineBeliefState` carriers.
Its hash/pointer fields are formal data, not a filesystem-hashing theorem;
the Clojure validator performs the byte resolution. Elaborating this module
does not prove all Clojure executions implement it. The measured-against-
production witness remains required for every Phase-1 constructor.

### Executed gates and pins

Lean contract commit: `57130bd716781df2987b8c06120f1a72fec87ceb` (mathlib4).
`lake env lean DarkTower/WarMachine/MachineModelSpec.lean` exited 0 in a separate process, using mathlib4’s own package authority. The final scoped Clojure run passed **3 tests / 40 assertions**, with zero failures/errors. clj-kondo reported zero errors/warnings; check-parens returned OK; scoped staged diff checks passed. No production match or live integration is claimed.

| Source | SHA-256 |
| --- | --- |
| `futon2/src/futon2/aif/machine_model.clj` | `26d1542ec08256b3388300e6e884c223ae20864545c4e0743eb9ae76bca167be` |
| `futon2/test/futon2/aif/machine_model_test.clj` | `c03ffe0f6eb292c8d9cd4b19abad3118f6bf3cb588384ea5264409f962b53de3` |
| `mathlib4/DarkTower/WarMachine/MachineModelSpec.lean` | `c433d6c890debc0f33028208d085f56271d63d98055ad75bc957e7b08f4732d7` |
| `futon2/src/futon2/aif/full_loop_cohort.clj` | `f36bffb4e081df76d925ba8d2b2501d0e95190845e7c81934a052f857bed72cf` |
| `futon2/src/futon2/aif/ruled_outcome_c.clj` | `8d8a680268d580267e3c534366dc6b77e0f18bf43234a37babaac986580a078b` |
