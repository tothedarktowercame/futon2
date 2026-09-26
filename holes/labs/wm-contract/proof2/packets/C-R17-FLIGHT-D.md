# C-R17-FLIGHT-D — one trace-carried population across routes

2026-09-26. Discovery/design only; codex-1 for claude-8 review.

Read pins: futon2 `ebd831f22fa43bcfedd2bc0b7f4c0e737d5a862e`;
mathlib4 `4d565382b2934d6b96f5cfe6a0a5ad9ee668fb9c`.
WM below means `scripts/futon2/report/war_machine.clj`; unqualified source
filenames mean `src/futon2/aif/`. Recommendations are proposed decisions,
not claims that the existing code already implements them.

**Recommendation:** reading (i), one ongoing population for the configured
entity/model, with option A, trace as the sole authority. Flight boundaries
are reporting boundaries, not resets. But forwarding alone is insufficient:
strict history reads, identity continuity, and serialized read/update/publish
are necessary. Those findings prevent honestly sizing the complete repair as
only three forwarded keys. No implementation or registry correction here.

## 1. Forwarding alone

`full_loop_runner.clj:4831–4841` builds the default judge using
`(assoc (select-keys opts [... :flight]) ...)`; the three accumulation keys
are absent. `flight_runner.clj:69–75` supplies the run ID and flight in
`(run! (assoc base-opts :run-id run-id :flight flight))`.
`WM:7939–7947,7998–8021` passes judge opts to `(judge scan-data judge-opts)`.
Merely adding keys to select-keys supplies nothing unless upstream also sets
them. `WM:111–118`'s `(wm/accumulation-config)` returns the entity and declared
initialization, **not** `:trace?`; a flight must explicitly request trace
publication as well.

Given those values and `:trace? true`, `WM:7143–7147` reads
`(trace/recent-trace-records 12 :dir wm-trace-dir)` and takes `peek`.
`WM:105–106` and `trace.clj:63–87` name the same default directory,
`~/code/futon2/data/wm-trace`, with `wm-trace-YYYY-MM-DD.edn` daily files.
`trace.clj:739–754` orders records chronologically across dated files; there
is no flight/entity filter. This is append/file order, not a timestamp sort.

`WM:7351–7357` invokes the adapter when trace? OR entity is set. At
`WM:1531,1539–1555`, both prior matrix and predecessor identity come from that
same latest record. `WM:1518–1526` chooses its `:run/id`, else timestamp;
`machine_accumulation.clj:27` compares it with the matrix's `:last-tick`.
Thus a correctly identified preceding one-shot or click normally **passes**;
route interleaving by itself does not cause a chain gap. A record with a
mismatched stored last-tick causes `:carry-chain-gap`; one with no matrix
causes `:accumulation-migration-required` earlier (`WM:1540–1542`). No prior
record causes declared initialization (`WM:1543–1548`). A missing entity
posterior or identity throws (`WM:1532–1538`). These exceptions currently
escape the adapter, and the runner's selection catch can rethrow them
(`full_loop_runner.clj:4860–4876`): they are not yet record-only omissions.

Success publishes via `(write-trace-and-clock! result trace-dir)` only when
trace? (`WM:7704–7733`); `trace.clj:588–592` retains the matrix and update
input. Entity-only forwarding computes an ephemeral posterior, unless a
caller separately writes a trace. The scheduled script does exactly that
publication at `scripts/wm_scheduled_run.clj:148–152`.

**Correction to my preceding route claim:** the scheduled script at :133
passes only `(wm/accumulation-config)`, which has no tick ID. Its source has
no run-id/scan-id assignment, and `WM:7998–8019` builds scan-data without
`:scan-id`. Consequently, if that invocation reaches the adapter, :7354
supplies nil and :1532–1533 throws `:accumulation-identity-missing`; it cannot
currently reach its external trace write through that path. Also a hypothetical
scan-id-only producer would store that ID as last-tick but lack trace run/id;
the freshly minted trace timestamp (`trace.clj:442,526–527`) would mismatch
on the following tick. The one-shot route does supply a UUID run-id
(`scripts/futon2/run_tick_once.clj:329–333,273`) and requests trace at :254.
These are source deductions, not live-run observations.

The configured entity is `arxana/stack/futon-v1/leaf/2/2`, prior 1.0, revision
`wm-status-v1` (`holes/labs/wm-contract/machine-accumulation-config.edn:1–6`).
`CONTRACT-machine-model-v1.md:18–29` requires single-entity context and forbids
fallback pooling. It permits this interpretation but **does not decree a
cross-route trial**. Nor does current code enforce population continuity:
`WM:1549–1553` records entity/revision, but :1539–1555 never compares them to
the predecessor; the kernel state (:18–21,33–39) lacks both identities.

## 2. Trial and interleaving

`flight.clj:167–177` starts a target-bound flight with a vector of clicks;
`flight_runner.clj:63–75` runs an opportunity per click. The chosen target is
not the explicitly configured accumulation entity (`WM:1534–1538`).

(i) Each admitted judge observation/posterior pair is one tick in the single
entity/model population. A scheduled tick between clicks is simply the next
increment; the following click consumes its posterior. T counts successfully
accumulated pairs in the chosen finite segment; a typed skipped update is not
a fabricated zero observation. A flight can cite a segment of this history
without owning a second prior.

(ii) A flight owns a trial whose T is its accumulated clicks, publishing at
flight close. An interleaved scheduled tick then needs a rule: serialization
until close, inclusion in the flight despite not being a click, or a merge of
independently accumulated increments. None exists in the cited flight or
adapter code. Last-writer-wins would discard evidence; replaying full
posteriors would double-count priors. This interpretation requires an owner
and close/migration protocol beyond forwarding.

`aif-equations.edn:459–471` declares per-entry o/mu and posterior-as-next-prior,
not routes or flight boundaries. `mathlib4/DarkTower/WarMachine/DirichletLearning.lean:33–38,58–66,95–110`
defines a list and append composition, with no flight semantics. **No ruling
choosing (i) or (ii) found. Recommend (i): one authority per value preserves
the existing recurrence across routes without inventing a flight-local prior.**
Failures are recorded and omit an update, never gate selection.

## 3. Carrier options and recommendation

### A — retain trace authority (recommended, with repairs)

The carrier and write/read sites are those in §1. Healthy empty history can
use the explicitly declared config prior; a nonempty predecessor without a
matrix is migration-required, not permission to initialize. An unconfigured
route currently does nothing when both gate options are absent
(`WM:7351`); trace-only invocation throws identity-missing (:1532), and an
entity with no initialization on cold start throws
`:accumulation-initialization-required` (:1543–1546). The proposed receipt
should distinguish no configured population from no prior; it must not borrow
a4a's prior. Invalid config already has `:accumulation-configuration-invalid`
at `WM:115–117` (slurp/parse failures still need a typed boundary).

Successful receipt proposal under `[:decision :accumulation]`:
`{:status :accumulated :state-sha256 ... :previous-id ... :tick-id ...
:entity ... :model/revision ... :initialization? boolean}`. Hash the exact
serialized accumulation state retained on the trace, naming the serialization;
do not independently rebuild it for the hash. Only describe the update as
published after successful trace append; write failure needs its own typed
receipt. The tick record currently selects decision fields without this key
(`full_loop_runner.clj:731–738`); trace state fields are at `trace.clj:588–592`.

Breaks: identity mismatch, missing/invalid state, population change, unreadable
history, failed publication, and overlapping read/update cycles. Specifically,
`lane_futility.clj:175–183,198–212` locks **append/index update**, not the earlier
judge read. Two readers can both consume P, each pass the local chain check,
and append sibling posteriors; the later record loses the other's increment.
The one-shot has an outer run lock (:322–333), but no such wrapper appears in
full_loop_runner or the scheduled script (search of those files). A common
transaction protocol on all routes is required; the append lock alone is not
proof of serialization. Do not acquire its non-reentrant lock recursively.

Recommendation A includes serialized authoritative read/step/publish (or an
atomic expected-predecessor check that declines the conflicting update with
a typed receipt), and explicit entity/revision continuity checking. A declined
update must not install a new matrix or silently reset on the next invocation.
A receipt-only trace tail must have an explicit unchanged-state/lineage rule;
do not silently search backwards for a convenient successful record. Until
that rule is implemented, a later missing-matrix tail remains a typed
migration refusal. This is safe non-learning, not completed continuous carry.

### B — migrate to one dedicated store

`observation_label_store.clj:56–69,71–105,117–148` provides strict snapshot,
JVM monitor plus file lock, fsync/temp/atomic replace, explicit init, and a
locked read/merge/publish precedent. An entity/revision-keyed accumulator could
use those mechanics, but must replace trace as authority on **every** route;
a trace copy would then be evidence only. First initialization needs the named
config and an explicit migration from existing valid trace state; absent store
must not imply a virgin population when trace history already exists.
Read failures must yield a typed receipt, no reset and no update. If read-once
at judge entry and publication happen separately, a lock/CAS protocol spanning
that interval is still needed: copying the label-store API shape alone does
not solve lost updates. This is the largest change: new owner/schema,
migration, all entrypoints, recovery and tests. No basis to create it alongside
trace authority merely to make the flight patch shorter.

### C — previous click record

`flight_runner.clj:71–75` names clicks, but current tick records select no
accumulation state (`full_loop_runner.clj:731–738`). This needs matrix storage
and a new predecessor reader plus cross-flight handoff. Per-flight cold starts
would fragment the same entity/revision's evidence; a scheduled update between
clicks would be omitted or need merging. Missing/corrupt predecessor must be a
typed absence/refusal, never substitute the config prior on click two. First
ever flight could explicitly initialize; later flights need the prior flight's
posterior, not another 1.0 reset. Scope medium-to-large and semantically new.

**A is preferred by one authority per value and preservation of existing
history.** The single-entity contract is not permission to pool contexts.
Nothing gates a run: failure to accumulate should produce a receipt and leave
selection operational. The present adapter's throws must be isolated to the
accumulation result, not permitted to become a selection refusal.

## 4. Failed-read handling and packet boundaries

The literal catch at `WM:7145–7146` converts exceptions to `[]`; :7147 becomes
nil and :1543–1548 initializes from the config. Proposed accumulation receipt:

```edn
{:status :absent :reason :trace-read-failed
 :error {:class "..." :message "..."}
 :entity "..." :model/revision "..." :tick-id "..."}
```

No `step`, initialization, or new matrix for this tick. Selection continues.
Only a **successful authoritative read establishing empty history** permits
declared initialization. Keep this distinct from `{:status :absent :reason
:no-prior-concentration}` when no prior/declared initialization is available.
Do not serialize the Throwable itself.

**A second swallow is below this catch:** `trace.clj:684–704` explicitly skips
malformed EDN via `::skip`; `trace-files` (:718–726) maps a non-directory to
empty. Merely replacing the judge catch cannot distinguish corrupted/invalid
history from a cold start. Strict authoritative read must reject malformed
records and distinguish a valid absent path from an invalid path/read failure;
tolerant diagnostic readers can retain their documented contract.

Size this as a prerequisite history/transaction packet, not a concealed
one-line fix inside forwarding. It must test unreadable history, malformed
last record, healthy empty history, and no next-tick reset after a declined
update, plus racing publishers and changed entity/revision. Then
C-R17-FLIGHT-I can be the small forwarding-and-receipt packet. If the owner
combines them, acknowledge the wider multi-route scope; forwarding alone
cannot satisfy the stated invariant. No workaround is proposed.

Proposed file-by-file split (new names are proposals):

- Prerequisite: `trace.clj` supplies a strict authority reader and a common
  serialized/expected-predecessor publication seam; coordinate with
  `lane_futility.clj`'s existing append lock rather than nesting it.
- Prerequisite: WM carries typed history status, isolates accumulation
  refusals, validates entity/revision continuity and stable tick identity,
  and constructs the receipt from the state actually published. Keep the
  existing `machine_accumulation/step` as the sole numerical kernel.
- Prerequisite: `scripts/wm_scheduled_run.clj` supplies a stable run ID and
  participates in the same publication protocol around its external write;
  `scripts/futon2/run_tick_once.clj` participates without double-writing or
  weakening its existing outer run lock. Tests pin scheduled/one-shot
  continuity and two readers racing from one predecessor.
- C-R17-FLIGHT-I: `full_loop_runner.clj` loads the named accumulation config
  at default judge construction, forwards it with explicit `:trace? true`
  (and `:trace-dir` for hermetic tests), and retains `:accumulation` in the
  decision select-keys. Configuration-read failure becomes an accumulation
  receipt, not an exception that aborts selection.
- C-R17-FLIGHT-I: WM attaches that receipt to the returned decision; trace
  persists the same receipt. Tests in a new report flight-accumulation
  namespace use real kernel plus temporary trace/tick-record files: click A,
  intervening route B, click C consumes B; receipt hash matches persisted
  state; record-only failure leaves selection intact; no store created.
  No flight needs to run for these tests.

## 5. Consumer scope only

`machine_accumulation.clj:18–21,33–39` carries
`{:support {:observation [...] :state [...]} :concentrations {channel {status n}}
:last-tick ... :previous-tick ...}`. With actual carriers
`observation.clj:11–32` and `belief.clj:37–42`, that is 14 × 7.
`r17_offline.clj:50–62,75–78` consumes an explicit a4a-shaped object or
`(a4a/corpus->concentration ...)`, then `(a4a/reduce-concepts input)`.
`a4a.clj:132–165,167–183,211–225` expects capability IDs, an ordered mission
outcomes vector, and `{capability [concentration ...]}`, with pairwise row
reduction and uniform 0.1 full prior. The registry's model-reduction pointer
is offline (`aif-equations.edn:481–496`). These are different carriers and
prior semantics, not a field rename. A consumer packet needs an explicitly
justified model/coordinate/prior adapter; no BMR handoff is designed here.
A search of `:accumulation-state` in src/scripts/test finds only the adapter,
trace persistence and its test, not another concentration consumer.

## Findings outside this note's changes (unfixed)

- Scheduled route lacks tick identity; my C-R17-I registry first sentence
  overstates that route's operational realization (§1). No registry edit here.
- Trace's tolerant parser can hide corruption, independently of WM's catch (§4).
- Append lock does not serialize recurrence reads; identity/model continuity
  is not enforced; failed/receipt-only tails need explicit lineage semantics (§3).
- Existing adapter exceptions can abort selection (§1), contrary to the
  proposed record-only learning contract.
- Existing observation absence-to-numeric-zero issue documented in C-R17-D §1
  remains outside this carrier packet; no new absence interpretation is adopted.
- No BMR consumer of the trace matrix (§5). Historical registry annotations
  remain untouched. No tests/runtime calls were needed for this note; sources
  and the final Markdown diff were checked. No flights, clicks or live loads.
