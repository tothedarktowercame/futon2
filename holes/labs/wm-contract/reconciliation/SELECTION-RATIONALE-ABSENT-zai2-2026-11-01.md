# SELECTION-RATIONALE-ABSENT — discovery report (zai-2, 2026-11-01)

Row: `SELECTION-RATIONALE-ABSENT` (B3,D4; OPEN; Joe 2026-11-01).
Run read: `2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324`
(`data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn`,
Giraffe run `20261101T031213.797Z-cb8ff89c`, manifest sha
`1b409b0189a6a2d40ed18b1ce49d15e9f7314dba23947140ade0d4f724cc3c93`,
makeup sha `f312583522597c49b4134b08c4a186a013baaaa12a6f1c332fe9875e57f58e3a`).
Observed: `:cascade-scores [3.2121207095955158 3.2121207095955158 3.2121207095955158]`,
`:tied-cascades` all three, `:tie? false`, picks `[E-comprehension-foundation
E-comprehension-foundation :no-pick]`. Everything below was replayed read-only
from the pinned Giraffe pass and the exact step functions of
`futon2.aif.outer-score`; the pick and the score 3.212120709595516 were
reproduced to the last digit.

---

## (1) What can inform selection — and what each item reaches today

| item | reaches outer G? | reaches a cascade step? | reaches nothing |
|---|---|---|---|
| Giraffe readiness `:status` (ready/not-ready/undetermined) | yes — selects the `state-counts` row (q) | yes — `:ready-gate`, `{:gate-status …}` | |
| Giraffe `:unblocks` | only via `ready-gain` **inside** the landing, and only when the ending is known (never this run) | yes — `:by-unblocks` | |
| Giraffe `:cone` | **never** — G is computed on the pick only; no ordering term enters G | yes — `:by-cone` | |
| focus ("WM", commit-facet attribution / retention, `focus_receipt.clj:30-100`) | yes — via `ending-class` | | |
| relation corpus + pinned structure embedding (futon6, 2026-06-25) | yes — relation → ending class → landing | | |
| learned kits `c-kit`/`gain-kit`/`state-kit` (outer learning, E2) | yes — preference and q | | |
| candidate reach | nominally (kernel weights), but it is the makeup's uniform `:default-reach 1/2` for every candidate, and a stop-landing makes the kernel rows identical, so it carries no information | | |
| effective-run history | no — pre-G exclusion only (`duplicate-effective-run-exclusions`, `outer_select.clj:19-47`) | yes (excludes M-pattern-retrieval-calibration this run) | |
| Cascade Live lineage, clusters, arrows with delta-g | | | nothing — no reader of `/api/alpha/cascade-real` anywhere in the selection path (grep of futon2 src finds only the E-cascade-real probe namespaces) |
| held items and tickets (hold reasons, rearm conditions) | | | nothing — tickets enter only as registry rows, never as hold evidence |
| pattern feedback (which patterns each mission applied) | | | nothing — `:patterns` in the makeup is a decorative string list |
| prior outcomes | only in aggregate (kits, updated after runs); per-target history is only the duplicate-run exclusion | | |
| operator wants | only indirectly through the focus receipt, which is commit attribution, not a want channel | | |
| focus beyond "WM" | | | nothing discriminating — every candidate shares one focus |
| refusal history (`outer_refusal_history.clj`) | | | nothing — namespace exists, `outer-score`/`outer-select` do not consume it |
| want counts | | | nothing |

Net: of Joe's "so many items that can inform selection", exactly three
families reach G today — Giraffe status, the relation channel (corpus +
embedding), and the learned kits — and one of those three (status) currently
cannot separate policies (see (3)).

## (2) One real selection step, walked

Field construction: 609 pass rows (`:order`) → 16 registry/policy
exclusions (source-absent, status-class, all-policies-already-satisfied) →
47 graph-term exclusions by `known-term-field` (the pass's 52 `:undetermined`
readiness rows coincide with its 52 graph-unknown rows; the five that survive
registry filtering are the bulk of the 47) → 1 `duplicate-effective-run`
exclusion: **M-pattern-retrieval-calibration**, matched run
`2026-11-01-c5590f95-a22e-4f21-a5e2-2cf47f393803` on the same manifest.
545 admitted (`:outer-field/admitted` head: E-comprehension-foundation).
Read-only replay of the same steps over the same pass reproduced both picks
and the no-pick exactly.

Cascade `:what-others-wait-on-first` `[:ready-gate :by-cone :by-unblocks]`:

1. `:ready-gate`: 545 → **499** (:ready only).
2. `:by-cone`: 499 → 499, ordered; head becomes M-interim-director-proxy-metric-inventory (cone 9).
3. `:by-unblocks`: **re-sorts the whole field**; head becomes E-comprehension-foundation (unblocks 2 — the unique maximum; runner-up proxy-metric has unblocks 1).

Cascade `:only-what-can-start-now` `[:ready-gate]`: 545 → 499; the pick is the
first :ready candidate **in field order**, which is E-comprehension-foundation
only because M-pattern-retrieval-calibration (the pass's first :ready row,
also unblocks 2) was removed by the duplicate guard. For this cascade the head
of the field is literally an accident of source order plus one exclusion.

Cascade `:find-out-before-building` `[{:gate-status :undetermined} :by-cone]`:
`{:gate-status :undetermined}` matches the 52 undetermined rows, **all of which
were already excluded** as graph-unknown → `:no-pick`, retained with no reason.

Grounds for the final ranking: unblocks=2, unique max among ready. It is real
(the pattern's own predicate), but the record does not say it, and it is
invisible to G (see (3)).

Defect (outside this row, named and left): in
`apply-selection-step`, a later `:order` step does not tie-break the earlier
one — it replaces it. `:by-cone` in `:what-others-wait-on-first` is dead
whenever any two ready candidates differ in unblocks; the composite cascade is
exactly `[:ready-gate :by-unblocks]` + field-order tie-break. The pattern
(`task-selection/what-others-wait-on-first.flexiarg`) also obliges recording
"that the high-leverage one was deferred and why" — unimplemented.

## (3) Why the policies do not differ in G

Reproduced exactly. G = risk + ambiguity − novelty
(`outer_score.clj:185-196`).

- Learned `c-kit` normalises to {focused 11/20, related 7/20, unrelated 1/20,
  stop-the-line 1/20}; `gain-kit` normalises to {none 0.8054, one 0.0637,
  several 0.1309}.
- Both picked policies: relation of E-comprehension-foundation to focus "WM"
  is `:unknown` → `ending-class` nil → landing `[:stop-the-line :none]` → the
  doable and blocked kernel rows are identical and deterministic →
  **ambiguity = 0, novelty = 0** (Fisher term is 0 because a_d = a_b on every
  outcome). push puts mass 1 on `[:stop-the-line :none]`, so
  **risk = ln(1/(p_stop·g_none)) = ln(177405637081520/7144323575581)
  = 3.212120709595516**.
- The no-pick policy: q = {doable 0, blocked 1}, stopped kernel → same push,
  same risk; ambiguity 0; novelty 0 (no pick). Same number.

So on this field G is a constant: a function of the two preference kits and
one landing label. Which G terms carry any difference at all:

- **risk** — differs only through the landing label: ending class (relation
  channel) or, within a known ending, the gain from `:unblocks`.
- **ambiguity** — needs q-weighted kernel entropy > 0, i.e. kernel rows that
  differ, i.e. a landing ≠ stop (reach > 0 on a known ending).
- **novelty** — same precondition plus state-counts; also 0 today.

Routes to separation, with the Lean route closed (unknown relation →
stop-the-line is DarkTower law; amending it is Joe's call, not ours):

1. **Classify the relations.** The moment any candidate the cascades can pick
   has a known relation, its landing differs from stop, its kernel rows
   differ, and risk/ambiguity/novelty all separate — even between two
   policies picking the same candidate they would agree, but policies picking
   different candidates (e.g. proxy-metric via cone vs comprehension via
   unblocks, once the dead step is fixed) get different G. Concretely: refresh
   the scope census / relations corpus and declare a `:min-cosine` floor
   (embedding channel exists, `focus_receipt.clj:224-267`; blocked today on
   substrate-2 / port 7071 being down — same live witness owed as TIED-OUTER-CASCADES).
2. **Different statuses per pick.** A policy that picks :undetermined (today:
   structurally impossible, all excluded) would change q, but under a
   stop-landing q does not change G (both kernel rows are the same
   deterministic stop row) — so this route only separates once route 1 has
   opened landings. Worth saying plainly: **no route other than known
   landings separates G**, because a deterministic identical-kernel model
   makes ambiguity and novelty identically zero and push identical.
3. **Abstention preference.** The no-pick branch is scored with the same
   preference rows; if `:not-yet-evaluated` had positive mass, abstaining and
   acting would differ — but c-kit pins it at 0 (κ=100 mirror), i.e. another
   Joe/DarkTower decision, not a code fix.
4. **Per-candidate reach.** Reach weights landing vs stop; with a known
   landing, a per-candidate reach (e.g. from cone or an effort estimate)
   gives policies picking different candidates different risk. Today reach is
   the makeup's uniform 1/2 (`outer_observation.clj` giraffe-field docstring:
   "task reach is not yet produced by the runtime").

## (4) Is the cascades' evidence from Cascade Live actually read?

**No.** The three cascades are a hand-authored fixed list in
`data/wm-learning/outer-makeup.edn` (pinned, `:makeup/sha256` f3125835…),
each with a decorative `:patterns` string list. No component of the selection
path reads Cascade Live (lineage, clusters, arrows, held, patterns, tickets);
the only cascade-real references in futon2 src are the E-cascade-real probe
namespaces. Two further mismatches against
`cascades/select-tasks-from-cascade-live.flexiarg`:

- the pattern requires a cascade to *earn* its place from evidence of how work
  was actually selected, and requires each use to update that evidence;
  neither is implemented (nothing reads the witness catalogue, nothing writes
  back per-cascade outcomes);
- `:find-out-before-building` is implemented as a gate on `:undetermined`
  status, but the pattern is a **reshape** ("put a discovery task ahead of a
  build whose design depends on something unobserved"). The implementation is
  a different pattern (readiness inversion) and, on this field, a structurally
  empty one.

## (5) Proposed retained rationale + smallest separating changes

Proposed record addition `:outer/rationale`, all values already computed at
selection time (no new producers needed):

```edn
{:outer/rationale
 {:field {:pass-rows 609 :admitted 545
          :exclusions {:graph-term 47 :registry-policy 16
                       :duplicate-effective-run
                       [{:target "M-pattern-retrieval-calibration"
                         :matched-run "2026-11-01-c5590f95-…"}]}}
  :cascades
  [{:id :what-others-wait-on-first :patterns ["only-what-can-start-now" "what-others-wait-on-first"]
    :steps [{:step :ready-gate :before 545 :after 499 :narrowed-on "Giraffe :status = :ready"}
            {:step :by-cone :before 499 :after 499 :narrowed-on "cone desc — superseded by next step"}
            {:step :by-unblocks :before 499 :after 499 :narrowed-on "unblocks desc, field-order tiebreak"}]
    :pick "E-comprehension-foundation"
    :grounds "unique max :unblocks (2) among ready; cone-9 item M-interim-director-proxy-metric-inventory ranked 2nd (unblocks 1)"
    :deferred [{:target "M-interim-director-proxy-metric-inventory" :why "unblocks 1 < 2; highest cone (9)"}]}
   {:id :only-what-can-start-now :steps [ … :after 499 …]
    :pick "E-comprehension-foundation"
    :grounds "first :ready in field order after duplicate exclusion of M-pattern-retrieval-calibration (also unblocks 2, pass head)"}
   {:id :find-out-before-building
    :pick :no-pick :grounds "all 52 :undetermined rows are graph-unknown and were excluded pre-selection"}]
  :g-terms [{:policy :what-others-wait-on-first :risk 3.212120709595516 :ambiguity 0.0 :novelty 0.0
             :landing [:stop-the-line :none] :landing-cause {:relation :unknown :relation-receipt …}} …]
  :evidence {:giraffe-run "20261101T031213.797Z-cb8ff89c" :giraffe-manifest-sha256 "1b409b01…"
             :makeup-sha256 "f3125835…" :focus {:focus "WM" :origin … :as-of …}
             :relation-receipts […ochre ids…]}
  :sentence "Chose E-comprehension-foundation because it is the only ready item that unblocks two others (M-pattern-retrieval-calibration, which also unblocks two, was excluded as a duplicate of run c5590f95 on the same field); all three policies scored G 3.2121 because the target's relation to the focus is unknown, which scores as stop-the-line — identical risk, zero ambiguity, zero novelty."}}
```

Smallest changes that give the policies real evidence to differ on, in order
of leverage:

1. Refresh the relations/embedding scope census and declare a floor, so
   field heads classify (route 1 above; needs substrate-2 / 7071 live).
2. Fix the dead `:by-cone` step (make orderings compose as successive
   tie-breaks, or drop cone) — then cascades 1 and 2 genuinely order
   differently and would pick different candidates once landings are known.
3. Produce per-candidate reach (runtime term noted as owed in
   `outer_observation.clj`) so kernels differ once landings differ.
4. Implement `:find-out-before-building` as the reshape pattern (insert
   discovery tasks) rather than an undetermined-status gate, so its field is
   not structurally empty.
5. Retain the step traces (counts are computed by every `apply-selection-step`
   call and discarded today).
6. Mine the actual compositions from Cascade Live per the pattern's own
   NEXT-STEPS, so the cascade list earns its place rather than being
   hand-written in a pinned EDN.

## Closure condition status

Row closure = "each run retains why its target was chosen: each cascade step
and what it narrowed, the G terms per policy, and the evidence read". This
report is the discovery half; the packet (record-side retention of
`:outer/rationale`) is the implementation half and is **not** done here (this
row was dispatched as discovery, no source edits). No live witness is owed
beyond what TIED-OUTER-CASCADES already owes: the relations refresh is gated
on substrate-2 / port 7071 being up.

Defects named, outside this row, left for their owners:
- dead `:by-cone` composition step (`outer_score.clj:60-61` ordering semantics);
- `:find-out-before-building` implemented as a gate, not the reshape pattern;
- `:no-pick` retained with no reason (`outer_select.clj` cascade-picks);
- makeup `:patterns` decorative, no Cascade Live reader in the selection path;
- `M-interim-director` (and 51 others) graph-unknown with prerequisite
  evidence retained — the standing scope-census debt.

---

# Addendum — outer/inner parity and the library (zai-2, 2026-11-01, second requisition)

Joe: "how was a properly working outer selection not the 1st criterion?
futon3/library/task-selection/ exists, and I have insisted that the outer loop
be held to the same criteria regarding AIF as the inner loop."

## (a) The 19 patterns vs the three live cascades

The library (`futon3/library/task-selection/`, README + 19 `.flexiarg`, each
with `@guard`/`@effect`, written from ~100 witnesses in
`~/notes/selection-patterns/witnesses-2026-10-28.md`). The Lean vocabulary is
`R20Step.SelectionStep` (`mathlib4/DarkTower/WarMachine/R20Step.lean:88-104`)
with exactly five constructors: `.gate keep`, `.order key`, `.readyGate`,
`.byUnblocks`, `.byCone`; `SelectionCascade := List SelectionStep` (:104),
applied by `foldl` of filters and insertion sorts (:108-123).

**Which patterns reach live selection:**

| pattern (role) | reaches live selection? | how |
|---|---|---|
| only-what-can-start-now (gate) | **yes** | `.readyGate` / Clojure `:ready-gate` — the one faithful port |
| what-others-wait-on-first (order) | **yes** | `.byUnblocks` + `.byCone` (primary unblocks, cone secondary — see correction below) |
| find-out-before-building (reshape) | **degenerately** | not in the Lean vocabulary at all; the makeup invents `{:gate-status :undetermined}` (a Clojure-only generic gate), which is not the pattern (the pattern *inserts* a discovery task); NOTE-outer-G-computation §"What each term then means" intended it to fall out of the ambiguity/IG term, which is identically 0 under stop-landings |
| the other 16 (the-operator-asked, work-that-pays, hold-with-a-rearm-condition, repair-goes-to-the-front, spend-what-the-seats-have, end-in-something-shown, prepare-the-ground, take-the-cheap-win, finish-what-is-nearly-done, take-the-hard-one-to-learn, the-date-decides, halfway-house, bound-the-bet, write-it-down-instead, a-portfolio-not-a-winner, offer-a-menu-with-a-default) | **no** | nothing reads them; the makeup's `:patterns` strings are decorative |

**What stops the others composing into cascades — three structural walls:**

1. **The step vocabulary is filter+sort only.** `applySelectionCascade` is a
   left fold of filters and stable sorts over an existing candidate list. A
   cascade can narrow and reorder but can never *add* a candidate — so every
   *source* pattern (operator-asked, work-that-pays, held-with-rearm) and
   every *reshape* pattern (find-out-before-building, halfway-house,
   bound-the-bet, write-it-down-instead) is inexpressible by construction.
2. **The candidate record carries almost nothing.** `{:target t :reach 1/2}`
   (`outer_observation.clj` giraffe-field). No operator asks, no hold/rearm
   reasons, no seat or budget availability, no cost, no external dates, no
   resolvedness/progress, no prior per-target outcomes. A gate needs its
   `@guard`'s evidence in the record; none of these guards can read.
3. **Facts are three Giraffe numbers.** `status`, `unblocks`, `cone`. The
   generic `.gate`/`.order` constructors could express more, but no producer
   supplies a predicate or key over anything else — Cascade Live (lineage,
   clusters, arrows with delta-g, held, patterns, tickets) has no reader in
   the path (see main report §4).

## Correction to the main report (§2 "dead :by-cone defect")

✘ I withdraw "dead `:by-cone` composition step" as a defect. The Lean
comment (`R20Step.lean:126-127`) states the intended semantics: "`byUnblocks`
is last because the last stable sort is the primary key" — unblocks primary,
cone the stable secondary/tie-break, exactly what my replay reproduced. The
step is not dead; it is a tie-break. What stands: the record does not retain
the key order or the grounds, so the composition reads as unexplained — a
rationale deficit, not a scoring defect.

## (b) Parity table — inner vs outer, per active-inference obligation

Starting points: `NOTE-outer-G-computation-2026-10-28.md`;
GRID v2 stated limits (reach defaults 1/2; learning yield has no producer,
stays :none; cascade menu = the makeup list).

| obligation | inner loop (read from code/records) | outer loop (read from code/records) | parity |
|---|---|---|---|
| policies **constructed** from patterns | CONSTRUCTED: `interpretation_construction.clj:171` `construct` — backward search from the wants over the mission's interpretations (guard/produces), minimal support sets only, candidates carry pattern ids and precedence; prior from pattern evidence (`cascade_feedback/pattern-evidence-prior`). Joe's own wording applies: "the cascade should be constructed not chosen" | FIXED LIST: three hand-authored cascades in `data/wm-learning/outer-makeup.edn` (pinned sha f3125835…), `:patterns` decorative; nothing constructs a cascade for the click; nothing reads the library or Cascade Live | **outer fails** |
| G with risk, ambiguity, information gain | live: `CascadeEFE.stepG` risk+ambiguity with novelty term (`PatternThetaNovelty`/`CascadeCoApplyGNovelty` in Lean; g-terms retained per candidate in certificates, `cascade_efe_sut.clj:173-174`) | implemented (`outer_score.clj:185-196`) but **degenerate in fact**: run ac5bc477 — risk-only 3.212120709595516 for all three policies, ambiguity = novelty = 0 (stop-landings); G is a constant of the kits, blind to the pick | **outer fails live** |
| preferences learned | yes — slice 1b learned C, Smith–Friston–Whyte preference update (κ) in live use | kits learn (c-kit/gain-kit/state-kit) and were consumed live once (E2 closed 2026-10-30, run 3415b671); the learning-yield preference Joe specified (NOTE-outer-G) **has no producer** — every click observes yield `:none` (GRID v2 stated limit) | **outer partial** |
| beliefs updated from outcomes | yes — per-run belief update, consumed across clicks | live once (E2); most runs defer (`terminal-ending-class-absent`) — honest non-consumption, not learning | **outer partial** |
| prediction frozen and scored | **not exercised** — GRID B4 row: "Inner prediction was not exercised"; outer-only closure recorded | CLOSED outer-only (B4): `:outer/selection :prediction` frozen before effects on runs f74df9cc, 347b9b51, 5ba921b9, 376d76ff, ff4b4c5c | **outer ahead** |
| precision | live — `ChannelPrecision.lean`; precision-weighted inner scoring | makeup carries ω/η per kit and `:precision-iterations 16`, but they parameterise the posterior updates only; **no precision term in outer G**, no per-policy precision | **outer fails** |
| learning (from use, back into the policy evidence) | contested live — pattern feedback file read at construction, but Joe ruled it "a mistake" (learning lives in the connected pattern graph); per-pattern trials record-only | kits update, but the cascades themselves never update: per `select-tasks-from-cascade-live.flexiarg` THEN, each use should update the evidence for each selection cascade — nothing writes or reads per-cascade outcomes | **both fail; outer worse** |
| rationale retained | strong — construction receipts, per-candidate certificates with g-terms, `selection_cascade_identity.clj:64-77` selection-law receipts, execution journal | scores, picks, tie flags only; no step traces, no grounds, no evidence read, no sentence (this row) | **outer fails** |

Net: of eight obligations the outer loop meets one fully (frozen prediction),
two partially (learned preferences, outcome-updated beliefs), and fails five.
The failures share one root: the outer loop was given the inner loop's
*scoring* without the inner loop's *construction* — policies arrive fixed
rather than constructed from patterns against live evidence.

## (c) Draft outer criteria (for the GRID, to sit first)

O0 (ordering principle, Joe 2026-11-01): outer selection working properly is
the first criterion; the outer loop is held to the same AIF criteria as the
inner loop. No inner policy exists before the task is selected (standing
rule).

O1 CONSTRUCTED POLICY MENU. Each run's selection cascades are constructed for
that click from the pattern library (`task-selection/` guard/effect patterns)
against live evidence (Cascade Live), each naming its patterns and the
witnesses that earned them; a fixed hand-authored cascade list is a violation.
Evidence: the menu's construction trace in the run record, with library pins.

O2 GUARDS CAN READ. Every candidate carries the evidence the composed
patterns' guards read (status, unblocks, cone, hold/rearm reasons, operator
asks, seat/budget availability, cost, dates, resolvedness, prior outcomes).
A guard whose evidence is absent is a named hole in the record, never a
silent narrowing. Evidence: per-candidate fact keys vs pattern `@guard`
inventory; every absent channel named.

O3 EXPRESSIVE STEPS. The step vocabulary can express what the library's roles
need — gate, order, source (add candidates), reshape (insert derived tasks) —
or each inexpressible pattern is recorded out-of-scope with a reason.
Evidence: the Lean `SelectionStep` vocabulary vs the library role table.

O4 G SEPARATES ON EVIDENCE. G over policies carries risk, ambiguity and
information gain that can differ between the live policies. A run where all
policies score the same G with zero ambiguity and zero novelty is retained as
a **disclosed degeneracy** naming the input that would separate them (on
ac5bc477: relations unknown → stop-landings). Evidence: per-policy
risk/ambiguity/novelty decomposition in the record.

O5 PREFERENCES PRODUCED. The ending/gain/state kits update from outcomes
(retained), and the learning-yield preference has a typed producer (pattern/
cascade introduction step per NOTE-outer-G) — until it exists, every run that
could have been influenced by yield records the limit. Evidence: producer
receipts or the typed absence, per run.

O6 PREDICTION INCLUDES SELECTIVE POWER. The frozen outer prediction (B4, keep)
is extended with the selection's own prediction: which input would have
separated the policies, scored after the run against what actually changed.
Evidence: prediction before effects; post-run scoring of the named separator.

O7 USE UPDATES POLICY EVIDENCE. Each run's outcome updates not only the kits
but the evidence for the cascades themselves (which way of selecting won), per
`select-tasks-from-cascade-live` THEN. Evidence: per-cascade outcome ledger
read by the next construction (O1).

O8 RATIONALE RETAINED (this row's closure condition, unchanged): per-cascade
step traces with candidate counts and the fact each step narrowed on, G terms
per policy, the evidence read (Giraffe run + manifest, makeup sha, focus and
relation receipts, effective-run pins), and one sentence a person can read.

Relation to existing GRID rows: B3 asked only that cascades be *compared*
with the declared G; O1-O4 hold the comparison to the inner loop's standard
(construction, readable guards, expressive steps, live G terms). B4 outer-only
closure stands; O6 extends it. E2/O7 overlap cleanly: E2 is state
consumption, O7 is policy-evidence consumption.
