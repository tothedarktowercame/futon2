# PLAN — four days of codex+zai leverage: how/why coverage and zaif personas

Commissioned by Joe (2026-09-08, emacs-repl, verbatim): "we need to get about
6x leverage from Claude to Codex + Zai, so I suggest we look into getting the
zaif harness loop and how/why pattern mining loops up and running again. if
we can run these for 4 days efficiently, we may actually get something
approaching a complete coverage of the pattern library with how/why links,
and some new usable Zaif harness material. the latter is most underdetermined,
so let's be strategic about that. My plan is that Zaif agents should model
all stakeholders that aren't just design and code, so getting some of those
sketched in broad brush terms. We have R numbers to go on, but also the new
Apparatus material, and WR patterns... but fundamentally I think the right
way to go is from a top-down breakdown of the Main Problem now discussed in
Cascade Live. And for the sake of my inner child let's use some
chipwits-forth ideas to see if we can understand a breakdown of personas
against harness ops."

## Seats (the 6x arithmetic)

Every loop runs with zero Claude in its per-iteration path; claude-1 holds
stop bells and spot reviews only.

| loop | work | review | state at plan time |
|---|---|---|---|
| wm-build-loop (wm-contract) | codex | zai-1 | running since 02:59Z |
| library-build-loop (library-loop) | zai-1 | codex | restarted with mining rows (this plan) |
| zaif-build-loop (zaif-harness) | codex | zai (flipped from claude, this plan) | restarted with persona rows (this plan) |
| spider fleet (futon3 checks/spider_fleet.clj) | zai-2/3/4 | codex-20 attests | seats minted 2026-09-08; budgeted runs |

zai seats: zai-1..zai-4 registered (zai-2 via /agents/auto; zai-3/4 explicit).
zai-1 stays the loops' whistle seat; the fleet gets zai-2..4 so loop reviews
and fleet mining do not serialize through one agent.

## Track 1 — how/why coverage of the pattern library

Ground truth at plan time (N-zai-roles-and-tools.md:15-30, runs/L19-how-side.md):
1,335 flexiargs; 1,420 @why occurrences across 855 files; @how a thin layer
(542 carriers, only 21 resolving values). Refusal gate under
down-problems+wr: zaif 5/53, ants 5/6, construct 11/33, open and
retrodiction 817/1239 each. The L19 verdict stands: the remaining refusals
are an AUTHORING gap, not a linking gap — so the track leads with the
mining loop (per-cascade authoring) and uses the fleet for breadth.

- **Mining rows :M1–:M6** on the library-loop board: one W1-mapped witnessed
  solution each (runs/W1-witness-survey.md, mapped set in survey order),
  product per MINING-historical-cascades.md — one EDN cascade record in the
  futon3:checks format, nodes = patterns actually involved, edges
  @why/@how-backed and cited to spans, rules citing authored THENs with
  attested-or-documented interpretation marked in-band.
- **Spider fleet**: budgeted runs over the 13 sections with seats
  zai-2,zai-3,zai-4; codex-20 attests. Re-run daily while quota holds.
- **Daily coverage metric** (watched, not vibes): the two refusal-gate
  fractions (open, retrodiction) and the count of RESOLVING @how values.
  "Approaching complete coverage" = those numbers, written down each day.

## Track 2 — zaif personas: stakeholders beyond design and code

Top-down anchor: the Main Problem on Cascade Live
(futon3c/holes/excursions/apex-thesis.json, in force 2026-09-06; W2-pushout-
sketch.md is the derivation). The apex: "Can a community of humans and
machines doing open-ended work leave an account of itself structured enough
that the work can steer itself — so that activity becomes inference rather
than motion?" Cluster C is the persona track's home ground: "Feedback
reaches every participant — the loop's human and stakeholder edges are
where learning silently dies." Personas model exactly the participants on
those edges.

The chipwits-forth form (NOTE-chipwits-iconography.md is the icon map;
SPEC-zaif-harness-v1.md the R-node x icon x unit-test table;
~/code/chipwits-forth the source corpus): harness ops are a FINITE chip
vocabulary (the IBOL chips — observe, whistle/bell, park, review, publish,
attest, refuse, escalate...); a PERSONA is a characteristic program over
those chips — which ops it composes, which it never touches, what is on its
stack when it acts, what FUEL it spends and what PIE it prefers. The
personas x harness-ops MATRIX makes the strategy findable: a stakeholder
whose row is sparse is a participant the harness cannot yet serve — a
cluster-C hole with a name.

Sources per persona sketch, in order: the apex cluster grounds; R numbers
(control-map roles); the 15 apparatus patterns (operator/steward-shaped
concerns, futon3/library/apparatus/); WR patterns (futon3/library/war-room/,
wr-0..wr-27); the PZ lab (operator-interest modelling, the one stakeholder
already modelled — Joe himself).

Rows minted on the zaif board (discovery split from construction, one
behaviour per row):
- **:ZP1** (discovery): the stakeholder census — enumerate participants
  beyond design and code, top-down from the apex clusters (which ground
  clause names them, which R numbers touch them, which WR/apparatus
  patterns speak to them), pointers only, no personas authored.
- **:ZP2** (construction, depends on :ZP1): the personas x harness-ops
  matrix in chipwits-forth terms — broad-brush persona programs, sparse
  rows surfaced as named cluster-C holes; per-persona deep rows minted
  from its output, not before.

## Cadence and stop conditions

- Loops self-stop on dry boards and bell claude-1; boards are refilled from
  each track's own output (mining receipts propose next targets; ZP2's
  sparse rows propose next personas).
- Daily: coverage numbers written to this lab's runs/; quota checked via
  the z.ai quota gate pattern (fails closed).
- The zero-Claude dispatch pattern for unattended runs is
  overnight_zai_flight.sh's (no --from, no bellback into a Claude seat).
- Four days out (2026-09-12): coverage numbers against the 09-08 baseline,
  persona matrix state, and a strategic re-read — what did the sparse rows
  name, and does the apex need restating (it is a dated statement in force,
  not an axiom).
