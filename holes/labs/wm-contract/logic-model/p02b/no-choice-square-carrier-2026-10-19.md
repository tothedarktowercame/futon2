# P02b — no-selected-policy missing-square carrier

Static, constructed fallback investigation at futon2
`bfc268912a51318cb9e701d35ebaa1f89f97a35d`. No serving JVM, reload, click,
Agency contact, or square publication was used.

## Conclusion

P02's original no-choice diagnosis is **not reproduced as a missing-square
producer/consumer connection**. On a refused outer receipt, phase 10 does not
receive the outer pre-choice field's missing-square family. It receives an
empty *inner* field, because the ordinary runner deliberately gives inner
assembly no target when there is no selected outer target. The run is stopped
at selection before phase 10's consumer can request a square.

This is not evidence that square authoring could obtain any choice: the
fallback does not preserve a candidate target to author. It also does not
justify changing the selected-carrier branch; selected omissions may be
intentional because a sealed policy already has a viable selected policy.

## Pinned source chain

All paths below are in repository `futon2` at commit
`bfc268912a51318cb9e701d35ebaa1f89f97a35d`.

1. `meta_live_outer_selector.clj`, `select-live`, lines 602–616 forms the
   outer pre-choice field; its missing-square families are retained on the
   outer receipt at lines 703–739.
2. `scripts/futon2/report/war_machine.clj`, `generate-war-machine`, lines
   6653–6655 obtains the receipt, then lines 6690–6695 define
   `cascade-targets` as `[selected-task-id]` only when that ID is a string.
   A refused/no-choice receipt therefore produces `[]`.
3. The same function, lines 6696–6760, derives `:target-sources` from that
   empty target vector; lines 6764–6767 append the typed outer refusal but do
   not add a target source. Lines 6792–6808 invoke the no-carrier fallback.
4. `wm/cascade_decision.clj`, `cascade-decision`, lines 1348–1386 takes
   `:else`; `target-policy-field` lines 1007–1031 forms a field from the
   assembled `:target-sources`. With `[]`, the field is formed with `[]`
   families, then attached at lines 1377–1379.
5. `reading-family-decision`, lines 1217–1226 abstains on that empty field.
   The runner's phase-selection terminal predicate (`full_loop_runner.clj`,
   lines 6385–6419) permits phase 10 only when `selected-need` is non-nil.
   `occurrence_square_authoring/selected-need`, lines 27–37 sees no family;
   phase 10 (`full_loop_runner.clj`, lines 6474–6479) is not reached.

## Constructed production-shaped fallback

The focused test injects only external read ports (a temporary, byte-pinned
graph and an empty temporary reading root). It calls the real
`cascade-decision` no-carrier branch with the exact post-assembly shape
`{:target-sources [] :refusals [outer-refusal]}`. It does not stub
`cascade-decision`, `target-policy-field`, `reading-family-decision`, or
`selected-need`.

It observes an abstained decision, a certificate field with zero families, and
nil `selected-need`. This differs from P02a's direct consumer fixture: P02b
exercises the actual fallback projection, but it intentionally has no
missing-square family to consume.

## Bound

No P02 repair handoff is proposed. The original "prepare a square to obtain
any choice" diagnosis is not established: the first missing carrier is the
no-choice target population, before the square consumer. Whether an outer
selector should nominate a bounded target for preparation is a separate,
unverified boundary and would need a new packet rather than expanding P02.
