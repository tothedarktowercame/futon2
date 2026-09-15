# WM-01-admission-1: claude-3 review

**Reviewer verdict: ACCEPT at the scoped level** for futon2 `0adcadb0` (author codex-4). The successor identity is prepared, `:proposed`, formal-only, and honestly bounded. It is **not admitted and not published**, and this review does not promote it.

Two required gates are red. Both are **pre-existing and outside this packet's scope**, codex-4 reported rather than bypassed them, and one of them is a gap in my own earlier review (F1).

- Dispatch: `p4ng/wm-walkthroughs/build-loop/claude-3/WM-01-admission-1.md` (sha256 `42b8442d…`), elected in codex-28 job `invoke-1789515232872-21293-468359b8`.
- Author job: `invoke-1789515626929-21295-2874c2bf`. Handoff: `../handoffs/WM-01-admission-1-to-codex-4.md` (sha256 `04b01638…`).

## What I checked

1. **Scope.** `0adcadb0` touches only `checks/witness-fragments/PredictiveOutcomeKernel.edn` and this receipt directory. `checks/witness-registry.edn` is **not in the commit**, and its bytes are identical before and after (`5f153fc1…`). No other fragment, no Lean, runtime, validator or schema change. Nothing pushed: mathlib4 is clean at `f40c936a64`.
2. **The registry was not hand-edited.** It is absent from the commit, byte-identical, and global regeneration refuses before writing. There is no partially-published state.
3. **Successor entry** (the fragment holds exactly one node-witness):
   - new id `R4-forward-model-float-carried-formal-support-v2`; the old id appears nowhere in the fragment;
   - `:status :proposed`; **no `:verification` or `:review` keys at all**, so nothing was borrowed;
   - pins are the current bytes: `MachineForwardModelWitness` `62b15fe4…`, `MachineModelSpec` `020af77e…`, `FloatCarriedRowCorrespondence` `f8e9cdac…`, `Holes` `7f137e4f…`;
   - `:scope :constructed-formal-witness-support-laws`, `:as-of "2026-09-15"`;
   - the claim is constructed support laws only, and explicitly disclaims fresh observations, production composition correspondence and exact normalization of approximate rows;
   - the evidence limit records the retained row's total as `1 + 1/2^55` and states the entry is pending review, not admitted.
4. **Retirement by removal, justified.** codex-4 chose removal over demotion because demoting a withdrawn production admission to `:proposed` would read as work awaiting admission. I verified the preservation independently: `old-entry.edn` equals the pre-commit fragment's entry, `old-fragment.edn` matches the pre-commit bytes (`7f9d4d46…`), wrapper metadata is unchanged, and `successor-subject.edn` equals `n/subject` of the new entry. No status was invented.
5. **My own re-runs:**

   | Check | Result |
   |---|---|
   | `merge_witnesses.bb --check` (global) | exit 1, `:node-witness-pin-mismatch` on R17's `Holes.lean` pin — the blocker is intact |
   | scoped merge of the new fragment | exit 0, reporting the v2 id as `:pending` |
   | id reuse against the prior registry | exit 1, `:node-witness-id-revision-required` |
   | pin tamper through the merger | exit 0 (see F4) |
   | pin tamper through `resolve!` directly | `:node-witness-pin-mismatch` |
   | `merge_witnesses_control.bb` | exit 1 at `:genuine-deterministic-merge` (F3) |
   | both wrapper negatives | exit 2 (F1, F2) |
   | both negative Lean fixtures, directly | exit 0 — they still reject |
   | clj-kondo, check-parens | 0 errors / 0 warnings, OK |

   My runs left `checks/` clean.

## Findings

- **F1. The wrapper gate has been red since the repair, and my acceptance review missed it.** `positive?` in `checks/predictive_outcome_kernel_witness.clj` fails only on `:positive-source-drift`: the positive receipt pins the declaration slice for `predictive` at `5c6f447d…`, while the live slice is `24e92e86…`. That declaration lives in `PredictiveOutcomeKernelWitness.lean`, whose last change is **`480a666ad2`** (the repair, 06:36 today), which rewrote its `mass`, `nonnegative` and `normalised` fields and added the two new support fields. So the drift dates from the repair, not from float-carrier (`f40c936a64` touched neither this file nor this receipt) and not from this packet.
  - WM-01-acceptance-1 and my review of it checked builds, controls and axioms in mathlib4, but not the futon2-side receipts pinned to those Lean declarations. This registered wrapper was already failing when I concurred with ACCEPT. I should have checked the receipts that pin the changed declarations.
- **F2. The negatives still reject; the wrapper's message misleads.** In this idiom a negative fixture is expected to *elaborate*, because its `#guard_msgs` asserts the rejection text; `rejected?` is therefore `(zero? (lean-exit …))`. Both fixtures exit 0 directly, so both mutations are still caught. The wrapper nonetheless prints "mutation slipped" whenever `positive?` fails, which describes the wrong failure. codex-4's receipt states both facts, and they are consistent once the convention is read.
- **F3. The merger control suite failure is pre-existing.** Its inputs are unchanged since `43cf8563` (`git diff --exit-code`, exit 0), and it asserts every merged entry is `:proposed` while the three fragments it copies hold `:admitted` entries. **Consequence worth recording:** it fails before reaching `:stale-bytes`, `:missing-declaration`, `:nil-receipt`, `:missing-readback` and `:id-reuse`, so the merger's own negative controls are currently unexercised by the suite. codex-4's separate R4 controls cover `:id-reuse` and pin mismatch, but not the rest.
- **F4. Proposed-mode merges do not enforce pin hashes.** `validate!` on a `:proposed` entry only resolves paths for existence; hash equality is checked on the `:admitted` path. I confirmed both directions. So "the scoped merge passed" is weaker evidence than it sounds: the successor's current pins are evidenced by codex-4's separate `resolve!` calls, which I reproduced, and must be re-checked at promotion. codex-4 states this limitation rather than glossing it.
- **F5. Retirement is effective only in the editable fragment.** Because regeneration refuses before writing, the published `checks/witness-registry.edn` still lists the old entry and still advertises it in the `;; admissions` header as `:verified-binding`. Anything reading the generated registry today still sees the stale admission. That is the operative risk until R17 is reconciled, and the receipt says so.

## On codex-28's owner question

I'd separate the three into their own bounded packets rather than bundling:
1. **R17 reconciliation** in `DirichletConcentrations.edn`, mirroring this packet's protocol. It is the only remaining blocker to global publication, and until it lands, F5 stands.
2. **The positive-receipt drift** belongs with WM-01-bindings: it is repair fallout of exactly the kind K6 covers, so re-pinning or re-recording that receipt is a binding task, not a test fix.
3. **The merger-suite expectation** is a test-side defect. It deserves attention because, while it fails early, the merger's own alarms go unrun.

None of these is authorized here, and I have started none of them.

## Limits

- Scoped review only. I did not promote the successor, edit the registry, or run a global publication.
- I verified the successor's subject, pins and claim wording, but a subject-bound verification and review pair does not exist, so the entry remains correctly `:pending`.
- WM-01 is **not** checked. Its remaining clauses are unchanged by this packet.
