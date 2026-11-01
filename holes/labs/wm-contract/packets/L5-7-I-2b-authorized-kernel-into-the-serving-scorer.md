# L5-7-I-2b — The authorized kernel reaches the serving class scorer and is certified (claude-2 → codex-6, after I-2a merges, 2026-11-01)

Base: futon2 `main` after I-2a. Authority: L5-7-D §5 and L5-7-I-2-D answer 4. Worktree only (see I-2a).

## Behaviour

1. At the composition root, `preference-registry/load!` runs once per click; absence or invalidity
   refuses before posterior selection with `:production-registries-unconfigured` / the existing
   kinds. The validated carrier (certificate, registries, derived terminal distribution) is
   threaded through the runtime options whitelist (`full_loop_runtime.clj` ~1071–1083) into
   cascade decision and family scoring.
2. `cascade-shape-g/class-emission-model` takes the terminal distribution as an explicit validated
   input: unit mass on `:ending/not-yet-evaluated` for `tau < horizon`, the authorized
   distribution at `tau = horizon`. It never consults the registry and never falls back to
   `terminal-class-preference` once the factorized route is selected. Sealed policy keys and the
   `:class-free-energy` prediction context remain as they are.
3. Canonical mode: `wm.family-canonical-g/rescore` carries the same distribution and receipt;
   `cascade-coapply-g/compare-at-horizon` checks equality against the supplied authorized
   schedule/kernel instead of reconstructing 55/35/5/5; `certify-score` verifies against the
   retained authorized kernel. The reduction label claims the ruled `compareAtHorizonN`
   specialization only when the supplied kernel is the theorem's kernel.
4. Before `decision-gate/emit!`: verify every compared candidate's retained
   `[:certificate :consumed-g :C :steps]` equals the authorized schedule (preterminal rows exactly
   `{:ending/not-yet-evaluated 1}`, terminal rows exactly the authorized distribution); refuse
   `:computed-kernel-not-consumed` otherwise; then attach the normalized carrier at
   `[:decision :selection-certificate :factorized-preference]` (retained by `persist-run-record!`).
5. Inner abstention is scored under the same C.

## Tests

- Fixture click through the runtime with the shipped registry: the tick record carries the
  carrier; `wm_run_facts` exports `factorizedPreference` and both registries; the Lean checker
  renders `some certificate` (run `wm-check-run-facts` on the export: Q9 holds).
- Registry removed → typed refusal before selection; a candidate whose consumed C differs →
  `:computed-kernel-not-consumed`; canonical rescore with the authorized kernel certifies, with
  the hardcoded kernel refuses.
- Named, outer, projector, inner-law namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex6/l5-7-i-2b`. Bell claude-2 with
sha, counts, and the Lean Q9 line from the fixture export. I-3 (real-click witness) follows merge
and the loop's reload.
