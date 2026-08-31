# NOTE — a pattern as a production rule, and Q(o∣π)

**Written:** 2026-08-29 by claude-13, at claude-15's request during the
M-formal-war-machine review. **Rewritten the same day from the recovered
original.** The first version of this file was written from inference because the
source turns were missing; that version is superseded and its inferences are
marked as withdrawn in §6.

---

## 0. Provenance — the original was found

The discussion Joe remembered is **claude-13's own, 2026-08-27, roughly
12:40Z–15:12Z** — not 08-28, and not lost. The 2026-08-28 compaction dropped it
from the session transcript, but the turn texts survived in
`/tmp/futon-invoke-artifacts/` and claude-15 copied all 73 of that day's turns,
unmodified, to:

    futon2/holes/labs/wm-contract/claude-13-repl-turns-2026-08-27/

Six turns carry it: **T1240Z, T1402Z, T1431Z, T1451Z, T1456Z, T1501Z, T1512Z**.
Every quotation below is from those files, cited by timestamp.

**Why it was not found first time.** It is written throughout as `Q(o∣π)` with
the Unicode DIVIDES character `∣` (U+2223), not `|` and not `\mid`. Searching for
the ASCII forms returns nothing.

**One limit on the record.** These files are claude-13's *turn texts* — the
assistant side only. Joe's inputs are not preserved in them. So where his
positions appear below they are **as I recorded them at the time**, in my
sentences, not verbatim. That distinction is kept explicit; nothing here is
presented as a Joe quotation unless it is one.

---

## 1. A pattern as a production rule — SETTLED, and measured

A library pattern (a *flexiarg*) carries `IF` (antecedent), `HOWEVER` (forces),
`THEN` (action), `BECAUSE` (rationale). Stated at **T1402Z**:

> *"A flexiarg gives IF (antecedent), HOWEVER (forces), THEN (action), BECAUSE
> (rationale). That's a **production rule**; G(π) needs a **generative model**.
> The library says what to **do** and never what you would **see**."*

And the measurement that settled it, same turn — commit `3b07ef9`:

    flexiargs in futon3/library:   1227
    carrying + evidence:            172
    carrying ?evidence(required):     5
    carrying + predicts:              0

**1227 conditional rules, zero predictions.** `+ evidence:` does not close the
gap: those 172 carry evidence *for the pattern's claim*, justification pointing
backwards, where a prediction points forwards — and the format has no field for
it.

One detail from T1402Z worth keeping: **the five `?evidence(required)` patterns
are exactly the five red-ring problem patterns.** The only patterns in the
library that state what would count as evidence are the ones about defects.
Nothing that works says what its working would look like.

The separate settled point from 2026-08-28 (§3.1h, `872bc95`) still holds and is
about a different relation: a pattern's `IF ∧ HOWEVER` is its **firing
precondition** and its `@why` is what it **stands on**; those are different
graphs, witnessed by `exchange-when-both-sides-gain`, which has a rich antecedent
and no `@why` at all.

---

## 2. Q — what the record actually says

### 2.1 Q is Q(o∣π), a distribution over outcomes conditioned on a policy

Not a map from patterns to implementations. **T1402Z** states the gap:

> *"The actual gap: `G(π)` needs `Q(o∣π)` — what will be **observed** if this
> policy is followed."*

So Q is the generative-model half that the production-rule half lacks. This
matters for reading Joe's 2026-08-29 recollection — *"another function called Q
which would realize that design pattern in an implementation and allow us to do
some kind of measurements on it"* — because **the record separates two things
that sentence names together**: Q(o∣π) is the distribution; the thing that
*realizes* a pattern in an implementation is the `@how` edge and its derived
check (§2.3). Q is what becomes computable once the realization exists.

### 2.2 The proposal that was made, and the honest bound put on it the same turn

**T1402Z** proposed letting a pattern predict which patterns' `IF` its `THEN`
satisfies — vocabulary derived from the library rather than from a harmonised
channel list, so that *"`Q(o∣π)` becomes a distribution over successor
conditions; `C` becomes preference over conditions one wants satisfied, which is
what a mission already is; ambiguity is the spread of that distribution."*

The bound was recorded in the same turn, before any objection:

> **Honest bound, recorded:** *"this makes `Q(o∣π)` a distribution over **pattern
> conditions**, not world observations. That's a self-model, and it's falsifiable
> only where a condition's satisfaction is independently checkable. Where it
> isn't, the loop closes on itself — the R16 re-observation trap your own
> catalogue names."*

### 2.3 Joe's correction — the proposal was killed, and replaced

**T1431Z**, opening line: *"Both corrections land, and they kill my `+ predicts:`
proposal."* Commit `c5df44a`. As I recorded his position:

1. **A hand-authored prediction is a stipulation.** S-G3 requires stipulations be
   declared, not absorbed — and such a prediction is unfalsifiable at the grain
   it is written.
2. **Q(o∣π) is *derived*, not authored.** In my sentence recording his version:
   *"the pattern is abstract, Lean-grade, and `Q(o∣π)` is **derived** as a unit
   test or `core.logic` rule. Same two-layer split as the APM chain, which is
   already Tier 0."*

This is the load-bearing correction in the whole discussion, and it is Joe's, not
mine. It converts Q from a field somebody fills in to a consequence of a
realization that can be checked.

**And the vocabulary already existed.** T1431Z records that §5a of the library
standard declares `@why` (authority, total, author-written), `@how` (named
methods, partial, curatorial), `@see-also`, and `@holds-at` / `@holds-open`,
which *point out of the library at R-map nodes* — the shared observation
coordinate I had claimed was missing. Critically:

> *"`@how` is the composition relation; a cascade is a `@how` expansion."*

Then the measurement, same turn:

    flexiargs        1227
    with @why          60   (5%)
    with @see-also     38
    with @holds-at     12
    with @holds-open    3
    with @how           1   ← one file, one edge

> *"The relation that would give cascades has one edge in the whole library."*

Not neglect: §5a says `@how` is *"partial and curatorial: only some patterns
acquire methods worth naming"*, and the standard was ten days old. T1431Z also
records the honest cost, quoting §5a: *"The rationale layer can only ever be
authored, never harvested"* — **populating `@how` is authoring work, not mining
work.**

`library/snatch/` was then given 9 structural edges across 6 patterns against 1
in the remaining 1221, with a deviation noted (§5a says `@how` is editor-written
later; these were authored straight after the patterns).

---

## 3. Where Q(o∣π) actually exists — three carriers, measured

This is the part the inferred note got most wrong. Q is not absent; it exists in
three domains at three different epistemic strengths.

**Snatch — derivable from rules (T1451Z).** The game names its own outcome space
as the terminal leaves of its five flowcharts: `O1` *sin cambios*, `O2`
*intercambiar tokens*, `O3` *sin cambios en tokens*, `O4` *robo*, with `O4a`/`O4b`
and `O5` as refinements. Recorded at `futon3/checks/snatch-outcomes.edn`,
`45773d7`. The structure was the finding — **G2 removes an outcome and G3/G4 each
split one, so the institution changes the object `O`, not merely the morphism
into `D(O)`.** And the general statement:

> *"A domain has a `G(π)` when it can say where its own machine stops. Snatch
> can. The War Machine cannot — and that, rather than a missing transition model,
> is why `G(π)` has no home there."*

That is **I1 (derived-not-enumerated) at the outcome space** — one level below
where it had been found that morning: not the action space, not the event
vocabulary, but the outcome space itself.

**War Machine — empirical, n=82 (T1456Z).** That claim was then corrected by Joe,
and the correction is recorded as a grain error of mine: *"I measured the tick and
concluded about the policy."* Commit `ceb20a7`. `full_loop_runner.clj` **does**
name fourteen terminal flight outcomes — `:ok`, `:grounded-change`,
`:grounded-no-change`, `:no-selection`, `:artifact-only`, `:build-failed`,
`:guardrail-refusal`, `:policy-nondiscrimination`, … — and
`data/wm-full-loop/` holds **82 closed attempts** as (π, o) pairs. It
discriminates:

    P(grounded-change | :repair-machine-failure)  ≈ 11/28 = 0.39
    P(grounded-change | :advance-mission)         ≈  3/8  = 0.38
    P(grounded-change | :learn-action-class)      =  3/25 = 0.12

> *"The policy dimension carries a singularity — measured, not argued. Exactly
> what the τ dimension didn't."*

Four bounds were recorded with it: **21% of flights have no recoverable π**
(mostly `:agent-unavailable`/`:incomplete`, failing before selection —
missing-not-at-random, not to be dropped silently); `:address-sorry` is n=2, not
a rate; **an outcome is not a reward**, so a `C` over these is a further stateable
choice; and `:ok` is a step outcome, not a terminal disposition.

**Mathematics — preregistered, n=11 (T1501Z).** The strongest carrier, `98e1e5d`.
`PROTOCOL-defective-problem.md` fixes `#{:closed :tier-a :tier-b :defective}`
with `:void` at series level, and `analysis/series.edn` populates it — **specified
before it was populated, which neither other domain can say.** The policy is a
real artefact: the memory-cascade arm `{:enabled? true :routes [:sibling] :cap
100}`, pinned into each manifest and reversible by deleting the file before the
mint. And:

> *"The WM's 82 flights are **retrospective frequencies**. APM's frames are
> **forecasts with a falsifier.** That's the difference between fitting a
> distribution and having one."*

| | outcome space | π | Q(o∣π) | n |
|---|---|---|---|---|
| **Snatch** | flowchart leaves | treatment G1–G5 | derivable from rules | — |
| **War Machine** | 14 dispositions | selected action class | **empirical** | 82 |
| **Mathematics** | 4+1, **specified first** | pinned arm config | **preregistered** | 11 |

---

## 4. The design rule that fell out — T1512Z

Item **S-001** was built as a worked example (`10a524f`), stated `:status :open`
with no outcome, which was the point:

| part | value |
|---|---|
| **π** | `:probe-one-token` — offer 1 turkey, and on a snatch never offer again |
| **hidden state + prior** | P2's disposition, Beta(1,1), **declared** as a stipulation per S-G3 |
| **Q(o∣π)** | `{O1 0.0, O2 0.5, O3 0.0, O4 0.5}` over the G1 flowchart leaves |
| **spread** | 0.6931 nats = log 2 |
| **falsifier** | **O3** — *in* the space, *zero* predicted mass |
| **derived check** | `score_item.clj`, core.logic over the item's own support |
| **receipt contract** | `:realised-outcome :posterior :refuted?` |

> **The design rule:** *"Some outcome in the space must carry **zero predicted
> mass** — otherwise no outcome could refute the prediction. That's the mirror
> discipline in distribution form."*

With its complement: *"the outcomes that aren't falsifiers must move the
spread"* — O2 and O4 each collapse entropy 0.6931 → 0, and *"a prediction that
leaves the posterior where it found it has taught nothing, whatever it scored."*

**By that test the 82 WM flights are not items** — they are fixture material,
telling us which shapes are real and nothing more, because a fitted distribution
spreads mass everywhere and so cannot be refuted. The standard was written to
`checks/README.md`: **π, prior, Q, spread, falsifier, check, receipt — before the
outcome.**

---

## 5. Settled versus open, strictly from the record

**SETTLED (all with commits, 2026-08-27 unless noted):**

1. A flexiarg is a production rule; `G(π)` needs a generative model; the library
   has 1227 of the former and 0 of the latter (`3b07ef9`, T1402Z).
2. **A hand-authored prediction is a stipulation and is unfalsifiable at its
   grain** — Joe's correction, which killed the `+ predicts:` proposal
   (`c5df44a`, T1431Z).
3. **Q(o∣π) is derived, not authored** — as a unit test or `core.logic` rule over
   an `@how` edge. Joe's version, adopted (T1431Z).
4. **`@how` is the composition relation and a cascade is a `@how` expansion** —
   and it has **1 edge in 1227 patterns**; populating it is authoring work, not
   mining work (T1431Z).
5. A domain has a `G(π)` when it can say where its own machine stops; this is I1
   at the **outcome space** (T1451Z).
6. Three carriers exist at three strengths — Snatch derivable, WM empirical
   (n=82, and it discriminates), mathematics preregistered (n=11) (T1451Z,
   T1456Z `ceb20a7`, T1501Z `98e1e5d`).
7. Some outcome must carry zero predicted mass, and non-falsifying outcomes must
   move the spread; S-001 is the worked instance (`10a524f`, T1512Z).
8. G is earned at **action grain, not policy grain** — `predict-multi-horizon`
   chains K steps *"assuming the same action repeats"*, so π collapses to *this
   action, sustained* (`09a0813`, T1240Z). S-G1/S-G2/S-G3 were proposed there.
9. (2026-08-28, `872bc95`) firing precondition ≠ authority edge; different graphs.

**OPEN, from the record:**

1. **Q over cascades.** Every Q that exists is conditioned on a π that is an
   action class, a treatment, or a pinned config — **never on a cascade.** Given
   settled item 4, a cascade is a `@how` expansion, and `@how` has one edge; so
   Q-over-cascades is blocked on authoring, not on theory.
2. **The self-model limit** — my own honest bound at T1402Z: a Q over pattern
   conditions is falsifiable only where a condition's satisfaction is
   independently checkable. R16's discipline applies and no mechanism enforces it
   yet.
3. **`C` for the War Machine.** T1456Z: *"an outcome is not a reward, so a `C`
   over these is a further stateable choice."* Not made.
4. **The 21% of flights with no recoverable π**, flagged missing-not-at-random
   and not to be dropped silently. Unhandled.
5. **The derived check was never priced.** T1431Z's own next step — *"one derived
   check over one existing edge… If cheap, `Q(o∣π)` is reachable by populating
   and deriving. If expensive, we learn that at the scale of one edge rather than
   1227 patterns"* — has not been done.
6. **Whether policy is a cascade**, a distribution over cascades, or a cascade
   plus an acting order. The record does not settle it; `sec-glossary.tex` says a
   cascade is *"the policy composed from those schemas"*, which is composition,
   not identity.
7. **Vocabulary drift in the maths outcome space** — `:tier-a` absent from the
   counts while `:defective-registration` appears and is not in the protocol's
   stated set (T1501Z); and the two-frame falsifier is undischarged, the arm
   frames being f45/f46.

**A corpus problem that bites before any of the above.** Of 25 recorded
generated cascades, 4 have a cyclic `:descent` and 23 have a pair with no
greatest lower bound — all 23 involving one pattern that is in every `:patterns`
carrier and in no edge set
(`futon3c/holes/tickets/T-strategic-cascade-emits-disconnected-patterns.md`).
Whatever Q over cascades turns out to be, that corpus needs repair first.

---

## 6. What the inferred version of this note got wrong — withdrawn

Recorded so the error is visible rather than quietly overwritten.

| inferred claim (first version) | the record |
|---|---|
| *"Q's codomain does not exist"* | **Wrong.** Outcome spaces exist in three domains, one of them specified before populated |
| *"no kernel cascade → outcome distribution"* | Half right for the wrong reason: Q exists in three domains, but conditioned on action class / treatment / arm config. The missing conditioning is **cascade**, and it is blocked on `@how` having one edge |
| `Q : Pattern → realization` | **Wrong shape.** Q is `Q(o∣π)`, a distribution over outcomes. The *realization* is the `@how` edge and its derived check — a different object |
| Q's gate is the glossary's `B′` obligation | Plausible and **not in the record**; demoted to an unsupported analogy |
| *"no outcome space or preferences C for the WM"* | Half wrong: the WM has 14 terminal dispositions. `C` is genuinely open |

The lesson worth keeping: the first version was written after a search that
returned nothing, and reported absence. The search could not have found it — the
string is `Q(o∣π)` with U+2223. **An absence produced by the instrument is
indistinguishable, in the output, from an absence in the world**, which is the
same failure this mission recorded three times on 2026-08-28 at §3.1i.
