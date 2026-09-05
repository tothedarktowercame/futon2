# :B2 — seven strawman holes, seven refutation attempts, seven survivals

Every hole in `proposals/STRAWMAN-M-wm-aif-policy-grain-compliance.md` (4) and
holes 3–5 of `proposals/STRAWMAN-M-aif-policy-conditioned-eig.md` (3) was given
the cheapest experiment that could refute it, and none of the seven was
refuted; all seven are adopted into their mission docs marked machine-proposed.
The refutation attempt for E4 did discharge half of that hole's acceptance
(the boundary-rejection conjunct, already landed by `:F1`), so E4 was adopted
narrowed to the conjunct the experiment left standing rather than verbatim.

## What was run

`01-consumer-and-updater-probe.edn` — `b2_strawman_experiments.clj`, in the
JVM. Constructs the machine `Q(o|pi)` that `:F1` pinned
(`src/futon2/aif/machine_q.clj:374`; the `:acquisition` row reproduces
`runs/F1-machine-q/01-runtime-seam.edn` `:rows` at max abs difference 0.0) and
hands ONE row to each of the two consumers whose joint acceptance is E4's
acceptance evidence. The EIG kernel accepts it as `:predicted-observations`
(EIG 0.0 under a degenerate prior, which is the accept, not the number).
`core-efe/risk` — the only runtime risk consumer in the tree — has no way to
take it: as a map it throws `ClassCastException` rather than refusing with a
typed reason, and as the parallel scalar seqs its signature admits it computes
silently, returning 0.0 for the row and 1.170904 for the same row permuted. A
number that moves when a map's `vals` are reordered is a number the outcome
identity did not survive. The same script enumerates the whole public surface
of `futon2.aif.a4a` (13 vars) and `futon2.aif.bmr` (6 vars): none is named for
an update, an observation, a posterior or a fold, so E3's "route both paths
through one A4a/BMR updater" has no updater to route through yet.

`02-read-and-replay-probes.edn` — `b2_strawman_reads.bb`, no JVM. Each probe
reports the set it searched (306 `.clj` files under `src/`, `scripts/`,
`test/`) as well as the set it found, so an empty result reads as an exhausted
search rather than a mis-anchored pattern.

- **G1** — two `:candidate-source` values exist in the whole tree,
  `:coverage-saturation-frontier` (the Slice 1a threshold frontier) and
  `:p4ng-control-hypergraph` (mission grain, not cascade grain).
  `arguing_worlds.clj` never mentions `:semilattice`. The nearest existing
  non-threshold artifact, `holes/labs/slush-demo/findings/proposals/batch-2-worklist.json`,
  carries `{proposal, mission, patterns}` — a pattern bag with no ordering and
  no wiring, which mission invariant 2 says is not a policy identity.
- **G2** — the one committed cascade artifact with `truncated: true`
  (`holes/labs/M-evaluate-policies/exhibit/cascade-3-serve.json`, full size 14,
  budget 6) emits coverage-reward 6.242 where its own six `:shown` rows sum to
  1.921, and T-intensity 4.84 where the prefix sums to 2.109. Live construction
  was not attempted: `cascade_construct.py:56` imports `sentence_transformers`,
  which is not installed here.
- **G3** — `:cascade-prior-state` occurs 0 times in `src/`, `scripts/` and
  `test/`; `cascade_prior.clj` is required by nothing outside itself and two
  test namespaces.
- **G4** — `shadow-rank` has no non-test call site, and
  `holes/labs/M-wm-aif-policy-grain-compliance/` does not exist.
- **E5** — all five fields the acceptance requires persisted (prior entropy,
  expected posterior entropy, `:expected-information-gain`, degeneracy reason,
  realised information gain) occur 0 times in the tree, and
  `holes/labs/M-aif-policy-conditioned-eig/` does not exist.

`03-refused-set-replay.edn` — `b2_refused_set_replay.clj`. Both missions sat at
`:open-hole-count 0`, where `task-belief-ladder/classify` overrides every
support rung with the typed rung-3 `:no-open-holes` refusal
(`src/futon2/aif/task_belief_ladder.clj:262`). Replayed at the counts the
registry reports after the adoption — 4 for grain-compliance, 3 for
conditioned-eig — that override no longer fires: over an empty case history the
verdict stays rung 3 but its rule changes from `:no-open-holes` to the ordinary
`:construction-exhausted`, and over a field carrying one persisted decision per
candidate it moves to rung 1 at factor 0.4 and 0.375, which is the `h/(h+1)`
scaling the hole rule was zeroing. No live run: the counts come from the same
`mission-registry/mission-status` the field build reads, and everything else is
pure.

## What this is not

No ruling. Nothing was written to `aif-equations.edn :choices` or
`control-map-edges.edn :decisions`; the strawman QUESTIONS in both proposal
files are Joe's and are untouched. `gen_aif_dag.bb` was not run into a publish.
No live run, no tick, no run lock, no substrate call, no network; nothing was
written under `data/`.
