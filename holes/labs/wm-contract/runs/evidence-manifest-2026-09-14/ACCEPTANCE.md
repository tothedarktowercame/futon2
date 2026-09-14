# Independent review acceptance — close-evidence manifest v1 mechanism

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits ac6ef739
(spec + mechanism + controls), 9b916f18 (test lint), 069e9ecc
(receipts). Verdict: ACCEPTED.

Checked:

- File scope: spec + one namespace + one test namespace + receipts;
  no existing namespace touched.
- All five anchors implemented: exact entry shape (nonblank id,
  absolute path, 64-hex digest, parsed instant); manifest record with
  meaningful order, unique ids, and manifest-sha over the canonical
  pr-str of entries; build with MANDATORY injected read-bytes and
  per-entry supplied admitted-at (no clock, no filesystem default);
  optional expected-sha as an integrity constraint (spec correctly
  says it is not authority by itself); exact ordered agreement with
  a retention block's :admitted-evidence, empty-empty agreeing.
- Determinism handled properly: entries are canonicalized to a fixed
  key order (array-map) before hashing, so a manifest built with
  different caller key order still validates; entries contain only
  strings, so pr-str stability is not exposed to arbitrary data (the
  concern noted at close-retention acceptance does not recur here).
- validate-manifest revalidates WITHOUT rereading sources — correct
  separation: admission proves bytes at admission time; later
  validation proves the record's internal integrity only. The spec
  states the mtime/git-date/backdating exclusion from the design TN.
- Controls: happy path with order + rebuild determinism and
  closed-form independent digests; typed refusals (missing source,
  duplicate id, blank id, digest mismatch, malformed instant, extra
  key); agreement exact/reorder/extra/missing; empty-empty plus
  empty-vs-nonempty refusal. Receipts honest (bytes-shadowing warning
  retained, corrected in 9b916f18), finals kondo 0/0, full-driver
  parens, fresh-JVM 4 tests / 19 assertions exit 0 at 9b916f18.

Note for the seam packets (not a defect): the manifest proves
pre-cutoff existence only if the WRITER retains it no later than the
close and checks every admitted-at against its own :recorded-at —
that ordering check is a seam-packet anchor, since the pure mechanism
cannot know the cutoff.
