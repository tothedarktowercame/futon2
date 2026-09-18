# PROPOSAL — declaration-in-span checking (design, not built)

2026-09-18, zai-35, from the citation-layer repair (futon2 `9d5acd4f`).
Companion findings: `runs/E07-fundamentals-cap-2026-09-18/FINDINGS.md`.

## The gap

`pointer_check.bb` resolves a pointer by asking "does the span fall inside
the file?" A span that stays in range while the code under it moves is
invisible to it — and to `symbol_concordance_check.bb` and
`convergence_check.bb`, which inherit the same resolution rule. Today's
repair found **136 citation lines across four lab registries** in exactly
that state: every span resolved, none contained its subject. Holes.lean is
long enough that essentially every stale citation into it stays in range;
the drift bands were +4 to +290 lines.

## The proposed check

For every pointer whose citing text NAMES a declaration (the registries
almost always do — `:at` fields pair with `:read` prose or `:name`), ask the
question the repair asked by hand:

1. Extract the cited span from the target file.
2. Determine the pointer's SUBJECT name from the citation's own context
   (`:name` field where present; else the declaration named in the adjacent
   prose — the same `abbrev|structure|def|…` extraction `gen_rnode_dossiers.py`
   already uses for the FUNDAMENTALS join).
3. PASS iff a declaration with that name is INSIDE the cited span, or the
   span's first non-blank line is provably the named thing's continuation.
4. REFUSE typedly otherwise, naming the subject, the cited span, and the
   declaration's actual current span (so the repair is one edit, not an
   investigation).

## Deliberate limitations, stated

- **Prose-only citations** (no name) degrade to today's behaviour; the check
  must say WHICH citations it could not adjudicate rather than passing them
  silently — the empty-adjudication set is itself a finding.
- **Name extraction can be fooled** (the FUNDAMENTALS join's own comment
  records "Pi -- a machine policy carrier" landing on the wrong node). The
  check should report its subject guess in the refusal so a human sees a
  wrong guess immediately.
- **Quotations of old locations** (a `:citations-re-read` record saying
  "was efe.clj:37-61") must be exempt by marker, not by luck: exempt spans
  under an explicit `:was`-style key.
- It cannot detect a pointer whose subject was DELETED rather than moved;
  that is the empty-span case pointer_check already refuses.

## Why propose, not build

Two design decisions are not mine to make alone: (a) whether the check lives
in pointer_check (refusing the whole file on any unverified named citation)
or reports as a separate seam like the F6-top-level predicate; (b) whether
`:at` fields whose prose names NO declaration become a refusal or a census
row. Both change what "0 unresolved" means, so they need an owner ruling
before the checker exists.
