# C557 — F12 slice 21: the drifted citations re-pointed, and a run record that did not reproduce

**Slice 21 of `:F12`** (`:loop-mode :one-slice-per-invocation`). The slice C556
named as next: re-point the citations that slice 19's reflow left pointing at the
wrong lines of `runs/F12-organise/11-d2-denominator.edn`, and census whether any
other `runs/F12-organise/` artifact was reflowed the same way.

No ruling is taken. No machine was run. `mathlib4/DarkTower/WarMachine/Holes.lean:861`
is untouched and is still neither discharged nor amended. Nothing here bears on
D1, D2, D3, the third origin, the O3 field or O4 itself.

## 0. Two writers on one row, and what it costs the reader

This slice was written by `codex-2` under job
`invoke-1788765420094-13576-0eb73106` (caller `wm-build-work`, started
2026-09-07T07:17:00Z), dispatched by the previous loop invocation. That
invocation was cut off before the job landed, and the loop handed the same row
to a fresh invocation at 07:27:10 while the job was **still running**. Between
about 07:29 and 07:39 the second invocation edited `f12_repoint.clj` and ran
`git checkout --` on the three citing files twice, inside codex-2's window;
codex-2 overwrote those edits at 07:39:16 and committed at 07:56.

The consequence is recorded rather than smoothed over: **no working-tree state
during that window is evidence of anything**, and section 2's finding is a
direct product of the interleaving. The two Agency commits are `075e127c`
(probe + artifact) and `41c2a221` (the citation repairs); the review fix is
`e61e6d5b`. Nothing was lost — but a bell without a live poller on the caller's
side is what let a second writer onto the row.

## 1. What was re-pointed

`f12_repoint.clj` maps each citation by **key path**, not by line arithmetic.
It parses both revisions of the artifact into (path, span) nodes, finds the
maximal nodes the old span contained, refines each down to the deepest
descendant whose value is unchanged, and reads the new span off the same paths
in the current file. A blind line-diff remap was rejected on measurement, not
taste: C556 recorded that difflib over stripped lines remaps only 49 of the 66
registry citations to a span whose content matches (`worklist.edn:1385`,
`:F12` `:progress`, slice 20's entry).

Scoped to the citations that existed **before** the reflow (`git show
3c0ad833^`), there are 91: 89 re-pointed, 2 already correct, 0 non-contiguous,
0 needing a hand check (`runs/F12-organise/12-repoint.edn:2`,
`runs/F12-organise/12-repoint.edn:1920`). The 6 full-path citations written
after the reflow — 5 in C555 and 1 in C556 — were left alone
(`runs/F12-organise/12-repoint.edn:1961-1967`).

**C556's table is wrong in two cells, and this is the correction with the
command.** C556 published 66/66 for `aif-equations.edn` and 30/12 for
`worklist.edn`. Recomputed by `git show 3c0ad833^:<file>` and counting
full-path citations of the artifact: `aif-equations.edn` has **65**, not 66,
and all 65 drift; `worklist.edn` has **13** pre-reflow citations of which 12
drift, and C556's 30 is that 13 plus the 17 citations slice 20 wrote after the
reflow. `C554-F12-d2-denominator.md`'s 13/12 stands. Both tables are kept side
by side in the record (`runs/F12-organise/12-repoint.edn:8-17` published,
`runs/F12-organise/12-repoint.edn:1968-1977` recomputed,
`runs/F12-organise/12-repoint.edn:3-7` the disagreement).

## 2. The review found two things, and both are fixed here

**(a) Fifteen bare `:N` citations of the same artifact were left stale.** A
citation written as `` `:2909` `` after a full-path citation of the artifact
resolves against that antecedent and drifted exactly as the full-path ones did.
The full-path repair did not touch them, and the run record said
`:reason :out-of-scope-census-only`. That produced a sentence citing both forms
with different answers: `C554-F12-d2-denominator.md:55` read a re-pointed full
path beside an un-re-pointed shorthand inside one parenthesis. There are 11 in
`C554-F12-d2-denominator.md` (lines 10, 11, 55, 118, 141, 143, 145, 147, 150),
4 in `worklist.edn`'s `:F12` `:progress` (`:1148 :3014 :3485 :3537`), and 0 in
`aif-equations.edn`. All 15 are pre-reflow: the literal sequence in `git show
3c0ad833^` of each file equals the sequence at HEAD, in order. All 15 are now
re-pointed by the same key-path route
(`runs/F12-organise/12-repoint.edn:6860-7186`).

Nine of the ten distinct remaps put the cited line **byte-identical** at its new
number (`:1148`→`:1880`, `:3014`→`:4899`, `:3485`→`:6105`, `:1071-1075`→`:1214-1218`,
`:2909`→`:4791`, `:2303`→`:3767`, `:2842`→`:4724`, `:2299`→`:3763`,
`:1149`→`:1881`). The tenth, `:3537`→`:6157`, differs by one character
(` :problems []}` → ` :problems [],`) because slice 19 appended
`:split-denominator` after it: the node is the same, the delimiter is not — which
is the case a line-text comparison would call a miss and a key-path map calls a
hit.

**(b) The committed run record did not reproduce from the committed tree.**
Re-running the committed script against the committed tree gave sha256
`393978cd2a48e7366cedeaa448fda4ba50cf16dcf081e884a0da5312fef0e9ae` where the
committed artifact was `ef27a59a00ac437368dd31526805a58377739eef4cf850f9523a44b458dc9d22`.
Cause: the shorthand audit derived its candidate set from the **working file**,
and `worklist.edn`'s four shorthands were repaired and then reverted between the
run that wrote the artifact and the commit — so the record described a tree
state that was never committed, and its own account of that file
(`:pre-reflow-and-current-agree? false`, no audits) was a scan refusal that
nothing failed on. This is the same shape as the defect the slice exists to
repair, one level up: an artifact that passes every gate and does not say what
the tree says.

Fixed by making the candidate set **basis-only** — it comes from the pre-reflow
file and the two artifact revisions, so it does not depend on whether the repair
has already run. `:state` is the one field that reads the tree, and it reports
which of the two literal sequences is present: `:pre-repair`, `:repaired`,
`:no-candidates`, or `:unrecognised`. `:unrecognised` — something other than
this repair moved a shorthand — fails closed into `:needs-hand-check`, which
exits non-zero.

## 3. The census C556 asked for

`f12_reflow_census.bb` compares every `runs/F12-organise/` artifact's bytes at
HEAD against its bytes at the commit that introduced it, rather than reading
`git log -- <path>` (which history simplification can shorten). Of the 11
artifacts that predate this slice, **all 11 are byte-identical since
introduction**; `11-d2-denominator.edn` is the only one that changed, at
`3c0ad833`, and it is the reflow already known
(`runs/F12-organise/13-reflow-census.edn:119-130`,
`runs/F12-organise/13-reflow-census.edn:116-118`). `12-repoint.edn` shows
`:values-changed` because this slice's review fix rewrote it; that is this
slice's own edit and not a drift finding.

Four controls, each read back: identity (byte-identical), a reflow plant on
`07-o3-field.edn` (786 → 993 lines, detected `:reflow-only`), a value plant on
the same file (detected `:values-changed`), and the known reflow detected as
`:reflow-plus-additions` (`runs/F12-organise/13-reflow-census.edn:132-157`).

**Not claimed:** byte-identical since introduction means no citation of that
file *can* have drifted from a reflow; it does not mean those citations were
ever right. A pointer miscopied at writing time is invisible to this census
(`runs/F12-organise/13-reflow-census.edn:112-115`).

## 4. Controls on the re-pointing itself

Six, each a plant read back (`runs/F12-organise/12-repoint.edn:1921-1960`):
a value present only in the new artifact has no old counterpart and lands in
`:needs-hand-check`; corrupting a mapped value moves `:repointed` →
`:needs-hand-check`; a post-reflow citation is classified
`:written-after-reflow` and not re-pointed; the old artifact as **both** inputs
yields `:already-correct` with 0 edits; removing a key path moves `:repointed` →
`:needs-hand-check`; and a planted shorthand sequence that matches neither the
pre-reflow nor the repaired one yields `:unrecognised`, with the other three
states checked in the same row.

`:non-artifact-shorthand-unchanged?` is true for all three files: the bare
citations whose antecedent is some other file (`f12_d2_denominator_check.clj`,
`f12_o4_reachability.clj`, `futon3c:scripts/zaif_cascade_gate.clj`) are
untouched (`runs/F12-organise/12-repoint.edn:1978-1990`).

## 5. What this slice does not claim

The re-pointing is verified **per citation against the artifact**, not against
the sentence that cites it: each new span holds the same parsed value the old
span held, which is a stronger check than `pointer_check.bb` makes and a weaker
one than reading every sentence. C556's own three re-pointed citations and the
21 it added by hand are not re-derived here. Whether artifacts outside
`runs/F12-organise/` were reflowed the same way is not answered — the census
population is the one C556 named.

**The gate that would have caught this still does not exist.**
`pointer_check.bb` checks that a span resolves, not that it still says what the
citing sentence claims; all 90 wrong pointers passed it, and 3140 pointers
resolve today with the same blindness. Building a content-aware pointer gate is
not this slice's, and is not proposed here as a ruling.

## 6. Gates

- Two consecutive runs of `f12_repoint.clj` on the committed tree are
  byte-identical, sha256
  `9350e7a5e66741679db905c0d4d0f795fb1b55e37262eb3e0a03aaec39544c2c`; exit 0,
  `:needs-hand-check []`.
- `clj-kondo` 0 errors 0 warnings on `f12_repoint.clj` and `f12_reflow_census.bb`.
- `check-parens` OK on both.
- `negative_controls.sh` PASS (133 negative, 53 positive; shared registries untouched).
- `pointer_check.bb` 3140 pointers in 6 files, 0 unresolved.
- `worklist_check.bb` 183 items OK.
- No Lean touched, so no `lake build`. No machine run, so no run-lock. No
  `gen_aif_dag.bb` (TN §9a).
- This note's own pointers are **not** covered by `pointer_check.bb`, which
  scans the six registries and not the notes; they were verified by hand against
  the files.
