# Drift archaeology: commissioned R-node build to wm-contract closure loop

**Date:** 2026-09-05  
**Scope:** discovery account; no plan, registry, source, or lab artifact changes.

## 1. Commissioning texts

Joe commissioned Structure A on 2026-08-30: “line up a build process whereby we
push out the R numbers to associated agents and get them to help us build the
entire compliance system, in parallel as much as possible, with overlap points
moderated by the specification” and “bell out a tech-lead role to an Opus agent”
(`holes/problems/BUILD-tech-lead-charter.md:3`). Joe also directed the team to
coordinate “around interfaces using these tetrahedral model ideas, not just
working in parallel” (`holes/problems/BUILD-tech-lead-charter.md:53`).

Claude-15 translated that commission into the R5 spine on 2026-08-30 and quoted
Joe: “start there and see if we can't build a validated R5 and build everything
else around that” (`holes/problems/P-validated-R5.md:12`). Claude-15 gave the
record an `operator-acceptance` gate (`holes/problems/P-validated-R5.md:3`) and
derived this definition order: **Outcome → C → Cascade → Policy → Q(o|pi) →
G(pi) → coverage** (`holes/problems/P-validated-R5.md:90`). Commit `e01dab97`
(Joseph Corneli, 2026-08-30 15:08Z) anchored the charter, R5 spine, and nine
problem records after claude-20 found that the operative records lacked git
anchors (`holes/problems/BUILD-ledger.md:159`).

Joe supplied the exact broader clause again on 2026-09-05: “I clearly stated
that all nodes should be aligned with AIF formalism (terms and expressions) and
built in dependency order... that was the contract, nothing more or less.”
Claude-1 recorded it in commit `b649e29d` at
`holes/labs/wm-contract/EPIC-run-era.md:564`. I did not establish an earlier
verbatim occurrence of that full sentence. I searched the charter, all
`holes/problems/`, `holes/labs/wm-contract/`, futon3 war bulletins, the archived
claude-13 turns, and the claude-15 buffer; the nearest ancestors are Joe's R5
spine instruction above, the charter's interface instruction, and the explicit
R5 order.

## 2. Structure A: dispatch, closure, and stranding

Claude-20 acted as tech lead. On 2026-08-30 at 14:50Z, commit `4c0ecc84`
recorded four dispatches: CML-D1, R9-D1, R2-D1, and R8-D1. Commit `bbc0e285` at
14:59Z recorded all four discovery lanes closed and first-line reviewed. The
ledger then records named D2/D3 packets, reviewers, builders, job identifiers,
refusals, corrections, and gates; for example, claude-13 refused R8-D2 at
15:19Z (`89297bfd`), codex-1 refused R2-D2 at 15:51Z (`dcc6f167`), and
claude-20 recorded R9-D2 plus R2-D2 closure at 16:24Z (`9791f7b2`). The charter
required CML first and R9/R2/R8 next (`holes/problems/BUILD-tech-lead-charter.md:25`),
not the full R5 noun chain.

The packet directory's filesystem chronology runs from the 2026-08-30
discovery packets through NOUNS-D1 at 21:41Z, then adds four DELEGATE packets on
2026-08-31 at 09:34Z. Git records its last Structure-A ledger/packet activity in
commit `6c102ed2` (Joseph Corneli, 2026-08-31 14:34Z). Later commits updated the
R5 problem record itself; `ea89ff2b` (Joseph Corneli, 2026-09-02 11:32Z)
records Joe's confirmation that pattern equals policy at recursive grain.

Commit `f39ef137` (Joseph Corneli, 2026-08-31 09:19Z) preserved 74 claude-13
turn files and described them as stranded and unreviewed. Commit `b1a8fe93`
one second earlier preserved five untracked glossary formalizations because
eight committed records already cited them. Here “stranded” means that git did
not track the cited artifacts and that cleanup could erase them; it does not
mean that a reviewer rejected their mathematics.

Claude-1 documented claude-15's distinct failure in commit `4406a4dd`
(2026-08-31 16:05Z). Claude-15's OAuth refresh failed, then model safeguards
rejected every later request at roughly 448k context
(`holes/labs/wm-contract/NOTE-claude-15-buffer-mined-2026-08-31.md:13`). The
ledger and problem records preserved the build state, claude-20 resumed it, and
only the connective REPL text faced loss
(`holes/labs/wm-contract/NOTE-claude-15-buffer-mined-2026-08-31.md:20`).

No artifact I searched records an operator or owner decision to pause or retire
Structure A. Search trail: `BUILD-ledger.md`, `BUILD-status.md`,
`DECISIONS-REGISTER.md`, `DECISIONS-PENDING.md`, the charter, all BUILD packets,
the two REPL archives, and git subjects from 2026-08-30 through 2026-09-05.
Thus the pause decision is **not established**.

## 3. Structure B becomes operative

Claude-1 and Joe created Structure B in commit `44cb1e18` (Joseph Corneli,
co-authored Claude Fable 5, 2026-09-01 08:42Z). The commit message records Joe's
instruction to turn the item list into a simple automated workflow. The initial
`worklist.edn` names `TN-edge-review-aif-wiring.md` sections 6, 8, and 9 as its
source; it defines V/C/D/H/J classes, evidence-backed closure states, a
second-reader gate, and a loop that takes one open non-J row per invocation
(`44cb1e18:holes/labs/wm-contract/worklist.edn`, header and schema).

The loop immediately became the operative scheduler: commits `3bc04e77` through
`c20fef97` closed or reviewed D1–D8 between 08:49Z and 10:11Z on 2026-09-01.
Git attributes the commits to Joseph Corneli; the records name claude-1 as the
registry owner/reviewer and `wm-organization` as the drawing-row owner. From
2026-09-02 through 05, Joseph Corneli's commits repeatedly “mint” U, RE, and F
eras in `worklist.edn`; the individual rows and review commits name Claude and
Codex seats that performed and reviewed each unit. The plan therefore grew by
ledger minting rather than by a single replacement decision.

B touched A's nouns without consistently inheriting A's parent record. The
genesis row D5d called an action candidate `pi` and added R6→R4 from existing
runtime code (`44cb1e18:holes/labs/wm-contract/worklist.edn`, row D5d); commit
`a68ed222` closed that row on 2026-09-01 09:55Z. That row constitutes the first
B mint I found that consumed an A noun as a bound proxy instead of constructing
the commissioned Cascade/Policy/Q chain. Its pointers cite code and C452, not
`P-validated-R5.md`. Later B work explicitly revisited R5 and Q: U53 audited
the missing machine-Q constructor, and the 2026-09-05 F era re-anchored the R5
spine (`holes/labs/wm-contract/EPIC-run-era.md:580`). Those later links do not
retroactively supply the missing parent link in D5d.

## 4. The lost ordering clause

Structure A states dependency order in the R5 chain
(`holes/problems/P-validated-R5.md:90`), in the glossary formalizations (for
example `holes/labs/wm-contract/R5-glossary-formalisation.md:53`), and in the
commission that claude-1 recorded at
`holes/labs/wm-contract/EPIC-run-era.md:566`. Structure B did contain local
ordering language: its rows model `:depends-on`, its edge work distinguishes a
route from a dependency DAG, and `build_order.bb:4` says to implement in
dependency order. However, B's genesis schema offers no parent-plan field and
its loop rule chooses an open non-J row; it does not prove that upstream A nouns
exist before closing their consumers.

The chronology establishes displacement by mechanism, not intent: claude-15
defined the noun chain; claude-20 dispatched a narrower first wave; two session
failures forced artifact rescue and succession; Joe and claude-1 created an
automatable edge-review ledger; repeated mint commits expanded that ledger;
and D5d let existing action-grain code satisfy the `pi` edge without citing or
inhabiting A's Policy and Q prerequisites. I found no actor-authored decision
that replaced A with B, so that link remains **not established**.

## 5. Proposed standing check (no implementation)

Add a contract-fidelity gate to every operative plan artifact. Require fields
for a pinned commissioning ancestor, inherited clauses, explicitly dropped
clauses with an author/date/rationale, and a machine-derived dependency list;
then refuse a runnable plan when it lacks an ancestor or schedules a consumer
before its declared producers. Applied here, the checker would reject genesis
commit `44cb1e18`: its source points only to the edge-review note, while D5d
uses `pi` without a pointer to `P-validated-R5.md:90` or evidence that Cascade,
Policy, and Q already exist. The same checker would accept the 2026-09-05 F
restatement only after its dependency graph enforces the roots and consumers
that `EPIC-run-era.md:580` names.

## Verification and limits

I checked citations by running the repository pointer checker after adding this
file and by manually opening every commit-qualified citation with `git show`.
I ran `worklist_check.bb` read-only. This account makes no claim about intent
beyond authored text, and it labels the missing replacement/pause decision as
not established.
