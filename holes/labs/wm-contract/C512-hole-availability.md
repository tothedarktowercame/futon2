# C512 — hole availability enters the task-belief ladder

Date: 2026-09-05. Code commit: `24cea67e`. Step:
`015-015-holes`, run id `9ec7ab9f-6ce0-48ec-bf8b-2b0980e40a14`.

Behind the existing, still-default-off `FUTON_WM_TASK_BELIEF_LADDER` flag,
`:advance-mission` now reads its recorded `:open-hole-count`. Zero holes is a
typed rung-3 refusal with reason `:no-open-holes`; its refusal retains the full
case-history/kin classification that availability overrode. Positive hole
counts multiply the prior rung-1 or rung-2 factor by `h/(h+1)`, and the
derivation records the count, factor, and support-map string. An absent or
malformed count remains an explicit evidence gap and does not silently mean
zero. No flag or default changed.

## Controls

The focused namespace has 12 tests / 51 assertions, all green. The pre-existing
controls cover direct rung 1, constructed/provenanced rung 2, construction-
exhausted rung 3, case-history ordering, relation choice, removal/scaling,
empty-history refusal, planted classification, and tension mint shape. New
controls cover:

- a zero-hole candidate with four direct decisions is refused and retains that
  overridden rung/support;
- a planted one-hole equivalent remains rung 1 and receives factor
  `0.8 * 1/2`;
- same-support rung-2 candidates with 2 and 24 holes receive distinct factors.

The real seam namespace remained green at 97 tests / 546 assertions, including
flag-off identity and off-arm replay controls. `clj-kondo` reported 0 errors / 0
warnings on both changed files; `check-parens.el` reported `OK`.

## One generation-2 step

Invocation:

```text
FUTON_WM_TASK_BELIEF_LADDER=1 ./wm_step.sh step /home/joe/code/futon2/data/wm-step/w1 015-holes
./wm_step.sh check /home/joe/code/futon2/data/wm-step/w1 /home/joe/code/futon2/data/wm-step/w1/steps/015-015-holes
```

The pin verified with 0 missing, added, or changed files. No pin-drift override
was used. The tick exited 0 and the ladder judgment is present in the persisted
trace:

- input census: 17 out-of-scope, 3 rung-1, 32 rung-2, 98 rung-3;
- refusals: 98 total — 53 `:no-open-holes`, 45
  `:task-belief/zero-support-construction-exhausted`;
- ranked field after later capability admission: 48 candidates — 3 rung-1,
  32 rung-2, 13 out-of-scope;
- chosen candidate: `[:advance-mission "M-zaif-harness-v1"]`, rank 1, unique
  score (tie width 1, band `[1 1]`);
- field scores: 42 distinct among 48 candidates;
- RE7: `:green`, 9/9 controls (4 negative, 5 positive).

The `:no-open-holes` records visibly retain what they override. For example,
both `M-aif-policy-conditioned-eig` and
`M-wm-aif-policy-grain-compliance` retain direct rung 1, support 44, factor
44/45 inside `:refusal/overridden-task-belief`; the vacuous action is still
refused.

## Plateau comparison

Step 014 before widths: `[6 3 3 2 2 2 2]`.

Step 015 after widths: `[6 2]`.

Every mission-band plateau was drained. The width-2 survivor at ranks 37–38 is
the `address-sorry` pair; the width-6 survivor at ranks 43–48 is the declared
`learn-action-class` ignorance band. Both are out of scope. There is therefore
no remaining mission tie requiring another discriminator and no equal-positive-
hole evidence gap in this step.

## Isolation and repository gates

The live trace remained SHA-256
`f343432d772986ddc2f7fe38107913afc997201ebae9b6240dff152e235b1120`,
mtime `2026-09-04 07:50:47.992676896 +0000`, size 3000420. The four live
rationale files retained their pre-step mtimes and sizes. The run-era ledger
remained SHA-256
`6ad4172d7fee402833d49ae27de1ace6a39e8738c6736b881f999d70b9d3efe7`,
mtime `2026-09-04 16:41:01.480838648 +0000`, size 38699. The run lock was
released. The pin remains generation 2, accepted only through `010-accepted`;
the step was neither accepted nor deposited.

Repository gates: `negative_controls.sh` PASS (35 negative, 18 positive),
`pointer_check.bb` 1335 pointers / 0 unresolved, and `worklist_check.bb` 155
items OK with 0 validation failures. `worklist.edn`, `wm_step.sh`, and
`trace.clj` were not edited.
