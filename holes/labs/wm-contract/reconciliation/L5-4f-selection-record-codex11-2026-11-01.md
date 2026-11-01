# L5-4f — durable selection decisions, including refusals

Base: futon2 `9b89c7000`. Branch: `codex11/l5-4f-selection-record`.
No merge, serving reload, click, or canonical data write. Evidence/logs: `/home/joe/notes/l5-4f/`.

## Decision lifecycle and approved scope

Coordinator ruling `invoke-1793542606410-9343-05cce514` replaces mutable records with an immutable decision and a separate completion receipt. Ruling `invoke-1793543345526-9358-5cb7b7c3` extends this to **one chain per click**, because a reading refusal can cause reselection after inner entry. Both rulings are saved under the evidence directory.

- `selection_record.clj:61` writes `selection-record-<date>-<click-id>.edn` (run id fallback for callers without a click id). A reselection appends `-decision-<n>.edn`, with `:supersedes` SHA, predecessor reference, chain index, and reader-refusal cause/target/pin. Existing decision bytes are never replaced.
- The ordinary runtime retains the whole produced section before entering the inner runner (`full_loop_runtime.clj:1315`). Pre-score/no-Giraffe failures retain whatever evidence exists, with typed absence for the unavailable section. Refusals do not manufacture an inner tick.
- `reading-reselection-port` (`full_loop_runtime.clj:1257`) retains the next decision before publishing the replacement options. The tick references the terminal decision (`full_loop_runner.clj:1590`, using `selected-target-reading/current-options`).
- Completion, stored beside the chain root, references root, terminal, tick path/SHA or typed absence, and end status; it does not restate the decision. A crash before completion leaves a decision with no completion, not a successful work claim.
- `previous_run.clj:197` prefers the indexed previous click's terminal and includes chain length. Ordering uses start timestamps and chain indices, not filename order. A fork, missing index, unindexed record, changed bytes, invalid completion, or ambiguous predecessor refuses instead of skipping backward. Legacy sealed-tick fallback remains only before selection-record history exists.
- The exporter (`wm_run_facts.clj:1026`) verifies the reference and entire chain. A disagreeing embedded section emits `selection-reselected-after-decision` as not-recomputable facts rather than certifying the stale choice. Historical records without references retain their prior projection.
- The alert evaluates a standalone refused/abstained selection, reports work `not-run`, and exits 1 with conforms false. The launcher finds the selection root when no tick exists, applies its existing click/run ownership check, then invokes the alert. A successful tick still exports its referenced selection. Opening a completed root can resolve the terminal and the pinned work record.

### Bypasses, explicitly not selections

Direct `:task` entry (`full_loop_runtime.clj:1324`) writes `:not-exercised/:direct-task` before inner entry. Authority refusal and dedicated self-heal routes (`full_loop_runtime.clj:1649`, arbitration at `:run-opportunity-with-authority!`) write `:not-exercised/:meta-self-heal` at the public return boundary. Retry children get their own record context; a parent atom is not reused. Whether these bypasses should exist under the two-runs principle is left to Joe, per the coordinator ruling. `resume-on-prod-cascade!` remains a non-click checkpoint resume, outside this writer.

## Verification

Machine-readable details, names, environment overrides, and log SHAs: `L5-4f-test-comparison-2026-11-01.json`.

| Check | Base | Branch |
|---|---:|---:|
| Outer namespaces + both projectors | 159 tests / 650 assertions / 22 failures / 4 errors | 165 / 692 / identical 22 failures / 4 errors |
| Focused chain/history/projectors | — | 82 / 310 / 0 failures / 0 errors |
| Final record tests (including later index and filename guards) | — | 8 / 46 / 0 / 0 |
| Final record + projector tests | — | 81 / 290 / 0 / 0 |
| Alert Python | 26 tests, OK | 27 tests, OK |
| Click Python | 46 tests, one fixture timeout | 47 tests, same fixture timeout |
| Focused launcher selection-only/timeout tests | — | 5 tests, OK |

The initial base run had 3 errors. Canonical family-selection source advanced during this packet; the source-composition check then failed on **both** the unchanged base and branch. The repeated base run above captures that fourth error. Failure and error name distributions match. The existing click fixture timeout is `test_direct_script_preparation_refusal_never_posts_and_dry_run_never_loads`, mode current/run true, its 15-second limit. It reproduces on base. No full-green claim.

Existing assertions adjusted only for the added record reference/context (direct-task/no-Giraffe paths), and the alert's selection-first evaluation order. New tests cover refusal persistence/no tick, write-before-inner-entry, referenced projection, tamper refusal, completion conservation, two-decision reselection, stale embedded-section detection, predecessor chain length, missing index, explicit click-id filenames, and selection-only alert/launcher routing. The portable comparison subset in `test/fixtures/wm/l5-4f-selection-decision.edn` retains the actual 11-01 class/pick/digest and pins its source bytes.

All Clojure runs used scratch `FUTON_WM_*` roots; the suite's hermetic fixtures also supplied temporary roots. Canonical `/home/joe/code/futon2/data` count: **45,737 before and after**, including Python checks. Replay and its records live under notes and a retained `/tmp/l54f-replay…` directory. No live API evaluation or POST was used.

Parens: OK. Kondo: zero errors, same four pre-existing `full_loop_runner.clj` warnings as base (two misplaced docstrings, redundant let, unused checkpoint binding); all new files clean. Shell syntax, Python compilation, and `git diff --check` pass.

## Lean and real alert counterexample

`replay.clj` uses the retained 11-01 comparison from L5-4e and injects a **post-choice refusal** as a negative fixture. It writes two real decision records through the new store, reads the first through the predecessor reader, and exports the second. This is not evidence of two live refused clicks, nor does a tie refusal with no retained choice invent one.

| Requirement | Outcome |
|---|---|
| Q1 | holds |
| Q2 | holds |
| Q3 | holds |
| Q4 | holds |
| Q5 | unverifiable |
| Q6 | **violated** |
| Q7 | unverifiable |
| Q9 | violated |
| Q11 | holds |

Q6 compares the same class/pick and digest `21f204c69df883b67ea69b775dd9a267c2b8e6df22dd4bd6b7bed635b857807a` against a known refused predecessor. Q5 has no interpretation order; Q7 lacks a learning receipt in the replay; Q9 uses an explicitly empty offline seat snapshot. These are not new live findings.

Actual alert invocation over that exported selection: **exit 1**, `work run: not-run`, selection Q6 violated, conforms false. It used the paired fixture CLI arguments, but never evaluated the work fixture. Lean log SHA256: `dd472054d74924544a20cad4ca7d7798322de0083325022c77b358cb59958397`. Logs: `lean.log`, `alert-counterexample.log`, `counterexample-receipt.edn`; JSON receipt records the concrete decision paths and SHAs.

## Limits

This adds retention and detection, not new Q6 selection enforcement. Refusals that occur before a choice or input digest exists remain non-recomputable for those carriers. A chosen decision without a completion/tick does not establish work ran. Decision/index/completion writes are separate durable files; incomplete publication fails closed on the next reader, rather than promising transactional crash recovery. No fsync/power-loss durability claim. Dedicated self-heal's no-selection path remains explicit rather than being redefined in this packet.
