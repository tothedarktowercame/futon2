# TN: Row 18 discovery — R20's chartered commitment link

Date: 2026-09-12  
Scope: `WORK-REMAINING.md` row 18 discovery only. No production, registry,
census, or configuration change is made here.

Source heads read: futon2 `1bb20921dac43447c739c2258eda095a90d451ab`,
p4ng `fec6e538d76efbe777428f0e6cd7b91e13bbdb57`, and futon3c
`3682c693a7e379dc8d86cf90716d34da5c478544`.

## 1. The charter

The stage roster itself makes the minimum declaration only: `R20`, stage
`EVALUATE`, assurance band, label `Interoceptive tripwires`; it supplies no
basis or formula (`p4ng/empirics-futon/control-stages.edn:30-35`, specifically
`:32`). R20 is deliberately plumbing, with no equation or Lean declaration
(`holes/labs/wm-contract/aif-equations.edn:1049-1050` at this HEAD). The
substantive charter is therefore the catalogue, not the equation registry.

The catalogue's exact obligations are all in
`p4ng/sec-catalog.tex:360-373`:

1. **Phase-boundary sensorium.** Weave invariant wires around the live run at
   its phase boundaries. The named families are turn conservation, ledger
   closure, review/commit provenance, append-only repair stores and their
   lattice, wedge/livelock, wall-clock, loaded/file-code coherence, and the
   environment alphabet (`:369`).
2. **Epistemic action.** On a trip, “freeze, record, park, summon”: durably
   retain the warm state, open a typed stop-line, park an investigation join,
   and summon the outer reviewer. Its fail-safe ladder may degrade only toward
   recording, never toward blocking the run (`:369`).
3. **Calibration and declared incompleteness.** Wires must retro-trip the known
   incident corpus; later incidents must add either a detecting wire or an
   explicit blind-spot entry. Completeness is not claimed: identifiability,
   measured coverage, and a blind-spot map are the substitute (`:369`).
4. **Commitment link.** A genuine trip should lower confidence in the
   machine's own machinery until discharge (`:369`), and the evidence paragraph
   says expressly that the link from a trip to “the commitment dial of R14” is
   chartered and not implemented (`:373`). The adjacent figure instruction
   names the dashed edge `R20 -> R14` and calls it interoceptive gamma
   (`:376-383`). The machine-readable edge repeats exactly
   `{:from :R20 :to :R14 ... :status :chartered}`
   (`p4ng/empirics-futon/control-map-edges.edn:100-110`). R14's dial is the
   selection gain/inverse-temperature controlling decisiveness
   (`p4ng/sec-catalog.tex:241`). Thus the link binds **an unresolved, genuine
   R20 trip** to **R14's applied policy-selection gain**, with discharge ending
   the modulation.

“Chartered commitment link” does have three nearby readings, which must not be
silently conflated:

- R20→R7: the catalogue calls trips precision evidence (`sec-catalog.tex:369`)
  and the figure draws that edge (`:378-379`). This is a precision-learning
  interpretation, but row 18 says *commitment* and every explicit unbuilt-edge
  source names R14, not R7.
- The per-wire action-ladder precision described in
  `M-wm-tripwires.md:161-166` adjusts whether a wire records, stops, or summons.
  It is precision **of the sensor**, not R14 policy commitment.
- The S4 candidate is the intended referent: `M-wm-tripwires.md:137-142` says a
  trip modulates confidence, directly connects it to learned gamma, and
  `holes/TN-War-Machine-Restart.md:116-141` identifies gamma with R14's
  selection gain `g`. The latter's numerical `base^k`, threshold, and grounding
  gate are explicitly a **design sketch**, not ratified constants
  (`TN-War-Machine-Restart.md:126-145`). Row 18 therefore licenses the edge and
  its direction, but not a silently chosen decay base, floor, threshold, or
  operator-gating policy.

## 2. What exists today

The R20 account in `VERIFY-r-nodes.edn:1430-1497` is a valuable dated breach
report, not a current source census. It measured one unattributed metabolic
Andon wire and zero durable R20 fields in 889 old trace records
(`:1443-1457`), then concluded that the weave, action, calibration, and node
link were absent (`:1477-1492`). Current HEAD has since built most of that
missing surface:

| Declared obligation | Current evidence | Honest status |
|---|---|---|
| phase-boundary wires | `full_loop_runner.clj:222-238` calls `tripwire/observe!` at every emitted phase; `tripwire.clj:78-95` registers T1–T13 (T12 remains a disabled chartered stub) | built and hooked; not universal completeness |
| freeze/record/park/summon | `M-wm-tripwires.md:12-31` defines the action; `tripwire.clj:600-640` implements degradation and durable reporting | built action ladder; deployment/mode remains configuration-sensitive |
| retro-trip calibration | `tripwire_calibration.clj:61-125` reconstructs five incidents and refuses mismatches; `data/wm-tripwires/coverage-v2.edn:1-40` retains 5/5 | built for the five-item corpus, not proof over unknown failures |
| blind-spot map | `data/wm-tripwires/blind-spots.edn:1-11` records the incident-to-neuron rule and semantic-wrongness blind spot | built, one explicit blind-spot class |
| refusing check and attended surface | `tripwire/check!` returns passed/refused and refuses through persistence (`tripwire.clj:642-666`); the census credits checked `[E20-C]` (`ALIGN-rnode-process-census.md:294-305`) and surfaced `[E20-S]` via the Joe bulletin (`:134-145`) | exists at the node-linked boundary |
| Agency execution and continuation | Row 17 retained narrow work-execution and continuation/deadline evidence; its maximum claims expressly exclude catalogue-wide R20 (`runs/row-17-r20-tripwires-2026-09-12/working-evidence.edn:1`; `runs/row-17-r20-continuation-2026-09-12/working-evidence.edn:1`) | credited only at those narrow lifecycle grains |
| R20→R14 commitment | the edge is still `:chartered`; R14 currently derives task-level gain at `war_machine.clj:6156-6177` and hands that value to selection at `:6540,6571-6576`; no trip state enters that derivation | **NO IMPLEMENTATION** |

The present missing-piece list is consequently narrow:

- no authoritative snapshot joins durable trip reports/findings to their
  discharge state and produces an acute machine-confidence factor;
- no typed rule distinguishes genuine unresolved armed trips from shadow
  `:record` trips, test-root trips, malformed records, repeated observations of
  one trip, or already discharged findings;
- no composition point combines that factor with the task-evidence R14 gain;
- no decision/trace record carries base task gain, machine-confidence factor,
  effective gain, input trip identities, and revision together;
- no retained production witness shows a genuine trip lowering effective gain
  and its discharge restoring it.

These are not row-17 accounting gaps. The two row-17 records credit machinery
that already exists; they neither create nor test a causal R20→R14 edge.

## 3. What row 18 must build

### 3.1 Authoritative interoceptive-confidence snapshot (futon2)

Add one source module, naturally `futon2.aif.interoceptive-commitment`, whose
input is pinned trip reports plus the repair-obligation/discharge store. It
must join by durable trip/finding identity and emit a versioned record such as
`{:schema :interoceptive-commitment/v1 :open-trip-ids [...] :base-task-gain g
:machine-confidence m :effective-gain (* g m) :authority ... :revision ...}`.
It must refuse missing/malformed identities, contradictory discharge states,
unknown action modes, nonfinite factors, and unpinned authority revisions. It
must exclude `:record`-only shadow observations and test roots explicitly;
absence of readable authority is typed unavailable, never “zero trips”.

The numerical response is **blocked on a small operator/spec ruling**. The
charter fixes monotonic direction and restoration on discharge, but not the
function. `base^k`, a floor, and distinct-wire counting occur only as examples
in `TN-War-Machine-Restart.md:126-145`. Acceptance must pin a declared v1 law
before coding rather than smuggle in `0.5` as a default.

Negative controls: an unjoined report, a report whose finding is absent, a
discharged finding presented as open, a duplicate trip id, a shadow/test trip,
a stale revision, and a nonfinite/out-of-range configured factor. Positive
controls: zero open genuine trips gives factor exactly 1; one and two distinct
open genuine trips change the factor monotonically under the ruled law;
discharge restores it.

### 3.2 Compose at the R14 selection seam (futon2 WM source)

At the single point after task-level `selection-gain-state` is read/folded
(`war_machine.clj:6156-6177`) and before it is passed into
`policy/select-action` (`:6540,6571-6576`), multiply the declared
machine-confidence factor into the task gain. Do not mutate
`selection_gain.clj`'s task-evidence posterior: the two evidence sources remain
separate and auditable. Selection must refuse a required but unavailable
interoceptive snapshot; it must not silently substitute 1. The decision and
TRACE projection must retain all three quantities, input trip ids, authority
revision, and a typed exclusion list.

Negative controls: bypassing the composer, substituting 1 on unreadable state,
feeding a discharged trip, double-counting the same wire/trip, and a record
whose effective gain does not equal the declared composition. Positive control:
identical ranked inputs at factor 1 reproduce the pre-link decision; an open
trip lowers effective gain and changes the recorded temperature/weights in the
declared direction without rewriting G scores.

### 3.3 Production-match and discharge witness (futon2 retained run evidence)

Retain a redirected first, then genuinely live, pair at identical configuration:
pre-trip or discharged state versus one unresolved armed trip. Capture the
actual trip report, joined repair/discharge record, composed selection inputs,
decision, and trace readback. The witness must demonstrate lower applied gain
while open and exact restoration after discharge; malformed/unjoined and
shadow/test controls must refuse. No synthetic trip can discharge the positive
production claim.

The grounding-confirmation gate in `TN-War-Machine-Restart.md:132-135` is a
separate proposed behavior. It is not stated in the catalogue's R20→R14
commitment sentence and needs an explicit owner/ruling before implementation.
Likewise, expanding incident coverage or adding wires belongs to the R20
tripwire programme, not to this edge packet.

Row 19 owns author=reviewer refusal. Row 17 owns only retrospective crediting of
Agency lifecycle evidence. Neither may be used as a substitute for the causal
link above.

## 4. One-behaviour packet split

1. **Law ruling and breach fixture.** Record the v1 monotone factor law,
   identity/discharge authority, exclusions, floor, and revision semantics.
   Acceptance: a table of positive states and every typed refusal above,
   including the factor-1 compatibility case. No production edit.
2. **Snapshot constructor.** Add the source module that reads pinned real trip
   and discharge records and emits/refuses the versioned confidence snapshot.
   Acceptance: actual-reader retained readback plus commissioned identity,
   discharge, shadow/test, duplicate, malformed, and revision controls.
3. **R14 composition and recording.** Integrate exactly once at the WM
   selection-gain handoff; preserve task gain separately and add the effective
   gain/provenance to decision and trace. Acceptance: factor-1 byte-equivalent
   choice control, open-trip direction control, bypass/default/mismatch
   refusals, policy/trace/WM gates.
4. **Discharge round trip.** In redirected machinery, create a real typed trip
   through the actual R20 boundary, observe lowered effective gain, discharge
   through the actual repair path, and observe restoration. Acceptance: pinned
   trip/finding/decision/trace records and first-supported-occurrence checks.
5. **Live production witness and narrow credit.** After reviewer reload, retain
   the first live armed trip/discharge pair. Acceptance: production readback at
   identical pins, measured deltas, and a working-evidence claim scoped only to
   R20→R14. Calibration expansion, new wire families, grounding confirmation,
   row-17 credit changes, and row-19 self-certification remain outside it.

Dependency is strict: 1 → 2 → 3 → 4 → 5. In particular, packet 3 must not pick
a decay law because packet 1 has not supplied one, and packet 5 must not claim
production behavior from packet 4's redirected run.
