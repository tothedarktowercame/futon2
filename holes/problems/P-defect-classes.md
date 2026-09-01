# Defect classes for preemptive repair

**Status:** evidence catalogue, 2026-08-31. This is input to a later promotion
decision; it proposes no `sec-catalog` pattern text.

Each class below has a nonempty witnessed population. “Prevention type” is a
classification of the likely machine intervention, not approval to build it.

## Current measured state

C77/C79's six mechanical lints now measure classes 1, 4, 5, 8, and 9 as
**tested-extinct** across futon2, futon3, and p4ng: their historical instances
remain below, but the current scans find zero. Class 6 is the sole live
mechanical population. Its broad semantic census began at 27 sites; the guarded
disposition/lint population has fallen from 18 grouped unsafe sites to **7 live
blocked sites** after C136. All seven now require operator decisions rather than
mechanical edits (`checks/absence-coercion-dispositions.edn`). Classes 2, 3,
7, and 10 remain judgement-scoped and therefore have no honest extinction claim.

## 1. Acceptance that cannot fail

**Shape.** The instrument either accepts an empty/degenerate population, ignores a
failure quantity it computes, tests only shape/nonemptiness, or announces success
without inspecting the reader-held result.

**Instances.** The original seven below, plus a nine-site cross-layer population
measured on 2026-09-01:

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
7. Ten Lean witness wrappers treated a successful elaborator exit as sufficient
   positive evidence, so an empty source file made eleven negative modes claim
   their positive baseline was valid. C356 requires a declaration-bearing,
   `sorry`-free source before elaboration
   (`holes/labs/wm-contract/C356-nonvacuous-positive-lean-witness.md`).

The C361 population showed the same empty-subject acceptance in nine independent
boundaries: a semantically empty ledger; an incomplete run certificate; a
zero-declaration strict contract; unreadable run-id history treated as proof of
uniqueness; an all-zero model-coverage table; empty Lean positive witnesses; a
zero-target restoration manifest; a missing/empty restoration journal; and an
observation-free writer-fence receipt. These are one property, not nine local
idioms: success must establish that its subject existed and was examined.
`checks/empty_subject_acceptance_lint.py` registers the acceptance subject and
its executable nonempty proof, tests real pre-repair revisions, and runs in the
workspace gate in report-only mode. Its blocking self-test proves that Python
`all`, Clojure `every?`, and truthy defaults are rejected without a nonempty
precondition while explicit empty rejection is accepted. The lint is bounded:
novel and helper-hidden data flow is `unverified`, not automatically declared
defective. **Prevention type: check + lint.**

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

Two later counterexamples extend the same method beyond the original four-hole
sweep. An unconditional identification of posterior spread with canonical EIG
is refuted by a one-policy, one-outcome normalized point-mass model whose EIG is
0 while the supplied spread bonus is 1
(`holes/labs/wm-contract/C119-model-uncertainty-counterexample.md:1–18`). The
historical `machineHasNoC` proposition is refuted by the in-language census's
free `vertexLocalC` constructor
(`holes/labs/wm-contract/C122-free-preference-census.md:1–27`). Both were settled
by counterexample rather than retained as unwitnessed truths.

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

The same class appeared four times as **proof about a copy**: a theorem over a
generated sibling was offered for a contract-source constant with no equality
between them. The four witnessed surfaces were R9's verdict table
(`holes/labs/wm-contract/C99-R9-source-proofs.md:1–8`), R2's trace census
(`holes/labs/wm-contract/C92-R9-R2-hole-census.md:17–20`), R8's trace census
(`holes/labs/wm-contract/C102-R8-hole-census.md:27–38`), and the APM adapter's
hand-restated `required-*` contract copy
(`holes/labs/wm-contract/AD-D1-findings.md:131–149`). The first three require a
proof over the source object or checked source/generated equality; the fourth
requires an independently meaningful consumer check rather than equality to a
second authored table.

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

A concrete later instance showed why the class is semantic, not stylistic:
`free_energy.clj` converted the same missing observation to `0.0`, then returned
opposite avoidance verdicts solely according to whether each channel's avoided
range happened to contain zero. C118 replaced that diagnostic with
`:satisfied` / `:violated` / `:unknown`, preserving absence reasons
(`holes/labs/wm-contract/C118-tristate-avoidance-diagnostic.md:7–31`). The live
lint trajectory is 18 grouped unsafe sites at C81, then 16, 15, 13, 11, 8, and
now **7**; seven is not extinction, and each remaining site is held on a named
behavioural decision.

**Found by.** Census `get`/`get-in` defaults, `or`-to-scalar, destructuring `:or`,
nil guards, and serialization boundaries before attempting repair.

**Would prevent.** Typed present/absent envelopes at ingress, schema versions in
persistence, loud consumer policy for absence, and a lint over coercive defaults
with explicit exemptions for algebra/configuration. **Prevention type: lint +
check.**

### 6a. A format boundary is a coercion boundary (claude-1, 2026-09-01)

**Shape.** Absence survives every typed boundary and is then coerced by the
*renderer*. Java's `%d` on `nil` prints the string `"null"` rather than
throwing, so a renamed or missing key becomes visible text in a published
artefact with no error raised anywhere.

**Instance.** The war-room tetrahedron published `null/38 attributed cases
done` after `:completed-by-lanes` was renamed to `:closed-by-lanes` in the
workflow report's v1→v2 schema change. Nothing failed; the figure rendered.
It was caught only because the rename was announced explicitly, and then only
by eyeballing the raster — which claude-1 notes is not a gate.

**Why it belongs under class 6 rather than class 4.** The artefact boundary
held: the file existed, parsed, and was read. The value was absent and the
formatter supplied `"null"` for it — absence coerced to a value, with the
value being a string that looks like a legend.

**The boundary is aggregation, not formatting (claude-1, 2026-09-01).** The
audit's two most dangerous findings — vacuous all-zero output and silently
lowered counts — did not arise at a format call. They arose from `frequencies`
or `sum` over a population **whose vocabulary was never proved**: a row whose
status matches no known category contributes to no bucket and vanishes from the
total, while every rendered cell stays correct. *"Formatting was where the
symptom surfaced; aggregation was where the absence was coerced."* A lint aimed
only at format calls would miss both. `%d`-on-nil is the visible tail of the
class; the aggregation boundary is its dangerous half, because its output is a
plausible number rather than the string `"null"`.

**Would prevent.** Prove the population's vocabulary before aggregating over it,
and reconcile totals to the whole population rather than to the categories that
matched. **claude-1 (2026-09-01) identified which half a lint can actually
check:** a total computed from matched buckets is a *syntactic pattern*; a total
asserted equal to the population count is its *cure*. So the checkable rule is
`sum(buckets) == count(population)` — e.g. `closed + open + named-only +
unclassified == len(rows)`. Vocabulary proof stops a renamed category becoming a
silent zero; the reconciliation assertion stops an unmatched row vanishing from
the total.

**A rule-defined count is not a historical floor (2026-09-01).** wm-nouns
refused a numeric floor for the declaration count because *"a historical minimum
would become another stale count."* That reasoning was then misapplied by me to
a population **fixed by construction under a stated counting rule**, where the
invariant is genuine. The distinction: a floor derived from observed history
drifts; a count defined by a rule does not, and asserting it is correct. But
**key the assertion on the rule version**, so a legitimate rule change fails as
*"version 2 not understood"* rather than as a population mismatch that reads
like truncation.

**The connection to class 5 (claude-1, closing):** *"A number pinned **by a rule** is a fixture; a number
pinned **by observation** is a baseline that goes stale by construction."* That is the same split class 5 draws
between snapshot and live, arriving from the counting side. The `61` repair-ledger population was the first
instance, wm-nouns' refused declaration floor the second — **and the check that names the rule is how the two
stay distinguishable in code**, since both look like an integer in a guard.

**On lint design (claude-1):** *"A lint that accepts a comment as proof and a
lint that rejects proof without a specific spelling are the same mistake facing
opposite directions — both check for a token rather than the property."* This
campaign's lint made both errors within two hours. The checkable property is
"every `frequencies`/`sum` over a field is preceded, in the same file, by a
membership check on that field against a literal set". Both are needed: the first catches renames, the second catches
categories nobody anticipated. Then assert every key formatted *before* formatting
it — the same discipline as asserting a string replacement before trusting it,
which this campaign learned the same night by shipping a `.tex` caption whose
replacement had silently failed. **Prevention type: lint** over format calls
whose arguments are not previously asserted non-nil.

**2026-09-01 population and executable prevention (C278/C281).** An audit of
the eight live-paper generators found seven exposed boundaries: literal
`null`, blank strings, vacuous all-zero tables, silently lowered cell totals,
default-zero categories, and missing lanes rendered as idle. Only the
defect-repair tally validated every reader-facing value it rendered
(`holes/labs/wm-contract/C278-live-artifact-format-boundary-audit.md`).
`checks/live_artifact_format_boundary_lint.py` makes that population
executable. The audit census reported seven; while the lint was being built,
`gen_model_coverage.py` added a nonempty population and per-row field proof, so
the live lint now reports **six** findings across the eight declared generators
and identifies model coverage plus defect tally as clean. It requires an
explicit field/population proof to retire each finding. Its negative
control flags an unproved `%d` value while accepting the same formatter after
a same-scope non-nil assertion. The lint is deliberately bounded rather than a
claim of whole-program data-flow analysis.

**2026-09-01 C284 correction.** A marker is a claim about proof, not proof.
`FORMAT-PROOF` comments are no longer accepted by the lint. Only recognised
executable validation/reconciliation shapes retire a finding; a helper-hidden
proof stays review-required until the lint learns its executable shape. The
control proves that a marker-only formatter remains flagged.

**Related.** Renaming a field under an unchanged schema version is what let
this reach a consumer; the workflow report now bumps its schema version on any
field rename, and notifies consumers with an explicit field table.

## 6b. Access mistaken for evidence (2026-09-01)

**Shape.** A verification accepts a token, identifier or copied signature that
proves the presenter could **reach** something, and treats it as proof that
something **happened**. The artefact is well-formed and the check is real; what
is absent is any link between the credential and the event it is taken to
witness.

**Four instances in one night, each inside a repair for the previous one:**

1. **A fence identifier.** `--writer-fence <anything>` made the preflight report
   `:status :held` and `:event-free? true`. Knowing a name is not observing a
   fence.
2. **Caller-authored state maps.** Three consumers accepted a caller-supplied
   `{:status :held}`; one API offered it as an interface, so it would have
   manufactured new acceptors indefinitely.
3. **A private in-process token.** `ns-resolve` reached the supposedly private
   minting seam, and a public dynamic var could be rebound to mint a genuine
   token from fabricated output. **Privacy in Clojure is a convention, not a
   boundary** — resolved by removing the in-process capability entirely and
   making a fixed subprocess the integrity boundary.
4. **A copied HMAC.** Restoration attempt rows carried the manifest's signature
   rather than their own, so a fabricated attempt plus externally restored state
   reconciled as though an inverse had run. Copying a readable signature proves
   the copier could read the manifest.
5. **A recorded digest never revalidated at use.** C385 censused every logical
   hash/signature carrier in the verification and operator paths and classified
   each by what its consumer actually does. Two load-bearing carriers in
   `scripts/wm_quiet_run_state.py` recorded strong-looking SHA-256 values but did
   not compare them with their subjects when the ledger was resumed: transition
   evidence file hashes and the parking-specification hash. The row digest
   proved that the writer once had access to those bytes; it did not prove that
   the bytes later being trusted were the same. A filename looks like a pointer,
   while a SHA-256 looks like proof, but without recomputation both establish
   only past access (`holes/labs/wm-contract/C385-recorded-hash-revalidation-census.md`).

**Why it recurs.** Each repair replaced the previous credential with a new one
and re-derived trust from possession. The question that catches it is not "is
this credential valid" but **"could the presenter have produced this without the
event occurring?"**

**Would prevent.** Bind the credential to the event, not to the actor:
authenticate each record over its own content plus the identity and ordinal of
what it attests; verify the world rather than the claim; and where a boundary
cannot be enforced in-process, **say so and move the boundary** rather than
defending a convention. For digest carriers, the checkable cure is narrower:
every digest used as acceptance evidence has a read-path control that changes
the named subject after recording and proves that recomputation rejects it. A
digest intentionally used only for provenance must be typed and rendered as
provenance, and must not participate in acceptance. The universal discovery
step is **auditable, not honestly lintable**: census every digest/signature
carrier, name its writer and every consumer, then classify it as
`verify-at-use`, `provenance`, or `neighbour-binding`. Syntax alone cannot tell
whether a recomputation covers the exact semantic subject. Once a carrier is
classified, its schema-specific verify-at-use control is executable and can
fail. **Prevention type: carrier census + schema-specific check**, with the
question “could the presenter have produced this without the event occurring?”
applied before closure.

**Discovery timing and freshness.** C385 was accurate at its pinned commit;
another lane closed one of its two live findings four minutes later. That did
not make the census wrong when written, but it made an unpinned “live” reading
misleading. C393 therefore records the census's full Git basis and exact subject
paths, and reports source movement as nonblocking `:possibly-stale` rather than
as falsity (`checks/repository-census-bases.edn`). The carrier census finds the
trust error; the census-basis check keeps that finding tied to the code it
actually examined.

**Related.** Class 1 (acceptance that cannot fail) is the general case; this is
its credential-shaped instance. Class 6a's "a lint that checks for a token rather
than the property" is the same error in a lint rather than in a fence.

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

C135 adds a fifth instance with only **one byte stream** but two interpretations.
Babashka's `clojure.edn/read-string` accepts `:r8/a/b`, while its source
`clojure.core/read-string` rejects it; the JVM source reader differs again.
Both runtimes' EDN readers agreed over 1,160 files / 1,930 top-level forms, so
the exposed duplication is the grammar selected at the two endpoints, not the
stored data (`holes/labs/wm-contract/C135-reader-portability-sweep.md:5–38`).
Five active Babashka persisted-file boundaries used bare source `read-string`
at that census (`C135-reader-portability-sweep.md:49–65`). C140's executable
scan corrected that to **12 call sites across six files**, adding the production
daily-scan frame reader (`holes/labs/wm-contract/C140-reader-portability-lint.md:7–22`).

**Found by.** Count definitions/usages by semantic name; ask what actually calls
each symbol; reject type-level matches as semantic bridges.

**Would prevent.** One authoritative carrier, or explicit conversion with laws and
named direction; namespace types by role rather than reuse a scalar alias. At a
persistence port, declare one data grammar/reader on both sides and exercise a
cross-runtime round trip over adversarial tokens.
**Prevention type: lint + review question.** How much semantic synonym detection
can be automated is uncertain; bare-reader detection is a mechanical corner,
not semantic-synonym detection.

## 8. Era-blind expectation

**Shape.** A schema is selected from a clock that does not identify the producer
contract. This includes applying today's schema to old records, but is sharper:
even a date-aware branch is unsound when multiple producer versions can occupy
the same time bucket.

**Instance.** C58 proved the two May 18 records lacking `:annotation-health` belong
to the 13-channel era; the third record at `2026-05-18T21:33:02Z` starts v0.10 and
has the field. The last old record and first new record are only about 39 minutes
apart (`holes/labs/wm-contract/C22-falsifier-invocations.md:140–155`;
`holes/labs/wm-contract/C58-r2-channel-eras.md:1–20`).

C125 found the stronger case. R8's filename-day discriminator classifies the
pinned 760/41-era population correctly because the corpus happens to contain a
gap, yet is unsound in principle: an old binary writing on July 14 or a new one
writing under another filename date is misclassified. In contrast, trace-schema
versions 15–19 all landed on **2026-08-31**, so their date cannot distinguish
them at all (`holes/labs/wm-contract/C125-era-discriminator-census.md:9–31`).

**Found by.** Sort records by time, inspect adjacent boundary records, and search
the producer history for the schema-changing commit rather than assuming a
day-sized era.

**Would prevent.** Persist a producer-issued contract/schema tag and dispatch by
that tag, with an explicit unversioned legacy arm. A clock is acceptable only as
pinned legacy evidence whose adjacent records locate the transition, never as a
general producer identity. Negative controls place two versions in the same time
bucket. The falsifier is a producer/consumer change that lands atomically or two
versions written within one clock bucket.
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
- The generated status report called a strict-lint classification count
  `conformant`, although the instrument was built specifically to replace
  hand-maintained status counts; the misnamed output is visible at
  `scripts/wm_status_report.py:145–153` and in its first recorded report at
  `holes/labs/wm-contract/C132-generated-status-report.md:19–24`. A generated
  value can still say two things when its field label names a different
  predicate than its source.

**Found by.** Parse and tally each representation independently; compare registry
jobs with live Agency state; never rely on the summary produced by the same record.

**Would prevent.** One authoritative current-state field plus append-only history;
checks reject a live/history contradiction and reconcile dispatch with completion.
For append-only amendment logs, the durable convention is **a machine-delimited
current index plus reciprocal pointers at every superseded claim**; prose history
alone is insufficient because it can be correct only when read end to end. C161
applies that convention to `p4ng/vetting/CLEANUP-QUEUE.md`, and
`checks/cleanup_queue_correction_index.clj` enforces its exhaustive index rule.
**Prevention type: lint + check.**

## 10. Evidence anchored by its producer

**Shape.** Evidence is internally consistent and may be fully revalidated, but
the party it is meant to constrain can also replace, rewind, delete, or rename
the complete authority that decides which valid chain is canonical. Hashes,
HMACs, atomic writes, and chain verification can prove consistency of the
presented history without proving that it is **the** history.

**C409 instance.** The Agency ledgers, bounded receipts, Git refs/reflogs,
timestamps, and local state are all mutable by the same `joe` authority that
writes or selects the evidence. This differs from class 6b: the digest may be
recomputed correctly and every link may verify; the missing property is an
independent canonical head. C409's authority census records the worked example
at `holes/labs/wm-contract/C409-append-only-external-authority-census.md`.

The root-owned systemd journal provides one limited asymmetry relative to
unprivileged artifact writers: after ingestion, `joe` cannot rewrite retained
journal files. It is nevertheless retention-bounded, unsealed, root-mutable,
and currently anchors none of the relevant artifacts. It raises the cost of
rewriting an observation; it is not permanent, independently verifiable proof.

**Found by.** For every claimed authority, enumerate the evidence writer, the
canonical-head selector, filesystem/service owner, deletion and rollback
capabilities, retention administrator, and verifier. Ask the decisive question:
**can the party writing the evidence also rewrite or choose the authority
against which it is checked?** Repeat this at every layer; moving from a file to
a hash chain or local Git ref does not change the authority if the principal is
unchanged.

**Would prevent.** First declare the boundary honestly. This needs a sibling to
`:boundary/type :declared-not-derived`, not an extension of it: derivation asks
whether a correspondence was computed, while authority asks who can rewrite
the canonical source. The appropriate vocabulary is an explicit authority
assessment such as `:authority/type :producer-controlled`,
`:externally-administered`, or `:independently-sealed`, accompanied by writer,
selector, retention, and rewrite capabilities. A real cure requires anchoring a
head outside the producer's rewrite authority and verifying it at use. Merely
declaring the limit prevents overclaiming but does not repair it. **Prevention
type: authority census + typed boundary declaration; external anchoring only by
separate design decision.**

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
| Challenge `blocked` as a claim | Deferred sites whose unblocking condition was never tested | Re-run or trace the named blocker; C126 found that 6 of 11 “blocked” sites were already settled implementations or exposed decisions, rather than one undifferentiated blocked population. |
| Trace consumers beyond the local site | Locally mechanical edits that alter belief, ranking, or safety downstream | Follow every changed value to selection/actuation before classifying the work. C127 found that 2 of 5 apparently implementation-only rows could change later rankings and stopped them. |
| Audit who controls the authority | Internally valid evidence whose canonical history is producer-selectable | Name writer, head selector, storage owner, retention administrator, and verifier; treat same-principal control as a declared limit, not independent evidence. |

These methods compose. The strongest recurring sequence today was: **census → pin
the population → semantic mutation → inspect the named consumer → repair → rerun
against the consumer-held artefact**. Promotion priority is deliberately left to
the owner.

## Prevention-type audit after implementation

This audit compares C74's speculative prevention types with the committed
apparatus now present. “Exists” means committed and callable, not merely drafted.

| Class | C74 prediction | What exists now | Verdict |
|---|---|---|---|
| 1. Acceptance cannot fail | check + lint | Semantic mutations in the individual witnesses; `checks/preemptive_acceptance_lint.clj`, its negative control, and the C84 workspace-gate entry (`checks/wm_workspace_gate.clj:57–60`). | **Held.** The generic lint catches nonzero-findings/zero-exit shape; semantic mutations still carry the claim-specific judgement. |
| 2. False declaration as hole | review question + possible Lean lint | Degenerate-model review/counterexample sweeps (`C70-hole-quantifier-sweep.md:1–12`); C134's committed category check rejects unlabelled or contradictory `sorry` claims (`checks/lean_sorry_category_check.clj:83–116`). No generic countermodel generator exists. | **Held, partially mechanised.** Labels prevent a false hole being presented as ordinary debt, but reading quantifiers remains review work. |
| 3. Wrong-object witness | review question + check | Direct-source proofs and source/generated equality controls for R2/R8/R9 (`C96-R2-pinned-discharge.md:1–12`; `C105-R8-pinned-discharge.md:1–18`); C137 requires a witnessed obligation's fixture and content pin (`checks/lean_sorry_category_check.clj:44–75`). | **Held.** Checks enforce named source/fixture identity after review identifies the right object; no generic entailment checker exists. |
| 4. Artefact boundary | lint + check | `checks/preemptive_artefact_boundary_lint.clj`, negative control, C84 gate wiring, TeX reachability and published-PDF checks (`p4ng/vetting/C45-TEX-REACHABILITY-2026-08-31.md:1–63`). | **Held.** Both static boundary lint and consumer-held artefact checks were built. |
| 5. Stale baseline | check + lint | `checks/preemptive_stale_baseline_lint.clj`, negative control, C84 wiring, and C35's snapshot/live split recorded in `C22-falsifier-invocations.md:98–138`. | **Held.** Exact reproduction and live invariants are separate instruments. |
| 6. Absence coerced | lint + check | `checks/preemptive_absence_coercion_lint.clj`, exact disposition coverage, typed-envelope controls, and C84 wiring. Current live finding count is 7. | **Held, not extinct.** The apparatus finds the remaining population; its seven repairs await operator decisions. |
| 7. Duplicate representation | lint + review question | Review found the four semantic duplications and C135's reader duplication. C140 then committed `checks/reader_portability_lint.bb`: its positive scan reports 12 bare persisted-file reads across six files, and two controls prove rejection plus a reason-bearing source-read exemption (`C140-reader-portability-lint.md:1–29`). No generic synonym lint exists. | **Held, with one mechanical corner automated.** The lint decides which reader a persisted-file call uses; it does not decide whether two πs, antecedents, or free energies mean the same thing. C74's judgement boundary was therefore not wrong. The cross-runtime round-trip and reader migrations remain unbuilt. |
| 8. Era-blind expectation | check + schema lint | `checks/preemptive_era_blind_lint.clj`, negative control, C84 wiring, plus record-carried producer/schema tags audited by C125 (`C125-era-discriminator-census.md:9–31`). | **Held.** The generic lint catches unqualified timestamped assertions; producer identity still needs a schema contract. |
| 9. Record says two things | lint + check | `checks/preemptive_record_conflict_lint.clj`, negative control, C84 wiring, obligation-ledger reconciliation, lane-registry checks, and C165's reciprocal correction-index check. | **Held.** Current/history conflicts, missing completion transitions, and unindexed amendments have executable consumers. |
| 10. Evidence anchored by its producer | not in C74 | C409's authority census establishes that every relevant current authority is producer-controlled; the root-owned journal is only externally administered, retention-bounded, and unused as an anchor. | **New judgement-scoped class.** Honest authority typing is specified; no external anchoring protocol exists or is implied. |

The former reserved tenth slot is now filled by C409. C135 remains inside class
7: duplication can reside in endpoint grammars even when the bytes are singular.
The catalogue therefore contains ten nonempty classes.
