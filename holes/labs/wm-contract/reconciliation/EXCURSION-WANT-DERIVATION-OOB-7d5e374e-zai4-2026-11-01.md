# EXCURSION-TARGET-FALSIFIER-AND-REVISION — out-of-band medic evidence for injury 7d5e374e (zai-4, 2026-11-01)

Injury `injury/7d5e374e7297e8814b21cb5a1b4257839496bc62` (ledger ordinal 143,
`:reviewer-falsifier-failed` at `:reviewer-wait`, click `wm-click-3a9ee0a1`,
run `2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324`). Same method as
c309d796/a67d3e49: prepare + compute only, not admitted, no ledger write, no
work-order change, no click/reload/merge.

- **Root:** `~/notes/wm-next/criteria-completeness/run001/7d5e374e-oob/`
- **:fixed-by:** `fdc151e4a70ed7394e42e48fa7e25fc11453d832` (on-the-fly want
  derivation for excursions; merged to main via ccdb566e3), reachable on main
  from compute!'s base — ancestry checked by compute! itself.
- **:historical-pin:** the ledger's own cause-record-pin for ordinal 143,
  asserted equal in prepare.clj before anything was retained:
  `data/wm-runs/tick-run-record-2026-11-01-ac5bc477-9e34-4249-a692-f13a9f22a324.edn`
  sha256 `7dbf1614668d9f8c565562447c5d336b9424e6bb93d42335fde1ba1c32e7a9ac`
  (matches the live ledger entry and the live file bytes).
- **:failure-kind / :failure-stage:** from that record — `:reviewer-falsifier-failed`
  / `:reviewer-wait` (asserted against both the ledger entry and the record's
  own `:failure` map inside compute!).
- **Acceptance test:** `futon2.aif.meta-acceptance-injury-7d5e374e-excursion-wants-test`
  (id `injury-7d5e374e-excursion-wants-derived`), fixtures = the real ac5bc477
  selected-action shape (cascade candidate, empty want set, as the run record's
  `:selected-wants []` attests) and the real futon6 source bytes
  (sha256 `6863756378922c1aa289e75c0c750f04bc66e797e04dae721f3c9e39b1afb663`,
  matching the reading's `:source-sha256`), retained in-tree by the fix.
  Written against only vars that exist on both trees so the counterfactual
  fails by assertion, never by compilation error.

## Compute result (`:status :checked`)

- **current** (canonical repo, fix present): 3 tests / 19 assertions,
  **0 failures, 0 errors**; run log pin sha256
  `9973dafa654bb5a55548e20d62d695bf38a2bd5e1c0b3e72bc1ac3bea7760dd4`.
- **counterfactual** (fdc151e4a's src diff reverse-applied `--3way` in
  compute!'s throwaway worktree
  `~/worktrees/futon2/out-of-band/0b7d8cc7-0b78-48a5-aafa-6e48723272f7`):
  3 tests / 11 assertions, **8 failures, 0 errors**; run log pin sha256
  `eadc2df278c2bdd52ba9b13bc9d7a7a3aad55dcc5306af09c4f2d304301b0abb`.
  Pre-fix behaviour reproduced: `source-contract` returns nil for the E-
  target, `attach` no-ops, `require-before-author!` passes the empty-want
  excursion through — the injury's own signature, failing as assertions.
- **patch-sha256** `d9ccf7d73d36329bf2d3d8b47c402432bfc9e301e20e9a4bb32a7d8ea78e4396`;
  **acceptance-source-sha256** `03b2c81bd8419d02cd69988b5d90ae6687b4ed3478405b2962a49a10ad3895f9`.
- **Artifact pin:** `7d5e374e-oob/out-of-band/5c63580633400d495d71acd1f84ec1b7aa3c48efeea74ed046451514b92606fc.edn`
  sha256 `5c63580633400d495d71acd1f84ec1b7aa3c48efeea74ed046451514b92606fc`.

Both run logs, the artifact and the result receipt are retained under the
root above (`artifact.edn`, `result.edn`, `logs/…`, `out-of-band/…`). The
receipt is computed, not admitted: admission is claude-1's call through the
normal route.
