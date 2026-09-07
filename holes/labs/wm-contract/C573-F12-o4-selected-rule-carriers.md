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
