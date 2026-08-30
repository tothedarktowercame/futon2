# R2-D1 findings — observation-vector corpus census

Date: 2026-08-30. Packet: R2-D1. Discovery only; no source code changed.

## Instrument and scope

- **observed.** `find data/wm-trace -maxdepth 1 -type f` and `ls -1A
  data/wm-trace` both found 56 directory entries/files. Of these, 53 match
  `wm-trace-*.edn`; the other three are `.lane-futility-index.edn`,
  `.lane-futility-index.lock`, and `wm-shadow-step.json`
  (`data/wm-trace/.lane-futility-index.edn:1`,
  `data/wm-trace/wm-shadow-step.json:1`). The packet's pre-dispatch count of
  54 entries is therefore stale at census time.
- **observed.** I read each of the 53 trace EDN files with
  `clojure.edn/read` on a `java.io.PushbackReader` in a loop until a unique
  EOF sentinel, with `:default (fn [_ v] v)` for tagged values. I did **not**
  use `edn/read-string`. This is the same required reader shape recorded at
  `holes/labs/wm-contract/facts-R2.md:307-317`.
- **observed.** That loop read 792 top-level forms, all maps and all carrying
  map-valued `:observation`. File/form position is the record identifier; the
  corpus has no `:tick` field. Each record does carry `:timestamp`, e.g.
  `data/wm-trace/wm-trace-2026-05-18.edn:1-7`. Thus 53 files reconcile to 792
  records; the worksheet's 88-record figure is not this corpus census.

## Record schema census

- **observed.** No record has a top-level `:schema-version` or `:schema`.
  `:wm-version :trace-schema-version` is absent in 682 records and has these
  values in the remaining 110: v2=2, v4=75, v6=1, v13=30, v14=2. The field is
  visible in later records, e.g. `data/wm-trace/wm-trace-2026-08-30.edn:1`;
  its absence is visible in the early records at
  `data/wm-trace/wm-trace-2026-05-18.edn:1-7`.
- **observed.** Observation key sets have exactly two shapes. 790 records have
  all 14 declared keys; two have 13 and omit only `:annotation-health`. The
  authoritative vector literal contains 14 keys at
  `src/futon2/aif/observation.clj:11-32`, while its namespace docstring still
  says 13 at `src/futon2/aif/observation.clj:2-7`.
- **observed, falsifier fired.** The two 13-key records are:
  1. `wm-trace-2026-05-18.edn`, form/line 1, timestamp
     `2026-05-18T19:42:49.284838608Z`
     (`data/wm-trace/wm-trace-2026-05-18.edn:1`).
  2. The same file, form/line 2, timestamp
     `2026-05-18T20:54:12.717822372Z`
     (`data/wm-trace/wm-trace-2026-05-18.edn:2`).
  Neither record has a separate tick id or schema version.
- **observed.** All other records carry the 14-key set
  `active-repo-ratio, annotation-health, attack-coverage, consulting-pct,
  coupling-density, depositing-signal, loop-health, mathematics-pct,
  mission-health, portfolio-pct, sorry-count-norm, stack-pct,
  support-coverage, ticks-firing-ratio`; the producer returns that literal map
  at `src/futon2/aif/observation.clj:45-74`.

## Likelihood/typed-absence partition

- **observed.** Eight channels have likelihood models:
  `annotation-health, sorry-count-norm, mission-health, active-repo-ratio,
  support-coverage, attack-coverage, coupling-density, ticks-firing-ratio`
  (`src/futon2/aif/belief.clj:913-927`). `predict-observation` documents that
  only these are returned and others are absent at
  `src/futon2/aif/belief.clj:1053-1069`.
- **observed.** The six delivered channels without likelihoods are
  `loop-health, stack-pct, consulting-pct, portfolio-pct, mathematics-pct,
  depositing-signal`, the set difference between the declarations above.
  The code calls their classification `:n-a-by-design` at
  `src/futon2/aif/belief.clj:923-927` and explains that they have no emission
  row at `src/futon2/aif/belief.clj:940-947`. This status is prose/set
  membership, not a per-record typed `None` receipt.

## `:acknowledged?` and operator content

- **observed.** Command `rg -n ':acknowledged\\?|acknowledged\\?'
  futon2/src futon3c/src` found one non-fixture producer:
  `src/futon2/aif/lane_futility.clj:321-334` hard-codes
  `:acknowledged? true` on synthetic lane-futility nags. Therefore the
  packet/record's unqualified "expected: none" is false of the artifact.
- **observed.** The same search shows the production nag consumer is a
  four-term conjunction at
  `../futon3c/src/futon3c/wm/operator_lane.clj:29-33`; its production-path
  comment explicitly says acknowledgement persistence is not wired at
  `../futon3c/src/futon3c/wm/needs_you.clj:156-159`. The hard-coded synthetic
  producer is not an operator-turn acknowledgement producer.
- **observed.** A recursive key walk over all 792 trace maps found zero
  `:acknowledged?` keys. A key-name scan over every `:observation` map found
  zero keys matching `operator|turn|morning|brief`. The current observation
  input destructuring has no turn input at
  `src/futon2/aif/observation.clj:34-49`.

## Morning-brief states

- **observed.** Each of `:morning-brief-events`,
  `:morning-brief-held-events`, and
  `:morning-brief-consumed-event-ids` is present in 32 records, empty in all
  32, nonempty in 0, and absent in 760. The producer defaults all three to
  `[]` at `src/futon2/aif/trace.clj:245-260`, so present-and-empty does not
  establish observation or consumption.
- **observed.** The 32 present records are all records in
  `wm-trace-2026-07-14.edn:1-14`, `wm-trace-2026-07-15.edn:1-6`,
  `wm-trace-2026-07-16.edn:1-6`, `wm-trace-2026-07-17.edn:1`,
  `wm-trace-2026-07-18.edn:1`, `wm-trace-2026-07-19.edn:1`,
  `wm-trace-2026-07-21.edn:1-2`, and `wm-trace-2026-08-30.edn:1` under
  `data/wm-trace/`. The last record is why the earlier 31-record census at
  `holes/labs/wm-contract/facts-R2.md:351-362` is now stale.

## Required refusal: turn-channel semantics

- **observed.** The requirement rules out an operator-turn count because it
  measures presence rather than content
  (`holes/labs/wm-contract/R2-glossary-formalisation.md:69-84`). No accepting
  witness exists there.
- **refusal — inferred, untested.** I cannot choose a normalization because
  the record leaves two materially different candidates open:
  1. **Association-content channel:** aggregate the typed mark on each
     turn→pattern association into `[0,1]`; Joe must define the polarity and
     aggregation of ✘, ✓, and 💡.
  2. **Likelihood/evidence channel:** treat a typed mark as evidence about a
     named hidden pattern state and derive the scalar from that likelihood;
     Joe must name the hidden state and its three mark-conditioned rows.
  These are the two unresolved interfaces recorded at
  `holes/labs/wm-contract/R2-glossary-formalisation.md:86-96`. Selecting either
  without those definitions would replace operator content with a builder's
  invented meaning.

## Packet corrections

- **observed.** At census time the accurate counts are 56 directory entries,
  53 trace EDN files, and 792 trace records—not 54 entries or 88 records
  (`data/wm-trace/.lane-futility-index.edn:1` records the same 792-record
  fingerprint census).
- **observed.** `:acknowledged?` has one synthetic hard-coded producer, but
  no operator acknowledgement persistence producer
  (`src/futon2/aif/lane_futility.clj:321-334`;
  `../futon3c/src/futon3c/wm/needs_you.clj:156-159`).
