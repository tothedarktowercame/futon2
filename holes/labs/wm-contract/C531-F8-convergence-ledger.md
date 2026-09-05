# C531 — F8 convergence ledger adoption

Date: 2026-09-05

`CONVERGENCE.edn` adopts the draft's eighteen-row population but re-reads every
identity and formal line from `aif-equations.edn:74-200`.  Both legs now use the
eight-rung F6 order, and every rung has a pointer whose evidence reaches that
rung.  In particular, `scripts/generate_variable_situation_accounting.bb:417-428`
requires a machine record with a run identity for `:witnessed` and higher.

## Adjudication

The nine stale `:named` spec legs now resolve to reviewed Lean constructions:
observe, prediction error, precision, policy F, belief update, belief state,
depth, temperature, and action.  The action correction is resolved directly
against `mathlib4/DarkTower/WarMachine/MachineAction.lean:49-70`, because
`runs/U35-lean-state/lean-state-report.edn` predates the reviewed action module;
the registry records the later authority at `aif-equations.edn:181-186`.

Seventeen draft implementation legs said `:witnessed` using only PROBLEMS and
source ranges.  The F8 readbacks and F1/F3 receipts have no run identity, so
they license formulas or types, not witness rungs.  The exception is policy
set: `runs/F7-cascade-policy/f7-cascade-policy-decision.edn:140-151` carries run
`c149f9de-669c-4817-9b0e-ed4aad77db79` and two distinct constructed-and-scored
cascades.  It licenses `:constructed`, while its own record still says the
production lane constructs one cascade per target.

**Review finding, repaired in place (reviewing seat, 2026-09-05).** That one
licence was written as `:140-151` and the row claimed `:constructed` while
citing the F6 rule as its authority. The F6 predicate refuses this artifact:
`machine-record?` reads only the top level of the file or a top-level sequence
of maps (`scripts/generate_variable_situation_accounting.bb:450-462`), and the
receipt's `:run-id` is one level down, at `:basis :run-id`, line 146. So the
ledger's single rung above `:formula-transcribed` rested on a rule that, applied
as written, rejects it. The licence now names line 146, the row's `:basis` says
where the run identity sits and that the predicate does not reach it, and a new
top-level `:rung-rule-limit` field records the limit once for the whole ledger.
The rung is unchanged: the artifact does carry a run identity and does exhibit
two constructed, scored policies. What changed is that the claim is now
auditable against the file instead of against a predicate that answers false.

No row is converged.  The R5 pilot remains a green exact/refusing node
certificate, but `runs/F3-node-sim/00-r5-pilot.edn` has no run identity and
explicitly disclaims shipped wiring.  Every other row names its trailing leg
and the artifact that would license convergence.

## Caveats retained and checked

Box-5/U35 staleness remains for the late action and Dirichlet work
(`aif-equations.edn:181-192`).  The R5 certificate remains a node simulation,
not a shipped call-path certificate (`runs/F3-node-sim/00-r5-pilot.edn
:what-this-is/:not-done`).  The R17 divergence remains: A4a recounts
capability-by-mission substrate events rather than the declared tick-model
outer product (`aif-equations.edn:187-195`).  The draft additionally missed
that scalar F is retired with no producer (`aif-equations.edn:92-101`) and
policy F remains flag-gated (`aif-equations.edn:102-109`).

## Slice-2 checker specification

A checker must refuse a foreign schema, a row-set mismatch with the equation
registry, any non-verbatim identity/formal field, a rung outside or out of
order in the eight-rung scale, a rung without a resolvable licence, a
`:witnessed`-or-higher licence lacking both run identity and quantity-specific
evidence (and it must decide, rather than inherit, whether a run identity nested
below the top level counts -- the F6 predicate says no, and this ledger carries
exactly one such licence), a wrong leading-leg comparison, a certificate lacking either an
accepted green run or an explicit trailing-leg reason, and `:converged? true`
without both.  It must also pin the three still-live caveats rather than
silently accepting their removal.

## Not done

No certificate was minted, no checker was written, and no registry, source,
decision, or draft file was edited.  No live tick or run lock was taken,
nothing was written under `data/`, and `gen_aif_dag.bb` was not run.  The first
review should check the single `:constructed` licence and the five rows reduced
to `:type-transcribed`, because those boundaries determine whether the ledger
is measuring evidence or merely renaming source presence.
