# WM-13 mission-C composition declaration

**Decision requested from Joe:** choose whether mission-grain C is a region of
the one ruled tagged-sum preference, or a second C outside that sum. This
document makes the consequences decidable; it does not make the choice and
does not authorize an implementation.

## 1. Verified state and the ruled tagged sum

`fold-declaration` is a value, not a function. It declares four layers and
separately records whether each is folded and whether it belongs to the ruled
sum (`src/futon2/aif/ruled_outcome_c.clj:65-99`). The same declaration is an
admission input: changing it makes `construct-machine-preference` return
`:preference-layer-revision-mismatch`
(`src/futon2/aif/ruled_outcome_c.clj:121-128`).

Here, **the ruled tagged sum** means the single preference carrier whose
outcomes are tagged by vertex. Its declared vertices are `:organization`,
`:nouns`, `:verbs`, and `:evidence`; organization carries terminal disposition
outcomes, nouns and verbs are named-empty, and evidence remains owed
(`src/futon2/aif/ruled_outcome_c.clj:40-54`). Construction materializes that
shape as tagged outcomes such as `[:organization outcome]`, with one
distribution over the organization support and the other vertices retained in
the schema (`src/futon2/aif/ruled_outcome_c.clj:147-169`). Thus “sum” here is
first a disjoint/tagged outcome carrier, not permission to add arbitrary risk
numbers produced on unrelated domains.

The layers mean the following today:

| Layer | Declaration | Present contribution |
|---|---|---|
| `:ruled-outcome-c` | `:folded? true`, `:in-ruled-sum :yes` (`ruled_outcome_c.clj:69-77`) | The twelve-wide terminal-disposition seed is the organization region: support and masses are declared at `ruled_outcome_c.clj:56-63`. Its numeric risk can be added only in `compute-efe`, behind `:ruled-outcome-c-enabled?`; the disposition prediction and weighted contribution are computed at `src/futon2/aif/efe.clj:707-713` and enter `G-risk` at `efe.clj:723-727`. |
| `:c-int` | `:folded? true`, `:in-ruled-sum :no` (`ruled_outcome_c.clj:78-84`) | It is an independent channel preference family: healthy channel ranges are declared at `src/futon2/aif/preferences.clj:9-24`, converted to per-channel risk at `src/futon2/aif/efe.clj:690-706`, and then added as `channel-risk` at `efe.clj:723-727`. “No” therefore means separately scored, not absent. |
| `:c-ser` | `:folded? false`, `:in-ruled-sum :yes` (`ruled_outcome_c.clj:85-91`) | It names a region of the one sum but contributes no numeric term today because it is not folded. Its site is explicitly a named-empty organization-support region (`ruled_outcome_c.clj:85-91`). |
| `:c-mis` | `:folded? false`, `:in-ruled-sum :undeclared` (`ruled_outcome_c.clj:92-99`) | The declared source is `mission_c.clj`'s factored criterion density, constructed over measurable mission criteria at `src/futon2/aif/mission_c.clj:422-477`. Separately, the now-live `live_c.clj` producer projects aliveness/closure mission tokens through each mission's declared wants (`src/futon2/aif/live_c.clj:266-301,303-363`). These are two mission-grain producers and must not be collapsed in provenance merely because they share a grain. |

The live cascade scorer does not execute the `compute-efe` additive fold.
`rank-actions` dispatches an all-cascade family directly to
`rank-cascade-actions` (`src/futon2/aif/efe.clj:1329-1353`), which invokes
`horizon-g-sparse-cert` with the supplied spec (`efe.clj:1137-1152`). The war
machine now supplies the projected live mission spec at
`scripts/futon2/report/war_machine.clj:6159-6185,6208-6228`. That verifies the
premise that a mission-grain preference now reaches scoring, while its
`:in-ruled-sum` status remains undecided.

## 2. The two answers

| Consequence | Mission-grain C is a **region of the tagged sum** | Mission-grain C is a **second C outside the tagged sum** |
|---|---|---|
| Scored quantity | There is one C over a disjoint/tagged carrier. Mission outcomes must be admitted as a named region of that carrier and Q must predict on the same carrier. The score is one risk/KL against that composed C; the mission term is not independently added a second time. The existing terminal design illustrates this shape as one C family, with token outcomes before T and a token/disposition product at T (`/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/PROPOSAL-AUTH-C-bridge.md:22-36,119-121`). | Mission C remains an independent preference factor/term on the target-qualified cascade-token domain. Its log preference or risk is composed outside the terminal tagged distribution under an explicit combination law and exchange rate. The current cascade scorer already evaluates the merged token spec pointwise: weights determine token utility (`src/futon2/aif/cascade_model_manifest.clj:486-504`), and `log-preference-fn` produces the normalized log C (`cascade_model_manifest.clj:506-554`). |
| Certificate | One-C provenance must name the tagged carrier, every included region, its producer and revision, the Q/C domain match, normalization, and a per-region decomposition whose sum equals the one reported risk. Both `mission_c/c-mis` and `live-c/cascade-spec` must be named if both survive; grain equality is not source equality (`mission_c.clj:422-477`; `live_c.clj:303-363`). | The certificate must state that mission C is outside the ruled sum, name the composition law and coefficient, and report ruled-sum risk and mission-C risk separately before the total. The current certificate only echoes the cascade spec's `:c` record and weights (`src/futon2/aif/efe.clj:1196-1217`); it does not certify a two-C composition or a relationship to `fold-declaration`. |
| `:ruled-outcome-c` | Remains the organization/terminal region of the same C. Mission preference must compose without duplicating its mass or risk (`ruled_outcome_c.clj:56-77`). | Remains the ruled tagged C, separate from mission C. A later consumer may score both only under the newly declared outside-composition law; the existing `compute-efe` addition is not reached by cascade ranking (`efe.clj:1329-1353`). |
| `:c-int` | Stays outside the ruled sum because its declaration is `:no`; admitting mission C does not pull channel C into the tagged carrier (`ruled_outcome_c.clj:78-84`). | Stays a separately scored C family, as today. The ruling must distinguish its existing channel-risk addition from the newly authorized mission-C combination (`efe.clj:690-727`). |
| `:c-ser` | Remains a named region of the same sum, still numerically absent until separately authorized to fold (`ruled_outcome_c.clj:85-91`). | Remains a region of the ruled sum, not automatically a third outside C. Choosing “second C” for mission grain says nothing that activates serendipity. |
| Mission producers | Both mission-grain producers need an explicit relation: replacement, subregions, or a further partition inside the mission region. Neither may be silently counted twice (`mission_c.clj:422-477`; `live_c.clj:266-363`). | The ruling must say whether `mission_c/c-mis` and live C are one outside family with two sources or two outside terms. Without that sub-choice, “second C” can accidentally become a third C. |

The choice is therefore not merely a label change. The region answer changes
the carrier and demands one normalized C/Q domain. The second-C answer retains
separate carriers but owes an explicit numerical composition law and a
certificate that exposes both operands.

## 3. What is true today

**The wire in commits `a1111054`/`3567598c` presumes the second-C-outside
reading.** It projects mission tokens onto target-qualified cascade outcomes
(`src/futon2/aif/live_c.clj:275-301`), merges those weights into the cascade
preference spec (`scripts/futon2/report/war_machine.clj:6185,6208-6228`), and
sends that spec directly to `horizon-g-sparse-cert`
(`src/futon2/aif/efe.clj:1137-1152`). It does not extend
`ruled-outcome-c`'s tagged vertices, construct its tagged distribution, or
execute its `compute-efe` fold (`ruled_outcome_c.clj:40-54,147-169`;
`efe.clj:707-727`).

Therefore:

- If Joe chooses **second C outside the tagged sum**, today's live scoring is
  already on the chosen side of the boundary. It still owes the explicit
  outside-composition declaration and fuller certificate described above, but
  the fact that mission C scores independently is not itself a defect.
- If Joe chooses **region of the tagged sum**, today's behavior is a defect
  that has been scoring since `a1111054`: mission preference is being scored
  through an independent cascade-token C instead of as a region of the one
  ruled tagged distribution. The accepted wire established influence and
  domain projection; it did not establish tagged-sum membership.

This is a direct answer, not a claim that both readings fit the current code:
the current wire chose the second-C architecture in fact, before authority had
chosen it in declaration.

## 4. Processual/terminal clause

The processual/terminal clause is **not reachable on the live cascade decision
under the accepted AUTH-C-bridge resolution**. The code dispatches cascade
candidates to `rank-cascade-actions` before the ordinary branch that can call
`compute-efe` (`src/futon2/aif/efe.clj:1329-1367`). The cascade scorer receives
`:spec`, not a step-indexed producer, from its production call
(`efe.clj:1137-1152`); `horizon-g-sparse` documents that `:spec` is a declared
constant C at every tau, while only `:c-fn-pointwise` can vary C by tau
(`src/futon2/aif/cascade_model_manifest.clj:825-854`). The live call supplies
`:spec` and no `:c-fn-pointwise` (`efe.clj:1137-1152`).

AUTH-C-bridge explicitly records the architectural reason: the cascade branch
never reaches `compute-efe`, the only consumer of ruled-outcome C, so terminal
disposition C is deliberately not read by the live decision
(`/home/joe/code/p4ng/wm-walkthroughs/build-loop/closure/closure-dag.json:3522-3546`).
The later verified node note repeats that restoring the ruled-outcome fold is
not authorized and would not by itself establish the required state-dependent
bridge (`closure-dag.json:309,333-342`).

Accordingly, satisfying “processual C connected to terminal C at the mission's
declared grain” requires either a new authorized cascade-path terminal
construction or reversal/replacement of the AUTH-C-bridge resolution. This
packet authorizes neither. Quietly routing cascade ranking through
`compute-efe`, or turning its optional disposition fold on, would reverse the
standing resolution rather than discharge this node.

## 5. Decision text offered

Joe can choose exactly one of these statements:

> **REGION:** Mission-grain C is a named region of the one ruled tagged-sum C.
> Its mission producers must be reconciled as sources/subregions of that one
> region; Q and C must share the resulting tagged carrier; scoring reports one
> normalized C-risk with a region decomposition. Independent mission-C scoring
> on the cascade-token carrier is not authorized as the final composition.

> **SECOND C:** Mission-grain C is outside the ruled tagged-sum C. It is scored
> on the target-qualified cascade-token domain and combined with other C
> families only by a separately declared numerical law. Certificates must
> expose the ruled-sum and mission-C operands, their producers, coefficients,
> domains, and total; sharing a mission grain does not identify
> `mission_c/c-mis` with `live-c/cascade-spec`.

Neither choice, by itself, reverses AUTH-C-bridge or authorizes the
processual/terminal path.

## 6. Verification boundary

This is a decision artifact only. No `.clj`, `.edn`, or `.lean` source was
changed. No executable acceptance scope has been pinned, so no test was run
and no warrant was required or manufactured.
