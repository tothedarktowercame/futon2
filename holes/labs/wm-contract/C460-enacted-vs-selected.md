# C460 — the enacted action vs the recorded Q(π) selection, per run record

**Date:** 2026-09-01
**Scope:** TN-edge-review worklist V1
**Bearing:** falsifies H1 as written (see §4)

## 1. The question

Do the two actuation paths — `full_loop_runner`'s `selected-entry`
(`futon2/src/futon2/aif/full_loop_runner.clj:870-873`, which matches
`(get-in judgement [:decision :action])`) and `close-loop!`'s first passing act
gate (`futon2/src/futon2/aif/enact.clj:287-316`) — enact the same action on
recorded runs?

## 2. What is available, and what each record can answer

Two families of record exist, and only one of them can bear on the question.

| Record family | Count | Carries `:decision` | Carries `:enactment` / `:act-gate-verdicts` |
|---|---|---|---|
| `holes/labs/wm-contract/tick-run-record-2026-08-{30,31}.edn` | 2 | no | no |
| `data/wm-trace/*.edn` + `data/wm-trace-escrow-witness/*.edn` | 57 | 54 | 5 have gates; 3 have an enacted mission |

The two tick-run records answer nothing here: their keys are
`:entriesLimit :entriesRead :inputIssues :inputsRead :preferenceLayers :route
:route-verdict :selectorSeam :startedAt :storeBasisCount :storeBasisMaxAt
:traceWritten` — no decision, no enactment. Both also record
`:selectorSeam "stub:first-ranked-authorized-mission"`, a declared stub rather
than either production path.

**Three records carry both halves**: `data/wm-trace/wm-trace-2026-07-0{3,4,5}.edn`.

## 3. The comparison, per run

| Run record | Recorded Q(π) selection (rank 1) | Act gates | Enacted | Verdict |
|---|---|---|---|---|
| `wm-trace-2026-07-03.edn` | `:address-sorry` → `:sorry/pudding-g1-arrow-witness-binding` | `M-canon-fingerprint-store` fail, `M-bayesian-structure-learning` pass | `M-bayesian-structure-learning` | **not determinable** — the selection is an action on a sorry, the enactment is a mission; no join key |
| `wm-trace-2026-07-04.edn` | `:advance-mission` → `M-first-flights` | `M-canon-fingerprint-store` fail, `M-bayesian-structure-learning` pass | `M-bayesian-structure-learning` | **differs** |
| `wm-trace-2026-07-05.edn` | `:advance-mission` → `M-first-flights` | `M-canon-fingerprint-store` fail, `M-bayesian-structure-learning` pass | `M-bayesian-structure-learning` | **differs** |
| the other 54 traces | present in 54 | no enacted mission | — | **not determinable** — actuation not recorded |
| `tick-run-record-2026-08-{30,31}.edn` | absent | absent | — | **not determinable** — neither half recorded; stub selector |

For 07-04 and 07-05 the vocabularies do join: the selection's `:target` is a
mission id, and so is the enactment's `:mission`. They are different missions.

## 4. The mechanism, not just the mismatch

In `wm-trace-2026-07-04.edn` there are 110 ranked actions. Rank 1 is
`:advance-mission "M-first-flights"`. Two missions reached the act gates —
`M-canon-fingerprint-store` (fail) and `M-bayesian-structure-learning` (pass) —
and **both are present among the ranked-action targets**, so gating was not
restricted to unranked candidates. The rank-1 selection was simply not gated.

So the divergence is not a tie broken differently. The selected policy never
reached the gate stage, and a lower-ranked candidate was enacted because it was
the first to pass. That is `close-loop!` behaving exactly as written — first
passing gate, ordered by the ranking, not by the decision — over a candidate set
that did not include the decision.

## 5. Bearing on H1

H1 (`enactedActionEqualsSelected`, `mathlib4 DarkTower/WarMachine/Holes.lean:6596`)
holds open the claim that the enacted action equals the recorded selection on
every path, with falsifier "a run in which the enacted action differs from the
recorded Q(π) selection".

**Runs 2026-07-04 and 2026-07-05 are that falsifier.** H1 was written on
2026-09-01 naming V1 as its evidence source; V1 supplies the evidence and it
refutes the claim rather than supporting it. H1 should be revisited by its
reviewer — as a refuted claim closed by record, or restated as a bound on when
the paths agree — rather than left standing as an open hole.

Not amended here: this report is V1's deliverable, and rewriting another row's
Lean declaration on the strength of it is the reviewer's call, not the
reporter's.

## 6. Limits

- The three informative records are from July 2026. The code carries later
  flags (`v0.15` multi-horizon, `v0.25` R3d aggregation), so whether the
  divergence persists on current code is **not established here**. H1's
  falsifier says "a run", not "a current run", so the refutation stands as
  stated; a claim about today's behaviour would need a fresh run.
- No record pairs a `full_loop_runner` enactment with a decision. This report
  therefore compares the recorded selection against `close-loop!`'s enactment
  only. Whether `full_loop_runner`'s `selected-entry` path agrees with the
  decision is **not found** in any available record — by construction it should,
  since it matches on `[:decision :action]`, but that is reading the code, not
  a run.
- `war_machine.clj` calls neither `close-loop!` nor `enact!` (C451, D3b), so
  the judge path that writes most traces cannot produce an enactment. That is
  why 54 of 57 traces carry a decision and no actuation.
