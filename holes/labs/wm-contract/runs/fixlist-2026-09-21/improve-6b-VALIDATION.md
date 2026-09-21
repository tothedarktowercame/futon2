# improve-6b — revision and friction candidate scanner

Branch `fix/narrative-improve-6b`, base `8301836b`. Read D1–D5 in the definitions
note. This is a standalone reader, not on the click path; no load-identity
registration for the scanner. No capability grading, selection/model changes,
serving-JVM evaluation, clicks or reloads.

## Inputs, declaration and output

```sh
clojure -M -m futon2.aif.revision-scanner CONFIG.edn > ledger.edn
```

The configuration explicitly names `:repos`, `:surprise-files`, `:run-files`,
`:bindings`, `:as-of`, and a positive `:unanswered-after-seconds`. No interval
is invented when it is absent. The output is an as-of **record-only ledger
snapshot**, not a production consumer or an append to the calibration ledger.

The real declaration is `improve-6b/live-config.edn`: only canonical **futon2**
is a declared Git repository. Git's reachable HEAD history, including merges,
is scanned. Proper `Surprise: <id>` trailers are read with Git's trailer parser,
not a regex over arbitrary prose. Merge file changes are against the first
parent. Every citation retains repository, commit, committer time and changed
paths. IDs not present in the supplied surprise files are flagged
`:unknown-surprise-id`, never joined. Conflicting copies of one surprise ID
are flagged and excluded; identical copied receipts deduplicate.

The declared unanswered interval is **21,600 seconds (six hours)**: a follow-up
signal within a working day, not a definition of how fast learning must occur.
An uncited surprise older than that interval yields `:unanswered`. Prefer its
observation time; for the historical record lacking that timestamp use the
declaration time and explicitly label `:clock-basis :declaration-time-proxy`.
The deadline is therefore approximate for that old record, not a recovered
measurement timestamp. Citations after the scan time do not answer a surprise.

The expectation class is the structured tuple:

```clojure
{:rule rule :target target :token effect-token
 :action-class action-kind :patterns ordered-pattern-ids :model-part part}
```

It excludes surprise/expectation IDs, probability, model digest and occurrence
clock. Different re-freezings of the same class can therefore recur. Target,
effect and order remain in the class to avoid conflating unrelated work. A
recurrence needs a distinct action occurrence and a declaration after the citing
revision. It is emitted as `:recurring-after-revision` with the cited commit
and prior surprise; `:consumption-established?` distinguishes evidence of use
from a source-only revision. It is a friction **candidate**, not an automatic
assertion that the changed model was running when the recurrence occurred.

## What each grade proves

* `:committed`: a reachable commit cites the retained surprise after its known
  observation time (or explicitly limited declaration-time proxy). This does
  not prove a structural revision. `:revision-kind :unclassified` and
  `:candidate-not-capability` stay explicit.
* `:loaded`: the commit changed one or more **declared part bindings**, and a
  later run receipt has fix-20's `:source-digest-at-namespace-load` identity.
  For every changed bound part, its `:namespaces ns :loaded-source` is captured,
  its SHA256 matches `git show COMMIT:path`, its canonical path matches the
  declared repository/path, and capture happened after commit but no later
  than the run. Old/new file digests are retained. Disk/current alone, a commit
  date, or an old disk-vs-disk runner identity is insufficient. This inherits
  fix-20's limitations: capture is not bytecode attestation; mutation or a file
  edit during compilation is not certified.
* `:consumed`: **the same run receipt** also exercises the same expectation
  class. Supported carriers are a frozen token-outcome prediction with rollout,
  or the older tick's per-policy `:g-term-decomposition` Q trajectories together
  with its declared target wants. The latter records planning consumption, not
  successful execution. Both require matching target, action kind, ordered
  patterns, wanted token and, for `:B-effect`, the declared produced token.
  Older Q records are explicitly adapted as `:recorded-trajectory/:policy-term-Q`;
  the scanner does not run a model to manufacture consumption evidence.
* A loaded revision followed by evaluated work that does not exercise its
  class has `:grade :untested`, preserving `:attained-grades [:committed :loaded]`.
  Load-only evidence without an evaluated successor has `:grade :loaded`.
  A never-loaded commit remains `:committed`, with `:successor-status :untested`.
  Positive consumption retains all three attained grades.

The real binding declaration covers B-effect's cascade model namespace and the
AIF source declaration. Namespace load evidence exists in fix-20's carrier.
**There is no equivalent declaration-load receipt here**: changing only the
source declaration can reach committed, not loaded. A read-time file digest is
not silently relabelled a load receipt. Other changed files/model parts without
a supplied binding remain committed with `:no-declared-changed-part-binding`.
Bindings are explicit account scope, not an inference from a filename.

Separate files are not joined on timestamp or bare `attempt-001`; consumption
requires source identity and evaluated-class evidence co-retained in a run.
This conservative boundary avoids combining unrelated snapshots. It can miss
legitimate consumption retained only across separate files; adding such a join
requires an occurrence/manifest-backed adapter, not a temporal guess.

No scanner grade supplies D3's reviewer-approved revision kind, or D6's successful
successor. These are learning-event candidates for review, not a count of
confirmed structural learning or capability.

## Real scan and historical updater

The live scan found **0 retained surprise receipts** and no citing revisions.
The legacy reference run predates improve-6a. It would be false to report the
replay as a newly written close receipt.

`improve-6b/replay.clj` separately reconstructs the frozen prediction from the
original occurrence/action, original D and recorded Q horizon, and original
source declarations, then calls the real fix-10a comparator and improve-6a
surprise function. The old tick did not contain today's complete decision map;
its declaration inputs must be reconstructed from retained records. Input
paths and hashes, both configurations and the result are committed beside it.

That explicitly labelled `:historical-replay` has exactly **one** surprise:

`surprise-12ae64452b8a9cbf6abd336686b6aa4583e656ce301c3fd3c3bf47d23bcf9bae`

It is the updater `["M-aif-policy-conditioned-eig" :hole/h6378c65a4012]`, class
`:B-effect` / `:apparatus/one-authority-per-question`. No commit cites that ID
in the declared history. It is `:unanswered` under the six-hour interval, with
the timestamp proxy explicitly shown. No learning revision is claimed. No files
were added to the real attempt's `retained/` directory.

Reproduce (outputs confined to this branch's report directory):

```sh
D=holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-6b
clojure -M:test "$D/replay.clj" "$D" AS_OF_UTC_ISO_INSTANT
# Use :as-of from committed live-config.edn to repeat the recorded scan clock.
clojure -M -m futon2.aif.revision-scanner "$D/live-config.edn"
clojure -M -m futon2.aif.revision-scanner "$D/historical-config.edn"
```

## Gates and bad case

```sh
clojure -M:test -m cognitect.test-runner -n futon2.aif.revision-scanner-test
clj-kondo --lint src/futon2/aif/revision_scanner.clj test/futon2/aif/revision_scanner_test.clj holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-6b/replay.clj
emacs --batch -Q -l /home/joe/code/futon4/dev/check-parens.el -f arxana-check-parens-cli -- src/futon2/aif/revision_scanner.clj test/futon2/aif/revision_scanner_test.clj holes/labs/wm-contract/runs/fixlist-2026-09-21/improve-6b/replay.clj
```

**2 tests / 13 assertions, 0 failures/errors**. Kondo 0 errors/warnings; parens OK.
Tests create and delete temporary Git repositories and run/surprise records;
there are no Git stubs, network calls, production data writes or serving-JVM
reads. Controls cover every requested bad case, all three positive grades,
wrong source digest, the pre-deadline boundary, and the real older Q receipt
shape. Tests use synthetic load/run receipts; they do not claim a JVM reload.

Copied only the test namespace onto unmodified `8301836b`: **2 tests / 13
assertions, 12 failures / 0 errors**. Example:

```text
expected: (= :committed (get-in result [:learning-event-candidates 0 :grade]))
  actual: (not (= :committed nil))
```

Another failing assertion pins the missing unknown-ID flag. Logs are retained
in `improve-6b/`; no production gate or invariant was bypassed.
