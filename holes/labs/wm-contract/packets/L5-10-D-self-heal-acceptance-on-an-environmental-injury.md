# L5-10-D — Discovery: the self-heal acceptance cannot pass on injury 93d40d07 (claude-2 → codex-5, 2026-11-01)

Read-only; report only; worktree only. Base futon2 `main` (≥ fcf96f5d7).

## What happened (clicks 6–8, 2026-11-01)

- Click 6 (`wm-click-ca9ec295`, run `720f17f4`) selected E-comprehension-foundation; the work run
  failed at `:ticket-readiness-refresh` with `:untyped-failure` "authoritative substrate read
  failed": futon1b answered HTTP 504 `:query-deadline-exceeded` to
  `GET /api/alpha/hyperedges?type=clock/clocked-on&limit=100`. The injury store opened
  `injury/93d40d07…` and (L5-5c-1) published finding `repair-occ-1c42c9ec…` and its ticket.
- Click 7 (`wm-click-36c314da`) was taken by the dedicated self-heal route. The medic diagnosis
  (`data/wm-meta-injuries-live-2026-10-22/diagnoses/diagnosis-e068c0c7….edn`) classifies it
  `:environmental-substrate-read-timeout`, cause-entry = the terminal-receipt seam over the pinned
  run record (input path `[]`, record pin sha `1c255fc3…`), healthy predicate
  `{:op :result-path-absent :path [:failure-kind]}`. The acceptance run
  (`acceptance-runs/injury-93d40d07…-terminal-receipt-acceptance-2026-11-01T18-22-18….log`)
  errored: `cause_scope/assert-repair` threw `:cause-scope/input-mismatch` "Retained carrier
  changed", although the record's bytes still hash to the pinned sha. Result:
  `meta-self-heal/acceptance-run-refused`; the injury stays open; the ordinary route is not called.
- Click 8 is on the same route; the loop pauses on the repeated signature.

## Questions

1. **The digest mismatch.** `assert-repair` digests `(pr-str (get-in record input-path))` and
   compares to the scope's `:input-digest`. Where is `:input-digest` minted (coordinator / production
   module), in which JVM, under which print bindings, and why does it differ from the acceptance
   JVM's `pr-str` of the same bytes? Candidates: `*print-namespace-maps*`, tagged literals read
   with `:default tagged-literal`, map ordering, `*print-length*`. Reproduce the two digests from
   the pinned record and name the difference. The record pin already carries a byte sha; say
   whether digesting the retained bytes for input path `[]` (and the pr-str only for sub-paths) is
   the smallest honest fix, or what is.
2. **Environmental causes.** With input path `[]` and the predicate "no `:failure-kind` on the
   terminal receipt of the pinned record", acceptance can only pass if code changes how that
   failed record is read. For an environmental timeout nothing in code can do that: the record
   is the failure. Is there any route today by which an environmental classification is accepted
   or discharged (grep `environmental` finds nothing in the self-heal namespaces)? If not, state
   the smallest honest design: e.g. for `:environmental-*` classifications the acceptance is a
   fresh probe of the named external read (the cause-entry's own substrate call) with its own
   receipt, not a replay of the pinned record; and what Joe would have to authorize (the medic
   authority policy says no new approvals).
3. **futon1b.** The same query now answers in 4 ms, 377 ms or 503 depending on the moment; the
   futon1b JVM is at ~87% CPU running an ingestion census (its log: "census error … HTTP connect
   timed out"). Read-only: which census, started when, by what, and is it the load source.
   No restart, no mutation; this is for Joe.

## Report

`reconciliation/L5-10-D-self-heal-acceptance-codex5-2026-11-01.md` on branch `codex5/l5-10-d`
(worktree). Bell claude-2 with the sha and the three answers in brief.
