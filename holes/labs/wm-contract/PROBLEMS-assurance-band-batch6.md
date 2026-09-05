# Per-node problem dossiers — batch 6: assurance band

**Scope and numbering.** This is discovery, not a War Machine change.  R9,
R10, R12, R20, and TRACE use `control-stages.edn` numbering and all occupy its
`:assurance` band (`p4ng/empirics-futon/control-stages.edn:39-44`).  R9 and R12
are divergent: contract R9 means named validation properties while catalogue
R9 means no self-certification; contract R12 means hyperparameter inference
while catalogue R12 means two-layer calibration
(`p4ng/R-concordance.md:30-33,56-67`).  TRACE has no R number, catalogue
pattern, or contract criterion; those missing joins are stated rather than
invented.

## R9 — No self-certification

### The problem it solves

Control-stage R9 solves who may certify a result: the roster places **“No
self-certification”** in ACT's assurance band
(`p4ng/empirics-futon/control-stages.edn:43`; control-stage/catalogue
numbering).  Catalogue R9 requires verdicts to move only on independently
made, birth-tagged evidence, with untagged evidence counting as nothing
(`p4ng/sec-catalog.tex:174-178`; catalogue R9).  Contract R9 instead specifies
four quantitative validation properties and records seven tests for them
(`docs/futon-aif-completeness.md:243-258`; contract R9, a different node).
The equation registry classifies control-stage R9 as plumbing and supplies no
equation (`holes/labs/wm-contract/aif-equations.edn:472-473`; control-stage
R9).  The control-stage problem is therefore architectural independence: a
claimant cannot manufacture the evidence or issue the check that closes its
own claim.

### Residual problems it does not solve

- `:implementation-flaw` — The process census marks control-stage R9's
  `checked` cell named-only: no node-linked refusing check records a second
  checker (`holes/labs/wm-contract/ALIGN-rnode-process-census.md:84,98-101,
  138-139`).  Naming independence without enforcing checker identity does not
  prevent self-closure.
- `:both` — Contract R9's validation harness and catalogue R9's independence
  gate are different mechanisms, and the concordance says the contract node
  has no catalogue counterpart (`p4ng/R-concordance.md:56-61`).  The numbering
  needs a declared model join and running code must route each check to its
  proper authority.
- `:implementation-flaw` — The conventions note records T5 reflexivity failure
  as a distinct gate trap (`holes/N-process-trap-recording-conventions.md:148-162`),
  while the process specification requires every gate to satisfy its own
  property or record an exemption and says no such WM checker exists
  (`holes/problems/P-assured-process.md:50-65`).  The assurance mechanism does
  not yet assure itself.

**Would we know? — component properties exist; checker independence is
missing.** `clojure -M:test -n futon2.aif.r9-named-validation-test` checks
contract R9's quantitative properties (`docs/futon-aif-completeness.md:249-258`).
Missing is a process test that refuses author/checker identity and records the
independent return before closure; the nearest census is
`ALIGN-rnode-process-census.md:138-139`.  Absence search: `rg -n
"R9|self-cert|checker-id|author-id|independent" /home/joe/code/futon2/src
/home/joe/code/futon3c/src/futon3c`.

## R10 — Scheduled entrypoint

### The problem it solves

R10 solves unattended liveness: the roster places **“Scheduled entrypoint”**
in PERCEIVE's assurance band (`control-stages.edn:39`; control-stage
numbering).  Contract R10 requires recurring execution without operator
intervention plus a queryable trace (`docs/futon-aif-completeness.md:260-268`;
contract R10).  Catalogue R10 sharpens liveness to observable state change,
not a scheduler firing or painting green no-op rows
(`p4ng/sec-catalog.tex:156-158`; catalogue R10).  The equation registry gives
R10 no equation and classifies it as plumbing
(`aif-equations.edn:472-473`; control-stage R10).  R10 therefore solves
repeatable entry into perception while refusing to confuse wakeups with useful
work.

### Residual problems it does not solve

- `:implementation-flaw` — The operational record says the entrypoint is only
  scheduled-execution-ready and no cron is installed
  (`docs/futon-aif-completeness.md:266-278`).  The apparatus exists, but the
  recurring live condition does not currently hold.
- `:implementation-flaw` — The census finds all seven handoff stages absent for
  R10 and names commissioned plus dispatched as merge blockers
  (`ALIGN-rnode-process-census.md:85,140-141`).  A scheduled run lacks a
  process commission identity and dispatch receipt tied to the node.
- `:undetermined` — Catalogue evidence says the schedule was correctly disabled
  after a stop-line because no-op schedules must not count as live
  (`p4ng/sec-catalog.tex:156-158`).  Whether re-enabling is appropriate depends
  on evidence of possible state change, not merely installation readiness.

**Would we know? — entrypoint check exists; durable liveness/process identity
is missing.** The one-shot `clojure -M:wm-scheduled 14` check and its recorded
trace witness are documented at `docs/futon-aif-completeness.md:266-268`.
Missing is a repeated-run check that binds commission and dispatch IDs and
requires observable change before claiming liveness.  Absence search: `rg -n
"R10|commission|dispatch|schedule|state.change" /home/joe/code/futon2/scripts
/home/joe/code/futon2/src /home/joe/code/futon3c/src/futon3c`.

## R12 — Two-layer calibration

### The problem it solves

Control-stage R12 solves separating internal consistency from externally
witnessed value evidence: the roster places **“Two-layer calibration”** in
BELIEVE's assurance band (`control-stages.edn:40`; control-stage/catalogue
numbering).  Catalogue R12 requires Layer 1 and Layer 2 to retain distinct
labels, and permits only independently witnessed Layer 2 to clear value
(`p4ng/sec-catalog.tex:297`; catalogue R12).  Contract R12 instead specifies a
slower outer loop that treats inner-loop hyperparameters as hidden state
(`docs/futon-aif-completeness.md:286-294`; contract R12, a different node).
The equation registry classifies control-stage R12 as plumbing and gives it no
equation (`aif-equations.edn:472-473`; control-stage R12).  The control-stage
problem is therefore provenance-preserving calibration: cheap self-consistency
may inform the record but cannot impersonate external value evidence.

### Residual problems it does not solve

- `:implementation-flaw` — The census finds every process stage absent for
  R12 and identifies returned plus checked as merge blockers
  (`ALIGN-rnode-process-census.md:86,142-144`).  Calibration artifacts are not
  process-bound to their commission and independently checked before admission.
- `:both` — Contract and catalogue R12 are two different second loops, as the
  concordance explicitly records (`p4ng/R-concordance.md:63-67`).  A model join
  must distinguish hyperparameter inference from evidence-layer calibration,
  and implementation routing must preserve that distinction.
- `:implementation-flaw` — Even contract R12's apparatus leaves two action
  classes at prior pending scheduled evidence and leaves per-channel
  hyperparameters static (`docs/futon-aif-completeness.md:292-294`).  Apparatus
  presence is not accumulated calibration.

**Would we know? — missing at returned-evidence authority.** Existing
outer-loop checks are named by `docs/futon-aif-completeness.md:290-299`, and
catalogue R12 reports deterministic fail-closed Layer-2 evidence
(`p4ng/sec-catalog.tex:297`).  Missing is a single check binding a returned
calibration artifact to its commission, independent checker, and L1/L2 label.
Absence search: `rg -n "R12|layer-1|layer-2|returned|checker|calibration"
/home/joe/code/futon2/src /home/joe/code/futon2/test
/home/joe/code/futon2/scripts`.

## R20 — Interoceptive tripwires

### The problem it solves

R20 solves observation of the machine's own failure trajectories: the roster
places **“Interoceptive tripwires”** in EVALUATE's assurance band
(`control-stages.edn:41`; control-stage/catalogue numbering).  Catalogue R20
requires invariant tripwires around live phase boundaries because task-level
observation cannot see wedges, livelocks, mixed code images, or lost wakes
(`p4ng/sec-catalog.tex:369`; catalogue R20).  The completeness contract has no
R20 criterion; the concordance marks R20 native to the catalogue
(`p4ng/R-concordance.md:41`; explicit missing contract source).  The equation
registry lists R20 as plumbing, not an equation-bearing AIF quantity
(`aif-equations.edn:472-473`; control-stage R20).  R20 therefore solves
fail-closed self-observation: detect invalid machine trajectories, record the
refusal, park, and surface the condition rather than narrating it forever.

### Residual problems it does not solve

- `:implementation-flaw` — The census marks R20 checking named-only and every
  other lifecycle cell absent; checked and surfaced directly block the merge
  (`ALIGN-rnode-process-census.md:87,102-104,145-146`).  No R20-linked checker
  refuses the transition and no discharge reaches an attended surface.
- `:implementation-flaw` — The conventions define T7/T8 as catastrophic
  single-response and a watcher that observes then defers
  (`holes/N-process-trap-recording-conventions.md:176-195`), while the process
  record says neither has a WM checker (`holes/problems/P-assured-process.md:50-53`).
  These are precisely the monitor-failure territory R20 is meant to observe.
- `:aif-extension-needed` — R20 has no contract criterion at all
  (`p4ng/R-concordance.md:41`), so its catalogue tripwires lack a contract-side
  checkable form.  The assurance model must declare what constitutes detection
  and surfacing before implementation can claim completeness.

**Would we know? — missing.** There is no R20 test namespace or contract
criterion identified by the census; the nearest required behavior is a
refusing result plus real discharge (`ALIGN-rnode-process-census.md:145-146`).
A concrete check would inject each known invalid trajectory, require refusal,
and observe a surfaced bulletin.  Absence search: `rg -n
"R20|tripwire|livelock|mixed.code|lost.wake|surfaced" /home/joe/code/futon2/src
/home/joe/code/futon2/test /home/joe/code/futon3c/src/futon3c`.

## TRACE — WM trace store / route ledger

### The problem it solves

TRACE solves durable attribution of a WM tick to its recording route: the
roster places **“WM trace store (route ledger)”** in ACT's assurance band and
names the write basis (`control-stages.edn:44`; control-stage TRACE).  Runtime
trace records preserve observations, beliefs, scores, ranked actions,
decisions, mode, and optional version provenance
(`src/futon2/aif/trace.clj:1-44`; runtime TRACE).  Immediately before writing,
the WM attaches `:TRACE` to `:wm/route`
(`scripts/futon2/report/war_machine.clj:6750-6759`; runtime route ledger).
TRACE has no catalogue pattern or contract criterion, and the equation
registry classifies it as plumbing (`aif-equations.edn:472-475`; explicit
missing-source facts).  Its scoped problem is thus making the tick and its
route inspectable after execution, not proving that all process stages
occurred.

### Residual problems it does not solve

- `:implementation-flaw` — The census credits only `recorded`; commissioned,
  dispatched, parked, returned, checked, and surfaced are absent
  (`ALIGN-rnode-process-census.md:88,105-109`).  A tick record cannot stand in
  for a handoff lifecycle record.
- `:implementation-flaw` — TRACE's blocking process cell is `surfaced`: route
  recording exists but lacks a process-dispatch identity and discharge
  (`ALIGN-rnode-process-census.md:147-149`).  Persisted evidence that nobody is
  notified about does not complete the lifecycle; the conventions make this
  exact distinction under T6 (`holes/N-process-trap-recording-conventions.md:163-175,283`).
- `:both` — TRACE has neither catalogue pattern nor contract criterion, while
  its implementation already makes consequential choices about what survives
  stripping (`src/futon2/aif/trace.clj:84-148`).  A declared assurance contract
  and a runtime conformance check are both needed to say which evidence the
  route ledger must retain.

**Would we know? — recording exists; lifecycle/surfacing agreement is
missing.** `trace/write-trace!` appends the route-tagged record, as verified by
the census (`ALIGN-rnode-process-census.md:105-109`).  Missing is a check that
joins the trace to dispatch identity, independent checking, and a surfaced
discharge while refusing closure if any is absent.  Absence search: `rg -n
"TRACE|dispatch-id|checked-by|surfaced|bulletin|discharged-at"
/home/joe/code/futon2/src/futon2/aif/trace.clj
/home/joe/code/futon2/scripts/futon2/report/war_machine.clj
/home/joe/code/futon3c/src/futon3c`.

## Scope limits

I read the five accepted dossier forms, control-stage roster, concordance,
catalogue patterns, completeness criteria, equation registry, process census,
process specification, recording conventions, and TRACE's writer/route sites.
I did not inspect live scheduler state, execute a tick, replay Campaign S,
audit every test assertion, resolve the R9/R12 numbering policy, invent a
contract criterion for R20 or TRACE, or test operator notification delivery.
I did not run the War Machine or its tests: the `would-we-know` commands name
existing checks or concrete missing checks, not results obtained today.
