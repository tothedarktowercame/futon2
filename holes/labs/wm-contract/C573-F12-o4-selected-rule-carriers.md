# C573 — F12 O4 selected-rule carrier measurement

## Question and pinned corpus

This slice tests whether any member set in the slice-6 corpus contains at least
two rules at the grain actually used by the gate. The checker reads the corpus
and futon3 pin from
`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:12-15`, then
reads every futon3 record with `git show` at that pin in
`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:17-21`.

## Measured answer

The answer is false:
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:18`.
No measured member set contains two selected, non-counterfactual rule carriers.
The checker derives each carrier list from the record member set and the parsed
gate partition at
`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:30-64`.

The dispatching premise therefore held. The gate removes counterfactual rules
when it constructs `selected-rules` at
`futon3c:scripts/zaif_cascade_gate.clj:412`, then filters those selected rules
against cascade membership at `futon3c:scripts/zaif_cascade_gate.clj:413`.
The two rules counted by slice 6 are explicitly counterfactual at
`futon3c:scripts/zaif_cascade_gate.clj:234-235` and
`futon3c:scripts/zaif_cascade_gate.clj:249-250`.

## Delta from slice 6

Six rows differ, not four: the kangaroo `:find`, budget cascade, and marginal
cascade in each retrodiction encoding. One example shows slice 6's two carriers,
this slice's empty selected-carrier list, the old row pointer, and the named
reason at
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:1158-1171`;
the budget and marginal rows follow at
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:1225-1239`
and
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:1293-1307`.
The same three deltas for the per-clause encoding are recorded under run-record
id `22-o4-selected-rule-carriers`.

Slice 6's headline was about both preconditions and rested on four cascade rows
with edges. This slice measures precondition A at the gate grain and also exposes
two additional `:find` rows where slice 6 counted the same counterfactual rules;
that distinction is recorded rather than rewriting the signed slice-6 artifact.

## Counterfactual execution path

The counterfactual rules do reach `x-plays`: the gate forms that rule stream at
`futon3c:scripts/zaif_cascade_gate.clj:419`. They do not reach O4. The O4 pair is
constructed only from `cascade-rules` at
`futon3c:scripts/zaif_cascade_gate.clj:444`, its threshold is applied at
`futon3c:scripts/zaif_cascade_gate.clj:445`, and the emitted O4 block is at
`futon3c:scripts/zaif_cascade_gate.clj:549-558`. The measured path conclusion
and all five pointers are recorded at
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:6-17`.

## Source-grounded controls

The validity predicate recomputes rows and counts from pinned record bytes and
the gate text, verifies carrier membership, checks slice-6 deltas, and resolves
the pointers carried by the report at
`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:109-146`.
Six plants flip a counterfactual reading, shift a rule pointer, alter a count,
add a nonmember carrier, drop a member-set row, and erase a real delta at
`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:147-172`.
Every plant landed and was refused for a named source-grounded reason in
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:19-63`.
The unmutated verdict is true at
`futon2:holes/labs/wm-contract/runs/F12-organise/22-o4-selected-rule-carriers.edn:2258`.

## Scope

This is a measurement only. It takes no ruling, changes no choice status, and
does not modify the gate, any earlier signed artifact, Lean, or machine source.
Unresolved source claims: not found.

## Review (seat wm-build-work, 2026-09-07)

Re-ran the checker twice: exit 0 both times, artifact byte-identical, sha256
`8326eef1e35fa2f9d7053266869d15cf23aed6e926a3e24c24c33bb707b309f7`, matching the
value the delivering seat reported. clj-kondo 0 errors 0 warnings;
`check-parens.el` OK; negative_controls PASS (133 negative, 53 positive);
pointer_check 3,555 pointers in 6 files, 0 unresolved.

Read the gate rather than the report: `{:id` occurs exactly four times in
`futon3c:scripts/zaif_cascade_gate.clj` (206, 218, 234, 249), so the checker's
regex table parse is the rule table and nothing else, and the two
`:counterfactual? true` keys sit at 235 and 250. The selection path was printed
line by line — 412 `selected-rules`, 413 `cascade-rules`, 419
`counterfactual-rules`, 444 `two`, 445 `o4-exercisable?` — and `two` is built
from `cascade-rules`, so a member set whose only carriers are counterfactual
reads `:exercised? false` at 549. The dispatching premise holds as recorded.

The headline was re-measured here without the checker. A sweep of all ten pinned
records collected every map anywhere in the tree carrying a sequential
`:members` or `:selected` — a superset of the checker's `:find`/`:cascade`
criterion — and found 71 such nodes, the same count as the artifact's 71 rows;
the maximum non-counterfactual carrier count over all of them is 1, held by
zaif's three member sets. Every occurrence of the four rule ids anywhere in the
corpus was also listed: construct's five are in `:admitted`, `:scores-at-step-0`
and `:record` rather than a member set, and open-short-cue's two are
`:zero-mass-pattern` values. So `false` does not rest on the walk missing a
node. This is the one claim the plant battery cannot reach: a member set the
walk never visits cannot be planted by mutating the report, so it has to be
checked against the records directly.

One repair, made in review rather than re-dispatched: the carrier count was by
occurrence, not by rule. The gate builds `two` by filtering the four-entry rule
table, so an id contributes at most once no matter how often it appears in a
member set; the checker counted list occurrences and would have read a member
set that names one selected carrier twice as meeting precondition A. The three
carrier expressions now count `distinct` ids
(`futon2:holes/labs/wm-contract/f12_o4_selected_rule_carriers.bb:62-64`). No
member set in this corpus repeats a carrier, so the artifact is unchanged —
same sha — and the verdict is untouched; the fix removes a grain mismatch of
exactly the kind this slice was dispatched to correct, not a wrong number.

The correction is contained, which the review checked rather than assumed. The
only readers of slice 6's two-carrier reading outside its own artifact and C572
are this slice's checker and the F12 ledger row; a grep of the registries for
the two counterfactual ids and for `kangaroo` returns nothing in
`aif-equations.edn`, `p4ng/empirics-futon/control-map-edges.edn` or the F12
decision sheet. The registry's own statement of this quantity is the opposite
one and already agrees with this slice: `:organise-o4-denominator` says the
maximum rule-carrying member count on any recorded run is 1
(`futon2:holes/labs/wm-contract/aif-equations.edn:745`), citing
`futon2:holes/labs/wm-contract/runs/F12-organise/02-o4-reachability.edn:354`,
where `:max-rule-carrying-members-on-any-recorded-run` is 1. So slice 6's
reading was the outlier, no ruled or registered choice rests on it, and nothing
in the registries needs repair.
