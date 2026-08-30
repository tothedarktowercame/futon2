# P-R9 — No self-certification, as a check the machine runs rather than a rule it cites

Problem record (delivery-lifecycle v2). Node R9 (assurance). Lane 1 of the R-node build.
Opened 2026-08-30 by claude-15 on Joe's go. Owner: claude-15. Tech lead: see `BUILD-tech-lead-charter.md`.

## S1

**problem.** R9 is the one node whose noun is *theory-defined as a rule* (PREREG §1): "a claim must be
backed by a witness outside the part of the system making the claim" (`p4ng/sec-glossary.tex:72`), with
two calibration layers — L1 compares the model's predicted G to its own realised G (cheap, self-referential,
**may never be reported as value evidence**); L2 compares predictions to outcomes the model did not produce,
and only L2 certifies value. Nothing in the stack *runs* this rule. The July machine cited it and breached it:
R20 self-reported, badges were awarded on vocabulary, and the paper's own audit finds "Partial, and breached
at the close step … the author then closed thirteen [rows]; no independent close-verification exists"
(`sec-discussion.tex:238`). Every other node's evidence contract (R2 §5, R8 §5 in the worksheets) names a
`method` and a `falsifier`; R9 is the node that must refuse a contract whose witness the claimant produced.
It is built first because it is the reviewer of everything built after it.

**now.** Provenance exists at the record layer: evidence records carry `:evidence/author`,
`:evidence/session-id`, `:evidence/type` (export at `futon1b/migration-export/evidence.edn`; birth-tagging
in `futon0/scripts/futon0/futonzero/rollout_ledger*`, glossary footnote). The two-layer distinction exists
as prose and as the missing L2 constraint in `GainChain.lean` (R8 worksheet §3: "g may be updated only from
(expected, realized) pairs whose realized the model did not produce" — every pair in the corpus is L1).
`sec-discussion.tex:238` is the only place independence has ever been *graded*, and it was graded by hand.

**solved** (a property of the model, checked before anything runs):
1. A definition on the theory's terms —
   `Producer := {author, session, component}`;
   `independent : Claim → Witness → Prop := witness.producer ∉ claim.producingPart` where
   `producingPart` is declared per claim (a set of producers, never inferred);
   `Layer := L1 | L2`; `valueEvidence w → layer w = L2`.
2. A checker over evidence contracts: given `{subject, claim, artefact, producingPart, witnesses}`, it
   returns `independent | self | unknown` per witness and refuses `valueEvidence` on L1. Run over the R2
   and R8 evidence contracts (worksheets §5) and over `sec-discussion.tex`'s thirteen closed rows.
3. **Falsifier:** a claim whose only witness shares its producer must return `self`; if the checker returns
   `independent` for it, the checker is broken. Registered expectation: the thirteen closed rows return
   `self` (the author closed them); the C1 turn-storage witness (R2 e3, 20/20) returns `independent`
   for *storage* and `unknown` for *reading* — the two must not merge.

**Registered expectation added 2026-08-30 (from R9-D1 + claude-20's deepened review):** the badge file's
own headline (`data/r18-badges.edn:13`, "4 :derived-from-FEP") disagrees with its data (a parse finds 5;
two badges raised on 2026-07-04 at `:141`, `:143`, headline never updated). R9-D2's checker, run over
that file, must return `self` for the headline claim (claimant = the file, witness = the file) and report
the parsed count beside it. The file is left as it is — it is evidence in this lane, and correcting the
headline would erase the instance. Also from R9-D1: `sec-discussion.tex` carries only the aggregate
"closed thirteen", so the earlier expectation "the thirteen return `self`" is replaced by "the aggregate
claim returns `self`"; and `producingPart` must be declarable without a session-id (present on 56,379 of
90,583 export records — the 90,583 verified by claude-15 on 2026-08-30, the 56,379 the builder's).

**Corpus and expectation for R9-D2, settled 2026-08-30 after R9-D1b (codex-5 via claude-20; claude-15
reproduced the parse):** the thirteen closed rows are identifiable — `O1 O2 O3 O5 O6 O7 O8 O9 O14 O15 O16
O17 O20` in **`p4ng/vetting/OBLIGATIONS.md@6c288174`** (22 sections: 13 `fixed`, 7 `open`, 2 unmarked
O21/O1c). **The corpus is sha-pinned**: at HEAD the same parse gives 24 rows and fourteen closed (O1d added
later); a checker pointed at the bare path would be correct about the wrong corpus. **`producingPart` is not
in the artefact:** the ledger has no per-row closer field (the closer is recoverable only from `git blame`,
which is VCS metadata, not a declaration — codex-5's refusal, upheld). Therefore the checker takes
`producingPart` from a **declaration record that cites its source**, and R9-D2 runs twice: (i) against the
ledger alone → the thirteen return **`unknown`** (no declaration); (ii) with the paper's own sentence as the
declaration — `sec-discussion.tex:238`, "the author then closed thirteen", an authored admission by the
claimant — → the thirteen return **`self`**. Both are reported; the earlier expectation "the thirteen return
`self`" was under-specified about its instrument. **The finding for the node:** the artefact built to record
closures cannot say who closed — self-certification was undetectable from the ledger and detectable only
because the author said so elsewhere. That is the R9 problem in one sentence.

**facades** (named against this node): a field `:independent true` set by the claimant (rename); a
`:reviewed-by` written by the author (self-report); "a different model family" treated as sufficient when
the author commissioned and briefed the reviewer (`sec-discussion.tex:238` — independence is about who
chose what would be attacked, not who typed); L1 reported with a value-evidence badge; a checker that
infers `producingPart` from similarity of text instead of reading it from a declaration.

**status.** open.
**holder.** claude-20 (tech lead) → codex-2 (R9-D1)  
**parent.** BUILD (the R-node build; root = M-formal-war-machine)  *(fifth precept, §0.10 — added 2026-08-30)*

## Edges (overlap points — owned by the specification, not the builder)

From `p4ng/empirics-futon/control-map-edges.edn`: `R9→R16` (drawn), `R9→R12` (derived), `R9→R2` (derived:
"observation records became the referent ledger", `sec-discussion.tex:39`). One `Delivery` each, schema to
be fixed in `P-control-map-lint.md`'s fixture set; proposed payload for all three:
`{claim, witness {id, producer, layer}, verdict ∈ {independent, self, unknown}}` with `guarantee
ExactlyOnce`, `idem-key (claim-id, witness-id)`, `receipt = the verdict record`.

## deliveries
- **R9-D1 — discovery, no code.** Inventory every place the stack *claims* independence or awards value on
  evidence: badges (`r18-badges`), `:reviewed-by`/`:attested-by` fields, birth tags, `sec-discussion.tex`'s
  verdict table, `GainChain`'s L1/L2 gap. For each: who is the producer, who is the claimant, which layer,
  and whether `producingPart` is declared anywhere or would have to be. Output ≤ 200 lines, every claim
  with file:line. Refusal permitted.
- **R9-D2 — build (after D1's review).** The checker (Clojure, `futon2/src/futon2/aif/independence.clj`
  or where the tech lead's review of D1 says it belongs), the three registered expectations as its
  acceptance, kondo/parens/tests. Acceptance dry-run against D1's inventory before dispatch.
- **R9-D3 (blocked on spine).** The L2 constraint wired into R8's g update — needs the Outcome carrier.

## log
- 2026-08-30 record written (claude-15).
- 2026-08-30 R9-D1b owner gate PASSED (codex-5 via claude-20; note `6075b82`, authorship marked; parse reproduced by claude-15: 22/13/7/2, ids match). Corpus sha-pinned; producingPart decision above.
- 2026-08-30 R9-D1 owner gate PASSED; expectations amended (aggregate not thirteen; r18-badges headline as a `self` instance; producingPart without session-id).

**Lean interface (2026-08-30, `Holes.lean@93f0da26`, ratified from claude-20's two signature proposals —
charter clause 6, first use):** `IndependenceVerdict = independent | self | unknown` (closed: `unknown` is
a value, not the absence of one); `independenceVerdict declared witness decide?` (closed: the decision
procedure over a *declared* producing part — `none` → `unknown`); holes `r9VerdictSound` (the stated
falsifier: producer inside the part is never `independent`; verdict `independent` ↔ `independent claim w`)
and `r9TwoRunCensus` (the two runs over the thirteen rows: ledger alone → all `unknown`; the paper's
admission as declaration → all `self`). `valueEvidenceRequiresL2` stays its own hole — R9-D2 does not touch
it, and without the two new holes R9-D2 could have passed every gate and closed nothing (clause 3).
**Parser note for R9-D2 (claude-20):** at `6c288174` the status marker takes six forms — `fixed` (7),
`fixed.` (5), `fixed by withdrawal.` (1), `open.` (4), `open` (2), `open, partial progress 2026-07-31.` (1);
HEAD adds `fixed (declared, not renumbered).` — an equality test on `"fixed"` returns 7, not 13. The parser
strips trailing periods, treats a clause after a comma or parenthesis as commentary, and counts
`fixed by withdrawal` as closure; a naive parser reproduces exactly the failure the checker exists to detect.

**R9-D2 read (claude-13, third read; via claude-20 2026-08-30) — two changes, both taken:**
1. *Interface:* `r9VerdictSound` with `_sound` as a hypothesis could never see a wrong checker — replaced at
   `c131af37` by `r9CheckerSound` (a predicate on a given checker, no soundness hypothesis) and
   `r9WmCheckerSound` (the hole); and claude-13's load-bearing lemma added as `r9VerdictConsultsChecker`
   (there exists an unsound `decide?` under which a self-producer is judged `independent` — provable of the
   definition, false if it bypassed its argument). R9-D2 quotes all of `IndependenceVerdict`,
   `independenceVerdict`, `r9CheckerSound`, `r9WmCheckerSound`, `r9VerdictConsultsChecker`, `r9TwoRunCensus`.
2. *The thirteen rows are not uniform:* **3 of 13 name a specific agent as closer** — O7 `codex-1`, O14
   `codex-1` + `codex-7`, O15 `zai` (claude-20 reproduced 3 / 5 / 5: named agent / generic role words only /
   neither). So the paper's blanket "the author then closed thirteen" is too strong in three rows.
   **Run (ii) is NOT cut** (claude-20 had proposed cutting it as uniform-by-prohibition): it becomes per-row —
   the declaration record carries the paper's sentence for the ten rows with no named closer and the row's
   own text for the three — and the declaration must say whether a *commissioned* agent (codex-1, codex-7,
   zai, briefed by the author) is inside the author's producing part. The paper's own audit answers that
   ("independence is absent at *chose what would be attacked*", `sec-discussion.tex:238`): inside. Registered
   expectation under that declaration: 13 `self`; **falsifier with mass:** a declaration that places
   commissioned agents outside the part flips the three rows to `independent` — and that is the argument the
   node exists to have, made checkable rather than asserted.

**Lean interface @`6fd8a33f4d`:** `VerdictRow`/`VerdictTable`, `r9VerdictsSound` (closed, decidable over a recorded table); holes `wmVerdictsLedgerAlone`, `wmVerdictsDeclared` (the two run tables, transcribed), `r9WmVerdictsSound`, `r9TwoRunCensus` (13 unknown / 13 self; the three named-agent rows are where it can fail), `r9VerdictConsultsChecker`. `r9WmCheckerSound` retired (∀ checker — refuted by `fun _ _ => false`).

**Ratified 16:14Z (claude-13's 5th read via claude-20; `Holes.lean@2a98a0cd`, JSON `1bfba954`; 24 bodies /
24 holes):** (1) `VerdictRow` carries the **facts** — `producer`, `declaredPart : List String` — and
`inDeclaredPart` is **computed** (`producer ∈ declaredPart`), never transcribed: the earlier row let the
transcriber set both sides of the soundness implication, i.e. a claim backed by a witness the claimant
produced, inside the node that exists to refuse that. (2) `DeclarationSource := paperSentence | rowText id`
(a sum type, not a free string), with `r9PerRowDeclarations` (closed: exactly O7, O14, O15 carry row text)
and the hole `r9WmPerRowDeclarations` over `wmVerdictsDeclared`. (3) The commissioned-agent question is
**not the builder's to answer**: this record's facades already rule it (independence is about who chose
what would be attacked, not who typed — `sec-discussion.tex:238`), so the packet cites that and forbids
re-opening it; the falsifier with mass is a named closer who was *not* commissioned, which is a fact about
the artefact. (4) "I will diff the Lean literal against the EDN" was a reviewer's promise — the packet now
requires a script that emits the Lean from the EDN, gated by `regenerate && git diff --exit-code`.

**R9-D2 PASSED the owner gate 16:31Z — and the holes moved** (codex-8, futon2 `732f4d7`; claude-20 first-line).
`R9-D2-report.lean` elaborates with **zero errors and zero sorries**: `r9VerdictConsultsChecker`,
`r9WmVerdictsSound`, `r9WmPerRowDeclarations`, `r9TwoRunCensus` discharged by `decide` over the transcribed
tables. Run (i), ledger alone: 13 rows `producer "unknown"`, `declaredPart []` → 13 **`unknown`** (reported as
tautological, as required). Run (ii), per-row declarations: 10 rows `author`/`["author"]`; O7 and O14 `codex-1`,
O15 `zai`, against `["author","codex-1","codex-7","zai"]` → 13 **`self`**. `inDeclaredPart` occurs once in the
emitted file, inside a `simp` — derived, never written. **The falsifier has mass:** a declaration listing only
`["author"]` flips the three named rows to `independent`. This is the R9 argument — *independence is about who
chose what would be attacked* — as a decidable proposition over facts from the artefact, checked by Lean, and
its first witness is bound in `checks/witness-registry.edn`. Note miss (claude-20's): the findings headline
"8" where the derived split is 3 specific-agent / 5 generic-only / 5 neither — one line for the note.
`valueEvidenceRequiresL2` remains open (R8's g).

