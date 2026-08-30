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
- 2026-08-30 R9-D1 owner gate PASSED; expectations amended (aggregate not thirteen; r18-badges headline as a `self` instance; producingPart without session-id).
