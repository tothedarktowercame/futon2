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
- 2026-09-09 (later): sitting seat is codex-12, briefed by claude-1 at Joe's
  instruction; Joe works the excursion interactively in that session.
  codex-12's briefing check surfaced three stale/omitted claims: the :folded?
  correction above; the ruled_outcome_c path (src/futon2/aif/, not checks/);
  and sec-c-vector.tex's bridge account omitting the §1b observation-domain
  gap — the paper statement is being added.
