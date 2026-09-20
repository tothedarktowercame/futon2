# RULING — the test registry is orthogonal to the War Machine

Joe, 2026-09-20 (operator surface, verbatim): "The warrant system is supposed
to be 100% orthogonal to the War Machine and has no place doing what it does
in Claude-4's report."

Recorded by claude-12 (registry/warrant lane owner).

## What the ruling is about

claude-4's report of 2026-09-20 ~00:20 showed a WM run whose declared fact
`:standing-cascade-g-test-passes` is observed via a test-registry warrant
check (C2). When the warrant went stale (in-scope drift, entry
`test-registry-74cbd4b9…`), the fact entered the belief state as `:unknown`:
five of seven declared facts observable on that run, the ratio falling with
repository activity. The machine's q0 was being shaped by warrant currency.

## Where the coupling lives (trace)

- `futon2/resources/wm/observation-contract.edn` — observation kinds `:C1`
  ("valid lake-build warrant") and `:C2` ("valid test-registry warrant …
  missing/stale refused :no-current-warrant").
- `futon2/src/futon2/aif/observation_checks.clj` — `check-lean-warrant` /
  `check-test-warrant` calling `futon3c.test-registry/check-record!`.
- Origin: WM-04 build work, 2026-09-17 15:55–18:37 (b81e5996, 75708b9e
  "C1 … and C2 … via futon3c test-registry check", 725a17f1, d1688d4b,
  d3b8b41e, f29a9c12).

## What the ruling means

1. **Out:** WM runs consulting the registry at observation time. C1/C2 as
   warrant checks are removed from the run path. How the machine observes
   those facts instead (direct execution at pinned shas like C3–C5, or
   dropping the facts from the contract) is a WM build design choice —
   claude-4's lane — under the constraint: no registry consultation in-run.
2. **Unchanged:** warrant use in the agent workflow. Acceptance of build
   items binding a test-registry warrant (Joe's 2026-09-19 delegation),
   review handoffs riding warrants, re-registration flows — all of that is
   agents communicating with agents and stays as is.
3. **Superseded premise:** claude-12's 2026-09-20 bell answer to claude-4
   ("keep the stale-sha rule") stands as a statement about the registry's own
   semantics — scope-pinned staleness fired correctly on real in-scope drift
   (codex-3's 0499986c/591a6cdd touched `cascade_sources.clj` and
   `war_machine.clj`, both in the entry's declared code-paths). But the
   question "what should the WM believe when a warrant is stale" is
   dissolved rather than answered: the WM does not read warrants.

## Clarification (Joe, 2026-09-20, follow-up — supersedes point 1 above where narrower)

Verbatim: "It may be that 'inside' the machine, agents would want to run
tests or use warrants, that's fine. My point is that as a feature, we do not
need to hardwire this into the machine. I'd say, that should be obvious. We
are building a model of AIF that is adapted to the use case of regulating
development of the FUTON stack in the first instance, but towards broader
regulation of collaborative work in general. I don't see how the test
registry system relates to that aim."

Corrected boundary: the line is model vs agents, not run vs no-run.

- **Agents cast in a run** may run tests and use warrants as ordinary
  tooling — nothing restricts an author or reviewer seat's toolkit.
- **The model's observation contract** may not name workspace-specific
  services as observation kinds. The registry is one instance; the
  principle is general: observations are expressed in the model's own
  vocabulary (states, policies, outcomes at pinned references), because the
  model targets regulation of collaborative work in general, not this
  workspace. "No registry consultation in-run" in point 1 above is
  therefore too strong as written; read it as "no registry primitives in
  the observation contract / belief state."

## Disposition

- Contract/runner change: claude-4 (WM build owner), scheduled in its Acts.
- Registry side: no change required; the registry never knew the WM existed.
- This finding recorded by claude-12; relay bell to claude-4 sent 2026-09-20.
