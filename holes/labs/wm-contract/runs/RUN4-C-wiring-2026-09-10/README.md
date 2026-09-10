# RUN4 config-to-scorer wiring

2026-09-10, Codex-17, at Joe's explicit request. Implemented, tested in separate
processes; no live tick, lock, shared JVM load or data/ write.

The proposed run sheet now contains an executable :c-fold stanza: enable flag,
named seed and constant adapter, and SHA256-pinned EDN resources. Relative paths
resolve beside the sheet. The original descriptive :flags entries remain as
historical rationale. The sheet remains PROPOSED-FOR-REVIEW; this change does
not adopt its other settings or authorize a run.

`src/futon2/aif/c_fold_config.clj` reads FUTON_WM_RUN_CONFIG only if the caller
has not explicitly supplied :ruled-outcome-c-enabled?. Explicit false wins and
reads nothing. Enabled configuration verifies resource bytes, matches the seed
against the loaded canonical seeded-c value, and constructs the existing
constant-checkpoint-kernel. No fitting against a changing corpus occurs during
a tick. Missing sources, wrong pins, unknown selectors, malformed enabled flags,
and the legacy prose-only fold configuration refuse with typed reasons.

The seed resource was emitted from futon2.aif.ruled-outcome-c/seeded-c. The kernel
resource was emitted through checks.disposition-kernel/read-kernel over its
existing default cohort ledger; the fitted artifact carries that ledger's
source SHA. Runtime does not depend on the checks namespace or re-read the
historical cohort: it consumes the frozen fitted artifact at the config pin.

The existing judge configured-fold-efe-opts boundary now materializes this
configuration before efe/rank-actions. Each configured score carries the seed
and kernel pins, fitted source, enabled flag, and :efe-disposition-risk boundary.
Trace serialization preserves that provenance plus :G-ruled-outcome-c and
:predicted-disposition-risk. These additions are present-only for configured
scores, preserving the old trace shape otherwise.

## Validation

- New config tests: 3 tests / 16 assertions, zero failures/errors. Drive the
  real stepped diagnostic-judge-opts, the judge's fold-options helper, real
  compute-efe, and real trace projection. They do not execute a complete judge
  scan or live tick. Real pinned resources yield log 2; absent/false scorer
  output matches legacy pr-str bytes; invalid pins/selectors/sources refuse.
- Existing efe, policy, trace, disposition-risk, and run-tick-once suites:
  153 tests / 661 assertions, zero failures/errors.
- Re-executed preference-risk receipt: 24 mass equalities, deliberately changed
  seed rejected by Lean, 10 runtime checks PASS. Generated artifacts accompany
  this note unchanged. Generator's :as-of remains its historical 2026-09-09;
  actual invocation was 2026-09-10.
- run4_readiness summary: all nine lines GREEN, `VERDICT: READY`.
- check-parens PASS. clj-kondo: zero errors, one existing unused-private-var
  warning for efe/ambiguity. Re-running lint on HEAD's pre-edit efe.clj in a
  matching namespace path reproduces the same warning. No suppression or gate
  alteration. New resolver/test and other touched Clojure files lint 0/0.
- git diff --check PASS.

The generated certificate's pins include the changed scorer bytes; it remains
finite correspondence evidence, not a live-run certificate or a universal
refinement proof. Constant risk cannot distinguish policies. Institutional
selection, the observation bridge, and organise's remaining O4 exercise are
not claimed by this implementation.

To activate this capability on the established stepped entry point, supply
FUTON_WM_RUN_CONFIG with the absolute path to the updated run sheet. Its beta
and recording environment requirements remain separate and must also be
satisfied for the complete proposed RUN4 configuration.
