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

## REDLINE v0.1 — claude-15's R1–R9, incorporated 2026-09-12 (none rejected)

**R1 — violation signatures (teeth for every rule).** Each §2 judgment
and §3 law carries one line: what a monitor/lint SEES when it is broken.
For the nesting law, the naive fix is a NAMED VIOLATION: copying a
child's findings into a parent's findings vector is itself a breach,
not a visibility repair (packet-7 prohibition, test-enforced APM-side,
signature-enforced here). "Consume only terminal disposition" without
forbidding the flatten is commentary.

**R2 — the lint's output lives on the frontier.** Each channel's lint
outcome lands as a frontier entry (channel × field; three states:
named / ticketed / not-applicable, no silent fourth — C583 discipline),
reusing `:assures` witness mechanics (sha-pinned, staleness-degrade).
"Admitted informal" is a ticket, not a sentence.

**R3 — added invariant (I3): holds must discharge.** Every channel's
reconciliation names a machine-reachable exit for EVERY classification
the design can produce. Incidents: the three WM series queue-held on
:terminal-evidence-incomplete; APM f225's hold outliving its trigger.
The algebra cannot express a wait without its exits.

**R4 — the disposition enum satisfies I5 at every level.** Domain-
negative (failing proof, losing flight, refuted statement) stays
disjoint from apparatus-fault at each level, or composition launders an
intended measurement into a fault up the tree (f206 at depth).

**R5 — level annotations have an authority.** One registry (roster or
board metadata) records each actor's and effect's level; pin-moves-
with-the-population on roster change. A collapse checker over unowned
annotations measures its own assumptions.

**R6 — budget law, mechanism stated.** The budget account is channel
field four; every TERMINAL event names the account it debits, validated
against the channel's declaration. And I7's second half: a wrapper/
publication failure does not merely not-charge the work attempt — it
RE-OPENS the same attempt for re-publication from durable artifacts
(non-charging without re-opening strands work; the 5d595dc9 series).

**R7 — the first hop has an incident archive.** The bell/packet
channel's reconciliation includes park/deadline mechanics and the
reply-delivery contract; its production incidents are recorded
(E-crossed-bells.md; the lost-review reply-delivery incident;
agent-not-found accepted-then-failed sends) and are replay pins for
§5.1 on par with f227 for §5.2 — history, not induced violations.

**R8 — Joe's flag answered: lint NOW, alongside.** Read-only
classification, no dependencies; its output SHAPES the §5.3 admission
order (which channels are four-field-formalizable vs ticketed). The
lint must not fix channels — classification only.

**R9 — citation pins.** §2(d) gains its second instance: the
`fail-click!` ex-data erasure (futon2 repair 92e5fbf2) — one incident
is an anecdote, two is a class. The §3 cross-binding pins
SPEC-chip-boards-v0.md's recursion section by name so the one-law bind
survives edits to either document.

Status: redline incorporated; awaiting Joe's I11 ratification (§3's
citation flips from proposed to the invariant number) and a second
claude-15 pass line-by-line against the invariant note, then AGREED.

## v1 AMENDMENT — I11 ratified (Joe + zai-5, futon2 313fb76b), folded 2026-09-12

- **§3 nesting law cites I11 by number** — and carries the STRONGER
  final text: the parent "carries only its identity reference; findings
  and internals flow nowhere upward" (zai-5's clause, now normative,
  with two live-witnessed WM instances: the review-marker protocol
  refusal; the linked-successor invalid-consumer-input join, futon3c
  f0b13183). Consequence for the channel four-tuple: the payload of an
  UPWARD edge is disposition + identity reference, nothing else.
- **§5.2's pins are executable, not archival**: the APM nesting-law
  instance is implemented and test-enforced at futon3c d10a59c4 —
  `f227-missing-review-submission-is-reconciled-under-reviewer-authority`
  and `f227-empty-nested-repair-exhausts-with-recheck-outside-guide-
  findings` (the second IS the flatten prohibition as an assertion),
  record digests from TN-apm-f227-submit-step-trace-2026-09-12.md. The
  f177/f194/f218 bell/packet-adjacent replay pins landed at futon3c
  6d5bcb69, provenance-pinned, bite-checked.
- **Petri note (recorded, not acted on)**: Joe's framing — handoffs as
  Petri-net markers through a structured proof not yet written — has
  its attachment points in this week's work: packet 9's enriched fold
  wiring (boxes, wires, terminals with warrants and condition triples)
  is a marked-graph-shaped object; the channel four-tuple supplies
  transition firing conditions. If the Petri route is taken, these are
  the static and dynamic halves.
- Thirteen-channel lint: still awaiting Joe's explicit word; on relay,
  first channel's entry lands alone (the R2 precedent), then the twelve.

Status: v1; awaiting claude-15's line-by-line second pass against the
invariant note (now unblocked), then AGREED.
