# RUN4 audit refresh attempt — blocked on upstream inputs

2026-09-09, codex-17; dispatch `invoke-1788935186607-16136-8613ed63`.

The requested current-pin audit could not be produced by the dispatched
refresher. `refresh_hole_closability.bb` reads only
`variable-situation-accounting.edn`, selects its open-hole rows, and copies
its date and authority. It does not re-derive that accounting from the live
contract. No tool or input was patched to evade this dependency.

## Executed derivation and pins

From `futon2/holes/labs/wm-contract`, ran `bb refresh_hole_closability.bb`
twice. Both exited 0 and printed:

```
hole-closability refresh: WROTE 11 rows at e239086a44147f14defc88e74a662a2cd4487e67
```

Both `audit.edn` and `AUDIT.md` were byte-identical across the two runs and
unchanged from the existing September-8 artifacts. Their authority remains:

- `:as-of "2026-09-08"`
- `:contract-git-sha "e239086a44147f14defc88e74a662a2cd4487e67"`
- SHA256 `7b41d69121ba82ef7c4bb8b9c305a756d9825418f73f3a534fb8bbc54d8cb170`

Live `mathlib4/DarkTower/WarMachine/holes-contract.json` is clean and last
committed at `0222969e1ed5181b374b9d04bd0031ed5ac20a08`. Its bytes have SHA256
`444b9493e3a9b603b8027027b479c807eb1d377fab18e3f9f10859349b3b47ce`.
Its embedded `source.git-sha` is
`fcd1261c303c2beca08a6812eba4a7ce2e83d722`: the readiness check compares this
Holes-source authority, not the later contract-file emission commit. Neither
live identity matches the accounting/audit authority.

## Fresh readiness verdict

`bb run4_readiness.bb --summary` exited 0. Per its header, a blocked verdict
is an answer, not a tool error. Exact output:

```
RUN4 certificates        GREEN
RUN4 definitions-intact  GREEN
RUN4 wiring-pin          GREEN
RUN4 run-pins            GREEN
RUN4 regenerates         BLOCKED-ON [1]
RUN4 lean-probe          BLOCKED-ON [1]
RUN4 hole-open           GREEN
RUN4 closability-audit   BLOCKED-ON [3]
RUN4 invalidators        GREEN
VERDICT: BLOCKED-ON [regenerates lean-probe closability-audit]
  - regenerates: the committed manifest does not regenerate byte-identically: 00-source.edn 04-controls.edn
  - lean-probe: the axiom probe was taken at a different Holes.lean than the live one (:stale) -- re-run --probe
  - closability-audit: the closability audit was derived at contract e239086 (:as-of 2026-09-08) while the live authority is fcd1261 -- its readiness typings are about a different contract
  - closability-audit: the closability audit types wmRunConformsToWiring :readiness :witnessed-and-held-open (:closability :run-gated) -- RUN4's row names this hole
  - closability-audit: the closability audit types enactedEqualsSelectedWhenRankOneGated :readiness :not-ready (:closability :run-gated) -- RUN4's row names this hole
```

## Delta from the September-3 audit

Compared the generated rows to `audit.edn` at futon2 `f67a23fc`, dated
2026-09-03 and pinned to `6de47bd0503dd4e6090c420ca32d8e7474438d7e`.
These are changes already present in the September-8 input; this refresh
introduced no new typing or closure:

- Rows: 16 to 11. Removed from the open-hole audit: `findF1Containment`,
  `findF2Receipted`, `findF3NonSelfCertifying`, `findF4Falsifiable`, and
  `nonDegenerateAblationLaw`. No rows added.
- `wmRunConformsToWiring`: readiness moved from `:not-ready` to
  `:witnessed-and-held-open`; closability stayed `:run-gated`.
- Both closability and readiness held for the other ten surviving rows:
  `C`, `Strategic mission selection`, `dirichletAccumulationImportAbsent`,
  `enactedEqualsSelectedWhenRankOneGated`, `find`, `organise`,
  `policyPosteriorImportsPolicyF`, `policyPrecisionIsGammaFromBeta`,
  `preferenceStackLiveRecorded`, and `wmRunsOnce`.
- `enactedEqualsSelectedWhenRankOneGated` remains `:run-gated / :not-ready`.
  Neither of RUN4's two run-gated holes was retyped to force readiness.

## Handoff and scope

Current-pin acceptance is **blocked**, not achieved. The accounting producer
must first supply reviewed/current accounting; merely rerunning the audit
projection cannot advance its pin. The fresh readiness result also exposes
manifest regeneration drift and a stale axiom probe. Route those prerequisites
through their owners; none was repaired in this packet.

Only this execution report is committed. No worklist, dependency frontier,
workflow report, accounting input, registry, or mathlib4 file was edited.
The running build-loop's pre-existing work is left alone. No Clojure/EDN
change: clj-kondo and check-parens are not applicable to this Markdown-only
diff. `git diff --check` passed.
