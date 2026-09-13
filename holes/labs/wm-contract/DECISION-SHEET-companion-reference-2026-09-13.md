# Decision sheet: which paper owes the companion material (P001-P005)

Date: 2026-09-13. Prepared by claude-15. Blocks the two red OUTSTANDING
cards in plop-2026 (opening + conclusion) and inventory rows P001-P005.
This is the ruled-shaped blocker the cards themselves name: "deciding
which active paper owes the formal and experimental material is an
operator decision that no amount of build work discharges."

## The question

plop-2026.tex:3-4 promises "the preregistered experiment reported in the
companion paper" (\xzaif = "The companion paper"). The futon-2026 driver
distinguishes a separate mathematical companion. Which active paper owns
(a) the formal material and (b) the preregistered experimental report?
The dormant empirics file cannot discharge the promise.

## Proposed principle (from Joe's blanket remark, 2026-09-13)

"The blanket for this paper [plop-2026] is the REPL, most likely! the
futon-2026 closure over the operator changes that."

Applied: the two papers draw different Markov blankets over one stack,
so the material splits by blanket, not by topic convenience:

- plop-2026 (blanket at the REPL): the pattern catalog as seen at the
  operator interface -- what the machine does, witnessed at its
  channels. Its companion promise for FORMAL material points to the
  mathematical companion futon-2026 already distinguishes.
- futon-2026 (closure over the operator): owns the formal treatment AND
  the preregistered experimental report, because the experiment's
  design (cohorts, castings, operator rulings) includes the operator
  inside the system boundary -- exactly what its closure models and
  plop-2026's blanket excludes.

## Options

A. RULE THE SPLIT ABOVE: \xzaif resolves to futon-2026; plop-2026's
   promise becomes a live cross-reference; P001-P005 close by binding
   the promise to futon-2026's preregistered-run section (the cohort
   47/48 machinery-test series and its successor qualifying run are
   the natural referents). The two OUTSTANDING cards flip to resolved
   with a dated note.
B. plop-2026 keeps the experimental report itself (an empirics section
   is revived in-paper); futon-2026 keeps only mathematics. The cards
   close only after that section is built from admitted evidence.
C. Defer: cards stay red; P001-P005 remain open obligations.

## Recommendation

A. It follows the recorded blanket principle, requires no new evidence
construction, and the qualifying-run series (row 26 machinery now
demonstrated end to end) gives futon-2026 a real preregistered
experiment to report. Option B builds a second evidence home the
completion list already marks dormant.

## RULING (Joe, 2026-09-13, emacs-repl)

"I am happy with your decisions. the one addition is not an empirics
section but a lean certificate, as part of a new Methods section. To
add when we have the relevant certificate!"

Applied reading: Option A stands -- \xzaif and the companion promises
resolve to futon-2026, which owns the formal treatment and the
preregistered experimental report. The one amendment to A: plop-2026
additionally gains a NEW METHODS SECTION whose content is the Lean
certificate (machine-checked), added ONLY when the relevant certificate
exists -- not an empirics section, and nothing is added now. The
certificate's identity is not pinned by this ruling; the natural
candidates are the run-certificate chain the repair-attempt-001 work is
building (:wm/f2-reconciliation-certificate-v1 and successors) and the
M-f11 Lean acceptance (F1-F4 witnessed, find sorry discharged), to be
settled when one exists to pin. P001-P005 close to
RULED-PENDING-CERTIFICATE.
