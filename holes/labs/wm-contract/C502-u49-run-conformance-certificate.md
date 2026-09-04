# C502 — the run-conformance certificate: s5's route, proved conformant in Lean

**Item:** worklist `:U49`. **Date:** 2026-09-04. **Ruling executed:** Joe's RUN4
ruling of 2026-09-03 (`worklist.edn :run4-lean-ruling`).

## 1. What the ruling asked for, and what it refused

Joe, verbatim:

> I'm not sure I believe that `wmRunConformsToWiring` cannot be attested to…
> all that's really needed here is to run the machine and see if it conforms to
> the wiring that we drew. And that should be something we can validate in Lean.
> So I don't see this as a permanent hole at all… It might be the last one we
> fill. But it's not permanent.

Two things follow, and they are different things. **The refusal:** the
declaration's annotation opened with `PERMANENT EXTERNAL ATTESTATION · Lean
cannot prove an event`, and its owner has now said he does not believe that.
**The construction:** a Lean certificate over a pinned run — the reassembled
route against the drawn Figure 4 layers, proved by `decide` — is leg (3) of the
J9 criterion (`RUNBOOK.md`) applied per run, and is what he means by validating
it in Lean.

C114 had declined exactly this narrowing ("a theorem over pinned route and
wiring snapshots would prove one pinned comparison, not the existing world-level
conformance claim"). That decline is superseded by the ruling, and the docstring
now says so rather than dropping it.

## 2. What was built

`mathlib4 d4f05a0246` adds to `DarkTower/WarMachine/Holes.lean`, immediately
after the declaration it certifies:

* `RouteNode` — the control map's own 20 declared nodes as constructors,
  `TRACE` included;
* `figureDrawnEdges` (22), `figureMeasuredEdges` (8), `figureRetiredEdges` (8,
  each with the SET of grounds it was retired on);
* `classifyHop`, `routeHops`, `runConformsToDrawnWiring`;
* `s5Routes`, the four routes the run recorded, and `s5Hops`, their 36 hops;
* `wmS5RunConformsToDrawnWiring` and `wmS5RouteCensus`, both `by decide`.

The whole block is GENERATED from the two source files by
`holes/labs/wm-contract/u49_route_transcribe.bb`, not hand-typed, and the
producer is deterministic: no wall-clock field, so two runs over unchanged
sources write byte-identical artifacts.

`#print axioms` on both theorems: *does not depend on any axioms*. No `sorry`,
no `native_decide`, and the module's sorry count is unchanged at 10.

## 3. The comparison transcribed is RUN3's, and that is not a formality

`run3_conformance.bb` produced this run's pinned `:verdict :conformant`, so its
decision rule is the one the certificate has to reproduce. Its rule is *by
grounds*, not pass/fail (`run3_conformance.bb:28-37`): a `:code` retirement at
route grain is a REFUTATION; a `:code` retirement at dependency grain — a pair
that is also on `:route-measured-drawn` — is EXCLUDED; a `:ruling` retirement
traversed means the ruling is not realised in code, a build row.

**Transcribe the third clause literally and the certificate flips to
`not conformant`.** The row's own statement describes the
proof obligation as "every hop an edge, route non-empty, no retired edge
traversed", and a literal reading of that third clause makes s5 NOT conformant,
because s5 traverses two retired pairs — `R2 → R7` (`:code`, but also on the
measured layer, so excluded) and `R5 → R6` (`:ruling`). The certificate
transcribes the grounds rule and the docstring says why in as many words, so a
reader who arrives with the loose phrasing in hand is told where it diverges.

## 4. What the certificate does not show

Recorded here, in the theorem's docstring, in `certificate.edn :limits` and in
the accounting's `:basis`, because a reader counting "conformant" will otherwise
take it for more than it is.

**(a) Five of the nine distinct hops are drawn only because a run was
measured.** `R20→R12`, `R12→R2`, `R3→R8`, `R6→R14`, `R14→TRACE` are on the
`:route-measured-drawn` layer, which p4ng `0598d19` added *because a route
measurement found them*. For those five the run is being compared against a
record of a run. Only `R7→R3` and `R8→R5` are on the originally drawn layer.

**(b) Nineteen of the twenty-two drawn edges never fired.** The run exercises
three: `R7→R3`, `R8→R5`, and `R5→R6` (which is drawn and also ruling-retired).
`R2→R7` is not among them — it is on the measured layer only. Conformance here
means the run stayed inside the union of the two layers; it does not mean the
figure predicted the run, and a figure of which the conforming run
touches three edges in twenty-two is worth saying out loud.

**(c) It is one run of four ticks on one tree** (futon2 `5a66411`). Per-run is
the design — the ruling says the hole closes when Joe accepts a certificate over
a *qualifying run*, and which run qualifies is his call at certificate time.

## 5. One finding: the two checkers disagree on the drawn set

`run3_conformance.bb:55` builds its drawn set from **all** of `:edges`,
regardless of `:status`. `checks/wm_route_conformance.clj:28` filters to
`:status :drawn`. The map holds 22 edges of which one, the self-loop `R5 → R5`,
is `:unresolved` — so run3 counts 22 and `wm_route_conformance` counts 21, and
this is why the `:drawn` field of `runs/2026-09-01-s5/conformance.edn` reads 22
while the per-tick receipts' own `:route-verdict` reads `:drawn-edges-total 21`.

The transcription follows run3, because run3 produced the pinned verdict, and
the divergence is inert on this run: `R5 → R5` is not traversed, so no hop's
classification turns on it.

**Reported, not repaired.** Which of the two is right is a question about what
an `:unresolved` edge means in the figure, and that is a decision for
`control-map-edges.edn :decisions` or a ruling, not for a transcription row.
`04-controls.edn` control C5 carries the numbers.

## 6. What was deliberately not done

* **`wmRunConformsToWiring` is not closed.** It is still `sorry`, still
  `mkHole`, and its registry row's owner, evidence and falsifier fields are
  byte-identical. The ruling reserves `mkClosed` for Joe's acceptance of a
  certificate, and the s5 certificate demonstrates the machinery rather than
  discharging the claim. Contract counts unchanged: 124 declarations, 114
  closed, 10 holes.
* **No ruling was written.** Nothing was added to `aif-equations.edn :choices`
  or `control-map-edges.edn :decisions`, including the C5 finding.
* **No live run, no run lock, nothing written under `data/`.** The producer is
  read-only over both sources and does not re-run `run3_conformance.bb`, which
  would rewrite `conformance.edn` and `runs/latest-conformance.edn` with a fresh
  `:checked-at`.
* **`gen_aif_dag.bb` was not run into a publish** (TN §9a gate rule).
* **The p4ng-side figures were not regenerated.** No p4ng file changed on this
  row; the drawn map was read, not edited.

## 7. The commits

| repo | sha | what |
|---|---|---|
| mathlib4 | `d4f05a0246` | the transcription and both `decide` theorems |
| mathlib4 | `f5924bdd25` | the `wmRunConformsToWiring` close path — a follow-up, not an amendment, as the ruling directs |
| mathlib4 | `5167172a78` | `holes-contract.json` re-emitted at authority `f5924bdd25`; one field moves, the source git-sha |
| mathlib4 | `11c2e44aff` | five drifted line pointers in the transcription's docstrings, corrected at source and regenerated from the producer; literals and both theorems byte-identical |
| mathlib4 | `49e8528116` | `holes-contract.json` re-emitted at authority `11c2e44aff` |
| futon2 | this commit | producer, artifacts, certificate record, accounting, this note |

The pointer correction is a fifth commit rather than a rewrite of the first
three. The branch is 251 commits ahead of `origin/darktower` and the checkout is
shared, so nothing already committed there gets rebased out from under another
seat; the five wrong pointers are corrected forward and named.

## 8. Gates

`lake build DarkTower.WarMachine.Holes` 2704 jobs, 0 errors, at each of the
three mathlib4 commits; `#print axioms` on both theorems reports no axioms;
`generate_variable_situation_accounting.bb` WROTE and `--check`,
`--negative-empty`, `--negative-untyped`, `--negative-drift` all PASS, at 133
rows with 11 `:open-hole` and closability 4 `:pre-run-closable` / 7
`:run-gated` (declaration split 4/6) — unchanged, since the row moved readiness
and not closability; `negative_controls.sh` and `pointer_check.bb` as recorded
in the ledger; `clj-kondo` and `check-parens` on the producer; `worklist_check`
re-run after the ledger commit.
