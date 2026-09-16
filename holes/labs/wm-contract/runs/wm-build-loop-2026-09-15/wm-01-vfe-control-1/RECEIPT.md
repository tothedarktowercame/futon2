# WM-01-vfe-control-1

Author: codex-2. Independent review and reviewer reruns: pending claude-3.
Dispatch SHA-256: `e7e1987fd18735323a973c5eab781d0810d555e62f74b49ab60a66528664d8e2`.

## Change and meaning

Only the weakened-positive replacement string in the wrapper changed. Its RHS
now mirrors the positive theorem's construction of nonnegative precision:

```lean
-- old RHS (ill-typed after the precision repair)
variationalFreeEnergy (fun _ => gaussianReference.precision)
  (fun _ => gaussianReference.predictionError) := by
  rfl

-- corrected RHS
variationalFreeEnergy
  (fun _ => ⟨gaussianReference.precision, by norm_num [gaussianReference]⟩)
  (fun _ => gaussianReference.predictionError) := by
  rfl
```

This deliberately replaces the substantive F=1 conclusion by self-equality.
The positive theorem still computes F=1 from fourteen channels with precision 2
and prediction error 1. Its precision argument discharges nonnegativity by
norm_num; the mutation uses that same argument and elaborates by reflexivity.
No positive theorem, fixture, negative Lean fixture, validator, mode semantics,
message or exit convention changed. No exactly-once guard was added to the
wrapper; the authored independent control enforces exactly one occurrence for
these measured sources.

## Four independent controls

`control.clj` reads the actual substitution strings from the predecessor and
edited wrapper, checks their target strings agree, and counts **exactly one**
occurrence in the positive source. It retains the full exact old and new strings
in `replacement.edn` and resulting sources in `old-mutated-source.txt` and
`mutated-source.txt`; no copied substitute test string stands in for the wrapper.

1. Exactly one substitution, resulting source differs from the original:
   `replacement.edn`.
2. Separate elaboration of the corrected mutated source: **exit 0**, empty
   stdout and stderr, `corrected-elaboration.edn`. The old replacement gives
   **exit 1**, the expected ℝ versus NonnegativeReal type error,
   `old-elaboration.edn`.
3. Fresh unmutated receipt: **pass true**, no failures,
   `unmutated-validation.edn`. After installation the canonical receipt was
   loaded and freshly validated again: `canonical-validation.edn`, pass true.
4. Fresh receipt with corrected weakened source override: **pass false**,
   failures exactly `[:positive-source-drift]`, `weakened-validation.edn`.
   Comparing all retained witness declaration slices locates the sole change at
   constantGaussianReference (`drift-location.edn`):
   `6b5ab5c8be55a1ea15f814db40384e329ecb4930bc93f9dbe737ecb409325e45`
   → `2709d50a58599e1aa69c2d3cd23b51a291ba1a3167ac5c890ed2df0238f746fa`.

The wrapper modes run only after these controls and installation. Thus neither
the old stale receipt nor a compilation error can account for rejection of the
corrected weakening.

## Three separate receipt identities

- Predecessor: `predecessor.edn`, recorded-at `2026-09-08T00:00:00Z`, SHA-256
  `fe6f51a9eef81f59de6b8aadc9be4f15fd8c158fcb6c345517f77a90d53afbf5`.
- Retained receipts-2 candidate: `retained-candidate.edn`, recorded-at
  `2026-09-16T00:26:24.673983334Z`, SHA-256
  `522bcd85eb14e4b22c77c10350cd0d9df42be18ca68b6261b87996b7322e417c`.
- Freshly executed successor: `fresh-successor.edn`, recorded-at
  `2026-09-16T12:56:48.954835438Z`, SHA-256
  `5678a915a38bd3380906cf243d25f3c74931506abdb9cad527d573e6f9eb9fca`.
  Byte-equal to the installed canonical receipt.

`basis-record` freshly measured source slices, fixture identity, toolchain and
the theorem/axiom result at current sources. `candidate-to-fresh-diff.edn`
shows only the timestamp differs from the retained candidate, after fresh
execution. `predecessor-to-fresh-diff.edn` separately records historical changes.
Fresh axiom result: exit 0, [propext, Classical.choice, Quot.sound]. The explicit
PrecisionMap and NonnegativeReal pins from the candidate are retained. Adapter
and dependency-boundary maps remain equal as EDN values.

`before-hashes.json` and `after-hashes.json` record source/receipt byte identities
and confirm unchanged positive Lean source, negative Lean fixture, Holes and
fixture EDN. `wrapper-before.clj.txt` preserves the exact original wrapper.

## Commands, cwd, outputs and exits

All top-level commands below run in `/home/joe/code/futon2`. R denotes
`holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-vfe-control-1`.
No gate output is piped; the actual exit is recorded immediately.

- Baseline: `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb`,
  exit **1**, two VFE declaration drifts: before-gate.log / before-gate.exit.
- Independent controls and fresh derivation: `bb R/control.clj`, exit **0**,
  control.log / control.exit. `protocol-commands.edn` retains every internal
  `lake env lean` argument vector, cwd, stdout, stderr and exit. Those run
  serially in `/home/joe/code/mathlib4`, with temporary Lean files and no build.
- `python3 R/gates.py` runs the following serially; `commands.json` records full
  argv/cwd/exit and each named log retains stdout/stderr:

| Wrapper mode | Exit | Evidence |
|---|---:|---|
| positive | 0 | positive.log |
| --negative-value | 0 | negative-value.log |
| --negative-type | 0 | negative-type.log |
| --negative-weakened-positive | 0 | negative-weakened-positive.log |
| --unrelated-positive-edit | 0 | unrelated-positive-edit.log |

Each command is `bb checks/variational_free_energy_witness.clj` with the listed
flag, or no flag for positive. Value mode detects the altered expected F;
type mode elaborates a #guard_msgs rejection; weakened mode now elaborates the
tautology and detects only its slice drift. Unrelated-comment mode retains a
passing receipt report.

The post-install re-attestation gate is the same baseline command: **exit 0,
zero drifts**, after-gate.log / after-gate.exit.

Lint: `clj-kondo --lint checks/variational_free_energy_witness.clj R/control.clj`,
exit 0, no warnings/errors. An initial namespace/file-name mismatch in the new
control was corrected before execution; the final lint log records the rerun.
Parens: `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
'(arxana-check-parens-cli)' -- --no-defaults <both files>`, exit 0.
Python helper parsed successfully with ast.parse before running.

## Limits

A green re-attestation gate closes this binding/control defect only. It does
not close WM-01 or admit R4. Adapter correspondence and full dependency closure
remain declared-not-derived; no runtime or empirical correspondence is claimed.
No serving action, reload, click, push or other receipt repair occurred.
Independent review remains required.
