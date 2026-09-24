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
