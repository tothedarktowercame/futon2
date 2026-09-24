# E-cascade-real — why the War Machine's critical step is passive

Date: 2026-09-24
Parent: the PROOF-2 plan (`labs/wm-contract/PROOF-2-STRATEGY-draft-2026-09-24.md`,
`PROOF-2-THEOREM-draft-2026-09-24.md`). An excursion, not a mission: the proof
plan already exists and there is no time to open a new mission.
Owner: claude-10. Driver: Joe.
Status: OPEN — defects D1–D18 written down; I1 and I2 done.
Cross-refs: `labs/wm-contract/proof2/packets/CLICK2-D.md` (why click 2
abstained; Part 2 per-target check); register row AR-16.

## Joe's framing (dictated 2026-09-24, voice transcript)

> "There are two effectively passive constructions in this description ...
> the war machine can't choose its own work ... it's allowed to do work on
> something in a queue whereas before, we had an outer loop whereby the war
> machine would choose its own work. And one of the considerations that it
> would choose that work based on is feasibility."

> "Cascades are meant to be constructed in the current regime ... having a
> cascade doesn't match how that's supposed to work. Maybe you couldn't
> construct the viable cascade. But because this is called active inference,
> not passive inference, we need to figure out why it has these two passive
> formations in this critical step."

And on the first draft of the findings: they read "like a list of defects
rather than the list of features" — so they are recorded here as defects.

## What prompted it

Click 2 (wm-click-2b77ec0d, 2026-09-24) abstained. CLICK2-D Part 2 ran the
judge's own admission check offline: all four admitted targets declined
`:no-new-wanted-token`. Clicks 3–10 are held. The machine did not fail; it
ran out of pre-written cascades.

## Defects

Each defect names where it lives and the commit that introduced it. Commits
in this tree are authored as Joseph Corneli with an agent co-author line;
"no ruling found" means none has been located yet, not that none exists (I1
looks).

**D1. Proposing was deleted along with flat ranking.** `5d55e7a0`
(2026-09-17, "H5b ... flat decision path deleted (Joe 2026-09-17)").
Before it, the judge called `action-proposer/compose-proposers` over the
six proposers (bootstrap, pattern, mission, ticket, sorry, tension; the
portfolio proposer existed but was not composed), ranked the proposals with
`efe/rank-actions`, and selected. The proposer namespaces still exist; the
judge no longer calls them (only a comment at
`scripts/futon2/report/war_machine.clj:7029` mentions them).
I1 (`0b06df90`) found the ruling text H5b cites, in
`p4ng/wm-walkthroughs/build-loop/closure/FOCUS.md` (Priority 0): "rip out
the flat decision path"; "make it impossible to run the machine with the
flat decision"; "a cascade is a policy, and G is computed over policies".
Every one is about the decision. None mentions proposing. Deleting the
proposers' call site was the implementer's reading. The same file's work
item 2 points the other way: the enacted decision should come "over cascade
candidates the tick builds itself". The planned step for that, H7f (feed
the constructor's families into the cascade sources), is recorded "still
open" on 2026-09-17 and was never committed.

**D2. Feasibility is a gate after the fact, not a consideration in choosing
work.** The target list is broad — every substrate mission, plus declared
targets, plus the proposal supply, plus the ticket queue
(`war_machine.clj:7072–7080`) — but a target with no pre-written candidate is
refused. The machine cannot prefer feasible work; it can only drop
infeasible work from what it was handed.

**D3. Cascade construction is placed outside the tick by a docstring.**
`src/futon2/aif/cascade_proposals.clj` (`65e523ef`, 2026-09-21): "Never infer
applicability or token production from signature prose. An agent authors a
complete cascade-source-v1 declaration ... proposals never populate its
executable :candidates ... Retrieval is explicit, outside the tick." The
first sentence guards against a fake constructor (claiming a pattern
produces a token because its prose says so). The rest removes construction
from the machine altogether. I1: the first part (an agent authors the
full declaration; never infer from prose) rests on two claude-12 Agency
rulings of 2026-09-21 named in `runs/proposal-supply-b1-2026-09-21/STATUS.md`
(full texts not retained by the Agency API). For "Retrieval is explicit,
outside the tick" no ruling was located (p4ng, futon2 holes, the commit's
parents searched). It also contradicts FOCUS.md work item 2 above.

**D4. The in-machine constructor exists and is not wired.**
`src/futon2/aif/interpretation_construction.clj` (`4328238d`, 2026-09-21,
"Construct bounded candidate families from given interpretations") and
`construction_moves.clj` (`3b01790d`, 2026-09-17, the four
construction-library moves). Their only callers are each other, tests, and
the load-identity digest list. No click has run them on a real target.
I2 (`56a58026`) ran it offline on the four admitted targets. Its output has
exactly the shape admission accepts (non-empty `:precedence` plus a
`:machine-constructed` receipt), and its refusals are typed:
`:want-already-observed` on the two finished targets, `:no-supported-order`
with finding `{:kind :unproduced-need :token :hole/h2045faa0e7cc}` on M-f11.
Rerun by claude-10: same four results.

**D5. The supply is five hand-written files, and it ran dry.**
`resources/wm/cascade-sources/*.edn`, written 2026-09-22/23. By 2026-09-24
two of the four admitted targets were already complete — one by the 09-23
click's own commit (T-repair-occ-444fb018), one by a reported rehearsal
(M-wm-08-external-f2). The machine's own success exhausts its work supply.
`data/wm-cascade-proposals/` holds one proposal.

**D6. The declarations do not aim at the remaining false wants.**
M-f11-find-production-successor: the false want `:hole/h2045faa0e7cc` is
produced by no declared pattern. M-aif-policy-conditioned-eig: the false want
`:hole/h42fceb4ad48b` is produced by the declared `:aif/two-layer-calibration`,
but the candidate using it was withdrawn in `8f97757b` (2026-09-22) because
the task needs held-out evidence that does not exist. A constructor would
have to find the route, or report what evidence would open it.
I2: the constructor does both. It reports M-f11's missing producer as a
typed finding, and on M-aif-eig it builds `[:aif/two-layer-calibration]`,
which the judge's own admission check (copied verbatim; claude-10 diffed it)
passes. So the machine can find the route that was withdrawn by hand.

**D7. Running out of work produces a hold, not a proposal to learn.** The old
bootstrap proposer turned "no concrete actions for this class" into a
`:learn-action-class` recommendation (docstring cites Joe 2026-05-17). The
cascade path records an `:environmental-hold` finding and abstains. Nothing
proposes the work that would make the next cascade possible.

**D8. The run record does not say why it abstained.** The per-candidate
decline reasons are in the cohort's selection event and the scan markdown,
not in the tick run record (AR-16, as corrected after walkthrough 05).

**D9. A withdrawn route's restore condition was met and nothing noticed.**
The EIG source's withdrawal comment (claude-3, 2026-09-22) reads "Restore
when that evidence has a locator." Held-out calibration evidence was
published at `resources/wm/eig/held-out-calibration.edn` on 2026-09-23
(`f0adf39a`), and the 09-23 click's ticket commit `97e17e10` already cites
it. The route is still withdrawn. The condition lives in a comment, so no
code can observe it being met. Not yet checked: whether that file is the
preregistered split and post-split outcomes the reviewer asked for. I2 §3
says there is no locator for the held-out evidence today; it did not find
this file, so that sentence is wrong as written. Whether the constructor's
EIG route is now correct therefore depends on that open check.

**D10. The click can write the token it is selected to produce.**
T-repair-occ-444fb018's only want is observed through its ticket's Status
line; the 09-23 click's own build commit `97e17e10` changed it from OPEN to
DONE (register AR-31, from walkthrough 06). The commit cites a recheck and
evidence, but the observation reads the Status line, not the evidence.

**D11. 245 of 248 substrate targets have no declared interpretations.** I2
§4. The constructor needs interpretations with receipts to search over;
only three substrate targets have any (plus two declared targets outside
the substrate list). Wiring the constructor in would not by itself give the
machine work: interpretations are the actual supply. Agents writing them is
not the defect (Joe, 2026-09-24: "I trust agents to make good
interpretations given the chance"). The defect is that nothing gives them
the chance: when the machine runs out, no step asks an agent for the
interpretations a target lacks. Asked claude-1 about its work interpreting
Joe's speech acts in pattern terms (job invoke-1790257263157-23671-d7e9076d).

claude-1's answer (2026-09-24; files checked by claude-10). The loop lives in
futon3c: `emacs/session-turn-analysis.el` (records each operator turn and
bells a delegate seat, `session-mode-analysis-agent`, now kimi-1),
`scripts/session_turn_analysis.py` (validator: pattern ids must resolve to a
real flexiarg, cue spans exact, malformed records refused with a typed
reason), `scripts/xlate.py` (BM25 over the pattern library),
`scripts/turn_batch.py` (`55ece9ac`, blocks of historical turns split across
seats). Corpora: `~/.emacs-graph/session-turn-analysis/`,
`~/code/storage/operator-turns/`. Per fragment it records intent, target,
rationale, relations, cue spans, pattern refs with `source_sha256`, and
pattern rejections with reasons. Its counts, as claude-1 reported them (not
re-derived): 145 live turns interpreted, 113 candidate proposals, 79
rejections; on one block kimi-4 cited patterns on 1% of fragments and kimi-5
on 33%.

Against the interpretation shape: the pattern and the receipt are there;
the guard (`:needs`/`:forbids`) and `:produces` are not, because the
records are about what Joe said, not about a target's state. claude-1 names
a token vocabulary as the missing piece. For missions one exists already:
`src/futon2/aif/mission_hole_wants.clj` projects each mission's unchecked
`- [ ]` tasks into `:hole/h…` want tokens with a checkbox observation (99
of 441 retained holes are observable this way). Those are tokens a seat can
write `:produces` and `:needs` against. What transfers from claude-1's loop:
the block-dispatch harness, refuse-with-typed-reason validation, and
rejections (close to `:forbids` in prose).

**Probe P1 result (2026-09-24).** Two Kimi seats, same brief, same frozen
inputs (`96fae955`: the 10 live missions with projected hole tokens and no
declared source). Seat A kimi-6 `9e8892b8`, seat B kimi-7 `29ce17b1`. Scored
by claude-10 with `probe-interp/score_probe.clj` (loader, then constructor,
then the judge's admission check; same G, budget and horizon 4 as I2).

| | files | loaded | constructed | admitted | refused |
|---|---|---|---|---|---|
| seat A | 9 (skipped M-daily-scan) | 9 | 5 | 5 | 4 `:no-supported-order`, each an `:unproduced-need` on one task the seat declined |
| seat B | 10 | 10 | 4 | 4 | 6 `:no-supported-order`: 4 `:want-unreachable-within-horizon` (6-pattern chains at horizon 4), 2 `:unproduced-need` |

Before the probe none of these 10 missions had anything the machine could
act on; after it, 5 (A) and 4 (B) are constructible and admissible, 7
distinct missions across the two seats. Hand spot-check, two per seat
(futonzero-generative and federated-agency-hardening for A; futonzero-generative
and daily-scan for B): every pattern file exists, its `@flexiarg` line matches,
the receipt sha matches, and the produced token is the task the reading
quotes. The one `:needs` edge seen was not checked against the mission's
order.

Seat agreement: 12 pattern ids shared across the 10 missions (of 34 in A and
43 in B); on 9 of those 12 the seats name the same produced tokens. Both
seats independently used `:aif/two-layer-calibration` for the same
futonzero-generative task. Agreement is partial: different seats find
different but defensible patterns, which is more like two readers than two
runs of one procedure.

**D15. The constructor is all-or-nothing over wants.** `search-plans`
(`interpretation_construction.clj`) searches backward from ALL wants, so one
want with no producer refuses the whole target. Admission needs only one
newly satisfied want. Seat A's four refusals each covered 4–5 of 6 wants
and are refused because the seat honestly declined one task. The brief said
declining is better than a bad interpretation; the constructor penalises
exactly that.

**D16. Horizon 4 silently refuses long honest chains.** Seat B's four
6-pattern chains refuse `:want-unreachable-within-horizon` at 4; seat B
reports 8 of 10 construct at horizon 8. Which horizon construction answers
to is D14's open question; this is its cost measured.

**D17. The loader accepts pattern ids in two spellings.** Seat B wrote
symbols (`aif/two-layer-calibration`), seat A keywords; `load-declared`
accepted both, and a naive comparison reported zero agreement. A canonical
id form belongs in the loader.

**D12. Nothing at the call site can say whether a constructed plan is worth
taking.** The constructor takes a plan only if its G beats the empty
family, but G is computed later, in `select-and-record-cascade!`. I2 had to
inject a G. With plain precedence length, M-aif-eig refused
`:construction-not-taken`; with the empty family pinned worst, it
constructed. The I2 script's pin decides the outcome for that target. No
budget or move cost is declared anywhere either (I2 §5 gaps 1–2).

**D13. Withdrawals and restore conditions are comments.** The general form
of D9: the constructor's guard vocabulary has `:needs`, `:forbids`,
`:produces`, and no way to say "not until this evidence exists". A
withdrawal has to become data that the constructor and admission can read
(I2 §5 gap 6).

**D14. The constructor and the judge disagree on inputs.** The judge's
observation emits `:unknown`, which the constructor refuses; receipts are
optional in `cascade-problems/assemble` and mandatory in the constructor;
the judge's horizon falls back to T=2 while the sources declare 4 (I2 §5
gaps 3, 4, 7). Each would change which targets refuse, and with which
reason.

**D18. The machine's cascade is a list; the definition says semilattice.**
Everything on the WM path carries a cascade as a `:precedence` vector: the
declared candidates, `cascade_problems/constructed-candidates`, the
constructor's output, admission's rollout, and the P1 probe's scoring. The
paper's own glossary (`p4ng/sec-glossary.tex`, "Policy π") defines the
cascade as a semilattice, sequential dependency (`BV.seq`) plus cross-cutting
co-application (`BV.copar`), calls the temporal reading "the impoverished,
tree-shaped reading", and notes that "a cascade flattened to a linear list
folds to an empty wiring". A semilattice constructor exists outside the WM
path: `futon3a/holes/labs/M-memes-arrows/cascade_construct.py`
(`construct_cascade`, `chosen_semi_lattice`, over
`futon6/data/pattern-phylogeny-edges.json`). Joe (2026-09-24): "cascade is a
semilattice is definitional ... we shouldn't hard-code cascade is a list."
Any fix to D4/D15 must not harden the list form further.

## Design notes (not defects)

**N1. Stances are priming rules, not failed production rules** (Joe,
2026-09-24, after P2 was dispatched). A pattern that names a stance does not
change world state; it conditions which lower-level patterns are likely to
fire: priming, in Hawkins's sense, or a top-down prior/precision over lower
policies. The constructor has one level (needs/forbids → produces), so a
stance has nowhere to go. Conditioning with several parents gives a
structural reason for the semilattice: a lower pattern can be primed by
several higher ones at once. Plan: P2 runs as briefed; its not-a-rule
entries are the stance candidates; a follow-up (P3) asks for priming rules
over them, and both kinds go on the rating sheet.

**N2. The semilattice may be a projection of a hypergraph** (Joe,
2026-09-24; "a proof is not a tree"). The proposed evidence is the superpod
mining runs rendered as margin pages, e.g.
`/var/www/zone.hyperreal.enterprises/wip/mark7-math_9906038-margin.html`
(run mark7master-20260921; generator `futon6/scripts/typeset_preview.py`;
related ticket `futon6/holes/T-argument-outside-of-proof.md`). Not yet read
for this excursion; the representation question for D18 should be settled
against that data, not against the list form now in code.

## Investigations

| id | question | who | job | status |
|---|---|---|---|---|
| I1 | History: what did the pre-H5b proposers propose and on what grounds (feasibility among them)? Is any ruling recorded behind "construction is outside the tick" (D3)? | kimi-2 | invoke-1790256229096-23668-ed1a1a26 | done: `0b06df90`, checked by claude-10 (two code sites at `5d55e7a0^` and the FOCUS.md ruling text read) |
| I2 | Constructor: run `interpretation_construction` offline on the four admitted targets and the substrate missions; what does it build, where does it stop, and what would it need in order to run inside the judge (D4, D6)? | kimi-3 | invoke-1790256230680-23669-faccb791 | done: `56a58026`, checked by claude-10 (script rerun, same four results; admission copy diffed verbatim) |
| P1 | Interpretation probe (D11): two seats independently write cascade-source interpretations for the same 10 missions (inputs frozen at `96fae955`, `probe-interp/inputs.edn`); score by loader + constructor + admission; compare seats | kimi-6 (seat A), kimi-7 (seat B) | invoke-1790258132294-23672-43efc807, invoke-1790258134048-23673-49bd041c | done: A `9e8892b8`, B `29ce17b1`; scored under D11 |
| P2 | Production rules from claude-1's turn interpretations (level 2 over level 1): for every fragment with a pattern citation, write needs/forbids/produces over a per-turn token vocabulary with cue spans, or record why it is not a rule; build a rating sheet for Joe. Live set (60 citations, 37 turns) and historical block 0 (45 citations, 25 turns) | kimi-2 (live), kimi-3 (hist) | invoke-1790262016057-23674-33363f0c, invoke-1790262018097-23675-0cef8b7d | done: live `a30ab5ed` (37 turns, 60 rules, 104 tokens, 14 shared), hist `4e297a30` (25 turns, 45 rules, 70 tokens, 8 shared); not-a-rule 0 of 105 in both; gates rerun clean; 18 live readings quote an empty conclusion (gate does not check); rating sheet `rules-from-turns/RATING.md` |

Both read-only: no clicks, no writes under `data/`, no shared-JVM loads.
