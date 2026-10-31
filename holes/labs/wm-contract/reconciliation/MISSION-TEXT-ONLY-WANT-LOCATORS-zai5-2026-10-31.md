# MISSION-TEXT-ONLY-WANT-LOCATORS — zai-5 discovery (2026-10-31)

Discovery only; no source edits, no click, no canonical store write. Case run:
`data/wm-runs/tick-run-record-2026-10-31-ebbbe42a-9889-4bff-921d-0843ea85a699.edn`
(record sha256 `04bb8e1b…`, per zai-6's matrix `MATRIX-A1-E4-ebbbe42a-2026-10-31.md`
@ `0b375d5d9`, branch `zai6/matrix-ebbbe42a`). Read against futon2 origin/main `7ce003f4c`.

## 1. Where locators are minted for selected wants, and why all four are C4 on the mission file

Two minting sites, both producing C4-over-the-mission-file, both at `:sha "HEAD"`:

- **Checkbox wants** (`futon2.aif.mission-hole-wants`): `current-checkboxes`
  (line ~100) lifts every `- [ ]` task line; `hole-locator` (lines 66–76) mints
  `{:class :C4 :repo … :sha "HEAD" :path <mission-file> :decl <closed-form>}` —
  the same line with its box ticked (`closed-form`, lines 58–62). The witness is
  *affirmation-shaped by design*: the observation is that a CHECKED item is
  present (ns docstring).
- **Criteria wants** (`futon2.aif.mission-criteria` `wants`, lines 121–145): a
  stated-verdict criterion gets `{:class :C4 :repo :sha "HEAD" :path :decl
  "<criterion text>**Met.**"}` — again over the mission file.

All four M-typed-memories wants are checkbox limbs (P1b/P2/P3/P4), so all four
locators are checkbox C4. Record pin: `:observation-locators` at offset ~121643
holds exactly four entries, every one `{:class :C4, :repo "futon3c", :sha "HEAD",
:path "holes/missions/M-typed-memories.md", :decl "- [x] P1b/P2/P3/P4 …"}`; the
record contains 68 `:class :C4` occurrences, one `:C3`, and **zero** `:C8/:C5/:C6`.

**Why nothing richer is ever minted for such a mission — two structural reasons:**

1. The ask channel that CAN propose richer locators
   (`futon2.aif.mission-reading`, LOCATOR reading: C3 path, C4 decl, C5 registry
   entry, C6 witness, **C8 registered passing run / named command**, prompt at
   lines 55–64) fires only for a *prose criterion with no stated verdict* — its
   own docstring: "A stated verdict always wins: a locator reading is asked only
   for a criterion with none."
2. `mission-source` (mission_hole_wants, lines ~111–118) **filters unlocated
   criteria out entirely once a checkbox surface exists** ("When one exists, do
   not let older prose criteria … make the whole mission unselectable: admit only
   criteria that themselves have a checkable locator"). So a mission that states
   its work as checkboxes never gets a locator reading asked at all — the C8
   demo/test route is unreachable for exactly the missions most likely to need it.

## 2. Other locator classes, and what a demo/test measurement would take

Classes with runtime decision procedures (`futon2.aif.observation-checks`,
locator-fields lines 41–44; consumers `completion_check` supports `#{:C3 :C4}`,
`cascade_problems` `#{:C3 :C4 :C5 :C6 :C8}`):

- **C3** path-exists at a pinned sha.
- **C4** declaration-head present in a file (the checkbox class).
- **C5** contract-registry bundle entry exists.
- **C6** EDN witness references an existing commit/entry.
- **C8** the test registry holds a passing run for a **named test namespace or a
  named command**, pinned to current content, postcheck matched, no failures
  (header comment lines 160–176: "C8 observes that such a record exists … and
  that its pinned bytes are still the bytes on disk. It does not rerun tests").
  A C8 locator may name evidence that does not exist yet — "it then reads false
  now, and producing that evidence is the flight's work" (mission_reading
  locator-request) — so binding a want to a C8 locator does not presuppose the test.

**What it would take for a want to be measured by its demo or a named test:**
the want's locator (or a second locator beside the checkbox) must be C8 —
`{:class :C8 :repo "futon3c" :namespace "futon3c.peripheral.memory-supersession-asof-test"}`
— proposed through the mission-reading LOCATOR ask and validated by
`mission-reading/checkable`. Two gaps block that today: checkbox wants bypass the
ask (§1), and C8 requires a *registered* run at current content (the ebbbe42a
demo replay — `clojure -M:test -n futon3c.peripheral.memory-supersession-asof-test
→ 1 test, 22 assertions, 0 failures`, retained in `:run-output` `:demo`
`{:pointer {:sha "4fd00fa63…" :repo "/home/joe/code/futon3c"}}` — was a
worktree run, not a registry entry, so even a C8 locator bound to that namespace
would read false until a warrant-registered run exists).

## 3. Across the last 20 runs: every reached want rests on C4 mission text alone

Method: structural scan of the last 20 `tick-run-record-2026-*.edn` (dated
terminal records only; `.requirements/.learning` sidecars excluded). 15 of 20
carry no `:mission-state` (refusals/abstains/close exceptions). The 5 that do:

| run | reached wants | after-locator instruments |
|---|---|---|
| 10-29-ff4b4d5c (M-peradam-grounding) | 1 (`h0f13c792f7e5`) | 5 measurements, all C4 over `futon7/holes/M-peradam-grounding.md` |
| 10-29-b1a049d7 (already-satisfied) | 0 | 0 measured |
| 10-30-3415b671 (M-peradam-grounding) | 1 (`hfbcd8a18ced5`) | 3 measurements, all C4 over the mission file |
| 10-31-4955b6e6 | 0 | 0 measured (wants never measured — the DUPLICATE row's case) |
| 10-31-ebbbe42a (M-typed-memories) | 1 (`ha4d7c19e5cdd`) | 4 measurements, all C4 over `holes/missions/M-typed-memories.md` |

**All 3 reached wants in the last 20 runs (100%) rest on a C4 `- [x]` limb of
the mission file alone; 12/12 total want measurements used C4-mission-file
locators; C3/C5/C6/C8 were never the instrument for a selected want.** In
ebbbe42a the run *also* held real executable evidence — the demo replay at the
grounded commit `4fd00fa63`, retained in `:run-output` — but
`selected_want_outcome.clj` (lines 48–86) consults only
`expected-locator`/`declared-locator`/`after-locator`; it never reads
`:run-output`, so the outcome for `ha4d7c19e5cdd` (`:after {:observed true
:measurement {:declared-locator C4 … :after-locator {:class :C4 … :sha
"4fd00fa63…" :decl "- [x] P2 demonstrates supersession…"}}}`, record
`:mission-state`) was decided by the checkbox at that sha with the demo as
unconsulted corroboration.

## 4. Would refusing checkbox-only wants strand mission work?

Yes. `mission-hole-wants`' whole design is that the checkbox is "an operational
closure surface" — the only closure form most missions state; with no checkbox
surface the fail-closed path makes assembly refuse the target outright
(`:tokens-without-checkable-locator`). Refusing checkbox-only measurements would
strand every mission that declares only checkboxes (currently: all of them — see
§3) and inverts Joe's 2026-09-24 rule (compute the check on the fly; the machine
should compute reasonable completion criteria for any open mission; never refuse
for a list the apparatus could compute). The owner's checkbox also stays the
*honest* authority for "the owner considers this done": the defect is not that
the checkbox is read, but that it is the *only* instrument and that richer
instruments the run itself produced go unconsulted.

## Smallest change that keeps a checkbox from counting alone

**Two-instrument rule at measurement, not at admission.** In
`selected_want_outcome`, a want whose declared locator is C4-over-a-mission-file
may not be recorded `reached` on the checkbox alone: the measurement must retain
a second instrument from the same run — the run's own `:run-output` demo receipt
at the grounded commit (already retained, already sha-pinned: `:demo {:pointer
{:sha …} :replay […]}`), or a C8 locator reading — and the outcome records
`:instrument :checkbox-only` vs `:corroborated` instead of a bare boolean.
Checkbox-only stays an *observation* (true/false, as now) and stays a want; only
the `reached` promotion needs the second instrument.

- **Effect on ebbbe42a-class runs:** P2 still reaches — its demo replay at
  `4fd00fa63` is retained in the same record; the run's grounded-progress
  survives, now with the instrument named.
- **Effect on selection:** missions whose checkboxes were flipped without any
  executable artifact stop promoting wants to reached, so those wants stay open
  and stay selectable — mission work is *not* stranded, it becomes re-targetable
  with an honest "checkbox-only, demo owed" status. No admission-side refusal is
  added, so no target becomes unselectable that is selectable today.
- **Complementary (not required for the row):** let the mission-reading LOCATOR
  ask fire for checkbox wants too (proposing C8 alongside the checkbox C4), and
  drop the `mission-source` filter that suppresses locator readings whenever a
  checkbox exists. That gives the class a durable second instrument, but it is a
  larger change than the measurement-side rule and can follow it.

## Separate answer: `:claim-matches? false` with `:author-response/decisions-mismatch`, grounded anyway

Pins (record): `:claim-matches? false, :author-response-defects [{:kind
:author-response/decisions-mismatch, :claimed-decisions {…}}]` inside
`:answer-delivery {:source :reviewed-patch-function, :patch {:namespace
futon2.aif.wm.authored.req-6a8fe374 … :commit "4560d631b…"}, :requisition-id
"6a8fe374…"}`.

Mechanism: `validate-product` (`wm/open_cascade_resume.clj:295–345`) compares
the requisition's *claimed* decisions against the decisions the reviewed patch
function actually returned, in the JSON value domain (`json-claim-matches?`).
A mismatch or non-operational claim is appended to `:author-response-defects`
on the delivery map — but the acceptance branch (`{:status :accepted …}`)
gates only on the returned product being complete, operational and routable;
the claim defect does not gate, and **nothing downstream consumes
`:author-response-defects`** (sole producer is `validate-product`; no other
reader in src). Grounding then proceeds on its own evidence chain — grounded
commit + demo replay + mission observation — none of which reads the author's
claim.

Assessment: the mechanism is *disclosure-correct and consequence-free*. The
author's claimed decisions differing from the deposited patch's actual decisions
is a C1 identity defect (the claim is not what the code does), honestly retained,
but currently with the same effect as if it had matched. Whether a claim/patch
decisions mismatch should block or merely be surfaced is a policy question for
the owner; the smallest honest step consistent with the disclosure already in
place is to require the reviewer-of-record to acknowledge the mismatch defect
(the C4 row of zai-6's matrix notes review evidence is thin this run: no
`:review-gate` map, and the mismatch "was not a review finding").

## Method note

Reads only: the ebbbe42a record, the last 20 dated terminal records, zai-6's
matrix commit `0b375d5d9`, and futon2 origin/main `7ce003f4c` source. Offsets
above are into the named record's bytes. No run-stream, index, ledger or store
write; no click; branch holds only this report.
