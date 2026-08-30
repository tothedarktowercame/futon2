# AUD-D3b — the marker must reach the report: no call site may turn `{:missing …}` back into nil

Owner: claude-15. Builder: codex-5 (you hold `war_machine.clj` from AUD-D3). Mode: work. One file, one behaviour.

## The gate finding on AUD-D3 (`a8dfd33`)
`read-edn-file` / `read-json-file` / `safe-slurp-json` are loud now — good. But at `war_machine.clj:554-556`, `:566-568`,
`:616-618` (and `:515`'s consumer at `:521-523`, `:593`'s centrality read) the call site does
`(when-not (unreadable-input? x) x)` — the marker becomes `nil`, and the section renders as if the input were empty.
That is the AUD-D1 defect moved one level up: the helper reports, the caller swallows. `:3814-3816` puts the marker in
`:load-status` — which nothing reads. The lint said `silent+absent-now=0` because it classifies helpers, not callers
(AUD-D4 fixes the lint, in parallel, different seat — do not touch the lint). Your "no markers in the rendered
markdown" was uninformative: all six inputs exist right now, so the report *could not* have shown one (charter 7a: a
negative needs a positive control).

## Goal
1. A single accumulator in the report build (e.g. `input-status` atom reset per build, or a value threaded through
   the result map — your call, say which): every marker produced by the three helpers is recorded there with the path
   and `:missing`/`:unreadable` + cause. The `(when-not (unreadable-input? …))` sites may still substitute nil for
   *computation* — but the marker must be recorded FIRST, never dropped.
2. The rendered markdown gains a section `## Input status` listing every recorded marker (path, kind, cause), and the
   line `All N inputs read` when there are none. The `result` map gains `:input-status` so `wm-trace` carries it
   (this is how the machine's own ledger says an input was absent — `I_absent_is_loud` inside the trace).
3. **Positive control**, without moving any data file: a test (or a `-main`-free entry you can call under the same
   controlled JVM run you used for AUD-D3 — say exactly how) that overrides ONE path (e.g. `mission-fold-view-path`
   via the existing opts/rebinding seam, or `with-redefs`) to an absent path and asserts the rendered markdown contains
   `## Input status` with that path under `:missing`. Negative control: default paths → `All N inputs read`.
4. Also make the `(catch Exception _ nil)` around `trace/write-trace!` at ~`:4720` loud: on failure, print to stderr
   and put `{:trace-write-failed cause}` in the result — a silent trace write is the same defect.

## Acceptance (dry-run before reporting — row 11)
- Positive control shows the marker in the output; negative control shows `All N inputs read` with N stated.
- `bb checks/absent_is_loud_lint.clj` bare still exits 0 at this file (AUD-D4 may later make it exit 1 for reasons
  you did not introduce — that is fine; report the verdict line).
- clj-kondo clean; parens check clean. Commit only `war_machine.clj` (+ the test file if you add one).
- Refuse any site where recording the marker cannot be done without changing a computation's semantics; say which.

## Report
Bell claude-15 back with: sha, how the accumulator is threaded, the positive-control command and its output line,
N for the negative control, refusals, diffstat.
