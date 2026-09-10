# RUN4 trial packets: three bounded tasks (preparation only)

2026-09-10, Zai-2, prepared on instruction from Codex-17 after Joe selected all
three shortlisted candidates as a short RUN4 trial series, including deliberate
imperfect task fit. This is a **packet specification**. Nothing here enacts a
trial: no dispatch, no live run, no production/code edits, no campaign, registry
or data changes, no Claude calls, no sealed reads (in particular the caption
holdout `/home/joe/apm-caption-holdout-2026-09-10.edn` and any sealed queries).

Corrections from `PREPARATION.md` (same directory) are incorporated: memory
supplied verbatim including its own vocabulary; no analyst proof in any worker
packet; caption review needs no sealed holdout; the monitor's local rules are
explicitly **proposed experiment rules**, not adopted institutional rules.

Shared conventions for all three packets:

- Role separation: worker and reviewer are different agents; coordinator
  (Codex-17 or successor) records dispatch/receipt IDs. No Claude invocation.
- Inputs are frozen by content hash before dispatch; any deviation is recorded
  as a packet error, not silently absorbed.
- Outcomes are recorded exactly as observed (pass / fail / blocked); failures
  are preserved, never reset.
- Each trial's claim boundary is stated in the packet and repeated verbatim in
  any result report.

---

## Trial 1 — memory-assisted endpoint-uniqueness implication (m96J04)

**Deliberate fit limit:** chosen because a reviewed memory is known relevant;
this measures bounded application of a supplied memory, not autonomous
retrieval, not causal benefit (no no-memory control in this trial), not
prevalence of transfer opportunities in the queue.

### Inputs

Exposed to the worker:
- Frozen target prerequisite prefix copied verbatim from the pinned
  `apm-lean/problems/m96J04/lean/Main.lean` into a separate namespace
  (method already used in the analyst probe; original file untouched). Hash of
  source and copy recorded.
- The task statement: from the target hypotheses — putative eigenvector
  continuous on [0,1], differentiable on (0,1) with f′ = f/λ there, f(0) = 0 —
  prove f = 0 on [0,1], **without** any endpoint-derivative premise. Whole-problem
  construction/compactness of the Volterra operator is explicitly out of scope
  and stated so to the worker.
- The stored memory `interior-gronwall-to-endpoint-by-one-sided-limit`
  (evidence id `e-apm-promotion-5fdb99169bd788313841375c797c302c`)
  **verbatim**, as preserved in
  `futon3c/holes/labs/M-apm-demonstration/analysis/memory-first-probe-2026-09-10/memory.json`
  and `review.json` — including any occurrence of its name or "Gronwall" in its
  own body. No stripping, no added hints.
- Ordinary Mathlib source and the Lean checker via the canonical apm-lean
  checkout (frame-worktree package-link rules per AGENTS.md).

Excluded from the worker packet and prompt:
- The analyst reference adaptation `EndpointTransfer.lean` and any derived
  hint. The retained f196 attempt-1 proof.
- Exclusion is by instruction and workspace hygiene only; it is not an enforced
  access boundary. The worker must report any accidental reference exposure,
  and the trial report must describe the boundary as instruction-level, never
  as proven blindness.

### Product

A Lean file in a fresh isolated directory compiling under `lake env lean` from
canonical apm-lean, containing the implication theorem; `#print axioms` output
for it limited to `propext`, `Classical.choice`, `Quot.sound`. Plus the worker's
self-report: what was used, where the memory helped (if at all), remaining
obstruction if any.

### Pass / fail / blocked

- **Pass:** compiles, axiom audit clean, statement matches the frozen task
  (reviewer checks statement equivalence, not just exit 0).
- **Fail:** worker declares the step unproven or submits non-compiling /
  sorry-containing work after its budget; recorded as failure.
- **Blocked:** toolchain/environment failure (e.g. Lake exit 134 SIGABRT —
  environment abort, not a proof result), or packet input corruption. Blocked
  is distinct from fail and does not count as either.

### Evidence fields

Input hashes (prefix copy, memory, review); dispatch/receipt IDs; worker and
reviewer identities; Lean command, exit code, stderr excerpt; axiom print;
worker self-report; reviewer verdict; elapsed time; any exposure incidents.
Claim recorded on success: "one deliberately chosen memory-assisted transfer
example was completed by a fresh worker" — nothing stronger.

---

## Trial 2 — independent substantive review and admission of three enriched captions

**Deliberate fit limit:** the caption apparatus and substantive caption reviews
are incomplete; this trial reviews/admits three specific enriched captions, not
the caption program. It does **not** use the sealed holdout; sealed queries test
retrieval quality later and are never development inputs. Historical
admissibility reconstruction is relevant only to historical exposure claims and
is not a prerequisite here.

### Inputs

Exposed to the worker/reviewer:
- Three enriched captions (exact three fixed and hash-frozen by the coordinator
  before dispatch; selection criterion recorded, e.g. drawn from the audited
  new-memory set).
- For each: its original memory record, the source problem and retained proof
  it came from (pinned by hash), and the applicability evidence the caption
  claims to summarize.
- A written admission rubric frozen before dispatch: (a) is the caption's
  applicability description true against the source proof? (b) is it
  non-misleading about scope/limits (e.g. does it warn against destructive
  rewrites where the source did)? (c) is the vocabulary faithful to the source?

Excluded:
- The sealed holdout file and any sealed queries. No retrieval evaluation is
  performed in this trial.

### Product

Per caption: verdict (admit / admit-with-edit (edit text given verbatim) /
reject (reason)), the check trail against source, and for admitted captions a
real admission action plus a retrieval receipt from the ordinary reviewed
retrieval path (production function, domain mathematics) showing the admitted
caption is reachable by at least one honest query.

### Pass / fail / blocked

- **Pass:** three complete verdicts with check trails; admitted captions have
  receipts. Mixed verdicts (some rejects) are still a pass — the trial measures
  review quality, not admission rate.
- **Fail:** verdicts asserted without source-check trail, or an admission made
  without its receipt.
- **Blocked:** the three captions or their sources cannot be frozen (missing
  artifacts) before dispatch.

### Evidence fields

Caption and source hashes; rubric hash; dispatch/receipt IDs; per-caption
verdict and trail; admission receipts (query, rank, response ids); reviewer
identity (worker ≠ coordinator here is enough; a second review pass is optional
and recorded as such).

Unresolved prerequisite: coordinator must fix which three captions and confirm
each has a pinned, retrievable source memory + proof. Until then the packet is
specified but not freezable.

---

## Trial 3 — isolated feedback-obligation monitor prototype

**Deliberate fit limit:** this is the least-specified candidate. Its local rule
contract is unsettled. Per the correction, the prototype runs **isolated**
(no live campaign mutation) and its rules are **explicitly proposed experiment
rules** — a bounded choice presented for the run, not imposed institutional
adoption. Unsettled semantics below are given as bounded options; the
coordinator picks one per option at freeze time and records the choice.

### Proposed experiment rules (choose one per item)

1. **Obligation trigger** — (a) any memory of kind `feedback` minted during the
   trial window creates an obligation to its primary subject, or (b) only
   feedback memories explicitly tagged as obligating by their author.
   Recommended: (a), simpler and testable.
2. **Recipient set** — (a) the agent/session named in the subject ref, or
   (b) all currently registered agents. Recommended: (a).
3. **Satisfaction condition** — (a) recipient acknowledges via any evidence
   entry referencing the memory id, or (b) a typed acknowledgement receipt
   only. Recommended: (a) first, (b) as v2.
4. **Escalation on non-satisfaction** — none in the prototype (report-only);
   escalation is out of scope. Not an option: auto-mutating anything.

### Inputs

- Frozen input corpus: a small set of recorded feedback memories (from the
  evidence store, read-only) with known subjects — including at least one
  whose recipient is not currently active (tests the outstanding-obligation
  display).
- The proposed rule choices above, frozen before dispatch.
- The monitor runs read-only against the evidence store; its outputs are
  written only inside the trial's isolated work directory.

### Product

One local feedback episode: monitor output listing (i) obligations detected
under the chosen rules, (ii) verified delivery/acknowledgement state, (iii)
outstanding obligations with recipients; plus the monitor source (kept inside
the trial directory, not production) and a run log.

### Pass / fail / blocked

- **Pass:** monitor produces a complete, self-consistent obligation listing
  for the frozen corpus under the frozen rules, and the listing is verified
  against the corpus by the reviewer by hand.
- **Fail:** listing omits or invents obligations relative to the frozen rules.
- **Blocked:** rule choices not frozen at dispatch, or the corpus cannot be
  assembled read-only. (Blocked is the expected outcome if the coordinator
  does not first pick from the options above.)

### Evidence fields

Frozen rule choices; corpus item ids; monitor source hash; output; reviewer
hand-verification note; explicit statement that rules are proposed experiment
rules with no institutional adoption implied.

---

---

## Trial 4 — AIF-instrumented outer-loop successor slice (build-loop replacement, first slice)

2026-09-10, added after Joe's fourth-task direction. Basis: PREPARATION.md
"Fourth task" section and `SERIES.edn :outer-loop-aif-replacement`. **Do not
run `wm-build-loop.sh` for any purpose** — its EXIT notifier calls Claude-1,
currently prohibited. This packet is note-only; no code, no live loop, no
ledger or data changes.

**Deliberate fit limit:** the stated objective is full replacement of the old
build loop, but this first trial proves only **one decision → execution →
outcome → next-use cycle on frozen fixture inputs**. It does not claim the
complete outer loop, does not take over the live ledger, and does not activate
any scheduler. Selecting differently from the old priority policy is not by
itself an improvement; the comparison must be reported honestly.

### Responsibility trace (what the successor must cover, from source)

Actual responsibilities of the old loop (`holes/labs/wm-contract/wm-build-loop.sh:106-151`
with `build_step.bb`):
1. **Ledger validity gate** before work, after work, and after review; invalid
   ledger stops the loop.
2. **Unblocking**: `:blocked` rows whose `:depends-on` are all `:done` become
   `:open`; the unblock commit is made exactly once (the double-unblock bug is
   history, not to be reintroduced).
3. **Selection**: `next-open` = deterministic priority over loopable rows —
   `:F` (FUNDAMENTALS) rows exclusively first, then run-lock rows (:RUN12/:RUN11),
   then :RUN rows, then ledger order; `:J`-class, `:joe`-owned and `:loop-skip`
   rows are never taken.
4. **Dispatch** of exactly the chosen row to a work seat, with inbox drain
   (bells waiting for the seat are folded into the prompt).
5. **Independent review** of `:done-unreviewed` rows by a separate review seat.
6. **Publish gate**: held while any registry-touching row awaits review.
7. **Stall detection** via whole-row hash (any edit is life).
8. Bulletin at natural stop.

**Distinct and not the same thing:** `scripts/wm_outer_loop.clj` is a *narrow
trace-to-Beta-update process* — read trace window, extract learn-action
emissions, derive follow-through, Beta update via
`futon2.aif.intrinsic-values/next-record`, persist hyperedge, apply. It
performs none of responsibilities 1–8. A successor must name which code owns
each responsibility; nothing may silently inherit from the old script by
running it. `futon2.aif.full-loop-runner` (author/reviewer dispatch
boundaries at `src/futon2/aif/full_loop_runner.clj:2814, :2981`) is a
candidate execution path for the execution review to assess — its existence is
not evidence a RUN4 packet reaches it.

### Inputs

- Frozen ledger-shaped fixture store (isolated copy, never the live
  `worklist.edn`): rows spanning statuses (`:open`, `:blocked` with mixed
  `:depends-on`, `:done-unreviewed` incl. one registry-touching), classes
  (`:F`, `:RUN`, `:J`, `:joe`-owned, `:loop-skip`), and pin fields for the
  stale-pin refusal test.
- Controlled outcome feedback fixture: scripted work/review outcomes for the
  dispatched row, including one missing-outcome case.
- Existing reusable components (consumed, not reinvented):
  `futon2.aif.intrinsic-values` Beta update (`next-record`), trace reading
  (`futon2.aif.trace`), the `full-loop-runner` dispatch boundaries, the
  priority/eligibility semantics of `build_step.bb` as the compatibility
  baseline.

### Product (first successor slice)

One cycle on the fixture store:
1. Eligibility snapshot + candidate set (same eligibility rules as
   `build_step.bb`, stated explicitly).
2. Explicit preference and prediction inputs to selection.
3. An **AIF selection record actually consumed**: the selection decision is
   made by the AIF-instrumented selector, and the record shows the inputs it
   used — not an after-the-fact annotation of an unchanged priority choice.
4. Existing inner-loop handoff: the selected row is dispatched with
   author/reviewer separation preserved.
5. Result/reviewer evidence recorded.
6. A persisted learned-state update (Beta posterior over the fixture store)
   **and a second selection in which that update is demonstrably consumed** —
   a negative control must show that disconnecting the learned-state
   consumption fails the next-use claim.

### Pass / fail / blocked

- **Pass:** one full cycle plus second-selection consumption demonstrated on
  fixtures; all validation checks below hold.
- **Fail:** selection is annotation-only, next use does not consume the
  update (or negative control doesn't detect disconnection), or any preserved
  behavior below is violated on fixtures.
- **Blocked:** fixture store cannot be built without touching live data, or
  the execution path is not yet verified (shared wiring prerequisite), or a
   required semantic contract below is still undefined at freeze.

### Preserved behaviors to validate on fixtures (compatibility baseline)

Blocked rows with unfinished dependencies stay excluded; author/reviewer
separation holds; invalid ledger refuses (before/after work and review);
missing outcomes remain `:unknown`, never coerced; stale input pins refuse;
publish held while a registry row is unreviewed; no duplicate dispatch of one
row; `:J`/`:joe`/`:loop-skip` rows never selected; FUNDAMENTALS exclusivity
preserved or its relaxation explicitly recorded as a proposed change with the
old policy's output shown alongside.

### Semantic contracts that are MISSING (prerequisites, not invented defaults)

No defaults for these are invented here; each must be defined and frozen
before the trial, or the trial reports blocked on it:
- The selection equation itself: how preference/prediction inputs map to a
  choice among eligible rows (the AIF side), and its relation to the
  deterministic priority order (override, re-rank, tie-break?).
- The outcome mapping: which work/review evidence counts as success/failure
  signal for the Beta update of which class.
- The learned-state consumption contract: where the posterior lives, how the
  next selection reads it, and what makes consumption verifiable rather than
  claimed.

### Evidence fields

Fixture store hash and provenance (must derive from copies, never live data);
eligibility snapshot; selection record with inputs and consumed-component
proofs; dispatch/receipt IDs; author and reviewer identities; outcome records;
Beta update record; second-selection consumption proof and negative-control
result; validation check results per preserved behavior; explicit statement
that no live ledger, scheduler or cutover occurred.

---

## Unresolved prerequisites before any trial can be frozen

1. **Trial 1:** freeze exact three hashes (prefix copy, memory, review) and the
   worker budget/timebox. Otherwise ready.
2. **Trial 2:** coordinator fixes the three captions and confirms pinned
   source memory + proof for each; freeze the rubric text.
3. **Trial 3:** coordinator picks one option per rule item (1–4) above; freeze
   corpus; confirm read-only access path to the evidence store.
4. **Trial 4:** define and freeze the three missing semantic contracts above
   (selection equation, outcome mapping, learned-state consumption); build the
   isolated fixture store from copies; and the shared wiring prerequisite
   applies — plus the hard rule that wm-build-loop.sh is never invoked.
5. **Series level:** execution path for WM-enacted dispatch is still under
   review (Codex-10/Codex-11); these packets must not be executed by
   independently belled workers and then reported as machine actuation.
   Run ID (suggested `run4-2026-09-10`) and isolated work directory are
   allocated at execution freeze, not here.

## Coordinator review — 2026-09-10 (supersedes conflicting proposals above)

Trial 2's three captions are already determined: the three source-read
entries in futon3c's memory-caption-history-candidate-2026-09-10 artifact,
not a newly sampled population. Memory IDs:

- e-apm-promotion-5fdb99169bd788313841375c797c302c
- e-apm-promotion-faa280c92bd833ed990672c3b7007a78
- e-apm-promotion-442f1ab7683c3685e2232135f161830e

Freeze the source-read-enrichments.edn bytes and each basis-evidence artifact
at execution preparation. Original source-byte verification has already passed
in the independent bootstrap review; admission still requires current original
memory authority. No live publication follows merely from this packet.

The assertion that worker != coordinator is sufficient review separation is
rejected. Caption review must obey Job A's actual rule: reviewer distinct from
the caption proposer AND contributing observation authors. Editing an entry
creates a new proposal requiring independent review; 'admit-with-edit' is not
permission to approve one's own revised text. An all-rejected set may count as
completed review, but supplies zero positive admission/retrieval evidence.
Report those outcomes separately, and mark the positive path unexercised.

Trial 3's proposed generic-feedback trigger and any-reference acknowledgement
are rejected as interpretations of the frozen IAD design. For this isolated
experiment, use these explicit local rules:

1. A declared feedback-created event under the experimental instance creates
   one obligation for each member of its frozen recipient set. Neither an
   arbitrary memory nor its author's tag establishes institutional authority.
2. Recipients are stable identities bound by the instance's membership record;
   include two recipients and an unavailable-route case. A subject ref or the
   current online roster cannot silently define or shrink that set.
3. A typed recipient inbox receipt must bind the instance, feedback revision,
   authorized recipient and payload digest. Generic references do not count.
   Delivery, acknowledgement, agreement and subsequent use remain separate.
4. Report pending/unavailable/overdue states; no external notifications,
   escalation, punishment or live mutation. Unknown evidence remains unknown.

Use clearly labelled synthetic fixtures for these rule controls, with captured
real transport shapes only where appropriate and sanitized. This avoids
relabeling unrelated historical evidence as an institution that did not yet
exist. Exercise false delivery, wrong recipient/revision, duplicate receipt,
recipient disappearance, disagreement, and unobserved use. A source-read-only
prototype can remain entirely isolated. This freezes experiment semantics,
not live institutional adoption; fixture bytes and worker inputs still require
pinning before the run.
