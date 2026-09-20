# Mission scan structural worktree fence

Authority/owner: claude-2, bell invoke-1789942674897-22801-c2b521eb.
Implementer: codex-5.

The bounded repository enumeration now classifies primary checkouts by a .git
DIRECTORY, excludes .git FILE repositories before enumerating mission docs, and
preserves the existing admission of directories with no .git. The name-based
fence remains unchanged, including the futon3b cross-repo exclusion. Its docs
and dedupe comments now distinguish structural filtering from heuristic choice
among still-admitted duplicate IDs. No mission-path widening is included.

## Real-data measurement (read-only, /home/joe/code)

| Measure | Before | After |
| --- | ---: | ---: |
| Matching candidate files before fences | 1668 | — |
| Candidates surviving fences, before dedupe | 1583 | 288 |
| Final mission IDs | 242 | 242 |

Changed retained paths: **[]**. Vanished mission IDs: **[]**.
The initial old-pipeline projection was checked against the unmodified actual
load-missions-from-files result. After editing, the real production file reader
was invoked with a forwarding parser observer to count pre-dedupe candidates;
no classifier, dedupe, or reader was replaced. Its final paths equal the
prospective result. Full path maps are retained in before-and-projection.json
and actual-after.json. No substrate reads or writes were used by the census.

There are **3** immediate code-root directories with holes/missions/ and no .git:
- /home/joe/code/futon6-old-copy
- /home/joe/code/futon2-p9-baseline.FKhOjF
- /home/joe/code/futon3c-index-check

They remain eligible at the structural filter; the existing index-check pattern
still excludes futon3c-index-check. No-git eligibility does not classify these
as primary checkouts. Remaining duplicates in admitted plain directories can
still require the documented path-length dedupe heuristic.

## Acceptance and gates

The new tmpdir fixture exercises .git DIRECTORY primaries versus .git FILE
worktrees in two relative path-length arrangements. In the second arrangement
the worktree is explicitly shorter than the primary. Both retain exactly one
M-X with the exact primary path; a forwarding observer of the real parser
also proves the worktree was excluded before parsing/dedupe. A separate fixture
pins admission of a no-git directory and exclusion of a futon3b primary checkout
whose unique mission ID could not be hidden by dedupe.

Fresh-JVM tests:
- clojure -M:test -m cognitect.test-runner -n futon2.aif.mission-registry-test:
  21 tests / 65 assertions / 0 failures / 0 errors, exit 0.
- clojure -M:test -m cognitect.test-runner -n futon2.aif.mission-registry-substrate-test:
  7 tests / 22 assertions / 0 failures / 0 errors, exit 0 (substrate ports stubbed).
- clj-kondo: 0 errors / 0 warnings on both modified Clojure files.
- check-parens: OK on both modified Clojure files.

Commands, statuses and static-log hashes are in checks.json; namespace logs are
retained here. No substrate backfill, file moves, watcher changes, or top-level
holes/M-*.md admission. Only explicit owned paths are staged; no amend.
