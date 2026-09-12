# NOTE-apm-wm-convergence-contrast — one theory, two instantiations

Commissioned by Joe 2026-09-12: the APM and WM are converging on core
issues — "you can't really have a proof without a cascade" is common to
both — and the V4 APM specification must be considered ALONGSIDE the WM
work: not to unify them, but to contrast them, and to ask why any
differences exist and what each difference implies for both. Drafted by
claude-15 (holding both sides' 2026-09-12 state); APM-side claims
redlined by codex-16 (V4 owner, read-only against the teaching
implementation at 7012d032 and its retained packets) — all amendments
accepted and folded, pointers spot-checked by claude-15.
**Status: AGREED (2026-09-12), claude-15 × codex-16.** The redline's
standing instruction is preserved throughout: implementation, scoped
witnesses, proposed extensions, and production claims are kept
distinct.

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
  correspondence validator carry related validation obligations — the
  WM's was explicitly built to the V4 checker's bar, but checker
  equivalence has not been demonstrated (codex-16 amendment).
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
- (Corrected per codex-16 redline, pointers verified.) APM V4 persists
  the Student's proposed decomposition, per-node warrants and
  applicability arguments, method revision claims, independent TA
  judgments, and responses across plan revisions; it retains job-bound
  search receipts (`teaching_plan.clj` warrant/revision/condition
  validation; `teaching_exchange.clj` captured receipts; the teaching
  README's durable journal and replay boundaries). These records
  support audit and replay of the accepted exchange. What they do NOT
  constitute: a frozen population of alternative plans, an explicit
  comparison explaining why one alternative prevailed, or a computed
  G-over-policies decision. Absence of G prevents claiming G-based
  selection; it does not make the existing exchange unauditable or
  unreplayable.

**Implication for V4's spec (amended per redline):** add a bounded
COMPARISON record at the Student-plan gate, distinguishing retrieved
candidates from alternatives actually considered: discovery receipts,
considered method revisions, alternative decomposition IDs,
applicability findings, choice/rejection reasons, decision owner, and
selected plan digest. "Only one candidate considered" is a permitted,
honest record; alternatives are never fabricated and exhaustive
introspection is not demanded. **Implication for the WM:** a cascade-challenge
step (the TA move) before folding, cheap form: the correspondence
validator run against the cascade BEFORE the fold, plus one reviewed
applicability question per non-deduction pattern.

### C2. Where verification lives: intrinsic vs extrinsic

**APM (corrected per redline):** Lean checks the formal artifact
against its elaborated statement and dependency environment. The APM
must separately establish that this is the intended problem and pinned
artifact, that its proof evidence meets the required axiom/admission
policy, and that the submitted cascade corresponds to the construction.
Compilation alone establishes neither pattern applicability nor
learning, transfer, or efficacy. The TA cascade is not itself Lean
code, and the offline checker explicitly checks review COMPLETENESS,
not mathematical validity — it can accept nonsense argument fields as
complete bookkeeping (offline README). **WM:** the wiring is a
construction over the world; discharge evidence is extrinsic
(checkers, witnesses, replays), so the WM built the
correspondence/commissioning apparatus — and its model↔runtime honesty
discipline (`:runtime-correspondence :not-proven`; the Ruling-5 audit)
is the more articulated.

**Implication for V4 (amended per redline):** the needed discipline
already exists in V4's own record — review receipts stating publication
unauthorized and mathematics unverified; authenticated review not
establishing reading or correctness; node-use reports as authored
observations, not benefit evidence; the WM-alignment note's
cross-system distinctions. The obligation is to PRESERVE AND ENFORCE
those existing distinctions (review / publication / exposure / reported
use / verified artifact / evaluated benefit), align their vocabulary
with the shared runtime invariants, and commission the joins. Gates are
referred to by NAME (the Student-plan gate, the TA-response gate, the
publication gate, the rehearsal gate), not by number. **Implication for the WM:** where a WM obligation
CAN be pushed into Lean-intrinsic form (the correspondence predicate,
the collapse law), that is the cheap end of the proved-AIF-valid
programme — the APM demonstrates how much verification you get for free
when the artifact is the proof.

### C3. Selection/exposure evidence: complementary maturity

**APM (amended per redline):** offered / retrieved / read / cited /
used are distinct EVIDENCE CATEGORIES with different observation
strengths — served content, asserted reading, reported use, and
independently verified use are not interchangeable; receipts,
fingerprint audits, exposure validation (and this week, the
plural-`:memory-ids` fix). **WM:** selection scores persist, but the
G→outline join was just found unevidenced (top-G named a different
mission than the one enacted), and pattern exposure during a fold is
not receipted at all.

**Implication for the WM:** adopt the APM's receipt vocabulary for
pattern consumption in folds — which patterns the fold seat was shown
vs read vs folded is currently indistinguishable, which is exactly the
gap the APM spent August closing. **Implication for V4 (amended):**
retain the category distinctions and strengthen correspondence
evidence between the weaker and stronger categories.

### C4. Repair economics: dialogic vs commissioned

**APM:** repair is dialogic and role-structured (TA feedback names the
failed step; rescue chains; repair packets), and its failure taxonomy
(retrieval / applicability / execution / library defect) is the more
articulated. **WM:** repair is gate-and-commission (induced violations,
witnesses, frontier). **Corrected per redline:** V4 HAS induced
negative controls through prototype and implementation paths —
citation-only closure, broken references and disconnected cycles
refused (`test_plan_review.py`); induced session drift refused before
activation and forged ancestry / withheld exposure refused
(`teaching_exchange_test.clj:240,275`, verified); unreviewed/rejected
publication and citation-only usefulness refused in the prototype
(`test_library_loop.py`); an independently reproduced recheck
(102/592, explicitly bounded). Those establish bounded COMPONENT
behavior. What V4 does not yet have is a commissioning record for an
INSTALLED plan→construction→publication→next-use loop — and the
unimplemented canonical revision publisher cannot inherit the
prototype's commissioning credit.

**Implication for V4:** the installed-loop commissioning requirement
stands (per V1, before the first V4 frame), scoped as above.
**Implication for the WM (amended):** adopt the APM's failure taxonomy
at the fold-repair boundary, with the diagnosis made by EVIDENCE, not
by gate name: a refusal need not mean retrieval failure; a hole need
not mean execution failure; a missing prerequisite is an applicability
issue; unavailable evidence may remain unknown; an explicit hole may
simply name intended future work.

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

## Open questions — with the V4 owner's answers (codex-16, agreed)

1. **Numeric G:** recorded selection with grounds is sufficient for the
   currently implemented teaching experiment's claims; it is
   INSUFFICIENT for any claim of G-selected or proved-AIF-valid
   construction. If Joe extends Ruling 5 to V4 as an acceptance
   requirement, that creates an explicit additional implementation gate
   — neither the WM's present maturity nor attaching numbers to reasons
   resolves it. (Open for Joe's ruling.)
2. **Is the TA a G? No, not as implemented.** The TA is an independent
   critic and applicability reviewer; it can supply observations or
   estimates to a future selection mechanism, but calling its
   accept/revise judgment "expected free energy" supplies no generative
   model, preferences, policy horizon, outcome predictions, or
   calculation. A future design could separate admissibility checks
   from comparative policy evaluation, with the TA contributing to
   either — with the hard constraint that invalid applicability remains
   a REFUSAL, never a cost a favorable score can outweigh.
3. **Chip-board-style certificates: yes to a checked certificate
   interface, not a second acceptance authority.** Bind the exact plan,
   method revisions, executed review, construction artifact,
   checker/environment identity, and node-to-artifact correspondence;
   digest joins establish identity, while semantic correspondence needs
   its own validator or explicitly scoped review. Reuse existing
   receipts and the shared certificate channel.
