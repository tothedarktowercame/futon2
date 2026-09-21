# Runner discharge stage

Author: codex-8. Requesting owner: claude-12.
Authority: invoke-1789966046653-22916-a702739f and witness addition
invoke-1789966579020-22921-a7d79046. Independent review: pending.

Implementation and warrant commit IDs are recorded in postcommit.json after
landing. **COMMITTED IS NOT LOADED.** No serving-JVM load, live tick, or
production repair-store write was performed by this packet.

## Behavior

The runner finalizes discharge after its immutable execution close, before
persisting its terminal run record. The terminal record carries
`:repair/discharge`; the repair library appends a separate outcome event.
An evaluator-supported machine repair registers implementation A and explicitly
waits for successor B. B must have a different actual attempt and run identity,
be later, and carry finding-specific replay evidence. The existing
`successor-resolution!`, registrar, and `resolve!` conjunctions still apply.

The former action-type-bound writes and first-memory-item validation shortcut
are removed. Substrate insertion remains execution evidence; it does not
resolve a repair. Rejected reviews make no implementation/resolution writes.
Ordinary failure-finding intake remains unchanged. Store refusals retain their
message and data on the close result instead of re-entering execution close.

Each phase retains a store-owned immutable intent and its evidence. The
successor relation references the actual intent file and its raw byte hash,
not a conceptual witness name. The resolution retains the exact intent bytes,
review job, evaluator admission, observation, artifact binding, and close
capture. Those fields supplement existing store gates; they do not replace
or weaken them.

## Derived receipt and recovery

The success-only path is
`holes/labs/wm-contract/discharges/<full-native-repair-id>.edn`.
It embeds exact finding/implementation/resolution bytes and hashes, both phase
identities, the successor relation, and retained review/witness evidence.
The receipt's own commit and byte hash are returned outside its contents.
`:repair/discharged?` becomes true only after committed readback verification.

Publication uses `git commit --only` for the exact receipt path: Git's temporary
commit index and normal ref/index locks preserve other staged changes. There
is no reset, stash, amend, forced ref update, code commit, or broad staging.
An explicit stale expected-head token refuses; conflicting receipt bytes
refuse. Publication retries twice without repeating store transitions.

At every tick start, catch-up enumerates `resolutions/`, independently of the T
queue. New stage-produced resolutions reconstruct identical receipt bytes even
if the process stopped immediately after the store write. Already committed
receipts are readback-verified without another commit. Legacy resolutions
without retained discharge context report `:resolution-context-unavailable`;
no historical review/run provenance is fabricated. Supersession is not a
successful resolution and receives no success receipt.

C3 uses the separately returned `:c3-locator`, whose repo is relative to the
observation reader's workspace root. The generic receipt locator retains the
absolute Git repository. Presence alone is insufficient: `verify!` compares
receipt semantics and embedded bytes with authoritative store records.

## Evaluator authority and first implementation

`resources/wm/repair-evaluator-admissions.edn` is intentionally EMPTY pending
independent evaluator admission. Code registration alone cannot grant admission.
Absent admissions produce `:no-repair-evaluator`; orchestrated closing remains
the route for unsupported kinds/contracts. Initial stage support is the machine
and independent-review-failure code-commit contract, not invented support for
other artifact shapes or dismissal transitions.

A committed evaluator admission names its failure kind, registered evaluator,
source revision and source closure, author, reviewer, and actual review job.
The review must execute tools and approve; its retained prompt binds
`REPAIR_EVALUATOR_ADMISSION_SHA256: <digest>` of the admission without
`:review-job`. Changing its source, inputs, reviewer, or kind changes that
binding. The declaration cannot name arbitrary callable code.

The first authored evaluator is `:history-artifact-read`, for the explicitly
bound unreadable-history case within `:initialization-failed`. It requires a
per-finding input binding: original finding byte hash, recorded locator text,
actual input path and byte hash. Other cases of that broad failure kind refuse.
It archives the actual repaired revision into a disposable sibling directory
and runs the replay in a fresh tooling JVM. This avoids mistaking the serving
JVM's older loaded implementation for the repaired revision. Commands have a
60-second bound; all replay writes use scratch roots, and the archive is removed.

The negative control exercises the original unguarded reader on the recorded
poison bytes. It is a replay of that failing reader seam, not a claim that an
entire pre-repair checkout was executed. The repaired path must consume good
history, exclude the non-identity poison, admit the next opportunity, and still
refuse admission when authoritative identity is unreadable. A readable unrelated
input cannot supply the required failure observation.

T actions must carry `:repair/id`, exact `T-<id>` target, `:finding-source`
with canonical path and byte hash, verbatim `:discharge-contract`, and admitted
`:interpretation-receipts`. Prefix matching alone cannot authorize discharge.
The prompt reads that same pinned native finding. Supply must retain those
fields on its admitted action; proposed records alone are not executable.

## Validation and limits

`precommit.json` records exact source/test hashes and validation role. The
postcommit warrant records use a separate role and compare those hashes.
Static gates: clj-kondo and futon4 check-parens. Namespace results are retained
in the pre-*.log files and postcommit registry records.

Demonstrations use real repair-store APIs, Git publication, the production
observation reader, and the actual quarantined history input. Outer Agency
jobs and A/B identities in integration fixtures are constructed; these are
NOT live discharges or independent reviews. The production finding and its
quarantine remain unchanged.

Controls cover both phases, same-run/self-successor refusal, repeated A without
another implementation write, missing admission, stale source and finding pins,
review rejection, actual store validity refusal, irrelevant replay input,
process death after resolution before publication, byte-equal catch-up,
unrelated staged files, moved HEAD, and forged receipt bytes. The runner's old
substrate-only auto-discharge assertions were replaced with no-discharge
assertions; its execution/review assertions remain.
