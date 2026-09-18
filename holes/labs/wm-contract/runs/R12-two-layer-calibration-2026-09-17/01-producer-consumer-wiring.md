# R12-4 — current calibration producer/consumer wired (second slice)

Author: zai-35 (R12 item owner), 2026-09-18, under briefing set v2
(`p4ng/wm-walkthroughs/item-owners/briefings/v2.edn`), which withdraws the
propose-only rule for this seat. Slice 1 is `00-reconciliation.md` (futon2
`db23dec8`): clauses C1/C2/C4/C5 executed, C3 reported as a gap. This slice
implements what remained implementable: cascade node **R12-4**, connect current
calibration use.

## What changed

| File | Change |
|---|---|
| `src/futon2/aif/calibration_cycle.clj` (new) | Pure commission→return→check→`admit!` cycle for the R12 apparatus scan |
| `scripts/futon2/report/war_machine.clj` | `generate-war-machine` runs the cycle; admitted receipt enters `scan-data` under `:r12-admission`; the R12 render section consumes it (admission status line) |
| `test/futon2/aif/calibration_cycle_test.clj` (new) | 8 tests / 24 assertions on the cycle |

## Clause-by-clause

- **R12-4 (producer/consumer with admitted revision).** Producer unchanged:
  `war_machine.clj` `scan-r12-apparatus` (route-tagged R12). NEW: its result is
  wrapped by `calibration-cycle/apparatus-return` into a commission-tied return
  (`:wm/r12-apparatus-return-v1`), structurally checked by `check-apparatus`
  (boolean availability; when available, numeric α/β per class and consistent
  counts; unavailable XTDB is a *valid approved negative*), and passed through
  `calibration-admission/admit!`. The admitted receipt — not the raw scan — is
  what the render layer now surfaces (distinct consumer with actual effect:
  refusal reasons would appear in the report). `admit!` at last has a
  production caller; the census "no production caller" finding from slice 1 is
  superseded by this commit.
- **C4 (Layer 1 never-value-evidence).** The label travels ON the admitted
  artifact: `:layer-1/label {:value-evidence :never, :source
  "p4ng/sec-catalog.tex:387"}`. Test `layer-1-is-never-promoted` asserts no
  `:value-evidence` key on the artifact and no outcome claim on the layer-1
  label. The raw scan stays visible under `:scan` (L1 diagnostics visible, as
  R12-4 requires).
- **C3 (independent L2 evidence) — still honestly NOT MET.** The artifact
  carries `:layer-2/independent-evidence {:status :not-available …}` citing
  WM-04 (2026-09-16): no eligible independent pair exists and the eligibility
  rule is pending prospective reviewed authority. This label is a feature, not
  a gap in the wiring: only eligible L2 evidence may support the external
  outcome claim, and none exists. Closing C3 requires R2/WM-04 authority work
  that is outside this seat.
- **C1/C2/C5** — unchanged from slice 1 (P121/P125/P130 UNAVAILABLE as
  originals; producer/consumer now strengthened by this slice; catalogue R12 ≠
  hyperparameter contract, re-pinned).

## Validation

- clj-kondo: 0 errors / 0 warnings on all three files (one pre-existing info
  in war_machine.clj at :4574, outside the changed regions).
- check-parens: clean on all three files.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.calibration-cycle-test
  -n futon2.aif.calibration-admission-test`: **11 tests, 32 assertions,
  0 failures, 0 errors.** Registered as a Test Registry warrant (see below).

## Item disposition

R12 remains OPEN: C3 (independently grounded Layer 2 evidence) is not met and
cannot be met from this seat — it needs the prospective eligibility/blinding
authority that WM-04's owner assessment names as outstanding. Everything else
in the acceptance text is now executed and retained here.
