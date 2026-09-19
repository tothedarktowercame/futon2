# E02 — pattern source hashes at admission

Implementation: `87fea4d7`, author codex-31. Independent review pending.

The unhashed source is not stored admission data under `data/`: it is the
hand-admitted receipt at
`resources/wm/cascade-sources/M-wm-08-external-f2.edn`, copied verbatim by
`cascade-sources/load-declared`. That loader now reads every document-backed
receipt's source and attaches a SHA-256 of the exact byte array it read.
Workspace-relative paths resolve against the same canonical code root used by
observations, rather than the serving JVM's working directory.

A supplied digest is verified against those bytes; disagreement is
`:interpretation-source-hash-mismatch`, not a silently updated admission pin.
Missing/unreadable sources throw the loader's existing typed exception shape:
`:error :invalid-cascade-source`, `:reason :interpretation-source-unreadable`,
with the source path and exception class. A source map without a usable path
is `:interpretation-source-path`. Source-less judgement receipts remain
judgement receipts; no document or hash is invented for them.

The byte-oriented `interpretation-evidence/sha256` is used rather than
`cascade-model-manifest/source`'s text encoding round trip. Both produce the
same digest for this UTF-8 pattern. The tests also cover invalid UTF-8 bytes
so a future text-based substitution cannot silently change the hashed input.

## Execution evidence

The real `run-it-on-a-real-case.flexiarg` admission produced:

`3d006bdff9d3af808112139f9c0342bd93d1c4ab38adf550d9c4754a026d9046`

An independent MessageDigest over the current file bytes agreed. The test
reads the real tracked declaration and retains its actual path-only receipt;
it removes fact observations solely to isolate admission from git/HTTP fact
checks. The file read and receipt producer are real, not stubbed.

Negative controls use a genuinely missing path and a directory that cannot be
read as file bytes. Both must throw the typed unreadable refusal; returning a
receipt, with or without a hash, fails the test. A changing temporary source
proves each admission reads fresh bytes; a previously pinned receipt refuses
those changed bytes. Temporary fixtures are removed after each test.

The one relevant namespace was registered:

`test-registry-8833affea862c7f9348d7f3c00e501d8c209078c8e38c777bd7aca3926c41365`

**6 tests, 22 assertions, 0 failures, 0 errors**, 18231 ms registered execution
(the namespace retains its existing git/registry observation test). Both
changed Clojure files passed clj-kondo with **0 errors / 0 warnings**,
`futon4/dev/check-parens.el` returned OK, and `git diff --check` passed.
`receipt.edn` is extracted from the registered log. The sibling pattern's
observed hash is recorded there; it is an external input rather than a file in
the warrant's futon2 code manifest.

No files under `futon2/data/` were edited, no run invoked, and no shared JVM
reloaded. Historical records remain historical. Reloading the admission
namespace from the canonical checkout and the next ordinary source load will
produce hashed receipts without editing the declaration or old run records.

The initial registry attempt refused the obsolete `:test-environment` option:
the registry now fixes its own canonical environment. Removing that option
used the current interface; the successful warrant used the canonical
LC_ALL/LANG/TZ values. No registry behavior was changed.
