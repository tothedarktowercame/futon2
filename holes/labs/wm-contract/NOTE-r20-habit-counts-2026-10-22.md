# R20 habit-count discovery — 2026-10-22

Read-only discovery for claude-2; no runtime changes. Inspected checkout
`234efdac725ebd10e56a61f41b1d907e51733b56`. Line references are relative to
futon2. The two recent clicks **do not consume the W_c habit fold**. They use
the sealed outer-policy route, whose ln E is currently a different quantity.
Recording more fields alone will not establish the R15/R20 habit contract.

## 1. Where the counts live, and whether live clicks use them

The durable authority is `<machine-interpretations-dir>/flights/*.edn`, at
`[:flight :enactments i :increment]`, not a mutable replacement count file.
The default directory is `/home/joe/code/futon2/data/wm-interpretations/flights`
(`src/futon2/aif/want_interpretation.clj:359`).

- `src/futon2/aif/flight_runner.clj:1021` (`wc-verdict-fn`) obtains the checker
  verdict and returns the increment at line 1045. The flight driver persists
  the containing flight at `src/futon2/aif/flight_driver.clj:241`–248.
- `src/futon2/aif/enactment_fold_source.clj:36` extracts those receipts;
  `fold-from-flights`, line 63, rebuilds state from scratch, adding file paths,
  hashes, and read diagnostics under `:folded-from`.
- `src/futon2/aif/enactment_habit.clj:88`–118 defines the increment:
  W_c pass is an empty verdict vector and gives delta 1; failure or absent
  verdict does not count. Identity/outside-candidate refusals also do not count.
  `fold`, line 120, deduplicates successful receipts by `[click candidate]`.
  Counts are keyed by structural policy identity (including reading-policy
  keys), not by the observation-class vocabulary.
- `src/futon2/aif/cascade_prior.clj:68` defines the cold state with `:counts {}`,
  `:samples 0`, and positive smoothing alpha. `observe-policy`, line 108,
  increments `:counts` and `:samples`. Lines 156–170 normalize count + alpha
  over distinct menu identities, splitting mass for duplicate identities.

The report still constructs/passes the fold at
`scripts/futon2/report/war_machine.clj:6832`–6838. However, the current sealed
outer-policy handoff takes the early branch in
`src/futon2/aif/wm/cascade_decision.clj:1332`–1336, projecting the already
selected policy without calling the fold-consuming selector. Therefore the
fold may be computed as an unused argument; this is not evidence that it
influenced the live choice.

Concrete run evidence (both files are one-line EDN; paths below are EDN paths):

- `data/wm-runs/tick-run-record-2026-10-21-fcf06c3f-8c08-4c55-a322-632090fa7220.edn:1`
- `data/wm-runs/tick-run-record-2026-10-21-752b48a7-6d82-4a0e-b888-9966e8c46116.edn:1`

Both have `[:decision :selection-certificate :selection-route] =
:wm/selected-outer-cascade-policy-v1`, `:selection-basis {:kind
:strict-objective}`, empty `[:habit-reads :occurrences]`, and the typed legacy
reinforcement retirement. These establish non-consumption on these clicks,
not a claim that no older/other route ever consumed the fold.

Current disk census: all seven top-level flight EDNs in the default directory
have one enactment entry, status `:no-progress`, and no increment map. Example:
`data/wm-interpretations/flights/flight-d00574c8.edn:849` has
`:enactment {:absent :no-decision}`; line 857 says `:no-progress`.
Thus the source reader extracts zero receipts from this directory today and
its fold would have empty counts/zero samples. This is a present filesystem
observation, not a reconstructed snapshot of the directory at either click.

## 2. The E actually used by these live clicks

The production provider is installed at
`src/futon2/aif/full_loop_runtime.clj:346`.
`src/futon2/aif/outer_policy_provider_runtime.clj:94` loads learning state and
builds policy evidence. Its `policy-terms`, lines 36–43, sets:

    ln E = -weighted-resource-cost
           -recent-selection-opportunity-cost
           +continuation-credit

`src/futon2/aif/outer_policy_evidence.clj:201`–202 retains the components and
sums them. `src/futon2/aif/meta_live_outer_selector.clj:656` calls the objective;
`src/futon2/aif/outer_policy_objective.clj:86`–96 computes
`objective = gamma * G - ln E`, then softmaxes the negative objectives and
selects a minimum (drawing uniformly among exact ties). The sealed handoff
projects that choice at `src/futon2/aif/selected_outer_cascade_policy.clj:158`.

In **each** cited run, all six rows under
`[:outer-task-selection :policy :policy-objective :policies]` have
`[:terms :lnE] = -0.05`, resource = -0.05, cadence = -0.0,
continuation = 0.0, and gamma = 1.0. Component source pins name
`wm://existing-outer-controller-inputs`. These equal prior weights are
**effectively uniform over the six policies after normalization** (1/6 each),
but the retained -0.05 is not itself log(1/6), and its source is not success
counts. Different resource/cadence/continuation inputs can make this prior
nonuniform. The provider's Dirichlet outcome counts are a separate predictive
model, not the retired habit counts.

For comparison, the alternate selector in `src/futon2/aif/policy.clj:434`–454
coerces the enactment fold and calls `cascade-habit-store/attach-state`.
That is the count-derived E path; its existence does not establish consumption
on the sealed route. Gamma on the current outer route is read from
`[:authority :outer-policy :gamma]` at
`src/futon2/aif/outer_policy_provider_runtime.clj:53`, not derived there from
this fold.

## 3. Why `:never-read`

The logger is working as implemented. The runner allocates and binds its atom
at `src/futon2/aif/full_loop_runner.clj:8155` and 8178, and retains it at
1211–1212. `src/futon2/aif/scoring_input_receipts.clj:32` calls an empty observed
log `:absent / :never-read`.

`src/futon2/aif/cascade_habit_store.clj:100`–138 already records the full folded
state, snapshot hash, candidate identities, keys, and consumed masses when
`attach-state` runs. The sealed outer route bypasses that call. The file-level
fold reader itself does not append to this log. So `:never-read` means **no
logged habit-consumption occurrence**, not necessarily no filesystem read or
no evaluation of `fold-from-flights`. This is chiefly a bypassed consumer,
not a missing receipt around an existing count-based live policy prior.

## 4. Smallest change to make the requested evidence real (proposal only)

Separate instrumentation from the behavioral gap:

1. Retain a selection-time fold snapshot with counts, alpha, class/key mapping,
   admitted receipt identities and source hashes; mark whether it was actually
   consumed. On today's route, `:consumed? false` is the truthful value.
   Merely populating `:habit-reads` with an unused snapshot would be misleading.
2. At the witnessed-outcome boundary retain the selected class/key, witness
   and outcome, admission verdict, unique event id, before-counts, delta and
   after-counts. Reuse the existing W_c increment/deduplication rule where
   that is the intended success authority. A runner close or selection alone
   is not a W_c success. Persist the receipt before the next selectable click;
   ordinary clicks currently need an explicit connection to this durable
   receipt authority. Do not revive the retired ungated close writer.
3. At the next selection retain the exact snapshot actually used, predecessor
   receipt/hash, menu-to-class mapping, normalization and resulting E.
   Recompute from the authoritative receipts; check the previous after-state
   matches the next before-state, accounting explicitly for intervening events.
4. To satisfy R20, feed that same E to the **actual outer objective and precision
   update**, retaining both consumption receipts. This requires a semantic
   wiring change: current evidence/objective validators explicitly allow/sum
   resource, cadence, continuation only
   (`outer_policy_evidence.clj:12,155`–176;
   `outer_policy_objective.clj:74`–78). Define how the success-count prior
   relates to those existing terms, update the sealed evidence contract and
   recomputation together, and connect the precision consumer. A logger-only
   patch cannot claim this property.

Resolve the count grain and normalization explicitly before implementation:
existing counts are per structural policy, include smoothing, and split
multiplicity. The R20 catch-up note limits its current witness to menus with
one policy per class. Do not silently equate observation classes, targets,
structural policies, or raw success counts with smoothed concentrations.
The existing fold is reusable accounting machinery, not a completed R20 wire.

Validation: inspected source and parsed both run records plus every top-level
flight EDN with Babashka. A direct namespace load under bare `bb -cp src`
failed on missing `clojure.data.json`; no fold execution is claimed above.
The zero-receipt conclusion follows from the parsed disk census and inspected
pure fold definition. No live clicks, checker calls, runtime writes, or test
suite runs were performed.
