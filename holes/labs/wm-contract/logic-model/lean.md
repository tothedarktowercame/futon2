# Lean logic-model units

## L1 — self-heal route, level 1

Module: `DarkTower.WarMachine.SelfHealRoute`.

This unit transcribes the level-1 injury-versus-ordinary routing table from
`phases.md` under the 2026-10-19 ruling: an active injury remains on the
dedicated self-heal route, while ordinary selection is available only when
there is no active injury.  Its finite-table certificate corresponds to the
run-time `:meta-self-heal/readiness` receipt and the recorded click
disposition.  It includes the conforming click-1 route and certifies the
reverted-F2c click-2 fallback as a violation rather than as behaviour.

Still pending: an admission witness carrying its bounded run result, and an
enact witness.  Their states are present in the table but have no record
witness in L1.
