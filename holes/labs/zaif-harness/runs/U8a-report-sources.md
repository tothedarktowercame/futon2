# U8a — sources from which a ZAIF report can be re-derived

Discovery date: 2026-09-02. This is a read-only survey of the current edge
facilities. No live JVM namespace was loaded and no store record was written.

## Result

The reporting gate is not yet buildable without exposing a genuine failure.
Decision evidence and live R16 witnesses are populated and join by session and
turn. Z1 exists and is callable, but recorded decision bodies have no populated
mission, so Z1 mission attribution cannot attribute any inspected decision.
The event-derived mission-status view also produces false status signals from
ordinary chat prose. A correct gate would therefore fail on mission attribution
and status rather than silently taking either value from a mission document.

## 1. Z1 Ledger query library

**Location and API.** Z1 is not in a production `src/` tree. It is the Babashka
namespace `z1-views` in
`futon2/holes/labs/M-zaif-harness/z1_views.clj`: namespace and design at lines
3-40, store endpoint at line 52, bounded GET/page implementation at lines
81-151. Its named replayable views are:

- `operator-turns` at line 206;
- `gamma-events` at line 283;
- `mission-attributed` at line 365;
- `mission-status` at line 480;
- the later `bug-queue` addition at line 597.

The CLI dispatch enumerates all five at lines 665-682. Each view returns its
query parameters with the results. The live-store tests load this file at
`z1_views_test.clj:23-37`; read-only endpoint assertions are at lines 143-156
and mission-status tests at lines 197-227.

**Executed probe.** From `/home/joe/code/futon2`:

```text
$ bb holes/labs/M-zaif-harness/z1_views.clj mission-attributed \
    --since 2026-08-01T00:00:00Z --limit 50
{:view :mission-attributed,
 :query {:endpoint "http://127.0.0.1:7073/api/alpha/evidence", ...},
 :results [{:mission "M-zaif-harness-v1", :count 1, ...}],
 :summary {:total-missions 1, :total-attributed-turns 1}}
```

**Population verdict:** populated. The one returned turn is the clock witness
for this U8a dispatch itself (`emacs-36e88ac9a12eb872fef41aef3ca08615`).
Qualification: this is a lab-script namespace loaded by file, not a promoted
runner library. The original four views are test-covered; `bug-queue` is not
bound in the test namespace at lines 28-37.

Search trail before locating it: searched `futon3c/src/futon3c/agents`,
`futon5/src`, then all canonical `futon*` repositories for ledger/query and
mission-status definitions. The hit was
`futon2/holes/labs/M-zaif-harness/z1_views.clj`.

## 2. Decision evidence: arm choice and G terms

**Location and shape.** The pure decision returns `{:arm ... :g-terms ...
:mission ...}` in
`futon3c/src/futon3c/agents/zaif_controller.clj:105-141`. The persisted record
is constructed at lines 185-218 and appends synchronously through the evidence
boundary at lines 228-257. Its queryable identity is tags
`[:zaif :arm-choice]`, event `:zaif-arm-choice`, plus
`:evidence/session-id`; its body contains `:turn-id`, `:arm`, `:g-terms`,
`:gamma-used`, `:mission`, inputs, and paired-constant fields. The runner emits
both constant arms at `zai_api.clj:1126-1175`.

**Executed probe.** A read-only filtered text-index query (the ordinary evidence
endpoint's singular `tag=zaif` currently ignored the filter, so this uses the
server-supported `tags=zaif` search and checks event membership):

```text
$ bb -e '<GET /api/alpha/evidence/text-search?tags=zaif&limit=2>'
{:count 2, :checked 2, :index-as-of "2026-09-02T17:42:58.279522498Z"}
{:evidence/id "e-0f2f9aec-6240-40e9-a25a-e45d9452076f",
 :evidence/session-id "zai-476ceb7567124811958ec8d6dabedd1c",
 :evidence/tags [:zaif :arm-choice],
 :evidence/body {:arm :retrieve,
                 :g-terms {:retrieve 0.7456643332946383,
                           :act 0.0, :ask 0.15, :yield 0.0},
                 :constant-label :sweep, :round 1, ...}}
```

**Population verdict:** populated for real recorded decisions. A wider executed
probe over 100 tagged results found 56 decision records. It also found
`:mission-populated 0`; that absence is treated under source 4.

## 3. R16 tool-call witnesses

**Location and shape.** The durable `:turn-round` record is defined in
`futon3c/src/futon3c/agents/zai_api.clj:922-964`. `transcript-calls` at lines
1014-1031 records tool name, exact EDN argument string, and result digest; the
executed calls are persisted at lines 1283-1309. Who/when are
`:evidence/author`, `:evidence/session-id`, and `:evidence/at`; what was asked is
`:evidence/body :calls [{:tool ... :args ...}]`; `:turn-id` and `:round` join
the witness to its decision. U7 pins these fields at
`test/futon3c/agents/zai_memory_tool_contract_test.clj:78-123`.

**Executed probe.** From `/home/joe/code/futon2`, GET text-search with
`tags=turn-round,zaif&limit=100`, then require non-empty `:calls`:

```text
{:results 100, :with-calls 80}
{:evidence/id "e-4bb840b9-e87e-4348-9f65-99d899d8cd8a",
 :evidence/session-id "zai-eb0d928c70154a8d8f91c98385ec3c82",
 :evidence/at "2026-08-09T11:26:20.944710491Z",
 :evidence/body
 {:event :turn-round,
  :turn-id "zai-turn-c5ed4e18-8619-4e77-8e85-a55e2386b3a9",
  :round 2,
  :calls [{:tool "run_shell",
           :args "{:command \"cd /home/joe/code/apm-lean && sed ...\"}",
           :result {:sha256-16 "8fba2f3899bdebf5", :chars 4599, ...}}]}}
```

**Population verdict:** populated in live ZAIF sessions, including non-empty
calls. This is sufficient to report tool name/query/time/requester without
trusting the agent's prose.

## 4. Mission attribution

**Location and query shape.** Z1 `mission-attributed` is at
`z1_views.clj:365-400`. It GETs bounded Joe-authored coordination entries,
accepts `{:since ISO :limit N}`, requires real `:turn/:user` tags, reads
`:evidence/body :clocked-mission` (fallback `:mission-id` at lines 357-363),
and returns groups `{:mission M :count N :turns [...]}` with query provenance.

**Executed probe.** The source-1 probe proves the clock query itself is live:

```text
{:mission "M-zaif-harness-v1", :count 1,
 :turns [{:id "emacs-36e88ac9a12eb872fef41aef3ca08615", ...}]}
```

A second executed probe queried 100 `tags=zaif` search results and inspected
all `:zaif-arm-choice` bodies:

```text
{:all-results 100, :decisions 56, :mission-populated 0}
```

The sample decision in source 2 likewise omits `:mission`; its recorded
`:inputs-snapshot` contains no mission. Session queries find turn-start and
turn-round records, but those shapes (`zai_api.clj:922-964`) also contain no
clocked mission.

**Population verdict:** the Z1 clock view is populated for newly clocked
operator turns, but is **not populated/joinable for the inspected real ZAIF
decisions**. Typed finding: `:u8/decision-mission-attribution-absent`. The gate
must not infer a mission from prompt text, a current clock belonging to a later
turn, or the mission document.

## 5. Mission-status oracle

**Location and query shape.** The event-derived oracle is Z1
`mission-status` at `z1_views.clj:480-591`. It runs one bounded BM25 query for
the mission, filters full returned evidence entries for commit/chat status
signals, chooses the newest signal at lines 560-564, and returns
`:derived-status`, `:derived-at`, `:derived-from`, and its exact query. It reads
the doc header only for the separate comparison fields `:doc-header` and
`:stale-header?` (lines 565-590); a report status must use `:derived-status`.

`futon2.aif.mission-registry/mission-status` at
`src/futon2/aif/mission_registry.clj:294-304` is **not** this oracle: it derives
`:open?` from parsed mission documents and is disallowed by U8's “never from
the doc header” rule.

**Executed probe.** From `/home/joe/code/futon2`:

```text
$ bb holes/labs/M-zaif-harness/z1_views.clj mission-status \
    M-zaif-harness-v1 --limit 20
{:view :mission-status,
 :query {:search "M-zaif-harness-v1", :candidates-found 31,
         :entries-scanned 31, ...},
 :derived-status :complete,
 :derived-at "2026-09-02T17:42:01.973759290Z",
 :doc-header {:keyword :draft, ...},
 :stale-header? true,
 :summary {:total-signals 5, ...}}
```

**Population verdict:** populated but currently unsound for this mission. The
newest alleged `:complete` signal merely says “parked dependencies complete”
inside a review turn; two other ordinary turns are also classified complete.
The predicate at lines 525-553 accepts any candidate that mentions the mission
and contains `COMPLETE`, even when that word describes a dependency, test, or
sentence fragment rather than the mission status. Typed finding:
`:u8/mission-status-signal-overbroad`. The existing tests demonstrate a clean
historical closed mission but do not pin these negative prose cases
(`z1_views_test.clj:190-227`).

## Gate-build specification implied by discovery

A future U8 gate can use `(session-id, turn-id, round, pairing-key)` to join
decision evidence to R16 rounds, and can reconstruct arm/G claims directly.
Before it can pass on a real decision, the recording boundary must persist a
clock witness that joins to that same decision, and the status oracle must
reject incidental status words. Until both facts exist, the correct reporting
test outcome is failure with the two typed findings above.
