# Fix 9 discovery — what a recorded cascade's shape establishes

2026-09-21, codex-13; branch `fix/narrative-9`, base `c81e4e00`.
Discovery only: no production edits, live loads, clicks, store writes, or Lean
builds. The discovery test namespace is `futon2.report.cascade-shape-discovery-test`;
its output and finite-control results are in [fix-9-evidence](fix-9-evidence/). Historical input hashes are retained there.

The records establish a priority list of interpreted patterns and a computable
**declared token-dependency graph**. They do not establish the upward-closed
`@why` authority structure described in p4ng. The reference construction's
`:semilattice []` happens to accompany a one-box/no-wire construction, but it
is a literal, not a measurement. Neither an ordered vector nor a nonempty DAG
is evidence of an overlapping, intersection-closed family.

There is also a correction to the checklist: **`organise` is already called in
the serving diagnostic cascade lane**. Its input repository is synthesized with
no authority edges, and its output is dropped when joint selection constructs
its action maps. The task is to supply and preserve warranted structure, not
just insert a previously uncalled function.

## 1. Four different structures must retain their names

References below are relative to futon2 at the base commit unless absolute.

| Retained/computable object | What it establishes | What it does not establish |
| --- | --- | --- |
| `:precedence`, interpreted guards and `:produces` in certificate candidates' `:id` action | A priority list; which positive/negative tokens each pattern reads and which tokens its model adds. | Chronological execution, observed completion, or authority edges. |
| For patterns P,Q, `produces(P) ∩ positive-needs(Q) ≠ ∅` | A declared possible producer-to-consumer need edge P→Q, with the intersecting tokens as witnesses. | That P was necessary, was fired, or caused Q: a token may already be observed, or have another producer. Negative guards are inhibitions, not positive need edges. |
| `fold-cascade/realize` boxes and `:wire/declared-dependency` wires | An admitted source/guard-backed construction projection, over boxes that passed its checks. Wires retain token/observation witnesses. | `@why`, `BV.copar`, meet closure, or a causal execution trace. Missing boxes/holes make this a partial projection. |
| `@why` / authored `:stands-on`, with source-bound admissions | The repository authority relation needed for p4ng's upward closure. | Not present in any of the 51 candidate action maps inspected. An interpretation receipt's `:source` identifies text; it does not itself retain parsed authority edges. |

`CascadeEFE.lean:48–95` filters precedence by node membership, resolves the whole
firing list, then `choose` takes the first enabled guard and its kernel. One
acting pattern per model step is linear **execution**, not a proof that the
support structure is linear. For example `[PAR, bounded, stop]` can consult PAR
first yet fire stop before PAR once the latter's prerequisite is needed.

The Clojure constructor makes these distinctions observable:

- `interpretation_construction.clj:39–57` derives token need edges and checks
  reachability with the existing model; its final candidates retain
  `:need-edges` at lines 141–149.
- `cascade_problems.clj:183–184` retains only candidate ID, precedence and
  construction receipt, dropping those need edges. They remain recomputable
  from the interpreted patterns, but their original construction provenance is
  lost at that projection.
- `war_machine.clj:6140–6165` builds joint action maps afresh and does not retain
  repository/organiser output.
- `full_loop_runner.clj:1321–1336` copies the selected precedence but writes
  `:semilattice []` unconditionally.
- `fold_cascade.clj:100–107` derives wires only from admitted boxes, declared
  outputs and positive observed input conditions. It does not read that literal.
- `full_loop_runner.clj:1493–1518` uses this interpretation fold for selected
  cascades; the old `semilattice-fold v1 (descent=BV.seq, co_app=BV.copar)` test
  remains in the **legacy** branch.

A shape report must retain these distinct bases. Replacing `[]` with a list of
need edges and calling them authority or co-application would change the false
claim, not repair it.

## 2. Pure shape definition and its limits

### A computable basis for slice 1

Use the finite carrier V of the selected patterns, with target-qualified tokens
and unique pattern IDs. For the currently recorded conjunctive, interpreted
guards, derive labelled need edges P→Q as above. Reject unsupported guard forms,
missing interpretations and cycles with typed findings. In particular, an OR
of clauses needs alternative enabling sets; unioning its clauses would invent
joint requirements. Do not add precedence-neighbour edges.

For each p, let **S(p) = {p} ∪ {q : q reaches p through need edges}**.
The finite family F = {S(p) : p∈V} consists of principal prerequisite-support
sets. Reflexivity retains isolated/preparatory patterns. Its elements are sets
of **pattern IDs**, not outcome tokens. Record both the pattern→support map
and the distinct family, its basis (`:declared-need-support-v1`), labelled edges,
and derivation/input digest. It is a static dependency account, not observed
causality. For the records here, every graph is acyclic and every support set
is distinct; the census checks this.

Define `overlap(A,B)` iff their intersection is nonempty and neither contains
the other. Define `closed?(F)` iff every pairwise intersection is a member of
F. Check the family actually supplied; **do not silently adjoin the empty set
or missing intersections**. Adding them would assert construction units not
in the record. A finite nonempty intersection-closed family is a meet
semilattice under inclusion, with meet equal to intersection.

Use this ordered classification, with separate Boolean witnesses retained:

1. One family member: `:singleton`.
2. More than one, every pair comparable by inclusion: `:chain`.
3. More than one, pairwise disjoint: `:antichain` (for a complete principal
   support family this is exactly an edgeless pattern order).
4. Otherwise, no overlapping pair: `:tree`, explicitly meaning Alexander's
   **laminar-family/tree condition**, not necessarily one connected rooted graph.
   Record component count and `:connected?`; disconnected laminar families are
   forests in graph terminology. This convention agrees with p4ng's table
   calling the disconnected G1/snatcher a tree but not a meet semilattice.
5. Otherwise, intersection-closed: `:semilattice`, i.e. an **overlapping** meet
   semilattice in this mutually exclusive display vocabulary.
6. Otherwise: typed unclassified `:overlap-not-intersection-closed`, with the
   offending pairs/missing intersections. The five labels cannot honestly
   cover every possible family. Empty candidates likewise get `:empty-family`,
   not an invented singleton or an acting no-op.

The labels intentionally have precedence: a chain is also mathematically a
meet semilattice, but it displays as `:chain`. Keep `:overlap?`,
`:intersection-closed?`, connectivity and witnesses so that the display label
cannot erase that fact. A cycle is not made acyclic by collapsing support sets
without a declared quotient.

Concrete controls, evaluated by the discovery script:

```
{{a}}                                  => singleton
{{a}, {a,b}, {a,b,c}}                   => chain
{{a}, {b}}                             => antichain; no meet in this family
{{a}, {b}, {a,b}}                       => tree; missing intersection {}
{{a}, {a,b}, {a,c}, {a,b,c,d}}           => semilattice; overlap meet {a}
{{a,b}, {b,c}}                         => unclassified; missing meet {b}
{}                                     => empty-family
```

### Which family is Alexander-relevant?

Alexander's distinction concerns **membership and overlap among actual units
of organisation**, not a count of edges or a temporal ordering. Shared
prerequisite patterns are a defensible, explicitly limited instance: two
constructions can share P without one containing the other. F_need measures
that overlap, including preparatory P in the P/Q case. A direct
`produces(pattern) ∩ wanted-tokens` family maps P to the empty set when P only
produces Q's prerequisite. Dropping that empty coverage unit erases P; retaining
it gives a formal bottom meaning “no direct wanted output,” not the fact that
P enables Q. Neither choice recovers the dependency from coverage alone. More generally,
shared outputs can be alternative/redundant producers rather than warranted
co-application. Served-want sets alone do not establish the intended structure.

For the **Alexander-relevant authority family**, use **authority extents**,
not the sets of ancestors of each leaf. In the retained upward-closed authority
graph, with edges `pattern → what it stands on`, define
**D(a) = {p : p=a or p reaches a}**: the pattern units whose authority scope is
a. F_authority = {D(a)} includes every node of the source-bound closure. A
rooted authority tree has a root extent containing the whole tree and disjoint
sibling extents, hence a laminar family. Two incomparable authorities supporting
a shared subordinate unit have crossing extents: this is membership in
several wholes, the overlap Alexander's contrast concerns.

**Orientation matters.** Ancestor sets `{root,left}` and `{root,right}` already
overlap in an ordinary three-node rooted tree; calling that an overlapping
Alexander organisation would misclassify every branching hierarchy. This is
why the report does not promote F_need's prerequisite-support reading into an
authority classification. They are explicitly different bases. Nor is the
paper's greatest-common-authority predicate automatically the same as closure
of authority extents: in the order `pattern ≤ authority`, common authority is
an upper-bound question, whereas intersection of extents is a common-supported-
units/lower-bound question. Retain both tests with the orientation declared.
For example X standing on two unrelated authorities A and B gives extents
`{{X},{X,A},{X,B}}`, closed with an overlapping meet `{X}`, but A and B have
**no common authority**. A report must not declare p4ng's common-authority test
passed from that extent-family result.

Thus slice 1 can honestly report the **computed need-support shape** and a
separate `:authority-graph-not-retained`. The authority extents and
common-authority check require a verified full graph, including warrant-only
nodes, and cannot be recovered from the candidate maps alone. Historical
source revisions/hashes may permit a separate reconstruction from pinned blobs;
reading today's unpinned library would not establish the historical family.

`app-snatch.tex:133–199` itself separates overlap from meet existence and lists
all four combinations. Its operational “some pattern stands on two authorities”
statistic is not generally identical to the set-overlap predicate above: a
redundant transitive edge can increase out-degree without changing a single
support set. Record any graph-degree statistic separately and specify edge
orientation/transitive reduction. Likewise `BV.seq`/`BV.copar` operators and a
field named `semilattice` do not constitute an intersection-closure proof.

## 3. Census of the retained cases

Scope: **all 51 certificate candidate rows** in the three named run records,
not just the chosen action, plus their three selected trace forms, all seven
reference-attempt checkpoints, and the actual fix-5a P/Q constructor fixture.
This is not a claim to have searched every historical run directory. The
machine-readable census enumerates every row, including each empty target.
The three run records carry 24, 24 and 3 candidates, respectively.

Let O = `coordination/par-as-obligation`, B = `coordination/bounded-execution`,
S = `futon-theory/stop-the-line`.

| Run / candidate | Priority vector | Derived need edges | Prerequisite family | Shape / meet status |
| --- | --- | --- | --- | --- |
| 1789964661, `M-aif-policy-conditioned-eig / C1` | `[apparatus/one-authority-per-question]` | none | `{{one-authority}}` | singleton; closed |
| 1789964661, `M-f11-find-production-successor / C1` | `[apparatus/done-is-observed-running]` | none | `{{done-observed}}` | singleton; closed |
| 1789964661, `M-wm-08-external-f2 / C1` | `[cascade-construction/run-it-on-a-real-case]` | none | `{{run-real-case}}` | singleton; closed |
| 1789951020, EoI / C1 | `[O,B,S]` | `S→O` | `{{S}, {S,O}, {B}}` | tree condition / forest, 2 components; not closed (missing `{}`) |
| 1789951020, EoI / C2 | `[O,S,B]` | `S→O` | same | same; not a dependency chain |
| 1789952479, EoI / C1 and C2 | same respective vectors | `S→O` each | same | two more identical forest shapes |
| Both earlier runs, `M-wm-08-external-f2 / C1` | `[run-real-case]` | none | `{{run-real-case}}` | two more singletons |
| Both earlier runs, remaining 21 candidates each | `[]` | none | `{}` | 42 typed empty families |
| fix-5a actual constructor result | `[P,Q]` | `P→Q` via `:q` | `{{P}, {P,Q}}` | chain; closed |

Thus the 51 historical rows contain **five singletons, four laminar forests,
42 empty families, and no overlapping family** on the declared-need basis.
The P/Q fixture adds one genuine chain. Priority vectors of length three would
all be chains if artificially converted to neighbour edges; that is precisely
the extra structure the evidence does not warrant.

EoI's outputs are three distinct tokens: O produces
`:obligation-resolved-through-the-account`, B produces `:change-authored-and-bound`,
and S produces `:premise-refused-before-work`. Only O positively needs another
member's output, namely S's. B's output is not an input to O in the retained
interpretation; the prose about a correction cycle cannot add that missing
edge. Both orderings have the same static supports but can have different
first-enabled execution orders/costs. Shape must not replace precedence in
policy identity.

For the reference run the singleton output tokens are respectively
`:hole/h6378c65a4012`, `:hole/h9ab212b3281d`, and
`:route-a-rehearsal-reported` (each target-qualified). All candidate
`:semilattice`, `:need-edges`, `:organised`, `:repository` fields are absent.
The selected `003-construction.edn` instead contains the literal `:semilattice []`
and a `fold-cascade/realize` wiring with **one box, zero wires** (duplicated in
its retained representations). Other reference checkpoint maps add no structural
fields. Trace forms 1–3 select, respectively, an empty M-dionysus-winddown/C0,
EoI/C1, and the one-authority singleton; they introduce no extra structure.

## 4. What the organiser and Lean carrier actually establish

`cascade_policy/organise:118–165` takes a previous cascade, a selected **set**,
repository pattern set plus directed authored `:stands-on` edges, attributed
admissions, a temperament/closure policy/precedence, and acting-order and score
ports. It validates repository membership, admissions and acyclicity. It can
add upward-reachable support nodes under `:stands-on-up-closure`; under
`:selected-only` it adds none. It emits a diff with source edges, nodes,
selection/admission/addition provenance, before/after priority, acting order and
score, checked against seven ruled clauses:

- osel/oauth/oattr preserve selection, authored graph and attributed admissions.
- O1 records the union of selected, organiser-added and admitted nodes.
- O2 permits only authored-reachable organised edges.
- O3 requires precisely fast-forward edges on **nodes minus organiser-added
  nodes**. The runtime subtraction is literal; do not repair its appearance by
  adding edges through a larger endpoint carrier. Support additions are retained
  in nodes but excluded from these organised-edge endpoints. Paths may pass
  through omitted intermediate repository nodes.
- O4 forbids changing precedence while both acting order and score stay equal.
  It is a consequence check, not an optimisation procedure or a demand that G
  improve. The caller supplies the new precedence and semantic ports.

It does **not** compute set intersections, test laminarity/meet closure, invent
co-application, or guarantee a semilattice. It does not choose the best order.
`fast-forward-edges` is an authored-reachability projection, not an arbitrary
transitive closure or a BV composition proof.

`F12RuledCarrier.lean:18–78` defines the ruled signature/conformance predicate
and a noncomputable existence witness. The witness returns `selected ∪ admitted`,
**no added nodes**, fast-forward edges on that union, **both precedence and
acting-order fields empty**, and both scores equal to the inhabited default.
It proves the seven clauses, not a useful optimiser or a meet-semilattice
property. Lines 83–125 instantiate the 11-selected/9-admitted, 20-node recorded
example and explicitly establish that O4's premise is false there. Later
counterexamples isolate misattribution, ignoring admissions, own-node
bootstrapping, and a consequence-free precedence change. No theorem in this
file licenses calling every ruled result a semilattice.

### Existing serving call and the missing inputs

The actual call chain at this base is:

```
cascade-decision → per-target cascade-lane (:R6)
                → cascade-policy/candidate-space → organise
```

`cascade_problems.clj:168–171` supplies all interpreted pattern IDs but
**`:stands-on #{}`**, with the explicitly constructed precedence vectors.
`candidate-space:330–354` supplies `first-attempt-cascade`, no admissions,
`:selected-only`, the candidate precedence, and model-derived acting-order /
**token coverage** score ports. It retains the organiser diff inside its lane
candidates. Joint selection instead reconstructs candidates from
`:constructed-candidates`; it discards that diff and scores with canonical G.
Do not describe the coverage port as that G. Do not silently replace it merely
to make O4 pass: a semantics-preserving reordering can legitimately be refused
by O4 when the declared observations do not change.

`receipt_construction.clj:557–611` is another existing, source-rich route:
it builds a repository from receipt edges, carries a previous cascade and
admissions, calls the organiser with an up-closure temperament and a declared
shadow score, and returns `:semilattice {:descent … :co_app []}`. It is not the
selected-cascade constructor used in this reference run. Reuse its provenance
contracts where appropriate rather than assuming its output is the missing
live record. The legacy Python `cascade_construct.py:168–181` similarly projects
phylogeny descent and weighted co-application edges; the name
`chosen_semi_lattice` alone is not a closure check.

For meaningful authority organisation on this serving path, supply:

1. Revision/digest-bound `@why` repository edges and their complete support
   closure, including authority patterns outside the selected interpretations.
2. A genuine previous cascade (or an explicitly warranted first attempt),
   separated from the current candidate's fixed precedence.
3. Attributed support admissions and a declared closure temperament; no invented
   causal→authority conversion and no inference that prose is an admission.
4. Acting-order/G ports whose carrier and observation domain include any newly
   acting patterns. Decide explicitly which closure nodes are warrant-only;
   adding them to the firing list changes the scored policy and needs readings.
5. Retention through assembly, joint selection, policy identity, construction
   and narrative, with source graph and O3 projection kept distinct.

Size: current nonempty candidates have 1 or 3 pattern nodes (P/Q has 2).
`candidate-space`'s default powerset has 2^n candidates including empty and
refuses n>8; the serving **explicit precedence** path does not enumerate that
powerset and has no equivalent n>8 check. Authority up-closure is bounded by
the repository, not selected-list length. The runtime repeatedly runs graph
reachability, including up to O(n²) pair queries; a conservative per-candidate
bound is O(n²(n+e)), with O(k n²(n+e)) for k candidates, apart from semantic-port
cost. Record actual node/edge counts and explicit budgets before enabling a
library-wide graph. Pairwise support intersections cost O(m² n) with naive
sets for m family members over n atoms; **constructing** all missing meets can
be exponential, up to 2^n distinct subsets, and is not the same as checking.

Would today's supply produce a non-chain? **Yes as a measured partial order,
no as evidence of an overlapping authority semilattice.** With today's empty
repository relation, when its semantic ports admit the call, the current organiser returns an edgeless antichain for
EoI's three selected nodes, although it retains a three-element priority list.
With recomputed need edges, the same candidate is a chain component plus an
independent node. The reference singletons cannot show within-candidate overlap.
A source-bound authority closure could introduce overlap even from one acting
pattern, but these records do not say what that closure is. The generic
organiser can preserve authored branching/diamonds if supplied; it cannot
manufacture warranted authority structure from these singleton interpretations.

## 5. Slice plan and acceptance

**Slice 1 — record only.** Introduce a pure, versioned structural receipt under
`:cascade-structure`, with declared basis, pattern carrier, witnessed need edges,
pattern→support family, the shape/axes/findings above, and typed unavailable
authority evidence. Replace the selected-constructor literal with that receipt
(or remove its misleading `:semilattice` slot); never insert a shape map into a
legacy edge slot consumed by old fold code. Retain it in the construction
checkpoint and narrative. Do not change scoring, selected precedence, admissions,
habit identity or folding. In particular `cascade_habit_store/policy-view`
currently includes any action `:semilattice` in policy identity: a record-only
receipt must not silently reset learned identities by occupying that key.

Acceptance, on the real selected constructor and a pure helper:

- The singleton fixture returns one explicit support set, `:shape :singleton`,
  zero derived edges and typed authority absence; its construction never returns
  the old literal `:semilattice []`. Assert the specific old field value is gone;
  empty `:wires []` remains legitimate.
- P→Q yields `{{P},{P,Q}}` and `:chain`; the overlapping diamond support family
  above yields `:semilattice` and its explicit overlap/meet witness.
- EoI C1/C2 produce the same two-component laminar support family without adding
  B→O. Priority vectors stay distinct and unchanged.
- An overlapping nonclosed family yields a typed missing-intersection finding,
  not `:semilattice`. Empty, duplicate-ID, cyclic and unsupported-guard inputs
  are explicit absences/refusals. Do not invent a bottom element.
- Artifact roundtrip retains all sets/IDs/digests. Decisions/G/habit keys and
  construction dispatch/fold outputs remain identical aside from the new
  structural evidence. The fixture test must fail on the present literal.

**Slice 2 — retain authored organisation.** Thread verified repository/admission
and predecessor receipts into the existing call; preserve the diff and full
source closure through selection. Record graph-overlap and set/meet predicates
separately. Test a pinned branching authority example, an omitted source, cycle,
wrong revision, O3's excluded added-node carrier, and a real before/after order
change with declared score/acting consequence. Preserve current runtime refusal
laws; do not make O4 vacuous to admit an otherwise unchanged order.

**Slice 3 — construct shared support when warranted.** Extend supply beyond
singleton/provided sequences using source-bound shared prerequisites/authority
patterns and checked support admissions. A fork with shared prerequisite can
already yield crossing support cones; derive/test their actual intersections.
If an intersection has no represented organisational unit, report that gap;
adding a synthetic set is not admitting a pattern. Explicitly budget search
and any meet completion; distinguish supporting nodes from acting nodes.

**Slice 4 — fold and evaluate that structure.** Declare an evidence-backed
mapping from supported sequential dependencies to BV.seq and actual
co-application units to BV.copar. A token need edge alone does not license
copar. Test shared-subconstruction reuse, warrant preservation, overlap/missing
meet negatives, and that both scorer and fold consume the same proposed
structure. Retain outcome comparisons; a graph diagram is not an observed
successful build. Any new policy identity/habit migration is a separate declared
change, not a side effect of adding an honest label.

## Reproduction and validation

The discovery census reads only the three canonical run records and executes
the pure P/Q constructor fixture in its own CLI JVM. It emits every candidate's
priority, need edges, output sets and classified family. No production test
namespace or source is modified; no fix regression is claimed for this discovery.
The counterexamples above are finite controls for the proposed label.

```
# /home/joe/code/futon2-narrative-9
clojure -M:test -m cognitect.test-runner -n futon2.report.cascade-shape-discovery-test
clj-kondo --lint test/futon2/report/cascade_shape_discovery_test.clj
emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults test/futon2/report/cascade_shape_discovery_test.clj
```

Environment: Linux, Java 21.0.11, Clojure CLI 1.12.5.1664 / Clojure 1.11.1.
The evidence directory retains the census output, checkpoint/trace projection,
input hashes and lint/paren results. No unchanged production suite was rerun.

Fresh discovery checks: **2 tests / 15 assertions, zero failures/errors**;
clj-kondo **zero warnings/errors**; check-parens **OK**. These validate the finite
classifier controls, the 51-row census, absence of cyclic support collapse, and
the actual P/Q constructor result. They do not validate a new serving path.

Registered discovery warrant:
`test-registry-c444e5b23357a91ce929efc3305719aae316fc87162e37a2e5647960a37a5599`,
on `78b7d1be31fc83b7dc23e87304f02b5987fc28a3`: `:warrant? true`, 2 tests,
15 assertions, zero errors/failures, exit 0. This warrants the discovery
computation and controls, not an implemented serving classifier or a universal
semilattice theorem. Historical data and external paper/Lean inputs are
identified by `input-sha256.txt`; recheck those hashes when reproducing the
historical claims.

Registration command (separate CLI process, from `/home/joe/code/futon3c`):

```
clojure -M -m futon3c.test-registry run /home/joe/code/futon2-narrative-9/holes/labs/wm-contract/runs/fixlist-2026-09-21/fix-9-evidence/registry.edn
```

The registry requires its `clojure -M:test -n <namespace>` command form, so the
original standalone computation was moved into the named discovery test
namespace; no registry guard was changed. Registration, content-addressed log,
and closure are retained in the evidence directory.
