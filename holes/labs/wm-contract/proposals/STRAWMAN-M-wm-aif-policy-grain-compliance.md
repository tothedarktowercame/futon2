# DRAFT FOR JOE'S REVIEW: strawman holes for M-wm-aif-policy-grain-compliance

This proposal does not alter the mission. Each item is independently
accept/edit/strike-able.

## QUESTIONS

1. **query** — Slice 1b requires comparison with arguing-worlds/slush and
   diversity in both membership and wiring
   (`holes/missions/M-wm-aif-policy-grain-compliance.md:119`). Which existing
   arguing-worlds or slush artifact should be the first authoritative adapter,
   and what same-mission case should demonstrate that it adds a genuinely new
   topology rather than another threshold prefix?
2. **challenge** — The mission says truncation currently leaves a full-cascade
   score attached to the shorter policy that is actually folded
   (`holes/missions/M-wm-aif-policy-grain-compliance.md:19`). Should acceptance
   require a receipt proving every selection term was recomputed from the exact
   `:shown` and `:semilattice`, while full-construction telemetry remains
   explicitly non-selective as required at
   `holes/missions/M-wm-aif-policy-grain-compliance.md:115`?
3. **query** — The persistence slice deliberately leaves the increment event
   preregistered but undecided
   (`holes/missions/M-wm-aif-policy-grain-compliance.md:152`). Is structural
   construction validation the intended habit event, and which cancellation,
   fold failure, and enactment failure records must prove that no habit mass was
   added?
4. **suggest** — Before arming tactical choice, the mission asks for useful
   non-degenerate support and no increase in construction failures
   (`holes/missions/M-wm-aif-policy-grain-compliance.md:166`). Would you accept
   a preregistered shadow threshold that names the minimum useful-case fraction
   and the incumbent failure baseline, so the eventual flip is a decision over
   recorded criteria rather than an after-the-fact reading?

## STRAWMAN HOLES

- [ ] **Adapt one non-threshold diversity source for Slice 1b.** Produce a
  read-only adapter from one named arguing-worlds or slush artifact into the
  complete cascade-candidate schema. **Acceptance evidence:** a pinned fixture
  for one mission yields at least two foldable candidates including the
  incumbent, with a deterministic seed/deposit and an explicit difference in
  membership or semilattice wiring.
- [ ] **Make cascade scoring prefix-local and auditable.** Recompute every
  candidate selection term from that candidate's exact `:shown` and
  `:semilattice`, retaining full-construction measurements only under distinct
  telemetry keys. **Acceptance evidence:** a truncation fixture proves the
  selected score equals a direct recomputation on the enacted prefix and proves
  that changing only discarded suffix telemetry cannot change its rank.
- [ ] **Specify and persist the cascade-habit return event.** Add a distinct
  versioned `:cascade-prior-state`, pin the complete selected identity, and
  record the one reviewed event that increments it. **Acceptance evidence:**
  round-trip/replay tests show a successful qualifying event increments once,
  failed or cancelled construction does not increment, and legacy traces with
  no reconstructible identity cold-start rather than acquiring fabricated
  mass.
- [ ] **Run a dark end-to-end hierarchy shadow.** Exercise candidate
  construction, prefix-local ranking, fold/gate, and trace production without
  changing live selection. **Acceptance evidence:** a committed report states
  the non-degenerate useful-case fraction, incumbent and candidate construction
  failure rates, separately named `E_scheduler`/cascade-score/`E_cascade`
  terms, deterministic replay results, and byte identity with the feature off.

## BASIS

I read the mission's grain-mismatch diagnosis, candidate contract, persistence seam, and operator-gated shadow criteria (`holes/missions/M-wm-aif-policy-grain-compliance.md:12`, `holes/missions/M-wm-aif-policy-grain-compliance.md:91`, `holes/missions/M-wm-aif-policy-grain-compliance.md:152`, `holes/missions/M-wm-aif-policy-grain-compliance.md:166`). I also checked the registry's countable-hole syntax at `src/futon2/aif/mission_registry.clj:34`, so every proposed hole is an unchecked task item that would count if Joe pasted it into the mission.

## EXPERIMENT VERDICTS (`:B2`, 2026-09-05)

Every strawman hole above was given the cheapest experiment that could have
refuted it — refuted meaning *the work it proposes is already done, so writing
it down would be asking for it twice*. None of the four was refuted; all four
are adopted into the mission doc marked machine-proposed with the measurement
cited, in commit `27a6dd5b`.

**Reversal: `git revert 27a6dd5b`.** That commit adds one section to
`holes/missions/M-wm-aif-policy-grain-compliance.md` and touches nothing else,
so the revert is complete and needs no follow-up.

The QUESTIONS above are untouched. They are Joe's, and no answer to any of them
is recorded here or anywhere else by this row.

| Hole | Verdict | The experiment, and what it measured |
|---|---|---|
| Adapt one non-threshold diversity source for Slice 1b | **validated** | Enumerate every `:candidate-source` in `src/`, `scripts/`, `test/` (306 `.clj` files). Two exist: `:coverage-saturation-frontier` (`scripts/futon2/report/cascade_lane.clj:126` — the Slice 1a threshold frontier this item is meant to be compared against) and `:p4ng-control-hypergraph` (`src/futon2/aif/mission_control_graph.clj:126` — mission grain, not cascade grain). `src/futon2/aif/arguing_worlds.clj` never mentions `:semilattice`. The nearest existing non-threshold artifact is `holes/labs/slush-demo/findings/proposals/batch-2-worklist.json`: `{proposal, mission, patterns}`, a bag with no ordering and no wiring, which invariant 2 says is not a policy identity. `runs/B2-strawman/02-read-and-replay-probes.edn` `:G1` |
| Make cascade scoring prefix-local and auditable | **validated** | A replay, not a read. The one committed cascade artifact with `truncated: true` (`holes/labs/M-evaluate-policies/exhibit/cascade-3-serve.json`, full size 14, budget 6) was re-scored over its own six `:shown` rows: it emits coverage-reward 6.242 against a prefix sum of 1.921, and T-intensity 4.84 against 2.109. The code path agrees — `cascade_construct.py:206` takes no budget and sums coverage over the whole trajectory, `cascade_serve.py:27` truncates afterwards and `:30` recomputes only the semilattice. Live construction was not attempted: `cascade_construct.py:56` imports `sentence_transformers`, which is not installed on this machine. `runs/B2-strawman/02-read-and-replay-probes.edn` `:G2` |
| Specify and persist the cascade-habit return event | **validated** | `:cascade-prior-state` occurs 0 times across the 306 `.clj` files; `src/futon2/aif/cascade_prior.clj` is required by nothing outside itself and its own two test namespaces. The Slice 0 kernel has no persistence seam to extend, so the item is minting one rather than amending one. `runs/B2-strawman/02-read-and-replay-probes.edn` `:G3` |
| Run a dark end-to-end hierarchy shadow | **validated** | `cascade-prior/shadow-rank` has no call site outside its own namespace and its tests, and `holes/labs/M-wm-aif-policy-grain-compliance/` does not exist. No shadow corpus has ever been collected. `runs/B2-strawman/02-read-and-replay-probes.edn` `:G4` |

The consequence for the field, replayed rather than run: this mission sat at
`:open-hole-count 0`, where `task-belief-ladder/classify` overrides every
support rung with the typed rung-3 `:no-open-holes` refusal
(`src/futon2/aif/task_belief_ladder.clj:262`). At 4 that override no longer
fires — over a field carrying one persisted decision the candidate moves from
factor 0.0 refused to rung 1 at factor 0.4, which is `h/(h+1)` scaling the
support instead of zeroing it (`runs/B2-strawman/03-refused-set-replay.edn`).
