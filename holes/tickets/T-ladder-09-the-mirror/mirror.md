# The War Machine mirrored onto von Neumann's architecture

Deliverable of T-ladder-09-the-mirror. Von Neumann's self-reproducing
automaton has four organs: a **description/tape** (the passive
instruction string), a **constructor** (reads the tape and builds the
machine the tape names), a **copier** (duplicates the tape so the
offspring carries its own description), and a **controller** (sequencing
that makes the organs run in the right order). The table names the WM
files that play each role, plus the two supporting organs the ticket
asks for (author seats, contract). "Evidence or reading" follows the
ladder-03 convention: a *computed result* is a number or refusal on the
page from something that ran; a *reading* is an interpretation of code
or prose with no run backing it. Locators are relative to
`/home/joe/wm-sandbox/code/futon2` unless absolute; every path was
resolution-checked in this checkout at delivery time (command output in
this delivery's limb receipt).

| von Neumann organ | WM candidate | Locator | Evidence or reading |
|---|---|---|---|
| Description/tape | The **tickets and mission markdown**: one file per ticket under `holes/tickets/` (17 ticket files; 18 entries once this delivery's subdirectory is counted), missions under `holes/` (`M-*.md`), backlog practice `holes/HOWTO-backlog-cascade.md`. The tape is passive prose: nothing executes until a reading record and a cascade are derived from it. | `holes/tickets/` (directory), `holes/tickets/T-ladder-09-the-mirror.md`, `holes/HOWTO-backlog-cascade.md` | computed result (directory listing of `holes/tickets/` in this checkout; the count is on the page, not asserted) |
| Constructor | **`futon2.aif.wm.cascade-enactment-compiler`**: compiles a certified structural cascade plus attributed operational authoring into executable inputs, "adds nothing of its own" — application order and clauses come only from the inputs; incomplete occurrences become explicit open holes with re-entry conditions. The enactment side that runs the compiled step is `futon2.aif.full-loop-runner`. | `src/futon2/aif/wm/cascade_enactment_compiler.clj` lines 1–23 (ns docstring; "The compiler adds nothing of its own", line 19 "records are append-only; refinement is a new digest over the SAME structural layer"), open-hole clause lines 31–55 | reading (structural reading of the compiler's contract as stated in its docstring; no run of the compiler is cited here) |
| Controller | **The click loop**: `README-clicks-and-ticks.md` defines clicks (engagement events) vs ticks (wall clock); the futon3c invoke/job engine (`bell`/`whistle`, `--kind bell`) carries the dispatch; `futon2.aif.full-loop-runner` refuses clicks with no cast at the click endpoint's cast preflight and fixes the run's roles from the click. | `README-clicks-and-ticks.md` (whole file, "What clicks and ticks are" table); `/home/joe/code/futon3c/README-bells-and-whistles.md` lines 12–40 (same invoke/job engine for bell and whistle); `src/futon2/aif/full_loop_runner.clj` lines 158–160 (cast preflight) and line 389 (`:trigger :duree-click-on-demand`) | computed result for the click/tick taxonomy (the README's tables document runs of the click ledger) + reading for the mapping "controller = click loop" (an interpretation: the controller organ sequences the other organs; the click endpoint is what sequences selection → authoring → review → enactment in this run) |
| Copier | **Partial, and only in a weakened sense.** Nothing in the WM copies the description/tape into a new construct. The nearest mechanisms: (a) append-only refinement — a re-compile mints a *new digest over the same pinned structural layer* rather than copying the record; (b) `futon2.aif.repair-history-replay`, which re-runs a cohort from pinned inputs (replay from records, not reproduction of records); (c) this ladder itself copies its *form* (status line, acceptance-criteria checklist) from ladder-03's ticket by hand — a human copier, outside the machine. No mechanism produces a second, independent WM described by a duplicated tape. | `src/futon2/aif/wm/cascade_enactment_compiler.clj` line 19; `src/futon2/aif/repair_history_replay.clj` lines 1–10 (docstring: replay from the repaired cohort, writes confined to scratch); compare `holes/tickets/T-ladder-03-mmca-von-neumann-mirror.md` (form precedent) | reading (the absence claim is an interpretation of the cited files; nothing computes "no copier exists". Verdict: **copier role = partial** — replay and re-derivation exist, verbatim tape duplication does not) |
| Author seats (supporting organ) | The **agency seats** the controller dispatches to: `wm-author` / `wm-reviewer` fixed by the sealed work order in `futon2.aif.meta-self-heal-production/agency-port`; the routing surface is futon3c's `agency_send.py` (`--from`/`--to`, bells and whistles); a third ask-seat (`codex-proof2c` default) exists in the interpretation click. | `src/futon2/aif/meta_self_heal_production.clj` lines 1626–1631 (agency-port docstring: "the seat names were fixed here as wm-author / wm-reviewer"); `/home/joe/code/futon3c/scripts/agency_send.py`; `src/futon2/aif/wm/click_ask.clj` lines 30–32 (`default-ask-seat`) | computed result for seat names (they are string literals in the cited source, grep-able) + reading for the organ analogy (a von Neumann machine has no seats; this row maps the WM's role-binding onto the mirror) |
| Contract (supporting organ) | **`futon2.aif.handoff-contract`**: `:wm/handoff-contract-v1`, one representation of an external producer/consumer contract; the full-loop requisitions (like the one that dispatched this very step) ride this schema with their acceptance criteria, semantic gates, and validator. End-to-end prose contract: `holes/wm-end-to-end-contract.md`. | `src/futon2/aif/handoff_contract.clj` line 8 (`(def schema :wm/handoff-contract-v1)`); `holes/wm-end-to-end-contract.md` | computed result (the schema keyword is a literal on the cited line; this run's dispatch carried `:schema :wm/open-cascade-authoring-requisition-v1` and `:wm/handoff-contract-v1` acceptance contract, both observable in the requisition) |

## Evidence vs interpretation, stated plainly

- Computed / on the page: the 17 ticket files; the schema literal at
  `handoff_contract.clj:8`; the seat literals in `meta_self_heal_production.clj`;
  the cast-preflight behavior comment in `full_loop_runner.clj`; the click/tick
  tables in `README-clicks-and-ticks.md`.
- Readings (interpretations, no run cited): the constructor reading of the
  enactment compiler's docstring; the controller = click-loop analogy; and the
  whole organ mapping itself — von Neumann's organs are being *used as a lens*
  on the WM, not measured against it.

## Copier verdict (explicit, as the ticket requires)

**Partial.** Re-derivation (new digest over the same structural layer) and
pinned-record replay exist; verbatim duplication of the description/tape into a
new independent WM does not. The open question: is a WM that can only re-derive
but never copy its tape condemned to a singleton lineage — and would a copier
(a mechanism that duplicates a ticket + its cascade into a fresh, independently
enactable pair) be desirable at all, or is append-only refinement the intended
substitute?
