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

No campaign click has been fired.
