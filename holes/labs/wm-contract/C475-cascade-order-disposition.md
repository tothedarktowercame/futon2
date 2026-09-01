# C475 — I4 slice (b): the disposition of `:r6-r14-order`

**Row:** `worklist.edn :I4` (`:class :I`, owner claude-20, `:loop-mode
:one-slice-per-invocation`). Slice (a) was the discovery
(`C474-cascade-order-discovery.md`, futon2 `1fe0704`). **This is slice (b), the
disposition.** No code changed, no run made, no registry entry written, no ruling
written. What it settles it settles from sources and code with pointers; what it
does not settle it names.

C474 §6 listed five things (b) owed. Each is answered below, and the answer to
the first changes what the row's own acceptance can ask for.

---

## 1. Which reading of R6 the ruling retires — settled from the source

C474 §6.1 left this as "a question for Joe if it cannot be settled from
`:r6-r14-order`'s own text". It can be settled, not from the registry entry —
whose `:statement` is one sentence — but from **the record of the exchange the
entry quotes**: `futon2/holes/problems/P-validated-R5.md` §3d′ (`:412-441`),
"The R6/R14 boundary drawn (Joe, 2026-08-30): target first, then the cascade to
match". Joe's sentence is quoted there at `:414-415`; the surrounding paragraphs
are the typing worked out in that exchange, and they name the reading:

- `:431` — "(i) the candidate space **R6-C** is a space of **targets**, not of
  cascades";
- `:435-437` — "(iii) the figure's `R5 →rank→ R6 → R13 → R14` is therefore
  **mis-ordered for the catalogue's R6**: the derived control path is
  `R6-C → R5 → R14 (target) → R13 (cascade) → R16`".

That path is character-for-character the registry's `:derived-control-path [:R6
:R5 :R14 :R13 :R16]` (`control-map-edges.edn:261`). So **the R6 in
`:r6-r14-order` is R6-C, the catalogue R6** — `control-stages.edn:15`,
"Candidate action space" — and the retirement of `[:R5 :R6]` is a claim about
the candidate-space node, not about `policy/select-action`.

`:438-439` settles the R11 leg of the same entry in the same way: "with R11's
arbitration feeding R14's choice of target rather than R16 directly — codex-22's
reading, now the decision". The entry's `:retires [[:R11 :R16]]` and `:adds
[[:R11 :R14] [:R14 :R13] [:R13 :R16]]` are codex-22's derived route adopted, not
new claims; that route is written out in the registry's own
`:drawn-not-derivable` reason at `control-map-edges.edn:142`.

### 1a. Under that reading the retired half is already true in code

The candidate space is built at `war_machine.clj:4933-4942`
(`ap/compose-proposers`) and enriched at `:4954-4960`, and is `rank-actions`'
second argument at `:5002`. R6-C precedes R5 in program order today. The registry
already records it: `:derived-undrawn` `control-map-edges.edn:68`, `{:from :R6
:to :R5 … "scoring needs the candidate set; reverses the drawn R5→R6 for the
catalogue R6"}`, found independently by both derivations (`:by [:claude-15
:codex-22]`).

## 2. RUN3's `:ruling-unrealised` on `R5->R6` does not measure the retired edge

The tick emits exactly five route tags — `war_machine.clj:4685` (`:R2`), `:4918`
(`:R8`), `:5003` (`:R5`), `:5113` (`:R6`), `:5130` (`:R14`) — and the `:R6` tag
names `futon2.aif.policy/select-action`. **No route tag names the candidate
space.** So a route can only ever carry the R5 → Q(π)-R6 hop, and that is a
different relation from the drawn `R5→R6` the ruling retires.

This is the two-relations distinction RUN3 states in its own header
(`run3_conformance.bb:28-42`) and applies to `[:R2 :R7]`. It was not applied
here, and its grain proxy could not have applied it: `classify`
(`run3_conformance.bb:117-124`) tests `(measured hop)` only on `:code`
retirements, and `[:R5 :R6]` is not in `:route-measured-drawn`
(`control-map-edges.edn:94-133`) in any case. The proxy is keyed on that layer's
membership; the R6-C / R6-Q(π) split lives in `:derived-undrawn`, which RUN3 does
not read (`topology`, `run3_conformance.bb:52-60`, reads `:edges`,
`:route-measured-drawn` and `:decisions`).

Nothing in RUN3's run is wrong as measured. What is wrong is the inference from
"the ruling-retired pair was traversed" to "the ruling is unrealised", when the
tag and the drawing name different relations.

## 3. What of the ruling IS unrealised, and it has no route signature

The second clause — "then construct the cascade to match" — is unrealised, as
C474 §1 established: `cascade-lane` is called at `war_machine.clj:5047-5051` over
`wm-ranked` (R5's sort), and `policy/select-action` is not reached until
`:5101-5110`; entry #1 is `(first ranked-actions)` (`cascade_lane.clj:397`).

One sharpening of C474 §1, from the docstring rather than the call graph:
`*gate-decision-target?*` (`cascade_lane.clj:381-389`) says it prepends "the
judge's rank-1 **DECISION** target … so the gate evaluates what the machine
actually decided". The code it documents takes rank-1 of the **ranking**. The
docstring and the code disagree about which of the tick's three targets (C474 §3)
this is; the 2026-07-06 operator ruling it quotes was implemented against the
first, and the machine commits to the third (`war_machine.clj:5155-5157`).

**Measured, extended from C474 §4's 24 records to 48.** Across
`runs/2026-09-01-s1b` (20), `-s2` (20), `-s4` (4) and `-s5` (4), every record
carries **zero** `:apply-cascade` entries in `:ranked-actions`. (`-s3` has no
trace file; `runs/2026-09-01` is the routeless first S1.) So the corpus cannot
show this clause realised or unrealised either way — not because the lane failed,
but because `:include-advisory-lanes?` is `false` at `run_tick_once.clj:211`.

## 4. The three retirements, revisited

The acceptance asks for this on the branch where the ruling is not realised. Each
pair carries a ground that does not depend on I4's outcome:

| pair | independent ground | stands? |
|---|---|---|
| `[:R5 :R6]` | `:derived-undrawn` `control-map-edges.edn:68`, both derivations, on the catalogue-R6 reading §1 names | yes — and §1a says it is code-true |
| `[:R6 :R13]` | `:drawn-not-derivable` `:141` (codex-22) **and** `:decisions :r6-r13-supplied-independently` `:234-239`, a code ground | yes, on two grounds |
| `[:R11 :R16]` | `:drawn-not-derivable` `:142` (codex-22) | yes, on a derivation ground; no code ground — see §4a |

**Two pointer corrections to slice (a), which are also in `:I4`'s `:evidence`.**
C474 §6.4 named the second `[:R6 :R13]` ground `:r6-r13-depth-precedes-selection`
at `control-map-edges.edn:235-239`. **No such key exists**; the key is
`:r6-r13-supplied-independently` and it begins at `:234`. And C474 §6.4 said
"only `[:R11 :R16]` has no second ground" — it has one, at `:142`. What it lacks
is a *code* ground, which is a different statement.

### 4a. R11 has two code carriers, and the retirement was reasoned against one

Looking for that code ground turned up a split of the same shape as R6's, and it
cuts both ways, so it is reported rather than resolved:

- **Carrier 1, the arbiter.** `futon2.aif.hierarchical-budget`
  (`hierarchical_budget.clj:2`, "R11 hierarchical shared-budget arbitration") has
  **no non-test caller**. Its two entry points are
  `policy/select-budgeted-actions` (`policy.clj:22-30`), called only from
  `hierarchical_budget_test.clj:85`, and `hierarchical_budget_adapter.clj:68`,
  called only from its own test. Under this carrier neither the retired
  `[:R11 :R16]` nor the ruling's replacement `[:R11 :R14]` is realisable: R11
  feeds nothing on any tick. The ruling swaps one unrealised R11 edge for
  another — which is not a defect in the ruling, but it bounds what "realising"
  the R11 leg of I4 could mean.
- **Carrier 2, the semilattice fold.** `close_loop.clj:54-58`'s
  `*semilattice-fold?*` docstring says "Bind false to **disarm R11**", and that
  fold *is* on the actuation path: `close_loop.clj:76-107` produces the act
  gate's `:coverage-score-delta`, `enact.clj:175` builds the gates from it, and
  `close-loop!` enacts the first `:pass` gate (`enact.clj:307`). That is an
  R11→R16 relation, not an R11→R14 one. It fires only for a cascade-lane entry
  with a non-empty `:semilattice`, so by §3's measurement it is unexercised in
  every recorded run.

Which carrier the drawn `R11→R16` names is not settled here.

## 5. Reconciling the two rulings (C474 §6.3)

They are not two rulings about different things. The 2026-07-06 operator ruling
("accept the decision so that the machine can act on its decision",
`cascade_lane.clj:381-389`) and Joe's 2026-08-30 one have the same content, and
the earlier one is implemented at the wrong step (§3). The ledger fact: **the
2026-07-06 ruling appears nowhere in `control-map-edges.edn :decisions` or
`aif-equations.edn :choices`** — it exists only in a `def`'s docstring, armed by
a bell (`cascade_lane.clj:387-388`). An operator ruling that is realised in code
and absent from the registry is invisible to every check that reads the
registry, including RUN3.

## 6. Disposition

**Neither branch of `:I4`'s acceptance is reachable as written, because the
acceptance's test is at the wrong grain.**

- The code branch — "the code changes and a run shows `R5->R6` no longer
  traversed" — cannot be met by any change that realises this ruling. §2: the
  traversed hop is R5 → `select-action`, and the ruling does not ask for that to
  stop (C474 §2 showed it would be a new selection law; §1 shows the source text
  asks for no such thing).
- The not-to-be-realised branch is not available either, because §1a says half
  the ruling is *already* realised and §3 says the other half is a build nobody
  has attempted.

**It is not a J row.** By the rule in `worklist_check.bb:16-44` (Joe,
2026-09-01), a choice the theory does not settle gets its branches built and run;
only a choice whose arms cannot be run reaches Joe as a question. The residual
here has runnable arms.

**The residual build, stated so it can be routed as its own row:**

1. Move `cascade-policies`/`cascade-actions` (`war_machine.clj:5047-5065`) below
   `wm-decision` (`:5155`) and construct for the committed target.
2. Settle the β/F_pi placement first. `f-pi-dark-readback` and `beta-dark-carry`
   (`war_machine.clj:5079-5086`) run on `wm-ranked+cascades`, and the comment at
   `:5069-5078` says they were put before selection deliberately. Under the two
   default selection-gain modes nothing below reads them
   (`war_machine.clj:643-664`), so the cycle C474 §5 warns of is prospective —
   but `FUTON_WM_TAU_MODE=variational-beta-gamma` is a live arm (RUN8/S3) and
   under it β *is* the selection temperature (`war_machine.clj:5109-5110`).
3. A demonstrating run needs `:include-advisory-lanes?` true
   (`run_tick_once.clj:211`), which starts the Python constructor
   (`cascade_lane.clj:20-22`, 30 s ceiling at `:32-35`).
4. **The acceptance test is not the route hop.** It is that the constructed
   cascade's target equals the persisted `:decision` target
   (`war_machine.clj:5283`). C474 §4 measured that this fails 24/24 today by
   construction, so the check has a known-red baseline to move.

**What is left for Joe, and it is one thing:** whether the drawn `R5→R6` in
Figure 5B is the catalogue R6-C (in which case §1a means the drawing changes and
no code does) or the Q(π) R6 (in which case §2's grain objection does not apply
and the retirement is about `select-action`). §1 answers it from the source
record of his own exchange; a reader who holds that the figure's R6 is the Q(π)
node should reject §1 rather than amend it, exactly as `control-map-edges.edn:207`
frames the same fork for `[:R6 :R4]`.

## 7. What this slice did not do

No code changed. No run made. Nothing written to `:decisions` or `:choices` —
including the §2 correction to RUN3's classification and the §5 registry gap,
both of which are findings for a reviewer to route, not entries this slice may
write. `gen_aif_dag.bb` and `gen_live_topology.bb` were not run (TN §9a gate
rule). No `src/` touched, so clj-kondo, `check-parens` and the test suite were
not acceptance-required; no run, so the RUN12 run lock was not taken.
`negative_controls.sh` and `pointer_check.bb` run after this document and the
ledger row are written.
