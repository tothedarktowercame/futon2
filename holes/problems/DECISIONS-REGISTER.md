# Decision register

Opened 2026-09-01 by `claude-20` at Joe's request: *"a list of all the decisions
that have been made or that need to be made."*

**Scope note.** `DECISIONS-PENDING.md` is the operator brief for fourteen
decisions about the machine's semantics, and `C295-pending-decisions-staleness-audit.md`
audits their currency. Neither records decisions already **settled**, and
neither has a place for operating questions like *how often should the Lean
checks run*. This file is the wider index: one or two lines per decision and a
pointer. Analysis stays in the linked record; nothing here supersedes it.

**Two axes per entry:** who decides (Joe / owner / lane), and whether it is
settled or open.

---

## Part A — settled

### A1. Workspace and handoff protocol (`/home/joe/code/CLAUDE.md`, Joe)

| # | Decision | Recorded |
|---:|---|---|
| S1 | Substantial coding is belled to Codex, then independently reviewed by the Claude owner; author ≠ reviewer. | CLAUDE.md, 2026-06-03 |
| S2 | Review is a gate: read the diff, run the verify step, state what was checked. | CLAUDE.md |
| S3 | Review findings are fixed by the owner, not re-belled. Carve-outs (a) trivial, (b) full live context, (c) tight loops, (d) orchestration/review/architecture. | CLAUDE.md |
| S4 | Packet text uses a quoted heredoc. Unquoted expands `$vars` and executes backticks silently. | CLAUDE.md, 2026-08-18 incident |
| S5 | Handoffs stay small: one file, one behaviour, one acceptance test; discovery split from implementation. The cost of a big packet is the missing checkpoint, not the time. | CLAUDE.md, Joe 2026-08-03 |
| S6 | Every bell is followed by a park on the returned job-id. A bell without a park is a protocol violation. | CLAUDE.md, audit 2026-07-13 |
| S7 | One JVM per repo on master; live-load only from its own checkout. Violations are resolved by merging to master, not by a second JVM. | CLAUDE.md, Joe 2026-08-23 |
| S8 | The futon3c checkout is shared: stage explicit paths, never `commit -a`. | memory |
| S9 | Whistles, not bells, to reconcile a crossed exchange; never both whistle at once. | CLAUDE.md |
| S10 | Continue while the obstruction moves; stop when it repeats. | memory, Joe |

### A2. Campaign method (owner, this campaign)

| # | Decision | Recorded |
|---:|---|---|
| S11 | Four lanes (`wm-nouns`, `wm-verbs`, `wm-organization`, `wm-evidence`) run in parallel against one shared repo; a repository-wide verdict therefore needs quiescence, which only stopping produces. | memory; `lane-registry.edn` |
| S12 | A reconstructed value may be kept as a qualified anchor with provenance and `:correspondence :unverified` — not as a bare scalar pin, and not reverted away. This overruled the owner's own two-option framing. | C408 (`wm-verbs`) |
| S13 | The ledger gap for C380–C440 is declared, not back-filled; reconstruction from memory is not contemporaneous record. | C441 |
| S14 | The workflow-report generator is not changed to count lane report files. That would make the number look right and remove the signal. | C441 |
| S15 | A packet stopped twice by the Codex content filter is re-framed in defensive terms once, not retried a third time. | C419 → C423 |
| S16 | Gate reach is derived from each command's `:dir` and absolute argv paths and reconciled before execution, rather than declared in a list beside it. | C435 (`wm-verbs`) |
| S17 | The source is corrected before the artifact is regenerated (`lane-registry.edn` cleared to idle before the p4ng lane table was rebuilt). | `0725848`, `be82ebe` |

### A3. Declared boundaries — decided to be *not establishable here*

These are decisions in the sense that matters: each closes a line of work by
recording why it cannot be closed, so nobody re-opens it by accident.

| # | Decision | Recorded |
|---:|---|---|
| S18 | Topology-bearing code is not a closed set under Clojure's runtime closure. The certificate pins a declared boundary, not the program. | `CERTIFICATE-CEILING.md` limit 1, C399 |
| S19 | No local append-only authority exists; the root-owned journal was assessed and rejected because it cannot distinguish a copy from its original. **No best-effort anchoring is wired**, because it would mislead. | limit 2, C409/C415/C416 |
| S20 | Cross-producer attempt membership is unprovable with present material; systemd unit identity and monotonic start are the nearest genuine material and are not carried into the run. | limit 4, C423/C429/C432 |
| S21 | Adapter receipts establish occurrence, not correspondence to the Lean definition. | limit 5, C391 |
| S22 | C395 findings 2 and 3 are classified `authority-limit-not-pending-local-repair` with explicit clearing conditions, rather than left as open bugs. | C421 |
| S23 | Both halves of the ceiling — what the certificate establishes and what it cannot — belong in the artifact. Stating only the first half would instance class 10 (evidence anchored by its producer). | `CERTIFICATE-CEILING.md`, C414 |
| S24 | Cancelling an operator run is recorded but not counted; `:cancelled` starts a new semantic stratum and is not pooled with cohort 46. | `C206`, C211 |
| S25 | No existing check is moved to nightly. Any future nightly tier must name its runner and cadence, never merely remove a check. | `RUNBOOK.md` |
| S26 | Lean negative controls are revalidated **targeted**: a packet that changes a witness runs that witness's negative modes before landing. The full 32 stay on the gate. | O15, Joe 2026-09-01 |
| S31 | Work divides into **validation** (does the machine do what it claims, and can that be shown) and **overlay** (scheduling, fencing, run certification, systemd entry points, pinned-commit orchestration). Overlay is held as a source of ideas, not worked, because it builds on a claim not yet established and consumes a machine shared with Joe's other projects. Joe: *"just having an entry point on systemd isn't that interesting… this is an overlay layer on top of the validation."* | O19, Joe 2026-09-01 |
| S32 | **This machine is not given over to this project.** Campaign work that would interrupt Joe's other work needs his call first, not a proposal after the fact. | Joe 2026-09-01 |
| S30 | The 72 historical Morning Brief items are **retained as a log and not opened**. Joe's reason is not the false-currency hazard C209 raised: *"they're just there as a log of how the system was working in some earlier iterations, and as we've seen, it comprehensively was not working very well."* No retrospective QA, no disposition pass, no epoch boundary. | O11, Joe 2026-09-01 |
| S29 | Tree hygiene is repaired by **fixing inbox zero**, not by giving each agent a worktree. Worktrees relocate the merge-discipline problem rather than removing it, five stale forks already exist unmerged, and the one-JVM policy exists because a worktree reached the shared server. First repair is to make refusals visible and countable; only then the `:no-session-id` cause. | O27, Joe 2026-09-01 |
| S28 | Lanes run **continuously and in parallel** — this uses the machine well and is the intended steady state. It is gated on tree hygiene being a facility rather than an event, which `claude-20` owns. | O24, Joe 2026-09-01 |
| S27 | **No War Machine check gets a timer.** The full suite's trigger is a milestone event, not a clock; it is invoked when something needs it and never on a schedule. Gate runs required by `make pre-merge` or by a certified commit are unaffected. | O15, Joe 2026-09-01 |

---

## Part B — open

### B1. Machine semantics — fourteen operator decisions (Joe)

Full text in [`DECISIONS-PENDING.md`](DECISIONS-PENDING.md); currency and
dependency order in
[`C295`](../labs/wm-contract/C295-pending-decisions-staleness-audit.md)
(11 current, 3 stale-and-how, 0 resolved). Listed here only so this register is
complete.

| # | Decision | C295 status |
|---:|---|---|
| O1 | Strategic outcome vocabulary | current |
| O2 | Authored R16 outward-act binding — **safety/authority** | current |
| O3 | Avoided-range diagnostics as hard action guards — **safety/authority** | current |
| O4 | Prediction triple: omit channel or refuse update | current |
| O5 | Belief aggregation: omit absent or refuse incomplete | current |
| O6 | Strategic-mode inference on absent feature — **safety/ranking** | current |
| O7 | Missing sorry pressure: abstain or route around — **safety/action** | current |
| O8 | Validated rollout-step producer | current |
| O9 | Unscored rollout moves (downstream of O8) | current |
| O10 | Fulab: `outcome-size-surplus` absence default at `adapters/fulab.clj:81` | stale-and-how — referent corrected by `13ed674`; narrower decision remains |
| ~~O11~~ | **SETTLED 2026-09-01 — retain, do not process.** Moved to S30. | Joe |
| O12 | Support-typed shadow as live selection authority | stale-and-how — evidence is directional, not absent |
| O13 | Invoke-jobs ledger backend (EDN vs SQLite) | stale-and-how — durability repaired by C254/C263; now a scaling cost choice |
| O14 | Cascade meet semantics | current |

Cheapest order per C295: **O14 with/before O1**, **O8 before O9**, **O4 before
O5**, and **O11 before the operator run** — the run is itself the boundary
event, so performing it first sets the boundary by accident.

### B2. Cadence — when checks run (open, no record anywhere until now)

This class exists because Joe asked it. **Nothing in this campaign runs on a
schedule.** The five active user timers (`mana-snapshot`,
`futon1b-metaspace-sampler`, `apm-watchdog`, `apm-axiom-audit`,
`futon-pattern-index`) run APM and sampling work; none runs the workspace gate,
the Lean checks, the absence lint, or the census-basis check.

| # | Decision | Who | Note |
|---:|---|---|---|
| ~~O15~~ | **SETTLED 2026-09-01 — option C.** Targeted revalidation: a packet that changes a witness runs that witness's negative modes (~6.5 s each). Full suite not on a cadence until a milestone is defined. Moved to S26. | Joe | brief `O15` |
| ~~O16~~ | **SETTLED 2026-09-01 — no timer.** The trigger for the full suite is an event, not a clock. Moved to S27. | Joe | brief `O15` |
| O17 | **What gate distance is acceptable** — now measured in milestones rather than elapsed time, so it cannot be answered before O25. | Joe | reframed; blocked on O25 |
| O25 | **Define "major milestone."** Joe: *"we don't have a definition of major milestone, [so] there's no real way to get the overall suite to run at a sensible cadence."* The full suite runs at each milestone once this exists. Until then it is invoked when needed and never on a schedule. | Joe | new, from the O15 sitting |
| O26 | **Make option C followable.** The wrapper→negative-mode mapping lives in a C437 report table, not in anything a packet author can query. A rule nobody can mechanically follow will be followed unevenly. | owner | C437 |
| O18 | **How often the census basis is refreshed**, given reconstructed cross-repo pins carry `:correspondence :unverified` and never become verified by waiting. | owner | C407, C408 |

### B3. The operator run and the writer fence (open)

| # | Decision | Who | Note |
|---:|---|---|---|
| ~~O19~~ | **HELD 2026-09-01 as overlay, not decided.** Would park a live APM campaign (196 open sorries, 81 proved, reporting every 15 min) to certify a commit for a run architecture under review, and would still return `:certified-commit :absent` because trees are dirty. Nothing expires. See S31. | Joe | C450 |
| ~~O19-orig~~ | **Send the writer-fence parking request.** Generated at `/tmp/parking-request-wm-fence-2026-09-01-a.json` (3 coordinators, 8 units, `futon3c-zone.service` must remain running). **Nothing is parked.** Owner-side standing condition — the quiet-run state machine surviving a review it did not write — is met. | Joe | |
| O20 | **Dirty trees are not covered by the fence.** The fence stops new writes; it does nothing about trees already dirty. futon3c has 6 APM-written files and futon3 has 1 probe artifact, neither the owner's to commit. A fenced run still fails to certify a commit unless all five trees are clean going in. | Joe | `RUNBOOK.md` clean-tree rule |
| ~~O21~~ | Dissolved with O11: no epoch boundary is being set, so nothing sequences against the run. | Joe | S30 |

### B4. Campaign continuation (open)

| # | Decision | Who | Note |
|---:|---|---|---|
| O22 | **Repair or accept C404 limit 3** — the observed program is bound to terminal observation, not to the run. Unlike limits 1, 2 and 4 this looks repairable (the run record could carry program identity at start). Not dispatched. | owner | `CERTIFICATE-CEILING.md` limit 3 |
| O23 | **Who records lane deliveries in the ledger going forward.** C441 found the ledger degraded exactly as throughput rose: 13 of 61 numbers in C380–C440, all of them owner reviews. Either lanes write their own heading or the owner writes one per delivery; today neither is required. | owner | C441 |
| ~~O24~~ | **SETTLED 2026-09-01 — yes, continuously and in parallel, but not until tree hygiene is a working facility.** Joe: *"I like them working continuously and in parallel because that uses the resources of this machine well. But we need to prepare the ground for that."* Ownership of that groundwork assigned to `claude-20`. Moved to S28. | Joe | C446 |
| ~~O27~~ | **SETTLED 2026-09-01 — option A, repair inbox zero.** Not per-agent worktrees. Moved to S29. | Joe | C446 |
| O30 | **Can a run produce its own brief?** Joe's acceptance bar: a run must emit the outstanding-decisions list and the build-state numbers without a person assembling them. The numbers half exists in p4ng's 22 generators, aimed at a paper; the decisions half has no producer at all. Mission: `holes/missions/M-run-produces-its-own-brief.md`. | owner, with Joe on the bar | M-run-produces-its-own-brief |
| O31 | **The architecture of a run.** Joe: *"rather than having two agents like we used to, maybe we want to have four agents exactly with this kind of handoff... the architecture of a run needs to be rethought."* | Joe | same mission |
| O32 | **Comprehensive rethink of operator-facing surfaces.** Ten face Joe today, several overlapping, one unreachable without knowing an alias. | Joe | same mission |
| O29 | *(overlay, held)* **Pinned-commit orchestration across repositories** is untouched by O27's answer. Joe raised it and worktrees would have addressed it; repairing inbox zero does not. Still open, still needed for a certified run. | Joe | raised in the O24 sitting |
| O28 | **Whether large EDN state files move off a full-map rewrite.** `state.edn` is 125.7 MB and `invoke-jobs` is 134.6 MB; two facilities reached the same ceiling independently. Probably one decision with O13, not two. | Joe | C446, O13 |

---

## Maintenance

Add a row when a decision is made or discovered; move rows from Part B to Part
A with the record that settled them. Do not restate analysis here — a row that
needs a paragraph needs a `C<n>` record instead.

## Overlay, held 2026-09-01

O19, O29, and the systemd/timer aspects of O16–O17 are held under S31. They are
retained with their reasoning so a later need finds the work already thought
through; they are not scheduled. O20 splits — clean trees so agents are not
misled is validation-adjacent and stays open; clean trees so a commit certifies
is overlay.

**The question S31 leaves open, and it is the largest one here:** what would
count as evidence that the machine works? C450 sets out what the last two run
records show — 9 hops, 3 of 21 declared edges firing, a diagnostic selector
seam. That is the state any validation criterion has to be written against.
