# C532 — F8 refusing convergence checker

Date: 2026-09-05

`convergence_check.bb` checks the adopted ledger rather than trusting its
labels.  It re-reads the eighteen equation identities from
`aif-equations.edn:74-200`, verifies all 54 copied fields, resolves all 36
licences by suffix across futon2, p4ng and mathlib4, checks the fixed F6 rung
order, recomputes leading legs, validates both certificate forms, and derives
accepted-run status from `data/wm-step/w1/pin/pin.edn:1`.

## Run-identity decision

Both predicates are computed.  The F6 predicate at
`scripts/generate_variable_situation_accounting.bb:450-462` sees only a
top-level map or top-level sequence of maps and finds zero run identities in
the licences at witnessed-or-higher rungs.  The checker licenses evidence using
a run identity anywhere in the artifact tree, while separately printing the
F6 count.  This admits the F7 decision whose identity is deliberately nested at
`runs/F7-cascade-policy/f7-cascade-policy-decision.edn:146`, without confusing
it with a fixture: the same record must also satisfy the quantity-specific
policy-set predicate (its schema and at least two constructed candidates).
Thus nested identity alone is insufficient.

## Certificates and acceptance

Artifact certificates and trailing-leg certificates are disjoint checked
forms.  A non-green or non-accepted artifact certificate must explain why; a
trailing certificate must name `:spec`, `:impl`, or `:both` and carry both its
reason and licensing condition.  Only a green artifact whose nested run id is
present in the accepted-step authority can support `:converged? true`.
Declared `:accepted-run?` is compared with that computation, not trusted.
The three R5 simulations remain valid artifact certificates but correctly
support zero convergence because `runs/F3-node-sim/00-r5-pilot.edn` has no run
identity.

All four ledger caveats are pinned: Box-5 staleness, the R5 certificate limit,
the R17 class-(b) divergence, and the retired scalar F/flag-gated F-pi status
(`CONVERGENCE.edn:7-12`).  Section 14 of the shared negative controls plants
each refusal on temporary copies and leaves the committed ledger unchanged.

No registry, source, decision, certificate, or ledger was changed.  No live
tick, run lock, generator, or publish was used.

## Review findings, repaired in place (reviewing seat, 2026-09-05)

The review was a real gate and found three defects, all in the same place: the
delivered checker validated the eighteen rows exhaustively and the ledger's own
FRAME not at all.

**1. THE CAVEAT PIN DEFENDED THE HEADLINE, NOT THE CAVEAT.** `required-caveats`
matched four substrings, so a caveat could be rewritten to say the opposite of
itself and still pass. Plant 31 of the reviewing seat's suite
(`runs/F8-convergence/leg3-slice2-adversarial-plants.bb`, committed at 19accec9
before the delivery was read) replaced the second caveat with "R5 CERTIFICATE
LIMIT: resolved, nothing to see here." and the gate reported ACCEPT; so did a
longer rewrite asserting that the R5 pilot IS a shipped accepted-run
certificate. Hand plants then found the rest of the family: deleting
`:registry-basis`, `:rung-rule` or `:rung-rule-limit` outright was accepted too.
Everything the frame asserts — what the rungs mean, where the rule stops, what
the transcription is against, which limits are still live — was editable with
the gate silent. The four frame fields are now pinned BY CONTENT against a
SHA-256 recorded in the checker (`:error/frame-digest-drift`), and each is
required non-empty (`:error/missing-frame-field`). Re-pin deliberately, in a
commit that says why the frame moved.

**2. THE NESTED-IDENTITY POLICY CARRIED NO OBLIGATION.** Licensing a
witnessed-or-higher rung on a run identity found anywhere in the tree is the
implementer's decision to make and it is argued above. But it means the ledger
claims a rung the F6 predicate refuses, and the ledger has to say so. It did not
have to: with `:rung-rule-limit` deleted, `:policy-set` still stood at
`:constructed` under a rule that answers false on its own licence, with the
limit recorded nowhere, and the checker accepted it. The obligation is now tied
to the policy that creates it — a leg licensed by nested-but-not-top-level
identity requires a non-empty `:rung-rule-limit`
(`:error/unstated-rung-rule-limit`) and a non-empty `:basis` on that leg
(`:error/nested-licence-without-basis`) — so it survives a deliberate re-pin of
the digest rather than resting on it.

**3. TWO NUMBERS ON THE ACCEPT LINE WERE STRING LITERALS.** "54 identity fields,
36 licences" were written into the line, not counted, inside a sentence whose
label says they were counted — and control 14u pins that line, so a control was
defending a number nobody computes. This is the leg-2 review's finding
recurring (claims counted inside a total that said they were not). Both are now
recomputed from the population they describe. The printed line is byte-identical
today, which is why 14u needed no edit: the numbers were right, the claim that
they had been measured was not.

**WHAT WAS NOT CHANGED, AND WHY.** Plant 19 removes the sentence in
`:policy-set`'s `:basis` that states the F6 limit, keeping other prose, and the
repaired checker still accepts it. That is deliberate: the limit is pinned once,
by content, where the ledger states it for the whole file, and the row-level
requirement is non-emptiness. A checker that greps a prose field for a
particular sentence is checking spelling. Plant 34 reverses the row order and is
accepted, which control 14t asserts on purpose — the specification speaks of a
row SET.

**THE PACKET WAS WRONG BEFORE THE DELIVERY WAS.** The first dispatch (job
`invoke-1788633962972-8010-71ed4f50`) wrote nothing and reported a contradiction:
requirement (h) as transcribed demanded an explicit `:trailing-leg` reason,
which the three committed artifact certificates do not carry, while the
acceptance bar demanded that the committed ledger pass. That reading was
key-shaped and the ledger uses two certificate forms; the reviewing seat
adjudicated the two forms and re-dispatched. Refusing to edit the subject until
the subject passed was the right call and is recorded here because a checker
that repairs its own subject is not a check.

**WHAT THE REVIEWING SEAT CHECKED,** so the review is auditable: the independent
probe was written and committed (a401ecd2) BEFORE the packet went out, and it
corrected two of the packet's premises (the caveats are four, not the three
C531's prose names; `:accepted-run?` is recomputable against the pin) and one
error of its own (it resolved the certificate artifact against the wrong root
and reported "exists false" about a committed file, fixed before it became a
premise); the 34-plant adversarial suite was committed (19accec9) BEFORE the
delivery was read, and 32 of its plants are scored — 0 findings after the
repair, 1 before; four further cases were planted by hand against the frame and
all four were accepted before the repair and refuse after it; clj-kondo 0/0 on
`convergence_check.bb`; check-parens OK; `negative_controls.sh` PASS at 125
negative / 51 positive, exit 0, against a baseline of 102/49 measured before the
delivery landed; `pointer_check.bb` 1697 pointers in 5 files, 0 unresolved;
`worklist_check.bb` 176 items OK; the committed ledger passes the repaired
checker with a byte-identical ACCEPT line. No ruling was written, no registry or
source file changed, `gen_aif_dag.bb` was not run, and no live tick or run lock
was taken.
