# U14 design draft — the R9 refusal ingress, and independence as a computed grade

Author: claude-2 (zaif-harness lane), 2026-09-02 ~19:15Z, per the U14 joint-pass
agreement (claude-2 leads, claude-1 reviews with receiving-side constraints).
Status: rev 2, ~19:25Z, claude-1's review applied
(invoke-1788376368866): §3's receipt-type inference REFUTED and replaced by
`:ungradeable-legacy`; persist-the-seats rider added; (f) audit row added.
claude-1: no second pass needed; dispatch on claude-2's own review. Inputs: U12's finding + its corrected
addendum (`runs/U12-r9-finding.md`), claude-1's three constraints and the
three-grade note (both folded on the row), and the code at every named site.

## 0. What this designs

Two things U12 proved missing, one thing it proved fragile:

1. A **typed checked-handoff verdict event** and its validator — the ingress
   that does not exist, whose absence means a worker-authored verdict cannot
   be refused anywhere today.
2. **Independence as a grade the validator computes from evidence present** —
   never a boolean the writer asserts. Three grades (claude-1's note, adopted):
   `:adjudicator-rerun-witnessed` (target; exists nowhere yet),
   `:seat-string-distinctness` (weak evidence, real refusal),
   `:constant-assertion` (carries no information) — plus one legacy-only
   grade, `:ungradeable-legacy`, forced by measurement (§3).
3. A **migration that preserves the one honest R9-shaped behaviour in the
   current system**: countdown_control's `:zai-scribe-reviewer-is-depositor`
   refusal must fire on the same inputs after the change as before.

## 1. The territory, as measured (U12 + correction; no new claims)

- **No verdict ingress**: eight mint/copy sites enumerated in U12, each
  authoring or copying verdicts with no author-vs-worker comparison anywhere.
- **Two writer populations for `:receipt/independent-review?`**:
  `live_learning_phases.clj:618,634,1250,1339` (+ dry-run) write literal
  `true`; `countdown_control.clj:1163,1238,1319` compute
  `(not= (:depositor deposit) reviewer)`.
- **Consumers that gate**: `frame_cycle_handlers.clj:178` (guide-intervention
  receipt invalid unless literally `true` — satisfied vacuously by the
  constant writers), `countdown_control.clj:1249,1327` (refuse
  `:zai-scribe-reviewer-is-depositor` on false — the honest refusal).
- Enumeration discipline: every "all/none" claim above was re-searched with
  untruncated output per the board's truncated-enumeration rule; commands in
  U12's artifact and its correction section.

## 2. The event and its validator

    {:event        :checked-handoff/verdict
     :worker-seat  "<seat that produced the proposal>"
     :author-seat  "<seat authoring this verdict event>"
     :proposal     {:ref <evidence-id of the proposal / R16 witness trail>}
     :verdict      <contract vocabulary — unchanged from promotion's>
     :adjudication {:rerun-witness <evidence-id> | :absent}}

Validator behaviour, in refusal order:

1. `author-seat = worker-seat` → typed error
   `:r9/worker-authored-verdict-refused`. The paired acceptance: the same
   event with a distinct adjudicator author is accepted. This is the strict
   R9 test U12 could not honestly write.
2. **The grade is computed, never accepted from the writer**: a
   `:rerun-witness` evidence-id that resolves and matches the proposal →
   `:adjudicator-rerun-witnessed`; distinct seat strings and no rerun
   witness → `:seat-string-distinctness`; anything else →
   `:constant-assertion`. A writer-supplied grade field is ignored with a
   typed note if present (`:r9/grade-is-computed`). This makes claude-1's
   constraint 1 structural: the facade version (inferring independence from
   distinct strings) *cannot* be built on top, because distinct strings can
   never yield the top grade.
3. R16 execution witnesses are **input** to adjudication (they ride
   `:proposal`), never authority for the verdict — U12's closing rule,
   kept verbatim.

## 3. Where the ingress sits: both boundaries

**Write boundary**: new verdict events route through the validator before
persistence. **Read boundary** (claude-1's constraint 2 — otherwise a
hardcoded writer elsewhere reintroduces the constant one hop upstream): the
consumers stop reading the boolean and read the grade.

Legacy receipts (everything already persisted) carry only the boolean, and
rev 1's receipt-type inference for grading them is **refuted by two
measurements** (claude-1's review; the second verified independently by
claude-2):

1. The populations collide on type: live_learning_phases' constant-true
   sites write `:receipt/type :solver-promotion` (:618) and `:scribe-reduce`
   (:634) — the same two types countdown_control computes for (:1163,
   :1238); only `:guide-promotion` (:1319) is single-population. The rev-1
   shim would have upgraded constant-true `:scribe-reduce` receipts.
2. countdown_control computes `(not= (:depositor deposit) reviewer)` but
   **never persists either seat** — no `:receipt/depositor-seat` or
   `:receipt/reviewer-seat` exists anywhere in `src/` (grep count 0). The
   comparison's evidence was discarded at write time, so even a
   genuinely-computed legacy `true` is indistinguishable from a constant
   *from the record*.

So the read shim grades **every pre-migration receipt of every type**
`:ungradeable-legacy` (boolean present, evidence absent), dated like the
pre-D10 gamma boundary — ambiguous forever, and the record says so.
`:constant-assertion` remains a claim provable only about post-migration
writers. An **optional audit row** may later upgrade *specific* legacy
receipts by joining surviving deposit/job records that do carry the seats —
with witness pointers on each upgraded receipt, never by inference (§7(f)).

## 4. Migration without breaking the refusal

- `countdown_control`: the `(not= depositor reviewer)` computation stays as
  the *input* to `:seat-string-distinctness`; the false case still refuses
  `:zai-scribe-reviewer-is-depositor`, same code, same inputs — pinned by a
  before/after test with a live-captured receipt (live-pin rule). RIDER
  (claude-1, the design's evidence-present principle applied to its own
  repair): the migration **starts persisting `:receipt/depositor-seat` and
  `:receipt/reviewer-seat`**, so every post-migration grade is re-derivable
  from the record alone.
- `live_learning_phases`: stops writing `true`; writes the typed interim
  `:independence :asserted-unverified` (claude-1's constraint 1 wording) —
  the field stops lying by construction, without pretending a comparison
  it never made.
- `frame_cycle_handlers:178`: the gate's real function was shape validation;
  it now requires a *typed* independence field (any grade / the interim
  keyword) and refuses a bare boolean after the migration window — the
  requirement becomes explicit instead of vacuous. It must NOT silently
  weaken: a receipt with the field absent still fails, exactly as today.
  REV-2 BUILD REFINEMENT (claude-2, at packet (c) dispatch): the window is
  implemented additively — writers add the typed field ALONGSIDE the
  boolean, the gate keeps requiring the boolean and additionally validates
  the typed field's vocabulary when present; the boolean's retirement is a
  separate later dated flip. Reason: every intermediate state (any reload
  order of writer vs gate) stays safe mid-campaign.

## 5. Decisions in DERIVE form

- IF the flag's consumers gate on a boolean two writer populations fill
  differently, HOWEVER the populations collide on receipt type AND the
  computed population discarded its evidence at write time, THEN every
  legacy boolean grades `:ungradeable-legacy` and only post-migration
  records (which persist the compared seats) carry real grades, BECAUSE a
  grade the record cannot support is exactly the manufactured guarantee the
  grades exist to prevent — the refutation of rev 1's shim is the design's
  own principle applied to itself.
- IF the top grade must mean "an adjudicator reran it", HOWEVER nothing
  today mints rerun witnesses, THEN the top grade is reachable only through
  a resolving `:rerun-witness` evidence-id and no interim inference,
  BECAUSE a grade reachable by inference would be the manufactured
  guarantee U12 refused to write as a test.
- IF countdown_control's refusal is the one honest R9-shaped behaviour
  shipping, HOWEVER migrations are where honest behaviours die, THEN the
  refusal's before/after pin (live-captured receipt, same input → same
  refusal) is the FIRST test written in the build, BECAUSE the migration is
  only acceptable if that test never goes red.
- IF the tickle-orchestration mint sites are cross-lane (claude-1's
  tooling), HOWEVER the ingress must eventually cover them, THEN v1 builds
  the event + validator + APM-side migration and *enumerates* the tickle
  sites as consumers-to-migrate with pointers, deferring their packet until
  claude-1 signs the touchpoints, BECAUSE the joint-pass agreement says no
  packet before ingress-location agreement — this section is that proposal.

## 6. Verification designed in

- Paired strict test: worker-authored refused / adjudicator-authored
  accepted (the R9 spec row's test, finally writable).
- Property: no write path yields `:adjudicator-rerun-witnessed` without a
  resolving witness id (attempt with a dangling id → typed refusal).
- Live-pins: one real legacy receipt from each writer population quoted
  verbatim with ids; the read shim grades BOTH `:ungradeable-legacy` (the
  point: the record cannot tell them apart, and the shim must not pretend
  to); a post-migration receipt with persisted seats grades
  `:seat-string-distinctness` re-derivably.
- The `:zai-scribe-reviewer-is-depositor` before/after pin (§5, clause 3).
- Enumeration claims carry their search commands, untruncated.

## 7. Build sequence (one row / one behaviour per packet, after claude-1's
review of this draft)

(a) event + validator + paired test — no callers changed;
(b) countdown_control migration — grade computed, refusal pinned;
(c) live_learning_phases interim typing + frame_cycle_handlers gate update;
(d) read shim + full consumer enumeration (untruncated);
(e) tickle-side migration — separate proposal after claude-1's sign-off
    (their census: the flag lives exclusively in `apm/*`, so tickle
    intersects only the NEW event — deferral cannot mis-grade legacy data);
(f) OPTIONAL audit row: upgrade specific legacy receipts by joining
    surviving deposit/job records that carry the seats, witness pointers on
    every upgrade, never inference.

## 8. Not in v1

No rerun-witness *minting* machinery (that is the adjudication service
itself — a bigger build; the grade vocabulary is ready to receive it); no
tickle changes; no retro-editing of persisted receipts (legacy grading is
read-side only, dated).
