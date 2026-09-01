# C399 — serving topology code closure

Date: 2026-09-01

This is a discovery record.  It does not change the operational certificate or
install a code pin.

## Result

The serving runner file is not, by itself, the topology-bearing code set.

There are two useful boundaries, and they must not be conflated:

1. **Route-shape authorship.**  `scripts/futon2/report/war_machine.clj`
   constructs the route tags `R20`, `R12`, `R2`, `R7`, `R3`, `R8`, `R5`,
   `R6`, and `R14`.  `src/futon2/aif/full_loop_runner.clj` chooses that
   producer for the production CLI path, conditionally appends `TRACE`, turns
   the tag vector into adjacent hops, and persists the run record.  These two
   files are the minimum source set that fixes the successful production
   route's vocabulary, ordering, and serialization.
2. **Traversal semantics and completion.**  The calls represented by those
   tags run through `futon2.aif.observation`, `futon2.aif.precision`,
   `futon2.aif.free-energy`, `futon2.aif.efe`, `futon2.aif.policy`, and
   `futon2.aif.trace`, with belief, forward-model, preference, habit-prior,
   action-proposer, selection-gain, and related War Machine dependencies
   affecting the values and whether the route completes.  The `R14` result is
   supplied by the external Agency HTTP boundary in production.  Those
   dependencies bear the meaning and successful execution of a traversal;
   they do not independently author the fixed hop sequence.

The distinction is observable in the code.  `route-tag` call sites live in
`war_machine.clj`; `observed-route` and `persist-run-record!` live in the
runner.  The runner also accepts `:judge-fn` and
`:strategic-selection-invoke-fn` injection seams.  The ordinary CLI does not
populate either seam, but their existence means the runner namespace alone is
not a closed description of every executable route-producing configuration.

## Can the closure be derived?

Only a coarse namespace closure is derivable mechanically.

The `ns :require` graph can enumerate namespaces loaded from the checked-out
tree.  It cannot establish an exact topology-bearing call closure:

- route provenance strings such as `"futon2.aif.efe/rank-actions"` are data,
  not executable links;
- higher-order option seams can replace the judgment or the external selector;
- `requiring-resolve` introduces runtime dependencies not represented by a
  fixed direct-call graph;
- live Clojure Vars can be redefined in a long-running JVM; and
- the Agency selector is deliberately outside this JVM and is reached over
  HTTP.

Hashing the transitive namespace graph would therefore be exact for loaded
source bytes only at the cost of being too coarse: it would include many
changes that cannot alter route shape while still failing to prove that no
runtime injection or Var replacement occurred.  Conversely, following only
literal call sites or `:via` strings would be incomplete.  Neither should be
presented as a machine-complete closure.

The honest future certificate shape is consequently a **declared boundary**:

```clojure
{:topology-code-boundary
 {:route-shape-sources
  ["scripts/futon2/report/war_machine.clj"
   "src/futon2/aif/full_loop_runner.clj"]
  :execution-semantic-namespaces
  '[futon2.aif.observation futon2.aif.precision futon2.aif.free-energy
    futon2.aif.efe futon2.aif.policy futon2.aif.trace]
  :external-boundaries [:agency-strategic-selection]
  :runtime-injection-seams [:judge-fn :strategic-selection-invoke-fn]
  :machine-complete false
  :boundary-reason :clojure-runtime-call-closure-not-exactly-derivable}}
```

That declaration could be checked for source identity and for absence of
injected seams in the particular production run.  It would not assert that the
listed namespaces are the complete semantic dependency closure.  A future
implementation should also identify loaded Var roots or class/JAR identities
at the run boundary if the serving process can be reloaded; a checkout digest
alone does not identify what a long-lived JVM is executing.

## Bottom line

One runner digest proves too little.  Two source identities are sufficient for
the narrow claim “these bytes authored and serialized this fixed route shape”
only when the production judgment seam is shown not to have been replaced.
They are not sufficient for the broader claim “all code affecting the meaning
or completion of this traversal is closed.”  That broader set must currently
be declared with `:machine-complete false` and the external and injection
boundaries made explicit.
