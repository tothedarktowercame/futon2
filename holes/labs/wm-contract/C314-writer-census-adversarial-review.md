# C314 — adversarial review of the scheduled-writer census

Date: 2026-09-01. Reviewer: `wm-evidence`. This review changed no process,
unit, registry, or fence implementation.

## Verdict

C309 correctly identified the known high-risk writers and its four claimed
read-only watchers are read-only in the inspected implementations. Its
conclusion that a clean window is reachable is plausible, but is not presently
machine-verified. `write-set-unknown` is **unverified**, not established empty,
and C292 does not observe the C309 writer population at all.

The material distinction is between **Git-clean** and **no writer**. During this
review, `/proc` showed two open writable handles inside a certified repository:

- `/home/joe/code/futon3c/holes/labs/M-diagramprover/apm-driver/axiom-audit.jsonl`
  held by `/usr/bin/python3 /tmp/axiom_audit.py`;
- `/home/joe/code/futon3c/data/apm-campaigns/jit-all-open-v2/coordinator.edn.tick-claim.lock`
  held by the serving JVM.

Both are compatible with a clean `git status` because they are ignored/runtime
paths. Therefore C292's clean-tree sandwich cannot establish that these writers
are parked.

## How the census was taken, and its blind spots

C305 supplies six point-in-time commands: user timers, path units, running
services, a process-name regular expression, and the two background-job lists
(`C305-writer-fence-procedure.md:36-45`). C309 then inspected named executables
and explicitly required a refresh at window-open
(`C309-scheduled-writer-census.md:3-6`). This is useful discovery, not a complete
writer inventory.

The process regular expression misses a manually launched writer whose command
does not contain `watch`, `generator`, `publish`, `apm`, or `bg.py`; future
process starts; editor/autosave processes; writers hosted inside an already
running JVM; system-level/container writers; and a known script whose bytes or
configuration change while its PID and command line stay constant. Unit
enumeration has the complementary gap: it misses hand-launched work. Open-file
enumeration can find current handles, as above, but cannot prove that a process
will not open a file later.

Consequently the C309 rule “anything not named is `write-set-unknown`” is sound
(`C309...:50-52`), but no current mechanism proves that this set is empty. The
required reconciliation with every session retaining write authority remains a
human/owner acknowledgement (`C305...:18-24`), not a derived census result.

## Re-verification of the claimed safe classes

- `mana-snapshot` reads Git/HTTP and writes its configured output. The observed
  unit uses the default `/home/joe/code/storage/futon0/mana-snapshot.json`; it
  excludes the five repositories. The target is configurable, so this finding
  belongs to the observed unit invocation, not every possible invocation.
- The Futon1b metaspace sampler appends beneath its configured storage output;
  the observed unit uses `/home/joe/code/storage/futon1b/metaspace`. The same
  invocation-scoped caveat applies.
- The three `apm-watch.sh` instances read campaign state and print. Their helper
  `apm-frame-pulse.py` also reads and reports; no repository write was found.
- `apm-watch-projection.sh` and its projection-watchdog calls read files/HTTP
  and report; no write was found.
- `futon1b-heap-watch.sh` retains counters in shell memory and prints; no
  repository write was found.
- The unauthorised-campaign ticking watch reads coordinator state and prints;
  no repository write was found.

Thus the sampler/watcher dispositions at `C309...:12-13,26-29` hold for the
captured executable bytes and invocations. PID and command-line equality alone
is insufficient if an interpreted script changes; a window-open disposition
must also bind the executable/script content or rely on the settled repository
basis plus the writer fence.

The scheduled APM watchdog and axiom audit are not repository-read-only. They
write ignored paths inside `futon3c` (`C309...:14-15`). Describing them as not
dirtying the repository is true in Git terms but does not satisfy an event-free
writer claim. Pattern indexing writes outside the five certified repositories,
as recorded, although it remains a resource and downstream-write concern.

## Does C292 re-check the census?

No. `checks/quiescence_check.py:90-96` observes only five repository HEAD/status
pairs, the four-lane registry, ordinary jobs, and bounded jobs. Its findings at
lines 99-124 have no timer, service, process, coordinator-enabled, babysitter,
watcher-PID, open-handle, or acknowledgement condition. Its state sandwich can
prove that **those sampled values** did not change; it cannot prove that a
parked coordinator remained parked.

A coordinator can resume and write ignored runtime state without changing any
C292 field. A writer can also perform an ABA write-and-restore between Git
observations. C305 acknowledges the latter limit and relies on the fence
(`C305...:91-98`), but the fence's writer premise is not executable. A
parked-then-restarted coordinator is therefore invisible to C292 unless its
effects happen to dirty a tracked/untracked path or create a listed job.

## Consequence for the drain

Do not treat `python3 checks/quiescence_check.py` as proof that the APM writers
are parked. Before opening the window, retain all C309 owner acknowledgements
and additionally record a fresh, reconciled roster of:

1. enabled durable coordinators and their state paths;
2. babysitter, closer, watchdog, audit, and pattern-index unit states/next
   activations;
3. all sessions/processes retaining workspace-write authority, including
   manual/editor sessions;
4. the exact executable/script identity supporting each read-only/excluded
   disposition.

Recheck that roster at every C305 checkpoint or install a writer fence/revision
witness that makes restart visible. Until then, “APM writers parked” is an
operator-attested precondition, not a condition C292 verifies. The shortest
honest characterization is: **known writer classes are nonempty now;
`write-set-unknown` is unverified; window reachability is conditional on the
full owner roster and its continuing acknowledgements.**

