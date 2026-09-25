# H-C-REACH-D — served-by links beyond one mission

Hole H-C-reach of `PROOF-2a-THEOREM-draft-2026-09-24.md` (Holes row L524,
owners row L624; opened on H-VALUE-T-D §2). Author: claude-13, 2026-09-25.
Discovery only: no code edits, no `data/` writes, no shared-JVM loads. One
script, run in its own process: `h_c_reach_d.clj` beside this file (sha256
`15ec7f74…`), output committed as `h-c-reach-d-output.txt`.

| input | sha |
|---|---|
| PROOF-2a draft | futon2 bf5ba478 |
| `H-C-DEF.md` §2 (L60-115), §4 E6 rule (L146-194) | 2ada0a9b |
| `scripts/wm/extract-outcomes.clj`: `instance-sections` L217-238, `coupling-artefacts` L292-320, `section-mention` L330-349, `artefact-links` L351-379 (sha256 `6f0e99be…`) | a31f9cee |
| `test/futon2/wm/extract_outcomes_test.clj`: the 13 reference mappings L319-332 | a31f9cee |
| target-field fixture `target-field@futon2-7bd17dfb.edn` (sha256 `760d5b31…`) | 6d2b39a7 |
| M-futon-seams at the test's pinned sha256 `d13c5cfe…` | futon3c 3f5f44dd |

Every target text was read at the repo head the fixture recorded, using
`git show`.

## 1. Why zero

The E6 rule, as coded, links outcome o to instance i when three conditions
all hold:
- (i) one of o's cues matches an entry's `:obstacle-res`;
- (ii) i's section has a sentence matching the same entry's `:mention-res`
  and one of its `:direction-verbs`;
- (iii) i is an `### n.` section in the extractor's shape.

The shape check (L226-229) looks for an `## …instances` heading to bound the
search. **When no such heading exists, it scans the whole file**
(`lo 0`, `hi` end). So on these 23 targets every `### n.` heading anywhere
counts as an instance.

| target | `### n.` sections (titles, first two) | admitted outcomes | (i) vocabulary | (ii) mention | (iii) shape |
|---|---|---|---|---|---|
| M-zaif-harness | 5: "Perception is a query library", "Queries are data…" | 0 | no outcome | miss | no anchor: numbered argument |
| M-autoclock-in | 4: "Implementation, runtime evidence…", "Last-seven-day coverage" | 0 | no outcome | miss | no anchor: diagnosis findings |
| M-weird-modernism | 8: "The two questions ARE…", "PKD's methodological…" | 0 | no outcome | miss | no anchor: reading notes |
| M-apm-demonstration | 11: "0. What we are building…", "1. Why this shape…" | 13 | miss (0 obstacle hits anywhere) | miss | no anchor: design subsections |
| M-mission-coherence-patterns | 6: "Invariance under user/operator…" | 0 | no outcome | miss | no anchor |
| E-live-loop-3 | 4: "Where the candidate/open-mission set comes from" | 0 | no outcome | miss | no anchor: proposal steps |
| E-memory-latency | 4: "Candidate construction can make two FTS calls" | 0 | no outcome | miss | no anchor: findings |
| E-close-S6 | 4: "Freeze the preregistration", "Bind the move…" | 1 | miss (0) | miss | no anchor: protocol steps |
| E-war-machine-qa | 7: "The two D&G `:partial` rows" | 0 | no outcome | miss | no anchor: open items |
| E-port-wiring-map | 4: "`:17070` is a tunnel…" | 0 | no outcome | miss | no anchor: confusions |
| E-R6-red-ring-fill | 4: "The generative proposer generates…" | 0 | no outcome | miss | no anchor: mechanisms |
| E-cascade-assembly | 8 (numbering restarts 1-3, 1-3, 1-2) | 0 | no outcome | miss | no anchor: three result lists |
| E-R14-red-ring-fill | 5: "Run slice 2 before slice 1…" | 1 | miss (0) | miss | no anchor: strategy notes |
| E-jax-demonstrators | 2: "Predictive structure…" | 0 | no outcome | miss | no anchor |
| E-loss-function-shape | 4: "COST — what was spent" | 0 | no outcome | miss | no anchor: bands |
| E-pipeline-pipecleaner | 3: "The pipeline prototype was a self-test…" | 1 | miss (0) | miss | no anchor: checkpoint findings |
| E-globe-6-closure-rubric | 5: "0. Open", "1. Stepped" | 0 | no outcome | miss | no anchor: rubric levels |
| E-inbox-zero-implementation | 6: "Session seat", "File observation" | 1 | miss (0) | miss | no anchor: data spec |
| E-candidate-queue-xtdb-shape-v0 | 3: "Canonical queue item doc" | 1 | miss (0) | miss | no anchor: doc shapes |
| E-btoa-evidence-sample-mapping-v0 | 4: "`cross-store-agreement` is visible…" | 0 | no outcome | miss | no anchor |
| E-first-flights-policy-grade-G-closure | 4: "Test suite re-run…" | 0 | no outcome | miss | no anchor: verification steps |
| E-strategic-bridge-motif-comparison-v0 | 6: "Packaging motif" | 0 | no outcome | miss | no anchor: motif types |
| E-first-flights-typed-grounds-tail-closure | 6: "The sorry-arrow…" | 0 | no outcome | miss | no anchor: census |

**Every row fails all three conditions.**
- **(ii)** None of the 23 has a sentence in a section matching any
  `:mention-res`.
- **(i)** None of the six outcome-stating targets has a cue matching any
  `:obstacle-res`. The five entries' obstacle patterns match nowhere in
  those six texts: 0 hits in the whole file, not only in cues.
- **(iii)** None has an instances anchor. Their `### n.` sections are
  numbered findings, steps, bands or rubric levels. They have no cascade and
  carry no wants. So "the sentence that came closest" does not exist for
  (i) or (ii).

The zero is correct as a number. But the 23 "instance sections" it is
computed over are an artefact of the anchor fallback (L228).

The six outcome cues, and what each names in plain words (full quotes in
the output):

| target | cue (start) | what it names |
|---|---|---|
| E-close-S6 | "- local handler-driven execution of `POST /api/alpha/portfolio/step` … observation source carried `THE-STACK.aif.edn`" | the conditions of one run: an endpoint, an agenda id, a file. An evidence sentence (H-C-DEF §2, true before and after) |
| E-R14-red-ring-fill | "The nearest are WR-26, whose BECAUSE weighs *'a stranded flag costs more…'*" | two ledger rules (WR-25, WR-26) compared. A citation, not a world state |
| E-pipeline-pipecleaner | "- a linear roadmap can be useful … otherwise the line is just a projection" | a roadmap needing a semilattice attachment point. A method rule |
| E-inbox-zero-implementation | "- Renames preserve association only when the watcher has a witnessed rename…" | a file watcher's rename rule. A spec clause |
| E-candidate-queue-xtdb-shape-v0 | "This is the record Arxana can read when it wants a ready-to-render queue surface…" | a queue record Arxana reads. Closest to an outcome: a party (Arxana, a tool) and an artefact (the record) |
| M-apm-demonstration (13) | e.g. "Path formation is measurable directly — the distribution of `used/offered`…"; a code block of `:reg/escalation` keys; "### First, the cost of tuning role cards"; "Merged (`see log`) but NOT reloaded" | a measurement, a config map, a heading, a merge log. Several fail H-C-DEF §2 on their face |

Five of the six targets carry one "outcome" each, and it is an evidence,
method or spec sentence. M-apm-demonstration's 13 include a heading and a
merge note. Off the mission it was built against, the outcome reader admits
text the four-clause definition would reject. The measured served-by zero
therefore rests partly on outcomes that are not outcomes. That is H-C's
precision finding, reported to its owner, not fixed here.

## 2. Vocabulary as data, from the field

Two options were measured on the control first, then on the field.

**Option A — new entries of the same shape.** The field supplies no
candidates. An entry needs an artefact named both as the obstacle in an
outcome cue and in a want's unit with a direction verb. Among the 23 texts
the only cue artefacts are:
- `used/offered`, `:reg/escalation` keys, `:cycle/outputs`, `:reviewer`,
  and a merge sha (M-apm-demonstration);
- `THE-STACK.aif.edn` and a portfolio endpoint (E-close-S6);
- "ready-to-render" (E-candidate-queue).

None recurs in a numbered section as something acted on. Anything added
from these would be an entry tuned to one target, which the bar excludes.
**Bad case for A:** an entry written from one target's text links that
target and no other. The control stays 10/13 only because M-futon-seams's
five entries stay.

**Option B — a rule predeclared only in its shape.** An artefact is a token
of one of three lexical shapes:
- a backticked code span;
- a file name with a source extension;
- a hyphenated identifier of three or more parts.

It must occur in an outcome cue and in a unit sentence carrying one of 14
generic direction verbs (`direction-verbs` in the script; no entry names an
artefact). Measured:

| rule | M-futon-seams recall (13 mappings) | misses | links | links outside the reference pairs |
|---|---|---|---|---|
| A (current) | **10/13** | [4 :caller-converted :o-2] [4 :redirect-test :o-2] [5 :impersonation-retired :o-4] | 8 | 0 |
| containment baseline | 3/13 | 10 | 4 | 1 ([8 :o-3]) |
| **B (shape only)** | **0/13** | all 13 | 0 | 0 |
| A ∪ B | 10/13 | the same three | 8 | 0 |

B fails the control: M-futon-seams's outcomes name their artefacts in prose
("the provider out of the agent id", "impersonate the first", "a hardcoded
prompt"), never as code spans. On the field B links 1 of 23 targets
(E-close-S6, unit 1, shared `THE-STACK.aif.edn`), and that link is false:
- the "direction verb" is `\bbind`. Its first hit is inside the file name
  `M-recommendation-bindings.md`, and its other hits are list labels
  ("binds spine: `:S6`"). The "sentence" is a preregistration record that
  the sentence splitter ran through a bullet list with no blank line
  (futon5a 1e4ab8d7 `holes/excursions/E-close-S6.md` L63-71). Nothing in
  it acts on `THE-STACK.aif.edn`;
- the "outcome" is an evidence bullet (§1).

**Bad case for B:** a shape rule loose enough to link prose artefacts also
links every section that mentions a shared file. Tightened to code tokens,
it loses the control. Neither side of that trade holds.

**What the field supports.** Neither option. The artefact in E6 is named in
prose, as H-C-DEF §4 condition 1 says: "a file, a protocol, a convention, a
flag, a provider string". Condition 3 ("w's enactment removes the obstacle
o's cue states") is an argument (the reference's `:how` paragraphs), not a
match. Missing: a definition of *artefact* off-mission that a predeclared
lexical table can carry.

What the texts do support is making the naming a reading step, the way
H-interp made interpretations one. A reader proposes (want, outcome,
artefact, both spans). The existing check then verifies:
- both spans resolve;
- the artefact string occurs in both;
- the want-side sentence carries a direction verb.

The five entries become the worked example of what such a reading returns.
This proposes no new field: the link's `:via` already carries `:artefact`,
`:want-span`, `:outcome-span` and `:direction` (L373-377). Its case under
the no-new-field rule: it removes the per-mission vocabulary a human must
write before any mission can have a served-by link. Not measured here,
because a reading needs a seat.

## 3. Shape

`### n.` does not hold as an *instance* shape outside M-futon-seams.

| census | the 23 | seeded sample of 40 of the other 320 (seed 20260925) |
|---|---|---|
| `## …instances` anchor | **0** | **0** |
| numbered `### n.` headings | 117 in 23 targets | **0** |
| headings containing "instance" | 2 in 1 target | 0 |
| lifecycle phase headings | 47 in 5 | 74 in 26 |
| numbered bullets | 545 in 20 | 386 in 35 |
| checkbox lines | 0 | 30 in 5 |
| table rows | 1351 in 16 | 526 in 24 |
| `**Exit criteri…` paragraphs | 3 in 1 | 0 |

Instances as M-futon-seams has them (sub-targets, each with a cascade file
and wants) appear on no other feasible target. At target grain the flight
has one target, and its wants are the target's criteria (Wₜ's
`:ask-interpretation` names each want with its criterion line). So the unit
a served-by link needs at target grain is the want's criterion span, not a
section. As data:

```edn
{:want-units
 [{:id :unit/instance-section :requires :instances-anchor
   :reads "### n. under ## …instances; absent anchor -> {:absent :no-instances-anchor}, not the whole file"}
  {:id :unit/criterion-line
   :reads "the want's criterion line as the criteria reader cued it (:line, :criterion)"}]}
```

Two things the second unit changes:
- **Anchor absence.** An absent anchor becomes a typed absence, not a
  whole-file scan. Case: it removes 117 false instance units on 23 targets,
  which the served-by zero was computed over. This is data, not a refusal.
- **Criterion-line unit, measured.** On the 12 targets with named wants,
  rule A gives 0 links: 6 have no outcome, and on the other 6 the five
  entries match nothing. Rule B also gives 0.

## 4. Falsifiers for the implementation

| id | case | must observe |
|---|---|---|
| a | M-futon-seams at `d13c5cfe…` under the generalised rule | 10/13 with exactly [4 :caller-converted :o-2], [4 :redirect-test :o-2], [5 :impersonation-retired :o-4] missed; 0 links outside the reference pairs; containment 3/13 beaten. Rule B as measured fails this (0/13) |
| b | remove one vocabulary entry (or one read link) | the links citing it disappear and no others change |
| c | the record | carries the vocabulary's sha (or the reading's receipt) and the want-unit id (`:unit/instance-section` / `:unit/criterion-line`) |
| d | an outcome cue naming an artefact no unit mentions | links nothing, `{:absent :no-shared-artefact}` as now |
| e | the field re-measured | under A: 0 of 23 gain a link. Under B: 1 of 23, and false (verb hits inside a file name and list labels). Under criterion-line units: 0 of 12 |
| f | a direction verb found only inside a file name, identifier or list label, or in a "sentence" that spans a bullet list | not a direction (B's one field link) |
| g | a file with numbered `### n.` headings and no instances anchor | yields no instance units (today it yields them; 23 targets) |
| h | an "outcome" that is a heading, code block or merge note (M-apm-demonstration) | the served-by result states that its outcomes came from the reader. Their precision is H-C's to fix, not the link rule's |

## Answers, one line each

1. On all 23, (i), (ii) and (iii) all fail: no obstacle or mention pattern
   matches anywhere, and no instances anchor exists, so the `### n.`
   sections are numbered prose picked up by the anchor fallback (L228). The
   six outcome cues are evidence, method or spec sentences plus one tool
   ask (Arxana), none naming a coupling.
2. The field supports neither: A has no field candidates that aren't tuned
   to one target; B (lexical shape only) gives 0/13 on the control and 1
   false link on the field. Missing: an off-mission definition of
   artefact. Supported: the naming as a reading step, checked by the
   existing `:via` spans.
3. `### n.` instances exist only on M-futon-seams (anchor 0/23 and 0/40;
   numbered `### n.` 0/40). The target-grain unit is the want's criterion
   line; the anchor fallback should give a typed absence.
4. (a)-(e) as asked, (e) measured 0/23 (A), 1/23 false (B), 0/12
   (criterion units); plus (f) verb in an identifier, label or list-spanning
   sentence, (g) no-anchor, (h) outcome precision.
