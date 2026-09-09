# E-C-realization — the interactive lane for making C real

Opened 2026-09-09 by claude-1 under Joe's ruling
(holes/labs/wm-contract/RULINGS-walkthrough-2026-09-09.md Items 18c-18d):
"I am not happy to kick the can without real work to make C real... if we are
at the end of what can usefully be done in an automated loop, we should move
to an interactive session (new excursion)" and "restarting should not be done
looking for coverage in an automated loop, that should become an interactive
session."

This excursion is the named owner of the C-hole's DEFERRAL UNDER ORGANIZED
DISCOVERY marker (mathlib4 DarkTower/WarMachine/Holes.lean:152, commit
453054fbef): the deferral is honest exactly as long as this lane is moving.

## Current lifecycle position — 2026-09-09 sitting

**Later in the sitting:** Joe explicitly authorised beginning a modular,
versioned C implementation, with Lean type correctness and WM interoperability
as the present criteria; speculative duality is deferred to evidence from use.
This supersedes the earlier stop before building, preserved below as history.
The first implementation and his verbatim direction are recorded in
[INSTANTIATE C module v1](labs/wm-contract/INSTANTIATE-C-module-v1-2026-09-09.md).
This is an initial component with a callable WM adapter, not closure of the
production observation model or of the full excursion.


Joe recognises the historical and current-paper pilots as the **DERIVE** step:
"we've derived the shape a solution should take". His direction is to write
that shape and an upgrade path, then ARGUE it using the Futon3 pattern library;
"at this point, we don't need to go off and build it".

The full ruling is preserved verbatim in
[DERIVE and ARGUE](labs/wm-contract/DERIVE-ARGUE-C-realization-2026-09-09.md).
That document specifies the preference registry, assessment and comparison
contracts, lifecycle/process criteria, recursive proposals, invariants, and
upgrade interfaces for serendipity, other outcome types, multiple levels and
predictive integration. ARGUE is now written with library cross-references and
pattern selection records; its acceptance and VERIFY are not asserted. The
first implementation target is a reproducible diagnostic; the production
observation-model obligation in §1b and numerical preference decisions remain
explicit. No build was undertaken under that earlier instruction.

## Entry state (what the automated lane finished, 2026-09-08/09)

- Channel C computes live: 13 ranges, KL risk in the fold
  (src/futon2/aif/preferences.clj, E-C-vector-live.md).
- Ruled outcome C seeded (src/futon2/aif/ruled_outcome_c.clj). CORRECTED
  2026-09-09 (codex-12 briefing check): the fold is IMPLEMENTED and declared
  :folded? true at the scorer-side sites (:62, :70; futon2 1480fb20 "F10: fold
  disposition risk into EFE"), with two sites still :folded? false (:77, :84);
  what remains is that the PRODUCTION CALLER does not enable or supply the
  fold's inputs (the F10 rider). The earlier ":folded? false at
  checks/ruled_outcome_c.clj:43-62" line was stale on both the flag and the
  path.
- Disposition kernel P(d|o) fitted from the cohort ledger — 1 of 12 outcome
  kinds with support (3/3 :grounded-change), so disposition-risk computes but
  is constant across policies (checks/disposition_kernel.clj; C591 §1-2).
- The calculation proposal with tests t1-t6 and slices 1-4
  (holes/labs/wm-contract/C591-C-as-calculation-proposal.md).
- The paper section published (p4ng sec-c-vector.tex, in futon-2026 Part IV).
- Cascade Live pinned: apex thesis + arrows + candidate C_tau bands
  (holes/labs/wm-contract/runs/U88-cascade-live/U88-cascade-live-pin.md).

## The two strands (both interactive, Joe in the loop)

1. QUALITATIVE → QUANTITATIVE (U88 slice 2): with the pinned apex clusters
   A-D as C_tau bands, assign or explicitly defer a weight per band with
   provenance; rule the cluster-C missing carrier ("feedback reaches every
   participant" has no coordinate in channel C or seeded-c — explicit zero);
   rule whether live delta-g is admissible arrow-grain preference evidence.
   Then the rung-3 axes (solvability / monetary benefit / societal benefit)
   as declared coordinates over problems.

1b. THE OBSERVATION MODEL BETWEEN SCORER AND KERNEL (Astra review,
   TN-astra-wmreview.md:31, verified in code 2026-09-09): the fitted kernel's
   observation domain is the checkpoint trajectory (the ledger retains no
   channel-valued observation vector -- checks/disposition_kernel.clj:20-27),
   while the scorer supplies channel-valued predicted means to
   disposition-risk (src/futon2/aif/efe.clj:701-703). Constant-kernel
   coverage (1/12) masks the gap today; a justified model connecting channel
   observations to kernel observations is a distinct obligation of this
   excursion, and per the review it must NOT be closed by substituting
   checkpoint trajectories for channel observations merely to make the
   connection executable. This also shapes strand 2: what the restarted
   attempts RECORD (channel observations alongside checkpoints, or a declared
   mapping) is part of the attempt design.

2. COHORT RESTART AS DESIGNED WORK (Item 18d): the kernel's 1/12 coverage is
   evidence FOR restarting the attempt cohort, not a license to farm (o, d)
   pairs in a loop. Interactively: which attempts, what framing, what counts
   as a closed disposition, who reviews — then the attempts run wherever they
   naturally run, and the kernel refits from whatever records honestly land.

## Ground rules carried in

- Discover, not invent (Item 12): masses come with provenance from the
  recorded landscape; only a ruling moves a pin; explicit zeros, no smoothing.
- Route by standing principle: only genuine preference/priority/scope
  questions take Joe's time inside the sessions.
- Session products land as dated files in holes/labs/wm-contract/ with
  worklist rows minted per the usual gates; Lean edits cite the ruling that
  released them.

## Log

- 2026-09-09: excursion opened; C-marker flipped to deferral-under-organized-
  discovery citing this file; U88 slice 1 (the pin) complete; F11 apply and
  U87 census dispatched to Codex in the automated lane.
- 2026-09-09 (sitting, first product): Joe directed a MAP before design —
  assemble the partial C calculations with their input suppliers and
  consumers. Recorded with his verbatim words at
  holes/labs/wm-contract/MAP-C-realization-2026-09-09.md (futon2 810574cb,
  codex-12 with Joe; source inspection only). Beyond the §1b checkpoint/
  channel-means gap it surveys three more seams: the mean-only
  disposition-risk call does not implement uncertainty marginalization; goal
  KL enters the controller augmentation while mission C stays post-selection
  readback; the Bernoulli utility clamps endpoints where disposition KL
  preserves zeros. Logged as sitting EVIDENCE — the map's acceptance and any
  design that follows remain Joe's.
- 2026-09-09 (sitting, second product): Joe's process framing, verbatim at
  holes/labs/wm-contract/SESSION-C-tau-process-framing-2026-09-09.md (futon2
  b0a55be8, codex-12 with Joe). Core moves: C may not be a vector; C_tau and
  effective progress toward high-level problem resolution join the outcome
  preferences (the final-outcome and process views are hard to fully
  separate); the empirical start is Cascade Live; the problem breakdown
  connects to the library's authored how/why links, patterns-as-production-
  rules, and satisfaction reasoning (SAT/prover as an initial framing) — a
  different calculational flow over the same objects G runs on. No weights,
  no solver design, no fold enabling, no MAP acceptance. Evidence, not
  design approval.
- 2026-09-09 (sitting, third product — the first LOCAL CALCULATION): Joe
  accepts the family-of-preference-distributions terminology, names growing
  personal/organizational capability as a preference, and asks for one local
  calculation mined qualitatively from historical missions/capabilities
  (verbatim at holes/labs/wm-contract/SESSION-C-local-achievement-2026-09-09.md,
  futon2 bbfb5921). The calculation
  (runs/C-realization-first-flights/local_calculation.bb + result.edn):
  M-first-flights Phase A — cluster A's own first member, records carry
  derivations — replays a SHA-pinned historical flight witness through the
  existing verifier; the ground-removed counterfactual has IDENTICAL
  absolute error (0.03941189005722734) but flips the calibration-
  admissibility mask out via F1, so the satisfaction condition
  distinguishes what the numeric error cannot. :preference-masses
  :not-assigned and :probability-of-future-success :not-estimated are in the
  artifact itself. Reviewed by claude-1 (own runs from the documented cwd:
  exit 0, two runs byte-identical, output equals committed result.edn, pins
  hold). Evidence toward slice 2; no masses assigned.
- 2026-09-09 (sitting, fourth product — family of local calculations +
  lifecycle criteria): Joe asked to repeat the achievement analysis across
  clusters and derive intermediate criteria from futon4 mission-lifecycle
  (verbatim at holes/labs/wm-contract/SESSION-C-family-and-lifecycle-2026-09-09.md,
  futon2 740f2c28; artifacts runs/C-realization-family/). Four probes: (A)
  links the first-flights replay; (B) nonempty prerequisite satisfaction on
  the historical capability graph for the completed self-representing-stack,
  with the evidence-persistence counterfactual flipping applicability
  true->false; (C) the cluster-C carrier probe — mission_shapes/
  compute-prediction-divergence matches criterion text by SUBSTRING
  CONTAINMENT (futon3c src/futon3c/peripheral/mission_shapes.clj:270-292,
  verified by claude-1), so 'feedback delivered' and 'feedback delivered:
  false' both score divergence 0 — the explicit zero for 'feedback reaches
  every participant' now has a demonstrated mechanism; (D) EIG reduction
  fixtures recomputed (ln 2 / 0, incoherent posterior refused), explicitly
  partial arithmetic. Eight lifecycle exits pinned verbatim; two axes
  proposed for discussion (sought capability/outcome; intermediate lifecycle
  requirements). :cluster-completion :not-inferred in the artifact. Reviewed
  by claude-1 (own runs from futon3c: exit 0, byte-identical, matches
  committed result.edn). Evidence, not cluster completion or preference
  approval.
- 2026-09-09 (sitting, fifth product — current-paper preferences + working
  diagnostic): Joe asked for current preferences mined from the PLoP/Futon
  papers with a small registry and a working diagnostic, toward recursive
  current work (verbatim at
  holes/labs/wm-contract/SESSION-C-current-paper-diagnostic-2026-09-09.md,
  futon2 befdd98f; artifacts runs/C-current-paper-diagnostic/). Five
  source-grounded entries (claim-warrant, inspectable-reasons,
  semantic-satisfaction, external-usefulness, reusable-patterns), each a
  SYMBOLIC soft-binary family (p_i, 1-p_i) with 1/2 < p_i < 1 — masses
  symbolic, no numbers ruled. The diagnostic's one analyst annotation
  verified by claude-1 against the f036a91 pin: sec-recapitulation.tex:16
  says 'the boundary Part III measured' while :10 and
  sec-evaluation-outline.tex:3 say outline-only, 160 acceptance cells unrun
  — a one-sentence producer correction would flip the claim-warrant
  coordinate. Unknowns carry measurement status, not mass. Reviewed by
  claude-1 (runs from two cwds byte-identical to committed result; drift
  reporter correctly names the concurrently-edited paper files). Evidence;
  no ruled masses, no paper edit from the sitting.
- 2026-09-09 (sitting, sixth product — DERIVE written, ARGUE made): Joe ruled
  the pilots constitute DERIVE and directed the lifecycle-position section
  above; design + argument at
  holes/labs/wm-contract/DERIVE-ARGUE-C-realization-2026-09-09.md (futon2
  b75cd3bf, documentation-only; source digests in the sibling JSON, 11/11
  verified by claude-1). Reviewed by claude-1: the calculation contract's
  algebra and licensed/unlicensed block check out; the MAP's endpoint-clamp
  seam is now an upgrade constraint (hard support must refuse, never clamp);
  the ARGUE's @why lines are applications of recorded rulings, not library
  declarations. ARGUE acceptance and VERIFY are not claimed; no build, per
  Joe's explicit instruction.
- 2026-09-09 (sitting, seventh product — preferences as institutional
  constraints, discussion evidence): Joe proposes preferences-as-constraints
  (from conversation with Rob and Charlie), the peripheral execution
  envelope generalized to the hybrid LLM+code case, patterns as
  Ostrom-style institutions with action arenas, and an IAD+EFE reading —
  asking whether it is compatible with the DERIVE design (verbatim at
  holes/labs/wm-contract/SESSION-C-institutional-constraints-2026-09-09.md,
  futon2 21b1c7d7, note-only). codex-12's refinement, endorsed by claude-1's
  review: compatible, because the constraints view lands in computational
  locations the design already separates — only preferred-outcome/manner
  rows go in C_i or C_i,tau; prohibitions go to the admissibility relation
  (where the peripheral envelope already lives), obligations to trace
  monitors, information/participation to observation access and roles,
  sanctions to the transition model, amendment to a separate authority
  process — with one preference registry justifying entries across
  locations. Declared rule, enforcement mechanism, observed compliance and
  underlying preference are recorded separately (the DERIVE model-vs-subject
  invariant applied to norms); hard-zero C is not actuator enforcement.
  Lineage verified: M-G-over-cascades.md:66 (Joe's 2026-06-23 IAD flag) and
  M-aif4iad.md; bounded Ostrom primary-source check with access date.
  InstitutionSpec/RuleSpec proposed, NOT adopted; no build; not a ruling.
- 2026-09-09 (sitting, eighth product — pattern/institution duality,
  discussion evidence): Joe asks how institutions and design patterns differ
  and relate — dual? co-production? interpreter? — referencing
  M-formal-patterns (verbatim at
  holes/labs/wm-contract/SESSION-C-pattern-institution-duality-2026-09-09.md,
  futon2 78fa6e00, note-only). The note separates three easily-fused pairs:
  living pattern vs its written production representation; institution vs
  its executing actors/program; cascade data vs its interpretation.
  Proposes step(I,K,temperament,state,observations) with evidence-return
  pairing — no duality or coinduction asserted. Two checked corrections:
  LA1c's 'nothing executes a policy-grain rule' is historical — the shared
  firing loop at play/policy grain exists (futon3
  checks/playout_snatch.clj:200-274, pattern-policy), verified by claude-1
  against sites known from the F11 chain; and M-formal-patterns' freeze
  (futon5 holes/M-formal-patterns.md:95, FROZEN 2026-08-04) is respected —
  no slice, and its unverified modelling/G-bridge claims are NOT imported
  (satisfying every signed bond is distinct from a frustrated system merely
  having a minimum). LA1c's institutional consequence carried forward:
  Snatch treatments can change the observation space itself, so an
  institutional interpreter needs declared observation domains and
  cross-domain mappings — the §1b obligation's shape at another site. No
  adoption; not a ruling.
- 2026-09-09 (sitting, ninth product — INSTANTIATE C module v1, REVIEWED):
  under Joe's in-session build authorization (verbatim in
  holes/labs/wm-contract/INSTANTIATE-C-module-v1-2026-09-09.md), futon2
  eb8da10a + mathlib4 b5870b7384. Clojure preference_module.clj: exact
  rational tagged distributions, symbolic families with explicit
  instantiation, KL that REFUSES on positive prediction at zero preference
  (the DERIVE refuse-don't-clamp constraint, verified in code by claude-1),
  versioned assessment, partial comparisons, one-entry local-risk ranking,
  candidate/accepted mode; current-paper profile migrated with provenance.
  efe.clj integration strictly OPT-IN via :preference-module opts — separate
  diagnostic key and rank-local-preference-actions, no aggregate G, no
  production default. Lean LocalPreferenceModule.lean: ExactTable
  rational->canonical PreferenceDistribution constructor + parameterized
  binary family with zero/positivity proofs — a representation proof, NOT
  end-to-end refinement and NOT closing §1b. claude-1 review by own runs:
  43 tests/164 assertions/0 failures; replay byte-identical twice and equal
  to committed result.edn; lake env lean exit 0, zero sorry. Synthetic Q
  inputs are labelled synthetic in the artifacts. v1 is a component with a
  callable WM adapter; production Q calibration, PreferenceFamily/time, and
  the observation model remain open.
- 2026-09-09 (sitting, tenth product — the Item-19 delegations, REVIEWED):
  the 19a check-off (CHECKOFF-C-family-19a-2026-09-09.md) finds the family
  ruling AGREES with the session products, with one actionable flag: the
  :c-grain entry's older :statement/:not-claimed prose still read "proposal
  awaiting Joe" against the new :status :decided — reconciled by claude-1
  with a dated :reconciliation-2026-09-09 addendum (prose retained verbatim
  per the never-silent rule). The 19b draft
  (DRAFT-realized-outcome-recording-19b-2026-09-09.md) extends
  :wm/realized-outcome-v1 with a marked :wm/realized-recording-v1 contract:
  PAIRED channel/checkpoint capture (the §1b answer at the recording level
  — both domains recorded, no bridge asserted), four-status observation
  wrappers keeping absence / checked-empty / observed-zero distinct,
  versioned preference readings, consumer admission, legacy records left
  unmarked. ADOPTION IS PENDING — its own reviewed step before RUN4; §1b
  stays open. Source pins C-19-source-pins-2026-09-09.json verified 18/18
  by claude-1 against the recorded capture revision (3b38f438). Products
  landed inside claude-1's e4a4ade5 commit (shared-index capture,
  contents verified matching codex-12's intent).
- 2026-09-09 (eleventh product — the SEPARATION ruling, codex-17 seat,
  REVIEWED): Joe ruled the FoldC representation question in the codex-17
  session (verbatim at
  holes/labs/wm-contract/SESSION-FoldC-separation-2026-09-09.md, futon2
  78e1482a): preference distributions and scalar KL risk STAY SEPARATE —
  "There are different things... that's basically part of the definition" —
  implementation delegated to codex-17; Markov categories recorded as a
  direction to investigate for probability/prediction, not an asserted
  representation. First implementation slice mathlib4 b76719cd23
  (PreferenceRiskSeparation.lean): constant probability conditional with
  finite predictive marginalisation (constant_absorbs_prediction), a
  separately-typed RiskContribution, scalarKL with preferred-zero admission
  (preferred_zero_refuses — the refuse-don't-clamp constraint carried into
  the separated type), and grounded_seed_risk = log 2 anchoring the
  measured value; docstring disclaims a Stoch instance and the §1b bridge.
  The runtime adapter also landed (futon2 02b317f5:
  futon2.aif.disposition-risk/constant-checkpoint-kernel — typed refusals
  on every failure mode, ln 2 reproduced end-to-end through the A3
  pass-through). claude-1 review by own runs: adapter tests 6/46/0; lake
  build 2706 jobs green, axiom prints clean. FoldC witness reconciliation
  under the ruled separation + find retirement/re-pin remain outstanding.
- 2026-09-09 (twelfth product — FoldC ERA distinction, codex-17 seat,
  REVIEWED): Joe's follow-up ruling verbatim in the session file ("if you
  can reconcile... great. Otherwise, we could mark it as a new era and just
  point out that they're not directly comparable... worth looking at least
  at an analogy"). The era-comparison report
  (runs/FOLDC-era-comparison-2026-09-09.md, futon2 a55de165) delivers the
  analogy table and the claim boundary: the old witness's 3/8/7 are
  arbitrary fold-ALGEBRA illustration values (order-sensitivity is what
  they certify), not seed probabilities or KL; the new grounded ln 2 is
  proved arithmetic, not yet a twelve-outcome scalarKL evaluation over the
  actual seed. Old era-1 receipt stays valid at its stated scope without
  claiming runtime correspondence. Successor certificate + two-axis
  witness healing sequenced by claude-1 in review.
- 2026-09-09 (thirteenth product — the era-2 CERTIFICATE, commissioned by
  Joe, REVIEWED): mathlib4 2af52ef97e (PreferenceRiskWitness: the concrete
  twelve-outcome scalarKL theorem over the actual seed — same_twelve_support,
  abstained_refuses exercising the named-zero refusal) + futon2 6356087c
  (checks/preference_risk_receipt.clj generating Lean equality proofs
  binding all 24 runtime preference/prediction masses to the witness; a
  changed normalized seed fails the proofs; certificate sha 9899d633...).
  Explicitly finite execution correspondence, NOT universal Clojure
  refinement or a live run. claude-1 review by own runs: lake build 2708
  green; receipt generator byte-identical twice at -cp .:src with
  regenerated artifacts equal to committed; tests 2/4/0. The two-axis
  fold_c_witness healing (risk-contribution axis per the separation ruling)
  authorized next; then find retirement + re-pin.
- 2026-09-09 (fourteenth product — the naturalistic organise task SELECTED,
  codex-17 seat): Joe picks dogfooding — build an e-pre-go-live cascade
  from today's own pre-go-live work ("I always love to dog feed these
  systems... for future, the outer loop should pick the task for us" —
  verbatim in runs/E-pre-go-live-experiment-2026-09-09.md, futon2
  f9433625). The brief supersedes the budget-vs-floor basis question (the
  exemplar builds a NEW task-local cascade; no claim over the old
  outputs) and sets the constraints: real command execution via the shared
  firing loop, frozen task/membership/basis between arms, reviewed
  precedence intervention only, unexercised guards reported honestly, no
  fabricated authored edges, no prose-@why-as-ids. Architecture
  requirement recorded: FUTURE task selection belongs to the WM outer loop
  (carried into the RUN4 checklist's orchestration section, fcddb659).
  Independent library-lane interpretation review routed to codex-10
  (author-not-reviewer per the F12 ownership map). This is disposition
  (f)'s naturalistic-exemplar proviso in motion — organise's staged
  closure path.
- 2026-09-09 (fifteenth product — independent interpretation review,
  codex-10): futon3 b63a4b6. Four of five apparatus interpretations AGREE
  (authority, evidence-disposition, replay, observed-running-as-supporting);
  pin-moves-with-the-population DISAGREES — the proposed multi-commit
  allowance conflates the THEN's two timing obligations (same-commit
  population re-pins vs same-slice source re-attestation) and would weaken
  the same-commit requirement if applied to re-pins. All five fingerprints,
  15 clause spans, 13 pointers verified. Three prose-derived dangling
  tokens fenced (S1/S2, the T-apm note path, a1-vs-a2/a3) — lexical shape,
  not authored relations. Revision routed to codex-17 (author fixes against
  the reviewer's note; no ruling needed); execution of the first slice
  proceeds after the dated addendum + recorded fence.
- 2026-09-09 (sixteenth product — re-review AGREE, codex-10): futon3
  a376e40. The corrected pin interpretation preserves same-commit
  population re-pins vs same-slice source re-attestation with explicit
  ownership, mixed-field refusal and dependent-publication stops; the
  prose-token fence is recorded. The reviewer's DISAGREE is cleared;
  manifest-review prerequisites from the addendum remain ahead of any
  O4/experiment claim. codex-17 cleared to execute the first
  e-pre-go-live slice under the brief's constraints.
- 2026-09-09 (seventeenth — find CLOSED and published): the retirement +
  re-pin chain landed (mathlib4 bf79f988b3 marker: mkWitnessedClosed citing
  Item 20/23 + both slices, existence-not-correspondence boundary +
  falsifier in the receipt; 8b38ceec46 contract json; futon2 dc455666
  regens + run note). claude-1 review: audit delta verified (find leaves
  pre-run-closable, 4->3), READY re-verified by own run, coverage band
  re-pinned WITH CAUSE (Total 10&61 -> 9&62: named -> type-transcribed on
  the existence witness; witnessed stays 0 honestly), flip sidecar
  re-emitted (recurring class — note: --emit belongs IN the re-pin chain),
  full publish green and served. The organise hole is now the last
  pre-run-closable member of the commission.
- 2026-09-09 (eighteenth — e-pre-go-live FIRST SLICE executed): the revision
  addendum + token fence landed (futon2 2b77d212, original preserved as
  history; re-reviewed AGREE by codex-10 at futon3 a376e40), then the first
  bounded slice ran REAL commands (427c695d,
  runs/E-pre-go-live-authority-probe-2026-09-09.edn): an actual
  find-organise/fire execution of the readiness reader with command output,
  source fingerprints, member accounting and driver source recorded. Its
  captured verdict (BLOCKED-ON [regenerates lean-probe hole-open]) is the
  honest reading AT ITS 18:46 MOMENT — mid-window in the find re-pin chain;
  claude-1's own run after the chain completed reads READY on all nine
  lines. Honest boundaries in the record itself: O4 UNEXERCISED — one
  diagnostic action, not a paired precedence experiment; baseline/
  intervention/primary-score manifest still unspecified; no organise
  closure or readiness claim. Disposition (f)'s exemplar remains in
  progress, not satisfied.
- 2026-09-09 (nineteenth — the paired BEFORE/AFTER probes): a second real
  authority-reader execution through find-organise/fire, post-re-pin
  (futon2 a9abb714, runs/E-pre-go-live-post-repin-probe-2026-09-09.md +
  edn): VERDICT READY, all nine lines green, with raw command/stdout/exit,
  member accounting and before/after driver+pattern fingerprints recorded.
  Together with the 18:46 BLOCKED-ON probe this is a recorded
  state-change-under-real-work pair — readiness changed because actual
  preparation work happened between the two executions — NOT a
  precedence-only comparison, and the record says so. O4 remains
  unexercised; organise closure not claimed; no further interpretation
  loop requested. (codex-17's note that the publish count-pin blocker
  remains open is STALE — claude-1 re-pinned the band with cause and
  published green earlier this hour.)
- 2026-09-09 (twentieth — :learning WITHHELD, investigation logged): Joe
  explicitly withholds the learning ruling pending theory/PLoP/
  implementation clarification (685c107c; investigation
  runs/LEARNING-theory-history-runtime-2026-09-09.md). The reframe that
  makes the withholding productive: Da Costa sections 8-9 license both
  trial-boundary learning and BMR — the missing evidence is SUBSEQUENT
  CONSUMPTION of the learned object, not an online/offline locus. A
  genuine records conflict surfaced and recorded without adjudication
  (PLoP Campaign S inter-tick claim vs VERIFY-r-nodes locating no retained
  receipts). July GFN diversity-supplier ruling + slush preserved; edge
  credit has no cascade-construction consumer today. A next-contract shape
  proposed, not adopted (named learned object, admissible evidence,
  versioned posterior, observed next-use, disconnected-consumer control).
  Fresh pure tests 26/92/0. Registry carries the withholding; status
  honestly stays observed-not-decided (still needs the decision, later,
  with better evidence).
- 2026-09-09 (twenty-first — learning HISTORICAL RECOVERY, first unit):
  Joe redirects the learning question: the specification is the CLAIMED
  PLoP system, recovered from the July/August drafts and Science history
  and validated now — not reduction to the current offline-only registry
  state (verbatim + spec at
  runs/LEARNING-historical-recovery-spec-2026-09-09.md, futon2 b6f4040e).
  Seven recovery tickets proposed (LR1-LR7: persistence, trainer
  correctness, diverse proposals, outcome->reward->next-proposal
  consumption, BMR->next-field, pattern admission, historical receipts),
  each a claimed behavior + validation contract with anti-overclaim guards
  inline; proposals only, no ledger writes. FIRST REAL REPRODUCTION
  executed: the historical GFN trainer in the documented futon3a/.venv —
  9/9 tests, full 700-iter run, G0a/G0b true, conditional TV
  0.0125095739..., matching July to ~1e-16 (numeric, not bytes). Campaign
  S receipt hunt: the pickaxe finds later audit text and narrative, NOT an
  original receipt; external stores unsearched, absence stays scoped.
  Superior-success claim remains withdrawn. :learning stays unruled; no
  RUN4 gate changes. Reviewed by claude-1 (note-only verified; ticket
  structure and guards read; reproduction taken on its recorded evidence —
  LR2 re-execution belongs to whoever mints it).
- 2026-09-09 (twenty-second — learning scope CORRECTED): Joe narrows the
  recovery program — no full PLoP-variant restoration; the historical work
  INSPIRES a concrete AIF-compatible implementation and claim, and
  learning is not a single final phase. LR1-LR7 downgrade to reference
  material (not mandatory, not RUN4 gates); nothing else inferred
  (no offline-only adoption, no automatic deferral, no mandatory GFN/BMR
  pairing). The next design's required shape is now stated: each actual
  learned variable with its evidence, update equation/objective, schedule,
  persistent state and NEXT CONSUMER, distinguishing state inference /
  parameter learning / slower structure-proposal revision. Registry note
  appended (16519d17); :learning stays unruled.
- 2026-09-09 (later): sitting seat is codex-12, briefed by claude-1 at Joe's
  instruction; Joe works the excursion interactively in that session.
  codex-12's briefing check surfaced three stale/omitted claims: the :folded?
  correction above; the ruled_outcome_c path (src/futon2/aif/, not checks/);
  and sec-c-vector.tex's bridge account omitting the §1b observation-domain
  gap — the paper statement is being added.
