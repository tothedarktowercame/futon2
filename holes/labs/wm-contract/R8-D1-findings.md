# R8-D1 findings — trace census

Date: 2026-08-30. Packet: R8-D1. Status: discovery only.

## Instrument and limits

- **Observed.** I enumerated only regular files named
  `data/wm-trace/wm-trace-*.edn`, then used `clojure.edn/read` repeatedly on a
  `LineNumberingPushbackReader` until `:eof`, with a default tagged-literal
  reader. This is the repository's established one-form-per-tick method
  (`scripts/wm_trace_census.bb:5`, `scripts/wm_trace_census.bb:35-39`). Command:
  `clojure -M -e '<reader loop and census>'`. It does not inspect the JSON
  shadow file or hidden index files (`data/wm-trace/wm-shadow-step.json:1`).
- **Observed.** `ls -1 data/wm-trace | wc -l` returned 54 visible entries;
  filtering to the named EDN trace files returned 53 files; the reader loop
  returned 792 top-level forms. The corpus runs from
  `data/wm-trace/wm-trace-2026-05-18.edn:1` to
  `data/wm-trace/wm-trace-2026-08-30.edn:1`.
- **Observed.** Top-level records have `:timestamp`, not `:tick` or `:tick-id`;
  the outcome's separate millisecond tick is documented at
  `holes/labs/wm-contract/R8-glossary-formalisation.md:52`. I therefore use
  each record timestamp as its tick identifier below.

## Census

- **Observed.** Schema is nested at `[:wm-version :trace-schema-version]`, not
  top-level `:schema-version`: 682 forms have no recorded schema; the known
  versions are v2=2, v4=75, v6=1, v13=30, v14=2. A v14 example is
  `data/wm-trace/wm-trace-2026-08-30.edn:1`; an early unversioned example is
  `data/wm-trace/wm-trace-2026-05-18.edn:1`.
- **Observed.** `:prediction-errors` occurs in 790/792 forms and
  `:precision-state` in 787/792. The five records not recomputable from both
  fields are the first five timestamps between
  `data/wm-trace/wm-trace-2026-05-18.edn:1` and
  `data/wm-trace/wm-trace-2026-05-18.edn:4`:
  19:42:49, 20:54:12, 21:33:02, 21:40:07, 21:58:44 UTC.
- **Observed.** The dispatch's 88 is reproducible as exactly the count of forms
  with `:realized-outcome` (and also `:enactment`), not the trace-record count.
  They run from tick `2026-07-02T13:30:55.818776522Z` at
  `data/wm-trace/wm-trace-2026-07-02.edn:16` to tick
  `2026-07-06T12:04:27.412283747Z` at
  `data/wm-trace/wm-trace-2026-07-06.edn:12`. The first outcome visibly carries
  its distinct `:realized-outcome :tick` at the former pointer.

## F recomputation and key comparison

- **Observed.** I recomputed the commissioned formula
  `0.5 * mean(precision-state[k].precision * prediction-errors[k].error^2)`
  (`holes/problems/P-R8.md:8-10`) for every form having both inputs: 787/792
  forms, range 0.1903302937544315 to 10.637526080614668.
- **Observed.** `:variational-free-energy` is absent from 760 forms and present
  in 32. For all 32 stored values, recomputed F equals the stored double
  exactly (maximum absolute delta 0.0). The first is 0.23702204619147013 at
  tick `2026-07-14T09:49:57.608850393Z`
  (`data/wm-trace/wm-trace-2026-07-14.edn:1`); the last is
  0.5223336238034448 at tick `2026-08-30T10:54:44.860119595Z`
  (`data/wm-trace/wm-trace-2026-08-30.edn:1`). This agrees with the glossary's
  intended key, but not its historical “schema v7+” boundary: the corpus has
  no recorded v7 (`holes/labs/wm-contract/R8-glossary-formalisation.md:28`).
- **Observed.** Every one of the 792 forms has top-level `:free-energy`. In the
  760 forms without stored F it is a G map containing `:G-total`; in the 32
  forms with stored F it is instead a controller map containing
  `:controller-score`. Compare the old shape at
  `data/wm-trace/wm-trace-2026-07-09.edn:1` with the later shape and separate F
  at `data/wm-trace/wm-trace-2026-07-14.edn:1`.
- **Observed.** The 88 realized-outcome forms occur on 07-02 through 07-06,
  before stored F begins on 07-14; consequently all 88 do have G under
  `:free-energy` and no `:variational-free-energy`. This is a valid subset
  finding, not the corpus size (`data/wm-trace/wm-trace-2026-07-02.edn:16`,
  `data/wm-trace/wm-trace-2026-07-06.edn:12`,
  `data/wm-trace/wm-trace-2026-07-14.edn:1`).

## Selection-gain ticks

- **Observed.** `:selection-gain` occurs in only 32/792 forms. Every present
  state is exactly `{:selection-gain 1.0, :perf-history [], :mean-perf nil,
  :samples 0}`. It is absent from the preceding 760 forms, so “1.0 throughout”
  is true only of recorded gain states, not all trace forms
  (`data/wm-trace/wm-trace-2026-07-09.edn:1`,
  `data/wm-trace/wm-trace-2026-07-14.edn:1`).
- **Observed.** Gain-bearing tick ids by file (each pointer contains the stated
  appended forms):
  - `data/wm-trace/wm-trace-2026-07-14.edn:1` through
    `data/wm-trace/wm-trace-2026-07-14.edn:13`: 09:49:57, 10:31:07,
    15:03:41, 15:08:39, 16:29:08, 16:43:16, 16:51:22, 17:00:21, 17:28:45,
    17:47:16, 18:36:10, 20:09:12, 20:16:36, 20:23:36 UTC.
  - `data/wm-trace/wm-trace-2026-07-15.edn:1` through
    `data/wm-trace/wm-trace-2026-07-15.edn:5`: 20:30:03, 20:47:33,
    21:37:11, 21:42:23, 22:24:19, 23:35:47 UTC.
  - `data/wm-trace/wm-trace-2026-07-16.edn:1` through
    `data/wm-trace/wm-trace-2026-07-16.edn:5`: 10:17:45, 11:00:52,
    13:53:42, 14:17:49, 15:18:40, 16:16:26 UTC.
  - `data/wm-trace/wm-trace-2026-07-17.edn:1`: 23:42:25 UTC.
  - `data/wm-trace/wm-trace-2026-07-18.edn:1`: 14:33:21 UTC.
  - `data/wm-trace/wm-trace-2026-07-19.edn:1`: 00:05:32 UTC.
  - `data/wm-trace/wm-trace-2026-07-21.edn:1`: 05:53:19, 10:05:12 UTC.
  - `data/wm-trace/wm-trace-2026-08-30.edn:1`: 10:54:44 UTC.

## Refusals and corrections to the packet

- **Observed; refusal.** I refuse “88 trace records”: the mandatory reader
  loop finds 792. Eighty-eight names the realized-outcome subset
  (`holes/problems/P-R8.md:11-14`, already amended during this discovery).
- **Observed; refusal.** I refuse “every trace record has no F”: 32 records
  already carry exact recomputable F (`data/wm-trace/wm-trace-2026-07-14.edn:1`,
  `data/wm-trace/wm-trace-2026-08-30.edn:1`).
- **Observed; refusal.** A future checker cannot demand recomputation for all
  792 without a typed missing-input result: five earliest records lack a
  usable `:precision-state` (`data/wm-trace/wm-trace-2026-05-18.edn:1`,
  `data/wm-trace/wm-trace-2026-05-18.edn:4`).
- **Inferred, untested.** D2 should distinguish at least three dispositions:
  missing F despite computable inputs (755 records), valid stored F (32), and
  insufficient inputs (5), rather than treating every record as the same
  falsifier (`holes/problems/P-R8.md:26-30`).
