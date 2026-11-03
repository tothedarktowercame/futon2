# Inner-loop alternatives recorded today (2026-11-03)

## Finding
The ordinary-click inner loop has a machine-readable policy comparison, but not
yet an argument frame.  Its tick run record retains the scored candidate family,
posterior, strict-G choice, prediction-class membership, stopping comparison,
and (when there is one) winner-versus-runner-up numerical contributions.  It
does not retain prose grounds or warrants explaining why each approach is an
appropriate candidate.  The newest run inspected has one candidate, hence no
alternative solution approach to compare.

The inspected records are
`data/wm-runs/tick-run-record-2026-11-03-561c5ec3-6510-42a3-930b-331ef089feb4.edn`
(inner/ordinary click) and
`data/wm-runs/selection-record-2026-11-03-wm-click-2780abb9-f6f8-4db6-9b8f-95d156737774.edn`
(outer selection).  The latter's alternatives live at
`[:outer/selection :why :classes]`; they are not the inner alternatives.

## Choice points and their records
1. `src/futon2/aif/flight.clj:326`, `choose-target`: chooses the oldest concrete
   actionable repair proposal, otherwise the requested target.  The flight
   retains the winner and its source under `[:target]` and `[:chosen-because]`
   (`start`, line 351); the proposal supply is retained inside
   `:chosen-because`, but alternatives have no G or comparative scores.

2. `src/futon2/aif/flight_driver.clj:65`, `resolve-target`: chooses a supplied
   outer-loop `:chosen-target` over a hand `:target`.  The flight plan records
   `[:placement]` and `[:target-selection]` (lines 196-202), including the outer
   posterior/draw when supplied; this places the inner loop but is not its
   solution-approach choice.

3. `src/futon2/aif/cascade_problems.clj:249`, `assemble-one`: chooses constructed
   advancing declared cascades, or constructs from interpretations when none
   advance (lines 267-283), and admits only constructed nonempty precedences.
   The supplied problem retains all admitted alternatives at
   `[:constructed-candidates]` and `[:cascade-problem :precedences]` (lines
   351-359), with construction receipts but no G (scoring happens later).

4. `src/futon2/aif/cascade_policy.clj:284`, `candidate-space`: when precedences
   are absent, enumerates every nonempty interpretation subset in topological
   order, plus the empty cascade; otherwise uses caller-authored precedences
   (lines 323-331).  Its return retains every alternative at `[:candidates]`
   with precedence, steps, acting order, established tokens, coverage and
   organisation receipt, plus `[:rules]` (lines 341-372); it makes no winner.

5. `src/futon2/aif/policy.clj:400`, `select-action-cascades`: this is the live
   ordinary-click solution choice.  It computes the full posterior (lines
   494-495), excludes non-acting cascades (510-521), optionally restricts to the
   earliest ticket stratum (522-532), then chooses the unique minimum-G
   prediction class and its first ranked representative (537-545).  The tick
   record retains alternatives and scores at
   `[:decision :selection-certificate :candidates]` (per candidate `:id`, `:g`,
   `:habit`, `:f`, inputs and emission certificates), probabilities at
   `[:decision :selection-law :posterior]`, the chosen class/winner under
   `[:decision :selection-law]`, and the compact winner/runner-up comparison at
   `[:decision :selection-law :policy-comparison]`.  Thus alternatives and G
   are retained, not only the winner.

6. `src/futon2/aif/cascade_selection.clj:258`, `minimum-class`, and line 328,
   `admissible-minimum`: group equal predictions, choose the strict least-G
   admissible class, and refuse an exact tie or empty admissible set.  Their
   result is folded into the selection-law fields above; forbidden policies
   remain scored and are named by the Q6/ticket receipts rather than erased.

7. `src/futon2/aif/inner_abstention.clj:86`, `comparison`: compares every
   admissible policy's action G with its separately scored no-action G, choosing
   the strict-G winner only if it beats stopping, otherwise abstaining (lines
   91-102).  The record retains every stop alternative and G at
   `[:decision :selection-certificate :abstention :per-policy]`, plus
   `:chosen-reference`, `:rule`, and `:cross-lens`; action candidates remain in
   `:candidates`.  No-action alternatives and scores are therefore retained.

8. `src/futon2/aif/cascade_selection.clj:448`, `selection-comparisons`: after
   selection, records the enacted class against the runner-up class, including
   habit, free-energy and G contributions (lines 455-484).  Its record is
   `[:decision :selection-law :policy-comparison]`; with no runner-up it says
   `:no-competing-policy` rather than inventing an alternative.

9. `src/futon2/aif/flight.clj:597`, `record-click`: does not choose again; it
   projects only `:chosen` into `[:clicks n :chosen]` (lines 621-644).  Likewise
   `flight_runner.clj:497` `record-summary` projects only the chosen id,
   candidate and precedence from the tick record (lines 513-522).  The complete
   alternatives remain in the referenced `tick-run-record-*`, not the flight's
   click summary.

## What the recent inner record actually carries
The inspected tick has one row in
`[:decision :selection-certificate :candidates]`, with `:g
1.6711199189323418`, and one posterior entry of mass `1.0`.  Its certificate
also has `:policies`, `:policy-stages`, `:g-term-decomposition`, scoring and
preference receipts; its selection law has `:candidate`, `:enacted`,
`:class-members`, `:posterior`, `:policy-comparison`, beta/gamma/tau and
softmax fields.  Its abstention carrier has `:per-policy`, `:chosen-reference`,
`:rule`, and `:cross-lens`.  Finding: the schema can retain competitors, but
this run supplies no runner-up and therefore cannot answer "why this approach
and not that one" with an observed alternative.

## Lean names versus production records
`CascadeCompetitorsEval.lean:14-21,57-121` names and evaluates three concrete
approaches (`delegate`, `delegateChecked`, `inPlace`) under `C4`; no production
record inspected names those three or records their evaluation as its candidate
family.  `CascadeEFEPolicies.lean` specifies policy-grain G and candidate-family
priors, while `PolicySelection.lean:24-72` specifies extended-G weights,
posterior, and infinite-risk exclusion.  Production records the corresponding
runtime quantities (candidate G, E/habit, F, beta/gamma and posterior), but not
a receipt asserting that its candidates are the Lean example competitors or a
Lean proof object for the selection law.

## Shape needed for an inner argument frame
- **Pick:** present today as selected candidate/action, enacted policy and class.
- **Grounds:** numerical predictions, G terms, inputs and receipts are present;
  a concise ground per alternative is missing.
- **Warrants:** selection-rule names and construction/scoring receipts are
  present; the explicit warrant connecting each approach's grounds to its
  suitability and comparative conclusion is missing.
- **G per alternative:** present for every admitted candidate as certificate
  `:g` (and stop G under abstention); absent for pre-scoring construction
  proposals/refusals and, in a singleton run, absent for any real competitor.

## Review (claude-2, 2026-11-03 16:4xZ, job invoke-1793723864788-10859-d5d31b8d, commit c7c88231a)

Checked: `policy.clj:400` is `select-action-cascades` and lines 537-545 take the
admissible minimum class and its first ranked member; `cascade_selection.clj:448`
is `selection-comparisons` ("the enacted class against the runner-up admissible
class"); `inner_abstention.clj:86` is `comparison` ("all compared entries
retained"). Cites hold. Not checked: cascade_problems and flight_driver lines.

Beyond the note: the newest tick had one candidate, but multi-candidate ticks
exist. `tick-run-record-2026-11-01-3f903a0e-…` has five candidates for
M-pattern-retrieval-calibration: four at G 1.7550 and the winner at 1.6996
(margin 0.0553 nats; `policy-comparison` `:near-tie? :threshold-undeclared`).
The posterior's keys are the policy identities and carry, per unit, the
occurrence square (G/I1/I2/B with `:source` path and sha and a
`:situation-source` line), so grounds and warrants per alternative are already
retained, more fully than the outer record's.

Judgement: an inner argument frame is possible today on such ticks. Arguments:
the admitted policies plus stopping; pick: the policy; grounds: the occurrence
squares; warrants: the patterns' conclusions; G: `:g`. What the outer frame's
attack vocabulary (gate excludes, order prefers) does not cover: inner
candidates differ by which units they include and in what order, so the attacks
to compute are coverage (a unit you lack whose need is live), forbiddenness
(Q6/ticket receipts) and stopping. Packet E, after the Lean clause (argument-A)
and the Clojure port (argument-B) land, should take this tick as its fixture.
Finding for Joe: four of five alternatives score identically, so "why this
approach and not that one" is answered there by 0.055 nats and by nothing read.
