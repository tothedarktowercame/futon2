# Packet H-A-CONSUMER-D — the `:checkable` branch discards labels, before it reads them

Date: 2026-09-25. Author: claude-3. Status: discovery, read-only; **no code
change, no click, no store write, no shared-JVM load**. The packet it replaces
(H-A-CONSUMER, a fix packet: "pass the ledger labels at the R5 call site")
cannot work, for the reason in section 1. Section 3 is a ruling for Joe; this
packet does not choose.

Reproduction: `h-a-consumer-d-repro.clj` in this directory, output committed
beside it as `h-a-consumer-d-repro-output.txt`. Run in its own JVM from the
futon2 checkout:

```sh
cd /home/joe/code/futon2
clojure -M holes/labs/wm-contract/proof2/packets/h-a-consumer-d-repro.clj
```

## Inputs, with the versions read

futon2 at `0358d4c5` (2026-09-25 00:35:06 +0000). sha256 truncated to 16 hex
characters; the file's last commit beside it where the file is versioned here.

| What | Path | sha256/16 | last commit |
|---|---|---|---|
| the branch under discussion | `futon2 src/futon2/aif/observation_rates.clj` (line 140) | `4043195129358156` | `a097e0b4` |
| the R5 call site | `futon2 scripts/futon2/report/war_machine.clj` (line 5885, `sourced-rates` call at 5889) | `5f546d48ea491edb` | — |
| the observation contract | `futon2 resources/wm/observation-contract.edn` | `a3bee89abe07a233` | `7d3d4ac2` |
| the measured-rates estimator | `futon2 src/futon2/aif/check_error_rates.clj` | `3ccc1dfce634c59e` | `f758d702` (r2), `cc831860` (r1) |
| the A-S spec | `futon2 holes/labs/wm-contract/proof2/packets/A-S.md` | `4d6c5400262d67f3` | `fc90808d` (r2), `4fa360f9` (r1) |
| the measured ledger | `futon3c holes/labs/M-futon-seams/exemplar/check-ledger.edn` | `2017be4e3ddef89d` | `c1ded160`, blob `d85e26b06b08843fcced22e579381c8faa69b74b` |
| the row classification | `futon2 test/fixtures/check-ledger-classification/m-futon-seams-v1.edn` | — | `f758d702` |
| the Lean carrier | `mathlib4 DarkTower/WarMachine/TokenObservation.lean` | — | contract pins `889429e6bf` |

A path correction from the requisition, confirmed: `cascade-lane` is in
`scripts/`, not `src/`. `A-S.md` is at `holes/labs/wm-contract/proof2/packets/A-S.md`,
not the wm-contract root; the measured-rates namespace is
`futon2.aif.check-error-rates` (`src/futon2/aif/check_error_rates.clj`), and
there is no `futon2.aif.measured-rates`.

## 1. The reproduction, and what short-circuits

### 1.1 What was run

`observation-rates/sourced-rates` was called twice with the same locators
(three tokens, all class `:C4`) and the same contract, once with labels that
carry measured error rates and once with `nil` labels. The labels are in the
shape `rates-by-class` documents — `:token-class`, the recorded verdict,
the admitted reference label — four labels for `:C4`: one established token
reported, one established token missed, one absent token not reported, one
absent token reported. So false-neg = 1/2 and false-pos = 1/2.

The producer did read them. `rates-by-class` over those same labels:

```clojure
{:C4 {:false-pos {:numerator 1, :denominator 2, :rate 1/2},
      :false-neg {:numerator 1, :denominator 2, :rate 1/2},
      :coverage 1}}
```

`sourced-rates` over them, and over `nil`, are the same value and the same
printed form:

```clojure
{:status :sourced,
 :source :futon2.aif.observation-rates/sourced-rates,
 :rates    #:t{:other {:false-neg 0, :false-pos 0}, :wanted {…0…}, :observed {…0…}},
 :basis    #:t{:other :checkable, :wanted :checkable, :observed :checkable},
 :class-of #:t{:other :C4, :wanted :C4, :observed :C4}}
```

`equal? true`, `printed forms equal? true`. Rates of 1/2 in, zeros out.

### 1.2 The function and the branch

`futon2.aif.observation-rates/token-likelihood-rates`,
`src/futon2/aif/observation_rates.clj:140`:

```clojure
(let [cls (get by-id class-id)]
  (if (= :checkable (:kind cls))
    (assoc acc token {:false-neg 0 :false-pos 0 :basis :checkable})
    (let [r (get rates class-id)
          fn-rate (some-> r :false-neg usable-rate)
          fp-rate (some-> r :false-pos usable-rate)]
      …)))
```

`rates` — the whole label-derived estimate — is first read in the `else` arm.
For a `:checkable` class the function returns the zero kernel without
consulting it. `sourced-rates` (line 154) is a wrapper: it computes
`rates-by-class` from the labels, hands it to `token-likelihood-rates`, and
returns what comes back. So the labels are discarded one call inside the
function the R5 seam calls, not at the seam.

Every class on the production path is `:kind :checkable` (from the contract,
printed by the repro):

```
:C3 :checkable   :C4 :checkable   :C5 :checkable   :C6 :checkable
:C8 :checkable   :J  :judgement
```

and `:J` never reaches scoring: `:production-path {:classes #{:C3 :C4 :C5 :C6 :C8}
:judgement-class-J :refused-at-assembly :enforced-by "futon2.aif.cascade-problems
(705adb39): every problem token needs a checkable locator, else
:universe-not-admitted naming the tokens"}` — enforced at
`cascade_problems.clj:135-137` (`checkable-classes`) and refused at `:214-219`.
The 17 declared-source locators in `resources/wm/cascade-sources/*.edn` today are
3 × `:C3` and 14 × `:C4`. So for every token the live lane can score, the
`if` takes the zero arm, whatever any ledger says.

C8 landed in the contract on 2026-09-25 (`7d3d4ac2`), also `:kind :checkable`;
it does not change this.

### 1.3 Who introduced it, and what it cites

- **Commit `06ecba8c`** (2026-09-17 15:01:50 +0000), "WM-04 S-3: adjudication
  rates from admitted labels (futon2.aif.observation-rates)" introduced
  `token-likelihood-rates` with this branch. `git blame` attributes lines
  140-141 to it. Git author is `Joseph Corneli
  <joseph.corneli@hyperreal.enterprises>` — the shared-checkout identity every
  agent commits under, so the commit does not name the agent. The S-3 row of
  `p4ng wm-walkthroughs/build-loop/closure/PLAN-WM-04-unblock-2026-09-17.md:56`
  assigns the work to `@WM-07 zai-15`; the WM-04 acceptance record of the same
  day does not name an S-3 author. Treat the agent as named by the plan, not by
  the commit.
- **Commit `a097e0b4`** (2026-09-18, WIRE-5) added the R5 call site that passes
  `nil nil nil` for labels/subjects/prior.

The justification each cites:

- `06ecba8c`'s message: "Checkable classes get the exact zero kernel
  `{:basis :checkable}` (`tokenLikelihood_checkable`); judgement classes get
  estimated rates".
- The R5 comment at `war_machine.clj:5878`: "an all-checkable universe sources
  the EXACT ZERO kernel (`tokenLikelihood_checkable`)".
- The contract's own authority: `:authority {:basis "P5 (Joe, 2026-09-16):
  checkable tokens observed exactly, judgement tokens need rates" …}`, and
  `:applicability-reading "claude-7, 2026-09-17: … zero rates hold only for the
  class tokens as defined here"`.
- P5 itself,
  `p4ng .../PROPOSAL-wm-model-design-2026-09-16.md:55`: "A reports the
  established-token state. It is **deterministic where a token is a checkable
  artefact**, and carries an attested adjudication error rate where a token
  needs judgement. … Ambiguity is then zero for checkable tokens and positive
  only where adjudication is uncertain."
- The WM-04 plan, same file family,
  `PLAN-WM-04-unblock-2026-09-17.md:31`: "A token checkable by a mechanical
  check (commit exists, test passes, file present) has zero adjudication rates
  **by construction** (`tokenLikelihood_checkable`) and needs no labels."

So a ruling is cited, transitively: the code cites the Lean theorem name, the
contract cites P5, and P5 is Joe's approved design statement of 2026-09-16.

**The theorem the code names does not carry the claim.** In
`mathlib4 DarkTower/WarMachine/TokenObservation.lean`:

```lean
theorem tokenLikelihood_checkable (r : AdjudicationRates V)
    (hfn : ∀ v, r.falseNeg v = 0) (hfp : ∀ v, r.falsePos v = 0) (s o : Finset V) :
    tokenLikelihood r s o = if o = s then 1 else 0
```

`hfn`/`hfp` are hypotheses. The theorem says *if* the rates are zero the kernel
is the identity; it cannot establish that a check's rates are zero. The module's
prose does assert the stronger reading — "checkable tokens (artefacts: a file
exists, a test passes) are observed exactly" — but that is the docstring, not a
proved statement, and `tokenLikelihood` and `tokenLikelihood_colsum` are stated
for arbitrary rates in `[0,1]`, so a non-zero kernel for a checkable class
breaks nothing in the carrier.

### 1.4 What this means for the packet it replaces

The fix packet asked for measured labels at the R5 call site so that
`:rates-provenance :labels` would read `:measured`. That is writable, and it
would change no rate, no G and no choice: the provenance field would say
`measured` while every token's `:basis` stayed `:checkable`. Section 3's
question has to be settled first.

## 2. What A-S measured (the answer to the requisition's question 2)

**Over other check kinds. No row of the ledger is a run of a C3-C8 class
check, so its rates have no consumer among the classes and the kind → class
mapping question is moot.**

The ledger's 22 rows expand to 33 runs in four kinds. The repro lists every
row's `:check` beside the contract's `:checks-implemented`:

| contract class | check function | is any ledger row a run of it? |
|---|---|---|
| `:C3` | `check-path-exists` (`git cat-file -e sha:path`) | no |
| `:C4` | `check-decl-in-file` (anchored line match, `decl-present?`) | no |
| `:C5` | `check-registry-entry` (bundle JSON entry + loci resolve) | no |
| `:C6` | `check-witness-reference` (EDN witness → commit → entry) | no |
| `:C8` | `check-registered-run` (registry warrant, pinned shas, counts) | no |

What the rows actually ran, by kind:

| kind | runs | the checks | observed errors |
|---|---:|---|---|
| `:test` | 5 | `roles_test` provider-routing / redirect tests (clojure.test) | 0 false pass, 0 false fail |
| `:grep` | 6 | a `starts-with?…"(claude\|codex\|kimi\|zai)"` regex over futon3c source, old and new | 1 false pass (`:g2`) |
| `:validator` | 18 | `proof2a_check` W_t/W_0 and falsifiers, `meets.clj` strict descendants, `cascade_check.py` receipt sha, seams stale-anchor flag, `reanchor.py` | 2 false pass (`:v3`, `:v7`), 4 false fail (`:v4`) |
| `:layout` | 4 | `check_seams_layout.js` Playwright measurements | 1 false pass (`:l1`), 1 false fail (`:l2`) |

The mechanical search in the repro for any of the five check-fn names in any
row's `:check` returns `NONE`. These are checks of M-futon-seams' own work —
a routing test suite, one regex over `http.clj`, record validators, a page
layout measurement — not observations of a WM cascade token at a pinned sha.

Two things follow, and they should not be run together:

1. **The numbers cannot be consumed for C3-C8 as measurements of those
   classes.** A-S's estimator is defined per check kind, over runs of that
   kind whose truth was independently established; none of its kinds is one
   of the classes. Feeding `:grep`'s 3/10 to `:C4` would assert that an
   anchored `decl-present?` match errs at the rate a particular
   `starts-with?` regex over `http.clj` errs. Nothing in A-S supports that.
2. **The measurement is still evidence about the reading of P5**, by analogy
   rather than by measurement. The kinds where errors were observed are
   mechanisms of the same family as the class checks: `:grep` is a pattern
   match over file text, which is what `decl-present?` is; `:validator` is a
   program resolving declared references in a record, which is what
   `check-registry-entry` and `check-witness-reference` are. The WM-04 plan's
   own example list of checkable tokens — "commit exists, **test passes**,
   file present" — includes the kind A-S built its ledger around. So the claim
   that mechanical checks are exact *by construction* has, for the first time,
   a population of mechanical-check runs to be read against; that population
   says 4 of 33 runs were wrong, in three of four kinds. It is an analogy: it
   bears on which reading of "observed exactly" is tenable, and it does not
   license a number for any class.

The distinction the requisition names — exact *given the check ran*, versus
exact *including the check's own error* — is already made by the contract, and
C8 is the clearest case. C8's token is "the test registry **holds a warrant** …
whose pinned … shas equal those files' shas now, whose postcheck matched, and
whose run **recorded** no failures and no errors". That is a proposition about
the record, which the check reads mechanically. Whether the tested code is
correct is a different proposition, and the contract pushes it out of the class
by its own rule: `:stated-conditions-rule "an output whose application states
further conditions (correctness, adequacy, coverage, behaviour beyond the class
token) is class J for those conditions; a locator makes only the class token
observable"`. A-S's `:test` rows measure exactly that further condition — the
gap between a suite's verdict and the truth about the code — which the contract
assigns to J, not to C8.

## 3. The two designs

### (a) Checkable classes consume measured rates where a row exists

A class with a measured rate takes it; the zero kernel stays only where nothing
was measured. `token-likelihood-rates` consults `rates` before the
`:checkable` test, or the seam post-processes its result.

**Bad case (from the requisition): a class with a measured false-negative rate
above zero must produce a non-zero kernel and a different G.** Exhibited, E3 of
the repro. The real `cascade-lane` ran twice on one minimal problem (one true
fact, one want, one pattern producing it, T=2, β=1, three `:C4` tokens), once
as production runs it and once with `token-likelihood-rates` redefined so
`:C4` tokens take `{:false-neg 1/6 :false-pos 3/10}` — A-S's measured `:grep`
rates, used only as illustrative arithmetic:

| | `:C1` G | `:C0` G | gap |
|---|---:|---:|---:|
| production (zero kernel) | 2.012817736156336 | 4.012817736156336 | 2.0 |
| design (a), simulated | 3.732445430609561 | 4.799112097276227 | 1.0666666666666663 |

G moves, and it does not move by a constant: the margin between the two
candidates falls by 46.7%, from 2.0 to ≈ 16/15 (`:C1` rises by 1.7196, `:C0`
by 0.7863). The mechanism is the ambiguity term. `horizon-g-sparse`'s own
docstring, `cascade_model_manifest.clj:995-998`: "at zero adjudication rates A
is the identity kernel (tokenLikelihood_checkable), so Q(o_τ|π) = q_τ and the
ambiguity term is identically 0 … Non-zero rates use the factorized path under
the preconditions below." A non-zero kernel makes that term positive, by an
amount that depends on each candidate's predicted state distribution. On this problem the ranking did not
flip (`:C1` is preferred in both runs); a family whose G gap is smaller than
its ambiguity difference would flip. So design (a) changes selection in
general, not only the recorded numbers.

What (a) needs that does not exist: a ledger of runs of the class checks
themselves (section 2), and a rule for a class that is mapped but under A-S's
minimum count of 5 eligible runs, whose rates are `{:status :insufficient}`
and must not become 0.

**Bad case that would falsify (a) as an improvement:** if the rate used for a
class is measured over a different check, (a) replaces an exact zero that is
wrong with a number that is unfounded. A-S §5's own falsifier applies —
"a rate table with no counts, or with declared numbers wearing the shape of
measured ones, is not measured and must be refused". Under (a) the
`:source-ids` of every consumed rate must resolve to runs of that class's
check, or the load must refuse.

### (b) P5 stands; measured rates apply to class J

Checkable class tokens keep the exact zero kernel, because the token is defined
as the fact the check observes; a check's fallibility about anything further is
class J by the `:stated-conditions-rule`. Then H-A's consumer is the class-J
amendment: J is `:refused-at-assembly` today, so nothing can consume a rate
until J is admitted, which needs the blinded-label machinery
(`observation-admission`, the S-4 pilot) that the contract records as
unfinished: `:not-claimed [:judgement-labels :judgement-rates
:stated-conditions-beyond-class-tokens]`, `:J-evidence "… unreviewed pilot; of
38 recorded realised steps, blind observers confirmed 15"`.

**Bad case for (b):** a check whose own failure is *inside* its class token.
C4's token is "a line of the file at the pinned sha starts with the declaration
head"; `decl-present?` decides it by one regex. If that regex can answer wrongly
about the anchored-match proposition itself — which is the shape of ledger row
`:g2`, where `[^)]*` stopped at a nested form before the literal and the check
said "held" when it did not — then the zero kernel is wrong for the class
token, not merely for conditions beyond it, and (b) is untenable for that
class. Deciding this case per class is the work (b) needs: for each of C3, C4,
C5, C6, C8, is the check's verdict the class token by construction, or an
attempt at it?

### What each source supports

**The Lean model is neutral, and slightly favours (a) as well-formed.**
`AdjudicationRates` requires only `falseNeg v ∈ Set.Icc (0:ℝ) 1` and the same
for `falsePos`; `tokenLikelihood` is defined for any such rates, and
`tokenLikelihood_colsum` — `∑ o, tokenLikelihood r s o = 1` — is proved for
arbitrary rates, so a non-zero kernel for a checkable class stays a normalized
observation model. `tokenLikelihood_checkable` is conditional on
`hfn : ∀ v, r.falseNeg v = 0` and `hfp`, so it supports (b) only as an
assumption, never as a finding about checks. The module docstring asserts the
(b) reading in prose — "checkable tokens (artefacts: a file exists, a test
passes) are observed exactly" — with no proof and no measurement behind it.

**The observation contract supports (b), and says so narrowly.** Its `:token`
definition: "each checkable class defines its own token (see :classes): exactly
the fact its check observes, nothing more. Zero adjudication rates hold only
for that fact (claude-7 applicability reading, 2026-09-17)." Its
`:stated-conditions-rule` sends correctness, adequacy, coverage and behaviour
beyond the class token to J. Its `:not-claimed` list disclaims judgement rates
and "stated-conditions-beyond-class-tokens". P5 is narrower than the code's
comment as well: "deterministic where a token is a **checkable artefact**" is a
statement about artefact facts, not about the reliability of the program that
looks for them.

**The tension inside the record.** The narrow definition (contract `:token`,
P5's "checkable artefact") survives A-S untouched: A-S measured checks of other
things. The broad phrasings do not sit as comfortably — the WM-04 plan's "by
construction … (commit exists, test passes, file present)" and the Lean
docstring's "a test passes … observed exactly" both name the test-suite case,
and A-S's ledger was built precisely to measure the gap between a check's
verdict and the truth. Nothing in the record resolves whether the C3-C8 class
tokens are *defined* to be the checks' own outputs (so (b) holds by definition,
and the fallible part is J), or are propositions the checks *attempt* (so (a)
is required, and a zero kernel is an unmeasured assumption). That is the
ruling.

## Decision sheet

**The question.** Are the C3-C8 class tokens defined as the facts their checks
output — so a check cannot err about its own token, its fallibility is class J,
and H-A's consumer is the J amendment (design (b)) — or are they propositions
the checks attempt, so that a measured non-zero rate for a class must enter the
kernel and the exact zero kernel is an unmeasured assumption (design (a))?

**Not in dispute, whichever way it goes.**

1. The `:checkable` branch (`observation_rates.clj:140`) discards label-derived
   rates before reading them; every production class is `:kind :checkable`, so
   no ledger can change a rate, a G or a choice through the R5 labels today
   (section 1, reproduced).
2. `tokenLikelihood_checkable` is hypothesis-bearing and cannot establish that
   a check's rates are zero; a non-zero kernel for a checkable class keeps the
   Lean model normalized (section 3).
3. The A-S ledger measured no run of any class check, so it supplies no number
   for any class either way (section 2).
4. If a non-zero kernel ever reaches a class, G changes and candidate margins
   change with it — 2.0 → ≈ 1.067 on the minimal family in E3 — so this is a
   selection question, not a bookkeeping one.

**If (a):** the packet is "checkable classes consume measured rates where a row
exists", and it needs, in order — a ledger of runs of the class checks
themselves with independently established truth (nothing in section 2's ledger
qualifies); A-S's `:source-ids` resolution enforced per class so an unfounded
number cannot be consumed; a rule for mapped-but-insufficient classes that is
not 0; and the contract's `:token`/`:stated-conditions-rule` amended, since (a)
contradicts "zero adjudication rates hold only for that fact" as currently
read.

**If (b):** the packet is the class-J amendment, and H-A's consumer does not
exist until J is admitted at assembly — which needs the blinded labels and the
S-4 pilot review that the contract lists as `:not-claimed`. The per-class
question from (b)'s bad case has to be answered on the record: for each of C3,
C4, C5, C6, C8, is the check's verdict the class token by construction, or an
attempt at it? Row `:g2` of the ledger is the shape of the case where the
answer is "an attempt".

**Either way, one correction is due now and needs no ruling:** the WM-04 plan's
"by construction" example list (`PLAN-WM-04-unblock-2026-09-17.md:31`) and the
Lean module docstring both say a passing test is observed exactly. Under the
contract's own `:stated-conditions-rule` that is only true of C8's
record-shaped token, never of the code's correctness, and A-S's `:test` rows
measure the latter. Those two sentences should be narrowed to the contract's
`:token` wording whichever design is chosen.
