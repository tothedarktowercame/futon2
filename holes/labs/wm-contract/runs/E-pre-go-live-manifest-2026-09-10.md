# F12 pre-execution manifest — 2026-09-10

Status: **proposed for codex-17 independent review; no primary execution**.
Machine-readable parameters: `E-pre-go-live-manifest-2026-09-10.json`.
This completes the bounded design specification, not its execution apparatus,
input freeze or F12 closure. Those remaining dependencies are explicit below.
No choice is reopened for Joe, and no Claude review is requested.

## Basis and purpose

Read the entire `E-pre-go-live-experiment-2026-09-09.md`, including the corrected
pin classification, dated review response and diagnostic-only dispatch
clarification. Read `futon3/checks/F12-O4-witness-discovery.md` and both independent
review revisions, b63a4b69e7d72502386d8b87edbbe39abcd294a7 and
a376e4043369167ec8b491a77ddc4f9695a6d991. The interpretation correction is settled;
this asks for review of the newly concrete design, not another interpretation
approval loop. The earlier post-repin probe is one diagnostic action and has no
O4 claim. Its historical READY is not this manifest's starting observation.

Useful output: one evidence-backed preparation disposition for the find and
separated-risk prerequisite chain, showing the actual owning readiness answer,
replay agreement and blockers. It does not adopt config, accept certificates,
prove activation, settle organise or authorize a run. Today's task selector is
Joe; future outer-loop selection remains unimplemented by this packet.

## Fixed cascade and intervention

Use the five members in the JSON, in its explicit `precedence_index` order.
Selected = nodes = those five; added-by-organise and admissions are empty.
The independently reviewed authored why basis has no outgoing edges from any
selected member. Thus organised edges must be exactly empty, not merely a
reachable subset. Preserve O1–O3 and selected/authored/admission input bindings.
Do not turn the three excluded prose tokens, references or see-also into edges.
Freeze the library reader and five source hashes in both arms; THEN citations
are lines 23–24. Changes to precedence are task-local interpretation, not
new authored graph edges.

Baseline ranks `[2,4,3,1,5]`; intervention ranks `[1,4,3,2,5]`. Lower is earlier.
The baseline is an **explicit diagnostic replay-priority control**. It is not
an observed workflow and is not claimed to be bad. The intervention exchanges
only authority/replay priority. No member, rule body, guard, source, score or
budget changes between arms; no ties need an identifier-order convention.

This design deliberately preserves a consequential prerequisite: a replay
cannot compare to an authority receipt that does not yet exist. Therefore the
anticipated firing order in BOTH arms is authority, replay, disposition. That
is an anticipated constraint, not measured output. If it holds, unchanged order
and score with changed precedence must fail O4. Report **unexercised**, not a
conformant exemplar. Do not weaken the guard to obtain an attractive witness.
The purpose is real preparation work plus an honest test of the proposed
precedence intervention; it is not guaranteed to discharge F12.

## Actual command allowlist and effects

Both authority and replay invoke this exact argv, through a THEN passed to
`find-organise/fire`, cwd `/home/joe/code/futon2/holes/labs/wm-contract`:

```text
/usr/bin/timeout --signal=TERM --kill-after=5s 60s /usr/local/bin/bb run4_readiness.bb --summary
```

The second invocation is a fresh execution, not reuse of the first stdout.
The authority reader owns all nine readiness facts. `--summary` exits 0 for a
valid blocked answer too; exit 0 must never be interpreted as READY.
The parser must accept exactly nine unique named lines in `line-order` and one
consistent VERDICT, preserving the original stdout/stderr. Unknown, duplicate,
missing or inconsistent fields are CHECK_FAILED, not absent green lines.

The command is read-only **with respect to repositories and live state**, not
free of temporary writes. Source inspection of the complete actuator search
shows these reachable descendants:

- `run4_readiness.bb`: Git log/status/ls-files reads; temporary-directory
  regeneration using `bb u49_route_transcribe.bb <temporary-run-dir>` once per
  awaiting certificate and `bb run4_prereg_transcribe.bb <temporary-manifest-dir>`;
  comparison and deletion of those temporary outputs.
- U49 producer: Git reads; writes its six output artifacts under the supplied
  temporary output directory. Its ledger-writing `deposit!` branch requires
  `--deposit`, which the caller does not supply and this manifest forbids.
- Preregistration producer: reads certificates, source modules and the DarkTower
  Lean-file census; writes six artifacts under its supplied temporary directory.
- Readiness's `--probe` branch invokes Lake and writes a probe receipt; `--emit`
  writes repository artifacts. Neither branch is allowed or reached by this argv.

Do not add `--emit`, `--probe`, `--negative`, `--deposit`, `F2_SKIP_REGEN`, or
source overrides to make a check pass. No shell pipeline may obscure exit codes.
The two top-level invocations are the command budget per arm; internal generator
counts are recorded separately and fixed by the frozen certificate population.
Each invocation has 60 seconds plus 5 seconds kill grace. No automatic retry.
At most three firings per arm and one planned primary pair. Timeout or signal
is an apparatus outcome, not a readiness result or mathematical failure.

The default reader uses canonical paths derived from user.home. An arbitrary
worktree cwd therefore does NOT isolate its inputs. No worktree may be loaded
into a shared JVM. Use an independently reviewed filesystem isolation plan
that preserves the expected paths, Git queries and frozen source bytes, allows
writes only to private temporary/output storage, and has no live service access.
The manifest does not pretend this facility has already been provisioned.

## Shared firing protocol and exact guards

One private process per arm, fresh empty state, same reviewed manifest/freeze.
Resolve the three partially executable member interpretations below; retain
pin-maintenance and observed-running as supporting/unexecuted. No unresolved
required member may be silently dropped. Rule IDs are the full pattern IDs.

| Member/action | IF and HOWEVER guard | THEN/effect |
|---|---|---|
| one-authority-per-question / A | requested and no A receipt; all basis guards pass | Execute authority argv, capture receipt, parse readiness answer |
| replayable-not-precious / R | A receipt exists, no R receipt; A is a valid command answer; basis unchanged | Execute replay argv independently; capture receipt and compare normalized readiness vector to A |
| evidence-to-disposition-once / D | A and R receipts exist, neither drift-refused; no disposition | Construct the single closed disposition value from those receipts |

Use `fo/fire rules (fn [rule] (rank (:id rule))) state` for every transition.
IF/HOWEVER are pure; only the selected THEN may execute a command. Record the
returned rule ID and returned receipt before updating state. Repeat until a
disposition, explicit failure/refusal, or the firing budget is reached. A
synthetic `:done` flag or planned order is not a command receipt. If a command
fails, the outer attempt protocol constructs the corresponding terminal
failure disposition once, without falsely recording a D firing.

Receipt fields: attempt/pair/arm/round identity; primary flag; rule ID and source
span/hash; exact argv/cwd; start/end monotonic times; exit/signal; raw stdout and
stderr plus hashes; before/after input manifest digests; normalized nine-line
vector and verdict or parse error. Retain command process identity and
termination evidence. The existing readiness implementation deletes its scratch
artifacts, so this interpretation claims stdout/stderr hashes and the reader's
reported comparisons only, **not retained hashes of unobserved scratch files**.
If resulting generator-artifact hashes are required by review, the recorder must
capture them in the isolated filesystem before cleanup, with that instrumentation
reviewed identically for both arms. No source patch to do that is in this packet.

## Authorities, pin classes and one disposition constructor

| Fact | Authoritative reader/comparator | Mutation owner/gate (not executed) |
|---|---|---|
| Awaiting certificate population | readiness `:certificates`, actual awaiting certificates vs manifest assertion runs | certificate/acceptance owning gate; population and its pin same commit |
| Certificate definitions and preregistration module | readiness `:definitions-intact`, verbatim blocks and module bytes | source owner plus reviewed source re-attestation slice |
| Control-map basis | readiness `:wiring-pin`, bytes and last-content commit | control-map owner; source attestation, plus population timing if membership changes |
| Extracted run basis | readiness `:run-pins`, trace hashes and tracked/clean queries | run evidence owner; never alter a precious trace for this experiment |
| Reproduction | readiness `:regenerates`, owning producers' generated-vs-retained bytes | generating source owner; no hand-editing generated outputs |
| Probe currency | readiness `:lean-probe`, source hashes, axiom/build/sorry receipt | owning probe after reviewed source edit; same source re-attestation slice |
| Contract source and hole status | readiness `:hole-open`, contract `source.git-sha` vs last Holes content commit and declaration kind | contract emission owner; source re-attestation, not mathlib HEAD comparison |
| Closability | readiness `:closability-audit`, audit contract authority and required rows | audit generator owner after contract changes |
| Invalidators | readiness `:invalidators`, measurements and delegated wiring result | owning manifest generator; mixed reports must satisfy both timing rules |
| Replay agreement | this preparation constructor, normalized A vs R on identical frozen inputs | experiment author/reviewer; no alternate readiness authority |

Per-field classification for outputs in this experiment:
`members/selected/admissions/edges/precedence_index` and their hash are population
pins (fixed; no mutation). `source_pins`, argv/driver hash, input-file hashes,
Git-query replies and environment digest are source/runtime attestations (fixed).
Round IDs, command outputs, exits, timestamps, score and disposition are attempt
observations, not maintenance of source or population. The final report contains
all three classes; changing membership requires same-commit population maintenance,
while changing attested source requires its owning same-slice re-attestation.
Neither licenses silently updating one arm. No owning generator output is
published by this experiment; temporary regeneration is comparison only.

Construct exactly one value, precedence in this order:

1. REFUSED_BASIS: missing/drifting input, unreviewed manifest or isolation failure.
2. CHECK_FAILED: launch/timeout/nonzero exit, invalid output or incomplete attempt.
3. EVIDENCE_CONFLICT: valid A and R disagree on the frozen basis.
4. PREPARATION_BLOCKED: they agree on an owning BLOCKED answer.
5. PREPARATION_CLEAR: they agree on an owning READY answer.

Two consumers dispatch totally over that enum: operator preparation summary and
experiment scorer. The summary respectively reports refused, failed, conflicting,
blocked prerequisites, or clear preparation with external acceptance/activation
still outstanding. The scorer respectively yields no eligible comparison, 0,
0, 1, or 1 for the sole primary round when its receipt exists. A missing
primary receipt makes the pair ineligible instead of supplying a synthetic score. Both consumers consume the constructed
value and referenced receipts, never independently re-join raw authorities.
Exhaustiveness check: consumer dispatch keys equal the five-element enum exactly;
plant an extra enum value and require rejection. Blame consumer is not applicable.
Observed-running remains supporting/unexecuted: no standing comparator or live
activation observation exists in this packet.

## Denominators, score and controls

Each arm has three planned transcript rounds: A, R, D. **Only R is primary**.
The full acting-order field is the ordered vector of actually fired pattern IDs
over all transcript rounds, including terminal partial attempts. Score has fixed
primary denominator 1 per arm: successful stable, valid replay = 1; attempted
failed/conflicting replay with a retained primary receipt = 0. Missing primary
receipt or refused basis invalidates the pair;
do not impute a score and claim O4. A correct blocked result scores 1 because
this task is to establish an honest preparation disposition, not make it green.
Report nine-line readiness vector, number of checks actually invoked on the
frozen basis, stale attempts, unresolved prerequisites and elapsed cost alongside
this deliberately narrow score. Cost is descriptive, not a second O4 score.

All six ruled fields must be present with provenance: the two frozen rank
vectors, two measured acting vectors, and two primary scores. O4 is exactly
precedence change ⇒ order change OR score change. Empty/invalid primary evidence
refuses comparison. Historical authority probes, controls and apparatus reruns
retain their own transcript IDs but are outside this pair's primary denominator;
never blend them into a manufactured before/after history.

Required diagnostic controls, in separate copies, never changing live inputs:

- Flat effect: change only claimed precedence on a copied measured receipt,
  preserve order and score exactly, and require `fo/o4-precedence-governance`
  false. Also reject precedence claims not bound to the executed rank vectors.
- Guard control: replay-priority still cannot fire R before A. If both valid
  arms have A,R,D and identical scores, report O4 unexercised/false.
- Drift: alter one byte of a frozen private input or its membership listing;
  refuse before a command. Alter between commands and require pair refusal,
  preserving any partial transcript rather than re-pinning.
- Missing primary / parser controls: missing R, duplicate readiness line,
  unknown line or missing verdict must not become a successful score.
- Enum control: a sixth disposition without both consumer cases must fail.

Preparation attempts are replayable; live runs are outside scope and untouched.
If the apparatus dies, preserve the partial record, mark apparatus-caused, and
start no replacement primary automatically. A later authorized diagnostic rerun
uses a new ID and remains outside the original denominator. Changing the frozen
basis creates a new preregistration and independent review, not a resumed arm.

## Freeze and exact remaining execution dependencies

The JSON pins every directly inspected driver/pattern/review source. It is
**not** a transitive readiness input freeze. Before primary execution, attach an
independently reviewed annex enumerating bytes and directory membership for:

- all `runs/*/certificate.edn` discovery candidates, selected certificate artifacts,
  produced-with environment maps, extracted traces, run README/conformance sources,
  referenced prior U49 control/table sources and input record discovery directories;
- six F2 preregistration artifacts, axiom receipt, closability audit, Holes.lean,
  actual preregistration module, contract JSON and control-map source;
- every DarkTower Lean file/name traversed by the preregistration census;
- transitive code/config inputs and exact Git query results: last content commits,
  tracked membership, cleanliness, and producer Git metadata;
- executable hashes/versions, cwd, user.home, locale, temporary-root mapping and
  relevant environment. Reject ambient F2/U49/FUTON_WM_TRACE overrides; retain only
  frozen per-certificate produced-with entries after path/flag review.

Pin absence as well as presence. Resolve symlinks and reject escapes from the
reviewed input set. Both arms must use the same immutable filesystem snapshot
and Git metadata, not sequential mutable checkouts checked only at endpoints.
Freeze once before either arm and bind its digest to the independent review.
No historical green answer fills this annex.

Exact unresolved items for codex-17:

1. Review the concrete replay-priority diagnostic baseline, precedence vectors,
   guards, score and allowable partial interpretations. The replay guard predicts
   no O4 exercise; accept that useful negative result or commission a separately
   reviewed genuinely independent action interpretation. Do not remove the guard.
2. Provision and pin the complete transitive snapshot/isolation annex above.
   This packet checks command availability and paths, not that environment.
3. Implement/review the bounded shared-fire adapter and total constructor against
   this manifest, including all controls and scratch-artifact capture if required.
   `fo/fire` exists; a generic interpreter for this JSON does not yet exist.
4. Pin the manifest/adapter/freeze SHA256s in an independent review receipt before
   either primary arm. No runner invocation is authorized by this author packet.

## Checks performed for this packet

`/usr/local/bin/bb` exists and reports `babashka v1.13.219`; `/usr/bin/git` and
`/usr/bin/timeout` exist. The two exact argv targets exist. Reviewed the readiness
main dispatch, its regeneration subprocesses, both producer write sites and
U49's separate deposit branch. The allowlist excludes all repository-write and
Lake flags. Source hashes are recorded in JSON; no readiness command, primary
pair, generator, Lean check or live call was executed. JSON parse, pin equality,
precedence permutation and path-scoped diff checks were run. Markdown/JSON only;
Clojure lint and parentheses gates do not apply to production sources.

Validation correction: the first availability assertion used the wrong argv
indices and failed; the corrected check locates bb at index 4 and the script
at index 5 and passes. No command was launched by either assertion.
