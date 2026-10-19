# Lean logic-model units

## L1 — self-heal route, level 1

Module: `DarkTower.WarMachine.SelfHealRoute`.

This unit transcribes the level-1 injury-versus-ordinary routing table from
`phases.md` under the 2026-10-19 ruling: an active injury prepares diagnosis,
runs admission before readiness, and remains on the dedicated self-heal route;
ordinary selection is available only when there is no active injury. Its
finite-table certificate is decided over the transcribed finite edge set and
its finite list of mentioned states; `RouteState` itself is not finite because
readiness refusals carry an absent-term list. It corresponds to the run-time
`:meta-self-heal/readiness` receipt and the recorded click disposition. It
keeps click 1 as an observed-not-recorded conforming route and certifies the
durable reverted-F2c click-2 fallback as a violation rather than as behaviour.

Still pending: an admission witness carrying its bounded run result, and an
enact witness.  Their states are present in the table but have no record
witness in L1. Finding F7 is also pending: click 1's binding carries only
binding/run-record absence, while the dedicated-route disposition existed in
the serving process's last-result and is not durable; futon3c owns its repair.
