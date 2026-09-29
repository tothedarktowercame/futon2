# NOTE: target-field coverage — why 291 targets get no admitted universe (2026-09-29)

Fact-finding only. Evidence: futon2 run records, git history, and read-only
forms evaluated in the serving JVM (`scripts/proof-eval.sh`).

## 1. Where a target's fact universe comes from today

Three sources, merged before `cascade-problems/assemble`:

1. **Declared cascade sources.** `cascade-sources/load-declared` reads every
   file in `resources/wm/cascade-sources/*.edn`; `observe-facts` turns each
   file's `:facts`+`:locators` into the universe at
   `src/futon2/aif/cascade_sources.clj:284` (`(assoc-in [:universes t] universe)`).
   Today there are exactly 5 such files: M-aif-policy-conditioned-eig,
   M-expressions-of-interest, M-f11-find-production-successor,
   M-wm-08-external-f2, T-repair-occ-444fb018… — the only 5 targets with a
   declared universe (verified live: each carries 3–6 fact tokens).
2. **Mission-stated wants.** `mission-hole-wants/merge-into-sources`, wired
   into the tick at `scripts/futon2/report/war_machine.clj:6502-6508`,
   derives a universe (want tokens → `false`) plus C4 locators from each
   live mission's unchecked checkbox holes
   (`src/futon2/aif/mission_hole_wants.clj:70-96`, `:157`).
3. **Flight continuations.** `wm/construction-inputs.clj:19` merges the
   flight's universe for the flight's one target.

Why 291 get none — **both** failures apply:

- **No source exists for most missions at all.** Live read: 221 live
  missions, 593 retained open holes, but only 120 are `unchecked-task`
  (checkbox) holes in 20 missions; 473 holes are `work-marker` /
  `pending-lifecycle` / `open-section-item` with "no check can witness
  closure" and are never projected. 201 of 221 live missions have no
  derivable universe even on a good day.
- **The deriving path is currently switched off.** `merge-into-sources`
  only generates when the declared sources agree on exactly one schedule
  and one scales (`mission_hole_wants.clj:140-152`, "when the declared
  sources do not agree on exactly one, generate NOTHING"). The
  T-repair-occ-444fb018 source file declares **no `:c-schedule`** (grep: 0
  occurrences; the four M files declare `:placement :terminal`), so
  `load-declared` defaults it to `:every-step`, the declared sources
  disagree, `one-of` returns nil, and **zero** mission-derived universes
  are merged. The 09-29 run record's own
  `:mission-hole-coverage {:status :absent, :reason :coverage-not-recorded}`
  hides this; the 09-23 records show it directly:
  `:not-generated-reason :declared-sources-lack-one-agreed-sched…`,
  `:targets-added 0`.

## 2. What the selector picked from on 2026-09-19..23

The tick's target list was already the full substrate ∪ declared ∪
proposals ∪ ticket-queue (`git show 6cf508b6e:scripts/.../war_machine.clj`,
lines ~7060, same concat as today, present since 5d55e7a08 09-17). But the
selection domain was tiny:

- `tick-run-record-2026-09-21-1789964661.edn`: selected
  `M-aif-policy-conditioned-eig`, candidate `:C1` — a **declared** candidate
  from its cascade-source file; universe from the same declaration
  (`:n-in-domain 5`, `:refusals nil`). The many M- ids in the record are
  habit-prior/context echoes, not assembled problems.
- `tick-run-record-2026-09-23-1790136186.edn` (and 1790131591, 1790161992):
  outcome `grounded-change`, selected `T-repair-occ-444fb018…`, candidate
  `:C2` `aif/declare-the-conditioning` — again a **declared** candidate; its
  source file was published 09-22 (commit 22b1971c8).

So the old path **did** need `:facts`/universes; the targets that were
selected had them by declaration. Additionally, 09-21..22 records
(`2026-09-21-1790033693`, `2026-09-22-1790053967`) show
`:targets-added 17` — 17 mission-derived universes from
mission-hole-wants were live in the family then. Those runs still show 0
`universe-not-admitted` only because selection succeeded; the ~260
substrate refusals were simply not printed on selecting records. The last
selection was `2026-09-24-a4b4fc78` (M-aif-policy-conditioned-eig,
`:event :cascade-selected`).

## 3. Which commit turned selectable into universe-not-admitted

No single commit flipped the refusal on; a target without sources has been
refused `:universe-not-admitted` since the cascade-only tick landed
(33b1c77cc / 5d55e7a08, 2026-09-17). What actually ended clicking, in
sequence:

1. **22b1971c8 (09-22, "publish the repair-ticket cascade source where
   load-declared reads it")** — the published T-repair file omits
   `:c-schedule`, so declared schedules stopped agreeing and
   mission-hole-wants went from `:targets-added 17` (run 1790053967) to
   `:targets-added 0` (run 1790110142 onward). This is the one data change
   that silently shrank the derivable field to the 5 declared targets.
2. **The declared targets' work closed**: the 09-23 record already shows
   the EIG checkboxes as `- [x]` and the T-repair ticket as
   `**Status:** DONE`. With wants holding, 4 of the 5 declared targets
   refuse `:no-constructed-candidate` and M-expressions-of-interest
   refuses `:universe-not-admitted :locators` (tokens without checkable
   locators) — exactly the 09-29 record's remaining 5 refusals.

The listed 09-24..26 commits (5922c56e5, 891b4af6a, 7bd17dfb6, d23f88b96,
3ecf7a162, 7e6ae82de) widened construction and the target-field report;
they did not remove any universe. The abstentions 09-25..28 are flight runs
abstaining before selection (`:selection-not-reached`), carrying no
refusal list at all — the 292-refusal abstention shape first appears on
the full-tick records of 09-29.

## 4. Unfinished M/E/T vs the 296

Counted read-only in the serving JVM (`reg/load-missions`,
`reg/open-missions`, `reg/live-ticket?`, `reg/load-excursions`):

- live (unfinished) missions: **221**
- live tickets: **43**
- excursions: **385 total, 373 live** — but excursions are **not** in the
  decision's target list at all: `cascade-problems/substrate-targets`
  (`cascade_problems.clj:44-50`) enumerates open missions then live
  tickets only. (The target-field *report*, `target_field.clj`
  `considered`, does include excursions via the excursion enumerator — the
  decision and the report disagree.)

`substrate-targets` = 264. The 296 in the run record = those 264, plus the
declared-source target T-repair-occ-444fb018 (no longer a live ticket,
added via `(keys (:universes cascade-sources))`, war_machine.clj:6527),
plus cascade-proposal-supply and ticket-queue entries
(war_machine.clj:6527-6530).

## 5. What it would take for every unfinished M/E/T to be admitted

Both a deriving path and per-task declarations already exist; coverage
fails on three concrete gaps: (a) the schedule-agreement gate is currently
tripped by one undeclared `:c-schedule`; (b) only checkbox holes project —
473 of 593 holes have no mechanical closure witness; (c) excursions and
most tickets have no deriving path at all (only mission documents are read
by mission-hole-wants).

**Smallest change I would propose** (not made): declare
`:c-schedule {:placement {:value :terminal :status :declared} …}` in
`resources/wm/cascade-sources/T-repair-occ-444fb018.edn` — a one-file data
edit that restores schedule agreement and re-admits the ~20 checkbox-carrying
missions' universes (120 want tokens) on the next tick, with no code change.
Broader M/E/T coverage would need a deriving extension (excursion/ticket
reader in the style of `mission-source`, plus a locator class for
non-checkbox holes), which is a design question, not a fix.
