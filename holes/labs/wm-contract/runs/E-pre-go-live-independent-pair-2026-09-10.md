# F12: independent preparation checks before apparatus investment

Codex-16, 2026-09-10. Note-only proposal for codex-17 independent review.
No check command or primary experiment executed. Superseding interpretation and
population require review; 06a8d5a8 remains the accepted ARD guard/flat control.
Read `E-pre-go-live-manifest-review-2026-09-10.md` in full.

## Finding

There is a concrete independent pair at **preparation-integrity grain**:

- **F:** compare the actual find implementation file to its reviewed F11
  executable-source SHA256 pin.
- **S:** compare the retained separated-risk mass-binding proof file to the
  binding-proof SHA256 attested by its retained execution certificate.

Both use the existing `/usr/bin/sha256sum --check --strict -` command with a
fixed single-line stdin. They do useful work: a changed implementation cannot
silently inherit historical find evidence, and a replaced binding proof cannot
silently inherit the separated-risk certificate. These are narrowly labelled
prerequisite checks, not a complete F11 gate, proof elaboration, certificate
regeneration, find closure audit, or RUN4 readiness verdict. A failure is useful
preparation evidence and does not authorize a re-pin. This is preparation of
the already selected find/separated-risk chain, not Snatch playout or a new task.

The existing five-pattern carrier **cannot distinguish their order** if both
firings are recorded only as `apparatus/one-authority-per-question`. A viable
order experiment therefore needs the small, explicit instance-carrier revision
below. Do not report an instance-order difference as O4 on the old parent IDs.
This is a viable command pair with a pending semantic/carrier review, not an
already qualified F12 witness.

## Exact commands and authoritative values

Launch directly from the shared firing adapter, without a shell or pipeline.
For both commands argv is:

```text
["/usr/bin/sha256sum", "--check", "--strict", "-"]
```

Cwd `/home/joe/code`. Provide a fresh stdin pipe containing exactly the respective
line below (two ASCII spaces between digest and absolute filename, final LF).
Capture stdout, stderr and process exit separately. Exit 0 with exactly the
expected filename OK is a match; exit 1 with an explicit checksum mismatch for
that filename is a mismatch. Missing/unreadable input, malformed checksum input,
extra result lines or a launch error is CHECK_FAILED, not a mismatch inferred
from exit 1 alone. Neither command runs Git,
Babashka, a generator, Lean, a runtime scorer, or another check.

F stdin:

```text
64c3abb4a5a8655736ffc6391cbf7cbae17a76cdc3e026cfc3e6adcf0da1d9ce  /home/joe/code/futon3/checks/find_organise.clj
```

Authority: `futon3/checks/F11-find-comparison-manifest.edn`,
`:source-basis :pins`, row whose `:file` is `checks/find_organise.clj`.
The file's retained `:status` is `:reviewed-and-pinned`. This single pin comparison
does not satisfy that manifest's additional population/blob/receipt checks;
those remain explicitly unchecked. The execution question is whether this
one retained source pin still describes the source a preparation consumer sees.

S stdin:

```text
305222dea73f92c92bf89166be7423e12d1eae04eb3b40e5067b338911fc6659  /home/joe/code/futon2/holes/labs/wm-contract/runs/separated-risk-certificate/runtime-mass-binding.lean
```

Authority: `futon2/holes/labs/wm-contract/runs/separated-risk-certificate/certificate.edn`,
`:binding-proof-sha256`. Its README identifies the 24 generated mass equalities
and distinguishes historical certificate comparison from fresh regeneration.
This check establishes file identity only. It does not establish source/cohort
currency, regenerate masses, prove the equalities, or check the open observation
bridge. Those omissions are part of the result, not silently green conditions.

Pin the authority documents and exact stdin payloads before either arm; do not
regenerate expected hashes from target bytes. Current authority-document hashes,
obtained by source inspection (not by executing F or S):

| Document | SHA256 |
|---|---|
| F11-find-comparison-manifest.edn | f19842e630a6433c1923b7067c3f6f5174a3c25d9846d7ca3e4f6b48fc96b355 |
| separated-risk-certificate/certificate.edn | 196c7408938610c9ad340b709945c15acec5f5bd9f44cfaacdfb92d754173ff5 |

## Finite inputs and effects, including hidden resolution

**Complete domain input set:** the two authority documents above, the two target
files named in stdin, the two literal stdin payloads, this reviewed interpretation,
and the two authored pattern sources used below. No recursive run discovery,
cohort ledger, certificate directory enumeration, .git state, full DarkTower
census, compiled Mathlib or HOME configuration is a domain input. The check
commands themselves open only their named target and stdin; the adapter's
reviewed setup reads the two authority documents to verify the payloads.
Neither action reads the other's receipt. The shared setup is a prerequisite
of both, not an edge from F to S.

**Executable/hidden runtime set, to freeze for this exact installed binary:**

- `/usr/bin/sha256sum`; ELF interpreter `/lib64/ld-linux-x86-64.so.2`, resolving
  here to `/usr/lib/x86_64-linux-gnu/ld-linux-x86-64.so.2`.
- `/lib/x86_64-linux-gnu/libcrypto.so.3` and `libc.so.6`, with their resolved
  `/usr/lib/x86_64-linux-gnu/` paths. Static ELF inspection finds sha256sum needs
  libcrypto and libc; libcrypto needs libc; libc needs the loader. Do not assume
  this is a statically linked coreutils binary.
- Loader inputs `/etc/ld.so.cache` and presence/absence of `/etc/ld.so.preload`
  (absent in this inspection). Dynamic resolution also probes architecture/
  glibc-hwcaps candidates; freeze the containing loader-search directory entries
  or expose only the named libraries in the later private filesystem. A new
  preload, substituted library, or resolution outside that set refuses the pair.
- Launch with an explicitly constructed environment: `LC_ALL=C`, `LANG=C`,
  `TZ=UTC`, `OPENSSL_CONF=/dev/null`; inherit no other variables. In particular
  no LD_PRELOAD/LD_LIBRARY_PATH/LD_AUDIT, OPENSSL_MODULES/OPENSSL_ENGINES,
  locale paths or profiling variables. `/dev/null` is the explicit empty
  OpenSSL configuration input. No shell startup files or executable PATH search.
- Close inherited descriptors other than stdin/stdout/stderr. Record executable
  and library hashes, operating-system/CPU identity, cwd, environment and path
  resolution. Kernel scheduling and wall-clock time affect cost, not hash truth.
  Treat executable/library or environment drift as apparatus refusal.

This is the finite proposed read/resolve contract, **not a claim that a syscall
trace has established absence of every hidden binary read**. No command may run
before review. The first approved availability diagnostic must verify that the
actual file/process trace stays within this domain/runtime set, including failed
path probes and any OpenSSL provider/config reads. Unexpected access is a design
finding: stop and revise the reviewed census, never silently broaden it. This
small trace is the remaining runtime-census qualification; a huge readiness
transitive freeze is unnecessary for these two commands. Do not describe static
ELF metadata alone as an exhaustive observed I/O audit.

**Writes:** command stdout/stderr pipes and exit status only; no intended file,
source, registry, temporary artifact, ledger or network writes, and no child
process. File atime may change on ordinary read mounts; use read-only private
inputs when building the approved experiment. The later recorder writes its own
private transcripts and output hashes, identically in both arms. Crash/core-file
creation must be disabled and unexpected writes refused. Setup, recorder and
shared-fire module are separate apparatus inputs and require their own bounded
code review; this note does not invent an adapter implementation.

**Symlinks:** both domain targets currently exist as regular, nonsymlink files
and resolve to the exact absolute paths shown. Pin all ancestor resolution and
reject symlink changes or escapes. Authority and target bytes are frozen before
both arms; frozen target bytes need not match the historical authority hash—a
stable mismatch is an honest check result, not apparatus drift. Only a change
from the experiment's frozen target bytes is drift.

## Authored IF/HOWEVER basis and minimal reviewed revision

Both F and S partially interpret `apparatus/one-authority-per-question`:
IF lines 17–18: multiple places can answer a fact; HOWEVER lines 20–21: the
usually agreeing second copy can be mistaken for authority; THEN lines 23–24:
designate the owner and compare deliberate pins, stopping dependent publication
on disagreement. Here the retained manifest/certificate owns the expected hash;
the actual file supplies observed bytes; the check compares them. Mutation gates
and full publication behavior are not claimed executed. A preparation-integrity
report is allowed, while readiness/certificate acceptance remains blocked.

Pattern hash:
`42371c5db7fa2cfbd82688b63aac9ab85518cc0baad7eeb69ccca94aca9bcda1`.

D partially interprets `apparatus/evidence-to-disposition-once`, IF 17–18,
HOWEVER 20–21, THEN 23–24: the preparation summary and experiment report consume
one constructed product of the two receipts, not two independently joined
verdicts. Pattern hash:
`9b07bae8e672031979881d6bacbff4926453b949691ffa99751e226b43e6033e`.

Propose **six typed contextual members** of P, fixed identically in both arms:

1. `[apparatus/one-authority-per-question, find-source-pin]` = F.
2. `[apparatus/one-authority-per-question, risk-binding-pin]` = S.
3. `[apparatus/evidence-to-disposition-once, preparation-integrity]` = D.
4. `[apparatus/pin-moves-with-the-population, supporting]`.
5. `[apparatus/replayable-not-precious, supporting]`.
6. `[apparatus/done-is-observed-running, supporting]`.

This replaces one authority member with two contextual instances and contextualizes
the other four; it is a population/carrier revision, not two secretly different
actions under the old ID. Each instance retains the exact parent pattern and
source span. No new canonical pattern, authored why edge or historical use edge
is created. Proposed authored relation pullback on this selected population is
empty (the reviewed five-parent why basis had no outgoing edges), so organised
edges are exactly empty. Selected=nodes, added/admitted empty. Reviewer must
accept this instance grain as the subject before O1–O3 and the three bindings
are applied to it. Under parent projection F,S,D and S,F,D both become A,A,D:
**no O4 difference on the original parent-pattern carrier**.

This is the smallest honest interpretation revision found here. Assigning F to
one-authority and S to pin-moves would be misleading: S performs no population
mutation/re-pin and does not satisfy that pattern's mutation IF. Replay still
requires a predecessor receipt and stays supporting in this candidate; its
original guard remains intact in the separately retained ARD control. Full
observed-running also remains unexecuted.

## Dependencies, legal orders and shared firing

Let G be reviewed manifest/instance interpretation, immutable inputs, availability
and allowed environment. Initial state has neither F nor S receipt.

- F IF: G and find-source comparison requested; HOWEVER: no F receipt yet.
- S IF: G and risk-binding comparison requested; HOWEVER: no S receipt yet.
- D IF: both complete command receipts exist; HOWEVER: no disposition yet.
- F and S THEN invoke their exact command/payload and return a receipt. Neither
  guard asks whether the other check passed. A stable mismatch still leaves the
  other independent check useful. A launch failure/drift terminates or refuses
  the attempt and cannot manufacture a successful changed-order witness.

Graph: `G→F`, `G→S`, `F→D`, `S→D`; no F→S or S→F edge. These are task
prerequisites, not authored why edges. Enumerating all six permutations of F,S,D
under the two D prerequisites leaves exactly **FSD and SFD**.

Ranks in the six-member order above:

| Arm | Rank vector | Legal successful order selected by unchanged guards |
|---|---|---|
| Baseline: explicitly diagnostic find-first | [1,2,3,4,5,6] | FSD |
| Intervention: explicitly task-local risk-first | [2,1,3,4,5,6] | SFD |

No observed practice or optimality is asserted for either preference. Task-local
risk-first simply asks whether prioritizing the separately outstanding binding
check changes actual action order; it need not improve preparation quality.
The adapter must call the existing `fo/fire` over these instance IDs and record
actual command receipts, never copy rank order into acting order. Both guards
are initially true independently; after one receipt only its guard becomes false.
Two top-level commands and three firings per arm; five-second timeout per command,
no retry. Limits are identical. Actual launch failures, budget exhaustion or
unqualified observations refuse the witness, not a reason to adjust the budget
for only one arm.

## Outcome, denominators, controls and review stop

One primary preparation episode per arm, denominator 1. Its transcript has the
F, S and D firings; diagnostics/controls have separate episode IDs and no primary
weight. Score = 1 if both commands produce interpretable match/mismatch results
and D accurately reports their product on the frozen basis; 0 for a completed
but malformed/inconsistent result. Missing primary evidence or apparatus drift
refuses the pair rather than inventing a score. Also report match bits separately;
CHECKED-MISMATCH is not a green readiness verdict. This measures completed useful
integrity checking, not canonical G or checksum success as preparation quality.
Expected score is flat for stable valid inputs. The potential O4 consequent is
**measured instance acting order only**, with all six fields and command provenance.

D's closed cases: BOTH_MATCH, FIND_MISMATCH, RISK_MISMATCH, BOTH_MISMATCH,
CHECK_FAILED, REFUSED_BASIS. One constructor; summary/scorer both dispatch totally.
No blame consumer. A mismatch stops any dependent readiness/acceptance publication;
this report may communicate the mismatch. Broader readiness stays unknown even
when both match.

Retain the old ARD guard control; copied changed-rank/flat-order/flat-score control
must fail O4. New controls: flip an expected digest only in a separate diagnostic
copy and require a mismatch; missing target must fail; delete a command receipt
and refuse the primary pair; project F/S to parent IDs and explicitly show no
order difference. Do not count these synthetic faults as observed failures.

**Review decision before apparatus:** accept/reject the useful narrow pair and
six-instance grain, its explicit preference change and score. If only the
unchanged five-parent carrier is admissible, this pair is precisely insufficient:
its order difference disappears on projection, and no two independent executable
members have been justified within those five interpretations. Do not build an
adapter on that basis. If accepted, qualify the small binary I/O census and pin
the finite inputs/adapter in a successor manifest with independent review before
primary execution. No large regeneration snapshot or replay adapter is commissioned
by this note.

## Inspection evidence and exclusions

Only source/availability inspection was performed: parsed the two authority EDNs,
read the candidate wrappers and reviewed patterns, inspected target existence and
symlink resolution, read ELF program/dynamic headers, and enumerated six abstract
orders (result FSD/SFD). Did not run sha256sum on either target or call any proof,
certificate or readiness check. The initial `ls` correctly reported absent
`/etc/ld.so.preload` with exit 2; that absence is recorded, not a successful file
read. No execution scores or command results are claimed.

Rejected larger candidates: `checks.fold-c-witness` calls Lean and regenerates
certificates; `checks.preference-risk-receipt` calls Lean and writes output;
`find_snatch_evidence/-main` writes evidence files. The older
`f11_non_self_certifying_check.bb` asserts find is still sorry and therefore is
not a current find-closure preparation check. These are specific inspected
exclusions, not an exhaustive impossibility claim about the entire workspace.

One Markdown file only. `git diff --check` passes; no Clojure/Lean source change,
ledger, registry, live data, service, or command execution. Independent review is
still required; no O4 exercise or F12 closure claimed.
