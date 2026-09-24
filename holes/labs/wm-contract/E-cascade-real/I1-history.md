# I1 — History: the pre-H5b proposers, the rulings behind H5b and D3, the unbuilt cascade proposer

Excursion `holes/E-cascade-real.md`, investigation I1. Author: kimi-2,
2026-09-24. Discovery only: no code, data, or resource writes; no clicks;
no shared-JVM loads. All `file:line` anchors at futon2 `5d55e7a0^` (the
commit immediately before H5b, i.e. `e2e1b477`-era tree) unless another
revision is named; namespaces were dumped with `git show 5d55e7a0^:<path>`
and quoted from those bytes. p4ng anchors are working-tree paths (p4ng is
Joe's walkthrough notebook; commit shas given where the text itself names
them).

## Q1. What the judge composed before H5b, and what entered the ranking

### The composition

At `5d55e7a0^:scripts/futon2/report/war_machine.clj:6808` the judge ran:

```clojure
wm-candidates (ap/compose-proposers
               [ap/bootstrap-proposer
                pattern-registry/pattern-enumerator-proposer
                mission-registry/mission-enumerator-proposer
                mission-registry/ticket-enumerator-proposer
                sorry-registry/sorry-enumerator-proposer
                ;; M-aif2 slice-1: credited + admissibility-gated
                ;; tension-proposer — emits existing S2 classes via κ at
                ;; high-curvature actionable substrate-2 nodes (E1 consume).
                (tension/tension-proposer)]
               wm-state)
```

`compose-proposers` itself (`5d55e7a0^:src/futon2/aif/action_proposer.clj:63`)
is a plain concatenation: "Concatenate proposals from multiple proposers in
order; de-duplicate by action-map equality."

The six proposers, what each proposed, and over which substrate:

1. **`bootstrap-proposer`** (`action_proposer.clj:44`): always `:no-op`
   (the abstain comparison) plus one `:learn-action-class` action per
   action-type whose `forward-model/can-propose?` returns false — "when
   no concrete actions exist for an action class, the highest-priority
   recommendation is to enable the class itself" (docstring, citing Joe
   2026-05-17). Substrate: the machine's own capability boundary.
2. **`pattern-enumerator-proposer`** (`pattern_registry.clj:350`): one
   `:fire-pattern` action per recent context-retrieval candidate —
   `keep` over `(:patterns state)` gated by `addressable-pattern?`,
   carrying `:retrieval-score`, `:weighted-score`, retrieval rationale,
   sha256. Substrate: the pattern registry's aggregated retrieval state.
3. **`mission-enumerator-proposer`** (`mission_registry.clj:322`): one
   `:advance-mission` per mission in `(:missions state)`. The docstring
   records why not `:open-mission`: proposing open for already-open
   missions "made the whole top of the WM differential un-earnable
   (teleport) ... (pilot cycle #1 finding, 2026-06-10)". Substrate: the
   open-mission registry.
4. **`ticket-enumerator-proposer`** (`mission_registry.clj:419`): one
   `:advance-ticket` per live ticket, `{:open-hole-count 1}`. Substrate:
   the ticket queue.
5. **`sorry-enumerator-proposer`** (same registry family): one
   `:address-sorry` per open sorry. Substrate: the sorry registry.
6. **`tension/tension-proposer`**: emits existing S2 action classes at
   high-curvature substrate-2 nodes, "credited + admissibility-gated"
   (M-aif2 slice-1, consent-gated Joe 2026-06-01 per the state assembly
   comment at `war_machine.clj:6801`).

`portfolio_action_proposer.clj` existed but was **not** in the judge's
compose list; it was dark by default (`*portfolio-proposer-active?*
false`, its docstring: "the WM is byte-identical to the pre-portfolio
composition" when off), adding `:close-mission` / `:survey-mission` /
`:apply-cascade` families only when armed. D1's "portfolio" phrasing
therefore overstates: the portfolio proposer existed but was not composed
by the judge at this revision.

### What entered the ranking

The candidates were enriched before ranking — structural pressure,
mission value, the interest network, and the task-belief ladder
(`war_machine.clj:6834–6845`: `enrich-candidates-with-structural-pressure`,
`enrich-candidates-with-mission-value`, `interest-net/enrich-candidates`,
`apply-task-belief-ladder`) — and then ranked by

```clojure
wm-ranked-domain-base (efe/rank-actions wm-state wm-enriched-candidates wm-efe-opts)
```

(`war_machine.clj:6892`), where `wm-efe-opts` (`:6859–6886`) carried:
`:time-pressure` (anticipation proximity), `:horizon-steps`
(multi-horizon when anticipation loaded), `:belief-update-opts`, and the
arena modes `:ambiguity-mode`, `:risk-mode`, `:goal-outcome-mode`,
`:structural-pressure-mode`, `:predictability-control-mode`,
`:homeostatic-control-mode`, `:graph-feasibility-mode`,
`:move-class-intensity-mode` (dark). Habit log-priors were attached after
ranking when `:learned-frequency` was the prior source (`:6894–6900`).

### Where feasibility entered, in code

Three distinct places, quoted:

1. **Class addressability at proposal time** — `forward_model.clj:256`,
   the `can-propose?` multimethod: "Per-action-type capability check: can
   this action class be addressed against the current substrate? Default:
   false (the WM has no proposer for it)." The bootstrap proposer's
   `:learn-action-class` gap actions are generated directly from the
   `false` answers (`action_proposer.clj:34`, `gap-actions`).
2. **Instance admissibility** — `forward_model.clj:280`, `can-execute?`:
   "Per-action-instance admissibility check. ... `can-propose?` ... answers
   'is the action class proposable at all?'; `can-execute?` answers 'is
   this specific action instance executable in this state?'" (e.g.
   `:address-sorry` executable only when the target is in the state's
   open sorrys, `:289`).
3. **Feasibility inside the ranking** — the `:graph-feasibility-mode`
   opt, defaulting to `:policy-support`
   (`war_machine.clj:997–1003`, `arena-graph-feasibility-mode`:
   "Typed-residual remediation (2026-07-13): graph applicability is a
   policy support condition, not a value penalty"). In `efe.clj`,
   `rank-actions` begins with `partition-policy-support`
   (`efe.clj:1104`, body: `(partition-policy-support (:capability-graph
   opts) candidate-actions opts)`), and `graph-control-terms`
   (`efe.clj:216`) states the split: "In `:policy-support` mode,
   `rank-actions` excludes infeasible policies before scoring and
   `:graph-control-score` contains only the pragmatic proxy," while the
   historical `:score-penalty` mode priced inapplicability as
   1000·[not-applicable] plus the off-map penalty.

So feasibility entered twice and differently: as *what can be proposed at
all* (can-propose?, producing learn-the-gap candidates) and as a *domain
mask before scoring* (policy-support exclusion). It was not a weighted
consideration among proposed actions in the default mode — the
`FUTON_WM_GRAPH_FEASIBILITY_MODE=score-penalty` hatch was the comparison
path only.

## Q2. The instruction H5b cites

H5b's commit message (`5d55e7a089a8a96597737c1209a0ca2a8e846256`,
2026-09-17 16:58:50 +0000) reads "H5b: judge enacts the gated cascade
decision or abstains; flat decision path deleted (Joe 2026-09-17)".

The primary text is p4ng,
`wm-walkthroughs/build-loop/closure/FOCUS.md`, section "## Priority 0:
the enacted choice is not selected by cascade G (Joe, 2026-09-17)",
which records three rulings verbatim:

> **RULING (Joe, 2026-09-17): rip out the flat decision path.** It is not
> kept as a baseline or shadow. Production enacts a cascade decision or
> abstains with a typed reason.

> **RULING (Joe, 2026-09-17, second): make it impossible to run the
> machine with the flat decision.** Removing the code is not enough,
> because the error has come back four times. The machine has to refuse
> to run any flat decision, with nobody's discipline involved.

> **Principle (Joe, 2026-09-17): a cascade is a policy, and G is computed
> over policies.** A per-action quantity exists only as an abstraction
> over cascades: the action marginal of the cascade posterior
> (`ActionMarginal`, `cascade-selection/bayes-choice`). ... No G, score
> or rank computed over single actions may reach it.

The same file's SPEC (`SPEC-flat-removal-and-cascade-decision.md:5`)
restates them as "rip it out; make the flat decision impossible to run;
a cascade is a policy, and G is computed over policies."

**Do the rulings say anything about removing proposing?** No. Every verb
in both rulings and the principle is about the *decision*: the flat
decision path, running the machine with the flat decision, G/score/rank
"computed over single actions" reaching the decision. The proposers
appear nowhere in the rulings or in the "Work, in order" list. The only
adjacent sentence is work item 2: "Make `judge`'s enacted decision come
from `select-action-cascades` over cascade candidates **the tick builds
itself**" — a construction phrase, not a proposal-removal instruction.
Deleting the proposer call site alongside the flat ranking (H5b's
"Deleted with the flat path: compose-proposers candidates, channel
ranking, ...") is a reading of the ruling that the ruling's text does
not itself contain. The AUDIT written the same day marks the consequence
explicitly (`AUDIT-flat-action-grain-2026-09-17.md`, row 10): "a cascade
proposer is the missing adapter; the protocol is agnostic but every
proposer is flat."

## Q3. The source behind the `cascade_proposals.clj` docstring

The sentence — "An agent authors a complete cascade-source-v1 declaration
... Retrieval is explicit, outside the tick." — landed in `65e523ef`
(2026-09-21 03:42:45 +0000, "Supply pinned retrieval proposals without
automatic interpretation admission"; parent `631f3f04`). The commit ships
its own run record:
`holes/labs/wm-contract/runs/proposal-supply-b1-2026-09-21/STATUS.md`,
which names the two rulings it implements:

- **claude-12, Agency job `invoke-1789961312569-22838-7089b6ad`**
  (2026-09-21T03:28Z, caller codex-6, ~15 min before the commit): the
  interpretation boundary. STATUS.md quotes it as: "proposals carry
  evidence only. An agent writes the complete reading, guard, produces,
  locators and interpretation receipt. Signature prose and retrieval
  scores establish no applicability." The job's API record retains only a
  truncated summary ("...your boundary is the correct one... The line
  falls at judgment, not at the receipt..."); the full text is not in the
  record surface I could reach (`GET /api/alpha/invoke/jobs/<id>`,
  `.../result` does not exist).
- **claude-12, job `invoke-1789961691882-22844-1cd0d1fe`**: the
  target-link boundary ("the exact branch stays unavailable, recorded as
  such, rather than gaining U-rows as a target kind tonight").

So the docstring sentence's *first* half (agent authors the complete
declaration; never infer from prose) has a recorded ruling behind it —
an agent-to-agent ruling by claude-12, quoted in a committed run record.
The *second* half — "Retrieval is explicit, outside the tick" — is
justified in STATUS.md only operationally ("The tick reads these records,
never invokes retrieval inline"), i.e. as this packet's own design
decision. I searched p4ng (`grep -rn "outside the tick"`, `cascade-source-v1`,
`pinned retrieval`), `futon2/holes` RULINGS-*/NOTE-*/labs, and the
commit's parent line for a Joe ruling placing retrieval or construction
outside the tick: **none found**. The closest primary source placing any
construction duty *inside* the tick is the opposite direction — FOCUS.md
Priority 0 work item 2's "cascade candidates the tick builds itself"
(Q2). Searched: `grep -rn "outside the tick" /home/joe/code/p4ng
/home/joe/code/futon2/holes` (hits are unrelated: telemetry/projection
uses), `grep -rln "cascade-source-v1" /home/joe/code/p4ng
/home/joe/code/futon2/holes` (7 files, all run records or snapshots, no
ruling text), the `65e523ef` commit message and parent history, and the
two named Agency jobs (full texts not retained on the API surface).

## Q4. Was a cascade-level proposer planned to replace compose-proposers?

**Planned, named, and never dispatched.** The record:

- The AUDIT of the flat-action grain (`p4ng
  wm-walkthroughs/build-loop/closure/AUDIT-flat-action-grain-2026-09-17.md`,
  row 10) names the gap on the day of H5b: "a cascade proposer is the
  missing adapter; the protocol is agnostic but every proposer is flat."
- The construction-side plan exists: H7c-2 landed the four
  construction-library moves as move functions (futon2 `3b01790d`,
  review fixes `6a8665f2`, `59d6d4de`, all 2026-09-17), and FOCUS.md's
  2026-09-17 evening update says: "Construction can now propose families
  from moves rather than by hand. **Feeding them into the production tick
  is H7f, still open.**"
- H7f's row in the SPEC (`SPEC-flat-removal-and-cascade-decision.md:81`):
  "Feed H7c's families and receipts into H2 `cascade-problems` sources
  and through the lane; independent R9 check."

**What happened to it:** nothing. `git log --all --format=%s | grep -i
H7f` over futon2 finds no commit; FOCUS.md's last word on it is "still
open" (2026-09-17 evening); `interpretation_construction.clj`
(`4328238d`, 2026-09-21) and `construction_moves.clj` (`3b01790d`) still
have no production caller (E-cascade-real D4, verified independently
there). H5b made the judge assemble cascade problems "for substrate
targets from `:cascade-sources`" (commit message) — declared, hand-written
sources — and the H7f feed that would have replaced declaration with
construction was never wired. Note the asymmetry the excursion names:
H7f was about feeding constructed *families for given targets*; no
H-series row plans a proposer that chooses *which targets* to work — the
old proposers' job. `holes/N-backlog-as-cascade.md:42` toys with
"backlog-cascade proposers" as a future A/B-comparable mechanism, but it
is a note's speculation, not a dispatched plan.

## Searched and not found (explicit)

- No Joe ruling removing *proposing* (as opposed to flat ranking) —
  searched FOCUS.md, SPEC-flat-removal, AUDIT-flat-path-removal,
  AUDIT-flat-action-grain, futon2/holes RULINGS-*/NOTE-* via the Q2/Q3
  greps above.
- No ruling placing *construction* outside the tick — the only "outside
  the tick" text is the `65e523ef` docstring/STATUS.md itself.
- No H7f commit in futon2 (`git log --all | grep -i H7f`: none).
- Full texts of the two claude-12 ruling jobs: not retained on the
  Agency job API (summaries truncated; no `/result` endpoint).
