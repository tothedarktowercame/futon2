# C554 — `:F12` slice 16: D2's three measured denominators

**What this is.** This slice measures which rounds O4's acting order could be
read over, on HEAD and on slice 15's temporary Arm-B construction. It chooses
no arm and writes no choice or decision: the checker's only write is its own
artifact (`f12_d2_denominator_check.clj:16`, `:260-261`), and that artifact
carries no `:choices`, `:decisions`, `:ruling` or `:chosen-arm` key — its five
top-level keys are `:arm-b`, `:controls`, `:head-baseline`,
`:head-gate-blocker`, `:problems`
(`runs/F12-organise/11-d2-denominator.edn:1`, `:1148`, `:3014`, `:3485`,
`:3537`). Every rule-table change is made in a temporary copy, read back, and
put first on a worker classpath (`f12_d2_denominator_check.clj:132-139`;
`f12_d3_encoding_check.clj:35-53`).

## HEAD, re-derived

Re-running slice 3's report at today's HEAD reproduces the earlier result: the
same exchange of `war-machine/ambient-pattern-retrieval` and
`math-strategy/missing-dependency-protocol` is false over **29 primary** rounds,
true over **49 paired** rounds, and true over **102 transcript** rounds. The
acting order changes on none, round 7, and rounds 3/4/7 respectively; scores are
14→14, 34→33, and 83→81
(`runs/F12-organise/11-d2-denominator.edn:3015-3420`, run record
`:head-baseline/:arms`). No number moved.

The nine recorded cascade files still contain **zero** runs carrying both
rule-bearing patterns
(`runs/F12-organise/11-d2-denominator.edn:3484`, run record
`:head-baseline/:recorded-carriage`). Thus the old `:not-a-witness` blocker
still holds. The Arm-B results below say what a construction carrying two
rule-bearing members **would** record; they are not a recorded cascade witness.

## A silent empty in the baseline floor, found and repaired here

The uncommitted draft of this slice computed the nested-round-set floor for the
HEAD baseline and for Arm B with one function that read `:contending-rounds`
off each arm. Arm B's arms carry that key (`f12_d2_denominator_check.clj:65`),
but the baseline's arms come from the reachability report, whose per-arm rows
carry `:rounds-on-which-the-fired-rule-differs` instead
(`f12_o4_reachability.clj:291`; its `:contending-rounds` at `:232` sits in a
different structure, `:contention`, not in an arm). The lookup therefore
returned `nil` and the baseline floor reported
`:contentions-in-paired-minus-primary []` and
`:contentions-in-transcript-minus-paired []` — an empty set produced by a
missing key rather than by an absence of contention.

The repair passes the key in rather than assuming it, records which measure was
read, and reports any arm that lacks it
(`f12_d2_denominator_check.clj:73-94`, call sites at `:122` and `:155`). The
two floors now read `:contention-measure :contending-rounds` for Arm B and
`:contention-measure :rounds-on-which-the-fired-rule-differs` for the baseline,
both with `:arms-missing-the-contention-key []`, and the baseline's true
contention rounds are **7** in paired-minus-primary and **3, 4** in
transcript-minus-paired — the same rounds Arm B reports, previously shown as
empty (`runs/F12-organise/11-d2-denominator.edn:3421-3425` and `:1071-1075`).
A `:problems` guard fires when the key is absent
(`f12_d2_denominator_check.clj:231-234`), and its control is recorded under
"Controls and gates" below. **The two measures are not interchangeable**
— both rules live, versus the fired rule differing — and the artifact now says
which one each floor read.

## Arm B: two member-carried live rules

The planted rule is slice 15's `fifth-rule` value, used rather than copied
(`f12_d2_denominator_check.clj:190-192`; `f12_d3_encoding_check.clj:24-31`).
The exchange is between `math-strategy/missing-dependency-protocol` and the
new member-carried `aif/status-gated-belief-update` rule
(`f12_d2_denominator_check.clj:17-18`, `:44-71`).

| denominator | rounds | contention | acting order | score | O4 |
|---|---:|---|---|---:|---|
| primary | 29 | none | unchanged | 15→15 | false |
| paired | 49 | 7 | changes at 7 | 15→15 | true |
| transcript | 102 | 3, 4, 7 | changes at 3, 4, 7 | 20→19 | true |

These rows, including the complete acting-order vectors before and after, are
in `runs/F12-organise/11-d2-denominator.edn:5-602`, run record
`:arm-b/:worker/:arms`. Each verdict is recomputed by
`fo/o4-precedence-governance`; none is read from a stored `:o4` field
(`f12_d2_denominator_check.clj:71`).

The difference has a measured floor. The round sets are nested:
primary ⊂ paired ⊂ transcript. All three arms exchange the same two rule IDs
with the same precedence maps, so only the admitted round set differs
(`f12_d2_denominator_check.clj:44-71`, the `before`/`after` maps at `:49-50`
and the round filter at `:46`). Round **7** lies in paired but not primary and
carries contention; rounds **3 and 4** lie in transcript but not paired and
carry contention (`runs/F12-organise/11-d2-denominator.edn:1071-1075`, run
record `:arm-b/:worker/:nested-round-set-floor`).

## The other blocker at HEAD

`require-pass!` (`futon3c:scripts/zaif_cascade_gate.clj:565`) still aborts with
`:rule-does-not-encode-an-authored-then` for **3 of 4** rules: the finding is
built at `futon3c:scripts/zaif_cascade_gate.clj:579-582` and thrown at
`:589-591` (`runs/F12-organise/11-d2-denominator.edn:3485-3536`, run record
`:head-gate-blocker`). Re-reading each cited span through
`then-correspondence` gives:

| rule and source | failing condition(s) |
|---|---|
| `war-machine/ambient-pattern-retrieval`, `war-machine/ambient-pattern-retrieval.flexiarg:16-16` | none |
| `math-strategy/missing-dependency-protocol`, `math-strategy/missing-dependency-protocol.flexiarg:59-77` | `:span-inside-the-then-block?` |
| `agent/budget-bounds-exploration`, `agent/budget-bounds-exploration.flexiarg:19-19` | `:span-inside-the-then-block?`, `:span-is-non-empty?` |
| `agent/pause-is-not-failure`, `agent/pause-is-not-failure.flexiarg:19-19` | `:span-inside-the-then-block?`, `:span-is-non-empty?` |

For all four, `:file-exists?` and `:pattern-id-matches-path?` are true; the
per-condition read-back is in run record `:head-gate-blocker/:rules`
(`runs/F12-organise/11-d2-denominator.edn:3500-3536`).

## Three arms, with measured costs

1. Read acting order over transcript rounds and score over primary rounds. This
   makes the acting-order half true by admitting rounds 3/4/7, while the score
   remains the primary 15→15. Its measured cost is two denominators and 73
   additional acting-order rounds (102 rather than 29), including the three
   contention rounds (`runs/F12-organise/11-d2-denominator.edn:5-602` and
   `:1071-1075`, run records `:arm-b/:worker/:arms` and
   `:arm-b/:worker/:nested-round-set-floor`).
2. Keep acting order and score on primary. Its measured cost is O4
   exercised-false: zero contentions, unchanged order, and 15→15 over 29 rounds
   (`runs/F12-organise/11-d2-denominator.edn:1163`, run record
   `:arm-b/:worker/:arms`, denominator `:primary`).
3. State O4 over the full play and report its denominator. Over **103** rounds
   O4 is true, contention and fired-rule differences are rounds 3/4/7, and the
   score is 21→20. Its measured cost relative to transcript is one extra round
   and a differently counted score (21→20 rather than 20→19)
   (`runs/F12-organise/11-d2-denominator.edn:607-933`, run record
   `:arm-b/:worker/:full-play`).

No arm is chosen.

## Controls and gates

All plants were read back before worker execution
(`f12_d2_denominator_check.clj:132-139`). The five verdict-moving controls are
recorded under `:controls` in
`runs/F12-organise/11-d2-denominator.edn:1148-3013`:

1. Rename the fifth rule to a nonmember: the pair is absent and O4 moves to
   not-exercised (`:rename-to-nonmember`, `:2909`).
2. Make the planted antecedent false: contention becomes empty and the wider
   O4 verdicts move true→false (`:false-antecedent`, `:2303`).
3. Use an identity precedence exchange: O4 moves false→true through its
   unchanged-precedence disjunct (`:identity-precedence-exchange`, `:2842`).
4. Force an empty denominator: the probe refuses with `:empty-denominator`
   instead of returning vacuous true (`:empty-denominator`, `:2299`).
5. Fit every rule ID to a cascade member: the read-back succeeds and
   `:rule-table-is-fitted-to-the-cascade` occurs in `require-pass!`'s failure
   vector (`:all-rule-ids-fit-cascade`, `:1149`; the finding is built at
   `futon3c:scripts/zaif_cascade_gate.clj:585-586` and thrown at `:589-591`).

A sixth control was run against the repair in the section above, in a temporary
copy of the checker put first on the classpath: replacing the baseline's
contention key with `:no-such-contention-key`, read back out of the copy before
the run, moves the acceptance run from `PASS {:problems []}` to
`FAIL {:problems [[:floor-contention-key-absent :head-baseline]]}`. Before the
repair the same absence produced two empty vectors and `PASS`.

The acceptance run prints `PASS` with `:problems []`
(`runs/F12-organise/11-d2-denominator.edn:3537`); two unchanged-tree runs
produce a byte-identical artifact, sha256
`e166e4df22c98887020cd06db0b2842eb8b195f127e8e0ba4f9ab511e4b1d5e9`
(written at `f12_d2_denominator_check.clj:260-261`).
No Lean command and no machine run were performed, so no `lake build` and no
run-lock. `git status --short` is empty specifically for
`futon3c:scripts/zaif_cascade_gate.clj` and for `futon3:checks/`; no broader
repository-cleanliness claim is made. Gate results are in the ledger row.
