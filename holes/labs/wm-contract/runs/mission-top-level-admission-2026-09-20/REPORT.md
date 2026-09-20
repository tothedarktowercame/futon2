# Primary-only mission scan and top-level admission

Request: claude-2, invoke-1789943079691-22802-2f23e924. Builds on accepted
structural-fence commit 0d3f5a4d8eb4d19748d4bf9d9b382c4956811a1b.

Only repositories whose `.git` is a directory enter the file scan. Plain
no-`.git` directories are now excluded as explicitly authorized after the
owner's census established they supplied no unique missions. Existing named
fences remain, including the futon3b cross-repo exclusion.

The captured mission stem now accepts both `holes/M-*.md` and
`holes/missions/M-*.md`. The loader lists only these two specific directories
per admitted checkout, without recursive traversal. No substrate writes,
backfill, watcher edits, or mission file moves were performed.

## Two-step read-only measurement

| Stage | Survivors before dedupe/derived filter | Distinct IDs before derived filter | Final missions |
| --- | ---: | ---: | ---: |
| Accepted structural fence baseline | 288 | 254 | 242 |
| Change 1: primary checkouts only | 254 | 254 | 242 |
| Change 2: admit both directory shapes | 345 | 345 | 328 |

Baseline counts come from the accepted fence evidence and owner's supplied
census. Step 1 and step 2 each ran the actual loader in a fresh tooling JVM.
A forwarding observer around `mission-doc->entry` counted parsed entries;
it called the original parser and did not change admission or parsing.
The retained ID/path map from each stage is in `step1.json` and `step2.json`.

- Change 1 changed retained paths: `[]`; vanished IDs: `[]`; added IDs: `[]`.
- Change 2 changed retained paths: `[]`; vanished IDs: `[]`; added IDs: 86
  (complete list in `step2.json`).
- `M-a-wmc-scaling` occurs exactly once after change 2, at
  `/home/joe/code/futon2/holes/M-a-wmc-scaling.md`.

The widened current scan adds 91 candidates and 86 final missions; these are
observations of the current filesystem, not the earlier packet's 64-file estimate.
Dedupe is a no-op in both measured new stages: survivor and distinct-ID counts
are equal.

## Both-side controls and gates

The existing structural worktree test remains unchanged. The added fixture
uses a primary named `primary-checkout-with-a-deliberately-long-name` and a
worktree named `wt`, both containing top-level M-X and nested M-Y documents.
It asserts the worktree path is shorter, only the two primary paths reach the
real parser, and each primary mission is retained exactly once. Stem extraction
accepts both supported shapes and rejects a notes/ path. Plain-directory
exclusion and the independent futon3b exclusion are pinned. Legacy document
fixtures now explicitly create primary `.git/` directories.

The actual top-level upsert returns `:unchanged` for an explicitly provided
matching record; substrate lookup and write functions throw if invoked. The
unsupported notes/ shape still returns `:path-not-admitted`.

Fresh tooling JVM test runs (both exit 0; logs retained here):

- `clojure -M:test -m cognitect.test-runner -n futon2.aif.mission-registry-test`:
  22 tests, 74 assertions, 0 failures, 0 errors.
- `clojure -M:test -m cognitect.test-runner -n futon2.aif.mission-registry-substrate-test`:
  8 tests, 24 assertions, 0 failures, 0 errors.
- `clj-kondo --lint` on all three changed Clojure files:
  `linting took 42ms, errors: 0, warnings: 0`.
- `emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval
  '(arxana-check-parens-cli)' -- <three changed Clojure files>`: `OK`, exit 0.

Final source SHA-256 (only comment wrapping followed the test runs):

- `src/futon2/aif/mission_registry.clj`: `ec9fc3f0230f76ca39b08426693442545bc03795d62e45f554e0109ca1204c33`
- `test/futon2/aif/mission_registry_test.clj`: `75d7e9a93fe6ab0a73c7df0c3c993b42fc25e4423dee8ea2500db86dd175f3bf`
- `test/futon2/aif/mission_registry_substrate_test.clj`: `7b501979d711156129c2729d937e0390b71f157c4ff43ddd70e0f905fe22eb6a`
