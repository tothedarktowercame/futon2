# E-kimi-task-419 — Euclid: case-split coverage + tutor question

**Requisition:** in-progress — dispatched 2026-11-07T17:35:40Z to kimi-2 as invoke-1794072940855-15298-bbe1867f

Clocked in by claude-9 for kimi-2 on 2026-11-07 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

Packet P-euclid-play-case-coverage (E-euclid-tutor), for kimi-2, from claude-9.

Your case moves (11c57cf) are accepted; they are live on :8100. My review found one hole and I fixed it myself (448daef, read it): a by_contra with ZERO cases asserted its conclusion outright (I.21 reached Solved with its by_contra emptied); play() now refuses an empty block at any depth. The remaining gap is this packet: dropping ONE case of several is still accepted, because the engine leaves exhaustiveness to the caller. Joe approved: the referee checks that a split covers every possibility, and when it does not, the tutor asks about the missing case instead of saying "wrong".

Goal (scripts/euclid_play.py, euclid_play_test.py, viewer/play.html only):
1. coverage(s, rule, cases, conclusion) in euclid_play.py -> {"covered": bool, "missing": [fact lists], "family": name}. A split is covered when its cases (each a list of hypothesis facts; use each case's single fact, or the case's conjunction where the engine uses two-fact cases as in I.24's g-on-DF split) together with, for by_contra only, the conclusion itself, cover one of these families, which come from System E:
   - three distinct collinear points p, q, r: between p q r | between q p r | between p r q (between_points, Diagrammatic.lean:87; read it). between is symmetric (between p q r = between r q p): normalise.
   - two points and a line: same_side p q L | opposing_sides p q L | on_line p L | on_line q L.
   - lengths |pq|, |rs| (and angles, same): eq | gt | lt (written as gt with the sides swapped).
   - any fact and its negation: on_line/not_on_line, peq/neq, same_side/not_same_side.
   A family member counts as covered also when assuming it on a copy of the current state is already contradictory (eng.contradictory after close), e.g. peq g f when neq g f is known, or on_line p L when not_on_line p L is known. Report the uncovered members as "missing". If no family matches the cases at all, covered=False with missing=[] and family=None.
2. In play(): a CLOSED case block whose split is not covered is refused with error "missing case" plus the missing facts (structured, so the page can phrase it). Open blocks are unaffected.
3. Page: on that refusal, show the tutor's question in plain words, e.g. "What about the case where b is between e and d?" (reuse the fact -> English table), with a button "add this case" that opens it as the next case of the still-open block (so the student can show it is contradictory or reaches the conclusion). If family is None: "I can't tell whether these cases cover every possibility. Try splitting on where a point lies, which side of a line, or which length is bigger."
Tests: every engine case split in the catalogue (I.10, 13, 14, 19, 21 with all their nested blocks) is covered, so their engine steps still reach Solved (say which family each matched, and any member covered only by contradiction with the state); I.21 with either refuting case dropped -> refused, missing names exactly that between fact; I.19 with one case dropped -> refused with the missing comparison; a by_cases on {same_side d g EF} alone -> missing includes the opposing_sides and on_line members unless the state rules them out; a split of unrelated facts -> family None. Extend ~/notes/euclid-cs/play_check.py (port 8124): in I.21, close the by_contra with one case missing, see the question, press "add this case", complete it, reach Solved; screenshot ~/notes/euclid-cs/kimi-play-coverage.png (look at it). Kill test servers by the pid from `ss -ltnp`; never use 8100 (Joe's live server).
Rules: do not modify euclid_engine.py, euclid_trace.py or index.html (kimi-4 is editing euclid_trace.py and euclid_play.py's GOALS table: touch only your functions in euclid_play.py, and if you both edit it, rebase-free: commit only your hunks with git add -p is NOT available, so coordinate by keeping your edits away from GOALS and committing promptly). Never read the engine whole (grep + sed <= 120 lines). Stage explicit paths only; no commit -a, no stash, no push. Commit when the checks pass, before belling.
Bell claude-9 back with a summary + commit sha, test counts, and the family table per engine split.
