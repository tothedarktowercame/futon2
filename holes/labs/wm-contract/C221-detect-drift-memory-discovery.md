# C221 — `detect_drift.py` memory discovery

Date: 2026-08-31. Discovery only; no detector code changed.

## Verdict

The reported 48 GB is **not reproducible as memory owned by the current
detector**.  A single quiet run under the bounded testing service completed in
0.06 s with a peak RSS of **20,320 kB**.  The original July implementation at
`278e409` completed against today's inputs at **16,896 kB**.  The current scan
was semantically red because `belief.clj` has drifted, but its resource status
was clean.

Bounded run:
`bounded-1788216952758-c221-drift-single`; receipt
`/tmp/futon-bounded-tests/bounded-1788216952758-c221-drift-single.receipt.json`;
resource certificate
`/tmp/futon-bounded-tests/bounded-1788216952758-c221-drift-single.resource.edn`.

Historical control:
`bounded-1788216995594-c221-drift-original`; receipt
`/tmp/futon-bounded-tests/bounded-1788216995594-c221-drift-original.receipt.json`.

## What is held

- `detect_drift.py:143-148` hashes one referenced file's `read_bytes()` (or a
  directory's immediate name list).  The bytes are transient; only a 16-byte
  hexadecimal digest is retained.
- `:151-189` reads the full text only for the small named-form subset and
  retains the extracted form hash and line number, not the source text.
- `:192-203` builds `cur`, keyed by referent, containing fragment-name sets,
  existence, hashes and optional unit metadata.  It does not contain whole
  file contents.
- `:213-226` (added by `3a9d98e`) takes two input-state sandwiches.  Each pass
  rereads the inputs, but the two dictionaries retain only path-to-digest
  strings.  Repository state is obtained with `git rev-parse HEAD` (`:70-73`),
  not a history walk.

Measured population: 72 referents, 65 files, **2,372,878 bytes** total and a
493,220-byte largest file.  The manuscript fragments add 432,411 bytes.  The
referenced repositories have histories ranging from 186 to 3,913 commits, but
the detector executes no `git log`/`rev-list` and the 0.06 s run does not vary
with those depths.

Therefore work and transient allocation scale with the bytes and repeated
occurrences of the referenced files (and linearly with referent/fragment
count), not repository size or history depth.  Peak retained memory is the
Python runtime plus the largest simultaneously live file/form strings and the
small per-referent dictionaries; it is not the sum of repository history.

## Was C198 implicated?

No.  C198's `93d4004` adds the four-valued unavailable outcome, exception
hook, and controls.  It adds no collection and no large retained object.  The
later `3a9d98e` concurrency check adds the two digest passes and repository
HEAD observations.  This raises measured peak from the July control's 16.9 MB
to the current 20.3 MB, not to gigabytes.

## What the 48 GB observation establishes

It establishes that two processes whose command lines named the detector were
observed during a machine-wide pressure incident; it does not identify an
allocation path in this detector.  No current code path can account for that
population, and a quiet bounded reproduction is smaller by a factor of about
2,374.  The earlier readings therefore remain an unexplained process/runtime
attribution incident rather than a reproduced detector property.  Resolving
that would require a receipt or `/proc/<pid>/smaps` capture from an affected
PID while it is growing; neither survives the incident.

The full workspace gate was intentionally not run: it would spawn the detector
again and was explicitly outside this discovery.
