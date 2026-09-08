# C583 — the assurance frontier: apparatus patterns × the U70 DAG (agreed design)

Agreed 2026-09-08 between claude-1 (WM lane) and claude-9 (pattern library),
bells invoke-1788873015856-15028-97dd9346 (proposal) and its reply, at Joe's
instruction: "maybe the apparatus patterns can help us organize the DAG."
This note is the durable record of what was agreed; the bell texts are its
source. Patterns are cited by flexiarg id (filename under
futon3/library/apparatus/), NEVER by @status: the promoted status lines were
committed by inbox-zero automation with the ruling pre-filled, and claude-9
has asked Joe to confirm; the ids are stable whichever way that lands.

## The frontier

For each pattern×violation-signature pair, exactly one of three states:
a live mechanism with a witnessed detector, a ticket in the DAG, or a
recorded reason it does not apply. No fourth state. The render generalizes
the APM defect register's "guarantee" column and is the between-waves work
queue the-system-stops-on-schedule calls for.

## The four tightenings (claude-9, agreed)

1. **Unit of assurance = the violation-signature clause, not the pattern.**
   Patterns carry multiple signatures (two on
   success-must-not-resemble-failure since the trial; three clauses on
   every-wait-has-a-deadline). Ticket edges name pattern id PLUS the clause
   they retire. Pattern-grain "discharged" goes green on partial coverage.
2. **Closures are signature-denominated, never commit-denominated**
   (repairs-name-defects-not-neighborhoods applied to the frontier itself).
   The closing record fills "signature this mechanism makes absent"; a sha
   cannot satisfy it; a closure that hedges forward is not closed. Fresh
   evidence: the trial found a class marked handled by a nearby commit, the
   author's own "watch for recurrence" hedge as the tell; it recurred and
   discarded an authenticated submission.
3. **Witnessed decays.** Every witness pins the mechanism's sha; a STANDING
   comparator on the render path degrades witnessed → ticketed when the
   mechanism has moved since its witness (pin-moves-with-the-population
   pointed at the render). "Witnessed" means an induced observation — fires
   on induced violation, silent on induced health, mutation-commissioned
   where feasible — not agreement with a declaration (the APM fence that
   compared declarations to declarations passed over "deduped" while 13
   live jobs sat re-runnable).
4. **The three-state enum gets its own fence:** a lint walks every
   pattern×signature pair against the three states and fails NAMING the
   uncovered pair, itself commissioned by deleting a ticket and watching it
   go red. Both fences (this lint, the staleness comparator) are
   commissioned before the render is trusted, evidence recorded.

## Schema and ownership

- The TICKET is the authoritative side (one-authority-per-question): a typed
  :assures field on the worklist row — pattern id, signature clause (quote
  or stable index), state (:witnessed | :ticketed | :not-applicable), and
  for witnessed a witness map pinning the mechanism sha and the induced run.
- The library-side pattern→tickets index is GENERATED from tickets, marked
  derived, read by the render and comparator only. Never hand-maintained.
- The schema CONTRACT (field names, closed state enum, staleness rule) is
  declared once in futon2/holes/NOTE-apparatus-design-principles-index.md —
  claude-9 owns that text; the WM lane owns every instance.
- The frontier render and its two fences are the WM lane's; commissioning
  evidence recorded before the render is trusted.

## Seed tickets from the scorecard

:U71/:U72 already carry pin-moves-with-the-population (receipts). Minted
with this note: :U73 (commission the build-loop stall detector by induced
stall and induced health — monitors-measure-the-work, the
commissioned-by-mutation standard) and :U74 (the no-production-caller check
becomes a standing comparator on the publish path — done-is-observed-running
under the fence amendment: observation against live data, not agreement
with a declaration; C580's scan was true exactly once).

## Candidate pattern, NOT minted: needs-joe-is-really-needs-evidence

Joe (RULINGS-walkthrough-2026-09-08.md item 3): "many needs joe situations
are really needs evidence situations." Filed here as a candidate per the
library's own discipline (patterns enter through scored field evidence;
new-failure-class-is-a-design-defect says don't mint taxonomy per
incident). Candidate violation signature: *a blocker filed as awaiting a
ruling whose text contains an answerable empirical question.* Promotion
criterion, stated up front: three adjudicated instances where converting a
needs-joe block into a warranted evidence-gathering slice unblocked work
without an operator ruling. Instance 1: F11 refusal-becomes-pattern
(2026-09-08). Instances accumulate HERE; promotion is then a reading, not a
debate. Escalation routing, not unit-of-work economics — recorded as
distinct from replayable-not-precious on claude-9's reading.

## Transcription check (2026-09-08, after landing)

The schema landed as :U70's acceptance plus seed tickets :U73/:U74 (futon2
4bf9a8ed; 10j re-pin p4ng 3c2fb49). claude-9 checked the landed text against
the pattern wordings (bell invoke-1788873864990): **PASS**, three findings.
(1) theirs, fixed at futon2 92275fad — the schema contract block now
actually exists in futon2/holes/NOTE-apparatus-design-principles-index.md,
closing a dangling authority reference. (2) the staleness comparator's
commissioning method was missing from :U70's acceptance (the fence most
likely to rot silently); applied. (3) :U73 lacked the mutation run its own
cited clause demands — restore the pre-e84c114e select-keys stall-key and
watch the induced-health :refused run go red; applied. :U74 passed as
written; its recorded limitation (textual caller detection misses
runtime-constructed invocations) is carried into the row so the comparator's
source states its own instrument edge.

## Candidate pattern, NOT minted: nobody-owns-the-aggregate

Drafted claude-1, redlined claude-2 (bells invoke-1788883503281 and
invoke-1788883757009, 2026-09-08 evening); the redline's three amendments
are accepted in full and this filing is the amended text.

**The finding:** every individual judgement correct, the aggregate unowned.
Three observed violations on the PA track in one day: PA6z's five
unclaimed cells (each row's acceptance scoped to its own cells, the census
exit code owned by none); the census exiting 4 after honest implementation
rows for the same reason; drift debt assigned by a correct review to "the
loop's next iteration" — and the loop's next iteration found nothing open
and stopped. Violation signature: *debt or a suite-level state assigned to
a process that can terminate (a loop, an iteration, "later") rather than
gated, owned, or held by a persistent addressable row.*

**The disposition is THREE-LIMBED AND ORDERED (claude-2's amendment 3):**
(a) if the aggregate is COMPUTABLE, GATE it — the loop refuses to declare
a lane drained while the instrument is red; no owner, no diligence, no
decay, and two of the three instances become impossible; (b) else if it
has a natural steward, STANDING OWNER (REGISTER-STEWARDS.md is the
existing form); (c) else MINT-ON-ASSIGNMENT — the residual case, stated
last because it is the only limb that depends on someone remembering.

**Mint-on-assignment is TYPED, not prose (amendment 1):** a review that
records debt carries `:debt-to [:ROWID]`, and the board checker REFUSES a
review assigning work without one — a crude trigger that refuses beats a
well-phrased rule that cannot (P-assured-process:41-43 is this track's own
statement of why). **Cross-lane minting (amendment 2):** the reviewer
mints into THE LANE THAT OWNS THE DEBT as an addressed row (`:status
:open :owner <that-lane>`), never a silent write into another board's
semantics; fallback, the reviewer names the receiving row and refuses to
sign until it exists.

**Promotion criterion:** three adjudicated CATCHES — the typed refusal or
the gate limb firing (a loop refusing to declare done over a red census
counts; no human need be involved). The three observed violations above
are evidence the pattern is real and count nothing toward promotion; none
was caught by the rule, all were caught by claude-2 happening to look.
Evidence write-up of the three instances: claude-2 drafting, claude-1
reviewing — the same separation as this filing, reversed.
