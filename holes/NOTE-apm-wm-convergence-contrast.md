# NOTE-apm-wm-convergence-contrast — one theory, two instantiations

Commissioned by Joe 2026-09-12: the APM and WM are converging on core
issues — "you can't really have a proof without a cascade" is common to
both — and the V4 APM specification must be considered ALONGSIDE the WM
work: not to unify them, but to contrast them, and to ask why any
differences exist and what each difference implies for both. Drafted by
claude-15 (holding both sides' 2026-09-12 state); APM-side claims to be
redlined by the V4 owner. Status: DRAFT.

Companion documents: `NOTE-cascade-structured-proof-validation.md`
(Rulings 1–5, the WM slice), `NOTE-runtime-validation-invariants.md`
(I1–I11, V1–V5 — already common), `labs/wm-contract/SPEC-handoff-algebra-v0.md`
(AGREED, already common), futon3c
`holes/labs/M-apm-demonstration/analysis/apm-v4-production-2026-09-11/README.md`
and `TN-APM-pattern-first-development-2026-09-10.md` (the V4 side).

## The shared core (already converged, mostly this week)

- **No proof without a cascade.** Both systems: the cascade/plan is the
  outline of the work (Ruling 2), holes are typed interfaces (Ruling 3),
  the deliverables are outline + filling + correspondence and nothing
  else. The V4 offline checker (applicability argument + proof argument
  per node, no broken refs/cycles) and the WM enriched fold contract +
  correspondence validator are the same checker in two dialects — the
  WM's was explicitly built to the V4 checker's bar.
- **Pattern-first library economics.** Prefer known patterns; new
  patterns enter through holes with review; publication is gated;
  next-use is the evidence of value. (V4 gates 3–4; WM retro-promotion
  receipts and the apparatus frontier.)
- **The runtime invariants and the handoff algebra** (I1–I11, V1–V5,
  the channel four-tuple): agreed jointly, instanced from both records.
  I11 was proven in APM code (f227, d10a59c4) and stated independently
  by the WM (chip-boards recursion) — the convergence exhibit.

## The contrasts, and what each implies

### C1. Who creates the cascade: computed vs deliberated

**WM:** the cascade is COMPUTED — selected by score (and per Ruling 5,
properly by G over policies), from the mission's circumstance, before
the fold. **APM V4:** the plan is DELIBERATED — the Student authors it
from retrieved patterns, the TA challenges it (does this δ depend on the
function?), conditions get statuses, revision is durable.

Today's evidence shows each lacks what the other has:

- The WM's computed cascades were two patterns of i-ching with a
  negative score and no applicability argument — selection without
  deliberation produced outlines nothing argued for. The APM probe's
  finding names the disease: **ranking cannot decide applicability.**
  The WM's missing half is the TA move — the condition matrix between
  retrieval and use. (Partly adopted already: condition triples in the
  enriched contract. Not yet adopted: the dialogic revision loop —
  nothing in the WM challenges a cascade before it is folded.)
- The APM's deliberated plans have no computable selection functional —
  no G. Which patterns are candidates, which plan wins, is judgment
  (Student + TA), recorded as prose. "If we can't compute G we can't do
  anything" applies: the APM has no persisted record of WHY this outline
  beat alternatives, so plan selection cannot be audited, replayed, or
  eventually proved the way Ruling 5 demands of the WM. The APM's
  missing half is the WM's scoring machinery — candidate populations
  persisted with scores and a named selection decision.

**Implication for V4's spec:** the Student-plan gate should persist a
candidate record (which patterns were considered, from which discovery
receipts, why the chosen decomposition won) — the APM analog of the
audit's minimal-new-evidence list. Not necessarily a numeric G — but a
RECORDED selection with grounds, so the plan link of the chain is
evidenced, not asserted. **Implication for the WM:** a cascade-challenge
step (the TA move) before folding, cheap form: the correspondence
validator run against the cascade BEFORE the fold, plus one reviewed
applicability question per non-deduction pattern.

### C2. Where verification lives: intrinsic vs extrinsic

**APM:** the wiring is literally Lean code — verification is intrinsic
(the proof compiles or it does not); mark 6 (discharge evidence) is
nearly free. **WM:** the wiring is a construction over the world;
discharge evidence is extrinsic (checkers, witnesses, replays), so the
WM had to build the correspondence/commissioning apparatus the APM never
needed — and conversely the WM's honesty discipline about model↔runtime
correspondence (`:runtime-correspondence :not-proven`; the Ruling-5
audit) is AHEAD of anything the APM has needed to articulate.

**Implication for V4:** the APM inherits an attestation-hygiene risk the
moment it claims anything beyond compilation (learning outcomes,
transfer, pattern efficacy) — those claims are extrinsic, and the WM's
vocabulary discipline (route-green is not overall conformance; fixture
scope stated; no retroactive proof) should be adopted verbatim in the
V4 evaluation gates. **Implication for the WM:** where a WM obligation
CAN be pushed into Lean-intrinsic form (the correspondence predicate,
the collapse law), that is the cheap end of the proved-AIF-valid
programme — the APM demonstrates how much verification you get for free
when the artifact is the proof.

### C3. Selection/exposure evidence: complementary maturity

**APM:** offered / retrieved / read / cited / used are DISTINCT observed
events with receipts; fingerprint audits; exposure validation
(and this week, the plural-`:memory-ids` fix closing its gap). **WM:**
selection scores persist, but the G→outline join was just found
unevidenced (top-G named a different mission than the one enacted), and
pattern exposure during a fold is not receipted at all.

**Implication for the WM:** adopt the APM's receipt vocabulary for
pattern consumption in folds — which patterns the fold seat was shown
vs read vs folded is currently indistinguishable, which is exactly the
gap the APM spent August closing. **Implication for V4:** none needed —
this is the APM teaching.

### C4. Repair economics: dialogic vs commissioned

**APM:** repair is dialogic and role-structured (TA feedback names the
failed step; rescue chains; repair packets), and its failure taxonomy
(retrieval / applicability / execution / library defect) is the more
articulated. **WM:** repair is gate-and-commission (induced violations,
witnesses, frontier) — the more MECHANICAL discipline, and its
commissioning practice (every check watched to fail before trusted) is
ahead of the APM's, where gates are tested but not systematically
induced-commissioned.

**Implication for V4:** commission the V4 gates per V1 before the first
frame — the plan checker, the exposure validator, the publication gate
each get one induced violation watched to fail; the APM has good tests
and few witnesses. **Implication for the WM:** adopt the APM's failure
taxonomy at the fold-repair boundary (a refused cascade is a retrieval
failure, a failed condition an applicability failure, a hole an
execution gap) so WM repairs route by evidenced cause, not by gate name.

### C5. Why not unify

The differences in C1–C4 are not accidents to be engineered away: C2 is
intrinsic to the domains (proof-artifact vs world-construction); C1's
computed-vs-deliberated split reflects turn economics (WM clicks are
minutes and must be cheap; APM frames are hours and can afford
dialogue); C3/C4 are maturity differences that SHOULD converge. The
value of two instantiations of one theory is exactly that each system's
scar tissue commissions the other's checks — five of this week's
mechanisms crossed over (the rubric, the condition triples, the
correspondence checker, I11, the receipt fix), every one evidence-first.
The standing rule this note proposes: **when specifying either system,
walk C1–C4 and either adopt the other's answer or record why the
difference is legitimate** — a difference neither adopted nor justified
is a defect by default.

## Open questions (named, not resolved)

1. Does the APM need a numeric G, or is a recorded-selection-with-
   grounds sufficient until the WM's G→outline join is itself proven?
   (The WM should answer first; the APM should not adopt an unevidenced
   functional.)
2. Is the TA a G? — the deliberated selection COULD be modeled as an
   expected-free-energy judgment (information gain of a plan vs its
   cost); if the shared core theory is to be more than analogy, this is
   where it would be made precise. Joe's policy/G-over-policy pointer
   (Ruling 4.3) and the Petri/structured-proof framing both attach here.
3. Should V4's frames emit chip-board-style certificates (triple digest,
   replay-verified) for the plan→proof pipeline? The handoff algebra's
   certificate channel is NAMED and system-neutral; nothing in it is
   WM-specific.
