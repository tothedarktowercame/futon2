# T-giraffe-undetermined-triage — read Giraffe's undetermined items before the War Machine works on them

**Requisition:** in-progress — dispatched 2026-11-06T07:08:15Z to Kimi seats (claude-6, for Joe)

**Asked by:** Joe, 2026-11-06: "the big learning here is that we need to treat
the under Giraffe 'undefined' things the same way, lest we risk running more
automated turns on outdated work."

**The same way** means what was done for M-formal-patterns
(`T-formal-patterns-unblock`): find out what the item is waiting on, and
whether it is still current, before any automated work is spent on it. That
case turned out to be work that was mostly re-running finished experiments; Joe
retired it and opened a redux.

## Items (Giraffe run 20261106T065137.916Z-47c60c8f, status undetermined, reason dependency-reading)

Group A (kimi-1): M-superpod-mark3, M-stack-essay-code-alignment, M-stack-morphogenetic-rewrite, E-early-memories-review
Group B (kimi-2): E-futon1a-to-futon1b-migration-pipeline, E-ground-G, E-memory-whitepaper-v3-programme, E-mfuton-native-installation-20261003
Group C (kimi-3): E-mfuton-typeclass-scope-state-20261003, E-river-feedback-results, E-transitive-proxy-ghosts, E-wm-apparatus

Not triaged here: seven E-kimi-task-* requisition records that Giraffe also
lists as undetermined (103, 104, 129, 130, 26, 27, 40). Requisitions should not
be scanned at all (Joe, 2026-11-04); that is a Giraffe defect, filed separately.

## Per item (filled by the worker)

For each item: what it is in two sentences; why Giraffe could not decide
(the unresolved edge from its reading, quoted); a verdict, one of
**current** (concrete next items exist and nothing blocks them),
**blocked** (each blocker stated as a technical question for Joe: the
decision, the options, what each changes, evidence file:line), or
**likely outdated** (evidence: superseded by what, finished where, or
depends on work that has moved on). No edits to the item itself.

## Group A

### M-superpod-mark3 — **current**

What it is: futon6 mission (`/home/joe/code/futon6/holes/missions/M-superpod-mark3.md`) to extend the mark2 arXiv substrate with an arxiv-aware pattern-tagging prompt, a hierarchical pattern set, coverage discipline (no silent-null `analysis`), slot-distinctness, and per-paper geometric artifacts — the substrate delivery for prototype 2 of M-pattern-application-diagnostic.

Why Giraffe could not decide: the reading's `unresolved` list is **empty**; the undetermined verdict came from reading its declared `depends-on` edges (M-superpod-mark2, E-math-prototype-pilot). Both target docs exist and are in their expected states (mark2 is the finished predecessor; the pilot is the finished seed), so the dependency reading is satisfied, not blocking.

Evidence of currency: active work this week — futon6 commits `3ae38c1` (2026-11-06, "mark3 R-1: absorb futon3 taxonomy drift into arxiv pattern prompt") and `6bef7cb` (2026-11-06, "M-superpod-mark3: coverage-record schema with the two named silent-fail modes"). MAP-exit Q1–Q6 answered 2026-04-27 (`22ff9ac`). Concrete next items exist (R-2 pattern-set authoring, R-3, geometry artifact, INSTANTIATE as `superpod-mark3` runner).

Questions for Joe: none. **0 questions.**

### M-stack-essay-code-alignment — **blocked**

What it is: futon7 interface mission (`/home/joe/code/futon7/holes/M-stack-essay-code-alignment.md`) keeping the descriptive essay of the stack and the running code aligned via typed events, with drift recorded as first-class lift-anomalies. It is a HEAD-as-escrow scaffold: every substantive section (§1.1–1.5 Right View/Intention/Speech/Mindfulness/Livelihood, §2 tensions T1–T4) is marked "Joe: verbatim — TO FILL".

Why Giraffe could not decide: unresolved edge quoted verbatim — `"raw_target": "M-stack-essay", "kind": "depends-on"` — "No task document named M-stack-essay.md exists in any repo's holes/; it appears to be shorthand for M-descriptive-essay-of-the-stack.md (which is separately cross-referenced), but the text names both, so it is left unresolved rather than assumed."

Blockers / questions for Joe:

- **Q1 (naming):** Is `M-stack-essay` simply `M-descriptive-essay-of-the-stack.md` under a short name? Evidence: `M-descriptive-essay-of-the-stack.md` §0 "Suggested decomposition into a mission cluster" lists "M-stack-essay (this file, narrowed)" — i.e. the essay mission was meant to be this same file narrowed, never a separate document. If yes, the fix is a rename/alias note, and Giraffe's unresolved edge is a naming artifact, not a missing dependency. If no (a separate narrowed essay doc was intended), that doc was never created.
- **Q2 (sequencing):** The mission "cannot close until both siblings close" (M-descriptive-essay-of-the-stack and M-war-machine-aif-completion), and both are still OPEN scaffold/early state (essay last revised 2026-10-01, commits `019fa09`/`59c8462`; WM-AIF-completion still awaiting Joe verbatims). The codex-7 dependency it names as load-bearing — M-interest-network-coupling step (b), the typed event vocabulary — **landed** 2026-05-26 (`futon3/library/structure/interest-event-vocabulary.flexiarg`; M-INC status line, `/home/joe/code/futon4/holes/missions/M-interest-network-coupling.md:4`). Should this mission's verbatim authoring start now (scaffold-and-design work was explicitly allowed to proceed without the vocabulary, which now exists), or stay escrowed until the siblings progress?

**2 questions.**

### M-stack-morphogenetic-rewrite — **blocked**

What it is: futon7 after-the-exit mission (`/home/joe/code/futon7/holes/M-stack-morphogenetic-rewrite.md`): rewrite the stack's code shape under typed morphogenetic discipline (Lafont/interaction-combinator framing as direction, f:(A,B)→C rewrites as substrate) once the essay/alignment cluster closes. Also a HEAD-as-escrow scaffold with all Joe-verbatim sections TO FILL; engineering ownership explicitly "TBD pending Q-CL5".

Why Giraffe could not decide: unresolved edge quoted verbatim — `"raw_target": "M-stack-essay", "kind": "depends-on"` — "No task document with id M-stack-essay exists in any holes/ tree; the essay work may live under M-descriptive-essay-of-the-stack, but the id as written does not resolve."

Blockers / questions for Joe:

- **Q1 (naming):** Same as the alignment mission's Q1 — the gating sentence names `M-stack-essay`, which does not exist as a document; M-descriptive-essay-of-the-stack §0 is the presumed referent. Confirm the alias (or create the narrowed doc).
- **Q2 (dormancy):** This mission "does not begin until `M-stack-essay`, `M-war-machine-aif-completion`, AND `M-stack-essay-code-alignment` have closed" (line 9) — none of the three has closed, so it is blocked by design, possibly for months. Should it be marked dormant/excluded from work-selection until the precondition holds (so the War Machine stops spending reads on it), and re-activated by the cluster closing? Also open inside it: Q-CL5 — what the rewrite target is: (a) the codebase `.clj`/`.cljs`/`.el` files directly, (b) the `stack-annotations.edn` annotation hypergraph with code regenerated as a projection, or (c) both; the doc's default suggestion is (b). That decision is yours and gates any engineering, but nothing is lost by deferring it until the precondition is near.

**2 questions.**

### E-early-memories-review — **blocked**

What it is: futon3c excursion (`/home/joe/code/futon3c/holes/excursions/E-early-memories-review.md`) running a receipted review pass over the 203 problem-centric memories — population A (~89 loop memories) reviewed per-memory with a `generalize` verdict, population B (114 compression-pilot diaries) sampled then class-verdicted. Only the six-item proof-text slice was closed (retired 2026-08-25, verified 2026-08-30); the full review remains open.

Why Giraffe could not decide: unresolved edge quoted verbatim — `"raw_target": "invoke-…878", "kind": "depends-on"` — "An ellipsised invoke job id for the cross-frame snapshot machinery, not a task document; no M/E/T doc resolves to it." The doc sequences its packet "after `invoke-…878` lands" (line 80), and the ellipsised id cannot be checked for landed/not-landed.

Blockers / questions for Joe:

- **Q1 (the dependency):** Did the cross-frame snapshot machinery referenced as `invoke-…878` (the thing that would let the Student see prior frames' reviewed memories at all) ever land, and under what name? The doc itself separates the two concerns: "Review makes them *eligible*; snapshot seeding makes them *accessible*" (§What this does not do) — so the review pass does not technically need the snapshot to run, only the packet's step ordering assumes it. Options: run the review now without waiting (the urgency argument in the doc — "Run population A before F32's `:promote-solver`" — is from 2026-08 and that frame boundary has presumably passed), or confirm the review is moot because the math-loop campaign has moved on. Which is it?
- **Q2 (reviewer):** If the review runs, the reviewer must have deposited none of the 203 ("a Zai or Codex agent; not `zai-1`, not any `f*-scribe`/`f*-guide` identity"). Is that reviewer seat still to be dispatched, or has the memory substrate changed enough since 2026-08 (e.g. the current memory/promotion pipeline in futon3c) that the candidate list should be re-derived first via `memory_shape.py --list-problem-centric` before any review is dispatched?

**2 questions.**

## Group B

## Group C

### E-mfuton-typeclass-scope-state-20261003

**What it is.** A compiler-work requisition (futon3c/holes/excursions/E-mfuton-typeclass-scope-state-20261003.md):
implement SourceTypeclassScope state construction and
`_build_instance_result_owner_index` in the SBCL Lisp port of mfuton, with a
real Python differential, continuing from the accepted declaration-index
commit 97479d0. Dispatched to codex-13 on 2026-10-03.

**Why Giraffe could not decide.** Its single edge is a depends-on on
E-mfuton-typeclass-declaration-index-20261003, which is completed
("integrated as 97479d0"), so the dependency resolved; the status was left
undetermined because "in-progress" had no completion evidence either way.

**Verdict: likely outdated.** The Lisp curriculum this requisition belongs to
was paused the day after dispatch: mfuton-share f8eed5a1 (2026-10-04, codex-19)
"Pause Lisp curriculum at source HAdd OfNat wiring boundary", plus 99ed4a64
"Pause transpiler push". No `lisp/src/source-typeclass-scope.lisp` or
`instance-result-owner-index.lisp` was ever committed (no such paths in
mfuton-share history). The repo named in the requisition,
/home/joe/code/mfuton-sbcl, no longer exists; the tree lives on as
/home/joe/code/mfuton-share, whose post-2026-10-04 work went into Python-side
psw/* merges and EmitPython, not the SBCL port. The packet also binds the
worker to a stale environment ("/tmp/mfs-venv", MFUTON_HOME=/home/joe/code).

Questions for Joe:
1. Retire this requisition, or re-point it? The Lisp port is paused upstream
   of this step (paused at the HAdd/OfNat boundary, before typeclass scope
   state), so the requisition cannot proceed as written. Options: (a) retire
   as superseded by the pause; (b) rewrite to name mfuton-share and the
   current pause boundary, queued behind a resume decision. Evidence:
   mfuton-share commit f8eed5a1 (2026-10-04); requisition header
   "Continue in /home/joe/code/mfuton-sbcl" (path gone).

### E-river-feedback-results

**What it is.** A results note (mmca-clj/holes/E-river-feedback-results.md)
measuring the original paper river's phenotype→genotype feedback by matched
ablation: ~0.90 genotype divergence within 5 steps, saturating near 0.92.
Its stated purpose was to be "the control the correction round (E4/E5/E6
reruns) should adopt."

**Why Giraffe could not decide.** Both unresolved edges point at a target
with no document: "E4/E5/E6 reruns are future correction-round work with no
task document; the existing E4-results.md / E5-results.md / E6-results.md …
are results notes without M-/E-/T- ids, so no target resolves."

**Verdict: likely outdated.** The purpose is fulfilled. The E4/E5/E6 reruns
already adopted exactly this control: mmca-clj holes/E4-results.md is now
"E4 — Directed predictive information (corrected: authentic river + matched
ablation)" (16f9d77) and E5/E6/E7 were regenerated "with fixed ablation
(frozen p0, true X→G cut)" (1f0352a, 2026-07-19), one day after this doc's
v2 (3076c1b, 2026-07-18). E4/E5-results.md quote the matched-ablation design
verbatim. Nothing in mmca-clj has touched these files since (latest
repo-wide activity is the 2026-10-09 verdict batch). The note is a finished
results document whose downstream consumer consumed it.

Questions for Joe:
1. Close as completed? The doc's only open item was "the correction round
   should adopt this control", and it has (E4/E5/E6 all rerun against the
   matched control, July 2026). Options: (a) mark completed; (b) keep open
   only if a further rerun round is planned. Evidence: mmca-clj commits
   1f0352a, e3b1e8e, 3275bd8, 16f9d77; holes/E4-results.md:1.

### E-transitive-proxy-ghosts

**What it is.** A defect note (futon3c/holes/excursions/E-transitive-proxy-ghosts.md):
federated proxy pruning is strictly pairwise, so a relayed proxy of a
departed agent is never pruned — presence is transitive under relay, absence
is not. Two repair shapes sketched (origin-attested pruning; origin-attested
TTL), neither implemented.

**Why Giraffe could not decide.** Its one unresolved edge is a depends-on on
"futon0/README-federate.md F-0" — "A README, not a task document (no
M-/E-/T- id), so it does not resolve as a link target; it specifies the
:origin/site and :origin/epoch metadata the proposed fixes rely on."

**Verdict: blocked.** The defect is still in the code:
`prune-departed-proxies!` (futon3c/src/futon3c/agency/federation.clj:935) is
unchanged and still prunes only against the immediate peer's roster. The
2026-08-14 commit e81fb5098 "Canonicalize federation proxy origins" added
origin-identity canonicalization (the metadata shape-1 needs) but not
origin-attested pruning or TTL. So the defect is live and real, but which
fix to build is an undecided design choice the doc itself leaves open.

Questions for Joe:
1. Which repair shape? (a) Origin-attested pruning: propagate origin
   identity, prune when the origin stops advertising — closer to the
   federation design's F-0 commitments, and e81fb5098 already landed the
   canonical origin identity it builds on. (b) Origin-attested TTL: proxies
   expire unless refreshed by origin-attested presence — cheaper, fails safe
   under partition. The doc says they are not exclusive. Evidence:
   federation.clj:935 (pairwise prune, unchanged); e81fb5098 (origin
   canonicalization landed).
2. Is the relayed multi-site topology still in service? The defect was
   measured on zone→chi→lucy→oxf (2026-08-14). If the fleet no longer runs
   relayed proxies, this can be closed as no-longer-reachable rather than
   fixed. Evidence: E-port-wiring-map.md is the topology of record.

### E-wm-apparatus

**What it is.** A framing excursion (futon2/holes/excursions/E-wm-apparatus.md)
naming the apparatus-layer assurances (recording, delivery, liveness,
identity) the War Machine needs beyond its verified mathematics, and seeding
the assurance-frontier ticket queue :U70–:U74.

**Why Giraffe could not decide.** All five unresolved edges are depends-on
edges to the tickets the excursion seeds: ":U70 … no task document with that
id exists in any canonical repo" (same reason for :U71, :U72, :U73, :U74).

**Verdict: likely outdated.** The seeded queue is done. In
futon2/holes/labs/wm-contract/dependency-frontier.md:171-210, :U70, :U71,
:U72, :U73, :U74 are all marked `:done` (and the frontier has grown past
them to :U81). The assurance-frontier table the excursion asked for exists
in the same file (:witnessed / :ticketed states per pattern × signature),
and the wm-contract lane stayed active through 2026-11-06 (e.g. c753538f7).
The excursion was a seeding document; its work queue is drained.

Questions for Joe:
1. Close as completed? Every concrete deliverable the doc names (:U70 DAG,
   :U71/:U72 receipt discipline, :U73 detector commissioning, :U74 standing
   caller comparator) is :done in the frontier. Options: (a) mark completed,
   keeping it as the historical framing for the frontier; (b) keep open only
   if the "walked pattern × signature" sweep it defers ("to be walked … when
   :U70 builds the DAG") still owes a pass — but that is now frontier
   content, not this document's. Evidence: dependency-frontier.md:171-210,
   :236-244.
