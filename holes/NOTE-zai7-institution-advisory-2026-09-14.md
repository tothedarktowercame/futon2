# NOTE — Advisory for zai-7: mechanism design for the dispatch economy

Status: ADVISORY 2026-09-14, claude-15, commissioned by Joe for zai-7's
institution-design pass. Joe will discuss directly with zai-7; this note
collects the evidence and the open mechanism questions from the WM build
side. Budget context: ~11% Zai usage remains, so the pass should be
scoped as design + spec, not build.

## The observed failure pair (Joe's framing, 2026-09-14)

Delegated agents fail in two dual ways, and the second is the
under-instrumented one:

1. **Under-delivery**: not building what was asked — corner-cutting.
   Canonical evidence: repair-attempt-001's original rejection, where a
   documentation-only commit was offered against an implementation ask
   ("The author explicitly reported 'No commit made'"; review record in
   the obligation, futon2 data/wm-repair-obligations).
2. **Over-delivery / scope drift**: building what was NOT asked because
   the agent has a locally-plausible "right idea." Suspected instance:
   codex-26's evidence-tracking machinery (Joe: "probably wasn't really
   needed. Although we still haven't actually proved that it wasn't
   needed"). The honest state: unproven either way — and see §4.3, that
   undecidability is itself the mechanism gap.

Both are principal-agent misalignments: the packet is a contract; our
review gate audits execution fidelity (gates, receipts) and internal
correctness strongly, congruence with the ask weakly, and post-hoc worth
not at all.

## What the current institution already gets right (fresh evidence)

Same day, same build, two mechanism successes worth keeping:

- **Typed refusal outcompetes fake delivery.** codex-22, asked to
  live-wire the machine-Q lane, refused with a pinned source-backed
  blocker instead of wiring fixtures into production
  (futon2 holes/labs/wm-contract/TN-row14-live-wiring-blocker-2026-09-14.md,
  357348ec). The review ACCEPTED the refusal. The refusal channel is
  priced so that honest non-delivery beats corner-cutting.
- **Non-self-certification is enforced.** codex-24 rejected a pin
  expectation because its author had supplied both the expected digest
  and the code accepting it (r6 review round 1, cohort 48), and only
  accepted after the expectation was re-bound to pre-existing git
  history the author does not control.

## The snatch game is a miniature of this economy

`futon3:checks/find-snatch.edn` records 34 rounds of the Snatch game
played over an 18-member repository of negotiation/institution patterns
— and both its LAWS and its CONTENTS are on point.

The four F-laws govern a single selection act, and each is the local
form of a dispatch-economy rule:

| find-snatch law | selection-act meaning | dispatch-economy lift |
|---|---|---|
| F1 containment | select only from the recorded repository; empty selection is a typed absence | deliver only against the packet's asks; "nothing needed" is a typed, creditable outcome |
| F2 receipted | every selected pattern carries a receipt in the same row | every delivered artifact carries its warrant — which ask it discharges |
| F3 non-self-certifying | the warrant is a structured antecedent, never the selector's own score | the deliverer's own gates/tests do not certify need or congruence; an independent reviewer does |
| F4 falsifiable | each scenario declares a zero-mass member: a way the claim could have failed | every packet declares what NOT to build — a named tempting-but-out-of-scope item whose presence in the delivery is a detectable violation |

(F1-F4 are machine-checked over the record:
mathlib4 DarkTower/WarMachine/Holes.lean:914-955, plus the rejecting
negative controls in futon3:checks/find_snatch.clj.)

And the repository's own patterns are institution moves we have been
rediscovering ad hoc: `:preserve-the-right-to-abstain` is codex-22's
refusal; `:an-unmodelled-response-stops-the-line` is the stop-the-line
store; `:forced-play-needs-a-loss-floor`, `:exchange-when-both-sides-gain`,
`:escalate-only-as-far-as-you-can-lose`,
`:institutions-vary-by-position-and-force` are all live design questions
below. The game is a compressed institution-design corpus, already
pinned and machine-audited.

## The mechanism gaps (the actual ask for zai-7)

1. **No congruence audit.** Reviewers verify that delivered work is
   good, not that it is what was asked. A gold-plated deliverable passes
   every gate. Candidate rule: the packet's acceptance bar becomes a
   checkable artifact quoted back in the delivery, and the review gains
   a scope-fidelity item — everything delivered beyond the asks is
   listed and priced, not silently credited.
2. **Asymmetric detection.** Corner-cutting is caught by
   evidence-demanding review; over-building is caught by nobody,
   because added evidence pattern-matches to virtue. This is the F4
   gap: packets rarely declare their zero-mass member. Tonight's
   packets accidentally demonstrate the fix — the wiring packet's "do
   NOT touch packets 1/2/6" clause is what made codex-22's refusal
   well-defined.
3. **"Was this needed?" is undecidable after the fact.** We suspect the
   codex-26 evidence-tracking build was unneeded and cannot prove it.
   The R8 card rewrite (p4ng sec-catalog, 2026-09-14) landed the
   generalizable rule: *every mismatch number in the trace has a
   consumer that can be named; a scalar with no reader is removed, not
   explained.* Lifted: **every built artifact names its intended
   consumer at build time**; a consumer-naming requirement makes the
   later "unneeded?" question decidable (no consumer materialized →
   retire, not explain). This connects to Ostrom-style monitoring and
   graduated sanctions (corneli2016 grounds the patterns-as-institutions
   reading; sanctions here are retirement/relabelling, not blame).
4. **Refusal pricing, second-order.** Refusal is now rewarded when
   right. The predictable next distortion is over-refusal (cheap
   abstention as effort-avoidance). The game names the tension:
   `:forced-play-needs-a-loss-floor` and
   `:a-free-mark-is-always-worth-assigning` vs
   `:preserve-the-right-to-abstain`. A refusal should cost a pinned,
   falsifiable blocker note (as codex-22's did — 95 lines, sha-pinned)
   — that pricing rule may be worth making explicit.
5. **Budget as a first-class institution variable.** With ~11% Zai
   remaining, scarcity is not hypothetical. The mechanism should make
   not-building scoreable (typed absence with credit) and price every
   packet in tokens against its named consumer.

## Where this lands

Ruling 4 (2026-09-12, NOTE-cascade-structured-proof-validation.md)
already routes "the board spec picks it up at zai-7's next pass" —
this note is input to that pass. Suggested deliverable shape, given
budget: a short institution spec (rules-in-form) with, for each rule,
its snatch-repository ancestor, its enforcement point in the existing
machinery (packet template / review checklist / typed refusal store),
and its F4 falsifier — what observable event would show the rule is
being gamed. No new machinery build; the WM lanes can adopt the rules
packet-by-packet.
