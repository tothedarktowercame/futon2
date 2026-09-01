# C295 — pending-decision staleness audit

Date: 2026-09-01. Reviewer: `wm-evidence`. The reviewed
`holes/problems/DECISIONS-PENDING.md` was not edited.

## Result

Fourteen entries reviewed: **11 current, 3 stale-and-how, 0 wholly
resolved**. One stale entry contains an old question that has been resolved,
but a narrower replacement decision remains at the same site.

The seven absence sites remain a live population: the canonical absence lint
reports exactly seven Futon2 findings at the same current loci. This audit does
not infer currency from the digest's prose.

| # | Decision | Status | Current evidence and deferral consequence |
|---:|---|---|---|
| 1 | Strategic outcome vocabulary | **current** | `C166-strategic-outcome-stop.md`'s missing finite outcome support and independent usefulness rule remain absent. Deferral still leaves the strategic family noncanonical and unbuilt. New dependency noted below: the policy side also depends on decision 14's cascade semantics. |
| 2 | Authored R16 outward-act binding | **current** | `enact.clj` still produces artifact-only construction; no owner-approved armed external operation, independent read-back, and next-belief observation binding now exists. Tonight's click/certificate work observes execution of the machine; it does not turn R16's construction into an outward act. Deferral remains safe refusal, not runtime breakage. |
| 3 | Avoided-range hard guard | **current** | `free_energy.clj` still emits tri-state diagnostics and the compatibility view; no policy or actuator consumes them as a guard. Deferral still means informational avoidance with no new veto. |
| 4 | Prediction triple | **current** | The lint still finds `free_energy.clj:98-100` coercing missing observation/mean/variance to zero. The omit-channel versus refuse-update choice still changes belief and ranking authority. |
| 5 | Belief aggregation | **current** | The lint still finds `belief.clj:1040-1052`. C195's two immediate-boundary measurements remain directional only; no later repair settled omit-versus-refuse. Deferral cost is still the absent/malformed/measured-zero collapse. |
| 6 | Strategic-mode inference | **current** | The lint still finds `free_energy.clj:138-143`; no partial/prior versus reason-bearing unknown rule has landed. Deferral still fabricates zero-valued features for inference. |
| 7 | Missing sorry pressure | **current** | The lint still finds `policy.clj:144-145`. C195 remains conditional evidence, not observed fallback invocation. Deferral still lets unknown pressure look low. |
| 8 | Validated rollout-step producer | **current** | The lint still finds `rollout.clj:129`; no total `:scored`/`:unscored` producer union has landed. Deferral still admits fabricated zero action scores. |
| 9 | Unscored rollout moves | **current** | The lint still finds `rollout.clj:158`; exclude-versus-refuse remains undecided. This is downstream of decision 8. |
| 10 | Fulab temperature without prediction error | **stale-and-how** | Commit `13ed674` corrected the referent after C226: Fulab now rejects canonical `:prediction-error`, names `:outcome-size-surplus`, and its own generic producer supplies that value. Thus the question as written—what to do “without prediction error”—is resolved by corrected referent: Fulab does not consume prediction error. A different decision remains because `outcome-size-surplus` still defaults absence to zero at current `adapters/fulab.clj:81`; with no live caller, its urgency is lower still. The digest and lint metadata both name the superseded quantity. |
| 11 | Historical versus live Morning Brief QA | **current** | The 72-item historical disposition remains unapplied, and no later operator run has established the proposed live epoch. Deferral now has a sharper operational cost: the next operator run can arrive before the boundary is recorded, leaving the first new item without an agreed historical/live classification. Nothing currently breaks. |
| 12 | Support-typed shadow as live authority | **stale-and-how** | The decision remains real and shadow authority remains explicit in `trace.clj`; however its evidence premise moved. C167/C182/C186 produced two sequentially dependent records (two selector decisions, not 288 trials), zero observed rank changes and zero incomparable pairs, with immediate effects measured for two other absence options. The digest entry points to C108's pre-v18 zero-record census and understates that evidence. Deferral still preserves legacy selection safely; it no longer means “waiting for first evidence,” and the existing evidence is directional, not an adequate independent denominator. |
| 13 | Invoke-jobs ledger backend | **stale-and-how** | C254/C263 repaired the stated acute durability failures: same-directory forced atomic replacement, serialization lock, loud schema validation, pre/post-rename commit semantics, and memory/disk agreement. The entry still says the current writer uses in-place `spit`, lacks locking/force/rename, and can replace torn history with an empty default; those claims are now false. The EDN-versus-SQLite decision remains only as a scaling/publication-cost choice: 134.6 MB and a full-map rewrite per mutation. Deferral costs growing write cost, not silent total loss or unresolved atomicity. |
| 14 | Cascade meet semantics | **current** | `C291-cascade-carrier-repair-blocked.md` is current: serialized `descent` and `co_app` do not establish a meet operation or order theorem. Coverage remains 31/33 in that record, and no partial carrier was added. Deferral preserves an honest semantic ceiling. |

## Dependency order

The digest's chronology is not the cheapest decision order.

1. **Cascade meet semantics (#14) before or jointly with strategic outcome
   (#1).** Strategic outcome itself is independent, but the family it claims
   to unblock includes `StrategicPolicy`, described as a control-pattern
   cascade. Deciding outcome alone no longer completes the carrier family if
   cascade/policy semantics remain blocked.
2. **Validated rollout producer (#8) before unscored-move handling (#9).** The
   latter's population and authority only become explicit once the producer
   emits the scored/unscored union.
3. **Prediction triple (#4) before belief aggregation (#5).** Producer refusal
   versus omission determines which incomplete collections aggregation can
   receive; deciding aggregation first risks answering against the legacy
   fabricated-zero population.
4. **Morning Brief boundary (#11) before the operator-triggered run.** This is
   sequencing against an event, not a dependency on another pending decision.

The outward-act binding, hard guard, mode inference, sorry-pressure fallback,
Fulab surplus handling, shadow authority, and ledger backend are otherwise
independent owner calls at their present boundaries.

## Focused evidence

```sh
bb -cp . checks/preemptive_absence_coercion_lint.clj
git show 13ed674 -- src/futon2/aif/adapters/fulab.clj
git -C /home/joe/code/futon3c show 3be9cc88
git -C /home/joe/code/futon3c show 0b091ee2
```

The absence lint returned its expected live exit 1 with exactly seven Futon2
findings and zero in Futon3/p4ng. No decision digest, behavior, or decision
source was changed.
