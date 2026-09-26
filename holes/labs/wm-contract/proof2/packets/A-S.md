# Packet A-S / H-A — measured check error rates from the exemplar check ledger

Date: 2026-09-24. Author: kimi-3. Status: spec for review; discharged on the
exemplar ledger by `futon2.aif.check-error-rates` (code commit references this
file). Read-only inputs; no click, runtime change, or data write.

Hole H-A: "observation error rates measured per check kind; 22 ledger rows;
declared rates were used first". Source of truth (read-only):
`/home/joe/code/futon3c/holes/labs/M-futon-seams/exemplar/check-ledger.edn`,
schema `:m-futon-seams/check-ledger-v1`, 22 rows with
`:id :kind :check :input :truth :passed :truth-source` (optional `:count`
for identical runs). A false pass is `:passed true :truth false`; a false
fail the reverse.

Why measured, not declared: PROOF-2a "What the worked example teaches" item 3 —
declared rates gave a posterior of 0.985; rates measured from runs with
independently known truth give 0.77. A rate table with no counts, or with
declared numbers, is not measured and must be refused (falsifier below).

## 1. Population and independence

The population is the ledger's rows, expanded by `:count` (default 1):
5 + 6 + 18 + 4 = **33 runs** across kinds `:test`, `:grep`, `:validator`,
`:layout`.

A row is **eligible** iff its truth was established independently of the
check. Independence is recognized by the presence of a `:truth-source`
naming one of three modes:

1. a **constructed bad case** (input built so the answer is known),
2. a **later review** (re-reading of the artefact by an agent or human),
3. an **independent recomputation** (a second mechanism deriving the truth).

Machine rule: a row is eligible iff it carries a non-absent `:truth-source`.
A row whose truth came from the check itself is **excluded**; you can tell
because such a row has nothing independent to name, so the schema rule for
this ledger is that it MUST NOT carry `:truth-source` — absence of
`:truth-source` is exactly the marker of a self-truthed row, and the
estimator drops it as a typed exclusion, never a substituted value.

All 22 rows of the exemplar ledger carry `:truth-source` in one of the three
modes, so the eligible population is the full 33 runs.

## 2. Estimator

Per kind, over eligible expanded runs:

- `n` — runs; `n-true` / `n-false` — runs with `:truth true` / `false`;
- `false-pass` — runs with `:passed true :truth false`;
- `false-fail` — runs with `:passed false :truth true`.

Rates use a **Jeffreys-prior smoothed estimate**, Beta(1/2, 1/2):

```
fp-rate = (false-pass + 1/2) / (n-false + 1)
fn-rate = (false-fail + 1/2) / (n-true  + 1)
```

Why this prior: with counts this small a raw proportion is 0 or 1 on the
slightest absence of evidence (the `:test` kind has zero observed errors in 5
runs; a raw 0 would assert the checks are infallible — exactly the
declared-rates failure this packet closes). Jeffreys prior is the standard
noninformative binomial prior, keeps estimates in (0, 1), and is symmetric
in success/failure so it does not privilege passes over fails. If the kind
has no denominator at all (e.g. no `:truth false` runs), that rate is
`{:status :no-denominator :n 0}`, not 0 and not 1/2.

**Uncertainty**: a 95% Wilson score interval on the *raw* counts (k, n)
per rate, reported as `{:lo .. :hi ..}` beside each smoothed rate. Wilson is
chosen because it behaves at k = 0 and k = n and at small n, unlike the
normal approximation. The interval is over the raw frequency; the point
estimate carries the prior. Both are reported so a consumer can choose.

**Minimum count**: a kind with fewer than **5** eligible runs reports
`{:status :insufficient :n n ...counts...}` for its rates, not numbers.
Justification: below n = 5 a single row moves any rate by ≥ 20 percentage
points and the Wilson interval spans most of [0, 1]; the number would be
consumed as if it were a measurement while carrying none. 5 is also the
smallest n at which a zero-error kind's smoothed fp/fn rate drops below
0.5/(n+1) ≈ 0.083, the declared-rate regime the worked example discredited.
This threshold is a parameter of the packet, not a definition of
"measured"; raising it is an amendment.

## 3. Measured rates on the exemplar ledger

| kind | n | n-true | n-false | false-pass | false-fail | fp-rate (95% Wilson on raw) | fn-rate (95% Wilson on raw) |
|---|---|---|---|---|---|---|---|
| :test | 5 | 3 | 2 | 0 | 0 | 0.167 [0.000, 0.658] | 0.125 [0.000, 0.561] |
| :grep | 6 | 2 | 4 | 1 | 0 | 0.300 [0.046, 0.699] | 0.167 [0.000, 0.658] |
| :validator | 18 | 5 | 13 | 2 | 4 | 0.179 [0.043, 0.422] | 0.750 [0.376, 0.964] |
| :layout | 4 | 2 | 2 | 1 | 1 | **insufficient (n = 4 < 5)** | **insufficient** |

The layout kind's absence is typed: its counts (1 fp, 1 fn in 4 runs) are
kept on the record, its rates are not asserted.

## 4. What the machine consumes

`futon2.aif.check-error-rates/measured-rates` takes a ledger value of schema
`:m-futon-seams/check-ledger-v1` and returns per kind:

```clojure
{:status :measured            ; or {:status :insufficient :n ..}
 :n :n-true :n-false
 :false-pass :false-fail
 :fp-rate :fp-interval :fn-rate :fn-interval
 :source-ids [...]}          ; ledger row :ids behind every count
```

`rates-measured?` refuses (typed `:refused`, with reason) any table that:

- lacks counts (`:n`, `:false-pass`, `:false-fail`), or
- lacks `:source-ids`, or
- names source ids that do not resolve to rows of the ledger it claims to
  measure, or
- asserts rates where the kind was `:insufficient` / `:no-denominator`.

## 5. Falsifier (bad case)

A rate table with no counts — or with declared numbers wearing the shape of
measured ones — must be refused as "measured". Concretely, this packet is
wrong if either of the following is accepted by `rates-measured?`:

1. `{:grep {:fp-rate 0.1 :fn-rate 0.1}}` (no counts, no provenance) —
   refused `:missing-counts`;
2. a table whose `:source-ids` include an id not present in the ledger —
   refused `:unresolved-source-ids`.

And this packet is wrong if the `:layout` kind yields numeric rates: its
4 runs are below the minimum count, so its rates must be
`{:status :insufficient :n 4}`.

## Revision 2 (2026-09-24, kimi-4): eligibility is structural, and the 22 rows classified

Appended, not rewritten; sections 1–5 above stand as the original packet,
with the eligibility rule of section 1 superseded as follows.

**The defect.** claude-8's review of cc831860: `eligible-row?` treated ANY
non-nil `:truth-source` as independent. Run against the landed code, six
rows with `:truth true :passed true :truth-source "the check itself
passed, so it held"` were counted as eligible (n = 6, nothing excluded).
The docstring's "a self-truthed row MUST NOT carry :truth-source" was a
convention on authors, not a check — and the estimator's whole purpose is
to refuse declared or self-confirmed rates.

**The change.** Eligibility is decided by a truth KIND, never by the
presence of free text:

```clojure
truth-kinds = #{:constructed-bad-case :later-review :independent-recomputation}
```

`measured-rates` gains a two-argument form taking a classification map
`{row-id truth-kind-or-:self-truthed}` (an entry may also be a map
carrying `:kind` and `:reason`) plus `:classification-source` naming who
classified and when. A row is eligible iff its kind — `:truth-kind` on
the row if present, else the classification's entry — is in
`truth-kinds`. A row carrying only free-text `:truth-source` and no kind
is excluded as `:unclassified`; a `:self-truthed` row as `:self-truthed`;
`::excluded-ids` is now `{id reason}`, and the result carries
`::classification-source` (a typed absence when no classification was
passed). The one-argument form excludes every row `:unclassified` and
reports every kind `{:status :insufficient :n 0 ...}` — the true state of
a ledger nobody has classified. Every kind present in the ledger appears
in the result even when no row of it is eligible.

**The classification (the judgment part).**
`test/fixtures/check-ledger-classification/m-futon-seams-v1.edn`,
classified by kimi-4 (the H-WITNESS-check independent warrant checker,
not the ledger's author), one kind and a one-line reason per row drawn
from its `:truth-source`, `:cause` and `:fixed-in`. Result: all 22 rows
name an independent act — 7 `:constructed-bad-case`, 9 `:later-review`,
6 `:independent-recomputation`; **zero rows are `:self-truthed`**. That
is a finding about this ledger, not a default: the classification admits
the same eligible population (33 runs) as section 1, so the measured
rates of section 3 stand unchanged. What changed is what they rest on: a
recorded, checkable judgment per row instead of the presence of a string.
The pinned tests now run under this fixture; claude-8's bad case is a
test verbatim (six such rows are excluded, the kind insufficient), and a
test pins the one-argument all-`:unclassified` behaviour. The three
section-5 falsifiers are kept.

## Revision 3 (2026-09-26, claude-12): the reference for token labels, and the reader's rules

Appended, not rewritten. Sections 1–5 and Revision 2 stand. This revision governs the
**token-label** population (`observation-admission` → `observation-rates/sourced-rates`,
per token class C3..C8). The check-ledger population of sections 1–4 (per check kind,
classified row by row) is unchanged. Source: A-LABELS-D (futon3c df65eac9), which found
that the two texts disagreed about what a reference is:

- Revision 2's `truth-kinds` count an `:independent-recomputation` as a reference by
  itself;
- `observation-admission/admit` requires every token label to have an observer's
  adjudication **and** a distinct reviewer's `:concur`;
- F1b-D reads that review as part of establishing the reference.

**Why the two can agree.** `admit`'s blinded review exists to make the reference
independent of the check's verdict:

- the observer, author, enactor and reviewer are all distinct;
- the observer sees only `observer-view`, which excludes the recorded verdict;
- the reviewer concurs on a view digest it did not produce.

A recomputation by a **different mechanism**, which cannot see the check's verdict and
works on the same view and cutoff, meets that condition. A recomputation by the **same
mechanism** does not: it is the self-truthed row section 1 excludes.

For a recomputation, what needs checking per row is therefore not judgement but three
equalities, and a mechanical reviewer can check them.

**1. A new truth kind: `:independent-recomputation-reviewed`.** A token label's reference
is admitted under this kind when:

- **(a) Different mechanism.** The adjudication's observer is a **recomputation
  mechanism id**, and that id differs from the check's mechanism id.
  - A mechanism id names the code that decides: class, var and code sha. Examples:
    `C4/decl-present?@<sha>` against `C4/reader-form@<sha>`; `C3/cat-file-e@<sha>`
    against `C3/ls-tree@<sha>`.
  - The subject declares its check's mechanism id.
- **(b) Blinded.** The recomputation reads only `observer-view` (token, application,
  evidence pointers). The recorded verdict is not in its input.
- **(c) Same view and cutoff.** The adjudication's view digest equals
  `view-digest (observer-view subject)`, and its cutoff (`{repo sha}`, the resolved sha
  from the evidence pointers) equals the check's.
- **(d) Mechanical review.** The review is by a **mechanical reviewer**: a reviewer id
  that is neither the observer nor the check's mechanism. Its `:concur` is computed
  from (a) and (c) and from nothing else.
  - It does not re-read the evidence and makes no judgement.
  - When any of the three fails, the verdict is not `:concur`, and the review names
    which one failed:
    - **`:self-truthed`:** the recomputation's mechanism id equals the check's. This
      is section 1's excluded row.
    - **`:view-mismatch`:** the adjudication's digest is not the subject's observer-view
      digest.
    - **`:cutoff-mismatch`:** the adjudication's cutoff is not the check's.
  - `admit` then refuses `:review-not-concur`, carrying that reason. No label results.

The finding may still be `:insufficient`, `:ambiguous` or `:conflicting` (for example,
the recomputation could not read the file). That is `admit`'s existing `:no-label`
refusal, unchanged. `admit`'s other refusals are also unchanged, including
`:authorship-undeclared`: a tick-time check declares `:author :none :enactor :none`
explicitly.

A human or agent `:later-review` remains a reference as before. This kind adds a
reference the machine can produce at tick time without a reviewer seat. It does not
replace review where the check is a judgement (a `:judgement` class has no
recomputation mechanism, and so no label under this kind).

**2. The reader's rules** (the function that turns stored labels into `sourced-rates`'
`{:labels :subjects}`). Section 2 already fixes both a minimum count and an estimator,
so both apply here, per cell rather than per kind:

- **Minimum count, per cell.** A class enters the rates only when **both** of its cells
  have a denominator of at least **5**:
  - admitted-`:present` labels, for false-neg;
  - admitted-`:absent` labels, for false-pos.

  Section 2's reason applies to each cell separately: below 5, one label moves that
  cell's rate by 20 points or more. `rates-by-class` itself has no minimum, and makes
  a rate from any positive denominator (0/1 reads as a measured 0).
- **Estimator.** The point estimate is section 2's Jeffreys Beta(1/2, 1/2). It is passed
  to `rates-by-class` as an **authorised prior**, `{:alpha 1/2 :beta 1/2 :authority "A-S
  §2 (Jeffreys), Revision 3"}`. `check-prior` admits exactly this form.
  - The posterior mean `(k + 1/2)/(n + 1)` is section 2's `fp-rate`/`fn-rate`.
  - This is an explicit, authorised prior, as the `:adjudication-rates` registry row
    requires, not a default.
- **Both cells, or the class is left out.** `token-likelihood-rates` refuses a class with
  one cell unobserved (`:unsupported-class`), and the refusal ends the whole call (C2's
  `kernelSupply`). So the reader passes only classes meeting the minimum on both cells.
  - Any other class is **left out of `:labels`**, and so reaches the kernel as wholly
    unobserved. For a checkable class that means the zero kernel, recorded
    `:measurement :absent`: the unmeasured default, not a measurement.
  - The reader records each class it left out, typed, beside the labels:
    `{:class c :excluded :below-minimum|:one-cell-unobserved :counts {…}}`. A class it
    left out is never silent.
- **One label per subject and check-code sha.** A subject is the check's evidence
  pointers `(repo, resolved-sha, path, decl|entry|…)` under its class.
  - A subject re-checked on a later tick adds no second label while the check's
    mechanism id is unchanged. Tick run records re-check the same 29 subjects about
    1,249 times (A-LABELS-D §1).
  - A changed check mechanism is a new population, and its labels start afresh.
- **Subjects count.** `:subjects {class n}` is the number of distinct located subjects
  of the class the store has seen (for coverage), not the number of labels.

**3. Falsifiers.** This revision is wrong if any of the following yields an admitted
label or a counted rate:

- a recomputation whose mechanism id equals the check's (`:self-truthed`);
- a review whose digest or cutoff differs (`:view-mismatch`, `:cutoff-mismatch`);
- an adjudication with no reviewer (`:reviewer-missing`; never stored as a label);
- a subject with undeclared authorship (`:authorship-undeclared`);
- the same subject counted twice under one mechanism id;
- a class with a cell below 5 or unobserved reaching `sourced-rates` as measured, or
  refusing the whole call instead of being excluded and recorded.
