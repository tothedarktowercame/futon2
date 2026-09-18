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

---

## Owner ruling on the two deferred decisions — claude-4, 2026-09-18

Both settled here so the proposal is complete. Recording the reasoning, not
just the choice.

**(a) Where it lives: a SEPARATE SEAM, not inside `pointer_check`.**

`pointer_check` is a publish gate — a refusal there stops the paper. This
check's own proposal says its name extraction can be fooled, and cites the
FUNDAMENTALS join's comment about "Pi -- a machine policy carrier" landing on
the wrong node. Folding an unmeasured check into a publish gate means its
false positives block publishes, and the pressure that creates is to weaken
the check rather than fix the citation. Report it as its own seam first, on
the F6-top-level-predicate precedent; promote it into the gate once its
false-positive rate has been measured on the real corpus rather than assumed.

That is the same order today's work kept arriving at from the other side: five
separate defects came from spans that resolved without containing their
subject, and every one was found by reading, because no gate could see them.
A check that reports is strictly better than no check; a check that blocks on
a guess is worse than both.

**(b) Unnamed-prose citations: a CENSUS ROW, not a refusal.**

A refusal would fire across a large fraction of the corpus on day one, and the
cheapest way to clear it would be to DELETE the prose context around a pointer
rather than name the declaration — exactly the wrong incentive, since that
prose is what makes a citation checkable by a human. A census row makes the
unadjudicated set visible and countable, which the proposal already argues for
("the empty-adjudication set is itself a finding"). If the census shows the
unnamed set is small, promoting it to a refusal later is one line.

**NOT BUILT, and not dispatched, on purpose.** Joe's standing instruction
(2026-09-15) is that WM work maps to an unchecked CHECKLIST-fundamentals item
and says how it clears it, with no new plans. This proposal maps to no
checklist item today. The evidence for building it is strong — of ~99 distinct
Holes.lean spans cited across the four lab registries, 95 were stale, and the
defects that reached registries today include a true pointer rewritten as
false — but "strong evidence" is not a checklist mapping, and inventing one
would be the maverick plan the instruction is against. It waits for Joe to
say whether it earns a slot.

