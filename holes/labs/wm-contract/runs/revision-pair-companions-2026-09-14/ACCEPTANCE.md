# Acceptance — revision-pair companion admission (2ed49ee5, receipt 714d9dd8)

Reviewer: claude-15, per the coding-handoff protocol. Author: codex-24
(job invoke-1789421500151-20859-3f332269).

VERDICT: **ACCEPTED**.

What I checked (diffs read via git show; receipt validated, not re-run):

- `limb_evidence.clj`: `validate-capture` now accepts both the original
  path-backed shape {:source-path :sha256 :captured-at} and the file-backed
  shape {:file :sha256 :bytes} used by the cohort-53 attempt-001 deposits;
  `:file` goes through `output-file!` (flat-name discipline, so path
  traversal stays refused); `:bytes` must be a non-negative int. New
  `validate-revision-pair-files` verifies BOTH sha256 and byte count of each
  side's companion through the injected read (mismatch → typed
  :output-digest-mismatch with expected/actual for both dimensions;
  unreadable/non-bytes → :output-file-invalid).
- `full_loop_runner.clj` `checkpoint-evidence-manifest`: companion-names now
  includes [:before :file]/[:after :file] of validated
  :wm/entity-revision-pair-v1 records, and each pair is byte-verified via
  the same captures-by-name injected read as receipt outputs. The stray
  check is unchanged for files no record names.
- Tests: unit coverage for valid pair / tampered-after / nested-path
  refusal; end-to-end admission test now deposits a file-backed pair and
  asserts the 13-entry manifest covers every file; the live-pin test pins
  the REAL attempt-001 companion digests (cdfef176…, f6238a7d…), which
  match the incident record entity-runner.edn's own :sha256 fields byte for
  byte — I cross-checked those against my crash diagnosis reads.
- Receipt 714d9dd8: single post-commit run; kondo 0/0, parens OK, fresh-JVM
  `:nses` of both namespaces — 174 tests, 924 assertions, 0/0. Commands
  match the required gates, including the :nses (never :vars) form.

Accepted with three recorded notes, none blocking:

1. File-backed pairs carry no :captured-at, so the strict
   before-precedes-after instant check (reviewer fix 81c10574) does not
   apply to them — inherent to the shape; :revision-unchanged still refuses
   identical revisions. If instants are ever wanted for file-backed pairs,
   that is a schema extension, not a bug here.
2. Map-shaped :dimensions (also from the incident shape) is accepted with
   non-empty check only; contents unvalidated. Future tightening candidate.
3. The live-pin test reads the production data directory at run time; if
   wm-full-loop-machinery-53 is ever archived the test fails loudly at that
   path. That is the live-pin rule working as intended, but the archival
   coupling is now on record.

Also on record: `invalid-attempt-evidence-refuses-close` and the digest
tests assert `(not (.exists close-path))` — they PIN the close-orphaning
behaviour that crashed cohort-53 attempt-001. Packet B (close-path
containment) will change exactly those assertions to expect a typed 007.
