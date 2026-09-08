# ZP1 — stakeholder census from the Main Problem

**As of:** 2026-09-08. **Scope:** discovery only. This census does not define
personas or a personas × operations matrix.

## Derivation and boundary

The in-force apex asks whether **“a community of humans and machines doing
open-ended work”** can leave a self-steering account
(`futon3c/holes/excursions/apex-thesis.json`, `as-of` 2026-09-06). Cluster C
supplies the inclusion rule and the quote required for every row below:

> “The loop's human and stakeholder edges are where learning silently dies.”

Its members make that rule concrete: “3 of 7 lifecycle stages are operator
turns (Box-12/PA11z)”, “agent discoveries do not teach humans”, “trainers'
equity family (UKRN)”, and “understanding did not update the argument
(leaf-cycle)”. W2 adds that project-domain problems (equity, ai4ci, ukrns) join
through C as instances (`holes/labs/library-loop/runs/W2-pushout-sketch.md:31-35,52-55`).

I therefore include people or machine actors who supply, receive, judge, fund,
or must learn from work. I exclude **design and code**: “apparatus builder”,
“developer”, a component, repository, model, document, and pattern are not
stakeholders. I also exclude role-card names (solver, scribe, proctor, guide,
and so on): those are harness-internal seats which an agent may occupy, not
participants served by the harness. Machine agents are included as a
stakeholder class because the apex explicitly says humans **and machines**;
their current role cards are merely implementations of that class.

The R pointers use the control map at
`holes/labs/wm-contract/SPEC-zaif-harness-v1.md:17-30`: R2 observation, R3
belief update, R5 preference-conditioned valuation, R6 choice, R7 source
precision, R9 independent witness, R11 shared-budget composition, R14
claimed-versus-verified precision, R16 action, R17 learning. A pointer means
the node touches the participant's feedback edge; it does not claim that the
edge is implemented.

## Census

Every row is grounded by the Cluster-C quote above; the `C member` column says
which concrete member forces the split.

| Stakeholder | C member / why distinct | R nodes that touch the edge | WR pointers | apparatus pointers | Already in the record |
|---|---|---|---|---|---|
| **Joe, operator and goal author** | “3 of 7 lifecycle stages are operator turns” | R2, R3, R5, R6, R7, R16, R17 | WR-14 (operator divergence becomes signal), WR-16 (operator half as observation channel), WR-17 (Joe-facing surfaces), WR-25 (operator reports alter ordering only until warranted) | `loudness-is-conserved`, `the-system-stops-on-schedule`, `done-is-observed-running` | The PZ lab is the one existing stakeholder model: `holes/labs/M-zaif-harness/pz1-final-truth.edn` records Joe's gold judgments; `holes/M-zaif-harness.md:450-487` reports PZ1's verdict. |
| **Machine agents / crew members** | apex “humans and machines”; “agent discoveries do not teach humans” | R1, R2, R3, R6, R9, R11, R14, R16, R17 | WR-0 (operator follow-up is not outcome evidence), WR-1 (agent dispatch/forum coordination), WR-3 (social exotype), WR-7 (interoperating frames) | `default-to-the-cheap-error` (do not charge roles for apparatus faults), `success-must-not-resemble-failure`, `repairs-name-defects-not-neighborhoods` (human or agent adjudicator) | Box-12 PA11z's real library-annotator dispatch, recorded in `worklist.edn:411-424`; role cards under `futon3c/holes/labs/M-apm-demonstration/role-cards/` are seats used by members of this class, not additional stakeholders. |
| **Human collaborators, readers, and reviewers who must receive machine discoveries** | “agent discoveries do not teach humans” | R2, R3, R7, R9, R14, R17 | WR-9 (claims made inspectable without an operator query), WR-10 (live next-move surface), WR-12 (typed essay/annotation events), WR-24 (peer review as external verdict) | `loudness-is-conserved`, `done-is-observed-running`, `one-authority-per-question` | The cluster member is the present record of absence. `futon5a/holes/stories/leaf-cycle.md` is also the named human-reading case, but its narrower “understanding did not update the argument” edge is separated below. |
| **Argument authors and downstream users of an argument** | “understanding did not update the argument (leaf-cycle)” | R2, R3, R7, R9, R17 | WR-8 (typed argument files are canonical), WR-12 (essay health consumes typed events), WR-22 (logic model as independent VERIFY) | `one-authority-per-question`, `model-upstream-and-coupled`, `pin-moves-with-the-population` | `futon5a/holes/stories/leaf-cycle.md` and `.aif.edn` are the cited case: an understanding change and its failure to reach the argument. This is narrower than the prior row because it has a particular shared artifact that must change. |
| **Trainers / local adapters** | “trainers' equity family (UKRN)” | R2, R3, R5, R7, R14, R16, R17 | WR-4 (inhabit before building), WR-12 (institution-object observations), WR-27 (returned demand changes the loop) | `loudness-is-conserved`, `monitors-measure-the-work`, `pin-moves-with-the-population` | `futon3/library/equity/visible-impact.flexiarg:10-18,30-37` says feedback must propagate to trainers and names the evaluation chain; `shared-maintenance-as-published.flexiarg:30-55` records adaptations that cannot flow upstream. |
| **Training recipients / programme participants** | trainers' equity family, at the downstream outcome end from which feedback must return | R2, R3, R5, R7, R9, R16 | WR-6 (external response tests the hypothesis), WR-12 (typed institution/annotation events), WR-27 (consumption-to-acquisition gain) | `monitors-measure-the-work`, `success-must-not-resemble-failure`, `replayable-not-precious` | `futon3/library/equity/confident-adaptation-merged.flexiarg:16-24` explicitly names participants and trainers; `visible-impact-merged.flexiarg:19-36` names recipients as the peer-evaluation source. |
| **Institutional leads and programme governors** | trainers' equity family: the feedback receiver able to change programme architecture | R2, R3, R5, R6, R7, R9, R11, R16, R17 | WR-11 (external application as predecessor exemplar), WR-12 (institution-object channel), WR-21 (shared cross-mission dependency), WR-25 (institutions check financial claims) | `evidence-to-disposition-once`, `one-authority-per-question`, `done-is-observed-running` | `futon3/library/equity/visible-impact.flexiarg:10-12,30-37` requires feedback to institutional leads and the scaffold; `visible-impact-merged.flexiarg:19` names them as actors needing downstream-impact evidence. |
| **Funders, commissioners, and external evaluators** | trainers' equity family: outward accountability must return as usable feedback rather than stop at the funder's desk | R2, R5, R6, R7, R9, R11, R14, R17 | WR-6 (external response), WR-23 (maintainer response as genuine external evaluation), WR-24 (compiler/maintainer/peer-review/money verdicts), WR-25 (financial facts need artifacts) | `default-to-the-cheap-error`, `evidence-to-disposition-once`, `repairs-name-defects-not-neighborhoods` | `futon3/library/equity/visible-impact.flexiarg:10-12,30-37` explicitly says feedback goes to trainers/leads, “not only to funders”; `visible-impact-merged.flexiarg:19` names funders among those needing evidence. |
| **External maintainers, clients, employers, and institutions affected by outward work** | W2 says project domains are C instances; these are the non-project actors whose response closes an outward edge | R2, R3, R7, R9, R14, R16, R17 | WR-11 (external applications), WR-23 (upstream maintainers' questions and replies), WR-24 (maintainer/peer verdict), WR-25 (clients, employers, institutions checking figures) | `loudness-is-conserved`, `evidence-to-disposition-once`, `done-is-observed-running` | WR-23's record includes the first maintainer reply; WR-25 names clients, employers, and institutions. No PZ-style participant model exists for this class. |

These nine classes are deliberately roles in a feedback relation, not
demographic personas. ZP2 may later sketch characteristic programs for them;
this row does not.

## Enumeration receipts

All commands below were run from `/home/joe/code`. None was piped through
`head`, a pager, or a tool output limit when counted; their outputs were **not
truncated**.

1. Apex and Cluster-C vocabulary:
   `jq -r '.apex, (.clusters[] | select(.key == "C") | .ground, .members[])' futon3c/holes/excursions/apex-thesis.json`
   — 6 lines, complete.
2. War-room population:
   `find futon3/library/war-room -maxdepth 1 -type f -name 'wr-*.flexiarg' -printf '%f\n' | sort`
   — 28 files, exactly WR-0 through WR-27, complete. Content search:
   `rg -n -i 'human|stakeholder|participant|operator|trainer|community|reader|reviewer|user|agent|collaborator|client|beneficiar|public|institution|funder|maintainer' futon3/library/war-room/wr-*.flexiarg`
   — exit 0, complete.
3. Apparatus population:
   `find futon3/library/apparatus -maxdepth 1 -type f -name '*.flexiarg' -printf '%f\n' | sort`
   — 14 files, complete. The same `rg` expression over
   `futon3/library/apparatus/*.flexiarg` exited 0, complete.
4. Existing stakeholder model:
   `rg -n 'PZ1|PZ2|PZ3' futon2/holes/M-zaif-harness.md futon2/holes/labs/M-zaif-harness`
   — exit 0, complete. It found operator-turn calibration only; no other
   participant has a PZ-labelled model.
5. Role-card boundary:
   `find futon3c/holes/labs/M-apm-demonstration/role-cards -maxdepth 1 -type f -printf '%f\n' | sort`
   — 31 files, complete. These names were inspected only to enforce their
   exclusion, not converted into census rows.

The census is exhaustive **for participant kinds named or forced by the
in-force apex, its Cluster-C members, and W2's project-instance clause under
the stated design/code exclusion**. It is not a claim that the repository
contains no other audience labels.
