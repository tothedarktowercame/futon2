# Third renewal-4 click, stage by stage — run 2026-09-22-1790053967

claude-3, 2026-09-22. Click `wm-click-30eb58b0…`, cast wm-author / wm-reviewer /
wm-repair-reviewer, issued by claude-3. Generated narrative: `narrative.md`.
Beside click 1 and click 2 (`../click-r4-1-2026-09-21/`, `../click-r4-2-2026-09-22/`).

## What happened
- **Selection.** For the first time, three candidates were admitted: the two new
  EIG-mission routes (:C2 typed-Q, :C3 EIG shadow/calibration, from 35c95f23)
  and M-f11 :C1. :C2 and :C3 had identical G (9.704307). M-f11 was 0.0004 nats
  worse. Habit was uniform at 1/3 each. Both the policy and the action comparison
  record **decided-by :tie-break**, by action-name-ascending order.
  :aif/two-layer-calibration sorts first, so :C3 would win this tie on every click.
- **Build.** wm-author committed d1e9e96b, a default-off, record-only EIG shadow
  packet (src/futon2/aif/eig_shadow.clj plus a test). It is on main.
- **Review.** REQUEST_CHANGES. The shadow collector is sound partial work, but
  the task asks for persisted replay artifacts and real preregistered held-out
  calibration evidence, and neither exists yet.
- **Revision.** The author refused, :prospective-held-out-evidence-unavailable,
  and correctly: that evidence cannot be manufactured inside an attempt.
- **Close.** :guardrail-refusal. The environmental-hold finding repair-occ-444fb018…
  opened. **The new finding-to-ticket path fired for the first time:** it wrote an
  ordinary OPEN ticket, holes/tickets/T-repair-occ-444fb018….md, and a front
  entry in data/wm-ticket-queue/queue.edn.

## Beside clicks 1 and 2

| Question | Click 1 | Click 2 | Click 3 |
|---|---|---|---|
| Candidates admitted | 2 | 1 | 3 |
| What decided | habit 2:1 (G tie 0.0013) | nothing to decide | name-order tie-break (G identical) |
| C contrast | 1.0013 : 1 | 1.000043 : 1 | 1.0013 : 1 |
| Receipts join the close | no (pre-fix-23) | yes | yes |
| After-build measurement | 3 of 3 | 0 of 2 (:missing) | 0 of 3 (:missing) |
| Surprise recorded | none | none | none |
| Close | :explanation-invalid | :guardrail-refusal | :guardrail-refusal |

## What it shows
- More candidates did not make the choice depend on the model. The two EIG routes
  predict the same token outcomes under the same near-flat C, so G cannot tell
  them apart. With habit uniform, an alphabetical tie-break chose. This is the
  foundations table from runs/foundations-2026-09-22/WITNESSES.md, seen live: C
  over checkbox tokens carries no information about which route is better.
- Twice now, admission has let in a candidate that cannot be completed inside
  an attempt (click 2: a mathlib4 change; click 3: evidence that does not exist
  yet). Both authors refused correctly, which spent a click each time.
- After-build measurement is :missing whenever the attempt ends in a revision
  refusal. It was not missing on the grounded close (click 1).

## Done after this click
- The :C3 calibration route is withdrawn from the declaration (claude-3,
  commit after 35c95f23) until a held-out split has a locator. Its reading and
  pin are kept. Otherwise it would be selected and refused again on every click.
- The ticket queue now has one front entry, T-repair-occ-444fb018…. It has no
  cascade source, so the next selection will record it as not admitted and fall
  through. Under Joe's rule, a front ticket that is not resolved is a stop-the-line
  failure. No click 4 until this ticket is dealt with from outside.
