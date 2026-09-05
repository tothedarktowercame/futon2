# C534 — RE6: splitting the fold vocabulary, and the fourteen relabelled absences

Worklist row `:RE6`, second implementation pass, under the ruling at
`EPIC-run-era.md:864-873` (Joe, 2026-09-05: "typed absences stop folding to one
word … names subject to Joe's veto at review").

**What this is.** The fold in `run_era_ledger.bb` no longer prints `:incomplete`
for a run whose catalogued checks all deposited. It prints the response the run
is asking for. The fourteen standing typed absences carry a label saying which
response that is, each with the measurement it was read off.

**What this is not.** No check code was changed and no verdict moved: every
`:row/verdict`, every `:row/sha` and `:ledger/head-sha` are byte-identical to
`HEAD` (shown below). No ledger row was deposited, no machine run was taken and
no run lock was held. No `aif-equations.edn :choices` and no
`control-map-edges.edn :decisions` entry. `gen_aif_dag.bb` was not run and
nothing was regenerated into a publish (TN §9a).

---

## 1. The status vocabulary, before and after

Before, three unrelated states shared one word. After
(`run_era_ledger.bb:506-565`):

| status | what it means | the work it asks for |
|---|---|---|
| `:incomplete` | a catalogued check never deposited, **or** an absence carries no label | wire it, run it, or label it |
| `:incomplete-uninstrumented` | every check deposited; at least one absence is `:not-contemporaneous` | instrument the next run |
| `:incomplete-data-pending` | every check deposited; every absence is `:data-pending` | wait for the datum, or derive it |
| `:red` / `:green` | unchanged | — |

The precedence is deliberate and pinned by control C26: one
`:not-contemporaneous` absence beside a `:data-pending` one reads as
uninstrumented, because that is the response the run needs.
`:incomplete-data-pending` is reserved for the run whose absences are *only*
waiting on data — the case the ruling says "does not read as nonfunctional".

An unlabelled absence keeps the pre-split `:incomplete` and is named under
`:unclassified` in `:status-reason`. Control C27 is that the split cannot
launder an unexamined absence into the milder word.

## 2. The discriminator, stated as one question

Declared in the ledger itself, `run-era-ledger.edn:74-89`:

> can this absence still be closed **without changing what a run captures**?
> `:data-pending` when yes — the evidence is committed and derivable, or the
> route to it needs no instrumentation and the datum simply has not been
> produced yet. `:not-contemporaneous` when no — the evidence had to be taken
> while the run ran, or at the tree state it ran against, and the run is closed.

`C511-repair-or-elaborate.md` is the per-`(run, check)` measurement the answers
are read off: its *repair demonstrated* is `:data-pending`, its *repair refuted*
is `:not-contemporaneous` — **except** where the refutation is that the datum
has not been produced yet rather than that nothing could produce it. That
exception is reached exactly once and is stated in the entry that uses it
(§3, `2026-09-04-010-accepted` / `:tensions-cashed`).

## 3. The fourteen labels

Every `:absence/basis` is in the ledger at `run-era-ledger.edn:468-570`; the
summary is what each rests on.

| run | check | kind | read off |
|---|---|---|---|
| `2026-09-01-s5` | `:flip-readiness` | `:not-contemporaneous` | `C511:113-131` — the check and 4 of its 5 futon2 sources were first committed 37–43 h after the run |
| `2026-09-01-s5` | `:contract-pin` | `:not-contemporaneous` | seq-2 note: the authority sha is in none of the store's 7 files; `C511:22-45` reads s5→re5 as a capture, not a repair |
| `2026-09-01-s5` | `:per-node-runtime-validation` | `:not-contemporaneous` | `C511:217-231` `:elaborate-only` — the missing part is a per-repo identity at tick time that nothing ever wrote |
| `2026-09-01-s5` | `:rationale-regret` | `:not-contemporaneous` | `C511:326-344`; and seq-8's own measurement — all 3 pairs refused at mint on this run's records |
| `2026-09-01-s5` | `:tensions-cashed` | **`:data-pending`** | `C511:358-399` — U52 minted a tension naming this run's tick after the deposit; the check's scan finds it |
| `2026-09-01-s5` | `:enumeration-completeness` | `:not-contemporaneous` | seq-10: the one missing member postdates the run's last tick; no as-of-run scan exists |
| `2026-09-04-re5` | `:flip-readiness` | `:not-contemporaneous` | measured here: this store holds no `step.edn` and no `world-before.edn`, which is what `C511:62-111`'s re-derivation needs |
| `2026-09-04-re5` | `:per-node-runtime-validation` | `:not-contemporaneous` | as s5 |
| `2026-09-04-re5` | `:rationale-regret` | `:not-contemporaneous` | `C511:326-344` — both capture points are instrumentation |
| `2026-09-04-re5` | `:tensions-cashed` | **`:data-pending`** | `C511:358-399` — same, for tick `8ae111bc…` |
| `2026-09-04-010-accepted` | `:flip-readiness` | **`:data-pending`** | `C511:62-111` — the verdict re-derives from `step.edn` + `world-before.edn` and reproduces the deposit line exactly |
| `2026-09-04-010-accepted` | `:per-node-runtime-validation` | `:not-contemporaneous` | as s5 |
| `2026-09-04-010-accepted` | `:rationale-regret` | `:not-contemporaneous` | structural: an accepted step is one tick and the rule needs a pair (`C511:306-325`) |
| `2026-09-04-010-accepted` | `:tensions-cashed` | **`:data-pending`** | the exception in §2 — `C511:381-386` refutes the repair because nobody has minted a tension naming this run, not because nothing could |

**Two labels are `:data-pending` and are NOT greens.** `:tensions-cashed` on s5
and re5 now *replays* `:red` under the (A)-strict reading adopted at
`aif-equations.edn :choices :tensions-cashed-reading` — every attributed tension
is uncashed and no `:cashed` event has ever been written. The deposited rows are
left alone: `:AD1` repairs by receipt, never by row mutation. Each basis says so.

## 4. What the live ledger now folds to

```
2026-09-01-s5                :red                        because :red-verdict [:selection-discrimination]
2026-09-04-010-accepted      :incomplete-uninstrumented  instrument the next run: [:per-node-runtime-validation :rationale-regret]
                                                         wait for the datum: [:flip-readiness :tensions-cashed]
2026-09-04-re5               :incomplete-uninstrumented  instrument the next run: [:flip-readiness :per-node-runtime-validation :rationale-regret]
                                                         wait for the datum: [:tensions-cashed]
```

**No run folds `:incomplete`** — the acceptance clause, re-read against the
split as the ruling directs. Stated plainly rather than claimed as more than it
is: the eight absences the `:blocker` measured are all still standing, and the
clause is met because the fold no longer answers with a word that stands for
two different jobs. Each run now names its two lists.

**`:incomplete-data-pending` is not reached on the live ledger.** Both incomplete
runs carry at least one `:not-contemporaneous` absence. The branch is exercised
only by the self-test fixture (control C26), and saying so is the point: a
status nothing reaches would otherwise look like a status that had been earned.

## 5. Machinery and its controls

- `--label-absence --run-id … --check-id … --kind … --basis … --by …`
  (`run_era_ledger.bb:382-418`) is the only write path into
  `:ledger/absence-kinds`, through the same validate-refuse-replace path
  `append-row!` and `catalogue-add!` take.
- The sha chain covers `:rows`, so labelling leaves every row and
  `:ledger/head-sha` untouched — control C24, and shown against `HEAD` below.
- Seven new controls, taking the self-test from 24 to 31 (19 negative, 12
  positive), all passing:
  C24 labelling touches no row · C25 an identical label is `:already-present`
  and the file byte-identical · **C26 the fold's branch table**, six outcomes on
  a fixture where each differs from its neighbour by one label · C27 an
  unlabelled absence folds to neither new word · C28 a label naming a pair that
  is not a typed absence is refused (both halves: no such row, and a `:green`
  row) · C29 a divergent relabel is refused · C30 a kind outside the declared
  set and a blank basis are refused.

## 6. Gates, bare exits

```
bb run_era_ledger.bb --check                       exit 0  (24 rows, 8 checks, 0 defects)
bb run_era_ledger.bb --self-test                   exit 0  (31 controls, 19 negative)
bb run_era_ledger.bb --report                      exit 0
clj-kondo --lint run_era_ledger.bb                 errors 0, warnings 0
check-parens.el run_era_ledger.bb                  OK
bash p4ng/empirics-futon/negative_controls.sh      PASS (133 negative, 53 positive)
bb p4ng/empirics-futon/pointer_check.bb            1727 pointers, 0 unresolved
```

Idempotence: all fourteen `--label-absence` calls replayed after the append and
every one returned `:already-present`; the ledger was byte-identical afterwards.

Rows untouched, checked against `HEAD` rather than asserted:

```
git show HEAD:…/run-era-ledger.edn  vs worktree
  :rows identical            true
  :ledger/head-sha identical true
  :check-catalogue identical true
  :ledger/absence-kinds      14 (new)
```

## 7. One drift found and not repaired here

`u56_fold_consequences.bb:11-13` replicates the run-fold rule in a comment and
cites `run_era_ledger.bb:432-451` for it. Both the line range and the rule it
quotes ("any `:typed-absence` => `:incomplete`") are now stale. That script is a
frozen `:U56` discovery record, is read by no gate (0 references in
`negative_controls.sh`), and refreshing it would change a committed discovery
artifact's output, so it is reported rather than edited.
