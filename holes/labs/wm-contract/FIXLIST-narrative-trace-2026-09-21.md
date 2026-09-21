# Fix list: defects found by narrating run 2026-09-21-1789964661
(claude-3, 2026-09-21, for Joe. Source: a stage-by-stage reading of the
records of the grounded-change run 2026-09-21-1789964661; reference
records under `data/wm-runs/tick-run-record-2026-09-21-1789964661.edn`
and `data/wm-full-loop-machinery-69/wm-contract-machinery-69-v1/attempt-001/`.)

Goal: a next run whose stage-by-stage narrative describes a machine that
works: it chooses among real alternatives for a stated reason, forms a
cascade with structure, and gives the author a build plan it can act on.
Each fix has a failure story (what the record shows now), a success story
(what the record should show), and an acceptance test.

Kinds: **fix** = implement; **discovery** = find out why and propose,
no production change (the fix follows as its own item after review).
Lanes: fixes in the same **file group** must be sequential (shared
worktree); different groups can run in parallel.

| # | kind | file group | summary |
|---|---|---|---|
| fix-1 | fix | runner | author is told `:C1`, not the mission |
| fix-2 | fix | runner | the build plan drops the cascade's content |
| fix-3 | fix | runner | prompts and transcripts are not retained |
| fix-4 | fix | runner | `:ranked-candidates` mislabels posterior as G |
| fix-5 | discovery | read-only | only 3 hand-written candidates; 441 wants yield none |
| fix-6 | discovery | read-only | G does not separate the candidates |
| fix-7 | fix | policy | the discrimination guard cannot fail |
| fix-8 | fix | habit | habit learns from selection, not from outcome |
| fix-9 | discovery→fix | runner (construction) | `:semilattice []` is a literal |
| fix-10 | discovery | read-only | no belief update at close |
| fix-11 | fix | war_machine | the scan's own account is discarded |
| fix-12 | fix | runner (grounding) | discharge id collides across cohorts |
| fix-13 | fix | war_machine | coverage account not saved; 113→5 drop unexplained |
| fix-14 | fix | new ns | trace renderer: run id → narrative page |
| fix-15 | doc | p4ng | paper claims that outrun the records |
| fix-16 | fix | admission | a candidate that adds no wanted token is admitted |
| fix-17 | decision | preference | the preference scale makes G differences ~1e-3 nats |

---

## fix-1 — the author is told the candidate id, not the mission
**Failure story.** `selected-target` (`src/futon2/aif/full_loop_runner.clj:1185`)
prefers `:cascade-id`/`:id` over `:target`. For a cascade candidate the
id is `:C1`, so: the author prompt says `SELECTED TARGET: :C1` (phase
log); the mission record is looked up by `:C1` and is nil; the selection
checkpoint says `:selected-mission ":C1"`; futon1b holds
`:implementation/target ":C1"` and `:discharge/mission ":C1"`. The
author built commit 7a9daa0f without being told which mission it served.
**Success story.** For a `:cascade-candidate` the selected target is the
candidate's `:target` ("M-aif-policy-conditioned-eig"); the cascade id
stays available separately as `:selected-cascade`. The prompt names the
mission and includes its record; every consumer (checkpoints, grounding
entities, brief item, delivery QA, tripwire observation) carries the
mission id.
**Acceptance.** A test that feeds a cascade-candidate entry shaped like
the 1789964661 selection through the prompt builder and the
checkpoint/grounding writers and asserts the mission id (not `:C1`)
appears in each; and that a non-cascade action still resolves as before.

## fix-2 — the build plan drops the cascade's content
**Failure story.** `author-prompt` (`full_loop_runner.clj:1844-1920`)
passes `select-keys [:mission :psi :shown :semilattice …]`, i.e.
`{:psi "enact cascade :C1" :shown [pattern-id] :semilattice []}`. The
box, its established guard conditions, the interpretation `:reading`,
`:scope-limit`, and flexiarg path are all dropped. The author receives a
pattern name and nothing about what the pattern requires or produces.
**Success story.** The prompt carries, per pattern in precedence order:
flexiarg path + sha, the interpretation reading and scope limit, guard
clauses with which conditions are established (and by which C3/C4
check), what it produces, and holes with construction blockers; plus the
wires between boxes. The author can say which pattern its change enacts.
**Acceptance.** Prompt-builder test on the 1789964661 construction
(`003-construction.edn`) asserting each of those fields appears; a
prompt-size bound so large cascades don't blow up the packet.
**Depends on** fix-1 (same function region).

## fix-3 — prompts and transcripts are not retained
**Failure story.** The dispatch checkpoint holds only
`:prompt-ref "agency-job:invoke-…"`. The prompt text exists only in
`/tmp/futon3c-invoke-jobs.edn` (ephemeral); author and reviewer
transcripts survive as tool-event counts. A narrative cannot quote the
build plan or show how the work was done.
**Success story.** The dossier retains the author and reviewer prompt
text (with sha256) and the final reply text of each job, under the
attempt directory, referenced from the dispatch/build checkpoints.
**Acceptance.** Runner test with a stubbed Agency returning a job with
prompt + result: the files exist under the attempt dir, their sha256
matches the checkpoint reference.

## fix-4 — `:ranked-candidates` mislabels posterior mass as G
**Failure story.** In `002-selection.edn`,
`:ranked-candidates` (`full_loop_runner.clj:3779-3800`) puts posterior
mass under `:G-efe` and `:controller-score`, ranks by map-iteration
order, and keeps only the first 10. A plot drawn from it is wrong.
**Success story.** Each ranked entry carries `:target`, `:cascade-id`,
`:G`, `:habit`, `:F` + `:f-status`, `:posterior`, sorted by posterior
descending, all candidates (or an explicit `:truncated-from N`).
**Acceptance.** Test on the 1789964661 selection certificate: three
entries, G values 15.17007/15.17133/15.17138, posteriors in descending
order, no field holding a value of another kind.

## fix-5 (discovery) — why the machine chose among 3 hand-written cascades
**Failure story.** The 441 stated mission items (99 checkbox wants)
enter C but generate targets with `:candidates []`, which admission
refuses (`cascade_problems.clj:125-156`). The live choice was among 3
single-pattern cascades hand-admitted by codex-1 that night. Retrieval
(embedding k=40, tier0 k=8) produces proposals that are never admitted
without an agent writing the declaration.
**Success story (for the discovery).** A note that says, with counts from
a current tick: how many targets, proposals, admitted candidates; which
stage removes each; and a concrete proposal for how a want gets a
constructed candidate without an agent hand-writing it and without
treating retrieval rank as applicability. Then a fix item.

## fix-6 (discovery) — why G doesn't separate the candidates
**Failure story.** G = 15.17007 / 15.17133 / 15.17138 nats; the winner
led by 0.0013 nats. In runs 1789951020/1789952479 all 24 G values were
within 0.003 nats and habit E decided. The six-term census reads A
identity, D point-mass, E uniform, F zero, Q open-loop — only C varies.
**Success story (for the discovery).** A per-candidate decomposition of
G into the terms that differ vs the common offset (likely the ~463
unreachable wants), showing which term produces the 0.0013 nats, and a
proposal for what would make G discriminate (scoring only
reachable/relevant wants, a non-identity A, a longer horizon…).

## fix-7 — the discrimination guard cannot fail
**Failure story.** The selection guard passes with ε = 1e-6, so a 0.0013
nat lead (posterior 0.3336 vs 0.3332) is recorded as
`:discrimination {:passes? true}`. Nothing in the record says the choice
was effectively a tie.
**Success story.** The selection record states what decided it:
`:decided-by` ∈ {`:G`, `:habit`, `:free-energy`, `:tie-break`} with the
posterior margin and each term's contribution to the margin, and a
near-tie flag with a declared threshold. (Recording, not abstaining.)
**Acceptance.** Construct the bad case: the 1789964661 three candidates
→ flagged near-tie. The 1789952479 case → `:decided-by :habit`. A
constructed well-separated case → `:decided-by :G`, no flag.

## fix-8 — habit learns from selection, not outcome
**Failure story.** `select-and-record-cascade!` bumps the habit count
(`data/wm-habit/cascade-prior.edn`) at selection time, before the
build's outcome is known. Since E decided earlier runs, the machine
reinforces what it picked, whether or not it worked.
**Success story.** Habit is updated at close from the attempt outcome
(grounded change vs failure/refusal), with the selection event recorded
separately; the update rule is written down in the record.
**Acceptance.** Tests: a selected-then-failed attempt does not increase
that cascade's habit; a grounded change does; the selection event is
still logged.

## fix-9 (discovery → fix) — `:semilattice []` is a literal
**Failure story.** `construct-selected-action :cascade-candidate`
(`full_loop_runner.clj:1297`) writes `:semilattice []`. Cascades are
1–3-pattern ordered lists; the recorded one has one box and no wires.
In Alexander's terms (A City is Not a Tree) that is a chain, not a
semilattice. The organiser (`cascade_policy/organise`,
`F12RuledCarrier.lean`) exists but is not on the serving path.
**Success story.** The construction records the actual order structure
computed from the wires (produces → guard), with an honest shape label
(`:singleton`/`:chain`/`:tree`/`:semilattice`, the last only when two
sets overlap without one containing the other), and the organiser is
either on the serving path or its absence is recorded. Discovery first:
what it takes to put `organise` on the path.

## fix-10 (discovery) — no belief update at close
**Failure story.** `007-closed.edn`: `:entity-state-at-close {:status
:absent :reason :in-force-belief-row-unavailable}`. The run changed the
repo but not what the machine believes.
**Success story (for the discovery).** Where the in-force belief row
should come from, why it is unavailable, and the smallest change that
lets the close condition belief on the observed outcome.

## fix-11 — the scan's own account is discarded
**Failure story.** `render-war-machine` produces a markdown account of
the scan and judgement; the runner discards it.
**Success story.** It is written beside the run record
(`data/wm-runs/…-scan.md`) and referenced from the run record.
**Acceptance.** Runner test asserts the file exists and is referenced.

## fix-12 — discharge id collides across cohorts
**Failure story.** futon1b entity `full-loop/discharge/attempt-001` is
keyed only by attempt ordinal; the next cohort's attempt-001 overwrites
it (PUT replaces the document).
**Success story.** The id includes the cohort id (and run id).
**Acceptance.** Test: two cohorts' attempt-001 produce distinct ids.

## fix-13 — coverage account not saved
**Failure story.** C's reach fell from 113/468 to 5/468 between runs
1789952479 and 1789964661; the per-tick `:mission-hole-coverage`
account (`mission_hole_wants.clj:158`) is not persisted, so the drop
cannot be explained from records.
**Success story.** The coverage account is in the run record; the drop
is explained (probably c155d690 removing empty cascades before scoring).

## fix-14 — trace renderer
After fixes 1–4 and 11. A tool that takes a run id and writes a
stage-by-stage narrative page (checkpoint order: time-step → selection →
construction → dispatch → build → adjudication → closed) with a
selection plot, the cascade as drawn from its wires, the prompt as the
author saw it, commit + review, close — citing file paths and the Lean
names the code corresponds to (stated as docstring correspondence, not
runtime use). Published to the web docroot.

## fix-15 — paper claims that outrun the records (doc, no dispatch)
NOTE-restatement-2026-09-20: "441 items … enter the decision as
first-class sources" — they enter C only; `g_term_decomposition` cited as
if a theorem — it is a Clojure namespace; "selected on its own merits" —
habit E decided the earlier picks.

## fix-16 — a candidate that adds no wanted token is admitted
(From fix-6.) In 1789964661 the M-wm-08-external-f2 candidate's wanted
token (weight 1/6) and its prerequisites are already present in D, so
its predicted terminal state equals the initial state: G − B = 0. The
machine scored a no-op alongside two real candidates. Same class as the
empty cascades removed in c155d690.
**Success.** Candidate admission refuses (typed, recorded) a candidate
whose predicted trajectory newly satisfies no wanted token.
**Acceptance.** The 1789964661 F2 candidate is refused with that reason;
the other two are admitted; a candidate adding one token is admitted.

## fix-17 (decision) — the preference scale
(From fix-6.) G differences are sums of newly satisfied terminal weights;
global normalisation over 468 sources makes those weights ~1e-3–1e-5
(229/175347, 5/116898), so any habit ratio dominates. Scaling all
terminal log weights by one declared k (C renormalised, so the Lean KL
and horizon definitions are kept) gives spread 0.13 nats at k=100, 1.3
at k=1000. k is a preference declaration, not a tuning knob: choosing it
to force a winner has no warrant. Needs a declared, justified scale
(e.g. operator-stated outcome odds). EoI C1/C2 tie at every k.

## fix-5a/5b/5c — machine construction of candidates (status)
- fix-5a MERGED (38f55e9d): pure constructor from given interpretations.
- fix-5b DEFERRED (note: runs/fixlist-2026-09-21/fix-5b-BLOCKED.md). My
  packet required construction to use the selection's G; that cannot hold
  as written, because selection G is computed over the joint admitted
  family, which construction itself determines (counterexample: same
  candidate 3.013 alone vs 4.399 jointly, a 2 ln 2 universe offset).
  Also: all four real declarations refuse in the constructor
  (policy-conditioned-eig and f11 :unproduced-need; EoI
  :observation-required; wm-08-external-f2 :want-already-observed, the
  same no-op fix-6 found), so wiring now adds no candidates.
  Ruling for when 5b resumes (claude-3): the invariant is relaxed by its
  author, not routed around — construction scores in a declared
  per-target context recorded as such on the receipt
  (`:g-context :construction-per-target`), and selection re-scores every
  admitted candidate jointly; no claim that the two G values are equal.
- fix-5c: the preselection interpreter step (retrieval proposal →
  attributed interpretation) — the missing supply of interpretations.

---

# improve-N: capabilities the model lacks (from claude-5, on Joe's instruction, 2026-09-21)
Source: holes/labs/wm-contract/MAP-rnode-to-lean-2026-09-21.md (codex-16, c09b6880).
fix-N = defects; improve-N = missing capabilities.

| # | summary | depends on | overlaps |
|---|---|---|---|
| fix-21 | registry bindings moved for R4 (predictedOutcome), R13 (horizonEFE), R17 (DirichletLearning.accumulate); FUNDAMENTALS.edn and the paper §9 generator stale | — | small; do first |
| improve-2 | ground C: a fitted outcome→disposition kernel (P(d\|o)) so risk KL[Q(o\|π)‖C] has one declared outcome domain on a real tick | — | fix-17 (scale k) and fix-6/fix-13 findings: live C is over target-qualified token outcomes with globally normalised weights; improve-2 decides the domain that fix-17 would scale |
| improve-1 | A/B learned from the tick record: (o,s,u) → R17 accumulation → normalised A/B → the ForwardModel the next rollout plans with; witness: a changed record changes a planned Q(o\|π) | fix-10b (signed observations, merged), fix-10e (next tick consumes observations, in flight) | fix-6: A is the identity kernel today, a named degeneracy |
| improve-3 | equations to the running system: R6 policy set (:diverges), Holes.wmRunsOnce sorry, the Lean decl → runtime fn → live call site → recorded choice join | — | partly covered: fix-7 records what decided the choice at policy and action level; fix-14 narrates runtime → record. Spec correction: the live law enacts the Bayes action on the ACTION MARGINAL (ActionMarginal.IsBayesAction, cascade_selection/bayes-choice), not the argmax of the policy posterior — the acceptance should name that law |

Ordering (claude-3): the narrative run first (fix list complete bar fix-10e); then fix-21; then improve-2 as a discovery (its answer changes fix-17's question); improve-1 after fix-10e lands; improve-3 last, scoped to what fix-7/fix-14 don't cover.

# Learning raised to critical (claude-5, on Joe's instruction, 2026-09-21)
Joe: "learning has been under-specified in the system to date, even though it's
a key preference of mine and a key part of AIF that we shouldn't leave lying on
the table."

| # | summary | depends on | status |
|---|---|---|---|
| improve-1 | parameter learning over the real model (B first; A has no labelled evidence) | fix-10a/b/d/e (merged) | CRITICAL. discovery 71e9621a; slice 1a record-only trial receipts 27e7b982; slice 1b attempt-grain trial ledger (codex-11, in flight); routing theta into the rollout waits for C strength (improve-1 discovery: under today's near-uniform C a failure LOWERS G via entropy) |
| improve-5 | novelty term in live G: expected parameter information gain over improve-1's Beta-per-effect kernels, beside risk + ambiguity; audit predictability-bonus / model-uncertainty-fn. Acceptance: a tick whose action is chosen because of its parameter EIG, visible in fix-7's decided-by | improve-1 kernels (the illustrative prior suffices for a record-only discovery); Joe's Q3 strength (EIG, risk and habit share the nat scale) | CRITICAL. discovery dispatched |
| improve-6 | accounts of learning and capability (JOINT: Joe, claude-5, claude-3): surprise followed by structural model revision = learning; surprise repeated without revision = friction. Natural experiment: 2026-08-30 facade discovery (commit-rate series futon0/analysis/audits/commit-timeseries-2026-09-21.csv) | none; observational | design starts now with claude-5 |

Correction to the proposal's location of the dark-room term (checked by claude-3):
predictability-bonus (efe.clj:431) and model-uncertainty-fn (efe.clj:236-255)
are in the channel compute-efe path. The live cascade score is
rank-cascade-actions → horizon-g-sparse (risk + ambiguity only), which contains
neither. On the live path the problem is the absence of any parameter-novelty
term, plus improve-1's finding that near-uniform C makes risk reward outcome
entropy.

| improve-7 | focus-conditioned C: a declared, time-scoped operator focus (mission-clock carrier) concentrates Cτ on the focus's outcomes; near-uniform C only when no focus is declared; off-focus work carries a MAY valence where the focus requires it (Joe via claude-5, 2026-09-21; ANSWERS-C-questions) | improve-2 Q3 strength (now to be asked per focus); improve-4 valences | filed; the strength question is re-framed per focus |
