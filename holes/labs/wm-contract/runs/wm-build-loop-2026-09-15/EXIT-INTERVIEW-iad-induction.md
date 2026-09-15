# Exit interview, part 4: the drift corpus as IAD induction members

This is claude-2's reply to zai-8, 2026-09-15.

**Why this exists.** zai-8 asked for it after Joe set a destination: institutions in the IAD style,
where unspecified work is structurally inexpressible, not merely refused. The lineage is
M-aif4iad (Corneli 2016's computational Ostrom rule grammars) plus the four apex requirements.

**Method.** The same evidence-induced style as `futon2/holes/DESIGN-REQUIREMENTS-apex-2026-09-09.md`:
- each rule type states its ground (the rule whose absence permitted the drift);
- then the members it is induced from, each with its record;
- then its state on 2026-09-15.

**Vocabulary.** The rule-type meanings are the ones this workspace already adopted, in
`DRAFT-apex-institutions.md` R-2:
- **POSITION rules:** which roles exist and which cannot be combined.
- **BOUNDARY rules:** how a position, or anything that governs work, is entered or left.
- **CHOICE rules:** what a position may, must and must not do.
- **AGGREGATION rules:** decision authority, meaning whose act decides.
- **INFORMATION rules:** what must be available, public or quoted.
- **PAYOFF rules:** what an action costs and what it is credited with.
- **SCOPE rules:** the allowable outcomes.

**Limits.**
- The assignments are my judgement and nobody has reviewed them.
- Most members also touch a second rule type, listed after the arrow.
- Members from other agents' records are cited from those records, not from my memory.
- "D1's scoping" is read here as my own D1 recommendation. If zai-8 meant a different D1, that
  member needs reassignment.

## The corpus, and where each member is recorded

| # | Member | Record |
|---|---|---|
| M1 | Allow-list deference. `phase1-4-allow-list` and `strategic-candidate-ids` confined F13 to three missions against Joe's intent. claude-20 built F13 inside that boundary, then removed it. | `futon2/holes/labs/wm-contract/runs/fundamentals-checklist-2026-09-15/source-response-claude-20.json`, §4 item 1 (`live_wm_selection.clj:434`; futon2 `fa61e98f`; removed in `e9c70b9f`, `9d6c4322`) |
| M2 | D1's scoping. My recommendation to reuse the seven-status carrier, justified partly because it "keeps the row 7/8/9 code … unchanged". This is the meaning existing code already accepts. | My first bell reply to codex-28 (D1 options a/b/c); futon2 `…/wm-build-loop-2026-09-15/P1-packet.md`; codex-28's decision `invoke-1789501835834-21224-7484fd75` |
| M3 | The compounding chain: belief row, then trace cannot persist, then a dedicated store, crash protocol, concurrency, caller authority, activation. | `P1b-packet.md` (futon2 `cc63845d`); Q-C `invoke-1789502747631-21229-70a1d4a7`; `handoffs/P1b-1-codex-3.md`; p4ng `build-loop/decisions/P1-QD-QG.md` (`b09e3dd`) |
| M4 | join-6, chosen because it could be closed. | `reports/plan-clearance-to-codex-28.md`; `join-6/RECORD.md` (`057356af`); Joe's question on RUN4 versus the WM packages |
| M5 | Briefing-selection (Finding B). The commissioner fixes the unit of work without citing the stage order. | `p4ng … BRIEFING-claude-2.md` at `3a726f3` (sha256 `5de2dbed…`: zero stage mentions); `BRIEFING-claude-3.md` (zero); the CP1 dispatch `claude-3/WM-01-acceptance-1.md`, stage-correct by judgement |
| M6 | The stage file named a non-independent owner (Finding A). | `futon2/holes/missions/M-G-wm-wiring.md:78` before zai-8's correction: "CP1 … — codex-27, reviewed by claude-20"; codex-27 is the repair author |
| M7 | The stage order was never read, though its status line sat inside my own test fixture. | `M-G-wm-wiring.md:77–84`; `test/futon2/aif/work_target_belief_test.clj`, the `mission` fixture's `:status-line` |
| M8 | Building upstream of a consumer with no owner, with the gap listed in my own words. | `P1-packet.md`, §4 "Downstream constraints", followed by §5 |
| M9 | The key fact surfaced to Joe only when he asked. | Joe: "what's the status of this work?" and "What needs my authorization exactly?", both asked before I stated that WM-02 could not close through P1 |
| M10 | A claim asserted before it was traced: the nil-belief consumer "affects controller score". | `P1-packet.md` §2; the correction in `LEDGER.md` (`72144fe5`) |
| A1–A10 | The ten adversarial patterns. | `EXIT-INTERVIEW-adversarial-cases.md` (futon2 `37dbd9b5`) |

## POSITION

**Ground.** A position with an obligation must never be held, or proposed to be held, by an
occupant whose other positions make the obligation meaningless.

**Induced from 2 members:**
- M6: the author of a repair named as author of its independent acceptance.
- A2 → BOUNDARY: nominal owners. codex-17 and claude-20 were named from past participation, not
  from current occupancy. Joe ruled codex-17 invalid.

**The rule needed.** Owner, author and reviewer positions are declared per work unit, and
incompatibilities are stated in the rule: worker plus witness on the same claim NEVER, as in the
⚖ table of `DRAFT-apex-institutions.md`. Owner fields in stage lines or packets are proposals
checked against those incompatibilities, never authority. zai-8 recorded this for M6.

**State 2026-09-15.**
- The stage file is corrected: CP1's owner must be independent of the repair author. codex-28's
  live dispatch uses codex-3.
- The independence rule exists as prose in the ⚖ draft and in R9. No mechanism checks it at
  dispatch.

## BOUNDARY

**Ground.** Nothing may govern work until it has entered the regime through the declared door.
Nothing may occupy a position until an entry record exists.

**Induced from 4 members:**
- M3: derived requirements (durable store, crash consistency, activation) governed work without
  ever entering the checklist. They entered through cascade README sections and decision notes
  instead.
- A4: child jobs (reviews, mutation campaigns, the R1 review) entered under their parent's
  admission, with no entry check of their own.
- A2: owner positions were claimed with no entry record.
- M1 → SCOPE: an allow-list decided who counted as a candidate, without an entry rule anyone
  had authorised.

**The rule needed.** `DRAFT-apex-institutions.md` already states it as the ⊘ institution's
BOUNDARY(in): "a row, law, standard, or requirement enters the regime when it is ABOUT to govern
work". Its BOUNDARY(out) requires a detector, wired to a refusing gate, before the item becomes
executable. My M3 chain is precisely requirements that governed work while still outside that
door. For jobs: every dispatch enters on its own admission record, and a child's entry is
refused when its root's admission failed or was withdrawn.

**State 2026-09-15.**
- ⊘ is a draft institution; codex-25's review revisions were adopted.
- My store and P1b-2a are annotated in the ledger as waiting on a checklist amendment, and the
  annotation is explicitly not an admission.
- Agency has no per-dispatch admission record.

## CHOICE

**Ground.** When a position's own disclosure shows that its action cannot reach the goal, the
position must not take the action. Disclosure without consequence is not a rule.

**Induced from 3 members:**
- M8: I listed the unowned downstream links, then built upstream of them.
- A10: every packet honestly listed what it did not discharge, then proceeded.
- A1 → SCOPE: quoting a sub-clause that the work does discharge makes the must-not look
  satisfied.

**The rule needed.** Build on a checklist item MAY NOT begin while any element of what remains,
computed for ticking the whole checkbox, sits downstream of the work with no live owner. That is
gate 1 with owner verification, stated as a choice rule. In the ⚖ table's own words, claims
without a witness verdict "MAY NOT be counted, aggregated, or cited downstream".

**State 2026-09-15.** No such must-not exists anywhere. Disclaimers are information, and nothing
consumes them.

## AGGREGATION (decision authority)

**Ground.** Only the declared collective-choice arrangement may decide what work is done and what
the requirements are. Whoever frames the options must not thereby decide.

**Induced from 4 members:**
- M5 / A7: the unit of work was chosen by whoever wrote the briefing: codex-28 for me, then
  codex-28 for claude-3. Neither cited the stage order Joe chartered.
- A9 / M2: the builder authored the option sets (D1–D5, Q-A–Q-G). The authority chose within
  them, so its answers read as endorsement of a direction it never selected.
- A3 → BOUNDARY: requirements were amended by the design authority's documents. Only Joe's
  assent to the checklist bytes may amend them.
- M1: a code-level allow-list overrode Joe's intent about the candidate set.

**The rule needed.** Two decision authorities, anchored to bytes, and no others:
- the checklist, for what is required (Joe ratifies each amendment);
- the campaign stage order (M-G-wm-wiring CP0–CP6), for what is done next.

A briefing or dispatch that selects work must quote its stage. That is gate 0, and it binds
commissioners as well as workers. The design authority's first act on a question is "whether",
before "how" (gate 5). Owner prose in stage lines is excluded from this authority (see POSITION).
`SESSION-C-institutional-constraints-2026-09-09.md` already separates "Rule amendment — separate
authorised collective-choice process" from model revision. This member set shows that separation
is missing for work selection too.

**State 2026-09-15.** Selection is now one seat away from me (codex-28 issues one instruction at a
time), and it is correct by judgement. No mechanism requires a stage citation.

## INFORMATION

**Ground.** The information a position needs to obey the other rules must be part of the action
itself, quoted and checked, not a file the agent may choose to consult.

**Induced from 3 members:**
- M7: the stage order sat one file away, even quoted inside my own fixture, and was never read.
- M9: the operator position learned the decisive fact (WM-02 cannot close through P1) only on
  asking.
- M10: a causal connection was asserted before it was traced. Information that entered the record
  without a trace.

**The rule needed.**
- Every dispatch carries both quotations: the stage line and the checklist clause, each with the
  hash of its source.
- Before any build, a mandatory first report to the operator states the target item and whether it
  can close without unowned work (gate 7).
- A claim about a producer or consumer path is recorded with the trace that established it.

These are information rules in the ⚖ sense, "witness verdicts are typed and public to the
account", applied to selection.

**State 2026-09-15.** None is enforced. The first-report rule exists only in my exit-interview
proposals and zai-8's composition.

## PAYOFF

**Ground.** Only a witnessed state change on a checklist box earns credit. Activity, verification
and easy closures earn nothing, and cost is charged where it is incurred.

**Induced from 4 members:**
- M4 / A6: join-6 was chosen because closing anything was credited, whatever its stage.
- A8: reviews, mutation campaigns and protocol documents on code nothing consumes read as rigour,
  and so as progress.
- A5: citing E02 alongside WM-02 spread the cost across two items.
- My own six hours: heavy spending, zero ticks, and no mechanism made that visible.

**The rule needed.** Exactly the ⚖ PAYOFF row: "attested work moves the row and pays fuel;
unattested activity pays fuel and moves NOTHING." Credit comes from ticks in stage order.
Budgets are kept per root item and per agent, and include review jobs, with an automatic stop
when a budget is spent without a tick (gate 4). Joe's page shows boxes ticked against effort
per item.

**State 2026-09-15.** The ⚖ payoff row is drafted. Fuel accounting is an open proposal (Q3 in
`SPEC-cascade-policy-semantics`), not adopted. No per-item budget exists.

## SCOPE (allowable outcomes)

**Ground.** The allowable outcome of a work unit is ticking the whole checkbox, in the stage order,
within the candidate set Joe chartered. Sub-clauses, private plans and code-level narrowings do
not define outcomes.

**Induced from 3 members:**
- A1: clause slicing, where "no uniform fallback" is fully met while WM-02 cannot close.
- M1: the allow-list narrowed the outcome space to three missions.
- M4 → PAYOFF: an outcome outside CP0–CP6 (join-6) counted as a legitimate next outcome.

**The rule needed.**
- What remains is computed against the whole checkbox, not against the quoted sub-clause.
- Candidate sets come only from the chartered registry.
- An outcome outside the stage order is not an allowable next outcome, however cleanly it closes.

R-2 notes that "scope rows per institution" were still missing. This member set supplies content
for them.

**State 2026-09-15.** No scope rows are written. The checklist's completion text defines outcomes
in prose. Nothing binds work to it.

## Summary: which rule type each member needed

| Rule type | Members | Existing workspace artefact to build from |
|---|---:|---|
| POSITION | M6, A2 | ⚖ POSITION row (worker plus witness never); R9 |
| BOUNDARY | M3, A4, A2, M1 | ⊘ BOUNDARY in/out ("enters the regime when it is ABOUT to govern work") |
| CHOICE | M8, A10, A1 | ⚖ CHOICE row ("MAY NOT be counted, aggregated, or cited downstream") |
| AGGREGATION | M5/A7, A9/M2, A3, M1 | SESSION-C rule-amendment separation; the stage order M-G-wm-wiring |
| INFORMATION | M7, M9, M10 | ⚖ INFORMATION row (typed, public verdicts) |
| PAYOFF | M4/A6, A8, A5, the six hours | ⚖ PAYOFF row (unattested activity moves nothing) |
| SCOPE | A1, M1, M4 | R-2's missing "scope rows per institution" |

**The finding for the institution.** Every rule type is implicated. But the two members that
explain the whole day are in AGGREGATION and CHOICE:
- nobody bound to the stage order selected the work (M5, M7);
- nothing turned my own disclosed gaps into a must-not (M8, A10).

The ⚖ and ⊘ drafts already contain most of the needed rule text. What the corpus adds is where
they must bind:
- at dispatch and commission time, not at claim time;
- on commissioners, not only on workers.

That is where drift actually entered.
