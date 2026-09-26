# C-R17-TRACE-D — construction traces did not enable accumulation

2026-09-26. Discovery only, codex-1 for claude-8 review.
Source pin: futon2 `0c7a284b6f61c85d10575e928130a20f0c4f9461`.
WM = `scripts/futon2/report/war_machine.clj`; unqualified source names below
are relative to `src/futon2/aif/`. Historical pins are listed separately.
No runtime judge, flight, click, live load, registry edit or data write.

**Cause:** the cited September 21–23 records are full-loop construction
traces, not `run_tick_once` diagnostic ticks: the runner omits the accumulation
opts from its judge call (`full_loop_runner.clj:4831–4841`), then independently
calls `trace/write-trace!` (:5124–5129). The gate is false, not a caught
accumulation exception. The premise that these are one-shot-route records is
incorrect. This corrects the premise without weakening any carry invariant.

## 1. Which branch the records support

### Current diagnostic route, and why its catches do not explain these records

`scripts/futon2/run_tick_once.clj:246–276` merges `:trace? true` and
`(wm/accumulation-config)`, then associates `:run-id` and optional trace-dir.
`:290–292` passes those opts to `generate-war-machine`. `WM:7947,8021` passes
the same opts to judge, whose destructuring is at :7078–7084. There is no
intermediate select-keys dropping the config on **that** route. Its gate
`:7351` fires; configured entity presence is checked at :1534–1538, predecessor
state at :1539–1548, and recurrence at :1554–1558.

The Throwable catch at `run_tick_once.clj:102–109` catches only selector
resolution, not the judge. The ExceptionInfo catch at :343–348 handles only
run-lock refusal (exit 3), rethrowing other exceptions. A thrown accumulation
failure happens before judge publication (`WM:7704–7733`) and before the
one-shot receipt write (`run_tick_once.clj:290–310`). It cannot explain a
successfully written trace with the state silently removed. A caught selector
resolution error is a different event, carried at :296–298.

### Actual retained evidence

I parsed the four daily files as successive EDN forms, without the tolerant
production reader. Counts are **4 on 09-12, 4 on 09-21, 4 on 09-22, 9 on
09-23**. Each of the six named runs occurs once as a top-level trace form,
not several records per run. All 17 forms on 09-21..23 have
`[:wm-version :trigger] :duree-click-on-demand`; all contain the configured
entity in `:mu-post`; none contains `:accumulation-state`. For the six named
runs both loaded runner/WM digests in the receipt's
`[:runner/source :namespaces <ns> :loaded-source :sha256]` equal SHA256 of
`git show <trace-wm-version-sha>:<source-path>`. This checks actual loaded-file
provenance rather than assuming the HEAD at trace-write time was loaded.

| Trace file under `data/wm-trace/`, form | `:run/id` | Recorded Git pin | Historical runner judge-call lines |
|---|---|---|---|
| `wm-trace-2026-09-21.edn`, 4 | `2026-09-21-1790033693` | `abde70b9c3c4caa72d0a48b93889c7fe696705b4` | 3926–3933 |
| `wm-trace-2026-09-22.edn`, 1 | `2026-09-22-1790037762` | `3134b61f11cad3ae18d1d676f2070b5782d225cf` | 4041–4048 |
| same, 2 | `2026-09-22-1790053967` | `16d4482c2c1e9d47d22f9e849ce4990fed92139a` | 4041–4048 |
| same, 4 | `2026-09-22-1790110142` | `33f1662f09b763a3d0296ff797a82be5a6cc7ee9` | 4105–4112 |
| `wm-trace-2026-09-23.edn`, 1 | `2026-09-23-1790131591` | `c24c903b9b9577d6192ea4c9918a5da4f8a81d2a` | 4092–4099 |
| same, 2 | `2026-09-23-1790136186` | `55b1c84a3d917cb487b6c8293aa94f71788b956a` | 4201–4208 |

Every historical call above forwards only
`[:accumulate-strategic-habit? :run-id :loaded-code-identity :cascade-habit-path]`
and associates advisory=false/defer-render=true. For example, at `3134b61f`,
WM:6476–6482 defaults trace? false and :6747 gates on trace? OR entity; runner
:4298–4303 separately writes the construction trace. Thus the default path's
actual gate inputs are **false / nil**, despite a run-id being present.
A custom `:judge-fn` is technically supported; these records do not serialize
the raw judge opts. The matched source and construction-route evidence support
the default-path explanation, not a direct logged function-call assertion.

In all six trace records the TRACE hop under `:wm/route` quotes:

```edn
{:node :TRACE :via "futon2.aif.trace/write-trace!"
 :reason {:kind :routing-rule :rule :constructed-selection-persisted
          :question "Does the constructed selection require operator review?"}}
```

Top-level `:trace/reason` is absent because the writer puts it on the route
hop (`trace.clj:631–644,661–667`). None of these records carries an
accumulation refusal/error/receipt. This is **silent disabled accumulation**,
not evidence that a thrown failure was swallowed. Entity presence in the
retained posterior also does not diagnose an entity-missing throw.

The corresponding exact receipt paths are
`data/wm-runs/tick-run-record-<run-id>.edn`, using the six IDs above.
All say `:selectorSeam "live:validated-selection"`, contain `:runner/source`
and a decision, and lack `[:decision :accumulation]`. Their :traceWritten is
true except `2026-09-21-1790033693`, which says false despite its retained
trace. That run's route is `:via :build-failed`; the 09-22 `1790037762` route
says `:guardrail-refusal`; 09-23 `1790136186` says `:grounded-change`. Those
are full-loop close outcomes, not R17 refusals. The diagnostic one-shot receipt
shape instead contains storeBasis/entriesRead/route-verdict
(`run_tick_once.clj:217–234`). Do not classify routes by the filename alone.

## 2. Two different meanings of “first 09-21 run”

The retained daily filenames between 09-12 and 09-21 are exactly
`wm-trace-2026-09-12.edn` and `wm-trace-2026-09-21.edn`; there are no 13..20
files. `wm-trace-2026-09-12.edn.pre-migration-backup` is also present, but
`trace.clj:718–725` excludes it. Other directory entries are earlier daily
files and `wm-shadow-step.json`, likewise not an additional daily predecessor.
`recent-trace-records` (:739–754) returns up to twelve chronological forms;
`WM:7147` uses only the last.

The **first retained September 21 run** is `2026-09-21-1789951020`, timestamp
`2026-09-21T00:39:14.826081074Z`, form 1. Reconstructing the pre-run prefix
from today's retained files, its last predecessor is September 12 form 4:

```edn
{:timestamp "2026-09-12T17:28:09.498448087Z"
 ;; :run/id absent
 :accumulation-state {:last-tick "2026-09-12T17:28:09.498448087Z" ...}
 :accumulation-initialization
 {:authority :declared :prior 1.0 :model/revision "wm-status-v1"}}
```

`trace-record-identity` (`WM:1518–1526`) falls back to that timestamp. It
**equals** last-tick. No migration or chain-gap refusal is forced by that
predecessor. The disabled runner did not advance/preserve its matrix on the
next trace, breaking availability for later enabled accumulation.

The **named run** `2026-09-21-1790033693` is form 4 that day, timestamp
`2026-09-21T23:37:34.513476835Z`. Its retained predecessor is form 3:

```edn
{:run/id "2026-09-21-1789964661"
 :timestamp "2026-09-21T04:26:36.942700495Z"}
;; :accumulation-state absent
```

The intervening form 2 is `2026-09-21-1789952479` at
`2026-09-21T01:03:31.261801898Z`. An **enabled** adapter against form 3 would
throw `:accumulation-migration-required` at WM:1540–1542, before initialization
or chain checking; the actual default runner has the gate off.

These are reconstructions from retained append order, not a time-machine read
of the filesystem at invocation. There is specific evidence for caution:
the pre-migration backup has four forms and the same last timestamp but **no
accumulation-state**, whereas today's last form has it. Today's seeded state
has `:initialization`, no `:previous-tick`, and the record has no
`:accumulation-update-input`. Presence of that state therefore does not by
itself prove that a live recurrence ran on September 12; at least a changed
snapshot exists. Who/when migrated it is not established by these bytes.


The reconstructed twelve-record window before the first 09-21 form is below;
paths are under `data/wm-trace/`. Before the named late run, drop the first
three rows and append the first three 09-21 records quoted above.

| Daily file | Record identity (run/id, else timestamp) |
|---|---|
| `wm-trace-2026-09-04.edn` | `8ae111bc-d758-45f3-9c5b-f98832e10bb6` |
| `wm-trace-2026-09-04.edn` | `308d1622-55ca-4671-aab7-273731d3c99e` |
| `wm-trace-2026-09-04.edn` | `67c72ac3-5d8b-4ffd-8732-650864d18182` |
| `wm-trace-2026-09-04.edn` | `c149f9de-669c-4817-9b0e-ed4aad77db79` |
| `wm-trace-2026-09-07.edn` | `85cb5a19-d053-4aa7-a7ad-f667f5e4321d` |
| `wm-trace-2026-09-07.edn` | `f24ccb9f-50ca-4654-aa3c-bedfe66eeaea` |
| `wm-trace-2026-09-07.edn` | `36820e88-3d68-499d-b359-2d8dbe9743de` |
| `wm-trace-2026-09-11.edn` | `2026-09-11T20:25:55.547326616Z` |
| `wm-trace-2026-09-12.edn` | `2026-09-12T13:16:12.144484983Z` |
| `wm-trace-2026-09-12.edn` | `2026-09-12T14:02:27.232494961Z` |
| `wm-trace-2026-09-12.edn` | `2026-09-12T16:48:03.815122125Z` |
| `wm-trace-2026-09-12.edn` | `2026-09-12T17:28:09.498448087Z` |

## 3. Scheduled route

Confirmed: `scripts/wm_scheduled_run.clj:133` passes only
`(wm/accumulation-config)`. WM:111–118 returns entity/init, no run-id or scan-id;
WM:7998–8019 creates no scan-id in scan-data. WM:7354 consequently supplies nil
tick-id and :1532–1533 throws `:accumulation-identity-missing` before checking
the entity or predecessor. The scheduled `-main` try begins at :101 (not :2,
which is a namespace docstring); catch Throwable at :180–183 prints timestamp,
`ERROR`, and exception message to stderr, then exits 1. Its external trace
write at :148–152 and close-loop at :141–143 are not reached. No partial
judgement trace is written by that failure path.

The retained 17 records dated September 21–23 are all run/id-bearing clicks;
there are **zero run/id-less forms after September 12** in this directory.
This is no evidence of a successful scheduled production tick after that day.
It does not prove cron ran and failed: scheduler invocation/log evidence is
not in these records. The one run/id-less state-bearing form on 09-12 is also
stamped `:duree-click-on-demand`, not a scheduled trigger.

For TRACE-I, mint a UUID run-id once **before** the scheduled judge, following
`run_tick_once.clj:329–333`, pass it as `:run-id`, and preserve it on the trace.
Do not reuse the present close-loop millisecond expression (:143): it is
computed only after judgement, only when live-wire? is true, and is an actuator
clock/dedup input. Changing that separate clock's type is unnecessary.

## 4. TRACE-I scope, tests, and the remaining initialization boundary

The historical cause does not justify changing the diagnostic forwarding:
that route already forwards correctly. TRACE-I should make enabled runs
record their accumulation outcome without throwing selection away. It cannot
honestly promise to resume accumulation on today's live tail: that tail lacks
state, and the requested migration-required test explicitly forbids inventing
a replacement prior. Resumption there needs an explicit lineage/migration
decision in **C-R17-LINEAGE-I**, not “skip backwards to September 12” or reset
to 1.0. Flight/full-loop forwarding remains its own packet.

File-by-file proposal:

- `trace.clj`: strict authoritative history reader which reports malformed
  EDN, unreadable/invalid paths, and actual empty history distinctly. Keep the
  existing tolerant diagnostic reader (:684–704) separate; wrapping only
  WM's catch cannot expose records it silently skipped. Persist the same
  `[:decision :accumulation]` receipt used by the caller, alongside existing
  state/update-input fields (:588–592).
- WM: replace the :7145 empty-vector catch on the accumulation authority path
  with an explicit read result. Wrap only accumulation adaptation/validation
  (:1528–1559,:7351–7357), retaining exact typed reasons. On read failure:
  `{:status :absent :reason :trace-read-failed :error {:class ... :message ...}}`.
  On adapter refusal: `{:status :absent :reason :accumulation-migration-required
  ...}` (or the actual refusal kind). No step or initialization on failed read.
  Healthy success receipt: `{:status :accumulated :state-sha256 ...
  :previous-id ... :tick-id ... :entity ... :model/revision ...
  :initialization? ...}`. Hash specified serialized state bytes, not a second
  calculation. A publish failure must remain typed rather than claiming
  durable success. Leave other consumers' history semantics out of scope.
- `scripts/futon2/run_tick_once.clj`: its current receipt (:217–234) does not
  even retain :decision. Add the accumulation receipt under
  `[:decision :accumulation]` from the actual judgement, not a later reread.
  No new config merge is needed. Existing uncaught accumulation failures then
  become ordinary completed ticks with a typed learning absence.
- `scripts/wm_scheduled_run.clj`: stable UUID before judge as above; retain the
  receipt on its externally written trace. Do not also enable an internal
  trace write and accidentally publish twice.
- `full_loop_runner.clj`: if TRACE-I is to retain the receipt for disabled
  clicks too, add :accumulation to the decision select-keys (:731–738), and
  WM emits `{:status :absent :reason :accumulation-not-configured}` for the
  disabled gate. This is receipt forwarding only, not enabling accumulation.
  Otherwise defer this one-line retention to FLIGHT-I and state that limit.
- Tests: extend `test/futon2/report/war_machine_accumulation_test.clj` for the
  adapter boundary; add hermetic strict-history and one-shot/scheduled receipt
  tests using temporary files and the real numerical kernel. Existing adapter
  test :23 already constructs a missing-state predecessor, :24 a chain gap;
  new tests must go through record persistence, not just assert a throw.

Required acceptance cases: (1) missing-state predecessor writes
`:accumulation-migration-required` on the record and selection completes;
(2) healthy chain writes `:accumulated`, matching state SHA and predecessor;
(3) unreadable history records :trace-read-failed and invokes neither step nor
initialize; (4) malformed last form is a read failure, never cold start;
(5) healthy empty history uses only explicitly declared initialization;
(6) scheduled generated ID equals trace run/id and state last-tick; (7) the
one-shot receipt retains the same decision receipt as the trace. A refusal
record may leave later attempts migration-required; TRACE-I must not claim
lineage recovery or silently synthesize unchanged carry to avoid that result.

Concurrency, entity/revision checks, receipt-only tail lineage and migration
are **C-R17-LINEAGE-I**, per this packet's boundary. Existing numerical step
remains the only recurrence. No runtime repair or data migration is performed
by this discovery note.

## Evidence identity and unowned findings

Daily-file SHA256 at read time (files are mutable and untracked):

- 09-12: `3b25d2d43e3ebbcf19fc6f82e103ae49907b81d0cf009fa8d7884e74ef53e175`
- 09-21: `e4de749ecca6e17728426ededc18884363c2e55c7041822f6a5f15657425c159`
- 09-22: `ab0772a1815f3105e89795b62614e05603d8233f0db366158df3f93cdb2a6dd0`
- 09-23: `2344b1b77b4454a73bdc187a40f341434116347037360b15bd9ad91c6a5f6ca2`

Unfixed: the 09-21 named receipt says traceWritten=false despite its trace;
the September 12 seeded state differs from the backup and has no update-input;
these observations do not certify historical live accumulation. The packet's
one-shot attribution and repeated-record premise are contradicted by the
retained forms. Scheduled identity omission, tolerant history corruption,
concurrency and lineage gaps remain as previously reported. No registry or
source edit; no claim to have recovered a historical exception log.
