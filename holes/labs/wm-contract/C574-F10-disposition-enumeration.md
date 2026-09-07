# C574 — F10 slice 2 (census): the ruled outcome domain names fourteen dispositions, and no artifact enumerates fourteen

**Row.** `:F10`, `:loop-mode :one-slice-per-invocation`. The next slice named in
`:progress` is "Lean carrier instantiation". This sheet is the discovery half of
that slice: the carrier cannot be written until it is known which names go into
`Obs organisations`, and the census says the ruling's number is not attested
anywhere.

**What this sheet is not.** Not a ruling. It records code-backed facts with
pointers and hands the choice back. Nothing in `aif-equations.edn` or
`control-map-edges.edn` is written by this slice, and no Lean file is touched.

---

## 1. What the ruling asks the carrier to declare

`aif-equations.edn :choices :outcome-domain :ruling :decision` (Joe, 2026-09-07):

> `Obs organisations` := the FOURTEEN terminal flight dispositions, the nine
> never-yet-observed carrying NAMED ZERO MASS (only 5 of 14 occur in the
> 82-attempt corpus, `runs/D1-evidence/kl-worked-example.edn`)

So the carrier needs a fourteen-constructor type, five of whose constructors are
inhabited by the corpus and nine of which carry a declared zero.

## 2. The five observed are settled

`runs/D1-evidence/kl-worked-example.edn:6` — `:outcomes [:agent-unavailable
:build-failed :grounded-change :incomplete :no-selection]`, recomputed from
`data/wm-full-loop` (86 attempt dirs; 68 (π,o) pairs). No dispute here.

## 3. The fourteen is not attested — the origin enumerates sixteen

The number traces, through every citation I could find, to one note.

**Origin.** `p4ng/empirics-futon/NOTE-the-flight-is-where-G-lives.md:16-21`
(commit `ceb20a7`, 2026-08-27T1456Z) says "names **fourteen** flight outcomes"
and then prints the list. The printed list has **sixteen** entries:

    :ok  :grounded-change  :grounded-no-change  :no-selection  :artifact-only
    :incomplete  :build-failed  :construction-failed  :grounding-failed
    :dispatch-failed  :agent-unavailable  :substrate-unavailable
    :guardrail-refusal  :policy-nondiscrimination  :cohort-complete  :error

The word and the list disagree in the same paragraph. The list is the accurate
part: it is exactly what `full_loop_runner.clj` names today — fifteen distinct
`:outcome :<kw>` literals plus `:grounded-change`, which the runner produces as a
bare value at `src/futon2/aif/full_loop_runner.clj:3095` rather than under an
`:outcome` key. Sixteen, verified by set comparison, not by eye.

**Propagation.** The number, not the list, is what travelled. It reaches
`futon2/holes/NOTE-pattern-as-production-rule-and-Q.md:174-178` (which re-states
"fourteen" and prints eight names followed by an ellipsis), then
`holes/problems/P-validated-R5.md:129-131` and `:142`, `:596`, `:651`,
`holes/problems/P-markov-category-spec.md:126`,
`R5-glossary-formalisation.md:117`, `R8-glossary-formalisation.md:113`,
`facts-R5.md:195`, and finally `RULINGS-walkthrough-2026-09-07.md:88` and the
ruling itself. Twelve documents carry the number; **not one enumerates fourteen
names.** Searched: `grep -rn 'fourteen flight|fourteen terminal|14 flight|14
terminal|fourteen dispositions' holes/ src/` over futon2 — every hit is the
number without a list, except the origin, which has a list of sixteen.

**The origin's list is also internally inconsistent with its own bounds.** The
same note disqualifies two of its own entries: `:ok` at `:85` ("does not appear
as a terminal disposition — it is a step outcome"), and `:cohort-complete` is
disqualified by the code it cites — `src/futon2/aif/full_loop_cohort.clj:28-30`
says it "is NOT here — it is a scheduler-level signal returned by
`run-opportunity!` when the cohort is exhausted, not an attempt outcome."

## 4. The one closed vocabulary in code has twelve members

`src/futon2/aif/full_loop_cohort.clj:25-33`, `outcome-kinds`, docstring "Valid
outcome classifications for full-loop ATTEMPT close checkpoints":

    :grounded-change :grounded-no-change :artifact-only :abstained :no-selection
    :agent-unavailable :guardrail-refusal :dispatch-failed :build-failed
    :substrate-unavailable :incomplete :cancelled

Twelve. This is the set the machine actually gates on, not a prose list:

- `src/futon2/aif/full_loop_cohort.clj:317` — membership failure raises
  `:unknown-outcome` on an attempt's close checkpoint;
- `src/futon2/aif/tripwire.clj:189` — `(contains? cohort/outcome-kinds outcome)`;
- `src/futon2/aif/realized_outcome.clj:28` — the one realized-outcome schema
  names its categorical field's vocabulary as `full-loop-cohort/outcome-vocabulary`.

All five observed dispositions (§2) are members. Twelve minus five leaves
**seven** unobserved, not nine.

## 5. The two sets differ in both directions

Computed, not eyeballed:

- in the runner's sixteen but not in `outcome-kinds` (6): `:cohort-complete`,
  `:construction-failed`, `:error`, `:grounding-failed`, `:ok`,
  `:policy-nondiscrimination`;
- in `outcome-kinds` but never a runner `:outcome` literal (2): `:abstained`,
  and `:no-selection` — `:grounded-change` is the third name the set difference
  reports, but it is produced at `full_loop_runner.clj:3095` as a bare value, so
  it is a grep artefact, not a real absence.

So this is not one vocabulary read two ways. The runner emits six terminal-ish
names the cohort checker would reject as `:unknown-outcome`, and the cohort
vocabulary admits `:abstained` which the runner does not emit under that key.

## 6. The ruling's arithmetic does not pick a set either

Fourteen-with-nine-unobserved is satisfiable — but not uniquely. Take the twelve
of §4 and add any two of the six runner-only names of §5 that are not observed:
every such pair gives fourteen members and nine unobserved. There are fifteen
such pairs. The pair `{:ok, :policy-nondiscrimination}` reproduces the eight
names printed at `NOTE-pattern-as-production-rule-and-Q.md:175-177`, which makes
it the reading closest to the propagated text — and `:ok` is exactly the name the
origin note itself disqualifies at `:85`.

So the numbers in the ruling do not identify the domain, and the closest textual
reconstruction is the one its own source rules out.

## 7. Why this blocks the Lean carrier rather than being routed around

The F10 row says building the carrier before the domain is ruled "would be
choosing the domain by implementation". That is precisely the move available
here: write `inductive FlightDisposition` with twelve constructors, or sixteen,
or a fourteen assembled to fit the arithmetic, and the choice is made by whoever
typed it. The consequences are not cosmetic:

- **Named zeros are a support claim, not padding.** The ruled design puts zero
  mass on the unobserved constructors. `predictiveOutcomeRisk`
  (`mathlib4/DarkTower/WarMachine/Holes.lean:6993-6997`) takes
  `_positivePreference : ∀ π o, o ∈ Q.support π → 0 < Cdist.mass () o`
  at `:6995`. Every
  named zero is therefore an assertion that the constructor stays outside every
  `Q.support π` — a claim about the Q that has not been built yet. Nine such
  claims versus seven is nine versus seven obligations, and which names they
  attach to decides whether the obligation is dischargeable at all.
- **`:policy-nondiscrimination` is load-bearing if it is in.** The origin note
  calls it "the singularity criterion, already an outcome"
  (`NOTE-the-flight-is-where-G-lives.md:30-31`) — a flight that ended because
  nothing discriminated. A domain that includes it can express a preference
  against non-discrimination; a domain built from `outcome-kinds` cannot, because
  the cohort checker rejects it.
- **`:ok` and `:cohort-complete` are grain errors if they are in.** One is a step
  outcome, one is a scheduler signal. Including either puts a non-flight event in
  a per-flight domain, which is the grain error Joe already corrected once
  (`NOTE-pattern-as-production-rule-and-Q.md:171-172`: "I measured the tick and
  concluded about the policy").

## 8. What is needed to unblock

One line from Joe fixing the enumeration. The three candidates, stated so the
answer can be short:

- **(i) the twelve of `outcome-kinds`** — the set the machine gates on; seven
  named zeros; cannot express `:policy-nondiscrimination`;
- **(ii) the twelve plus `:policy-nondiscrimination`** — thirteen, eight named
  zeros; keeps the singularity criterion, adds one name the cohort checker
  currently rejects (a code change is then owed, or the domain is knowingly wider
  than the checker);
- **(iii) the origin's sixteen minus its own disqualified members** — sixteen
  less `:ok`, `:cohort-complete` and `:error` is thirteen. Also thirteen, but not
  the same thirteen as (ii): (iii) carries `:construction-failed` and
  `:grounding-failed`, which (ii) lacks, and lacks `:abstained` and `:cancelled`,
  which (ii) carries. Choosing between them is choosing whether the two
  build-stage failure names are their own dispositions or fold into
  `:build-failed`.

There is no fourteen among them. Whatever is chosen, the ruling's "fourteen …
nine" is owed a dated correction in `aif-equations.edn :choices :outcome-domain`,
because the arithmetic it quotes is inherited from a miscount and not from the
corpus.

## 9. Pointer drift found while doing this, recorded not repaired

The C538 sheet and the F10 `:progress` cite `Holes.lean:151-152` for the
per-vertex `C` and `:6778-6780` for `PreferenceDistribution`. At mathlib4
`167c32668e` those are `:153` (the `C` definition, with its refusal docstring at
`:152`) and `:6905-6906` (docstring `:6904`) respectively; `machineHasNoC`, cited
at `:7117-7120`, is at `:7243` (docstring `:7242`). The per-vertex/single-kernel disagreement C538 §3 describes is
unchanged — only the line numbers moved.

## Scope limits

Census only. Nothing was run, no registry or Lean file was written, and the
`Q(o|π)` gap named in the ruling's `:consequence` (Q over dispositions must still
be built; F1's blocker is B's dependence on u) is untouched by this sheet.

## 10. The fourteen, traced (claude-1, 2026-09-07 evening — dated follow-up)

Joe declined to choose among section 8's candidates, and correctly: "Either
we can trace it properly. Or we can't. So, I'm not going to guess." Traced:

The origin note (p4ng ceb20a7, sole commit, 2026-08-27T14:56Z) was BORN
saying "fourteen" over a sixteen-name list — the list never grew under the
sentence. But the note names its source: `full_loop_runner.clj`. At the
futon2 revision current when the note was written (453cedfa,
2026-08-27T12:27Z), `grep -o ':outcome :[a-z-]*' | sort -u` over that file
returns EXACTLY FOURTEEN names:

    :agent-unavailable :artifact-only :build-failed :cohort-complete
    :construction-failed :dispatch-failed :error :grounded-no-change
    :grounding-failed :guardrail-refusal :incomplete :ok
    :policy-nondiscrimination :substrate-unavailable

The note's printed sixteen is exactly these fourteen plus
`:grounded-change` and `:no-selection` — two genuine outcomes that reach
records via variables (e.g. full_loop_runner.clj:3096 selects between the
grounded pair), so a literal grep misses them. **The "fourteen" is a count
of literal keyword occurrences, not of a vocabulary.** It excludes two
real outcomes for a syntactic reason and includes `:cohort-complete`
(scheduler-level, disqualified by full_loop_cohort.clj's own docstring),
`:ok` (disqualified by the origin note itself at :85) and `:error`. No
reading of it is a domain; section 6's fifteen candidate sets were
readings of an artifact.

Consequence (per Joe's trace-or-clear instruction, no enumeration
judgment made by anyone): the enumeration authority for Obs organisations
is the machine's own gated vocabulary, `outcome-kinds`
(full_loop_cohort.clj:31-33, TWELVE members, the set enforced at :317 and
tripwire.clj:189). The five observed dispositions are all members; SEVEN
carry named zero mass. The ruling's "fourteen ... nine" receives a dated
correction in `aif-equations.edn :choices :outcome-domain` citing this
section; the support widens later only by a change to the gate itself,
which is a machine edit with its own review, not a registry edit.

Recorded, not resolved here: the runner at HEAD can emit literal outcomes
the cohort gate does not admit (`:policy-nondiscrimination` among them),
and July-era morning-brief items carry outcomes from an older runner
(`:agent-job-stalled`, `:agent-job-timeout`) outside both sets. That
runner/gate seam is a code-consistency finding for a worklist row, not
part of the enumeration.
