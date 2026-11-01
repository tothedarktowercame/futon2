# L5-10-I-2 — The self-heal route accepts an environmental cause by a fresh probe, at click time (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` (≥ this packet, which includes L5-10-I `dedea1288`). Worktree only.
Authority: Joe 2026-11-01 ("medic doesn't need any special approval … go ahead") and the L5-10-I
medic disposition, which closed injury 93d40d07 live by one probe (receipt `2ddd9091…`, ledger
ordinal 147). This slice makes the machine do the same thing itself when it next happens, so a
transient substrate timeout costs one self-heal click and no human.

## Behaviour

1. In the self-heal route (`meta_self_heal_production`, the acceptance step that today proposes
   and runs the terminal-receipt acceptance), when the retained diagnosis classification is
   `:environmental-*`, the acceptance is **not** a replay of the pinned failed record. It is
   `environmental-probe/probe!` with the same inputs the medic used (injury id, cause pin,
   diagnosis pin, `:probed-by` the route's identity, `:source-revision` the serving source sha),
   followed, on `:checked`, by `environmental-probe/discharge!` under the standing authority. The
   receipt and the disposition are the ones L5-10-I already defines; no second format.
2. On a `:refused` probe (504, timeout, deadline) the route records the refusal as the
   acceptance result (`meta-self-heal/acceptance-run-refused` with the probe receipt pin) and
   the injury stays active, exactly as today, so the next click probes again. Nothing retries
   inside one click.
3. Non-environmental classifications take the existing path unchanged.
4. The click's runtime disposition names the probe (`:environmental-probe` with the receipt pin
   and status) so the run record and the L5-5a projection say what happened.

## Tests

- Fixture click with an active environmental injury and a probe port returning 200: the route
  discharges, the disposition names the probe, `effective-active-injuries` is empty afterwards,
  and the same fixture's next click takes the ordinary route (use the fixtures of
  `environmental_probe_test` and the self-heal route tests).
- Probe 504: refused acceptance, injury active, ledger bytes unchanged, probe receipt retained.
- Non-environmental diagnosis: the existing terminal-receipt acceptance path runs; probe port
  never called.
- Named self-heal + medic namespaces vs main; hermetic; data count.

## Gates

kondo, parens, no reload, no merge, no `data/` writes. Branch `codex5/l5-10-i-2`. Bell claude-2 with
sha and counts.
