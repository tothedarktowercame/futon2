# Finding: open repair findings pre-empt ordinary cascade selection

Status: read-only discovery.  This note proposes no remedy and authorizes no
click, reload, queue mutation, or production change.

Observed source HEAD: `17d1e92f45637f9a5e1e4d9ea42dbba41bdac383`.

## Source and record pins

| Object | Pin |
|---|---|
| `src/futon2/aif/full_loop_runner.clj` | SHA-256 `63090729a8594fa47851b2603b32725a1a10a120a443448b4f43432f023373f0` |
| `src/futon2/aif/repair_obligation.clj` | SHA-256 `526b50670d61f554e7e69f960c7b4a34e6c0d0bf7af878ab30ab0f7f09a0352c` |
| `data/wm-runs/tick-run-record-2026-09-19-1789835454.edn` | SHA-256 `ef261e684e38b2f56f2da53b1b99e458715814bef563521fe11ae3e12cde5fd3` |
| `futon3c/data/wm-click-run-bindings/click-run-binding-wm-click-599b9255-1653-4267-9874-d93a9424a2c7.edn` | SHA-256 `d7fce9d0ac523c4708c40fbfbbb709183e34205bf452e65cad98b1b057c11572` |

All line references below are to the pinned futon2 HEAD.  Data observations
were made on 2026-09-19 UTC from existing files only.

## 1. Branch point and provenance

The runner reads the durable repair store before selection through
`repair/open-obligations` (`full_loop_runner.clj:3734-3736`).  It chooses the
first record whose status is exactly `:open` and whose class is not
`:environmental-hold` (`:3737-3740`).  It still computes the ordinary War
Machine judgment (`:3778-3801`), but an extant stop line changes the enacted
entry from `ordinary-entry` to `repair-entry` (`:3810-3812`).  It then suppresses
discrimination (`:3850-3865`) and deliberately omits `:controller-decision`
from the selection cell (`:3866-3871`).  The retained reason is instead
`:source :stop-the-line` (`:3882-3886`).  This is a pre-emption, not a cascade
candidate winning on its posterior.

The actual repair-entry branch was introduced by Joe's commit
`cf7e5389f6b15b6f30704f15266e179fc176759f`, **Make full-loop failures
self-healing and reviewable** (2026-07-16).  Earlier commit
`3227e8f07a1d67c071554a5982f0a9f662ea37ae`, **Carry accumulated stop-line
findings into repair runs** (2026-07-14), introduced the store read and
accumulation.  Commit `64561ef02582c20e72f0b0e90700ef4f2dcad38b`, **Preserve
stop-line and transport precedence** (2026-08-20), later made the precedence
explicit by bypassing the optional judgment transform whenever a stop line
exists; its surviving comment is at `full_loop_runner.clj:3793-3798`.

Authorization finding: the implementation commits are authored by Joseph
Corneli, so it would be false to describe the code as anonymously introduced.
However, neither the introducing commit messages nor the source comment cites
a ruling, planning Act, owner, bounded queue policy, or authorization for
ordinary clicks to be indefinitely diverted.  Repository search found later
descriptions of the precedence (for example
`holes/labs/wm-contract/TN-stop-line-run-record-2026-09-12.md:12,52` and
`holes/problems/BUILD-packets/CONTINUOUS-WM-2026-09-11.md:7`), but those are
retrospective descriptions/build material, not an authority cited by the
branch.  Thus explicit cited authorization is **not located**; this finding
does not infer that none exists outside the searched repository/history.

## 2. Durable queue and whether it drains

The store root is the constant
`/home/joe/code/futon2/data/wm-repair-obligations`
(`repair_obligation.clj:21`).  Review failures and system failures mint
immutable finding files with `:repair/status :open` under `findings/`
(`:413-444`, `:446-480`).  `open-obligations` reads findings and removes only
ids having a file under `resolutions/`; implementation or verification files
merely change the returned status to `:awaiting-validation`
(`:794-816`).  The runner consumes only the first eligible `:open` record
(`full_loop_runner.clj:3734-3740`), so this is ordered by `:opened-at`, not by
an owning planning lane.

There is drain machinery, but it is conditional rather than automatic.
`record-implementation!` writes an implementation and says explicitly that it
does not clear the line (`repair_obligation.clj:818-888`).  `resolve!` requires
approved grounded evidence plus, for machine/review failures, an existing
implementation and a distinct production-shaped validation; only then does it
write a resolution file (`:917-970`).  `open-obligations` subsequently excludes
that id (`:797-801`).  Therefore the queue is reachable/drainable in code, but
ordinary successful repair implementation alone does not drain it, and no
separate scheduler/owner was found that drains all entries independently of
future full-loop attempts.

At observation time the exact runner-eligible queue had **20** entries (the
same predicate as `full_loop_runner.clj:3737-3740`).  Opened times provide the
unambiguous age basis:

| opened-at UTC | repair id |
|---|---|
| 2026-09-14 23:06 | `repair-ea1-a7a5fc7c81ad45251922d33718170eba32a100cda770df0af9bcd759e28df913--attempt-001-artifact-binding-mismatch` |
| 2026-09-14 23:12 | `repair-ea1-b28b40fe3c109454107faa2309cfcf0e51abf79e39c436fda76a2d7d665f1913--attempt-001-artifact-binding-mismatch` |
| 2026-09-14 23:53 | `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-close-exception` |
| 2026-09-14 23:53 | `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged` |
| 2026-09-15 00:10 | `repair-initialization-56485d40-2f54-4882-8917-15a59a473494-initialization-failed` |
| 2026-09-15 00:24 | `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-002-build-failed` |
| 2026-09-15 00:52 | `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-003-build-failed` |
| 2026-09-15 01:21 | `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-001-artifact-binding-mismatch` |
| 2026-09-15 02:18 | `repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn` |
| 2026-09-15 02:51 | `repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-001-build-failed` |
| 2026-09-15 03:22 | `repair-ea1-dd81768f87a081e5f36b45c302421bae20a283f07243dff7ce9fc53e9f099df2--attempt-002-build-failed` |
| 2026-09-15 04:14 | `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-001-build-failed` |
| 2026-09-18 22:09 | `repair-ea1-51a783ff43ce1fb432b0928b2f142d3f07450872ba4b96c005f50d1b29ddcddc--attempt-002-untyped-failure` |
| 2026-09-19 00:00 | `repair-initialization-8ceaae14-d8b5-4ceb-a8bf-ce9a4d4eed6a-tripwire-tripped` |
| 2026-09-19 00:54 | `repair-initialization-82224aab-86f0-40eb-bede-b377b4216671-tripwire-tripped` |
| 2026-09-19 01:09 | `repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-001-build-failed` |
| 2026-09-19 01:16 | `repair-initialization-4e120541-560a-4c43-b0b4-dabb95a5e7fd-tripwire-tripped` |
| 2026-09-19 01:24 | `repair-initialization-d31f5b22-a76d-4431-9c1f-6f0300d34a2f-tripwire-tripped` |
| 2026-09-19 14:24 | `repair-ea1-4e68d87bb2cfd67bd81841528e073bf8b73eb890903618a08da4de51c7a0e564--attempt-002-build-failed` |
| 2026-09-19 14:30 | `repair-ea1-b82bec4361658ac9fea3bf2acf2b155ee690554056cb43982b320c2554a19bb7--attempt-001-build-failed` |

The first twelve were approximately four to three days old on 2026-09-19;
the remaining eight were less than a day old.  Environmental holds and the
many `:awaiting-validation` records returned by `open-obligations` are not in
this count because the runner's branch predicate excludes them.

## 3. Existing-run measurement

For a bounded recent window, I examined every `data/wm-runs/tick-run-record-*`
whose filename is dated 2026-09-18 or 2026-09-19: **9 records**.  Five record
`:terminal/:enacted-action/:type :repair-machine-failure`; zero record a
`:decision/:selection-law/:applied` cascade law.  The other four terminate
before an enacted action (two tripwire, one cohort-complete, one older sparse
record).  Thus among recent records that actually retain an enacted action,
the measurement is **5 repair branch, 0 ordinary cascade selection**; among
all nine it is **5 repair, 0 cascade, 4 pre-selection/other**.

The five repair records are:

- `data/wm-runs/tick-run-record-2026-09-19-1789780157.edn`
- `data/wm-runs/tick-run-record-2026-09-19-1789827859.edn`
- `data/wm-runs/tick-run-record-2026-09-19-1789828199.edn`
- `data/wm-runs/tick-run-record-2026-09-19-1789828750.edn`
- `data/wm-runs/tick-run-record-2026-09-19-1789835454.edn`

The four other records are
`tick-run-record-2026-09-18-5bf0bbfb-df6c-43e6-a126-2068d49b6969.edn`,
`tick-run-record-2026-09-18-r12fix-1789773891.edn`,
`tick-run-record-2026-09-19-1789779250.edn`, and
`tick-run-record-2026-09-19-postfix-1789776011.edn` under `data/wm-runs/`.

The morning/ordinary-click evidence is the last repair record above.  Its
binding file names click
`wm-click-599b9255-1653-4267-9874-d93a9424a2c7` and that exact run-record
path.  The run record retains `:repair-machine-failure`, route
`STOP_LINE -> FULL_LOOP_CLOSE`, no selection law, and
`:g-term-decomposition {:schema :wm/g-term-decomposition-v1, :status :missing,
:reason :no-recorded-cascade-selection, :policies []}`.  That fallback is the
specified behavior of `g_term_decomposition.clj:54-63`, not evidence of a
cascade census.  The ordinary-click consumption ledger contains the same
click id and issue instant `2026-09-19T16:30:54.404878711Z`.

## 4. Coded exit condition

The immediate branch condition is not “all repair-related records gone.”  It
is narrower and exact: **there is no record returned by `open-obligations`
whose effective status is `:open` and whose class is not
`:environmental-hold`** (`full_loop_runner.clj:3734-3740`).  When that filter
returns nil, `entry` becomes `ordinary-entry` (`:3810-3812`), discrimination
runs (`:3864-3865`), and the controller decision is retained (`:3870-3871`).

An implementation or verification moves a finding to
`:awaiting-validation`, which also makes it cease pre-empting immediately,
even though it remains unresolved (`repair_obligation.clj:806-814`).  A true
resolution file removes it from `open-obligations` entirely (`:797-801`).
Environmental holds never pre-empt.  Therefore the ordinary path is reachable
in code, but with the observed 20 eligible open findings it remains blocked
until each is either implemented/verified, resolved, or otherwise gains a
non-`:open` effective status through an authorized store transition.  The
runner processes only the oldest eligible record per attempt; failures can
mint additional open findings (`record-review-failure!` and
`record-system-failure!` above), so the coded mechanism does not guarantee
convergence or queue exhaustion.

## Conclusion

This is a real, source-controlled stop-line pre-emption with a reachable exit,
not an accidental nil.  It is also presently a global head-of-line gate in
front of cascade selection.  Existing run records show the practical result:
no recent enacted-action record exercised ordinary cascade selection.  No
remedy, owner assignment, or change of safety semantics is implied by this
finding; those require a separate decision.
