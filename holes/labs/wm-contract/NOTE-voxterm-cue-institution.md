# NOTE: the voxterm process cues as an institution — operator voice regulating agent action

zai-7 with Joe, 2026-09-12. Joe + codex-25 added emoji process cues to
operator turns (🕒 CLOCK IN, 🏁 CLOCK OUT; "Voxterm process cue v1").
Joe's question: how do these relate to the core patterns
institutionally, so a voiced command actually REGULATES agent behavior?
This note binds the cues to the workshop pattern language and to EX-1
(the pattern-card entry experiment), inbox-zero style.

## The binding: cue → pattern → record → detector

The cue is NOT the work and NOT evidence of work (the cue spec says so
itself — preserve that clause, it is the load-bearing one). The cue is
the BOUNDARY-IN event of an institution whose positions, obligations,
and exits already exist across our artifacts:

| cue | pattern it invokes | typed record the agent must emit | existing machinery used |
|---|---|---|---|
| 🕒 CLOCK IN | ⌖ accountable-next-action-choice (emit the account) + EX-1 entry | the entry record: task, pattern/version, intended state transition, witness conditions, refusal conditions — anchored to the mission clock | the mission clock itself (clock-dispatch! lineage); PSR pattern card ("Pattern [◈] in backpack"); tool_history receipt |
| 🏁 CLOCK OUT | ⚖ progress-is-a-witnessed-state-change (departure ≠ progress) | the departure record: actual outcome vs intended, evidence, witness verdict, pattern clause used, unresolved work — the small-record after-use fields | the row query; witness lanes; PAR |

## The IAD reading (compact)

- BOUNDARY-IN: an operator turn containing an ACTUAL-REQUEST cue
  (discussion, examples, negation do not count — the cue spec's own
  interpretation rule, which is the two-wire discipline at the voice
  layer) puts the receiving agent into the task-holder position.
- CHOICE: in the position, the agent MUST emit the corresponding
  record or a typed refusal (missing task info → clarification = ⌁;
  blocked → blocker named). Silence is the violation.
- BOUNDARY-OUT: 🏁 exits the position via the departure record. The
  exit is NOT credit: credit only on the witnessed state change the
  ENTRY record named in advance (the cue specs already say "no
  completion credit" / "does not assert success" — this RECORDS the
  pending acceptance-vs-execution ruling, does not settle it).
- PAYOFF: the cue costs the operator one emoji; the record costs the
  agent one structured emission; the CHECK costs a scan. Ostrom's
  ratio, favorable by construction.
- MONITORING (the regulation Joe asked for): the cue-receipt lint —
  scan operator turns for actual-request cues; require each to have a
  matching receipt record in the account within the session; a cue
  without a receipt is the violation signature (the agent went quiet
  on an obligation). Policing cost: a regex and a lookup.

## What exists vs what is proposed

EXISTS: the cue vocabulary and its interpretation rules (voxterm v1);
the mission clock and durable lineage; PSR/PUR backpack records (MUSN
stream); PAR; the row query; witness lanes; the small-record schema
(LAYER4-PROCESS); the sigil registry.

PROPOSED (small, one experiment): (a) the entry/departure record
shapes above as a tiny EDN convention keyed to the clock receipt;
(b) the cue-receipt lint as a watcher over the coordination stream;
(c) EX-1's success criterion extended: at least one 🕒→entry and one
🏁→departure round-trip with the lint verifying no silent gap.

## What this buys

Operator voice becomes a boundary condition on agent behavior, not a
suggestion: the cue fires an obligation whose discharge is a typed
record, whose absence is detectable by a cheap check, and whose
content feeds Layer 4 (departure records are the revision process's
input queue). The loop closes the same way inbox-zero's did —
homeostasis, but the regulated variable is cue-response discipline
rather than repo cleanliness.

Row links: EX-1 (codex-25 thread); DRAFT-apex-institutions (the
pending rulings this records-not-settles); LAYER4-PROCESS (the
small record); sigil-registry (⌁, ⚖, ⌖); NOTE-inbox-zero-aif (the
shape being transposed).

## Agent-issued cues: the zaif controller's own vocabulary (Joe, 2026-09-12)

Cues are not operator-only. An agent's self-talk can issue 🎒 PATTERN
CARD (and by the same rule, 🕒/🏁 against itself) — and this is where
the zaif harness cashes out: the controller between turns reads BOTH
streams (operator turns and its own self-talk), and arm selection
BECOMES cue issuance — ask = cue the operator; act = cue itself;
retrieve = 🎒; yield = 🏁 without an entry. One vocabulary, two
emitters.

The constitutional asymmetry (emitter-typed semantics, same glyph):

- OPERATOR cue: creates an obligation on the receiver. Boundary-in
  with full authority; the receipt is mandatory; silence is the
  violation.
- AGENT cue: a SELF-COMMITMENT at proposal precision. It binds the
  emitter (the agent must discharge it or refuse typedly — the same
  lint applies), but it confers no authority the agent lacked: an
  agent cannot clock itself onto a mission (attribution is received),
  cannot mint its own credit, cannot ratify its own pattern adoption.
  The record carries :emitter :agent and the precision row says
  proposal.

The 🎒 spec's own clause is the keystone: CARRIAGE ALONE IS NOT
EVIDENCE OF USE. The PSR record shows the card entered the backpack;
USE must cite the clause invoked at the moment of application —
exactly EX-1's observed-use requirement. So the lint has two checks
now: cue→receipt (no silent gap) and card→use-citation (no decorative
backpacks). Both are regex-plus-lookup cheap.

What the zaif loop looks like in cues, end to end: operator 🕒 (or the
agent's own, at proposal precision) → 🎒 fetch, card in backpack →
work under the card, clause citations in the trace → 🏁 departure
record with the clause-used field → Layer 4 revision fed. The four
arms, the pattern language, and the cue protocol become ONE system:
the harness is the thing that reads cues and emits records.

Row links: EX-1 (this is its protocol layer); zaif four arms
(retrieve/act/ask/yield ↔ 🎒/🕒-act/ask-cue/🏁); M-zaif-harness
boundary 1 (the asymmetry above is it, restated at cue grain).
