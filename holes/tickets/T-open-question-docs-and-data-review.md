# Should documentation-only and data-only changes be reviewed like code?

**Status:** OPEN QUESTION. This is not a work order; nothing is to be fixed
until the question is answered.

## The question

The full-loop runner requires evidence that a review actually ran (tool-use
events counted from the reviewer's own job stream) only when the reviewed
files include code. `review-execution-gate` in
`src/futon2/aif/task_execution_evidence.clj` (at `62e29c87b3c1`) sets
`required?` from a code-file pattern; a review of documentation or data files
passes without execution evidence. The separate public validator
`independent-review-evidence` always requires execution.

The conference text of R9 (No Self-Certification) says review execution is
corroborated "before any review counts". The current text says the
corroboration is "conditional on changed code files", which matches the code.

Joe (2026-10-18): it is plausibly fine for documentation-only, or data-only,
changes not to be reviewed in the same way as code changes; but the rule
should be stated clearly and the handling thought through.

Things to settle:

1. What counts as review of a document or a data file, if not execution? For
   example: reading it against the code it describes, checking its references
   resolve, or running a validator over the data.
2. Do data-only changes exist in practice, and do any of them change
   behaviour (configuration, registries, preference tables)? Those may need
   the code standard.
3. Where is the rule stated: in R9's text, in the gate's docstring, or both?
4. Should the two validators (adjudication gate and public validator) share
   one rule, since two copies of one contract is the defect class behind the
   10-17 clicks?

## Provenance

Raised by claude-4 while annotating R9 for the plop2026 docbook
(`p4ng/plop-2026-code-annotations.json`), 2026-10-18.
