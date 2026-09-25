# H-VALUE-T-D — a value term at target grain

Hole H-value of `PROOF-2a-THEOREM-draft-2026-09-24.md` (Holes row L522;
owners row L616). Author: claude-13, 2026-09-25. Discovery only: no code
edits, no `data/` writes, no shared-JVM loads. One script, run in its own
process: `h_value_t_d.clj` beside this file, output committed as
`h-value-t-d-output.txt`.

Anchors, read this session:

| file | sha (last commit touching it) |
|---|---|
| PROOF-2a draft (Clause T L86-163; finding L408-442; rulings L574-594) | futon2 a4fa7572 |
| PROOF-2 register row AR-40 (`PROOF-2-THEOREM-draft-2026-09-24.md` L332) | f16dc1ad |
| PROOF-2-ASSUME A9 (L78) | 637fbb32 |
| `proof2/packets/H-VALUE-CAL-D.md` §3 (L59-117), §8 (L293-) | 247d3fcd |
| `proof2/packets/H-C-DEF.md` §2 (L60-115) | 2ada0a9b |
| `scripts/wm/extract-outcomes.clj` (sha256 `6f0e99be…`) | a31f9cee |
| `src/futon2/aif/target_field.clj` | 7bd17dfb |
| fixture `test/fixtures/target-field/target-field@futon2-7bd17dfb.edn` (sha256 `760d5b31…`) | 6d2b39a7 |
| `scripts/futon2/report/war_machine.clj` L6132-6136 (`construction-move-cost`) | 3aa8c479 |

## 0. Step 0: the owed non-author warrant

`AUTHOR=claude-13 scripts/wm/register-warrant.sh --pinned ce094860
futon2.aif.observation-checks-test` → **test-registry-3c6906d21d6274d1e5a0ab0349f187a449f939ed91058fca1b8ab6b8f61ac7be**
(`:kind :run`, `:warrant? true`, git-head ce094860, 28 tests / 131
assertions, 0 failures, `:postcheck :matched`).

## 1. The inputs, by definition

What a per-target score input is, from the texts:
- Pₜ (L126-132) asks for "the per-target score inputs" beside `:chosen`, and
  `:chosen` must maximise `selectionPosterior` over `T_f` (Wₜ, L102-104).
- A9 (ASSUME L78): C is "the prospective preference distribution over
  outcomes for each candidate universe … consumed through risk in G".
- The PROOF-2a finding (L422-426): C is computed at click time from the text,
  each outcome cued to its span. A target is never refused for lacking a
  precomputed C.

So a score input is a property of the target read from its text at HEAD,
with a cued receipt, that enters `selectionPosterior` through G (risk against
C) or through E. Feasibility is support, "not … a term of G" (L104-105).

| candidate input | status today | definition it rests on |
|---|---|---|
| the target's own outcomes C (extractor → `filter-outcomes`) | **(a)** readable at HEAD, all 343 in 3 s; 56 have ≥1 admitted outcome (§2) | H-C-DEF §2; PROOF-2a L408-426 |
| wants (`flight/click-wants`, the criteria reader) | **(a)** for 12 targets (`:ask-interpretation`, wants named); **(b)** for 331 (`:read-criteria`, finding `:criteria-not-stated`, fixture) | Wₜ `:next-step` (L96-101); ruling L582-594 |
| served-by links, wants → C | **(c)** outside one mission: the rule is defined (H-C-DEF §4, E6), but its vocabulary (`coupling-artefacts`, 5 entries, extract-outcomes L292-320) and its instance sections (`### n.` headings, L217-238) are M-futon-seams's; 0 links over the field (§2) | H-C-DEF §4 |
| `:next-step` | **(a)** for all 343, and **not a score input by definition**: "a target whose next step is longer is not thereby infeasible" (L105-106), and feasibility is support, not G | Wₜ |
| E[attempts] from θ | **(b)**: needs a constructed candidate. 0 of 343 are `:ready`. θ is declared (0.8, 5 pseudo-trials, futon3c click-001-enactment.edn L77) | clause 5; H-VALUE-CAL-D §3 `:completion` |
| declared cost | **(c)** at target grain: not a target-field key; a flight input (amended paragraph, L151-163) | Clause T, cost paragraph |
| G per target | **(c)**: "Target-grain scoring needs a G per target … stated here, not settled" (L142-145) | Clause T missing definition |
| habit E per target | **(b)/(c)**: E is over cascade policies (cascade_prior.clj `policy-key`, mission slot). A target marginal is undefined until the target has constructed candidates. H-E has one enactment record | H-E-D §1-2 (69bfd1ab) |

## 2. The field, measured

`bb holes/labs/wm-contract/proof2/packets/h_value_t_d.clj`: every feasible
target of the fixture (343: 59 missions, 30 tickets, 254 excursions), text
read at the repo head the fixture recorded (`git show <head>:<path>`),
through the extractor's own steps in `-main`'s order.

The harness was checked against the extractor first. M-futon-seams at futon3c
7fc18ef0 gives 6 outcomes, 9 rejected, 1 facet, 8 links, 5 linked outcomes.
`extract-outcomes.clj` `-main` on the same file with `--cascades
…/M-futon-seams/proto` gives the same counts: 6/9/1, 8 served links, 1
unlinked.

| partition | targets |
|---|---|
| ≥1 admitted outcome | **56** (missions 18/59, tickets 3/30, excursions 35/254) |
| 0: `{:absent :no-stated-outcome}` as the script emits it | **287** |
| refused (`:cue-does-not-resolve` or threw) | **0** |
| text not at the recorded head | 0 |

| admitted outcomes per target | 0 | 1 | 2 | 3 | 6 | 13 |
|---|---|---|---|---|---|---|
| targets | 287 | 45 | 6 | 3 | 1 | 1 |

85 outcomes over the field. The two largest are M-apm-demonstration (13)
and M-self-documenting-stack (6).

| served-by links per target | 0 |
|---|---|
| targets | **343** |

- Instance sections: 23 targets have ≥1. Only 6 of those also have an
  admitted outcome.
- Cue rules over the 85: `:so-party-could` 47, `:otherwise` 19,
  `:wants-a` 11, 8 others ≤4.
- Attribution: `:the-mission` 52, "Joe" 30, "claude-1" 2, unattributed 1.
  The party list is the extractor's data (L154): Rob, Joe, claude-1,
  claude-10, kimi-4.
- M-autoclock-in, the example target named in the draft (L556), has 2 cued
  sentences, both rejected: `{:absent :no-stated-outcome}`.

**What it shows.** A link-count value has nothing to rank. Served-by is 0 on
every feasible target: the artefact vocabulary it links through was built
from, and only matches, one mission, which is not in the field (COMPLETE).
Outcome counts are 0 on 84% of the field and 1 on most of the rest. A count
of outcomes is also not a value: H-C-DEF clause 2 requires a named party,
and the vocabulary is five names from one mission. The extractor reads C at
target grain, but on this field it reads C sparsely and with a vocabulary
tuned to one mission. The measurement bounds what any value computed today
could say. It does not show that a target lacks outcomes.

## 3. The form

Instance grain (§3, §8): V_i(w) = Σ_o w_o·a_i(o) − λ·E[attempts_i], with
a_i(o) summing d·f_o(T_i) over served-by links from i's wants to o.

Target grain, as data:

```edn
{:form "V_T(w) = sum_{o in C_T} w_o * a_T(o) - lambda * E[attempts_T]"
 :outcomes {:of :target :source :extract-outcomes/filter-outcomes :cued true}
 :a_T {:definition "sum over served-by links from T's wants to o, degree d, flow f_o(T_T)"
       :not "1 for every own outcome: H-C-DEF §2 clause 4, an outcome is not discharged by completing"
       :today {:status :absent :reason :no-served-by-links :links 0 :of 343}}
 :E-attempts {:status :absent :reason :no-constructed-candidate :ready 0 :of 343}
 :lambda {:status :unstated}
 :weights {:status :unstated}
 :deliverable :undominated-set}
```

Undominated set, by §8's definition over the union of the field's outcomes,
with weight on a vertex e_o:
- **Outcome spaces are disjoint.** Each target's C is its own. V_x − V_T at
  e_o, for o ∈ C_T, is −a_T(o) − λ(E_x − E_T).
- **At λ = 0** (E[attempts] absent), T is undominated iff a_T(o) > 0 for
  some o, or every candidate ties it.
- **Today** a_T ≡ 0 on all 343. Every V_T is identically 0, the whole field
  ties, and **the undominated set is T_f**.
- **With attainment defined** (links > 0), every target with one attained
  outcome would be undominated. The set would then be about the 56 targets
  with outcomes, and could not shrink below "targets whose text states an
  outcome". Disjoint outcome spaces give dominance no bite.

**Tie-break.** `:chosen` maximises `selectionPosterior` over T_f (L102-104).
By definition, what breaks a tie among undominated targets is that
posterior's other terms: G and the habit E. A9 goes further: C is
"consumed through risk in G", so at target grain the value term is not a
separate score but C entering G's risk. Both terms are undefined at target
grain:
- G: Clause T's missing definition, L142-145.
- E per target: §1, habit E row.

The cost ordering cannot break the tie: it is not a target-field key
(L151-163). **Missing definition:** the target-grain G, and in it which
outcome universe C ranges over when targets are compared. There are two
options: the union of each target's own C (disjoint, so dominance has no
bite, as above), or outcomes shared across targets (a stack-level C). No
document states a stack-level C. Clause T's "Missing definition" paragraph
(L142) is where the definition would go, with A9 naming the universe. No
weight is proposed.

## 4. AR-40 residual: does a target-grain value need a move cost?

No, by the definitions.

- `construction-move-cost` (war_machine.clj L6132-6136) prices a move
  *inside the constructor*. Its own reason is "a construction move is
  computation inside the tick, not in G's units". It is a property of how a
  candidate is built, so its residual (AR-40 L332: move cost 0,
  `:ruling :none-found`) stays with the constructor.
- The cost term at target grain is λ·E[attempts_T]: the expected attempts of
  the target's flight. That is a property of the flight's best constructed
  cascade (clause 4's unit, L299-305).
- A mission's declared cost ordering is an input to its flight's precedence
  (L151-163).

So the cost at target grain belongs to the flight in both forms: attempts,
and precedence. The target field only records it, once construction has
produced it. Two things are missing:
- λ, unstated (swept in H-VALUE-CAL-D §3);
- E[attempts_T] for any target not yet constructed, which is all 343
  (`:status :absent :reason :no-constructed-candidate`).

## 5. Falsifiers for the implementation packet

| id | bad case | must observe |
|---|---|---|
| a | remove one served-by link from a target's C | its score input changes. Not constructible on today's field (0 links); build it on M-futon-seams (8 links), the only record with links |
| b | a target with 0 admitted outcomes (287 today; M-autoclock-in is one) | `{:absent :no-stated-outcome}` on the record, never a 0 score; its V is not compared as 0 |
| c | a planted copy of a target | ties it everywhere; neither dominates (§8's planted check) |
| d | weights unstated | no single number recorded; only the undominated set and the inputs, each with its cues |
| e | the record's `:chosen` | is in the undominated set, or the record says why not |
| f | a field where every a_T ≡ 0 (today's) | the undominated set is all of T_f, and the record says so, not an arbitrary member |
| g | add one entry to `coupling-artefacts` that matches a field target's instance section | that target's links change. The score input depends on predeclared vocabulary, so the record carries the vocabulary's sha |
| h | the extractor over M-futon-seams through the implementation | 6 outcomes / 8 links, as `-main` gives (control, §2) |
| i | two targets whose outcomes are disjoint and each attained | both undominated; the record does not claim a strict pick without G or E |

## Answers, one line each

1. Score input = a cued property of the target read at HEAD, entering
   `selectionPosterior` through G's risk on C or through E. Readable today:
   own outcomes (all 343), wants (12). After a flight step: wants (331),
   E[attempts], E. Undefined: served-by outside M-futon-seams, per-target G,
   declared cost at target grain. `:next-step` is support, not a score.
2. 56 targets with outcomes, 287 typed-absent, 0 refused; 85 outcomes; 0
   served-by links on all 343. A link-count value has nothing to rank.
3. V_T with a_T from links; today a_T ≡ 0, so the undominated set is T_f.
   Ties go to G and E by `selectionPosterior`, and both are undefined at
   target grain. Missing: the target-grain G and C's outcome universe
   (Clause T L142, A9).
4. No move cost. Target cost is λ·E[attempts] of the flight's cascade plus
   its precedence, both flight-side. λ unstated, attempts absent.
5. (a)-(e) as asked, plus (f) the all-tie field, (g) vocabulary dependence,
   (h) the M-futon-seams control, (i) disjoint outcomes give no strict pick.
