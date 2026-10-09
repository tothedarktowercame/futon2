# E-kimi-task-129 — HGT-E-I: outer cascade E becomes the target-grain habit prior

**Requisition:** completed — 2026-09-28T21:13:06Z, job invoke-1790629768225-26359-add9395d, state failed

**VERDICT (2026-10-09, provisional):** OPEN — Requisition closed but the job ended in state failed, so the task's work was not achieved and the goal remains. _(WM status classification by zai-3, medium confidence; not yet confirmed by the author.)_

Clocked in by claude-1 for kimi-3 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HGT-E-I — the outer cascade's E becomes the target-grain habit prior

From claude-1 (PROOF-2a lead). Bell claude-1 back with a summary and the commit sha.

## Why
PROOF-2a ⟨1⟩3, Holes row H-G-target. Clause T's selection posterior is p(t) ∝ E_t·e^(−ΔG_t). Today E is hard-coded uniform, `{:basis :uniform-no-data}` (futon2 `src/futon2/aif/outer_cascade.clj` lines 9-10, 83-108 `mixture-posterior`, 90-92 the guard, 138-152 the record). The discovery HGT-E-D (report `/home/joe/code/storage/proof-2a/hgt-e-d/REPORT.md`, read it first) found no document defines E at target grain. claude-1 has now defined it (below). This packet implements that definition in `select` and nothing else. Wiring the live caller (`outer_loop.clj:36`) to pass the fold is a later packet: do not touch outer_loop.clj.

## The definition (claude-1, 2026-09-28; use exactly this)
Let S be the eligible support `select` already computes. Let F be an enactment-habit fold state (the value `futon2.aif.enactment-habit/fold` returns; its receipts are under `:enactment-records`, a map record-id → receipt, already deduplicated by `[click candidate]`).

- A receipt is ADMITTED iff its `:delta` is 1 (the fold sets 1 only for an empty W_c failure vector; do not re-derive that).
- The receipt's target is the mission slot of its `:policy-key` (use the existing `enactment-habit` accessor that exposes it, `key->view` → `:mission`, or `cascade-prior`'s own; do not parse the vector by hand if an accessor exists).
- n_t = the number of admitted receipts whose target is exactly t, for t ∈ S.
- E_t = (n_t + 1) / Σ_{s∈S} (n_s + 1). Exact rationals (Clojure ratios), not doubles.
- Admitted receipts whose target is not in S are UNJOINED: they count toward no E_t and are listed on the record (their record-ids, sorted), never dropped silently. This covers A1 (a key naming an instance below the mission) and targets outside today's support.

Properties this must have (they are the tests): with no admitted receipt in S, E_t = 1/|S| exactly, so today's posterior is reproduced exactly; this is the Lean's `Proof2.EnactmentHabit.habitPrior` with α = 1 over the menu S (`habitPrior_uniform_of_noRecords` is the empty case).

## Change (futon2 `src/futon2/aif/outer_cascade.clj` and its test only)
1. `select` takes a new optional opt `:enactment-fold` (a fold state). Absent opt → exactly today's behaviour and today's record, `{:basis :uniform-no-data}`, byte for byte. The existing `:enactment-records` opt stays a recorded input with no effect on the law (its test `declared-inputs-reach-the-record-without-changing-the-law` must keep passing unchanged).
2. With `:enactment-fold` given: E_t as defined; the record's `:E` becomes `{:basis :target-habit-prior :alpha 1 :counts {t n_t ...} (all t in S, zeros included) :unjoined [record-id ...]}`. `mixture-posterior` uses E_t where it used 1/n today, including E(D) = Σ_{t∈D} E_t. Replace the `:uniform-no-data`-only guard with a guard admitting exactly these two bases; any other basis still throws.
3. Update the namespace docstring's E paragraph to state the definition and the unjoined rule, in the file's existing style.

## Tests (futon2 `test/futon2/aif/outer_cascade_test.clj`; build fold states with `enactment-habit/fold` or hand-built maps of the same shape)
- Empty fold (no receipts, or only `:delta 0` receipts): posterior and chosen target equal today's for the same entries and seed; `:counts` all zero; `:E :basis :target-habit-prior`.
- Support {"M-a" "M-b" "M-c"}, two admitted receipts for "M-a", none for the others, no G defined: posterior exactly {M-a 3/5, M-b 1/5, M-c 1/5}.
- A `:delta 0` receipt for "M-b" changes nothing.
- An admitted receipt for "M-z" (not in S) appears in `:unjoined` and changes no E_t.
- With ΔG defined on a subset D, the posterior is E(D)·E_t·e^(−ΔG_t)/Σ… with the new E_t (one numeric case, tolerance 1e-12).
- All existing outer-cascade tests pass unchanged.

## Bad cases (run them, report each; do not commit them)
- Plant: E ignores counts (always 1/n) → the 3/5 test fails.
- Plant: count `:delta 0` receipts too → the `:delta 0` test fails.
- Plant: count unjoined receipts into the nearest target / drop the `:unjoined` list → the M-z test fails.

## Acceptance and rules
- `env -u GIT_DIR -u GIT_WORK_TREE clojure -M:test -n futon2.aif.outer-cascade-test` green in `/home/joe/code/futon2` (run it before your change once and record the count).
- Gates: clj-kondo on the two changed files; `/home/joe/code/futon4/dev/check-parens.el` (entry point `arxana-check-parens--check-parens`).
- One commit by explicit path (`git commit -m ... -- <the two files>`), never `-a`, amend, stash or `git add -A`. The checkout is shared with other agents. `git config user.name` must print `Joseph Corneli`.
- Do not reload anything into the running JVM. Do not touch outer_loop.clj, enactment_habit.clj, the registry or Lean.
- Report `/home/joe/code/storage/proof-2a/hgt-e-i/REPORT.md`: test counts before/after, each bad case and its failing test name, the commit sha. Mark each claim Ran or Read.

## Closure criteria (provisional, 2026-10-09)

_Drafted from this document's own stated goals during the War Machine status classification (zai-3, medium confidence); not yet confirmed by the author._

- [ ] Outer cascade E is wired as the target-grain habit prior
- [ ] clj-kondo on the two changed files and arxana check-parens pass
