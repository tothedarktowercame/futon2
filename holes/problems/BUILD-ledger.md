# BUILD ledger — R-node build (opened 2026-08-30)

| lane | packet | seat | job-id | park-id | dispatched (UTC) | state | tech-lead review (sha; checked) | owner gate |
|---|---|---|---|---|---|---|---|---|
| — | charter bell | claude-20 | invoke-1788101141452-4145-fefee03b | park-dda4f308-a900-41b9-ad56-eef03cf06d64 | 14:45Z | dispatched | — | claude-15: seat registered (opus, role tech-lead), charter sent |
| CML | CML-D1 (build: linter + fixture) | codex-1 | `invoke-1788101371647-4167-f8f06c65` | **none — park refused** | 14:49Z | running | — | — |
| 1 (R9) | R9-D1 (discovery, no code) | codex-2 | `invoke-1788101384872-4171-b799a2d9` | **none — park refused** | 14:49Z | running | — | — |
| 2 (R2) | R2-D1 (discovery, no code) | codex-8 | `invoke-1788101388250-4173-da930b0b` | **none — park refused** | 14:49Z | running | — | — |
| 3 (R8) | R8-D1 (discovery, no code) | codex-12 | `invoke-1788101391434-4174-520b9a01` | **none — park refused** | 14:49Z | running | — | — |

## Notes (claude-20, tech lead)

- **Parks are unavailable to this seat.** `agency_send.py --park` refused every dispatch with
  `--park sender has no session-id: claude-20`; the roster's entry for `claude-20`
  (`GET :7070/api/alpha/agents`, registered-at 2026-08-30T14:44:57Z) carries `session-id: null`.
  The bells themselves were accepted and all four reached `state: running` (checked 8 s after send).
  Until the seat has a session-id I **poll** `GET /api/alpha/invoke/jobs/<id>` instead of parking,
  which loses the deadline backstop the charter asks for. Owner action needed.
- **Pre-dispatch acceptance run (lifecycle log row 11/13) — CML-D1.** Parsed
  `p4ng/empirics-futon/control-map-edges.edn` with bb before sending. Baseline confirmed: 22 `:edges`
  (`:status` {`:drawn` 21, `:unresolved` 1}; `:kind` {`:control` 10, `:support` 12}), 26
  `:derived-undrawn` **all carrying `:by`**, **0 edges carrying `:schema`**, **0 carrying `:fixture`**.
  The record's predicted baseline ("21 drawn with `:schema :unspecified`") is correct and was written
  into the packet as the expected value.
- **Two commissioner-side defects found before the builder saw them** (the class of log rows 9-11),
  both flagged in the packet as "do not silently fix", both needing an owner amendment to S1:
  1. `P-control-map-lint.md`'s inventory line double-counts: the `:chartered` edge (R20→R14) is the
     26th entry *inside* `:derived-undrawn` (`:status` freqs there are {nil 25, `:chartered` 1}),
     not a fourth category. The file holds 22 + 26 = 48 edge entries, not 49.
  2. `:endpoints-agree?` is **not computable for any drawn edge today**. Only three `P-R<n>.md`
     records exist (R2, R8, R9); of the 21 drawn edges, **0 have a record at both endpoints**, 6 have
     one, 15 have neither. A boolean would report `false` for all 21 — "the endpoints disagree" when
     the truth is "there is no record to compare". The packet requires a typed absence
     (`:no-endpoint-record` / `:one-endpoint-record`) on real data, with `false` exercised only on
     synthetic fixtures, where the record's stated falsifier is satisfiable.
- **Corpus check for R2-D1 / R8-D1** (row 6 — name the instrument): `futon2/data/wm-trace/` exists and
  holds **54 entries** by `ls | wc -l`, while both records speak of **88 trace records**. I did not
  reconcile this; both packets require the builder to report files *and* records and to treat an
  irreconcilable count as a finding rather than adopt 88 from the record.
