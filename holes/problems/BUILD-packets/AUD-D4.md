# AUD-D4 — the lint must see marker-swallowing at call sites of LOUD helpers

Owner: claude-15. Builder: a seat that does NOT hold `war_machine.clj` (codex-5 has it). Mode: work. One file + fixture.

## The blind spot (found at the AUD-D3 gate)
`futon2/checks/absent_is_loud_lint.clj` classifies helpers (`:177-198`: a helper returning `{:missing …}` is loud) and
lists call-site guards only for SILENT helpers. After AUD-D3 made `read-edn-file` loud, its callers at
`war_machine.clj:554-556, 566-568, 616-618` do `(when-not (unreadable-input? x) x)` — the marker becomes nil and the
lint reports `silent+absent-now=0`. The invariant's falsifier is "nil/empty from a missing file flows into the same
branch as 'the file said nothing'" — that is exactly this, one call deeper. Do NOT edit `war_machine.clj` (codex-5 is
fixing the sites in AUD-D3b in parallel); your job is that the lint would have caught it.

## Goal
1. For every call site of a LOUD helper, detect **marker-swallowing**: the result bound and then passed through a form
   that maps the marker to nil/empty/default without recording it — at minimum these shapes:
   `(when-not (unreadable-input? x) x)`, `(if (unreadable-input? x) nil …)`, `(when (map? x) …)` guarded on the marker
   keys, `(dissoc x :missing …)`, `(or x default)` where x may be a marker, and `(:missing x)` / `(contains? x :missing)`
   tests whose non-marker branch is the only one that produces output. Treat any predicate whose body tests
   `:missing`/`:unreadable` as a marker predicate (find them by scanning `defn`s, as you found helpers).
2. A site is **conformant** if, on the marker path, the marker is (a) thrown, (b) returned/threaded as data (appears in
   a returned map or is `conj`/`swap!`-ed somewhere), or (c) printed to `*err*`. Static approximation is fine; refuse
   (`dynamic/refused`) when you cannot tell — never guess conformant.
3. New verdict fields: `loud call sites=N marker-swallowed=M`; exit 1 when M>0 (in addition to the existing rule).
4. Fixture: extend `checks/fixtures/absent_is_loud/{positive,negative}.clj` with a loud helper + a swallowing caller
   (positive → +1 violation, now 3) and the same caller threading the marker into its return map (negative → still 0).
   Update the expected positive count and say so in the commit message.

## Acceptance (row-11 dry-run first)
- On the real scope at futon2 HEAD *before* AUD-D3b lands (state the sha you ran against): the three sites above are
  reported as marker-swallowed, and `:3814-3816` (`:load-status` — threaded into a returned map) is reported conformant.
- Fixture controls 3 / 0. clj-kondo clean; parens clean. Commit only the lint + fixtures.

## Report
Bell claude-15 back with: sha, the verdict line, the list of marker-swallowed sites (file:line), refusals, and the one
shape you could not detect statically.
