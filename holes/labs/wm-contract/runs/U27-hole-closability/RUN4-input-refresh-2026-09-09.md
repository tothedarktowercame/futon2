# RUN4 inputs regenerated — two substantive audit blockers remain

2026-09-09, codex-17; dispatch `invoke-1788935392880-16142-a15b7544`.
Follow-up to `a4fc74ed`. No generated artifact was hand-edited.

## Derivations and determinism

1. From futon2, `bb scripts/generate_variable_situation_accounting.bb`:
   generated 133 rows, all 133 pointers resolving, including 11 open holes.
   `--negative-untyped` rejected `plantedUntypedHole` with
   `:error :untyped-open-hole`; `--check` passed. A second generation was
   byte-identical to the first.
2. From the lab, `bb run4_readiness.bb --probe`, twice: both exited 0;
   build `:ok`, zero sorries, all six theorems carry no axioms. The generated
   `axiom-probe.edn` was byte-identical across the two probes.
3. `bb run4_prereg_transcribe.bb`, twice: PASS, two certificates, four
   assertions, five authorities, four invalidators (one touching).
   All six emitted files were byte-identical across the two derivations.
   Only `00-source.edn` and `04-controls.edn` changed relative to the prior
   committed manifest, each solely replacing the Holes.lean SHA256.
   Assertions, authorities, invalidators and generated Lean are unchanged.
4. `bb refresh_hole_closability.bb`, twice: 11 rows at the current contract
   authority; both `audit.edn` and `AUDIT.md` were byte-identical across runs.
5. `bb run4_readiness.bb --summary`: exit 0, exact verdict below.

## Pins, date limitation, and typing delta

The generated accounting and audit now both carry contract authority
`fcd1261c303c2beca08a6812eba4a7ce2e83d722`, matching the live contract's
embedded `source.git-sha`. Their contract SHA256 is
`444b9493e3a9b603b8027027b479c807eb1d377fab18e3f9f10859349b3b47ce`.
The contract-file emission commit is separately
`0222969e1ed5181b374b9d04bd0031ed5ac20a08`.
The probe and preregistration now carry Holes.lean SHA256
`fc8a6a9906a6d334e838a7c598c220a4ac686d835505a09b0239764ac08d042d`.

**Date limitation:** `generate_variable_situation_accounting.bb:892` hard-codes
`:as-of "2026-09-08"`. Both generated artifacts therefore retain that date.
This is a fresh derivation at the current authority, but not a freshly dated
artifact. No generator or generated date was patched to claim otherwise.

Compared with the September-8 audit, the complete name-to-closability/readiness
mapping is identical for all 11 rows; no rows were added or removed.
`wmRunConformsToWiring` remains `:run-gated / :witnessed-and-held-open`;
`enactedEqualsSelectedWhenRankOneGated` remains `:run-gated / :not-ready`.
The accounting/audit changes carry current authority and refreshed source
pointers, not new readiness decisions.

## Final readiness output

```
RUN4 certificates        GREEN
RUN4 definitions-intact  GREEN
RUN4 wiring-pin          GREEN
RUN4 run-pins            GREEN
RUN4 regenerates         GREEN
RUN4 lean-probe          GREEN
RUN4 hole-open           GREEN
RUN4 closability-audit   BLOCKED-ON [2]
RUN4 invalidators        GREEN
VERDICT: BLOCKED-ON [closability-audit]
  - closability-audit: the closability audit types wmRunConformsToWiring :readiness :witnessed-and-held-open (:closability :run-gated) -- RUN4's row names this hole
  - closability-audit: the closability audit types enactedEqualsSelectedWhenRankOneGated :readiness :not-ready (:closability :run-gated) -- RUN4's row names this hole
```

The regeneration, probe-staleness and audit-authority blockers are cleared.
The two remaining readiness findings are reported as-is. This packet neither
accepts a run nor changes the interpretation of those findings.

## Validation and scope

clj-kondo on all five changed EDN files: 0 errors, 0 warnings.
check-parens on those files: OK. `git diff --check` passed.
Only generated inputs/audit artifacts and this note are committed. No ledger,
dependency frontier, registry, generator, or mathlib4 source was edited.
Pre-existing build-loop and other-agent changes are excluded from the commit.
