# U84: count the persisted question, not its input key

2026-09-10, Codex-17. Joe asked to work through the four board-blocked items.
This packet repairs the measurement, not the observation population or the
qualification. No campaign or WM run was started.

## Result

Two executions of `bb -cp . -m checks.u84-trace-reason-census` from futon2
produced byte-identical `census.edn`: 58 files, 892 EDN records, 69 records
with a TRACE hop, **zero with a structured reason/question**. All files parsed;
the report pins each file's SHA-256 and refuses a source changing during its
read. Historical opaque tagged values are retained as data, never evaluated.
The first diagnostic reader lacked tagged-value support and rejected one
May 19 file; its 883-record partial count is superseded by this complete one.

The old `rg ':trace/reason'` command is not a valid future trigger. That key
belongs to the judge input. `reasoned-trace-route` in
`src/futon2/aif/trace.clj:741` writes it to the TRACE hop's `:reason` inside
`:wm/route`; `trace-record` preserves the route at `:595`. The scheduled caller
supplies its question at `scripts/futon2/report/war_machine.clj:6849`.

The new census counts records rather than hops, separates producer reasons
from explicit missing-producer fallbacks, and reports malformed reasons.
It reads every EDN form, and incomplete parsing cannot yield a met threshold.
Twenty reason-bearing records would trigger review, not prove live origin,
answerability, surfacing, or successful operator review. These need their own
evidence before the ALIGN qualification changes. No synthetic record is added
to the retained corpus.

## What unblocks what

U84's re-assessment still requires 20 genuine live observations. It does not
block implementation or launch of the producer that supplies those observations.
Existing `test/futon2/aif/trace_test.clj:153` exercises the real writer in a
temporary directory and verifies both the explicit question and fallback.
Tests are capability evidence, not the missing live population. The next run's
writer/load provenance and retained output must be checked before claiming that
source code reached the live path.

F12 and U80 are now commissioned work, not requests for a new Joe decision:
Codex-16 has F12's missing reviewed experiment-manifest preparation
(`invoke-1789054927520-18880-64a29bfa`); Codex-12 has U80's historical/current
category correction (`invoke-1789054952364-18883-12a94258`). These are dispatch
records, not completed repairs. Zai-2 has the R16 one-label revision plan and
its dependent-generation/control census (`invoke-1789055004307-18887-d87358e9`).
The companion-paper no-edit-before-plan boundary is preserved while that exact
repair is prepared. No source lifecycle, ledger, or qualification was altered.

## Validation

`clojure -X:test :nses '[checks.u84-trace-reason-census-test futon2.aif.trace-test]'`
passed: 49 tests, 156 assertions, zero failures/errors. Controls distinguish the
unpersisted input key, duplicate hops, malformed questions, fallback reasons,
multiple EDN forms, opaque tags, and incomplete/non-map records.

clj-kondo: zero errors/warnings. check-parens: OK. `git diff --check`: clean.
