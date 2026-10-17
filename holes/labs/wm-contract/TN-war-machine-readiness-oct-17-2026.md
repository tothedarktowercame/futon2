# War Machine readiness campaign — 17 October 2026

## Campaign status

Prepared, not started. Joe authorized beta = 1 on 2026-10-17 as the neutral
starting precision for this five-click readiness campaign, whose purpose is to
make the system ready to tune. It is not claimed to be learned or calibrated.
The sealed declaration is
`resources/wm/reading-precision-beta/wm-readiness-2026-10-17-v1.edn`, bound to
reading-model identity
`f9c5cf02df9f933d2b554efbcb7b70ceb2ce1dddf595d35c0bd88245ae4186b2`, with
declaration digest
`72526b4b8b64dd084c67deb9903703b3290da0b494141c2e308a7dfb4ca898c0`.

Before click 1, the serving JVM must load and register that exact declaration,
then read it back by identity and verify the seal. Later clicks may carry beta
only while the reading-model identity is unchanged. An identity change pauses
the campaign for a new explicit declaration; neither the debugger nor the
runner may invent or silently migrate the value.

## Fixed Zai cast

Every click in this campaign will pass this cast explicitly; there is no roster
fallback:

- author: `zai-2`
- reviewer: `zai-6`
- repair reviewer: `zai-7`

At 2026-10-17T19:32:35Z all three Agency sessions were reset through the
`reset-session` boundary. Their prior session identities were discarded. The
post-reset roster reported all three as `idle`, `invoke-ready? true`, with no
session identity; first use will therefore create fresh sessions.

## Recording contract

For each of the five clicks append:

1. authorization and durable budget receipt;
2. exact cast and fresh session identities;
3. click, run, attempt, and selected target/cascade identities;
4. phase work time, wall time, and debugger dwell separately;
5. every held condition, repair dispatch, verdict, commit, reload, and restart;
6. run record, report card, requirement verdict, and publication paths;
7. whether a tick was carried and what grounded work changed;
8. unresolved defects and the evidence required before the next click.

The five clicks run sequentially. A debugger repair resumes the held phase of
the same click and must not spend another click or rerun outer selection. A
failure requiring operator authority pauses the campaign instead of repeating
the same predictable failure.

## Runs

### Click 1 — selection stopped by an obsolete/undeclared loop-node contract

- Authorization/load preflight: the five-click grant was recorded by raising
  the ordinary-click allocation from 81 to 86. The serving JVM reported five
  available clicks and read back the intact beta declaration for the exact
  identity above. Zai-2, Zai-6 and Zai-7 were reset immediately before issue.
- Click: `wm-click-454f8ade-720e-4ade-bec4-a0381b552aba`.
- Run: `2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492`, attempt 002.
- Selected target under construction: `M-recommendation-bindings`; one
  occurrence-keyed cascade with twelve pattern occurrences and validated
  four-map squares reached the selection gate.
- Terminal result: `build-failed`; no tick carried and no mission work was
  enacted. Run record:
  `data/wm-runs/tick-run-record-2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492.edn`.
  Report card:
  `data/wm-runs/2026-10-17-0daab1e4-c676-4070-8a34-17e6c1e00492/report-card.md`.
- Timing: 563,598 ms wall time, comprising 164,084 ms active selection and
  369,049 ms debugger dwell. Within selection: meta-field ranking 76,863 ms,
  strategic judgement 54,836 ms, strategic scans 20,374 ms, G admission
  6,282 ms, declaration/interpretation loading 5,111 ms, and the remaining
  measured subphases below 0.2 s each.
- Held condition: `required-loop-node-unexercised`. R6, R13 and R14 were
  legitimately bypassed because this run had one admissible policy and one
  action group. CTAU-CLASS was refused because the current reading-policy
  scorer retains progress-preference evidence, whereas the old gate requires
  a `:class-emission` observation model from the retired class-scoring route.
- Debugger handling: Zai-7 diagnosed the held phase under job
  `invoke-1792266440527-16272-d984469f`, then confirmed the terminal record
  under `invoke-1792266841488-16282-3cc3a535`. Both returned
  `cannot-repair`; no code was committed or hot-loaded. The second dispatch
  was the loop seeing the same terminal failure after abort, not an independent
  defect.

Disposition before click 2: do not click again unchanged. Joe must choose
whether CTAU-CLASS is (a) re-declared for the reading route, (b) implemented by
adding a genuine class-emission model to that route, or (c) formally retired
from the registry, Lean contract and live gate. Independently, the scorer's
observation-model evidence is currently lost when `family-selection` replaces
the scorer certificate's `:f`; retaining that evidence is a real repair but is
not by itself sufficient to settle CTAU-CLASS.
