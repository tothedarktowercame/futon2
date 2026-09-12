# Row 24 scoping — the full-scope Lean certificate checker

**Date:** 2026-09-12  
**Kind:** discovery; no checker, registry, ledger, or production change  
**Authority:** `WORK-REMAINING.md:888-896`  
**Negative-scope candidates today:** **17**

## 1. What exists

### 1.1 The September-1 run-to-Lean pattern

Row 25's precedent is U49/S5.  A deterministic producer transcribed the pinned
September 1 run routes plus the drawn/measured/retired edge tables into finite
Lean data, then `by decide` proved route conformance and a complete route census.
The construction and its limits are recorded in
`C502-u49-run-conformance-certificate.md:29-48,50-89`: four routes, 36 hops,
nine distinct hops, with exact source pins; the theorem establishes one pinned
route comparison, not prediction of the run, firing of every drawn edge, or
general system correctness.  The reusable pattern is:

1. freeze source bytes and revisions;
2. mechanically derive a strict finite certificate value;
3. generate Lean literals from that value, never hand-edit them;
4. decide a precisely reviewed predicate and a census theorem; and
5. elaborate plus `#print axioms`, retaining no `sorryAx`.

`SPEC-run-certificate-v1.md:142-176` names the same pattern and the intended
generated module.  The RE5 repetition demonstrates per-run generation and
source-map pin comparison (`C503-first-correlated-run.md:113-143`).

### 1.2 The certificate-v1 emitter lane already underway

The F11 lane owns `holes/labs/wm-contract/derive_certificate.bb`; this row must
consume or extend its reviewed product, not create a competing emitter.  At
HEAD it:

- requires exactly the seven checkpoint roles, a single cohort/attempt,
  contiguous sequence, monotone timestamps, and pinned whole-file bytes
  (`derive_certificate.bb:78-106`);
- binds construction cascade/wiring/fold-output, validator identities/results,
  selection/enaction, route and source hashes;
- resolves a realised equation row to exactly one checked declaration at the
  certificate revision; and
- enforces an exact six-entry negative scope (`derive_certificate.bb:14-40`).

The emitted fixture proves the current **data shape**, not full scope.  It
contains one realised equation binding (R2/`machineObservation`), one fixture
route, one selection/enaction match and an enriched construction chain
(`runs/certificate-v1-emitter-controls-2026-09-12/fixture-certificate.edn:1`).
Its `:lean-attestation` is `:suspended-vocabulary`.  Its seven checkpoint files
are a scratch/certificate-control corpus, not a qualifying production run.

The controls commission eleven refusals: incomplete genuine F11 deliverable,
incomplete checkpoint chain, source digest mismatch, removed checkpoint,
reordered checkpoint, deliverable digest mismatch, ungrounded divergence,
missing/ambiguous/wrong-revision declaration, and mandatory-scope mismatch.
They also accept one fully typed divergence and prove deterministic EDN output
(`runs/certificate-v1-emitter-controls-2026-09-12/controls.edn:1`).  This is
already the structural half of row 24/25.  It does **not** yet attest every node,
all equation witnesses, all record families or all connections, and its present
negative scope predates rows 13–23.

### 1.3 Witness registry and declaration bytes

`checks/witness-registry.edn:1-4` is generated from owning fragments.  Its
admissions header records 34 `:verified-binding` claims across eleven node
families.  There are 33 `:supports` claims and one `:refutes` claim:
`R16-machineAction-live-selector-divergence-20260912-v1`.  Scopes include
reference-model examples, two carried/bootstrapped trace rows, a machinery-only
depth capture, historical S4 consecutive records, production float-carried
pins, an F-absent posterior branch, and a single production selector-divergence
record.  These are maximum honest claims.  “Registry admission” therefore
cannot mean “this node is positively complete for the qualifying run.”

The registry dependency header pins witness sources, subject Lean files,
receipts, toolchain and the equation registry.  The subject-file freeze policy
keeps admitted declaration bytes stable.  The full checker can reuse this
identity shape: claim id + polarity + scope + declaration full name + subject
SHA-256 + verification/review receipts.  It must still prove that a claim's
scope applies to the candidate run; mere registry membership is insufficient.

### 1.4 Signature, conformance and connection ledgers

The signature audit declares row 24's checker itself to be the breach detector
and mandatory-negative-scope emptiness its final assertion; row 25 supplies
tamper tests (`runs/signature-audit.edn:57-61`).  Its checker validates counts,
ids and vocabulary only (`runs/check_signature_audit.bb:14-36`), and its own
review warns that it does not establish detector existence, wiring,
commissioning or independence (`runs/signature-audit.edn:91-100`).

Route conformance has two authorities that must not be conflated:
`run4_route_conformance.clj` classifies a concrete route against the pinned
control map, while `control-map-edges.edn` is the drawn/measured/retired edge
ledger.  The historical U49 review records a drawn-set interpretation mismatch
between two checkers (`C502-u49-run-conformance-certificate.md:91-107`), so full
scope must pin one ruled classifier rather than take “green” by name.

Row 21's reviewed disposition is the current connection worklist:
`TN-row21-discovery-2026-09-12.md:62-89` classifies twelve code-but-undrawn
connections as eight ordinary, three conditional and one retired-source/off.
Those dispositions are not yet equivalent to a completed canonical diagram;
row 22 still owns mandatory firing versus aspirational marking.

## 2. Full-scope obligations against today's state

### A. All records present and consistent

The checker needs a closed per-run record manifest, not “all EDN nearby.”  The
minimum families and joins are:

| Family | Presence | Required consistency |
|---|---|---|
| Full-loop checkpoints | exactly roles 001–007 | one cohort/attempt, contiguous event sequence, monotone times, terminal outcome agrees with adjudication/build, exact bytes pinned |
| WM trace records | every tick/run id cited by the attempt | schema-supported; run, cohort-attempt and trace path identities agree; decision fields survive strip; no inferred trace by temporal proximity |
| Tick-run records | one for every invoked tick/terminal path in scope | run id, trace path/schema, selector seam, repository/program pins and terminal context agree with trace and checkpoint |
| Close/cohort records | one terminal close per attempt | outcome entity, state-at-close/belief source and cross-ledger identities agree; disposition agrees with terminal checkpoint; append-only sequence has no duplicate terminal |
| Dispatch/job records | every dispatched work unit | commission, job, execution receipt and terminal delivery/refusal join by immutable ids; no missing execution evidence |
| Park/continuation records | every park or resumed dependency | park id, dependency job, deadline/wake and terminal continuation are single and chronological; absence is typed |
| Review/admission records | every load-bearing witness claim | author/reviewer jobs, receipt hashes, chronology and exact reviewed artifact join; row 19 independence refusal passes |

The current v1 emitter covers only the first family plus a separate route record.
Run-era ledger findings already explain why a validation catalogue evaluated
later cannot be attributed to an earlier run: it lacks as-of-run identity
(`run-era-ledger.edn:188-216,244-272`).  Full scope therefore requires the
validation and assurance snapshots to be records *of this run*, not current
queries pasted into its certificate.

### B. Chosen equals enacted, or divergence is typed

The live construction record now carries the row-23
`:selection-enaction` sum.  The checker consumes it directly:

- `:match` requires exact selected/enacted equality;
- `:typed-divergence` requires both actions, class, grounds and evidence source;
- absent, malformed, or “different” without grounds refuses.

Lean can decide the shape/equality.  It cannot decide that free-text grounds
make the divergence acceptable.  Acceptance of divergence classes must be a
pinned ruling table; an unknown class is a refusal.  Row 23's first natural
post-reload production instances are still required for the qualifying run.

### C. Every node's validation state

This is the hard unresolved design choice.  Three readings are possible:

1. **Attest typed state as-is.** For every node, emit a closed sum such as
   `supported-at-run`, `supported-at-other-pin`, `refuted-at-pin`,
   `typed-absence`, `mechanism-only`, or `unvalidated`, with claim ids and
   scopes.  “Full” means census-complete and honest, not all-positive.
2. **Require positive closure.** A qualifying certificate passes only if every
   load-bearing node has a supporting witness whose scope includes this exact
   run; refutes/partials/absences fail.  This matches “nothing load-bearing
   remains” but makes today un-certifiable, correctly.
3. **Two-level certificate.** Always attest the complete typed census, then a
   separate `QualifyingRun` predicate requires positive closure except an exact
   set Joe ruled non-load-bearing in writing.  This preserves facts without
   converting them into acceptance.

No choice is made here.  Option 3 exposes the fewest semantic traps, but it
still needs Joe's ruling.  In every option, R11/R15's seven `absent` census
cells, machinery-only depth, the F-absent posterior, historical fixtures and
the R16 `:refutes` claim retain their types.  They cannot be relabelled passes.
Under the row's mandatory-negative-scope rule, any exception to positive
closure must cite Joe's written rule-out by exact bytes; reviewer prose or a
certificate author's judgment is insufficient.

### D. Equation claims tied to checked declarations at pinned bytes

For every equation-bearing node applicable to the run, join:

`run evidence -> admitted claim id/scope/polarity -> exact aif-equations row ->
full Lean declaration identity -> frozen subject bytes -> verification and
independent-review receipts -> toolchain/checker bytes`.

The v1 emitter already refuses missing, ambiguous and wrong-revision
declarations.  Full scope adds claim-scope applicability and witness receipt
joins.  Declaration existence is not semantic correspondence; only the
row-specific witness theorem licenses that statement
(`SPEC-run-certificate-v1.md:107-115,170-176`).

### E. Connections attested

The certificate must separately census:

- every hop actually fired, classified by a pinned row-21/route authority;
- every mandatory row-22 connection, witnessed as fired in this run;
- every conditional edge, with its activation condition and whether that
  condition held;
- every aspirational/non-load-bearing edge, backed by Joe's written ruling; and
- the R6→R16 correspondence disposition, which is more than route membership.

Unfired is not disproved, route-measured is not originally drawn, and an
annotated retired source is not a live connection.

## 3. Mandatory negative-scope inventory at HEAD

These **17 items** cannot be positively attested by a certificate today.  Each
must either close before row 28 or be explicitly ruled non-load-bearing by Joe.
Existing negative-scope prose is not automatically such a ruling.

| # | Missing positive assertion | Owner/current type | Closure or ruling needed |
|---:|---|---|---|
| 1 | Live R1/R2 observation-state input actually feeds R17 accumulation | row 13 | live arrow capture/witness; existing row-12 recurrence is not wiring |
| 2 | Measured A is a licensed 7×12 model component | row 14 packets 1b/2 | categorical-status authority, accrued data, pinned estimator/model assembly |
| 3 | Machine-Q option is live in the judge, not merely callable/retained | row 14 packet 6/integration | live wiring and production witness |
| 4 | `machineBeliefUpdate` matches a real update | row 15 | retain events plus A/B/model revision and build the witness |
| 5 | Internal machineAction branches are positively measured | row 15 | proof from captured epsilon/details inputs |
| 6 | Reason-bearing live selector positively matches its Lean extension | row 15 packet 4c/4d | Lean extension plus first full-envelope live witness; current claim is `:refutes` |
| 7 | R6 posterior with nonzero F_pi | row 16 | a real F-present production pin and witness; current admission is F-absent only |
| 8 | R5 predictive outcome risk correspondence | row 16 batch A | complete real Q/C pair plus reviewed categorical-risk bridge |
| 9 | R5 expected-free-energy composition correspondence | row 16 batch A | full scoring-input capture or reviewed production composer |
| 10 | R5 ambiguity implements the declared categorical law | row 16 capture family | bridge/divergence decision; production currently uses Gaussian entropy |
| 11 | Retired R8 free-energy has a production object | row 16/J2 | Joe already ruled the producer deleted; confirm this exact exclusion is permitted negative scope, never a positive claim |
| 12 | R17 model reduction runs on real capability substrate data | row 16 | capability-typed substrate retention, non-vacuous envelope, composite subject and witness |
| 13 | R10 assurance boundary has a real production caller/record | row 17 | integration through `run-scheduled-dispatch!`; mechanism-only evidence is insufficient |
| 14 | R20→R14 interoceptive commitment law and production link | row 18 | Joe chooses factor law/count/floor, then snapshot, composition, joint record and witness |
| 15 | R9 no-self-certification is anchored and executable on real reviews | row 19 | Joe chooses genesis authority and commission-digest retention closes the real join |
| 16 | R11 and R15 lifecycle validation is positive | row 20 | currently seven typed absences each, 0/7; build/integrate or rule non-load-bearing |
| 17 | All canonical connections are disposed and every mandatory one fires | rows 21–23 | finish diagram packets, row-22 mandatory/aspirational rulings, and retain natural post-reload R6→R16 records |

The six v1 negative entries (`not-r1-r17`, `not-r2-r17`,
`not-fundamentals-inhabitants`, `not-above-witnessed-rung`, `not-task-success`,
`not-mission-closure`) are a useful old scope vocabulary
(`derive_certificate.bb:14-40`), but are not an authorization to carry these 17
load-bearing gaps into row 28.  In particular “not above witnessed rung” is too
broad: it would hide which node is partial, refuted, absent, or simply unrelated
to the candidate run.

## 4. Smallest packet split

1. **State algebra and Joe decision.** In a new Lean module define node-state,
   connection-state and selection/enaction sums plus `FullAttestation` and a
   separate candidate `QualifyingRun` predicate.  Obtain Joe's ruling on the
   three readings in §2C and on the exact negative-scope enum.
2. **Record-manifest checker.** Extend the F11-owned emitter with the record
   families/identity joins in §2A.  Do not fork `derive_certificate.bb`.
   Commission missing, duplicate, cross-run, reordered and stale-byte controls.
3. **Registry/witness join.** Consume all 34 admissions with polarity and scope;
   refuse a claim whose subject/review/toolchain pin moved or whose scope does
   not cover the run.  Controls include promoting the R16 refutation and the
   machinery-depth claim to positive run scope; both must fail.
4. **Equation/declaration population.** Generalize the emitter's one-row join to
   the exact applicable population and import the frozen witness modules.
   Commission missing/duplicate/wrong-revision/wrong-subject-byte controls.
5. **Connection census.** Consume the completed row-21/22 disposition artifact,
   route records and R6→R16 field.  Commission unknown hop, unfired mandatory
   edge, conditional-without-premise, and unruled aspirational controls.
6. **Lean checker.** Generate the certificate term plus census and full-scope
   theorem; elaborate and axiom-check.  Hash and cross-project EDN/JSON/Lean
   from one intermediate value.
7. **Row-25 tamper battery.** Mutate every certificate field/class at least once
   and require emitter or Lean failure; retain deterministic regeneration and
   mutation receipts.  This is the exhaustive transport detector.

Row 24 has its **own** breach detector: the full predicate must reject missing
records, unjoined claims, unruled negative scope and unattested connections.
Row 25's tamper tests commission that detector across serialized boundaries;
they do not replace its proposition.  The signature audit says exactly this at
`runs/signature-audit.edn:57-61`.

## 5. Pins and conclusion

Inspected futon2 tree: `4651c45a4586f039b7c355a09dcfb38bcfe385b1`
(the TN commit's parent).  Content pins:

- `derive_certificate.bb`: SHA-256
  `55cc537828e3a4890e268cb07e0590f3ccc1b4feeb7b2acb622baf69268b1c47`;
- `checks/witness-registry.edn`: SHA-256
  `a9524b1982f76a6e9db83f8e8085e45e99ffdc058b58a6ed1cc4b47f1a96b502`;
- `runs/signature-audit.edn`: SHA-256
  `bd129ac523524ede893e75984445856f0398a58f9475123f75fd9ed61054d74b`;
- `aif-equations.edn`: SHA-256
  `15aa434117f16c3c6262bb0b847e436296ec6e1954dd9061ca80c4505bbc45f8`;
- p4ng `control-map-edges.edn`: SHA-256
  `161d0abffd21551078ac2d7a87427e6cacafbfca0695c09a496260be21fafdec`.

The buildable core is clear: extend the existing pure emitter, generate one
finite Lean certificate, and make typed partials visible.  The unresolved
authority question is equally clear: whether a complete typed census can
qualify while any of the 17 items is non-positive.  Row 24 forbids the checker
from answering that question on Joe's behalf.
