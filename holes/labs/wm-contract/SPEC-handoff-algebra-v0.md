# SPEC: handoff algebra v0 (draft for redline)

Drafted by zai-7 (crew design holder) from claude-15's requirements
reply (2026-09-12, invoke-1789225259523) and the design corpus it
summarizes. To be redlined by claude-15 against
`futon2/holes/NOTE-runtime-validation-invariants.md` (I1–I10; I11
pending Joe + zai-5) — the inverse of the zai-5/claude-15 process.

## 1. The unit: a channel is a four-tuple, not a message

channel = (payload type, acceptance authority, reconciliation
procedure, budget account). All four fields mandatory.

The root finding from the audits: every major park class answered "did
the obligation transfer, and who says so?" incidentally — an HTTP
status (f177/f194), the enclosing role (f227), a sibling worktree
(codex20). A handoff whose channel cannot name its acceptance authority
and reconciliation procedure is an informal seam, and informal seams
fail silently. First formalization step: a LINT over the crew design's
thirteen prose channels — each either names its four fields or is
admitted to be informal (a typed gap, not a surprise later).

## 2. Four judgments, checkable at handoff time

(a) **Precondition**: the target's state admits the handoff.
    "Accepted" is not a state but a RECEIPT (actor-idle burn; the
    Agency accepted-before-running ambiguity — one defect at two
    levels).
(b) **Provenance**: chains append, never rewrite; identity includes an
    attempt ordinal (re-dispatch always possible; f209 job-id collision
    is the design counterexample); chains verifiable AT REST, not only
    at install (I10).
(c) **Level**: the collapse law (an L-N actor firing an L-(N-2) effect
    is a type error) becomes a typing rule over level annotations on
    channels; the theorem obligation is that composition preserves it —
    the one genuinely theorem-shaped item.
(d) **Deontic**: institution predicates evaluated at handoff time;
    REFUSAL is a first-class cheap outcome, never an exception —
    otherwise the institution layer recreates I1 (a handled throw that
    erases its own typed refusal; the C446 lesson at the algebra level).

## 3. Composition laws

- **Nesting law (proposed I11)**: every delegated obligation carries
  its own acceptance authority and reconciliation; enclosing layers
  consume only the child's TERMINAL DISPOSITION (closed enum), never
  its internal findings. Verdicts compose as trees with per-level
  vocabularies; they do not flatten. f227 (depth-two nesting blinded
  the rescue classifier) is the exhibit. NOTE: this is the SAME law as
  the chip-boards recursion ruling (opacity at the parent, finiteness
  at every level, return-with-value) — one law, two vocabularies. The
  algebra and the board runtime must not grow two versions of it.
- **Budget law**: work attempts, wrapper/publication failures, and
  environmental unavailability charge three DISTINCT accounts;
  mischarging is unrepresentable (I4/I7; the ea1 burn).
- **Reordering law**: acceptance is durably monotone — no permutation
  of subsequent events converts accepted work to fault (I2).

## 4. Validation obligations outrank the formalism

- Every handoff-time judgment gets INDUCED-VIOLATION commissioning
  before it is trusted (a check that cannot fail is not a check — the
  witness loop's five-round record is V1 working).
- Conformance = REPLAY of recorded production orderings through the
  actual channel code; the incident archive is the suite (APM packets
  5–8 are building that half).
- A Lean model of the algebra is evidence only when statement-hash /
  enumeration-pinned to the running registry (else a second, silently
  stale truth — the witness-status precedent applies to the algebra
  too).
- Every channel's ARTIFACT must be demanded non-nil by its gate before
  its structure is formalized (the twelve-consecutive-gate-passes-over-
  `:wiring nil` lesson).

## 5. Sequencing (one hop first)

1. Generalize the chip-board certificate atom (board digest + inputs
   digest + verbs digest, replay-verified) to ONE inter-agent hop: the
   bell/packet channel, with acceptance authority and reconciliation
   named.
2. Prove the nesting law against the f227 replay pins (if Joe ratifies
   I11, it arrives pre-agreed).
3. Admit channels one at a time, each entering with a production
   replay pin of its own historical failure where one exists
   (actor-idle has one); channels without an incident (ranked-actions,
   constellation artifacts) enter with induced violations instead.
4. Coverage accounting on the existing assurance frontier (V5), not a
   new ledger.

Status: DRAFT — redline requested from claude-15; I11 pending Joe and
zai-5; nothing here gates live behavior yet.
