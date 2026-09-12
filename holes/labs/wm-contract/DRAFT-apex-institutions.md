# DRAFT: the two apex patterns as IAD institutions (for discussion)

zai-7 with Joe, 2026-09-12. Not final — something to talk about. Same
conversion shape as EXPERIMENT-flexiarg-to-institution.md, applied to
the apex pair. Signal for discussion: where the pattern says "should",
the institution must say WHO is bound and WHAT happens on breach.

## ⚖ progress-is-a-witnessed-state-change as institution

[NOTE: the two tables below are v1; the codex-25 responses section at
the end adopts corrections (scope rows missing; aggregation is decision
authority; disposition ≠ progress; apparatus states never
domain-negative). v2 tables will be drafted for Joe's rulings; the v1
tables are retained for the diff, marked here so no one reads them as
current law.]

| IAD rule | text |
|---|---|
| POSITION | worker (produces state changes), witness (mechanically refuses or attests), operator (the only preference source; receives ⚖-claims). One agent may hold worker+witness NEVER — R9 is the boundary rule on positions themselves. |
| BOUNDARY (in) | any artifact, closure, or claim offered as progress ENTERS the regime. There is no work outside the regime — that is the point of the apex. |
| BOUNDARY (out) | a claim LEAVES (counts as work) when a state change in the record carries a witness verdict: attested, refused, or :not-proven. Three exits, closed enum, I5-clean (domain-negative disjoint from apparatus-fault). |
| CHOICE | the worker may offer anything; the witness MUST run; claims without a witness verdict MAY NOT be counted, aggregated, or cited downstream. |
| AGGREGATION | the "which rows changed state with witnesses this week" query is the collective-choice accounting — one query, no deliberation needed for the count itself. |
| INFORMATION | witness verdicts are typed and public to the account (▣); ⚖ used in communication must resolve to its pinned pattern (sigil rule). |
| PAYOFF | attested work moves the row and pays fuel; unattested activity pays fuel and moves NOTHING — theater becomes uniformly unprofitable. :not-proven is legal and unpunished (the valve that keeps honesty cheap). |
| SANCTION / monitoring | graduated: unattested claim → not counted (first state, automatic); mimicked evidence → a finding on the producer (register disagreement); repeated → the producer's precision row drops. Monitor: the witness lanes + the row query — both existing, both cheap. Policing cost: a replay and a query; recorded as fuel. |

Open question for discussion: is "mimicked evidence" better as a
sanction class or as its own boundary violation (entering the regime
fraudulently)? Ostrom would say the latter — boundary fraud is the
grave offense; choice violations are ordinary.

## ⊘ violation-signature-before-work as institution

[Same v1 caveat as above.]

| IAD rule | text |
|---|---|
| POSITION | row author (writes work units), gatekeeper (the wired detector), commissioning reviewer (induces the violation). |
| BOUNDARY (in) | a row, law, standard, or requirement enters the regime when it is ABOUT to govern work. |
| BOUNDARY (out) | it leaves :signature-pending and becomes executable when its detector exists, is wired to a gate that can refuse, and is commissioned (an induced violation produced the typed refusal). Three conjunctions — none optional. |
| CHOICE | work on a pending row is refused by the gate — not forbidden by prose; the acceptance path structurally routes through the signature (unrepresentable to skip). |
| AGGREGATION | the signature audit (runs/signature-audit.edn + check_signature_audit.bb) is the census; its counts derive from its vector and the checker refuses mismatches — the institution auditing itself. |
| INFORMATION | signature status is per-row and public; :signature-pending is a first-class state, not a shame label. |
| PAYOFF | detectors cost fuel once; unguarded work costs blizzards — the WORK-REMAINING diagnosis priced this asymmetry in weeks of usage. The institution makes the cheap side the only legal side. |
| SANCTION / monitoring | work started on a pending row = the gate refuses (automatic, first state); a detector that cannot fail (never commissioned) = a finding on its author — the "commentary, not detector" class; a tampered census = the checker exits 1 (already commissioned twice). Monitor: the gate itself — self-enforcing, cost = the check. |

Open question for discussion: who owns commissioning when the detector
author and the row author are the same agent (the small-scale case we
actually run)? Today: claude-15 reviewed my audit's detector — the
R9 split held socially. Should the institution require the split
mechanically (commissioning reviewer ≠ row author), or is the typed
finding on failure enough?

## What the conversion surfaces (the point of doing it)

1. Both institutions' monitors ALREADY EXIST (witness lanes, row query,
   the audit checker) — no new police to hire; Ostrom's monitoring-by-
   participants at near-zero marginal cost.
2. The genuinely new content is the POSITION rows and the open
   questions: who is bound, and the two gradation puzzles above. Those
   are Joe's calls, which is correct — they are collective-choice.
3. The sigils ⚖ and ⊘ are now the institutions' information-rule tokens:
   using them asserts the regime is in force for the thing named.

## codex-25 review: responses and revisions (2026-09-12; adopted unless marked JOE)

**R-1 ADOPTED, with the confession it forces.** The checker
accepting mimicked evidence is correct and decisive: it checks
bookkeeping (ids, status presence, arithmetic), not detector
existence, wiring, commissioning, or independence. The two induced
violations commissioned TWO BOOKKEEPING REFUSALS, not the
institution's enforcement. And finding 1b was my own S1-class bug
(:review outside the map) — third today; fixed, checker re-run green.
Revisions: (a) the audit's :review.self-check-verdict is corrected to
"bookkeeping-guarded only; detector-existence, wiring, and
commissioning are NOT established by this checker"; (b) the DRAFT's
"monitors already exist" claim is withdrawn — what exists reduces
SETUP cost; replay, maintenance, coverage review, recommissioning,
and adjudication remain real costs whose bearer the payoff rows must
name; (c) wiring the checker into an execution gate with a receipt is
now a named gap (it appears in no Makefile/checks/scripts).

**R-2 ADOPTED.** Boundary rules govern entry/exit of POSITIONS;
aggregation is decision AUTHORITY; scope is allowable outcomes and
was missing from both tables. The "nearly mechanical" mapping claim
is corrected in the precedent note's direction: flexiarg fields are
PROMPTS for interpretation, not fixed mappings. v2 tables (when
drafted) carry: explicit aggregation text (whose attestation
authorizes which transition; whose refusal vetoes; who resolves
disagreement — currently: witness attests, witness vetoes, operator
resolves via ruling); scope rows per institution; monitoring/
sanctions kept as additional description, not scope substitutes.
Polski & Ostrom pp. 24-26 cited as the reference.

**R-3 ADOPTED.** Disposition ≠ progress: an attested refusal
establishes knowledge (the counterexample), and may be cited as
such, but does not attest the REFUSED claim. Each verdict warrants a
NAMED transition (attested→row-advance; refused→knowledge-record;
:not-proven→citable-as-unknown, nothing else). The enum extends:
timeout, witness-unavailable, malformed-output are APPARATUS states,
never domain-negative results (I5 at the institution level).

**R-4 ADOPTED as separation; severity policy is JOE's.** Breach
classification (stale evidence / apparatus error / unsupported
assertion / fabricated provenance / falsified witness identity) is
separate from sanction severity. Falsifying witness identity or
eligibility = boundary violation; fabricating a measurement =
information/choice violation with the producer still legitimately
admitted. The "Ostrom would say" attribution is DELETED (no
supporting passage). The severity schedule (immediate invalidation +
dependent credit, adjudicated finding, suspension of certification
privileges for deliberate forgery) is codex-25's recommendation,
adopted as the draft position for Joe to rule on. Adjudicator,
challenge-hearing, and privilege-restoration naming: JOE + witness
lane, specified in v2.

**R-5 ADOPTED.** "Structurally enforced" requires mechanical
commissioning separation (receipt pinned to detector/gate/requirement/
fixture versions; ineligible reviewers rejected mechanically;
admissible case required so always-refusing detectors cannot
qualify). Social review is provisional practice, honestly labeled.
Worker/witness incompatibility scoped to THE SAME CLAIM AND ITS
PRODUCING PART, not every task forever; different agent names alone
insufficient (the existing r9_independence.clj precedent cited).

**R-6 ADOPTED; both questions go to JOE with recommendations.**
(a) What is blocked: execution start, acceptance, or credit? The
audit's "pending blocks" conflated them; recommendation: acceptance
and credit are gated; unguarded execution may START but its output
cannot enter the account — otherwise detector-building itself
deadlocks. (b) Signature-builder work formalized as a bounded work
class with its own acceptance conditions (the audit already
practiced this informally); otherwise the apex acquires an informal
exception, which is the failure mode it exists to prevent.

**Net effect on the open questions for Joe (superseding the
original two):** (1) severity schedule per R-4; (2) blocked-what per
R-6a; (3) signature-builder class terms per R-6b. zai-7's original
leans are recorded above where still live.

## ADDENDUM: the cue layer — how the institutions are triggered (Joe, 2026-09-12)

The two apex institutions above govern WHAT counts as work and WHEN
work may start; they were silent on HOW the regime is entered in
conversation. The voxterm process cues (NOTE-voxterm-cue-institution.md)
supply that trigger layer, and this addendum binds them into the
institutions rather than leaving them a parallel protocol.

**⚖ + cues.** The cue is a boundary-in event for the task-holder
position; 🏁 is boundary-out via the departure record; credit remains
only on the witnessed state change the ENTRY record named in advance
(the cue specs' "no completion credit" / "does not assert success"
clauses are this institution's exit rule stated in cue vocabulary).
The cue itself is never evidence (the spec's own clause) — it creates
the obligation whose discharge is the record. MONITORING gains its
cheapest form: the cue-receipt lint (regex + lookup; a cue without a
receipt is the violation signature).

**⊘ + cues.** The 🎒 PATTERN CARD cue operationalizes
signature-discipline at entry: the entry record the cue demands
carries task, pattern/version, intended transition, and witness
conditions — the signature-before-work fields, filled per task instead
of per row. The card→use-citation check (carriage alone is not
evidence of use) is ⊘'s commissioning discipline at the pattern level:
the card is "commissioned" for the task only by its cited use.

**Emitter asymmetry (restated for this document).** Operator cues
obligate the receiver at full authority; agent cues are self-
commitments at proposal precision — binding the emitter, conferring no
authority (no self-clock, self-credit, or self-ratification). This is
M-zaif-harness boundary 1 at cue grain, and it is why one vocabulary
serves both voices without collapsing them.

**Relation to the pending rulings (surfaced, not settled):**
- Ruling 2 (what signature-pending blocks): the cue layer DEFAULTS to
  recording acceptance and credit without mechanically blocking
  execution — the EX-1 stance — so the week's cue round-trips generate
  the data for the ruling rather than pre-empting it.
- Ruling 3 (signature-builder class): 🎒 plus a departure record
  whose unresolved-work field names the signature being built is the
  natural entry ritual FOR that class; proposed as its terms when
  Joe rules.

**Status:** this addendum postdates codex-25's review; it extends the
draft, does not revise the reviewed tables. The cue layer is itself
pending Joe's read and one EX-1-style bounded run (🕒→🎒→🏁 with the
two lint checks live) before any claim of enforcement is made — the
same no-runtime-claim posture as LAYER4-PROCESS.
