# Handoff P1b-2a to codex-2: the pure bridge from the P1a belief to the store

From claude-2, the War Machine build lead under Joe's 2026-09-15 commission. I
will review this independently. **When you are done, bell claude-2 back with a
summary, the commit sha(s) and the gate receipt paths.**

## Goal (one behaviour: build a store-ready proposal, deterministically, with no IO)

Add a new namespace `futon2.aif.work-target-tick` in futon2 with these **pure**
functions:

- `payload-validator`
- `predecessor-from-store`
- `operation-identity`
- `build-proposal`

It joins your reviewed P1a module (`work_target_belief.clj`, as fixed in
`474184a4`) to codex-3's store (`work_target_store.clj`, as fixed in `e38ea7e5`).

Checklist: WM-02 (cascade nodes Q2, Q4, Q8), node R1, E02.

**Out of scope for this packet:**
- no `war_machine.clj`, `full_loop_runner.clj` or `trace.clj` edits (P1b-2b
  and P1b-2c)
- no registry file reading
- no production store path, no genesis, no reload or click

Tests may use isolated temporary stores through the store API.

## Authority: read first

- The decisions:
  - Q-D to Q-G, which are binding:
    `/home/joe/code/p4ng/wm-walkthroughs/build-loop/decisions/P1-QD-QG.md`
    (p4ng `b09e3dd`).
  - Q-C: `invoke-1789502747631-21229-70a1d4a7`.
- The work-target declaration:
  `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/declarations/wm-work-target-interpretation-v1.edn`
- The reviews that set requirements here, both in
  `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/`:
  - `p1a-review/REVIEW.md` (F3, N1–N4)
  - `p1b-1-review/REVIEW.md` (N5–N9)
- The code:
  - `src/futon2/aif/work_target_belief.clj`, public API: `read-declaration`,
    `admissions`, `carry-and-introduce`, `target-belief-input`.
  - `src/futon2/aif/work_target_store.clj`, public API: `open-store`,
    `read-store`, `commit!`, `resolve-reference`.
  - Predecessor statuses `carry-and-introduce` accepts: `:present`,
    `:established-no-snapshots`, `:missing-after-genesis`, `:unreadable`.

## The four functions

### 1. `(payload-validator declaration-envelope)` → `(fn [payload] → :ok | typed refusal)`

This is the store's injected validator. It runs on **every** committed
snapshot on every read, and on every new proposal, so it must be pure and
cheap (N5). It refuses a payload unless all of the following hold:

- `:model-context` equals the declaration's model context (the same one
  `carry-and-introduce` builds).
- `(set (keys belief))` equals `(set (keys lineage))`. No half-present target
  may enter the store.
- Every belief row, for **every** admitted target and not only those later
  read by row 7, is over exactly the seven statuses, with finite nonnegative
  masses, and `row-sum-admission` is not nil.
- Every lineage has:
  - `:D` equal to the declaration's name, revision, authority and
    declaration SHA-256;
  - its interpretation revision and decision reference;
  - `:updates :no-admitted-observations`;
  - `:introduced-at` ≤ `:information-cutoff` ≤ the payload's
    `:information-cutoff`. Compare instants, not strings.
- `:candidate-population` and `:registry-context` are present, carrying pins
  (see function 4).

It must never repair, normalise or fill anything.

### 2. `(predecessor-from-store activation store-read)` → one of the three typed modes below

`activation` is an explicit input (Q-D, Q-G). Configuration supplies it in
P1b-2c. It is either:
- `{:status :not-activated}`, or
- `{:status :activated :evidence <opaque>}`.

The mapping:

| Activation | Store read status | Result |
|---|---|---|
| not activated | `:model-not-established` | `{:mode :inert :reason :model-not-established}` |
| not activated | anything else | `{:mode :stop :failure {:kind :activation-mismatch :store-status s}}` |
| activated | `:model-not-established` | `{:mode :stop :failure {:kind :activated-store-missing}}`. This is **not** inert. |
| activated | `:established-no-snapshots` | `{:mode :state-writing :predecessor {:status :established-no-snapshots} :expected-head h}` |
| activated | `:committed` | `{:mode :state-writing :predecessor {:status :present :state (select-keys payload [:belief :lineage :model-context])} :expected-head h}` |
| activated | `:initialization-incomplete`, `:damaged`, `:pending-recovery`, or anything unknown | `{:mode :stop :failure {:kind :store-not-usable :store-status s :reason r}}` |

In the `:committed` row, check that the state is exactly what
`carry-and-introduce` expects.

For every `:stop`, carry the store's own reason.

### 3. `(operation-identity caller store-head cutoff)` → an operation map for `commit!`, or a typed refusal

`caller` is either:
- `{:kind :full-loop-attempt :run/id .. :attempt/id ..}`, or
- `{:kind :scheduled-tick :tick-run/id ..}`.

Rules (Q-F):

- **Structured `:id`, never delimiter concatenation:**
  `{:purpose :work-target :caller <kind> :occurrence {...its ids...}
  :store/id <from head> :genesis-sha256 <from head>}`.
- **Scope:** the store id and genesis hash come from `store-head`.
- **Missing components:** refuse. A path without a click id must not invent
  one.
- **Stability:** the id depends only on the occurrence and the store scope,
  never on time or the head's sequence. A retry of the same attempt gets the
  same id.
- **Returned map:**
  - `:id`
  - `:kind :carry-and-introduce`
  - `:caller-identity-type` = the caller kind
  - `:information-cutoff cutoff`
- **Intent hash:** the store hashes `[expected-head operation payload]`.
  Document in the namespace docstring that **no** volatile metadata (wall
  clock, durations, hostnames) may enter the operation or the payload.
  Changing a semantic input must change the intent.

### 4. `(build-proposal {:keys [declaration activation store-read registry-snapshot candidates caller tick-context]})` → one of `:inert`, `:stop` or `:proposal`

The function is deterministic: equal inputs give an equal result, with no
`Instant/now`. All times come from `tick-context`. It proceeds in order:

1. **Mode.** Call `predecessor-from-store`. If the result is `:inert` or
   `:stop`, return it with no rows.
2. **Admissions.** Call `admissions` over `candidates` with the registry
   snapshot. A refusal here, such as an unreadable registry or a missing pin,
   is `{:mode :stop :failure ...}`.
3. **Carry.** Call `carry-and-introduce`. A whole-state refusal is a `:stop`.
   **Per-target `:target-refusals` are also a `:stop`**, since the validator
   would refuse them anyway. The failure names the targets.
4. **Coverage marking.** In the non-admitted receipt, mark every
   `:open-mission` candidate with `:coverage :unresolved-full-policy-coverage`
   and keep P1a's reason (N2). Every candidate stays in the receipt.
5. **Payload.** Build it with exactly these fields:
   - `:model-context`
   - `:belief` and `:lineage`
   - `:information-cutoff` (the tick timestamp)
   - `:admissions` (the admitted map)
   - `:not-admitted` (the complete vector)
   - `:candidate-population {:count n :sha256 <SHA-256 of a canonical EDN encoding of the candidate action maps, in their given order>}`.
     This is N3.
   - `:registry-context {:read-at .. :pins [{:id :path :sha256 :status-class} ...]}`,
     sorted by id.
6. **Self-check.** Run `(payload-validator declaration)` on the payload. A
   refusal means `:stop`; it is never repaired.
7. **Row-7 inputs.** For each admitted target, call `target-belief-input`
   with the full single-entity row-7 context and keep the results **outside**
   the payload, under `:row-7-inputs`.
8. **Return:**
   - `{:mode :proposal :payload p :expected-head h`
   - `:operation (operation-identity ...) :row-7-inputs ...}`

## Controls (tests; each must fail under the named wrong implementation)

- **Validator.** One test per refusal: a support mismatch, a bad mass, a
  wrong D or declaration hash, update lineage, a half-present target, a
  cutoff after the payload's cutoff, the wrong model context, missing pins.
  It accepts a production-shaped payload built by `build-proposal`. A mutant
  that validates only the targets row 7 reads must fail.
- **Adapter.** Every cell of the table above. Mutants that must fail:
  - "activated plus model-not-established is inert";
  - "damaged or pending is state-writing".
- **Identity.**
  - Injective: a run `"a/b"` with attempt `"c"` differs from a run `"a"` with
    attempt `"b/c"`.
  - Stable across two calls, and across different head sequences for the
    same occurrence.
  - Different attempts give different ids.
  - A missing run or attempt id refuses.
- **Builder.**
  - Deterministic: two calls on equal inputs give equal results, and the
    store-encoded payload is identical.
  - Not activated with an absent store gives `:inert` with no rows.
  - Every stop cause is covered.
  - `:open-mission` is marked and retained.
  - The candidate-population hash changes when a candidate changes.
- **End to end, through the real store API in a temporary directory:**
  1. Initialize a test genesis (test-only authority).
  2. Build a proposal and commit it with the validator.
  3. Read it back.
  4. Run the next tick's builder: it carries, and the ticket introduced this
     tick is new.
  5. Commit again.
  6. A lost-response retry, with the same operation and the **original**
     expected head, returns the original reference.
  7. A proposal built from a stale head gets `:stale-predecessor` from
     `commit!`. There is no recomputation (Q-E).

If the store or P1a APIs cannot support one of these coherently, **stop and
bell me with the concrete conflict**.

## Gates (futon2 root; run each gate bare, not piped, and keep exit statuses per AGENTS.md)

- `clj-kondo --lint` on the new src and test files
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval "(arxana-check-parens-cli)" -- --no-defaults <files>`
- `clojure -X:test :nses '[futon2.aif.work-target-tick-test futon2.aif.work-target-belief-test futon2.aif.work-target-store-test]'`
- Receipts go to `futon2/holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/p1b-2a/`.

## Commit

The worktree is shared, so stage **explicit paths only**: the new src and
test files and `p1b-2a/`. Never `commit -a`; never amend others' commits.
Do not touch the P1a or store sources. If a change to one of them seems
necessary, bell me instead.

Bell claude-2 back with:
- the summary
- the sha
- the receipt paths
- any conflict
