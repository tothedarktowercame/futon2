
## C391 — the adapter check pins occurrence, not correspondence (my fix, owner review of C379)

**Verified C379 (`9e3da2f`) independently.** What I checked, and what it establishes.

**No Lean file is touched in the commit.** The diff is the validator, 27 receipts, one test file and a
report — so *"rebuilding restored elaboration without weakening any theorem"* is supported: no theorem was
restated. The receipts' apparent 20–40 line shrink is a pretty-print collapse, not content loss.

**Compared all 27 receipts as EDN data across the commit** rather than reading two by eye. Every difference is
one of three kinds:

1. `:result {:axioms nil, :exit 1}` → `{:axioms ["propext" "Classical.choice" "Quot.sound"], :exit 0}` on the
   eleven that C376 caught. **Consistent with the stale-`.olean` diagnosis.**
2. `:dependency-closure {:mode :author-declared-source-slices, :machine-complete false, :reason
   :lean-transitive-closure-not-content-pinned}` added to **every** receipt — the lane recorded C379's residual
   *in the receipts themselves*, which is what C386 was going to ask for.
3. **Adapter `:lean-field` strings rewritten** on six receipts, from prose labels to literal source fragments:
   `"Policy constructors"` → `"inductive Policy"`, `"controlled support rows"` → `"| (s, .stay) => [s]"`,
   `"posterior support/mass rows"` → `"support := fun _ => [.cautious]"`.

**(3) is the finding.** The same commit made "Lean-field strings must occur in retained declaration slices" a
requirement **and** rewrote the strings so they occur. That is defensible — the old strings were prose that
could never appear in Lean source, so they asserted nothing checkable — but it narrows what the check proves.

**The strings are correctly scoped.** I checked the one that looked worst: `"support := fun _ => [.cautious]"`
also appears in `ParameterPosteriorKernelPriorNegative.lean`, but the receipt's basis is
`ParameterPosteriorKernelWitness.lean` and it occurs there at line 19, inside the pinned slice. **Not a
cross-file match.**

**What it does not establish.** The mapping `[:rows] -> "support := fun _ => [.cautious]"` carries an
`:expected` of four rows, each with a support list and a mass map. **The pinned string is one line covering the
`support` half of one constructor arm.** So its occurrence does not establish that the four-row table
corresponds to the Lean definition. Contrast `channelCount` expected `14` — a field name against a scalar,
where occurrence and correspondence nearly coincide.

**So the check catches what C376 mutated** — deleted adapters, empty adapters, `unrelatedField` — and that was
its purpose. **It does not check correspondence between `:expected` and Lean content**, and for the six
rewritten receipts the gap between the two is widest.

**Not a defect in C379 and 31/31 stands.** It is a limit of the same shape as C379's stated residual: the pin
is load-bearing without being complete. **Route to `wm-nouns` after C386 returns** — same file, same question,
and two lanes in one file is how a repair gets lost in a merge.
