# WM-01-positive-binding-1 — predictive receipt re-attestation

Author: codex-2. Independent reviewer: claude-3 (review and reviewer rerun
pending; these are author execution results). Authority: p4ng
`wm-walkthroughs/build-loop/claude-3/WM-01-positive-binding-1.md`, SHA-256
`20fc0354eb852cc30399ba7ecd182298a4e451838570f74d94369aca4cfbebec`.

## Result and meaning

The successor passes the real positive receipt validator. The old receipt fails
only `:positive-source-drift`; both validation maps are retained. No adapter
mapping, fixture value, theorem statement, or boundary declaration changed.

Reading the repair diff establishes the precise distinction: the old witness
returned mass one even off support. The repaired witness returns one only for
inspect/evidence/clear and repair/evidence/fixed, and zero elsewhere. Its
singleton supports and their named masses are unchanged. Thus the hand-derived
fixture's two deterministic rows and the allPolicyRowsNormalised statement
remain the same subject; this is not a claim that the old and new mass functions
agree off support. The repaired carrier additionally requires duplicate-free
support and zero mass off support, and the witness discharges both obligations.
The theorem still follows by `predictive.normalised p`, without extra premises.

The adapter strings `inductive Policy` and `| .inspect => [clear]` still occur
literally in the retained slices. The real validator confirms their occurrence,
expected shapes and fixture values. A direct ProbabilityKernel slice was added
using the existing source-basis mechanism, alongside the unchanged
PredictiveOutcomeKernel alias, to expose the repaired carrier dependency.
`repair.diff` and `pre-repair-*.txt` / `repaired-*.txt` preserve the inspected
carrier, alias, witness and theorem evidence (mathlib4 480a666ad2^ → 480a666ad2).

## Bytes, derivations and slice mapping

Predecessor bytes: `predecessor.edn`, SHA-256
`b1574cbfdd2169ad484a6987d800e950eb3d25038838e762bd4f7e165a1fe45c`.
Successor bytes: `successor.edn` (byte-equal to the canonical receipt), SHA-256
`13dfaa1c9afd865df26f1085b904013ad2fc382511a3549f66f212a5c943ffc1`.

The old recorded derivation is `old-recorded-derivation.edn`. It is historical
evidence, not a claimed rerun of old Lean sources. `fresh-predecessor-derivation.edn`
is a fresh basis-record invocation on that predecessor at repaired sources.
`successor.edn` is a second fresh basis-record invocation with the carrier slice
included and a new timestamp, 2026-09-16T00:04:25.753158436Z.
`derivation-diff.edn` retains clojure.data/diff's old-only, new-only, and common
maps, including every unchanged declaration hash. Fresh execution produced the
same toolchain, fixture hash and axiom result as the historical record; none was
substituted for fresh measurement.

Changed predictive slice:

- Old: `5c6f447de29814d89de8106b2c4fa3a4bb1ba407bbdf8defacee564ffe17d011`
- New: `24e92e8657312646cf9c355bf4a46f98c9c7060765153acd4115b9e7843b9350`
- Newly explicit ProbabilityKernel dependency:
  `5580409932915e7c077e980eaf4f8551246740d63eda8104d1207dcb39bd76bc`.
  It was absent from the predecessor basis; the alias hash is unchanged.

`before-hashes.json` and `after-hashes.json` confirm unchanged bytes for the
fixture, both negative fixtures, witness and Holes sources. Fixture hash:
`667873b464525ad9a14be4fbc4cf0d28ba55d379b5d9381bbe506264603eff48`.
Witness hash: `b38429046b74af1ed580c5fbfddda984d49d81f8705b5568f74a783494c05632`.
`identity.json` records repository HEADs; `artifact-hashes.json` hashes the
execution artifacts before this narrative was written.

## Commands and actual exits

`commands.json` records exact argument vectors, cwd and exits for the serial
commands in `gates.py`. Each command's stdout/stderr and immediate exit status
are retained in `<name>.log` and `<name>.exit`. No gate output was piped.

Additional commands, cwd `/home/joe/code/futon2`:

- `bb holes/labs/wm-contract/positive_receipt_reattestation_check.bb` before
  replacement: exit **1**, `before-gate.log`, 27 UNATTESTED-DRIFT declarations.
- `bb holes/labs/wm-contract/runs/wm-build-loop-2026-09-15/wm-01-positive-binding-1/reattest.clj`:
  exit **0**, `reattest.log`. Calls the existing basis-record twice and validate
  four times (old, successor, isolated good copy, isolated mutated copy), each
  using fresh `lake env lean` probes in the receipt's declared mathlib4 cwd.
  Internal temporary probes run `#print axioms` via the unchanged API; their
  result maps are retained. A separate raw-output axiom probe is also retained.

Serial gate results:

| Control | Exit | Evidence |
|---|---:|---|
| `lake env lean DarkTower/WarMachine/PredictiveOutcomeKernelWitness.lean` | 0 | positive-theorem.log |
| `lake env lean <run-dir>/axioms.lean` | 0 | axioms.log |
| `bb checks/predictive_outcome_kernel_witness.clj` | 0 | positive-wrapper.log |
| same wrapper `--negative-unconditional` | 0 | negative-unconditional.log |
| same wrapper `--negative-softmax` | 0 | negative-softmax.log |
| re-attestation gate after replacement | 1 | after-gate.log: 26 drifts |
| clj-kondo on reattest.clj | 0 | lint.log: 0 errors, 0 warnings |
| emacs check-parens, file after `-- --no-defaults` | 0 | parens.log: OK |

The theorem axiom probe reports only propext, Classical.choice and Quot.sound.
Both negative Lean fixtures elaborate successfully because their #guard_msgs
assert the expected type rejection. Their exit zero is the documented negative
success convention, not acceptance of the wrong kernel type.

`mutation-control.edn`: an isolated temp-root copy of the receipt, fixture and
source files passes first. Replacing only its predictive slice hash by 64 zeros
then yields exactly `[:positive-source-drift]` through the real validator. The
canonical receipt and Lean sources are never mutated by this control.

No build, serving reload, click, push or canonical Lean source edit occurred.
The existing mathlib4 package directory was retained; only lean elaboration and
version probes were requested, with no Lake build or output-artifact option.

## Residual and limits

The before/after gate count is **27 → 26**, both exit 1. The predictive receipt
is gone from the drift list; the other 26 drifts remain outside scope. No
known-stale registration was added. This repairs one source-drift gate, not the
whole U71 gate, WM-01, or the blocked R4 admission.

The fixture adapter remains declared-not-derived; this does not formally verify
the Clojure adapter. The dependency closure remains declared-not-derived; the
added carrier pin does not establish complete transitive closure. This
hand-derived deterministic witness does not prove machine-derived Q or empirical
adequacy. No meaning conflict was found within the declared fixture and theorem.
Independent review and reviewer rerun are still required before acceptance.
