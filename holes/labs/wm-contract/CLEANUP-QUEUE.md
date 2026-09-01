
## C455 — owner review of C451/C452/C453: the wiring is largely there, the drawing is not (my review)

**All three pass the gate.** The bar was that every claim carry `file:line`, a run-record field, or the
literal words "not found". All three met it, and C453 exceeded its packet by fetching the sources rather
than reporting them unavailable.

**What I checked, rather than read.** I opened two of C451's pointers myself. `src/futon2/aif/selection_gain.clj:1-15`
does say *"It is not Friston's variational policy precision: its learning signal is realised controller
performance, not expected free energy under the policy posterior"* — so the report's claim that R7's Π does
not feed τ is supported by the code's own docstring. And `scripts/futon2/report/war_machine.clj:4537` reads
`route4 (route-tag route3 :R5 "futon2.aif.efe/rank-actions")`, confirming that the recorded R8→R5 hop is a
**route tag**, not a data dependency.

### The correction this forces on my own C450

In C450 I reported the run records as showing "3 of 21 declared edges fire" and treated that as the state of
the wiring. **That number measures route instrumentation, not data flow.** Both C451 and C452 establish it
independently: the recorded route is a coarser sequence (`R2→R7→R3→R8`) than the imports, and C452 found
**no exact traversal for any of the ten theory edges in either run record** while nine of them are realised
in code. So the run record cannot be read as an inventory of which wiring fired, and I read it that way.

The honest restatement: the machine's route instrumentation tags 9 hops of which 3 match the drawn map. That
is a fact about the tags.

### What the three reports establish

**C452 — nine of ten undrawn theory edges are realised in code.** The priority chain in particular:
`o` and `μ` both reach `compute-prediction-error` (`war_machine.clj:4221-4223`, `:4368-4379`), and the
precision-weighted error drives the belief update (`:4392-4431`, `belief.clj:1023-1036`). So R8 does form ε
and R3 does update from it. **Figure 5A is incomplete; the machine is not unwired.**

**One genuine hole: R8→R17.** `F` is computed at `war_machine.clj:4450-4452`, placed in the judgement at
`:4753`, and never read by BMR, which builds its own concentration vectors at `src/futon2/aif/a4a.clj:115-131`.
C453 independently shows why the registry row was wrong to expect otherwise: BMR needs the full posterior plus
full and reduced priors, not a scalar `F`.

**C451 — of six drawn edges with no equation across them, none is a formalism departure.** One is route
sequencing (R8→R5; F is not an input to R5's score), one is a shortened drawing of an implemented path
(R2→R3, which goes through ε and Π), and four have no realised reader found. The precision-modulated-temperature
hypothesis the TN raised is **not supported**: τ uses the engineering `selection-gain`, which disclaims being
variational policy precision.

**C453 — four of six citations are unverified as recorded**, under a rule that treats a partial match as
unverified for publication. The registry's channel equations are local specialisations of Buckley et al.
rather than equations in it; `Q(π) := softmax(ln E − G/τ)` is not Da Costa eq. (10), which omits both `E` and
temperature. **`:parr2022` is verified and is the source that actually writes the form the registry uses**
(eq. B.7, B.9, pp. 246–247) — and no equation row cites it. That is the cheapest repair on the table.

### Coordination

Belled `claude-1` with C453's table, since Joe said they are fetching the same papers. C454 (query 3c) is
dispatched to `wm-organization`, carrying C451's τ finding and C453's item-8 finding so it does not re-derive
them.

### One defect in my own dispatch

`C448` names two different deliveries — my sweeper-stop diagnosis and a TN edge-review discovery committed as
`96917d8`. I issued the number without checking it was free. The sweeper report is
`C448-sweeper-stop-diagnosis.md`; the other is `C448-TN-edge-review-discovery.md`. Recorded rather than
renamed, since both are committed and cited.

## C391 — the adapter check pins occurrence, not correspondence (my fix, owner review of C379)

**Verified C379 (`9e3da2f`) independently.** What I checked, and what it establishes.

**No Lean file is touched in the commit.** The diff is the validator, 27 receipts, one test file and a
report — so *"rebuilding restored elaboration without weakening any theorem"* is supported: no theorem was
restated. The receipts' apparent 20–40 line shrink is a pretty-print collapse, not content loss.

**Compared all 27 receipts as EDN data across the commit** rather than reading two by eye. Every difference is
one of three kinds:

1. `:result {:axioms nil, :exit 1}` → `{:axioms ["propext" "Classical.choice" "Quot.sound"], :exit 0}` on the
   eleven that C376 caught. **Consistent with the stale-`.olean` diagnosis.**
2. `:dependency-closure {:mode :author-declared-source-slices, :machine-complete false, :reason
   :lean-transitive-closure-not-content-pinned}` added to **every** receipt — the lane recorded C379's residual
   *in the receipts themselves*, which is what C386 was going to ask for.
3. **Adapter `:lean-field` strings rewritten** on six receipts, from prose labels to literal source fragments:
   `"Policy constructors"` → `"inductive Policy"`, `"controlled support rows"` → `"| (s, .stay) => [s]"`,
   `"posterior support/mass rows"` → `"support := fun _ => [.cautious]"`.

**(3) is the finding.** The same commit made "Lean-field strings must occur in retained declaration slices" a
requirement **and** rewrote the strings so they occur. That is defensible — the old strings were prose that
could never appear in Lean source, so they asserted nothing checkable — but it narrows what the check proves.

**The strings are correctly scoped.** I checked the one that looked worst: `"support := fun _ => [.cautious]"`
also appears in `ParameterPosteriorKernelPriorNegative.lean`, but the receipt's basis is
`ParameterPosteriorKernelWitness.lean` and it occurs there at line 19, inside the pinned slice. **Not a
cross-file match.**

**What it does not establish.** The mapping `[:rows] -> "support := fun _ => [.cautious]"` carries an
`:expected` of four rows, each with a support list and a mass map. **The pinned string is one line covering the
`support` half of one constructor arm.** So its occurrence does not establish that the four-row table
corresponds to the Lean definition. Contrast `channelCount` expected `14` — a field name against a scalar,
where occurrence and correspondence nearly coincide.

**So the check catches what C376 mutated** — deleted adapters, empty adapters, `unrelatedField` — and that was
its purpose. **It does not check correspondence between `:expected` and Lean content**, and for the six
rewritten receipts the gap between the two is widest.

**Not a defect in C379 and 31/31 stands.** It is a limit of the same shape as C379's stated residual: the pin
is load-bearing without being complete. **Route to `wm-nouns` after C386 returns** — same file, same question,
and two lanes in one file is how a repair gets lost in a merge.

## C394 — owner review of C383/C392, and the file moved under me (my fix, owner review)

**Gated C383 (`8d4bd99`) because the parking request rests on it.** What I checked.

**Read the evidence acceptance paths, not the summary.** `evidence_quiescent` and `evidence_fence` invoke their
producers directly — there is no caller-supplied path into either. `evidence_tested` resolves three bounded job
IDs through the Futon3c registry, requires the terminal systemd unit and on-disk receipt to agree, and takes
the gate start instant from **systemd's `ExecMainStartTimestampMonotonic`**, so a presenter cannot freshen a
day-old fence by editing a receipt's `started-at`.

**`evidence_reload` and `evidence_click_issued` DO accept a caller-supplied JSON path** and check only its
contents. **This is a documented boundary, not a gap.** C371: *"The attestation claim deliberately ends at
`tested-commit`."* The code agrees — those transitions carry `attestation-coverage:
not-claimed-after-tested-phase`. C383's *"Caller substitutions are not accepted"* is the closing sentence of
its quiescence-and-fence paragraph, scoped by it.

**So two of the eight states rest on caller-supplied JSON by design**, because reload and click are Joe's
operations with unbounded latency and no bounded producer. The machine enforces their identity, terminal,
certificate, restoration and ordering predicates; **it does not claim the attestation covered them.** That is
the honest limit the operational certificate has to live with, and it is stated in the right place.

**Ran the controls rather than trusting the count: 13 green** (the report said 12).

### The file moved under me

**I grepped for a `parking-specification-sha256` comparison, found none, and dispatched C392 to add one.**
Between that grep and the test run, **C392 landed as `386b0e9` at 02:38** — and `load_ledger` now raises
`parking-specification-changed:recorded-request-differs-from-current;abort-or-re-initialize`, distinguishing
changed operator authority from ledger corruption.

**I noticed because a test name did not match what I had just told a lane**, not because I saw the commit. This
is the read-side of the hazard I flagged one hour earlier when `wm-organization` closed `wm-evidence`'s census
finding four minutes after it committed. **Holding a dispatch prevents two lanes writing one file; it does not
prevent reading one mid-write.** C393 is the durable answer for censuses; for review, the rule is to pin the
commit under review and read at that commit, not at `HEAD`.

**Both C385 exceptions are now closed.** With C389 (`fe698f3`) amending C284's prose, **both full suites are
green**: Futon2 CI exit 0, Futon3 248 tests / 1,518 assertions, zero failures.

## C397 — owner review of C390; fixed the guard's unresolvable-path crash (my fix)

**Gated C390 (`c9f71ff`) with my own fixtures rather than its self-test**, because a guard that cannot fail is
the defect class this campaign has been auditing others for.

**Its self-test is real.** `--negative-control` injects a Make target invoking a report-only command and
requires rejection; it exits 2 if detection silently stops working. **Shipped positive: 0 findings, exit 0.
Control: rejects, exit 0.**

**Fixtures I wrote, against synthetic file text (no shared file mutated):**

| exposure | result |
|---|---|
| clean Makefile | not caught (correct) |
| direct `python3 <path>` in a recipe | **CAUGHT** |
| indirect via Make variable `P=<path>` | **CAUGHT** — the literal path is in the file text |
| indirect via a wrapper `.sh` | **not caught** |

**The wrapper gap is real and not live.** Both report-only commands
(`live_artifact_format_boundary_lint.py`, `empty_subject_acceptance_lint.py`) are invoked **only** from
`checks/wm_workspace_gate.clj`, which is exactly where exit 3 is understood. C390's own claim is *"direct
exposure"*, so behaviour matches the claim.

**`:report-only-set-empty` deserves noting**: the guard reports a finding when the report-only set is empty, so
it cannot pass by having nothing to check. **That is class 1 addressed in the design**, not bolted on.

### What I fixed

**A report-only command whose argv names no `.py`/`.clj` script made `command-path` return nil, which reached
`str/includes?` and threw a bare `NullPointerException`.** Not hypothetical: this repo has `.bb` checks. It
failed *closed*, which is the right direction, but the error named the guard rather than the command it could
not resolve.

**Repaired directly** (owner carve-out — I held the full context and re-belling would have cost a round trip
for six lines): nil paths are now collected and reported as
`:report-only-command-path-unresolvable :command <name>`. **A crossing check that cannot locate a command's
script has not cleared it**, so it stays a finding.

**Verified after the fix:** the `.bb` fixture now yields that finding instead of an NPE; shipped positive still
exit 0; negative control still rejects; all four Make fixtures unchanged; clj-kondo 0 errors 0 warnings.

## C401 — owner review of C393; the staleness check was blind to two-thirds of its own census (my fix)

**Gated C393 (`fc445bb`) because it went into the workspace gate**, which gives it the widest blast radius of
anything landed tonight.

**Exit semantics are as claimed:** `:possibly-stale` exits 0 (nonblocking), `:unavailable` exits 1 (blocking),
negative control rejects a malformed registry. **The nonblocking/blocking split is the one I asked for.**

### The finding

**The check compares only the subjects it is told about, and the declared list omitted most of the census's
citations.** C385 cites **nine** source paths; the registry declared **three**.

**One of the omitted files had already moved.** `checks/positive_proof_receipt.clj` is cited in the census and
has **two commits since the pinned basis** (`f30a8e7`, `a6e1c24` — C386 and C391). **The check reported
`:possibly-stale` for two subjects while silently missing a third.** A staleness check that reports staleness
is the hardest kind to doubt.

**This is the same shape as C391 and C386**: an author-declared list standing in for a derivable set. The
census's citations are *in the document*; deriving them is possible and would have caught this.

### The second defect

**`moved` reads `(basis repo)`.** For a subject in a repo the basis does not pin, `commit` is nil,
`git diff --quiet nil HEAD -- path` exits nonzero, and the subject is reported **MOVED**. **A wrong answer
that looks like a right one** — "possibly-stale" is exactly what a genuine move reports, so the error is
invisible in the output. Both cross-repo citations (`futon3c/scripts/bounded_test_job.py`,
`p4ng/empirics-futon/gen_workflow_report.bb`) would have hit it.

### What I fixed

**Both, directly** (owner carve-out; I held the full context and this was ~15 lines):

1. **Registry extended to all eight futon2 paths the census cites.** The check now reports
   `positive_proof_receipt.clj` as a third moved subject.
2. **A subject whose repo the basis does not pin is now the failure
   `:subject-repo-not-pinned-by-basis`**, not a phantom move. Verified with a synthetic `:futon3c` entry:
   status `:unavailable`, that failure, `moved: nil`.

**The two cross-repo citations are deliberately still absent, and that is the honest position:** C385 pinned no
basis commit for futon3c or p4ng, and inventing one would pin a basis the census never recorded. **Recorded as
a comment in the registry** so the omission is visible rather than looking like completeness.

**Verified after:** positive exit 0, negative control rejects, clj-kondo 0/0.

**The durable repair — derive cited paths from the document instead of declaring them — goes to `wm-verbs`
when C400 returns.** My registry edit closes today's gap; it does not stop the next census from being declared
incompletely.

## C406 — owner review of C395; and the precise shape of my C394 error (my fix, owner review)

**Verified C395's finding 1 in code rather than accepting the attack narrative.** `evidence_click_terminal`
checks a schema string, matches `click-id` against **caller-supplied context**, and requires `run-id`,
`terminal-outcome` and a `resource-status` in `("clean","dirty")`. `evidence_certified` checks
`verdict == "pass"` and `run/id` against the same caller-supplied context. **Neither resolves its artifact
through the producer meant to have emitted it.** Handwritten files satisfy both. **Confirmed.**

**Both transitions do stamp `attestation-coverage: "not-claimed"`.** So the code is honest about what it
verified; **the state name `certified` is what overclaims.** That is why C402 asks what the name may mean
rather than asking for more validation.

### What I actually got wrong in C394

**Not "I called a forgeable seam acceptable."** C394 examined `evidence_reload` and `evidence_click_issued`,
found them caller-supplied, checked them against C371's *"the attestation claim deliberately ends at
`tested-commit`"*, and concluded code matched prose. **That conclusion was correct for those two states.**

**The error was stopping there.** I confirmed the documented boundary held where I looked and never tested
**whether the same caller authority continued past it** — into `click-terminal` and `certified`, which the
documentation does not cover and which purport to attest a run and its certificate. `wm-evidence` states it
exactly: *"The successful attack is therefore not the existence of those two seams. The finding is that the
same authority continues through `click-terminal` and `certified`."*

**The reusable form: verifying that a documented boundary holds at the sites you inspected is not verifying
where the boundary ends.** A boundary is a claim about the *first* state not covered, and that is the state to
test.

**Consequence for the parking request:** it does not go to Joe until C402 resolves what `certified` means, and
the remaining four C395 findings are repaired and re-attacked. **I told Joe I was one step from sending it; I
was not.**

## C407 — C403 blocked the gate on a real incompleteness; I pinned the reconstructed bases (my fix)

**C403 (`fc445bb` → derivation) shipped and immediately turned the workspace gate red**, exit 1, status
`:unavailable`. **The check was right and the gate was correctly blocked**: C385 cites three cross-repo files
— `futon3c/scripts/bounded_test_job.py`, `futon3c/scripts/bg.py`, `p4ng/empirics-futon/gen_workflow_report.bb`
— that the registry did not declare and whose repos the basis did not pin.

**Derivation found a citation I had missed by hand.** My C401 pass extended the registry to the eight futon2
citations I found by grepping the document; **`futon3c/scripts/bg.py` was not among them.** The derived scan
caught it. **That is the argument for deriving rather than declaring, made against my own manual attempt.**

**I asked C403 to say what the cross-repo convention should be. It answered by making it a failure** —
`:cited-repo-not-pinned-by-basis` — which is the right answer: a census that cites a repository must pin it,
or the citation cannot be validated at all.

### What I did

**Pinned both cross-repo bases as each repo's HEAD at the census commit's instant** (2026-09-01T02:28:46Z):
futon3c `e5f1d94`, p4ng `8ee6190`. **Declared the three cross-repo subjects.**

**These bases are reconstructed, not recorded, and the registry says so in a comment.** Earlier I refused to
add a basis on the grounds that inventing one would pin something the census never recorded. **Reconstructing
each repo's HEAD at the census's own commit instant is not inventing** — it is recovering the state the author
was necessarily reading — but it is weaker than a basis the author pinned, and the distinction belongs in the
file rather than in my memory of why I did it.

**Verified: status `:available`, no failures, exit 0. Negative control still rejects. Three subjects report
`:possibly-stale`**, including `positive_proof_receipt.clj`, which is correct — it has moved twice since the
basis.

**The durable lesson is C403's, not mine:** a hand-declared subject list was wrong twice in a row, once by the
original author and once by me while reviewing that exact defect.

## C412 — owner review of C402: the click binding is real (my fix, owner review)

**Gated C402 (`d704401`), the repair for the C395 finding I had previously mis-cleared.**

**`producer_bound_click` reaches the producer over a channel the caller does not own.**
`serving_click_status()` issues an HTTP GET to `http://127.0.0.1:7070/api/alpha/wm/click` — **the live serving
JVM, not a file on disk.** That is the property that matters: the earlier `evidence_click_terminal` and
`evidence_certified` compared caller-supplied JSON against caller-supplied context, so both sides of every
comparison were writable by the presenter.

**The receipt must now agree with what the JVM independently reports** on click id, run id,
`binding-status: verified`, `run-record-status: present`, and terminal outcome — **and the run record and
durable binding are then read from paths the JVM named**, not paths the caller supplied, with identity
re-checked in both. A handwritten file cannot satisfy this unless the serving JVM actually ran that click.

**`produce_certificate` selects its tested job from `context["bounded-job-ids"]`** — the fence-bound ids
recorded at `tested-commit` — so **C404's caller-selected-receipt finding does not apply to this path.** It
applies to the standalone certifier invoked with `TESTED_JOB_ID`, which is what C410 addresses. **Two paths
into the same certifier, one bound and one not.**

**Ran the suite: 14 tests green.**

**Residual, conservative in the right direction:** the endpoint reports `last-result`, so the state machine
queries the JVM at transition time rather than at click time. A restart or a later click between the two makes
the transition **fail**, not pass. Presenting a *different real* click's receipt is not forgery — it is a
different run, and the ids will not match.

**C395 finding 1 is closed. Four remain**, two of which (truncate-and-re-extend, copied ledger) wait on C409's
answer about whether any append-only authority exists locally.

## C416 — owner review of C409: the negative result holds, and the journal asymmetry is sharper than stated

**Gated C409 (`a843c45`) because I acted on it immediately** — I told `wm-organization` not to attempt the
truncate-and-re-extend and copied-ledger repairs. **A wrong census would have closed a line of work
wrongly**, so it needed checking rather than accepting.

**Verified, running as `joe`:**

| claim | check | result |
|---|---|---|
| evidence stores mutable by their writer | `stat` on the coordinator registry, `.git` | `joe:joe -rw-------`, `joe:joe drwxrwxr-x` — **confirmed** |
| journal externally administered | `stat /var/log/journal` | `root:systemd-journal drwxr-sr-x` — **confirmed** |
| no append-only/immutable protection | `lsattr` on both repos | `-------------e-------` (extents only) — **confirmed** |
| `joe` can append to the journal | `systemd-cat -t wm-anchor-probe`, read back | **wrote and read back** |

**One refinement, in C409's favour.** The report calls the journal *"unsealed, root-mutable"*, which is true
and understates the asymmetry **for the threat model that matters here**. The relevant adversary is the account
that writes the evidence, and **`joe` can append to the journal but cannot unlink or rewrite it**: the journal
directory is `root:systemd-journal` with group `r-x`, so even group membership grants no write. **Append
without erase is exactly the asymmetry a ledger anchor needs.**

**It remains not a proof.** Root can still rewrite, retention still expires, and nothing is sealed — so an
anchor raises the cost of forgery without establishing history length. **That is the distinction C415 is
assessing, and I have not pre-empted its answer**; this note records the probe so the review has the same
ground truth.

**Recorded rather than belled** — `wm-evidence` is mid-C415 and a second bell into a running job is how a
question gets mistaken for an answer.

## C422 — my ceiling document omitted C404's fourth limit (my fix)

**`CERTIFICATE-CEILING.md` listed three limits; C404 reported four.** I wrote the ceiling from the lanes'
*summaries* and the fourth appears only in the report body, under "Program-that-ran limit":

> *"The certificate does not bind the observation to the instant the run began… A reload or substituted click
> receipt between execution and terminal observation is not distinguishable from a single stable serving
> program by this artifact alone."*

**C410 bound the tested receipt to the fence attempt. This is the time binding of the one identity that is
observed** — the report says so explicitly: *"This is separate from the closed-dependency-set limitation; it
concerns the time/attempt binding of the one identity the implementation does observe."*

**Corrected**: the ceiling now carries four limits, and records that unlike the closure and history-length
limits **this one looks repairable** (the run record could carry program identity at start) and is **not yet
dispatched.** The summary claim gains a fourth clause: the certificate cannot say *this program ran every
stage*.

**The method error is worth keeping.** A ceiling assembled from delivery summaries inherits whatever the
summaries chose to foreground. **A lane that reports four findings and lists three in its bell is not being
careless** — the bell is a summary. **Reading the report is the owner's job**, and I did it for the attacks I
gated and not for the one I only summarised.

## C424 — owner review of C405/C417: the boundaries are enforced, not decorative (my fix, owner review)

**The ceiling document's closing claim rests on this**, so it needed checking: *"the limits are machine-readable
rather than prose, so a consumer cannot silently inherit the stronger reading."* **If the boundary records were
merely written into receipts and not enforced, that sentence would be false** — and it would be an instance of
the class it describes.

**Called `receipt-shape-valid?` directly on mutated maps** rather than swapping receipt files, so no shared
artifact was touched while lanes could read it:

| receipt | result |
|---|---|
| intact | **valid** |
| `:derivation-status` changed to the false class (`:not-exactly-derivable` for Lean, which *is* derivable) | **rejected** |
| `:dependency-closure` removed entirely | **rejected** |

**Both failure directions are closed**: a receipt cannot drop its boundary, and cannot misdescribe why
derivation stopped. **The second matters more** — `:derivable-not-adopted` and `:not-exactly-derivable` are the
difference between a limit worth investing to close and one that cannot be closed, and a receipt that could
claim the wrong one would misdirect exactly the reader the vocabulary exists for.

**Confirmed the wiring claims too.** The gate carries 53 registered checks including `:exit-code-scopes`
(C390) and `:repository-census-bases` (C393) — both "wired into the gate" claims hold. Receipt work is gated
under `:r9-proof-receipt`, `:pinned-operational-certificate` and `:lean-sorry-categories`. **`quiescence` is
correctly absent**: it observes live process state, not repository state, and belongs to the state machine.

## C430 — the repository-wide gate run: 128 checks, basis stable, three failures (my fix, owner)

**Held all four lanes idle to get a fixed tree**, then ran `make workspace-gate` under the bounded service
(`futon-test-bounded-…-wm-gate-c424`). **This is the first verdict tonight covering the whole repository rather
than one lane's corner**, and it found three things no lane could have seen — each lane ran focused checks, and
the gate is the only thing that runs these.

**Basis `:stable` across all four repositories**, futon2 at `7a646d1` with `dirty? false`. **The idle hold
worked**: a gate run against a moving tree returns `repository-basis-changed` and certifies nothing.
**128 checks, 127 executable, 6m35s CPU, 1.6 GB peak.**

**Verdict qualification `:content-only-event-free-unverified`** — no writer fence was declared for this run, so
event-freedom is unverified and the gate says so rather than assuming it. **That is the C292/C313 machinery
behaving correctly on an ordinary run.**

### The three failures

**1. `c277-perturbed-reduction-free-energy`, exit 2 — `mutation slipped`.** The most serious. **A negative
control perturbed its witness and the check did not notice**, so it has been reporting success for nothing.
Class 1 in the least visible place: the artifact that fails is itself a negative control. → `wm-nouns` (C426).

**2. `reload-click-certificate-rehearsal` — expected `:pass`, got `:fail`.** A regression from tonight:
C398/C410 added `:serving-program-matches-tested-program?` and run-identity checks that C230's rehearsal
fixture predates. The certificate reports `:program-identity-status :unavailable` and
`:resource-run-identity-matches? false`. **The open question is whether an unavailable serving identity should
collapse to `:fail`** — C404 established `:unavailable` blocks throughout the certificate path, which is right
for production and may be wrong for a rehearsal. → `wm-organization` (C427).

**3. `mutable-verdict-claims` — `:undeclared-member` ×3.** `exit_code_scope_check.clj` (C390) and
`repository_census_basis_check.clj` (C393) joined the gate tonight **without joining the verdict census**;
`empty_subject_acceptance_lint.py` has been undeclared longer. **The census caught its own authors**, which is
what a 73-member population with zero unexplained members is for. → `wm-verbs` (C428).

### One confirmation worth keeping

**The bounded wrapper exited 125 while the inner gate exited 1** — exactly as C387's census predicted:
*"Bounded runner: collapses any inner nonzero to outer 125/`test-failure`, while retaining `inner-exit`."*
**Tonight's own analysis of the exit vocabulary was verified by an unrelated run of the thing it described.**

## C436 — correction: the gate's basis claim excludes a repository it runs (my fix)

**C430 records the gate run as certifying a fixed tree because basis was `:stable` across all four
repositories. That is narrower than I made it sound.**

**The gate runs `futon3c.wm.chain-rehearsal-test` with `:dir "/home/joe/code/futon3c"`, and
`checks/wm_workspace_gate.clj:18` pins `:futon2`, `:mathlib4`, `:p4ng`, `:futon3` — no `:futon3c`.** So the
gate executes a test from a repository whose commit it does not record, does not report in
`PROVENANCE-FINISH`, and does not check for stability.

**`futon3` and `futon3c` are different repositories.** That near-collision is presumably why nobody noticed;
it is also why I did not, reading the provenance line and counting four.

**What the gate run does still establish**: futon2 at `7a646d1`, `dirty? false`, stable through the run, and
the same for mathlib4 and p4ng. **What it does not**: that the futon3c tree supplying the rehearsal was fixed,
or which commit of it was tested. **C427's repair for that very rehearsal landed in futon3c (`529428d9`)** —
the fix for a gate failure lives in the repository the gate does not pin.

**Same shape, fourth instance tonight**: a declared population standing in for the derived one (C386's closure,
C391's adapter fields, C393's census subjects, C429's identity survey — and now the gate's own basis). **The
derivable version exists: the commands carry `:dir` and argv.**

**Dispatched to `wm-verbs` as C435, and I am holding the gate re-run until it lands.** A whole-tree verdict
whose basis claim is knowingly incomplete is worth less than one that waits.

## C438 — every negative control in the Lean domain is gate-only (my fix, owner note)

**C437 (`0b637af`) turned a one-off into a property of the system:**

| invocation | count |
|---|---|
| automatically invoked **outside** the full gate | **0** |
| automatically invoked **only by** the full gate | **32** |
| invoked by nothing | 0 |

**All 32 remain independently runnable through 20 focused wrappers — but only when their explicit negative
flags are supplied.** An ordinary positive wrapper run omits them. **So C277 was correctly wired the entire
time**; its coverage was *"only as fresh as the last completed full gate"*, which is exactly the measured
**1:29:08** gap.

### Why this matters more than the original defect

**These 32 are the checks that prove the other checks still detect.** If their freshness is bounded by the last
full gate run, then between full runs **nobody knows whether any detector still detects** — and the full gate
costs 6m35s of CPU and needs a quiescent tree, so it runs rarely. Tonight it had not run for over an hour while
four lanes committed continuously.

**This corrects my framing twice over.** I called C277 a silent-detector defect (C426 packet); C434 showed
`#guard_msgs` catches unresolved referents loudly and **structurally silent controls number 0**. I then called
the follow-up an invocation-coverage question, which was right — **but I expected a small gate-only set. It is
all of them.**

**Consequence for C431's receipt**: *"distance since the last full gate"* is not operational trivia. **It is
the staleness of the entire negative-control population**, and the receipt is the only artifact that will
answer it. That raises its value and does not change its honest limit — it remains producer-controlled history,
as C431 recorded.

**No repair dispatched.** Running 32 Lean elaborations on every commit is not obviously right, and C437 was
told not to propose it. **The finding is that the risk is now measurable rather than invisible**, which is the
precondition for deciding whether to pay for it.

## C441 — my ledger is incomplete for C380–C440, and the generator caught it (my fix, owner)

**The workflow report's frontier moved backwards — 421 to 382 — and a frontier should not do that.** The
generator is right and I am the defect.

**Of C380–C440, only 13 numbers have ledger headings here**: C391, C394, C397, C401, C406, C407, C412, C416,
C422, C424, C430, C436, C438. **All thirteen are my own owner reviews.** Every lane delivery in that range —
C426's mutation diagnosis, C433's 115-control survey, C435's five-repository derivation, C437's invocation
census, C439's claim boundary, C440's 32 registrations — **is recorded in the lane's own commit and its
`C<n>-*.md` report, and nowhere in this file.**

**The generator's frontier logic takes the contiguous attributed run from the top and refuses to claim across a
gap** (C255: exact coverage, no gap tolerance). With 48 missing numbers it correctly fell back to 382. **A
generator that had reported 440 would have been lying about a ledger it could not see.**

### Why I am not back-filling

**Writing 48 headings now, from the bellbacks, would be reconstruction presented as record.** That is the
question `wm-verbs` settled in C408 about my reconstructed census bases: admissible as an anchor, **not
admissible as the author's contemporaneous observation** — and there the reconstruction had a defensible
method. Here I would be reconstructing attribution and timing from memory of a conversation.

**So the boundary is declared rather than papered over**: this ledger is **complete for owner reviews and
incomplete for lane deliveries in C380–C440**. The authoritative record of the lane work is the set of
`holes/labs/wm-contract/C<n>-*.md` reports plus the lane commits; the workflow report's per-lane counts
(31/41/43/29 = 144) are therefore a **floor, not a total.**

**The discipline that failed** was mine and it is specific: I recorded what I reviewed and stopped recording
what I dispatched, roughly from C425 onward, while the campaign accelerated. **The ledger degraded exactly when
the throughput rose** — which is when a ledger is worth the most.

**Not dispatched.** Changing the generator to count lane report files instead would make the number look right
and remove the signal that told me this.
