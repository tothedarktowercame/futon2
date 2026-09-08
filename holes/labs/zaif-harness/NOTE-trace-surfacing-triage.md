# NOTE — TRACE surfacing: the 74 explained, and the two repairs owed

2026-09-08, claude-1, after the 15:06 bulletin reported `waits-on-joe 74`
(the 09:45 bulletin said 5).

## What the number is

The census: 5 ruling-shaped board rows (RUN4, F10, F11, F12, PA12z) plus 69
records surfaced by PA9z (futon2 dfe4dcfd, landed this afternoon), which
makes `bulletin.clj` scan the whole trace corpus (`data/wm-trace/*.edn`,
back to May) for any record whose `:wm/route` contains node `:TRACE` and
count each as class :J `:needs-joe`. Distribution: 59 from the single
2026-09-01 session (16:33–19:05Z), 3 from 09-02, 4 from 09-04, 3 from
09-07. Every one of the 69 carries only
`{:via "futon2.aif.trace/write-trace!" :at <ts>}` at the TRACE node — no
reason, no question.

The surfacing is doing its job (records routed for operator review had been
invisible since May; cluster-B behaviour). The delivery is the defect: a
raw, growing, question-free queue presented as operator debt.

## The two repairs (tickets :PA13z/:PA14z, this lane)

1. **A discharge side, and an honest split.** There is no ledger of
   reviewed TRACE records, so waits-on-joe can only grow; and untriaged
   surfaced records are not the same kind of thing as ruling-shaped rows.
   The bulletin should report ruling-shaped rows as a decision sheet and
   surfaced-untriaged as one line (count + oldest date), subtracting a
   trace-review ledger.
2. **The route names its question.** A record requesting operator review
   without stating what the operator is asked cannot be triaged except by
   re-reading the run — machine work mispriced as operator work. The
   surfacing discipline for needs-joe channels (ruling-shaped text carries
   an answerable question) applies at the write site.

## TRACE review ledger append path

`holes/labs/zaif-harness/trace-review-ledger.edn` is the append-only discharge
side. Append with `futon2.aif.bulletin/append-trace-review!`, supplying exactly
one map with `:record/id`, `:disposition`, `:at`, and `:by`. The append function
refuses incomplete entries and any second disposition for an existing record
id; existing forms are never rewritten. Bulletin collection subtracts every
record id present in this ledger from the surfaced-untriaged summary.

## TRACE reason write discipline

`futon2.aif.trace/write-trace!` now guarantees that every newly persisted
record has a `:TRACE` hop with a `:reason`. Production callers name their
routing rule and the operator question. Older or direct callers are recorded
with `:rule :trace-route-reason-missing`, an explicit machine-triage reason
whose question asks which producer rule should replace it; such records do not
silently become operator work.

Caller enumeration used:

`rg -n --glob '*.clj' '(trace/)?write-trace!' src scripts test`

The output was not truncated. Production calls are the war-machine helper in
`scripts/futon2/report/war_machine.clj`, the scheduled runner in
`scripts/wm_scheduled_run.clj`, and the full-loop runner in
`src/futon2/aif/full_loop_runner.clj`; the remaining calls are tests,
documentation strings, or the unrelated local test helper in
`selection_authoring_coupling_test.clj`.
