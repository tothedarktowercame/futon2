# WM-01-admission-1: reconcile one stale R4 formal admission

From claude-3 to codex-4. **Author: codex-4. Reviewer: claude-3.** Bell claude-3 back when done (see the end).

Authority: codex-28, under Joe's standing successor authority. Dispatch text: `/home/joe/code/p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-admission-1.md` (sha256 `42b8442d5c363edcbf2fad23679a5e5b36710c4e4cb925150350fad6e031bcdc`). **Read it first; it governs.** This packet adds facts I verified; it does not widen the dispatch. DAG node: `WM-01-bindings`, cascade K6.

## Read this first: the global merge is already broken, by a fragment you may not touch

I ran `bb scripts/merge_witnesses.bb --check` at futon2 HEAD `43cf8563`. **Exit 1**, failing on a different witness:

```
{:message "node-witness-pin-mismatch",
 :claim "R17-dirichlet-accumulation-ieee-residuals-v1",
 :error :node-witness-pin-mismatch,
 :detail {:locator {:repo "mathlib4", :path "DarkTower/WarMachine/Holes.lean",
                    :sha256 "4dc0a76b99…"}, :actual "7f137e4f85…"}}
```

I then scanned all 56 fragments and all 212 pinned locators myself. **Exactly three are stale:**

| Fragment | Entry | Stale pin |
|---|---|---|
| `DirichletConcentrations.edn` | R17 (subject-artifact) | `Holes.lean` pinned `4dc0a76b99…`, actual `7f137e4f85…` |
| `PredictiveOutcomeKernel.edn` | R4 (subject-artifact) | `Holes.lean` pinned `4dc0a76b99…`, actual `7f137e4f85…` |
| `PredictiveOutcomeKernel.edn` | R4 (artifact) | `MachineForwardModelWitness.lean` pinned `02bc5c00a8…`, actual `62b15fe45e…` |

Consequences you must carry into the receipt:
- Fixing R4 **cannot** make the global merge pass. `DirichletConcentrations.edn` is outside your scope, so the global registry stays unpublishable after your work. That is the **exact merge blocker** codex-28 asked to have returned. Do not touch that fragment, and do not bypass global validation.
- The merger fails fast, so its own output names only R17. My inventory above is the complete list; re-derive it yourself rather than quoting me.
- The "old pins are rejected by the existing validator for the intended mismatch" control is already satisfied by reality. Capture it as a recorded run, not a synthetic one, and show the refusal names `:node-witness-pin-mismatch` on the R4 locators specifically (a scoped merge over just this fragment will surface R4 rather than R17).

## The protocol, as I read it from the code

- **`scripts/merge_witnesses.bb`** modes: default (validate and write), `--check` (validate and compare, no write), `--split`, `--negative-empty`, with `--fragments`, `--output`, `--authorities` overrides. A scoped run is `--fragments <dir> --output <tmp>`, which is how `test/witnesses/merge_witnesses_control.bb` exercises it.
- **Identity is frozen** by `preserve-ids!` against `identity-keys`: `[:id :node :equation :quantity :declaration :kind :artifact :subject-artifact :claim :polarity :scope :as-of]`. Both `:artifact` and `:subject-artifact` carry the shas, so **a successor with new hashes cannot reuse the old id**; reuse raises `:node-witness-id-revision-required`. For a prior `:admitted` entry, `:verification` and `:review` must also be unchanged.
- **Status may only be `:proposed` or `:admitted`** (`validate!`). There is no `:retired` status to set.
  - `:proposed` resolves every readable path but requires no verification or review receipt, and reports as `:pending` in the admissions header. The comment is explicit: proposals "are NOT evidence".
  - `:admitted` demands an executed verification receipt (`:executed? true`, exit 0, `:result :passed`), a review receipt with `:verdict :approved` and a named reviewer, subject equality between witness and both receipts, and declaration/axiom checks.
- **`:historical-receipt`** is the existing field for retaining a superseded measurement (`{:source-commit … :reported-witness-sha256 …}`), used in `machineObservation.edn` and others. This is the closest thing the protocol has to a retirement idiom.
- **No `-v2` id exists yet.** All 35 distinct ids end `-v1`, so your successor id will be the first of its kind; keep the suffix convention.
- `PredictiveOutcomeKernel.edn` owns exactly one node-witness: the R4 entry. Everything else in that file is wrapper metadata (`:check`, `:expected-rejection`, `:positive-proof-receipt`, `:contract-sha`, `:run-sha`).

## Retirement: two mechanisms, both expressible — choose and justify

The dispatch says to retire the stale current admission and publish a new identity. Nothing in the protocol sets a "retired" flag, so the honest options are:

1. **Remove** the old node-witness from the fragment, so it leaves the current inventory, and retain it verbatim (with source commit and hashes) in your receipt.
2. **Demote** it in place to `:proposed`, keeping every other field byte-identical. `preserve-ids!` permits this — status is not an identity key, and the check on `:verification`/`:review` only binds when the **prior** status was `:admitted`, which it compares against the unchanged values. It then reports as `:pending` rather than `:verified-binding`.

Option 2 preserves the entry verbatim but leaves a record that reads as an open proposal rather than a withdrawn admission. Option 1 removes it from the registry entirely and relies on your receipt for history. **Pick one, say why in the receipt, and if you judge that neither expresses an honest retirement, stop and return that refusal instead of inventing a third.**

## The successor claim

Accepted formal evidence: mathlib4 `f40c936a64`, futon2 `6f494738` (author) and `e99c08c4` (my review). Current hashes, which I verified:

| Path | sha256 |
|---|---|
| `DarkTower/WarMachine/MachineForwardModelWitness.lean` | `62b15fe45ed0cc9eb12eb659b55122c1894cbcb5778b64feec758e965d427389` |
| `DarkTower/WarMachine/MachineModelSpec.lean` | `020af77ed11f0b5407bd6dd22a0a9e6e81ae30132ac41508277c0567500ca80f` |
| `DarkTower/WarMachine/FloatCarriedRowCorrespondence.lean` | `f8e9cdacaa47f2bd4131a41bd2876c5731356cd7b33f2dea3268125360be6e22` |
| `DarkTower/WarMachine/Holes.lean` (current) | `7f137e4f85b2aae18b699942bdc505faa3f311752a096718704fb2ca7b7a07d3` |

The successor's claim must say **constructed formal witness and support laws** only. It must not claim fresh observations, production composition correspondence, or exact normalization of approximate rows. `float-carrier-1` proved the opposite for the retained row: its total is `1 + 1/2^55`, admitted under the bound and explicitly not exactly normalized.

**Keep the successor `:proposed`.** My review is the distinct subject-bound review; promotion to `:admitted` needs receipts that do not exist yet, and is not authorized here. Say plainly in the receipt that the successor is pending review, not admitted.

## Scope

Only these paths:
- `checks/witness-fragments/PredictiveOutcomeKernel.edn` (currently sha256 `7f9d4d46…`)
- generated `checks/witness-registry.edn` (currently sha256 `5f153fc1…`) — **regenerate via the merger; never hand-edit**
- receipts and controls under `holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-admission-1/`

Not allowed: validator, schema, runtime or Lean changes; other fragments; checkbox edits; serving actions; clicks; pushes; held P1 work; choosing successor work.

Both in-scope paths are clean in the tree right now. futon2 is shared, with ~189 unrelated dirty paths: stage explicit paths only, never `commit -a`, never amend, and check `git log -1` before committing.

## Controls to run and retain (command, exit, scope, output; don't pipe gate output)

1. **Recorded refusal on the current stale pins**, showing `:node-witness-pin-mismatch` against the R4 locators.
2. **Scoped merge of the new fragment** (`--fragments <tmpdir> --output <tmp>`), which validates this fragment in isolation. Label it exactly that: it is **not** a global registry publication.
3. **Global `--check`**, retained with its refusal, to evidence the R17 blocker.
4. **Id-reuse rejection**: reusing the old id for the changed subject must raise `:node-witness-id-revision-required`.
5. **Pin-tamper rejection**: altering a new source pin must raise `:node-witness-pin-mismatch`; borrowing the old review must be rejected.
6. **Merger control test**: `bb -cp scripts:test test/witnesses/merge_witnesses_control.bb` (it covers `:id-reuse`, `:stale-bytes`, `:missing-declaration`, `:nil-receipt`, `:missing-readback`).
7. **Witness negatives** for the wrapper this fragment registers, one at a time (about 6.5s and 3.4GB each, per futon2 `AGENTS.md`):
   `bb checks/predictive_outcome_kernel_witness.clj --negative-unconditional`, then `--negative-softmax`.
   Their Lean files do not import `MachineModelSpec`, so I expect them unaffected; record the result either way.
8. clj-kondo and check-parens (files after `--`) on any Clojure control you write.

## Receipt: `wm-01-admission-1/RECEIPT.md`

Record: the old entry verbatim with its source commit and hashes; the exact old→new identity mapping; which retirement mechanism you chose and why; what the scoped merge actually validated versus what global validation refused; every command with exit status; the fragment and registry hashes before and after; and the limits, including that the successor is proposed and not admitted, and that no production correspondence transferred.

## Stop and bellback

Stop once the scoped artifacts are committed. **Bell claude-3 back** with the commit sha, the author verdict, the old→new identity mapping, what the canonical merge validated and refused, the exact merge blocker, and the remaining WM-01 clauses. A discovery beyond this scope is a question in the bellback, not a task.
