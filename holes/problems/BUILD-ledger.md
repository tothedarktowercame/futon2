# BUILD ledger — R-node build (opened 2026-08-30)

| lane | packet | seat | job-id | park-id | dispatched (UTC) | state | tech-lead review (sha; checked) | owner gate |
|---|---|---|---|---|---|---|---|---|
| — | charter bell | claude-20 | invoke-1788101141452-4145-fefee03b | park-dda4f308-a900-41b9-ad56-eef03cf06d64 | 14:45Z | dispatched | — | claude-15: seat registered (opus, role tech-lead), charter sent |
| CML | CML-D1 (build: linter + fixture) | codex-1 | `invoke-1788101371647-4167-f8f06c65` | **none — park refused** | 14:49Z | closed | `7272099` + my fix `dfe8c80`; read both files; re-ran kondo/parens/tests/2x-determinism (report sha256 `aedb4c1b…` reproduced); verified the hardcoded 21-edge baseline == the file's drawn set; fired the falsifier (deleted R1→R4 → `:drawn-edge-missing`, exit 1); **found + fixed**: substring matching manufactured `:endpoints-agree? true` | pending |
| 1 (R9) | R9-D1 (discovery, no code) | codex-2 | `invoke-1788101384872-4171-b799a2d9` | `park-33959bcf` (released — job already done) | 14:49Z | closed | `81d074b`; 194 lines; **28/28 citations resolve**, 3 spot-checked by opening the line; 2 refusals recorded | pending |
| 2 (R2) | R2-D1 (discovery, no code) | codex-8 | `invoke-1788101388250-4173-da930b0b` | `park-14ac4a4e` | 14:49Z | closed | `41cc852`; 132 lines; **29/29 citations resolve** (2 apparent failures were my resolver, not the note); reader loop confirmed, not `edn/read-string`; 53 files / 792 forms reproduced here | pending |
| 3 (R8) | R8-D1 (discovery, no code) | codex-12 | `invoke-1788101391434-4174-520b9a01` | `park-16cb6c31` | 14:49Z | closed | `08925e6`; 114 lines; **21/21 citations resolve**; independently reproduced 53 files, 792 forms, 88 `:realized-outcome`, 32 `:selection-gain` (all `{1.0 … :samples 0}`), 787/792 `:precision-state`; 3 refusals, one of which refutes P-R8's falsifier | pending |

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
| — | owner gate: dispatch step | claude-15 | — | — | 14:53Z | passed | — | claude-15 14:53Z: model stated (claude-opus-5); 4 jobs verified running/work on codex-1/2/8/12 (API); seats conform; packet text NOT verifiable via API (prompts not exposed) → packet files required from now (charter 6b); records amended on claude-20's two findings (CML counts, typed endpoints-agree) and P-R8 corpus; parks: session-id now live on roster, park the four jobs retroactively |

## Lane closes — 2026-08-30 (claude-20 first-line review)

All four opening packets returned inside 8 minutes. Review depth is stated per lane in the table;
what follows is what the owner's gate needs and what I could not settle myself.

- **CML-D1 — PASS with one defect found and fixed by me** (`dfe8c80`, futon2). `record-agrees?`
  compared schema field names with `str/includes?`, so two records that never mention a field agreed
  about it: my probe gave `:endpoints-agree? true` and `:fully-specified? true` for records saying
  "the **id**entity of the sender" and "e**val**uation". Fixed to whole-token matching (hyphen
  excluded both sides, so `id` does not match the field `id-key`); regression test added; the live
  report is byte-identical (`aedb4c1b…`) because no edge reaches the two-record branch today —
  it would first have bitten when CML-D2 creates the records the check exists to compare
  (delivery-lifecycle log row 12a: fix the instrument before it runs).
- **Vocabulary addition needing owner ratification:** the builder emits a fifth value,
  `:schema-unspecified` (both endpoint records exist but there is no schema to compare), beyond the
  amended `:no-endpoint-record | :one-endpoint-record | true | false`. I think it is right; it is the
  owner's to ratify.
- **Standing note on the baseline test:** `current-control-map-baseline` asserts the live counts
  (21 drawn / 0 specified / `{:no-endpoint-record 15, :one-endpoint-record 6}`). It will fail *by
  design* the moment CML-D2 lands a schema. That assertion is to be updated deliberately, never
  loosened to keep the suite green.
- **R8-D1 refutes P-R8's registered falsifier — this blocks R8-D2 as written.** The record's
  falsifier is "every one of the 88 records (F absent) must fire today; a checker that passes them is
  reading the wrong key." Reproduced here independently: the corpus is 792 forms, **32 already carry a
  stored F**, and **5 of 792 lack `:precision-state`** so F is not recomputable for them at all. A
  checker that fires on everything would therefore be wrong in two directions. The builder's proposed
  three dispositions — missing-F-but-computable (755), valid stored F (32), insufficient inputs (5) —
  is the shape the acceptance needs. **Owner amendment required before R8-D2 is dispatched** (row 11).
- **Registered expectation, honestly graded (R8-D1):** `:selection-gain` is 1.0 with `:samples 0` in
  every state that carries it — but only 32 of 792 forms carry one at all, so "1.0 throughout" is true
  of recorded gain states and not of the corpus. Confirmed here. The builder reported the narrower
  true claim rather than the registered one, which is the behaviour row 8 asks for.
- **My own dispatch-time number was wrong and the builders corrected it.** I put "54 entries" in two
  packets from `ls | wc -l`; the trace corpus is **53** files (`wm-shadow-step.json` is not a trace
  file) plus two dotfiles `ls` does not show. Both R2-D1 and R8-D1 independently reported 53/792 and
  refused the 88. My instrument was crude and the packets were right to demand both numbers.
| CML | owner gate: CML-D1 | codex-1 | — | — | 15:02Z | passed | 7272099+dfe8c80 | claude-15 15:02Z: PASSED: tests 4/20/0 re-run here; live lint 15/6 typed; whole-token fix correct; :schema-unspecified ratified into the record |
| 1 (R9) | owner gate: R9-D1 | codex-2 | — | — | 15:02Z | passed | 81d074b | claude-15 15:02Z: PASSED: GainChain.lean:71 `inductive Producer where` and r18-badges.edn:5 verified at source; both refusals correct (no 13 identifiable rows; :independent?/reviewer-family/:reviewed-by are not evidence); provenance narrowed to 56,379/90,583 session-ids — keep |
| 2 (R2) | owner gate: R2-D1 | codex-8 | — | — | 15:02Z | passed | 41cc852 | claude-15 15:02Z: PASSED: operator_lane.clj:29-33 verified — `nag?` is the 4-term AND with :acknowledged?; refusal on the turn-channel normalisation is the deliverable P-R2 asked for |
| 3 (R8) | owner gate: R8-D1 | codex-12 | — | — | 15:02Z | passed | 08925e6 | claude-15 15:02Z: PASSED, and it corrected S1: claude-15 reproduced 88 realized / 32 selection-gain all pinned / 787 precision-state / 32 stored F / :free-energy holds G on 792-793 forms; falsifier rewritten as three dispositions with expected 755/32/5; R8-D2 unblocked |

### R9-D1 — review deepened after the auto-bellback (claude-20, same day)

My first-line review of R9-D1 rested on the citation audit (28/28) plus three spot-checks, and I said
so. The auto-bellback restated two numeric claims I had not re-derived, so I checked them:

- **`r18-badges.edn` says four `:derived-from-FEP`, complete parsing finds five — CONFIRMED.** The
  file's own headline comment (`data/r18-badges.edn:13`, "Current headline: 4 :derived-from-FEP, 1
  :principled-approximation") contradicts its data: a tree-walk over every nested map finds **5**
  entries with `:badge :derived-from-FEP`. The drift is dated in the file itself — two badges were
  raised to `:derived-from-FEP` on 2026-07-04 (`:141`, `:143`) and the headline was never updated.
  Note `grep -c derived-from-FEP` returns 14, because the legend and the prose narrate it; the count
  is only visible to a parse. This is the "done is dated against the instrument that produced it"
  class (lifecycle log row 12b) inside the artefact that was the apex's ancestor — a fitting finding
  for the node whose thesis is that a claimant cannot certify itself.
- **Not checked:** the evidence-export figures (author+type on 90,583 records, session-id on 56,379).
  Reading that export is expensive and I have not run it; the claim stands as the builder's, marked
  here as unverified by me rather than silently inherited.

No change made to `r18-badges.edn`: it is evidence in an open lane, and the stale headline is a
finding for the owner, not a line for me to quietly correct.
