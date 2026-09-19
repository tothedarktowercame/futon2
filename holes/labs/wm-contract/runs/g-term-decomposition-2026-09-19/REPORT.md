# AGG-single-kl-reduction — consumed G-term record

Author: codex-31. Implementation: `cd952809`.
Independent review and acceptance belong to claude-4. AGG remains open.

The scorer records the effective A kernel (after tempering), the constant C
specification and outcome universe, initial belief D, and each rollout belief
Q directly from the evaluation. Selection adds the E and F it actually
consumed. The full-loop runner copies this census to `:g-term-decomposition`
on its per-run receipt, independently of trace writing. A run with no recorded
cascade selection gets `:status :missing`, not invented neutral values. No
scoring, selection, detector, refusal, or run eligibility was changed.

Each policy has A/C/D/E/F/Q records with a value, verdict and reason. The
reductions mean: identity A, horizon-constant C (not necessarily uniform or
underived), point-mass D, E=1, consumed F=0, and Q with no observation updates.
These are term-level measurements, not proof that a changed term changes the
selected action. Function-valued, step-indexed C has no production caller and
is explicitly missing in this serialization; it is not labeled degenerate.

## Execution and warrant

Warrant: `test-registry-7c20e8379cab6701951f27cf052d332a624f5fea49f107ff20f63938102299fb`.
Checked successfully and bound to `AGG-single-kl-reduction` through
`futon3c.test-registry.validation/bind-subject!`. See `warrant.edn`,
`check-result.edn`, `binding.edn`, and the reproducible `registry.edn`.

Registered execution: **6 tests, 78 assertions, 0 failures, 0 errors, 3191 ms**.
The load closure and log are pinned by the warrant. All five changed Clojure
files passed clj-kondo (0 errors, 0 warnings) and
`futon4/dev/check-parens.el`; `git diff --check` passed.

`receipt.edn` is extracted from the registered log. Scope is explicitly
**offline-selection-replay**, using tick-001's committed VM test inputs,
through the production scorer, selector and run-record writer. No opportunity
runner invocation, actuator, shared-JVM reload, or budget click occurred.
The temporary writer output was read back and removed; this artifact retains
that read-back record inside the scope-labeled execution receipt. The writer's
historical `selectorSeam` label is unchanged; it is not a claim of live
actuation. Production stores were isolated with `with-hermetic-stores`:
repair files **200 before / 200 after**, trip files **293 / 293**.

Four policies, three scored steps each, six terms each: **24 degenerate
verdicts, zero non-degenerate verdicts**. Consumed F is **0** for all four;
computed F is **positive infinity** for all four and retained separately.
Reproduced G values (matching the recorded tick within 1e-12):

- C0-empty: 13.101074797244184
- C1-test-first: 11.434408130577516
- C2-fix-first: 11.434408130577516
- C3-fix-only: 12.101074797244184

The independent A probe calls `token-likelihood` on the replay input rates,
not a certificate verdict: the observed state has probability **1**, all
**6** single-token-flipped observations have probability **0**. The test also
checks those input rates equal every emitted A value. All six verdict
functions return `:non-degenerate` on explicit synthetic controls. Another
control checks effective A at rates 1/4 and zeta 2: the emitted false-negative
rate is **0.1**, not the untempered 0.25. Six existing selection-decision byte
comparisons pass after removing only the evidence certificate.

## Rechecked premises and findings

At dispatch HEAD `d94d6036`, the corrected packet matches the source:
individual lanes source A but joint comparison still defaults to identity;
C may be derived or a recorded no-overlap fallback but stays horizon-constant;
D is observed-belief; E defaults to 1; F is computed but only finite values
attach (selection defaults absent F to 0); Q uses rollout without conditioning.
The explicitly attached non-finite-F refusal remains intact.

No new contradiction of those corrected premises was found. The important
measurement is that computed infinite F does **not** supply a non-degenerate
consumed-F witness. The tick-001 R1 prose artifact also contains a duplicate
`:q0` key and cannot be read as EDN; the replay therefore uses the already
committed, readable VM test transcription (`tick_001_s06_r5_test.clj`, T through
spec), with its four independently recorded expected G values. The artifact
was not repaired or silently parsed permissively.
