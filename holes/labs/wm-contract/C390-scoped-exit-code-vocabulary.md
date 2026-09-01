# C390 — scoped exit-code vocabulary

The executable convention is now adjacent to the checks in
`checks/EXIT-CODE-SCOPES.md`. It states that exit codes are scoped to their
producer, records both live meanings of exit 3, and names GNU Make and the
bounded test service as lossy boundaries.

`checks/exit_code_scope_check.clj` derives the report-only population from the
workspace gate's `:expected-exits` declarations. It rejects a report-only
command named directly by the Makefile or bounded launcher, requires the exact
local set `#{0 3}`, and requires the status program to retain its separate
`DECISION-DUE-3` declaration. It is part of the workspace gate, with a gate
control that injects a direct Make invocation and proves rejection.

Focused results:

- positive: exit 0, two declared report-only commands, zero findings;
- lossy-boundary control: exit 0 after detecting
  `:report-only-crosses-make`;
- clj-kondo: zero errors and warnings;
- inventory: `{:exit 0, :unknown (), :missing ()}`.

No fourth nonzero code was added. Report-only remains a gate-local meaning;
status exit 3 remains `DECISION-DUE`. Structured receipt data is still required
if report-only state ever needs to cross Make or the bounded service directly.
