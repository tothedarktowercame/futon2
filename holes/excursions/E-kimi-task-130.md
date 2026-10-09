# E-kimi-task-130 — PROGRESS-I: intermediate-progress checker as a pure function

**Requisition:** completed — 2026-09-28T21:12:51Z, job invoke-1790629942852-26373-13db056f, state failed

**VERDICT (2026-10-09, provisional):** OPEN — Requisition completed with state failed — the work ran but did not succeed, so the goal is unachieved and not stated dropped. _(WM status classification by zai-2, medium confidence; not yet confirmed by the author.)_

Clocked in by claude-1 for kimi-2 on 2026-09-28 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# PROGRESS-I — the intermediate-progress checker, as a pure function

From claude-1 (PROOF-2a lead). Bell claude-1 back with a summary and the commit sha.

## Why
PROOF-2a ⟨1⟩4, Joe's ruling (2026-09-27 14:36Z, decision item 3): "the progress criterion is the theorem's clause, 'produces a token later consumed'; the checker is ... distinct from flight.clj's wants-flip". Theorem clause 2 (futon2 `holes/labs/wm-contract/PROOF-2a-THEOREM-draft-2026-09-24.md:66-70`): "A click that meets no criterion and produces no token a later click in the flight consumes is overhead with no progress, and counts against the machine as a stop would." Discovery PROGRESS-D (read `/home/joe/code/storage/proof-2a/progress-d/REPORT.md` first) found no code joins one click's produced token to a later click's guard. This packet writes the checker as a PURE function over an explicit input. Assembling that input from flight, enactment and run records is a later packet: do not read any record or file in this one.

## Decisions (claude-1; implement exactly these)
- Token identity: the pair `[target token]` (runtime identity, temporal_update.clj:33). A produced token and a needed token match only when both target and token are equal.
- "Later" means a click with a strictly greater position in the flight. A token needed by a pattern of the SAME click does not count.
- The checker reports both disjuncts of the clause. The first ("meets a criterion") is the click's existing wants-flip result `:advanced` (a vector of wants; non-empty = met), passed in, not recomputed.
- An unknown is never read as a negative: if no consumption is found among the later clicks whose needs are known, and some later click's needs are unknown, the verdict is `:unverifiable`, not "produced but not consumed".

## Input (one flight; a vector of clicks in flight order)
```clojure
[{:click-id "c1"
  :target "M-x"
  :advanced [...]                        ; wants flipped false->true by this click
  :produced #{:tok-a :tok-b}             ; tokens produced by this click's successful attempts
            ;; or a typed absence {:absent <reason>} (e.g. :no-decision, :no-enactment-record)
  :needs #{:tok-c}                       ; tokens this click's chosen cascade's guards needed
            ;; or {:absent <reason>}
  } ...]
```
## Output
A map `{:clicks [<verdict per click, in order>] :summary {<verdict kind> count}}`. Per click, exactly one of:
- `{:click-id .. :progress :criterion-met :advanced [...]}` when `:advanced` is non-empty (report consumed tokens too if also found, under `:consumed`);
- `{:click-id .. :progress :token-consumed :consumed [{:token [target tok] :by "c3"} ...]}` (every later consumer listed, sorted);
- `{:click-id .. :no-progress :no-token-produced}` when `:produced` is an empty set;
- `{:click-id .. :no-progress :produced-not-consumed :produced #{[target tok] ...}}` when every later click's needs are known and none matches (this includes the last click of the flight);
- `{:click-id .. :unverifiable <reason>}` when `:produced` is a typed absence (reason carried), or when the unknown-later-needs rule above applies (reason `:later-needs-unknown` with the unknown click-ids).
Invalid input shapes (non-set, non-absence `:produced`/`:needs`; duplicate click-ids) throw `ex-info` with a typed `:kind`.

## Files
- New `futon2/src/futon2/aif/progress_check.clj`, namespace `futon2.aif.progress-check`, public fn `check-flight`; namespace docstring quotes the clause and Joe's ruling and states the four decisions, in the style of neighbouring aif namespaces.
- New `futon2/test/futon2/aif/progress_check_test.clj`.

## Tests (constructed inputs; name each deftest for the case)
1. Two clicks: c1 produces :roles-named, c2 needs :roles-named, same target → c1 `:token-consumed` by c2.
2. Same, but c2's target differs → c1 `:produced-not-consumed`.
3. c1 produces and its own `:needs` contains the token, no later consumer → `:produced-not-consumed` (same-click does not count).
4. c1 produces, c2 `:needs {:absent :no-decision}`, c3 needs known and unmatched → c1 `:unverifiable :later-needs-unknown ["c2"]`.
5. Same as 4 but c3 consumes it → `:token-consumed` (a found consumer wins over an unknown).
6. `:advanced` non-empty, nothing consumed → `:criterion-met`.
7. `:produced #{}` → `:no-token-produced`; `:produced {:absent :no-decision}` → `:unverifiable :no-decision`.
8. The last click always yields `:produced-not-consumed` if it produced and met no criterion.
9. Summary counts equal the per-click verdicts.
10. Invalid shapes throw with the typed kind.
Use the real tokens of the worked example where natural (futon3c `holes/labs/M-futon-seams/exemplar/click-001.edn:129-165`, e.g. `:roles-named`, `:binding-recorded`), constructed into two-click flights.

## Bad cases (run each, report the failing test names; do not commit them)
- Plant: ignore target in the join → test 2 fails.
- Plant: count same-click needs → test 3 fails.
- Plant: treat unknown later needs as empty → test 4 fails.
- Plant: let an unknown suppress a found consumer → test 5 fails.

## Acceptance and rules
- `env -u GIT_DIR -u GIT_WORK_TREE clojure -M:test -n futon2.aif.progress-check-test` green in `/home/joe/code/futon2`.
- Gates: clj-kondo on both files; `/home/joe/code/futon4/dev/check-parens.el` (entry `arxana-check-parens--check-parens`).
- One commit by explicit path (`git commit -m ... -- <the two files>` after `git add` of those two only), never `-a`, amend, stash or `git add -A`; the checkout is shared with other agents. `git config user.name` must print `Joseph Corneli`.
- No edits to any other file; nothing reloaded into the running JVM.
- Report `/home/joe/code/storage/proof-2a/progress-i/REPORT.md`: test and assertion counts, each bad case with the test that caught it, the commit sha. Mark each claim Ran or Read.
