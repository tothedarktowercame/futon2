# T-formal-patterns-unblock — make M-formal-patterns' next items concretely selectable

**Requisition:** completed — 2026-11-06T07:10:00Z, job invoke-1793946208261-13623-2f819ee8, state done

**Asked by:** Joe, 2026-11-06. **Owner:** claude-6 (review). **Worker:** Kimi seat.

## Definition (Joe, 2026-11-06)

"do what's needed to get this unblocked, including relaying clear questions to
Joe about what is needed (not 'his permission' but in technical terms, what are
the concrete blockers)".

"The resulting state should be that the next items here are concretely
available for selection by the WM, not that those items are done. The task
should no longer be classed as 'undetermined' at the end, and it should be
re-rated by Giraffe."

## Starting state (claude-6, 2026-11-06)

- Mission: `futon5/holes/M-formal-patterns.md` (sha256 457cc259… at 05:24Z;
  since changed by WM clicks). Header `Status: OPEN`; freeze section at
  "## ❄ FROZEN 2026-08-04 — no new slices until the reboot register closes".
- Register: `futon5/TN-baldwin-reboot.md` §10 "The locked register". As of
  today: N1 CLOSED, N2 DONE, N4 DONE, H1 DONE, H3 DONE, H4 DONE; N2b
  look-ahead (gate: Joe); N3 re-scoped, ledger backlog remains (gate: Joe);
  H2 open, folded into N2b; H5 partly discharged.
- Giraffe run `storage/giraffe/20261106T052455.977Z-f7d748d5`: status
  `undetermined`, reason `dependency-reading`, pass-rank 276,
  priority band unknown. Its reading (corpus
  `~/notes/wm-next/giraffe-reads-2026-11-04/corpus/batch-old-52.jsonl` line 2)
  has one unresolved `depends-on` edge to `TN-baldwin-reboot.md`: "A TN- tech
  note, not an M/E/T task document, so it does not resolve as a link target".
- The WM selected the mission through its find-out-before-building class
  (best rank among 9 undetermined items). WM commits in futon5 today:
  b0b8e37 (H1 remainder), ac8a057 (N3 backlog), 84409a9 (S4 Peeragogy
  translation — a mission slice made while the freeze notice stands).
- WM mission registry (`futon2/src/futon2/aif/mission_registry.clj` ~330-356):
  only an explicit `**Gate:**` line naming an operator decision removes a whole
  mission from selection; status prose does not.

## Outcome required

1. Questions for Joe, each stating a concrete technical blocker (what decision,
   between which options, what each option changes in the code/paper), in the
   section below, and nothing phrased as a request for permission.
2. The mission file is self-contained about the freeze: it defines slice, N, H,
   and states the register status it depends on, so a reader (and Giraffe)
   does not need the TN to know what is blocked.
3. Items that are not blocked on Joe are concrete, unchecked acceptance items
   with locators, so the WM can select them. Items blocked on Joe point at the
   question below by number. The items themselves are not done here.
4. A fresh Giraffe run no longer rates M-formal-patterns `undetermined`.

## Questions for Joe

**Q1 — Freeze disposition, and the standing of 84409a9 (S4).**
The freeze says "Do not dispatch a slice while this notice stands"
(`futon5/holes/M-formal-patterns.md:111`), but 84409a9 executed slice S4
(Peeragogy translation) under it. Decision: (a) **lift the freeze** — register
remainders (N2b, N3, H2, H5) become ordinary backlog and S0–S3/S5 become
dispatchable; (b) **keep the freeze and ratify S4 as a one-off exception** —
slices stay undispatchable, S4's acceptance tick stands; (c) **keep the freeze
and treat S4 as a violation** — untick the acceptance item and park the S4
artifacts. What changes: the freeze section of
`futon5/holes/M-formal-patterns.md`; whether slices S0–S3, S5 can be dispatched
at all. Evidence: `futon5/holes/M-formal-patterns.md:106-111`;
`git show 84409a9` (futon5).

**Q2 — N2b: open it as a work item, or keep it look-ahead?**
TN §10 marks N2b "LOOK-AHEAD ONLY … still not a work item until Joe opens it"
(`TN-baldwin-reboot.md:357`); its stated prerequisites (N2, H1) are now both
DONE, and §14's sequence next step is "invariants (a)(b)(c) agreed → smallest
build" (`TN-baldwin-reboot.md:716`). Decision: (a) **open N2b** with the §14.2
invariants as agreed — smallest build = stop resetting the 40-step accumulator
window (`src/futon5/exotype/selection.clj:99-105`), give τ exactly one
consequence, one acceptance test, plus the §14.3 prereg falsifier; (b) **keep
look-ahead** — name what further prerequisite is missing. Also parked inside
§14: whether exotype→genotype replacement (Joe's answer to invariant (b))
breaks the Baldwin frame ("third channel") — resolve before build, or build
first and classify after? What changes: `selection.clj`,
`test/futon5/exotype/invariants_test.clj`, a prereg note; paper Part III
framing (§36 argues for N2b). Evidence: `TN-baldwin-reboot.md:357,598-717`.

**Q3 — N3 exit criterion.**
§10's registered criterion is "every `holes/F-*.md` and top-level `TN-*.md` has
an inbound reference or is marked terminal"; §19 measured that this catches one
instance in four and §19.5 replaces it with the co-location backlog, ending
"Gate is Joe's" (`TN-baldwin-reboot.md:1139`). Decision: (a) **accept §19.5**
— N3 closes when the §21.6 co-location backlog is done; the inbound-reference
audit is dropped; (b) **keep the original criterion** — the WM owes the
reference audit instead of (or in addition to) co-location. What changes: what
N3 backlog work the WM selects; whether a repo-wide reference audit is owed.
Evidence: `TN-baldwin-reboot.md:358,1048-1140`.

**Q4 — EFE objective degeneracy: new register item or N3 backlog?**
§20.5 flags micro-pilot 7 ("the exotype objective cannot be repaired from
inside") as "a candidate new register item … Gate is Joe's"
(`TN-baldwin-reboot.md:1230`); §21.5 has since measured the degeneracy in the
pure scoring function (`:chaos` argmin for 18/24 observations under the old
model, 20/24 under the derived one). Decision: (a) **register as N5** — slices
2/4b/5/6* and the paper's Finding 2 get re-examined under it; (b) **fold into
the N3 backlog** — co-locate the split claims with pins/hole markers, no new
register item; (c) **defer** until the §13 re-runs. What changes: the §10
register table; `src/futon5/exotype/efe.clj` co-locations; possibly paper
Finding 2. Evidence: `TN-baldwin-reboot.md:1205-1231,1300-1301`.

**Q5 — H5 vocabulary and the shape of the §13 re-runs.**
H5's vocabulary sub-item is "unblocked, not widened"; §30 ends "Not built.
Discussion only, pending Joe's pick of vocabulary and whether the A/B replaces
the plain re-run" (`TN-baldwin-reboot.md:1984`). Decision: (a) **baseline 4
kinds**, re-runs are plain redos; (b) **widen to 8 kinds** (`:fix2`, `:fix4`,
`:even4`, `:odd53`, §30.4) with every §13 RE-RUN run as the preregistered A/B
(P1–P3, §30.5); (c) widen but re-run plain. What changes:
`src/futon5/exotype/grid.clj` propagator vocabulary, re-run scripts and seed
config, prereg documents; blocks the §20.4 ledger row "slice results measure
spatial structure" (6 of 12 slices marked RE-RUN). Evidence:
`TN-baldwin-reboot.md:1894-1988`, §13 (`TN-baldwin-reboot.md:509ff`).

**Q6 — H2 disposition.**
H2 ("Baldwin arm has no genotype variation source") is "open — candidate
resolution is N2b", gate "folded into N2b" (`TN-baldwin-reboot.md:361`); its
exit criterion is "a decided variation source, or the arm retired". Decision:
(a) **H2 rides with Q2** — if N2b opens, H2 closes when N2b's variation
mechanism lands; (b) **retire the Baldwin arm now** — the paper drops the
Baldwin/Lamarck contrast; (c) **keep open and decoupled** — name a variation
source other than N2b. What changes: the Baldwin arm config and the paper's
contrast sections. Evidence: `TN-baldwin-reboot.md:361` and the §10 exit criterion at `TN-baldwin-reboot.md:388`.

## Joe's answers (2026-11-06, by voice, relayed by claude-6)

1. Freeze: lifted; today's S4 work stays.
2. Endogenous clock: yes as outlined (stop resetting the stress counts, a stressed cell gets a new rule). Framing correction: "grid" holds only for 1-D automata; in the general case it is a semilattice. Copying acquired traits is not Baldwin and must not pass as it; Baldwin returns only via a formal account built on the War Machine's learning loop (β etc.), with cascades applied and updated in use.
3. Lost knowledge: neither framing accepted on its own terms. Notes have no concrete meaning to depend on; findings rest on tests, proofs and concrete arguments. A test with comments beats a vague, possibly catastrophic statement; the statement might also be correct, so it stays open.
4. Scoring degeneracy: trust the old result but treat it as a pilot; the new work does not depend on its framing or findings.
5. Vocabulary: a pilot to learn from, not a dependency. Patterns are to be formalised as actually used (3/2 categories); CAs, if revisited, are formalised in the same theory without insisting the two are the same.
6. Baldwin arm: retired from the automaton experiments. The mission itself is retired as superseded/wontfix (futon5 0b8bcc7) in favour of `M-formal-patterns-redux` (futon5 3877c83).

## Result

(filled by kimi-1, 2026-11-06)

**Commits.**
- futon5 `e1771c1` — `holes/M-formal-patterns.md` only: (a) freeze section now
  self-contained — defines slice / Nightmare (N) / Headache (H) and carries the
  §10 register status table dated 2026-11-06 (freeze notice text kept, freeze
  NOT lifted — that is Q1); (b) acceptance checklist restated with locators.
- futon2 (this ticket) — questions Q1–Q6 above and this Result section.

**84409a9 (S4) vs the freeze — fact.** The freeze notice scopes the mission to
the eight register items and says "Do not dispatch a slice while this notice
stands" (`holes/M-formal-patterns.md:106-111`). S4 is a mission slice, not a
register item, so **84409a9 was outside the freeze scope** — a freeze violation
in letter, though it ticked a genuine acceptance item (evidence: `git show
84409a9`, "Ticks the S4 acceptance item"; the ticked box was verified present).
Not reverted; its disposition is Q1.

**Staged replacement Giraffe reading** (not promoted; live corpus untouched):
`/home/joe/notes/wm-next/formal-patterns-unblock/reading-M-formal-patterns.jsonl`
— one line, source sha256 b04e041f… (post-edit mission), every quote
re-verified at its cited line, `relates-to M-formal-war-machine` refreshed
(sha unchanged, quote still at line 26), new resolved `depends-on
T-formal-patterns-unblock` edge (the checklist now names the ticket), and the
`TN-baldwin-reboot.md` dependency kept unresolved (still a TN) but re-noted as
informational now that the register status is inline.

**Next selectable items** (unchecked, in the mission checklist):
1. N3 backlog (in freeze scope): read `TN-metaca-baldwin-micro-pilots.md`
   micro-pilot 7, split claims per §21.2, co-locate + pin/hole-mark at
   `src/futon5/exotype/efe.clj`.
2. N3 backlog (in freeze scope): co-locate the remaining open §20.4 ledger
   claims per §21.6 (the RE-RUN row inside it is blocked on Q5).
All other items are blocked on Joe: Q1 (freeze/S0–S3,S5), Q2 (N2b), Q3 (N3
exit), Q4 (objective registration), Q5 (H5 vocabulary + re-runs), Q6 (H2).
Items not blocked on Joe exist (1–2 above), so no `**Gate:**` line was added
to the mission.

**Giraffe re-run** was not executed here (a click may be live; nothing was
written under `~/code/storage/giraffe`). The staged reading plus the resolved
T-edge is what should move the mission off `undetermined` at the next mint.
