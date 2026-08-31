# Defect classes for preemptive repair

**Status:** evidence catalogue, 2026-08-31. This is input to a later promotion
decision; it proposes no `sec-catalog` pattern text.

Each class below has a nonempty witnessed population. “Prevention type” is a
classification of the likely machine intervention, not approval to build it.

## 1. Acceptance that cannot fail

**Shape.** The instrument either accepts an empty/degenerate population, ignores a
failure quantity it computes, tests only shape/nonemptiness, or announces success
without inspecting the reader-held result.

**Instances.** Six:

1. R2's historical census ran over a supplied empty population and passed
   vacuously. C35 distinguishes this population defect from the now-rejecting
   predicate (`holes/labs/wm-contract/C22-falsifier-invocations.md:100–118`).
2. futon3 `find-snatch` reported 21 mismatches from its first structured-find
   commit `f1998a5` through binding commit `5941270`, but exited 0. C61 commit
   `0cf5524` made them rejecting
   (`holes/labs/wm-contract/C60-find-snatch-acceptance-decision-2026-08-31.md:13–39`).
3. `wmRunConformsToWiring` accepted any nonempty route, even with unmapped hops;
   C69 added the drawn/measured population and an unmapped-hop mutation
   (`holes/labs/wm-contract/C69-measured-routes-figure.md:24–29`).
4. Count-only BMR accepted 6903/6903 pairs
   (`holes/labs/slush-demo/findings/bmr_constellation_experiment.out.txt:10–11`).
5. The former `ProbabilityKernel.normalised : ∀ _s, ∃ total : ℝ, total = 1`
   mentioned no mass and was true for every kernel; C7 replaced it with an actual
   row-mass equation (`holes/labs/wm-contract/C7-belief-update-findings.md:9–13`;
   current `mathlib4/DarkTower/WarMachine/Holes.lean:511`).
6. `p4ng/build-p4ng.sh:118` printed a publication-success line unconditionally;
   the discovery is recorded at `holes/problems/BUILD-PLAN-0831.md:655`.

**Found by.** Semantic mutations; deliberately empty populations; counting
accepted/rejected outcomes; reading the proposition rather than satisfying it;
and checking the published artefact instead of the producer's message. The
kernel, `find-snatch`, and build announcement were found while someone was already
reading those files, not by their original gates.

**Would prevent.** Every acceptance names a rejecting fixture in advance; a
computed error count participates in the exit verdict; population nonemptiness is
an explicit precondition; success announcements are conditioned on the actual
copy/check result. **Prevention type: check + lint.**

## 2. Provably false declaration recorded as a hole

**Shape.** A hole means “true but not yet discharged,” yet a universal quantifier
over a type or arbitrary function admits an empty, singleton, or constant
instantiation that refutes the proposition.

**Instances.** Four:

- Former `organiseO4PrecedenceGovernance`: constant acting-order/score functions
  or an empty policy carrier refuted its existential; repaired at mathlib4
  `ef6e501f0b` (`holes/labs/wm-contract/C64-organise-fixture-scope-amendment.md:7`).
- Former `valueEvidenceRequiresL2`: choose `valueEvidence := fun _ => True` and
  an L1 witness; repaired at mathlib4 `72fa098799`
  (`holes/labs/wm-contract/C68-final-unwitnessed.md:7–10`).
- Former `nonDegenerateAblationLaw`: `Policy := Empty`, a singleton, or identical
  constant graders defeats distinct minimisers
  (`holes/labs/wm-contract/C70-hole-quantifier-sweep.md:5–8`).
- Former `findF4Falsifiable`: `P := Empty` or `repo.patterns := ∅` defeats the
  required omitted member (same source, lines 8–10). Both latter declarations were
  restated at mathlib4 `a9d78f8904`.

**Found by.** Reading quantifiers and constructing the smallest degenerate model,
not by trying harder to produce a positive witness.

**Would prevent.** A declaration-entry review must instantiate every universal
carrier with empty/singleton types and every universal function with constants;
empirical claims use a pinned fixture or explicit nondegeneracy hypotheses.
**Prevention type: review question + possible Lean lint.** Automated generation of
all countermodels is uncertain; syntactic flagging of the risky quantifier shape is
not.

## 3. Witness measuring the wrong object

**Shape.** The witness passes, but its observation is not the referent named by the
claim, or its evidence type validates a vocabulary/shape rather than the claimed
mechanism.

**Instances.** Five:

- R16's `engine-wiring` returned construction inside the model, not outward
  actuation (`holes/problems/P-R16.md:111–118`).
- F2's receipt cited the authored antecedent while the runner fired on a separate
  abbreviated representation; receipt presence could not establish what fired
  (`holes/labs/wm-contract/C60-find-snatch-acceptance-decision-2026-08-31.md:27–39`).
- `EraTable` shape validation once accepted a declared vocabulary that no producer
  emitted; the first real report was `:wrong-shape`
  (`holes/problems/P-lean-clojure-adapter.md:140–148`).
- The ablation witness passed a finite Snatch case but was bound to the former
  universal `nonDegenerateAblationLaw`.
- The find witness passed six scenarios but was bound to the former universal
  `findF4Falsifiable`; both false bindings were removed and characterised in
  `holes/labs/wm-contract/C72-false-binding-audit.md:5–10`.

**Found by.** Ask “what does this witness actually demonstrate?”, then compare its
quantifiers, carrier, and referent to the declaration; trace the value to the code
that actually produces it.

**Would prevent.** A binding carries an explicit claim-to-observation adapter and a
negative control at the claim's quantifier scope; provenance distinguishes self
construction from independently readable effect. **Prevention type: review
question + check.**

## 4. Artefact-boundary failure

**Shape.** Correct work exists at one representation or staging boundary but never
reaches the committed or reader-held artefact used for acceptance.

**Instances.** Five populations:

1. Records were cited while still untracked (`holes/problems/BUILD-ledger.md:161`,
   `:1922–1930`, `:1990–2002`).
2. The control-map SVG was corrected while LaTeX rendered a stale tracked PDF
   (`p4ng/vetting/CLEANUP-QUEUE.md:363–368`).
3. Withdrawals were verified under `--stage` while the published PDF remained old
   (`p4ng/vetting/CLEANUP-QUEUE.md:859–875`).
4. Corrected tutorial rows did not occur in the printed rendering; commit
   `8ed7149` moved the correction into a printed section
   (`p4ng/vetting/OBLIGATIONS-REVERIFY-2026-08-31.md:171–173`).
5. Two dormant TeX sources received work: `sec-discussion.tex`, and `empirics.tex`
   behind a commented input. The bounded census found no third case
   (`p4ng/vetting/C45-TEX-REACHABILITY-2026-08-31.md:1–63`).

**Found by.** `git status`; transitive reachability census; source/derived agreement
check; rebuilding without `--stage`; `pdftotext` against `/var/www/.../wip/*.pdf`.

**Would prevent.** Acceptance names the consumer-held artefact, proves every cited
file is tracked, and verifies reachability plus derived/source agreement before
closure. **Prevention type: lint + check.**

## 5. Baseline stale by construction

**Shape.** A test compares a moving live corpus to a literal snapshot, or a binding
pins the whole evolving authority, so ordinary production by the system
invalidates its own expected value.

**Instances.** R2 exact file/form counts, R8 exact disposition counts,
`contract_lint` moving declaration counts, `control_map_lint` moving edge counts,
and the 16 witness bindings identified by C37. The R2/R8 failures and the split are
recorded at `holes/labs/wm-contract/C22-falsifier-invocations.md:49–56,98–138`;
C37 records the binding population at
`holes/problems/C37-strict-contract-qualification.md:18–38`. The shared trace
corpus moved repeatedly during the same day's verification, including 799→800
forms (`C22-falsifier-invocations.md:157–164`).

**Found by.** Count the same quantity across commits/reruns before updating it;
separate a change in population from a violation of the invariant.

**Would prevent.** C35's split: immutable dated fixture for exact reproduction,
live gate for semantic invariants and typed delta classification. Binding freshness
pins declaration text/fixture semantics rather than unrelated repository motion.
**Prevention type: check + lint.**

## 6. Absence coerced to a value

**Shape.** Missing measurement, malformed record, or unavailable model input is
converted to `0`, `1`, `{}`, or `[]`, after which downstream consumers cannot tell
absence from a legitimate datum.

**Instances.** The semantic census identifies 27 boundaries: 17 unsafe numeric
coercions, 9 explicit model/algebra defaults, and 1 compatibility default
(`holes/labs/wm-contract/C12-absence-census.edn:9–14`). It begins at observation
and repeats through free energy, policy, precision, rollout, adapters, belief, and
trace (`C12-absence-findings.md:9–27`). Historical numeric-only traces make the C50
counterfactual permanently unreconstructable (`C12-absence-findings.md:29–34`;
`p4ng/vetting/CLEANUP-QUEUE.md:2276`).

**Found by.** Census `get`/`get-in` defaults, `or`-to-scalar, destructuring `:or`,
nil guards, and serialization boundaries before attempting repair.

**Would prevent.** Typed present/absent envelopes at ingress, schema versions in
persistence, loud consumer policy for absence, and a lint over coercive defaults
with explicit exemptions for algebra/configuration. **Prevention type: lint +
check.**

## 7. Duplicate representation never reconciled

**Shape.** One semantic name has two or more carriers that happen to share a loose
type or prose label; no bridge establishes equality, precedence, or conversion.

**Instances.** Four:

- `find-snatch` authored and runner antecedents: 21 differences born unequal
  (`C60-find-snatch-acceptance-decision-2026-08-31.md:13–32`).
- `C` as scalar cost/payoff versus normalized outcome distribution
  (`holes/problems/P-validated-R5.md:83,308`).
- Scored-cascade π versus `Policy := InformationState → Action`
  (`holes/problems/BUILD-PLAN-0831.md:780–899`).
- Four distinct free energies sharing `ℝ`; the vocabulary audit begins at
  `holes/problems/BUILD-PLAN-0831.md:875` and the perceptual/BMR separation is
  preserved at `holes/problems/P-evidence-apex.md:527`.

**Found by.** Count definitions/usages by semantic name; ask what actually calls
each symbol; reject type-level matches as semantic bridges.

**Would prevent.** One authoritative carrier, or explicit conversion with laws and
named direction; namespace types by role rather than reuse a scalar alias.
**Prevention type: lint + review question.** How much semantic synonym detection
can be automated is uncertain.

## 8. Era-blind expectation

**Shape.** Today's schema is applied to records created under an earlier schema,
turning valid history into apparent malformed data.

**Instance.** C58 proved the two May 18 records lacking `:annotation-health` belong
to the 13-channel era; the third record at `2026-05-18T21:33:02Z` starts v0.10 and
has the field. The last old record and first new record are only about 39 minutes
apart (`holes/labs/wm-contract/C22-falsifier-invocations.md:140–155`;
`holes/labs/wm-contract/C58-r2-channel-eras.md:1–20`).

**Found by.** Sort records by time, inspect adjacent boundary records, and search
the producer history for the schema-changing commit rather than assuming a
day-sized era.

**Would prevent.** Persist schema/version at write time and dispatch validation by
record era; negative controls remove a field required in that record's own era.
**Prevention type: check + schema lint.**

## 9. A record that says two things

**C79 instance (2026-08-31).** `checks/preemptive_repair_lint.clj` contained both the live detector and its four deliberately bad `negative-text` specimens. Once the file became tracked, the detector reported those specimens as current corpus defects. An explicit specimen-region marker now distinguishes executable negative controls from the live corpus without exempting the `checks/` path. Found by rerunning every positive lint after C77 was committed; prevented by a narrow, line-preserving specimen boundary and a control proving the boundary does not disable direct mutation runs.

**Shape.** Current state is duplicated in a summary/table and amendment/prose, or a
dispatch record has no completion transition; different readers obtain opposite
answers from the same artefact.

**Instances.** Three:

- `R2-R7-delivery.edn` co-signed decisions under `:ratification` while retaining
  them as live `:disagreements`; it now preserves history separately and has an
  empty live vector (`holes/labs/wm-contract/pair/R2-R7-delivery.edn:4–6,107,134–155`).
- Obligation rows O20–O22 remained open/regressed in the table after dated prose
  closed them; reconciled in p4ng `896b6b6`
  (`p4ng/vetting/CLEANUP-QUEUE.md:1889–1901`).
- The lane registry still showed C61 active two hours after completion because it
  had a dispatch transition but no completion transition
  (`p4ng/vetting/CLEANUP-QUEUE.md:3219–3229`).

**Found by.** Parse and tally each representation independently; compare registry
jobs with live Agency state; never rely on the summary produced by the same record.

**Would prevent.** One authoritative current-state field plus append-only history;
checks reject a live/history contradiction and reconcile dispatch with completion.
**Prevention type: lint + check.**

## Reusable discovery and repair methods

| Method | Finds | Repair discipline |
|---|---|---|
| Semantic mutation / negative control | Acceptances unable to reject the named falsifier | Preserve valid shape, mutate one semantic fact, require rejection (`0/1/2` convention). |
| Read quantifiers; instantiate degenerately | False holes and overbroad contracts | Try empty/singleton carriers and constant functions before seeking a witness. |
| Count the same quantity across commits | Baselines stale by construction; false closures; born-broken checks | Freeze and compare populations before restamping; classify each delta. |
| Ask what actually calls/produces the symbol | Wrong-object witnesses; duplicate vocabulary; dormant mechanisms | Trace from producer through consumer, not from similarly named declarations. |
| Verify the reader-held artefact | Stage/publish, source/derived, dormant-source failures | Build the publication path and inspect the published PDF/output directly. |
| Independent fixture from mathematics | Implementation self-certification | Derive expected values independently; implementation output is the observed leg, not its own oracle. |
| Census before repair | Pipeline-wide defaults, reachability, referent drift | Bound the population, classify every member, then change only the justified subset. |
| Ask “what does this witness actually demonstrate?” | Claim/evidence quantifier and referent mismatch | State the narrow empirical proposition first; bind only if it entails the declaration. |

These methods compose. The strongest recurring sequence today was: **census → pin
the population → semantic mutation → inspect the named consumer → repair → rerun
against the consumer-held artefact**. Promotion priority is deliberately left to
the owner.
