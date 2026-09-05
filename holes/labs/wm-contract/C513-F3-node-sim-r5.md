# C513 — :F3: the per-node simulation harness, piloted on R5

Row: `worklist.edn :F3` (class F; epic `EPIC-run-era.md`). Commissioning ruling:
Joe, 2026-09-05 — "the need for per node unit tests. we have enough cores on
this machine that each node could be spun up and simulated by an agent."
Harness: `src/futon2/aif/node_sim.clj`. Carriers: `sim/R5-carriers.edn`.
Runner: `f3_node_sim.clj`. Receipt: `runs/F3-node-sim/00-r5-pilot.edn`.
Tests: `test/futon2/aif/node_sim_test.clj`.

## What running a node alone requires, and what it does not

A control-map node is a set of equations over named symbols
(`aif-equations.edn`). Nothing about evaluating them needs the loop, the
scheduler, a tick or a trace — what they need is their imports supplied as
values. So the harness is three parts kept apart, and the separation is what
makes it a gate rather than a demonstration:

| part | what it is | where it comes from |
|---|---|---|
| the node | the transcription under test | `node_sim.clj:241` (`r5`), or a substituted `:node-fn` |
| the reference | the same formulas, other route | `node_sim.clj:278` (`reference-r5`) |
| the laws | statements the output must satisfy | `simulate-node`, `node_sim.clj:382` |

The reference evaluates the same two formulas over **exact rational masses with
prime-factorised logarithms** — one `Math/log` per prime, at the end. It is not
an independent derivation of the formulas; both routes transcribe
`Holes.lean:6867-6884`. What it is independent of is **the node**: it reads the
carriers, so a formula planted in the node has nothing to hide behind. That is
the whole mechanism by which the negative control works.

The prime coefficients are in the receipt, and they are the part a reviewer can
check without running anything:

    risk(acquisition)  = -29/32 ln2 + 55/64 ln5 + 7/32 ln7 - 25/64 ln11
    risk(review)       = -23/32 ln2 +  7/64 ln7 + 13/32 ln13
    ambiguity(either)  =  17/8  ln2                       (= 2.125 bits)

## The pilot: R5 against the kernels F1 pinned

R5 is `:risk`, `:ambiguity` and `:expected-free-energy`
(`aif-equations.edn:116-142`), at the declared-outcome-alphabet grain. Its
imports come from the F1 composition unchanged — `machine_q.clj:336` for
`Q(s|pi)`, `:356` for `Q(o|pi)` — and R5 is not allowed to read a kernel that
has not first been checked against both `MachineQWitness.lean:234-241`
(25/64 and 11/64 at `ordinary`) and every row of
`runs/F1-machine-q/01-runtime-seam.edn`. Max deviation on both: **0.0**.

| | risk | ambiguity | G |
|---|---|---|---|
| acquisition | 0.24393607778459092 | 1.4729377586898837 | 1.7168738364744747 |
| review | 0.7566450629942134 | 1.4729377586898837 | 2.2295828216840974 |

Eleven checks: ten `:pass`, one `:finding` (below). Node-versus-reference max
deviation 0.0. Two runs over an unchanged tree write a byte-identical receipt
(no wall-clock field).

## The negative control: five planted wrong-formula nodes, five caught

Each is a formula someone could actually write for R5, not a scrambled one.
The receipt records which checks fired.

| planted node | caught by |
|---|---|
| `risk := KL[C‖Q]` (the KL the other way round) | `node-agrees-with-reference` |
| `risk :=` cross-entropy (the `-Σ Q ln Q` term dropped) | `node-agrees-with-reference`, `risk-zero-at-own-row` |
| `ambiguity := H(Q(o|pi))` (predictive entropy, not expected row entropy) | `node-agrees-with-reference` |
| `ambiguity := Σ_s H(P(o|s))` (unweighted by `Q(s|pi)`) | `node-agrees-with-reference`, `ambiguity-in-entropy-range` |
| `G := risk - ambiguity` (the other decomposition's sign) | `node-agrees-with-reference`, `two-term-core` |

Two of the five are caught **twice**, once by a law that needs no reference at
all — Gibbs' equality case (`KL[Q‖C] = 0` exactly when `C` is `Q`, tested by
re-running the node with `C` set to the policy's own row) and the entropy range
`0 ≤ ambiguity ≤ ln|O|`. The sixth control lives only in the tests: an
engineering leg smuggled into the output map is refused by `two-term-core`,
which is the R5 invariant ("the two-term core persisted apart from any
engineering controls") as a checked property of the returned map rather than a
claim about it.

## Three findings, none of them a ruling

**1. The `:ambiguity` row's declared imports name a symbol it does not read.**
`aif-equations.edn:119-120` declares `:imports [:Q-o-pi :A]`, but its own formal
line is `ambiguity(pi) := E_{Q(s|pi)}[H(P(o|s))]` — which reads `Q(s|pi)`, the
predicted **state** distribution, and never touches `Q(o|pi)`. `Q(s|pi)` is an
intermediate of the R4 composition (`machine_q.clj:336`,
`MachineQ.lean:139-146`) that the registry gives no symbol, so there was nothing
else the row could have named. The harness reports this as a `:finding` and not
a failure, because the two things it compares — what the registry declares and
what the transcription reads — are both stated in the tree and the disagreement
is the datum. Repairing the registry is Joe's.

**2. Ambiguity does not separate the two policies, and the reason is exact.**
Both policies get `17/8 ln 2`, so the entire policy-conditioned difference in
the two-term core is risk. This is not a numerical coincidence: the two rows of
`aRow` (`MachineQWitness.lean:115-127`) are **permutations of one another** —
the same multiset `{1/2, 1/8, 1/8, 1/8, 1/16, 1/16}` — so they have equal
entropy, and a weighting of two equal numbers by any distribution returns that
number. No plan over these carriers can make ambiguity discriminate. A carrier
with rows of differing entropy would; F1's demonstration observation kernel was
built to exhibit a difference in `Q(o|pi)`, and that is a different property.
This is the same shape as C487's U4 result reached from the other end — there
the live ambiguity term's zero discrimination was the forward model's variance
coverage; here, on declared carriers, it is the observation rows' equal entropy.

**3. R5 has no `:code` pointer, and the harness is not one.** None of the three
R5 rows carries a `:code` field, unlike `:forward-model` (`efe.clj:601-609`;
`forward_model.clj:279-324`) or `:policy-posterior` (`policy.clj:196-201`). The
alphabet-grain R5 was never wired in `src/`: the live scorer runs per-channel
Gaussians (`core_efe.clj:56-92`) with additive engineering legs
(`efe.clj:842-872`), which cannot consume a distribution over six declared
outcomes. `futon2.aif.node-sim` is the first implementation of these three
equations in this tree **and it is a harness, not a call path** — a passing
pilot is evidence about a transcription and its declared carriers, and about
nothing that ticked.

## What the carriers are

`sim/R5-carriers.edn`: states, alphabet, observation kernel, transition kernel
and plan are `MachineQWitness`'s demonstration carriers field for field
(`:56-67`, `:88-106`, `:115-127`, `:143-147`, `:198-214`), the same numbers as
the runtime fixture at `machine_q_test.clj:20-67`. Two things are added here
and both are declarations, not derivations:

- **the belief reading** is a declared state mass rather than Lean's logistic —
  what R5 consumes is only the mass, and a logistic of an irrational argument
  would put the reference outside exact arithmetic for no gain. Two beliefs are
  declared because the harness **checks** belief-independence rather than
  assuming it.
- **C, the preference distribution**: ordinary evidence at `11/16`, the five
  typed absences at `1/16` each. `:fundamental/machine-preference-distribution`
  is uninhabited and choosing the machine's C is Joe's ruling; this one is
  declared for the pilot and inhabits nothing. Strict positivity on the support
  is the Lean hypothesis (`Holes.lean:6869`) and a zero is refused
  (`:refusal/preference-zero-on-support`), which is the row's own recorded
  falsifier made runnable.

## Gates, bare exits

`clojure -M holes/labs/wm-contract/f3_node_sim.clj` exit 0; two runs write a
byte-identical receipt. `clj-kondo` 0 errors / 0 warnings on all three new
files; `check-parens` OK on all three. `clojure -X:test :nses
'[futon2.aif.node-sim-test futon2.aif.machine-q-test]'` 19 tests, 91
assertions, 0 failures / 0 errors. `negative_controls.sh` PASS (52 negative, 37
positive); `pointer_check.bb` 1453 pointers / 0 unresolved; `worklist_check.bb`
0.

**The suite, and two failures that are not this row's.** `clojure -X:test
:dirs '["test/futon2"]'` — 1,291 tests, 11,106 assertions, **1 failure**:
`war_machine_test.clj:1747` `mission-c-readback-hashes-the-criteria-source-test`,
where a pinned sha256 of a criteria document no longer matches the document.
`clojure -X:test` with no `:dirs` runs only the three root-level `test/*.clj`
namespaces and reports a second, separate failure at
`positive_proof_receipt_test.clj:14` — which fails identically with the two
`:F3` files moved out of the tree, checked. Neither touches this row: nothing
outside `node_sim.clj` and its own test requires `futon2.aif.node-sim` (the
only other matches for the string in `src/` and `scripts/` are prose about the
2026-08-30 role-play). Both are recorded here rather than repaired: they are
someone else's rows.

## Not done, stated

Only R5. The other control-stages nodes are later slices in aif-equations
dependency order, not this row. No live tick and no run lock; nothing written
under `data/`. `gen_aif_dag.bb` not run and nothing regenerated into a publish
(TN §9a). `aif-equations.edn` is read and not written — finding 1 is recorded
here, not repaired. No `:choices` and no `:decisions` entry.
