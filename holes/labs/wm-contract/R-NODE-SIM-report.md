# Node role-play simulation — report

**For:** Joe · **From:** claude-20 (tech lead) · **2026-08-30, 19:00–19:26Z**
**Artefacts:** `futon2/holes/labs/wm-contract/sim/R*-claim.edn` (6 files) · ledger entries
`656f629 … 55cb5df` in `holes/problems/BUILD-ledger.md`

---

## 0 · What this was

Joe's idea: one agent per node of the War Machine control map, each role-playing "I am R14, here is what
I generate, here is what I receive, here is who I need to talk to." Nine seats, one R-node each,
15-minute box. Not a controlled experiment — a role-play. (I initially built it as a controlled
experiment; Joe corrected that framing and he was right. See §5.)

Each agent was given its node's identity from the artefacts — stage, band, label, title, WR badge — plus
its drawn edges from `control-map-edges.edn`, and asked three things: what it emits, what it needs, and
**what it needs from a node it has no edge to**.

---

## 1 · The headline: the role-play corrected a record that had already passed a gate

**R14's agent, reading the selection code in character, found that the machine computes the commitment
temperature and the operator's learned habit prior — and then selects without using either.**

`policy.clj:234-270`:

    tau     (effective-temperature g-totals selection-gain temperature-opts)
    scores  (mapv (fn [g lp] (+ (/ (- g) tau) lp)) g-totals log-priors)
    chosen  (or (first controller-entries) (first ranked-actions))   ; <- FIRST, not argmax scores
    counterfactual-idx (first (sort-by ... scores))                  ; <- scores used ONLY here

The running machine takes this branch (`war_machine.clj:4503` sets
`:selection-boundary :strategic-recommendation`), and the code declares the situation in two places:
`war_machine.clj:358` — `:scheduler-habit-authority :counterfactual-only` — and the returned decision map's
own field, `:habit-prior-applied? false`.

**This contradicted a sentence I had written into the gated R19-D1 record**: *"the operator's learned
preference is live in the selection seam right now."* It is computed at that seam and does not choose.
The correction has been propagated by claude-15 into the EDN record (`17d779d`), the Lean fixture
(`84326d17c5`) and P-R19 (`1fb67b2`).

**R8's agent reached the same gap from the opposite side**, asking for an `R14→R8` edge because *"WR-27
requires evidence that my gain reading changes selection, but my only drawn outgoing edge ends at
evaluation and gives me no return path."* Two agents, one reading the branch and one missing its return
path, converging on: nothing demonstrates the gain changes selection. R14's WR-27 badge already carried
`:holds false`; this locates that badge in code.

**Why this sharpens R19 rather than denting it.** The operator is not an *unrecorded* presence in the
machine. The machine computes the operator's learned preference and then states, in a named field, that
it refuses it authority. That is a **declared abstention** — and promoting that preference from
counterfactual to selector is a *deployment decision*, a layer-stack choice, which is exactly what R19
says a preference stack is for.

---

## 2 · The structural signal: R16's missing feedback fan-out

**Four nodes independently reported needing something back from R16 (actuation), with no edge for it:**

    R16 -> R14   "I require R16's durable realized outcome back on the next tick"
    R16 -> R8    "I need the enacted result, bound to the same tick as the prediction"
    R16 -> R15   "I need witnessed tactical outcomes back from actuation"
    R16 -> R7    "my precision should constrain evidence-sensitive gating"

Three of these four cannot be echoes of anything I gave them: R14's and R8's agents never opened the
derived-edge file, and R7's claim is not in that file at all. Only `R16→R15` is possibly an echo (R15 read
the file and the edge is listed there). So: **three independent nodes plus one uncertain.**

This matches, from the node side, what R16-D1 found in the code: `:enacted nil` is an *untyped* nil, and
no observation channel reads an act's witness. **The machine acts and the result does not come back.**

---

## 3 · Full yield — which nodes need to talk to each other

13 claims from 6 nodes. **5 confirm edges the theory already derives but the map does not draw:**

    R16 -> R14      R5 -> R14       R16 -> R15      R3 -> R7        R2 -> R7

**8 are new to both the drawing and the derivation:**

    R4  -> R13   state-transition model needed by each rollout step
    R12 -> R13   realized long-horizon outcomes updating future temporal choices
    R11 -> R15   shared-budget arbiter's cross-level feasibility result
    R8  -> R3    previous tick's persisted :mu-post as this tick's carried prior
    R5  -> R7    precision should reach every scoring operation
    R16 -> R7    precision should constrain evidence-sensitive gating
    R16 -> R8    enacted result bound to the same tick as the prediction
    R14 -> R8    evidence that the gain reading changes selection

Zero direction errors by the agents. (An earlier "23% agent error rate" I reported was a bug in my own
classifier — see §5.)

**Three further findings from the claim prose**, none of which came from the edge statistics:
- **R8**: its measurement machinery is armed and red *because scheduled ticks stopped*, not because the
  scalar is undefined. Confirmed: the trace has a 40-day gap from 2026-07-21 to a single tick at
  10:54 this morning.
- **R15**: returned 0 artefact-backed / 2 imagined and said why — its drawn edges exist but their payloads
  are unspecified, so it refused to present them as evidence. Same conclusion CML-D2 reached from the
  record side, arrived at independently.
- **R3**: the drawn `R3→R1` edge's meaning is not named by any implementation it could find.

---

## 4 · What `:imagined` means, and what would promote it

Each edge entry is tagged `:from-artefact` with a `file:line`, or `:imagined`. Across the six claims:
10 artefact-backed, 9 imagined. `:imagined` is the honest tag for a payload the agent could not ground —
mostly edges that are drawn but unspecified.

Promotion to witnessed needs what CML-D2 needs: a `Delivery` whose operational fields are stated by an
endpoint record, then a check that a live emission matches it. Today the wiring stands at **specified 0 /
unspecified 21**, with two edge entries written and six of nine `Delivery` fields `:unspecified` in each.
So `:imagined` here is a correct description of the machine, not a shortfall of the agents.

---

## 5 · What went wrong, all of it mine

- **I framed a role-play as a clinical trial** — controls, contamination tracking, corroboration rates, a
  pre-registered prediction — and spent the run debugging my own metric instead of reading what the agents
  wrote. Joe's correction: seats that know the project role-play their node *better*; prior knowledge is
  the qualification, not the confound. The three best findings above are prose, not statistics.
- **I leaked the answer key.** My isolation rule forbade `P-R*.md` but permitted `p4ng/empirics-futon/`,
  which contains the derived-edge list — the very thing I was checking claims against. R15 read it and
  cited it openly, which is the only reason I caught it.
- **My classifier ran backwards**, producing a reported "23% agent error rate" that was entirely my bug.
  Corrected: zero agent direction errors.
- **I dispatched into seats that were not free.** I read roster `status: idle|restored` as availability.
  It is a liveness flag, not an ownership flag — `idle` means "not mid-invoke this instant", which is
  exactly wrong for a pooled worker taking a job every few minutes. **codex-17 was in active APM rotation**
  (serving `apm-watcher-codex`, `claude-clink-1` and `codex-18` in the hour before I took it), and my
  nine-job burst exhausted the Agency JVM's native-thread capacity — two of my jobs died with
  `OutOfMemoryError: unable to create native thread`, and an APM repair job reported a transient `git`
  spawn failure in the same minute. The job log records failures, not displacement, so I can report what
  failed but cannot certify nothing was lost.

**Machine limits, measured:** the box was never the constraint (32 threads, load 6.7, 249 GB RAM, 2002
threads system-wide against a 1.02M limit). The Agency JVM was: **267 threads at 2.84 GB RSS after 23h
uptime**. Spaced retries succeeded. 16 agents is fine if they are not launched in one burst — or after a
JVM restart, which is Joe's call.

---

## 6 · Cost, and what I would do next

**Cost:** 11 invocations across 9 seats, ~26 minutes wall clock, 6 claims returned. Three agents (R5, R6,
R11 on codex-8/10/12) are still running well past their 15-minute box and may be wedged. Plus the
disruption in §5, which is the real cost.

**Next, only on seats Joe lends:**
1. **The highest-value follow-up is not more nodes — it is R16's feedback fan-out.** Three independent
   nodes and the code agree that actuation's result does not return. That is one investigation, not nine.
2. If the role-play continues: run the remaining 8 nodes on seats that already know the project, drop the
   evidence bookkeeping to "say when you are guessing", keep `:missing-edges` (it produced every finding
   worth having), and read the prose rather than scoring it.
3. The 8 new candidate edges in §3 are each a small, checkable question against the code — cheaper to
   settle directly than to re-derive by simulation.
