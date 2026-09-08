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
