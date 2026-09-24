# E-cascade-real interpretation probe — seat-B (kimi-7) notes

Date: 2026-09-24. Seat: kimi-7. Input: `../inputs.edn` at futon2 `96fae955`.
Time spent: ~50 minutes (library search via `futon3c/scripts/xlate.py` + direct
`ls`/grep of pattern dirs, reading ~35 candidate flexiargs, writing, gating).

Method: for each target I read the mission document around the unchecked task
lines, searched the library per task, and kept a pattern only when I could say
why *this task* is an application of *this pattern* (citing the task line).
`:needs` follows the mission document's task order or stated dependencies;
`:forbids` names the produced token (same convention as the EIG example — do
not re-apply a pattern to a token already produced). All receipt `:source`
sha256s computed with `sha256sum` against `/home/joe/code/futon3/library/...`;
the path scheme was verified against the EIG example (its pinned
`one-authority-per-question` sha matches the file at that relative path today).

## Gate output

`(futon2.aif.cascade-sources/load-declared ".../probe-interp/seat-B")` in a
fresh `clojure -M` process: **all 10 files load, no refusal.**

```
:files 10 ; one per target
:collisions {}
universes: every target's tokens all observed false (no :unknown)
```

## Constructor preview (not required by the brief; run offline, fresh process)

Budget `{:max-moves 3 :max-expansions 200}`, move-cost 1, `evaluate-g = -(count
:precedence)`. Horizon matters: with horizon 4 the chained targets refuse
`:want-unreachable-within-horizon` (chains are 4–6 deep); with **horizon 8**:

| target | result |
|---|---|
| M-action-cost-modelling | :constructed — full 6-pattern chain |
| M-canon-fingerprint-store | refused :no-supported-order, finding :unproduced-need :hole/h3b2303097926 (the deferred follow-on; expected) |
| M-chipwitz-corps | :constructed — 6 patterns |
| M-daily-scan | :constructed — 2 patterns |
| M-distributed-frontiermath | refused :no-supported-order, finding :unproduced-need :hole/h16025dd5deeb (mentor-session task; expected) |
| M-federated-agency-hardening | :constructed — 4 patterns |
| M-futon-forward-model | :constructed — 6 patterns |
| M-futonzero-generative | :constructed — 2 patterns |
| M-kangaroo | :constructed — 6 patterns |
| M-wm-aif-policy-grain-compliance | :constructed — 4 patterns |

**8 of 10 targets construct; the 2 refusals are typed findings naming exactly
the tokens I declined to interpret, not malformed interpretations.** Note the
D12 caveat: with `evaluate-g = (count :precedence)` (longer = worse) four
targets flip to `:construction-not-taken` — the G injection decides, as I2
found.

## Per target

### M-action-cost-modelling — 6/6 wants interpreted
scale-register (cost signal) → wr-10-next-move-surface (gate CTA) →
campaign-as-temporary-institution (mission-or-campaign decision) →
every-entry-has-a-falsifier (discriminating test) →
explicit-exit-over-abandonment (finding-filed arm) →
shared-standard-has-no-single-owner (campaign resolved). Sequential chain per
document order. Note: campaign-as-temporary-institution cites this mission's
§3.5 as its origin — the strongest grounding in the set.
Rejections: `agent/escalation-cost-vs-risk` (about human escalation, not action
costing); `devmap-coherence/next-steps-to-done` (document hygiene, not the
live tile); `futon-theory/mission-lifecycle` (state machine, not the
ship-or-reject adequacy question).
Missing-token finding: none.

### M-canon-fingerprint-store — 5/6 wants interpreted
tri-store-separation (F1, SQLite placement per §8 trust-level argument) →
canonical-interface (F2, stable query/reduce seam) → status-gated-belief-update
(F3, per-binding posterior: unobserved bindings keep prior) →
smoke-before-the-paid-run (F4, held-out-gold validation with bands) →
startup-integrity-gate (F5, read store on startup gated on rehydration).
F3 stated "alongside" so it needs F1 only; F4 needs F2+F3 (validates what they
built); F5 needs F4 per document order.
**Skipped token**: `:hole/h3b2303097926` (follow-on OEIS-of-steps fingerprints).
The mission itself defers it "until statement fingerprints land" — that
precondition has no token in the want set. Missing-token finding:
*deferred-prerequisite* (the mission's "defer until X" has no token for X;
D13's missing guard vocabulary again). No honest pattern found for it.
Rejections: `storage/deterministic-substrate` (session persistence, not the
scope-binding schema); `library-coherence/library-evidence-ledger` (pattern
citations, not proof fingerprints).

### M-chipwitz-corps — 6/6 wants interpreted
threshold-shaped-events (DERIVE the warrant threshold from the C-score
distribution, both failure tails named); canonical-typed-event-vs-side-channel
(typed PXR channel — the task is almost verbatim the pattern);
pattern-warranted-choice-point (wire the check; needs the channel);
two-layer-calibration (:pxr evidence kind; needs the channel);
missing-dependency-protocol (no-warrant asks → localized library-gap records;
needs the wired check); working-where-others-can-see (the corps discipline the
warrant layer operationalizes — making choice-points visible/auditable).
Rejections: `war-machine/inhabitation-threshold` (entry friction, not warrant
thresholds); `test-registry/bind-warrant-to-the-diff` (review warrants, not
choice-point warrants); `cascade-construction/add-a-pattern-when-an-item-fits-no-class`
(close to the sorry-mining task, but about adding patterns, not localizing gaps).
Missing-token finding: none.

### M-daily-scan — 2/2 wants interpreted
mission-anchored-scan (named-vs-queried misalignment audit — the pattern's
instrument-split criterion is exactly the audit's criterion);
surface-earns-inhabitation (brief-render enrichment per the shallowness
promotion). Independent tasks, both `needs #{}`.
Rejections: `stack-coherence/staleness-scan` (stale deps, not probe
misalignment); `war-room/wr-6-daily-scan-is-depositing-heartbeat` (why the
scan exists, not how to audit its probes).

### M-distributed-frontiermath — 2/4 wants interpreted; 2 declined
single-routing-authority (zcodex configured same as codex = one routing entry
serving two identities); recorded-handoff (tickle bell orchestration as
modeled dispatch objects; needs zcodex reachable first).
**Declined**: `:hole/h16025dd5deeb` (claude-2 Mentor session started,
REPL-driven) and `:hole/h8606857c4374` (Rob has IRC access). These are
human-in-the-loop provisioning/social acts; every candidate pattern
(`agency/self-attribution`, `social/idempotent-handoff`,
`peripherals/surface-earns-inhabitation`) would be a claim that a pattern
application *causes a person to do something*, which I could not write
honestly. Missing-token finding: *external-human-action* — tasks whose
completion is a person's act outside the stack have no pattern-producing
form; maybe they should not be producible tokens at all (D13-adjacent:
guard vocabulary cannot say "waiting on Rob").

### M-federated-agency-hardening — 4/4 wants interpreted
single-routing-authority (tunnel OFF = one authoritative path via uplink) →
idempotent-handoff (reboot: re-announce reconciles-not-redoes) and
single-locus (removing the duplicate static peer entry; both need the uplink
task per the mission's "once the uplink is live") → invariants (final audit
against the named Agency invariant set; needs all three).
Rejections: `realtime/listener-leases` (phantom listener expiry, close to the
reboot task but about listeners not roster reconciliation);
`storage/durability-first` (persistence priority, not reconnect semantics).

### M-futon-forward-model — 6/6 wants interpreted
receipt-then-gate (clj-kondo as an act-gate) → observe-the-authority
(check-parens reads the real files, not a stand-in) →
every-entry-has-a-falsifier (§9.6's per-invariant adversarial traces are
falsifiers) → the-forward-model-is-a-pattern-cascade (the reader reads the
manifold *as* a cascade); read-only-first-then-extend (the no-side-effects
criterion) independent; recorded-handoff (bell-back summary + shas) needs all
gates. §9.8 shows a review PASS already landed — the checkboxes are stale, so
this target may flip `:want-already-observed` as soon as someone ticks them;
the interpretations describe the acceptance work, not new construction.
Rejections: `problems/r4-forward-model` (a problem node, not a pattern);
`measurement/warrant-travels-with-the-number` (metric provenance; the reader
prints an observation summary but the task is run-clean, not dispute-proofing).

### M-futonzero-generative — 2/2 wants interpreted
two-layer-calibration (G-SIM: internal consistency vs MEASURED calibration
pairs from the pilot loop); no-self-certification (G-REWARD: anti-laundering =
the verdict cannot move on evidence its maker manufactured; §4.5's red-team
fixture enumerates the laundering shapes). Independent clearances.
Rejections: `forward-model/reward-supersedes-evidence` (conjectural rung
ordering, not the anti-laundering gate); `math-strategy/preemptive-objection-clearance`
(objection handling, not evidence independence).
Missing-token finding: G-SIM's true precondition (accrued measured pairs) has
no token; written into the receipt's :scope-limit instead.

### M-kangaroo — 6/6 wants interpreted
evidence-over-assertion (W1 keystone fork: settle on the 2-turn evidence or
re-choose the mechanism) → settle-with-meters (warm≪67 s claim measured
against the recorded baseline), checkpoint-the-long-run (crash must not
strand the turn), mode-gate (flag OFF = byte-identical cold path),
single-locus (exactly one warm process per identity; all need W1) →
single-routing-authority (stretch: REPL + agency both route to the one warm
process; needs the singleton).
Rejections: `musn/pause-backtrace` (debugging pause, not crash fallback);
`realtime/branch-parallelism` (feature branches, not runtime mode flags);
`futon-theory/single-source-of-truth` (data authority, not process singletons
— single-locus is the sharper shape).

### M-wm-aif-policy-grain-compliance — 4/4 wants interpreted
on-the-fly-cascade (non-threshold diversity source = candidates derived from
artifact structure, not the pre-declared threshold frontier);
the-slice-is-the-unit-of-use (prefix-local scoring: the enacted prefix is the
slice; score computable from the slice alone, truncation fixture as proof);
interest-event-vocabulary (the return event as a versioned typed
state-transition event, replay-exact); two-layer-calibration (dark shadow:
live layer byte-identical off, shadow layer evaluated with separately named
terms; needs the first three per the task's own enumeration).
Rejections: `war-machine/ambient-pattern-retrieval` (retrieval ambience, not
candidate construction); `meta/baldwin-ratchet-defeats-darkroom` (observation
suppression — thematically near "dark" but about hiding failures, not dark
launches); `ants/cargo-return-discipline` (an ants-simulation policy tuning
document, not a habit-return event).

## Cross-target observations

- **The vocabulary gap is real but small.** Only two want tokens out of 46
  resisted honest interpretation, both for the same reason (a person's act
  outside the stack). One more (the deferred OEIS follow-on) is deferred by
  the mission itself. D11's framing ("agents never got the chance") holds: the
  tokens were interpretable once someone sat down with the documents.
- **Guard vocabulary suffices for these targets** because the wants are
  checkbox tasks in document order; none needed "not until evidence X exists"
  beyond what I could put in :scope-limit. The M-canon follow-on is the one
  place a D13-style withdrawal-as-data guard would have been used.
- **Reuse across targets**: agency/single-routing-authority (3×),
  aif/two-layer-calibration (3×), contracts/every-entry-has-a-falsifier (2×),
  orchestration/recorded-handoff (2×), invariant-coherence/single-locus (2×).
  Within a target, pattern ids are unique (map keys), so the two gate tasks in
  M-futon-forward-model needed two distinct hygiene patterns — which pushed me
  to observe-the-authority, arguably the better fit for check-parens anyway.
- **Horizon**: chains of 6 need horizon ≥ 6 to construct; the sources declare
  no :horizon-steps and the constructor was injected 4 in I2 — chained
  interpretations silently refuse below their depth. Worth surfacing when the
  probe is scored (D14-adjacent).
