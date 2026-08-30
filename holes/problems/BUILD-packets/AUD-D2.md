# AUD-D2 — lint for I_absent_is_loud (silent-missing file reads)

Owner: claude-15. Builder: codex-5 (you did AUD-D1 — this is its follow-up). Mode: work. One file, one behaviour.

## Context
AUD-D1 (futon2 `d1997fc`) found `war_machine.clj:2075,2089,2150,2211` and `futon0/.../joe_hud.clj:427-428`
reading `futon5a/data/stack-logic-model.edn` and `alignment.edn` — files that never existed (no commit
anywhere) — through `read-edn-file` (`war_machine.clj:482-486`), which returns `nil` for BOTH a missing file
and a parse failure, under `when-let`. Three report sections have rendered as nothing since May with no error.
Joe: "that rule definitely needs to be enforced." The rule is now an invariant:

    I_absent_is_loud : every read of a named input file by an instrument, report or loop REPORTS absence or
                       unparseability (fail closed, or explicit `:missing <path>` / `:unreadable <path> <cause>`
                       in the output) and never renders it as an empty result. Optional inputs are declared
                       optional AT THE READ SITE, and the declaration is what the lint reads.
    falsifier        : a read site where nil/empty from a missing/unparseable file flows into the same branch
                       as "the file said nothing".

Full text: `futon4/holes/delivery-lifecycle.md` §0.7 (second invariant); `futon2/holes/problems/PREREG-war-machine.md` §4.

## Goal (DISCOVERY + instrument; no fixes to call sites in this packet)
Write `futon2/checks/absent_is_loud_lint.clj` (bb or clojure -M, your call; say which) that:
1. **Helper census.** Over the scoped sources — `futon2/src/**/*.clj`, `futon2/scripts/**/*.clj`,
   `futon2/checks/*.clj`, `futon3/checks/*.clj`, `futon0/scripts/**/*.clj`, `futon3a/src/**/*.clj` — finds every
   file-reading helper (a `defn` whose body contains `slurp`/`io/reader`/`read-string`/`edn/read`/`json/parse*`
   over a path arg) and classifies it: `:loud` (throws or returns a tagged `:missing`/`:unreadable`), `:silent`
   (returns nil/empty/default on absence or exception), `:declared-optional` (name or docstring says optional).
2. **Call-site census.** For every `:silent` helper, every call site, and whether the result flows through
   `when-let`/`if-let`/`some->`/`or`/`when`/`(catch _ nil)` before use. Each row: file:line, helper, path
   expression (literal if literal), guard form, and — when the path is a literal or a literal joined to a
   known root — whether the file EXISTS now (`stat`), so the table separates "silent but present" from
   "silent and absent".
3. **Verdict line.** Counts: helpers total/loud/silent/declared-optional; call sites silent/absent-now.
   Exit non-zero when any `:silent`+absent-now site exists (this is the enforcement; it will be non-zero
   today — that is the correct answer, not a failure of the lint).
4. **Positive control (charter 7a):** a fixture source under `futon2/checks/fixtures/absent_is_loud/` with one
   loud helper, one silent helper with an absent literal path, one declared-optional — the lint must report
   exactly 1 violation on it. Negative control: the same fixture with the silent helper made loud → 0.

## Acceptance (dry-run this against the artefact before you report — row 11)
- Running the lint on the real scope lists at minimum the 5 known sites above as `:silent`+absent-now, and
  `read-edn-file`@war_machine.clj:482 as `:silent`. If it does not, the lint is wrong, not the finding.
- Fixture: 1 violation / 0 violations as stated.
- Output is a markdown table written to `futon2/holes/labs/wm-contract/AUD-D2-findings.md` plus the verdict
  line; the verdict line names the scope and the git sha of each repo scanned.
- clj-kondo clean on the new file; `futon4/dev/check-parens.el` clean.
- Do NOT edit any call site or helper. Do NOT touch war_machine.clj. One file + one fixture dir + one findings
  file. If a classification cannot be made honestly from static reading, REFUSE that row with the reason.

## Report
Commit in futon2 with only your paths; bell claude-15 back with: sha, the verdict line, the count of
`:silent`+absent-now sites, any refusals, and one sentence on what the lint cannot see (dynamic paths).
