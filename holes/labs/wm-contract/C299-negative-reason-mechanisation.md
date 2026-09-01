# C299 — mechanically preserving negative-control reasons (2026-09-01)

## Finding

Lean's `--json` output is machine-readable but does not provide a sufficiently
stable error taxonomy for this use.  In Lean 4.31.0-rc1 an ordinary application
type mismatch is emitted with `kind: "[anonymous]"`; rendered message text
contains the useful distinction.  A registry matcher over that text would be
brittle and would recreate the stale-annotation problem.

Lean can nevertheless enforce the reason at the negative source boundary.
`#guard_msgs` compares a command's actual diagnostics with diagnostics recorded
beside that command.  A negative fixture using a guarded command is itself
expected to elaborate successfully.  Therefore:

- the intended mismatch produces the recorded diagnostic and the fixture exits
  zero;
- a missing import, syntax error, renamed module, additional error, or changed
  diagnostic makes the fixture fail;
- the outer checker no longer treats every nonzero Lean exit as evidence that
  the mutation was rejected correctly.

The exact message comparison is intentionally sensitive to Lean-version wording:
an upgrade can make the gate red, but cannot silently bless a different failure.

## Proposed record

Each Lean-negative control should declare, rather than infer, metadata of this
shape in the witness registry:

```edn
:expected-rejection
{:mode :lean-guard-msgs
 :fixture "DarkTower/WarMachine/VariationalFreeEnergyNegative.lean"
 :claim :distinct-semantic-types
 :actual-type "ExpectedFreeEnergyValue"
 :expected-type "VariationalFreeEnergyValue"}
```

The fixture is the executable authority for the exact diagnostic; the registry
fields identify the semantic claim for audit and reporting.  Non-Lean fixture
mutations should analogously record the named predicate whose false result is
expected, rather than merely a nonzero process exit.

## Re-audit cadence

Until guarded fixtures are migrated, run the complete 46-mutation audit and
inspect direct diagnostics:

1. before each publish/release candidate;
2. after every Lean toolchain upgrade;
3. after imports, module names, or witness files move;
4. after a change to `Holes.lean` or a negative fixture's dependency surface.

The C294 full execution took roughly one minute locally.  Once guarded fixtures
are installed, their execution belongs in the focused gate on every change;
manual diagnostic review remains appropriate at toolchain upgrades and release
boundaries.

This packet establishes feasibility only.  It does not migrate or weaken any
control, add a binding, or alter the 31/33 glossary coverage.
