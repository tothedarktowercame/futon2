# C505 — RE6 review fix: the pointer, and why `no run folds :incomplete` cannot be
# reached from either of the two named runs

Account for the second RE6 pass, answering the review finding on worklist row
`:RE6`. Two findings were returned. One is repaired here. The other is reported
as a blocker with its measurements, because closing it needs either a ruling
(Joe's, not a lane's) or a new run (a different row).

## 1. The invalid pointer — repaired

`run-era-ledger.edn:212-379` does not resolve: the file is 297 lines, so the
cited end is beyond it and `pointer_check.bb` reported it twice (once in RE6's
`:evidence`, once inside the reviewer's own quotation of it).

The rows the pointer meant to name — seq 7 through seq 14, the eight this row
deposited — occupy **run-era-ledger.edn:152-263**: line 152 opens the seq-7 row
(`#:row{:run-id "2026-09-01-s5"` … `:check-id :per-node-runtime-validation`,
`:seq 7` at :163) and line 263 closes the seq-14 row
(`:check-id :enumeration-completeness`, `:seq 14` at :261). The `:evidence`
string now carries that range. The reviewer's quotation is left in place with
the colon removed (`run-era-ledger.edn lines 212-379`) so the record of what was
reported survives without the checker re-reading a dead pointer as live.

## 2. `no run folds :incomplete` — blocked, and the measurements say why

The clause is not met and cannot be met by any deposit. What blocks it is not a
gap in the wiring; it is that **every remaining typed absence needs evidence
taken while the run was running, and both named runs are closed.**

`fold-by-run` (run_era_ledger.bb:341-377) turns any `:typed-absence` verdict into
`:incomplete` at run_era_ledger.bb:370. So the clause is equivalent to: every one
of the seven catalogued checks deposits a non-absent verdict for both runs. Eight
typed-absence rows stand in the way — six on 2026-09-01-s5, four on
2026-09-04-re5 (two checks are absent on both). Taken one at a time:

**`:flip-readiness` (seq 1 s5, seq 4 re5).** `flip_readiness_check.bb:69-77` reads
six tree-level inputs — the runtime-validation catalog, the variable-situation
accounting, the U27 hole audit, the p4ng defect-repair tally, the wm status
receipt, and the mathlib4 holes contract — and no run store. Its verdict is a
property of the tree at the moment it runs. Neither run store holds a
flip-readiness artifact (`runs/2026-09-01-s5/` and `runs/2026-09-04-re5/`, listed
in the seq-1 and seq-4 `:notes`), so no verdict contemporaneous with either run
exists. Running it today measures today's tree. **Not repairable by deposit.**

**`:contract-pin` (seq 2, s5 only).** re5 is green because its store records the
contract authority; s5's does not — the seq-2 `:notes` records a scan of all
seven files in `runs/2026-09-01-s5/` for the recorded authority
`11c2e44affa167bf85b0a2d7c29d8705f89a8d08`, found in **none**. The difference
between the two runs is exactly contemporaneity. **Not repairable by deposit.**

**`:per-node-runtime-validation` (seq 7 s5, seq 11 re5).** Two reasons, either
sufficient. (a) Neither run store holds a runtime-validation artifact. (b) The
catalogue it validates carries no run identity at all: the 84 rows of
`runs/RUNTIME-VALIDATION-CATALOG.edn` (1022 lines) are keyed
`:node`/`:axis`/`:ns`/`:pointer`/`:status`/`:validation`/… with no run key, and
`:test-runs` is a map keyed by test namespace whose values carry `:at` — a date.
The four strings in the file matching `run-id` or a run name are prose inside
`:note`/`:validation` (RUNTIME-VALIDATION-CATALOG.edn:207, :716, :756, :762), not
fields anything could key on. **Not repairable by deposit, and not attributable
to a run without new machinery.**

**`:tensions-cashed` (seq 9 s5, seq 13 re5).** `tension-ledger.edn` (403 lines)
contains neither run-id string and none of the eight tick ids: grep over all ten
identifiers returns **0 — not found**. Of the ten `:event/` and twelve
`:tension/` keys in use, none names a run. The green branch is reachable — u39's
mint payload carries `:tension/provenance {:records [run-id …]}` — but no
committed tension was minted that way. Greenness needs a tension minted **during**
a run. **Not repairable by deposit.**

**`:rationale-regret` (seq 8 s5, seq 12 re5).** Two different absences, neither
closable retroactively. On s5 all three tick pairs are refused at mint, because
the run's chosen action sat at controller rank 123 (115 on one tick) with top-5
margins at -0.147 — a property of the records the run wrote. On re5 the rule's
refutation leg cannot fire and its UPHELD leg needs an outcome leg: the receipt
records `:records-of-this-run 4`, `:pairs-evaluated 3`, and
`:upheld-unreachable` — "no record in this corpus carries one"
(runs/RE6-check-deposits/rationale-regret-2026-09-04-re5.edn). Neither trace file
contains the string `realized` (0 hits in both). **Not repairable by deposit.**

**`:enumeration-completeness` (seq 10, s5 only).** re5 is green. s5's replay
reports 4/4 records incomplete on one member, `M-zaif-harness-v1`, whose file was
first committed 2026-09-02T12:40:23Z against a run whose last tick is
2026-09-01T22:54:48.942309598Z. The available population is scanned at deposit
time and the check has no as-of-run scan, so the population s5 saw cannot be
reconstructed. **Not repairable by deposit.**

### What that adds up to

Eight for eight: no deposit can move any of them, because a deposit can only
report what a store holds and neither store holds it. The two available exits
are:

- **A ruling** on whether `:typed-absences` should fold to `:incomplete` at all —
  i.e. whether the honest store rule reporting a thin run is the same status as a
  ledger nobody has wired. `fold-by-run`'s own docstring already argues they
  "call for opposite responses"; the fold nevertheless prints one word for both.
  This is a preference about what the status vocabulary should say, so it is
  Joe's, and no `:choices` or `:decisions` entry was written here.
- **A new run** that takes five of the checks *during* the run and writes their
  artifacts into its store, plus two machinery changes the checks need before
  they can speak about a run at all (run identity in the runtime-validation
  catalogue; a tension minted with `:tension/provenance` naming the run) and
  outcome records so the rationale rule's UPHELD leg is reachable. That is a
  run-lock row (RUN12), not this one.

### One observation, offered as material for the ruling and not as a decision

The row's `:statement` quotes Joe as asking for the bootstrap past `:incomplete`
because it "reads as nonfunctional behaviour". The wiring delivered exactly that:
`:checks-not-deposited` is `[]` on both runs, which is the state Joe's sentence
describes. The acceptance clause as written — `no run folds :incomplete` — is
strictly stronger than that sentence, because it also forbids the second cause,
which no amount of wiring can reach. Whether the clause or the sentence is the
bar is itself the ruling; recording the gap is not making it.

## 3. Gates, bare exits

- `bb p4ng/empirics-futon/pointer_check.bb` — exit 0, **1260 pointers / 0
  unresolved** (was 1260 / 2 before this pass; the count is unchanged because
  one dead pointer became a range that resolves and the other stopped parsing as
  a pointer at all).
- `bash p4ng/empirics-futon/negative_controls.sh` — PASS, 35 negative /
  18 positive, shared registries untouched.
- `bb worklist_check.bb` — exit 0, 148 items OK, `{:done 145, :blocked 3}`
  (RE6 joins the blocked set; no row is left `:open`).
- `bb run_era_ledger.bb --check` exit 0, `--self-test` exit 0.

No script and no `src/` file was touched on this pass, so clj-kondo and
check-parens have no new surface.

## 4. One thing running the gate repaired, stated because it was not asked for

`runs/RE2-run-era-ledger/run-era-self-test.edn` was committed stale: it recorded
`:rows 3` / `:committed-rows 3` and synthetic `:row/seq 4`, i.e. the ledger as it
stood **before** RE6 appended seq 7-14. Running `--self-test` as a gate rewrote
it to `:rows 14` and `:row/seq 15`, with the two synthetic row shas moving
accordingly. That is the artifact catching up to the ledger it tests, not a
behaviour change — the eleven changed lines are all counts and the two shas they
determine. It is committed here rather than reverted, because leaving it stale
would mean the next reader of the self-test artifact sees a three-row ledger.

## 5. Not done, stated

No `:choices` and no `:decisions` entry. No deposit, no new ledger row — the
ledger file itself is byte-identical. No machine run and no run lock taken.
`gen_aif_dag.bb` not run and nothing regenerated into the publish (TN §9a).
`workflow-report.edn` was already modified in the working tree when this pass
started (a build-loop `:as-of` timestamp bump) and is left alone rather than
folded into this commit.
