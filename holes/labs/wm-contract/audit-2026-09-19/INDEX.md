# WM defect-class audit, 2026-09-19

Six zai agents, one defect class each, discovery only. Joe's instruction: we
kept finding these one at a time as they stopped a run; find the class instead.
Each dimension was generalised from a defect that had actually stopped a run in
the preceding 24 hours.

65 findings, 14 `:confirmed` (counted from the files, not the agents' prose). Files are the agents' raw output, unedited. The
verdicts below are mine, from reading the code, not relayed.

| file | dimension | agent | n | confirmed |
|---|---|---|---|---|
| d1-absent-value.edn | absent value into a fn assuming presence | zai-9 | 13 | 0 |
| d2-key-list-drift.edn | hand-copied key list vs its source map | zai-10 | 6 | 2 |
| d3-nil-as-identity.edn | nil as an identity in group/dedup keys | zai-11 | 9 | 0 |
| d4-silent-miss.edn | lookup miss becomes a skip, not a refusal | zai-12 | 13 | 5 |
| d5-unbounded-write.edn | durable write with no size bound | zai-13 | 12 | 5 |
| d6-stale-controls.edn | negative control whose plant stopped landing | zai-14 | 12 | 2 |

## Fixed from this audit

- futon2 `353cdc29` — three `pprint` sites in the repair store. Measured on a
  real 19.4 MB finding: pprint 47,306 ms / 19.4 MB, pr-str 212 ms / 12.0 MB.
  223x faster, 38% smaller, and those 47 seconds were held inside the contended
  store lock. (d5, zai-13.)
- futon2 `f8a526f6` — `group-by :mission/repo` unguarded while its sibling 179
  lines earlier guards the same field. `:mission/repo` is present on all 390
  live missions and null on 298 (76%); all 298 were one nil bucket. (d3,
  zai-11.)
- p4ng `6a683aa` — `control_plants_check.py`, built from d6's proposed check.
  0.03s static pass; falsified by breaking a plant and confirming it reports
  CANNOT-LAND.
- futon2 `b6da1420` — T10 (found by hand while the swarm ran, same class as d4).

## One d5 conclusion is refuted — do not act on it

`d5-unbounded-write.edn` finding 1 concludes that the 11 MB `:judgment`
payloads inside `:backtrace :checkpoints` are "safe to elide" because nothing
reads them back. The reader survey is correct and I reproduced it across
futon2, futon3c, p4ng, futon0 and futon1b, all file types: the only reads are
`[:backtrace :code-state :repo]` (full_loop_runner.clj:598),
`[:backtrace :job-id]` (:3137) and `[:backtrace :trip-report]` in a test.

The conclusion still does not follow. The evidence that diagnosed the T8
livelock on 2026-09-19 — the `:artifact-binding` map carrying
`:text-artifact-ref "2e5e7409"`, `:observed-head "9a6a012f..."` and
`:corroborates? false` — lives at

    [:backtrace :checkpoints :selection    :judgment ...]
    [:backtrace :checkpoints :construction :judgment ...]

49 occurrences, all inside the payloads declared safe to elide. Had they been
elided when those findings were written on 2026-09-15, it would not have been
possible to establish that the author claimed the pre-dispatch head while HEAD
had genuinely moved, and the chain back to the 28-hour-stale JVM would have
been unreachable. That diagnosis produced b6da1420.

So: nothing reads them programmatically; they are read by hand, and that is
what a backtrace is for. "No code reads it" is the wrong test for a diagnostic
artifact -- under that test every backtrace in every system is dead weight
until the first time you need one.

What to do instead, if the size is worth attacking:
- serialise cheaply rather than elide (353cdc29 took a real 19.4 MB finding
  from 47,306 ms to 212 ms; the cost was the pretty-printer under the lock, not
  the bytes);
- elide by reference — write the judgment to a content-addressed blob and keep
  its digest in the backtrace. `record-finding!` already does this: its
  backtrace is `{:trip-report <path>}`, a pointer rather than the payload.

The other five confirmed d5 findings stand. Two are unacted:
tripwire.clj:815 (cross-run-observation still parses the whole 90 MB store into
memory per click; the 2026-09-18 fix bounded only the disk copy) and
full_loop_cohort.clj:229 (cohort cells, unbounded, under the cohort lock, re-read
wholesale by `ledger` per click).

## Two discriminators that earned their keep

**Does a sibling call site already guard this field?** This is stronger than
arguing that nil could occur: it is the codebase's own admission that it does.
It is what made the `:mission/repo` finding cheap to confirm, and it is what
says the `find_reconciliation.clj:29` finding has no evidence either way —
`:treatment` and `:disposition` appear at lines 25 and 29 and nowhere else.

**Does this code have a production caller?** Cheap, and it re-ranks findings
hard. Apply it carefully: my first pass searched `src/ scripts/ test/` and was
about to record `find_reconciliation.clj` as having no caller. It has one, in
`holes/labs/wm-contract/f11_f2_reconcile.bb`. An absence established by an
under-scoped search is the same defect as `d4`, one level up.

## The confusion that showed up three times tonight

Present-with-a-null-value is not the same as absent, and three separate things
tonight turned on it:

- `(get-in m ks default)` returns the default only when the key is ABSENT. A
  key present with a null value returns nil, and the default never fires.
  Demonstrated, not argued: `(get-in {:mu {:urgency nil}} [:mu :urgency] 0)`
  => nil, and `(double nil)` throws. `(or (get-in m ks) 0)` covers both.
- `:mission/repo` is present as a key on all 390 live missions and null on 298.
  I first reported that as "298 missions without `mission/repo`" -- same
  number, wrong claim.
- `(:ok adm)` on a nil `adm` is falsey, so the code took the branch that assumed
  a refusal map and called `(name nil)`. That is the defect that started the
  night.

Anything checking this class has to distinguish the two: `(contains? m :k)`
tests one, `(or (:k m) default)` covers both, and a `get-in` default covers
only the first.

Both zai-9 (d1) and zai-11 (d3) independently arrived at the same
discriminator for ranking findings in this area -- the GUARDED-SIBLING
DIFFERENTIAL: the bugs are where one call site misses a guard its neighbours
apply to the same field, not where a raw grep matches. Two agents on different
dimensions converging on it is some evidence it generalises.

## d2 finding 3, narrowed against the artifacts

zai-10 reports that `trace-record` hand-copies judgement keys and omits
`:cascade-lanes`, `:cascade-horizon`, `:cascade-sources`,
`:effective-run-configuration` and `:input-status` — "the horizon recorded with
its authority never lands in any trace".

The projection gap is real: all five are produced by `judge` and read zero
times by `trace-record`. But not landing in a TRACE is not the same as not
landing anywhere, so I checked the durable artifacts — 62 trace files under
data/wm-trace and the cohort 57 cells:

    :cascade-sources               0 traces   0 cohort cells
    :cascade-horizon               0 traces   0 cohort cells
    :cascade-lanes                 0 traces   0 cohort cells
    :effective-run-configuration   0 traces   1 cohort cell
    :input-status                  3 traces   0 cohort cells

So THREE of the five are genuinely absent from all durable evidence, and two
reach it by another path — `:effective-run-configuration` through the runner's
selection checkpoint, `:input-status` through the trace after all.

The narrowed version is the more useful one. The declared cascade sources and
the horizon are the subject of the WM contract work (e2e1b477: the judge loads
resources/wm/cascade-sources/*.edn and observes facts per tick). If neither
reaches any durable record, then "the decision used these declared sources" is
not a claim any artifact can be asked to support. That is a schema-30 change
with pinning consequences, so it is queued, not done.

## The summaries drifted from the artifacts, including mine

Counted from the EDN files rather than from anyone's prose:

    d1  13 findings   0 confirmed   3 likely   10 speculative
    d2   6            2             1           3
    d3   9            0             3           6
    d4  13            5             5           3
    d5  12            5             3           4
    d6  12            2             8           2
        --                --
        65            14

Three agents' bell summaries disagree with their own files on the split —
zai-13 reported 6 confirmed where its file has 5, and d4 and d6 each shift one
finding between likely and speculative. And my own first version of the table
above said 12 confirmed and gave d2 as 0: I built it from a pass that ran
before d2 had landed and never re-ran it.

Small in itself, and worth recording because it is this audit's own subject.
The prose and the artifact are a hand-copied pair, and only one of them is
checkable.

## Method notes, for the next pass

- Requiring `:why` (why the bad case is reachable) and capping at 15 findings
  kept these files readable. A grep dump was declared a failed packet up front.
- `:confirmed` clustered in d4 and d5, the two dimensions where the bad case
  can be demonstrated from data on disk. d1/d2/d3 are code-reading dimensions
  and their agents correctly ranked most findings `:speculative`.
- Distinguish *key absent* from *key present, value nil*. I reported "298
  missions without `mission/repo`" when the key is present on all 390 and the
  value is null on 298 — same number, wrong claim. `(or (:k %) default)` covers
  both; `(contains? m :k)` covers one.
