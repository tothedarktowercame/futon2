# U87 Lean `Vertex` ripple census

Date: 2026-09-09.  Basis: worklist row `:U87` and
`RULINGS-walkthrough-2026-09-09.md` Item 18b.  This is a pointer-only census;
no consumer was changed.  `MUST-UPDATE` includes prose and records that would
become false after the constructor change; an append-only record may need a
dated correction rather than an in-place rewrite.

The sweep was `Vertex\.|\bVertex\b|organisations|tetrahedr` over all of
`mathlib4/DarkTower`, `futon2/holes/labs/wm-contract`, and `p4ng`, followed by
constructor-specific searches and inspection of every hit.  Ordinary uses of
the English words “people”, “money”, and “organisation(s)” that do not denote
this four-way index are not consumers.

## Lean declarations and witnesses

| Classification | Pointer | Consumer and effect |
|---|---|---|
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/Holes.lean:142-147` | The declaration itself names all three replaced constructors. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/Holes.lean:150` | `Outcome (Obs : Vertex → Type*) := Sigma Obs` is constructor-agnostic. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/Holes.lean:153` | `C` is generic except for `v ≠ Vertex.evidence`; `evidence` exists in both models, so the premise survives unchanged. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/Holes.lean:6958,6966,6979,6987,6994,7067,7087,7095,7117,7126` | Kernel, model, risk, free-energy, and information-gain declarations quantify over `Vertex` generically. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/Holes.lean:7182-7183` | Private counterexample family is generic and its only attachment is `.evidence`. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:30-43` | `PeopleObservation` and `MoneyObservation` and their specialization explanations become false names under the canonical constructors. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:49-58` | `Obs` pattern-matches all four old constructors and `organisationOutcome` attaches dispositions to `.organisations`. This requires a semantic mapping, not merely plural spelling. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/F10RuledCarrier.lean:87-102` | `seed.mass` explicitly pattern-matches `.organisations`; all twelve arms must move to the ruled canonical vertex. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/FoldCWitness.lean:8-11` | `ruledBase` explicitly attaches grounded change to `.organisations`. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/PreferenceDistributionPragmaticCostNegative.lean:6,13` | The negative example explicitly instantiates `C .people` and prints `Vertex.people`; its expected diagnostic must be retaken. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/MachineQ.lean:64` | Generic `Obs : Vertex → Type*`. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/PredictiveOutcomeKernelWitness.lean:9-11`; `ParameterPosteriorKernelWitness.lean:11-13`; `PredictiveOutcomeRiskWitness.lean:9-11`; `ParameterPriorKernelOutcomeNegative.lean:7-8`; `PreferenceDistributionWitness.lean:8-10`; `ObservationVectorOutcomeNegative.lean:6-7` | Each family is generic; every concrete outcome shown is attached only to `.evidence`. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/ExpectedInformationGainWitness.lean:8-9,17`; `ExpectedFreeEnergyWitness.lean:7-8,13`; `GenerativeModelWitness.lean:8-10,17-18`; `GenerativeModelNegative.lean:8-9,15` | Indexed inductives are generic and use only `.evidence`. |
| SURVIVES | `mathlib4:DarkTower/WarMachine/MachineQWitness.lean:78-81` | Generic family, concrete outcome at `.evidence`. |
| MUST-UPDATE | `mathlib4:DarkTower/WarMachine/PreferenceLadderDraft.lean:20` | The comment says there are competing `Vertex` readings; after Item 18b there is a canonical model plus a specialization, not an unresolved choice. |

`DarkTower/FindDraft.lean` and `DarkTower/WarMachine/Run4Preregistration.lean`
contain no `Vertex` or constructor occurrence.  The full `DarkTower` sweep also
found no additional constructor consumer.  `Emit.lean:46` is the unrelated
field `decl.evidence`; `F12DischargeArm.lean:120,130` uses “Vertex” in a graph
theorem name, not the WarMachine inductive.

## Futon2 declarations, registries, and records

| Classification | Pointer | Consumer and effect |
|---|---|---|
| MUST-UPDATE | `futon2:src/futon2/aif/ruled_outcome_c.clj:29-38,76` | The seeded-C attachment map has keys `:organisations`, `:people`, and `:money`, plus old specialization explanations. These are the runtime names corresponding to the Lean carrier. |
| SURVIVES | `futon2:checks/disposition_kernel.clj:1-166` | Inspected in full: it consumes disposition probabilities but contains no vertex name or attachment key. |
| MUST-UPDATE | `futon2:holes/labs/wm-contract/aif-equations.edn:214-218` | The ruled `:outcome-domain` record repeatedly assigns carriers and regions to `organisations`, nouns, verbs, and evidence. It needs a dated reconciliation explaining which canonical vertex receives the specialization. |
| MUST-UPDATE | `futon2:holes/labs/wm-contract/worklist.edn:1392-1393` | F10 history records the old `Obs organisations` carrier and constructor-specific Lean implementation. Preserve history, but the live reading needs a dated correction/certificate reference. |
| MUST-UPDATE | `futon2:holes/labs/wm-contract/worklist.edn:1576-1577` | U87 names both models and is the controlling row; completion must record the selected mapping. |
| SURVIVES | `futon2:holes/labs/wm-contract/FUNDAMENTALS.edn:215` | Emitted `C` signature uses only generic `Vertex` and `Vertex.evidence`; verified to survive. |
| MUST-UPDATE | `futon2:holes/labs/wm-contract/C42-carrier-family.md:6-7`; `C538-F10-outcome-domain-decision-sheet.md:162-163,265,313`; `C574-F10-disposition-enumeration.md:6,19,216`; `C576-F10-lean-carrier.md:14-18,83`; `C577-F10-runtime-fold.md:5-11`; `C580-F10-rider-gap.md:8` | These contract notes state the old specialization as the actual carrier vocabulary. Historical conclusions need an explicit canonical-name mapping; do not silently rewrite evidence. |
| SURVIVES | `futon2:holes/labs/wm-contract/C538-F10-outcome-domain-decision-sheet.md:31,201,207`; `DESIGN-c-vector.md:120` | Generic `Vertex`/`Obs` discussion and the `evidence` exclusion remain true. |
| MUST-UPDATE | `futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-07.md:23-32,63,136-140,182`; `RULINGS-walkthrough-2026-09-08.md:263-265,285,315-340` | Ruling history names old vertices. Keep it as history, but link a dated Item-18b reconciliation so it is not read as the current constructor set. |
| SURVIVES | `futon2:holes/labs/wm-contract/RULINGS-walkthrough-2026-09-09.md:31-40` | Item 18b already states the canonical constructors and identifies the old names as specialization. |
| REVIEW | `futon2:holes/labs/wm-contract/runs/F10-outcome-domain/03-live-run.edn:110` | Immutable run evidence says `named-empty organisations-support region`. The recorded site label must not be rewritten, but downstream display may need a canonical alias. |
| SURVIVES | `futon2:holes/labs/wm-contract/runs/F8-belief-state/lean-state-join-after.edn:1496`; `F8-belief-state/lean-state-join-before.edn:1492`; `F8-belief-update/lean-state-join-check.edn:1404`; `F8-dirichlet-accumulation/lean-state-join-after.edn:1715`; `F8-dirichlet-accumulation/lean-state-join-before.edn:1684`; `F8-policy-free-energy/lean-state-join-check.edn:1431`; `F8-policy-free-energy/lean-state-probe.edn:1431`; `F8-prediction-error/lean-state-join-check.edn:1372`; `F8-depth/lean-state-join-before.edn:1496`; `F8-depth/lean-state-join-after.edn:1614`; `F8-action/lean-state-join-after.edn:1684`; `F8-action/lean-state-join-before.edn:1649`; `F8-temperature/lean-state-join-after.edn:1649`; `F8-temperature/lean-state-join-before.edn:1614`; `F8-observe/lean-state-join-check.edn:1464`; `F8-precision/lean-state-join-check.edn:1340`; `U35-lean-state/lean-state-report.edn:2159` | Recorded Lean-state snapshots contain only the generic `C` signature and `.evidence` exclusion. They remain truthful historical bytes and need no retake for this rename. |

There is no `control-map-edges.edn` under the wm-contract directory; the live
file is `p4ng/empirics-futon/control-map-edges.edn`.  It contains no `Vertex`
constructor or old specialization token.  This resolves the packet's seeded
control-map check without inventing a missing consumer.

## Paper and figure consumers

| Classification | Pointer | Consumer and effect |
|---|---|---|
| SURVIVES | `p4ng:sec-c-vector.tex:28-36` | Already states `nouns, verbs, organization, evidence`, explicitly relegating people--money--organisations to the specialization; this is the canonical pin. |
| SURVIVES | `p4ng:empirics-futon/gen_war_room_tetrahedron.bb:4,104`; `gen_lane_campaign_table.bb:15`; `gen_workflow_report.bb:2,51` | Figure/report generators use the canonical lane set `wm-nouns`, `wm-verbs`, `wm-organization`, `wm-evidence`. |
| SURVIVES | `p4ng:war-room-tetrahedron.svg:16` | Generated figure visibly labels `evidence`, `nouns`, `verbs`, and `organisation`; spelling differs from Lean's ruled `organization`, but the four concepts are canonical and no old people/money carrier appears. |
| SURVIVES | `p4ng:sec-case-study-vetting.tex:83-98,242` | Figure inclusion and explanatory text describe the canonical tetrahedron/lane names. |
| REVIEW | `p4ng:war-room-tetrahedron.svg:16` | Before changing the Lean constructor, decide whether British display `organisation` intentionally remains distinct from the ruled constructor `organization`; this does not currently falsify the model, but exact-name tooling could care. |

Other `tetrahedr` hits in the three trees describe the figure, its generator,
publication sequencing, or unrelated historical models; they neither import
the Lean inductive nor attach an `Obs`/`C` value to a constructor.  Likewise,
`sec-evaluation-outline.tex:11` and `sec-related-futon.tex:125` use
“organisations” as an ordinary plural noun.

## Closing accounting and certificate impact

Counts are per table row (a row may group repeated occurrences of one consumer
or one immutable snapshot family):

| Classification | Count |
|---|---:|
| MUST-UPDATE | 13 |
| SURVIVES | 17 |
| REVIEW | 2 |
| **Total** | **32** |

**CLOSED-BY-RECORD declarations that themselves mention a constructor:**
none.  The two nearby CLOSED-BY-RECORD declarations are `Outcome` at
`Holes.lean:149-150` (generic and survives) and the preceding `DecisionRule` at
`Holes.lean:139-140` (does not mention `Vertex`).  `C` at `Holes.lean:152-153`
mentions `Vertex.evidence`, but its current record class is **DEFERRAL UNDER
ORGANIZED DISCOVERY**, not CLOSED-BY-RECORD, and its premise survives.

Therefore the constructor edit does not, by itself, require retaking a
CLOSED-BY-RECORD Lean certificate.  It **does** require re-elaboration of the
constructor-dependent F10 declarations/witnesses and retaking the expected
diagnostic in `PreferenceDistributionPragmaticCostNegative.lean`; those are
the concrete proof/test effects listed above.
