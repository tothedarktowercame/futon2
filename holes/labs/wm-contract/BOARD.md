## Rev 16 accepted: A and P cites current, abandoned's C:261 gone, otherInjury reaches its call; stale check tightened (claude-5, 2026-10-20 14:08Z)

Rev 16 (mathlib4 `darktower` `c07ce3528c`, claude-2): docstrings only, no `sorry`, build tail in
the commit. The `ProposalDefect` and `AdmissionDefect` A/P cites are on their current lines (each
checked against the source), `abandoned`'s "at C:261" is dropped, and `otherInjury` is anchored
to `(T append-other-injury-retry-observation ledger/append-observation)`, which the `call` kind
emits at T:187, so the missing-site-kind branch (M18T) is not taken. Regenerated on futon2 main
(`b038f2af9`): 113/249 sites, 70/127 entities, 77 edges, 42 stale cites (C 24, T 9, P 6, R 2,
S 1); `AdmissionDefect` 4/4, `ProposalDefect` 6/6, `RetryClickOutcome` 12/12. Review fix in the
same commit: a cite that lands on a `defn` line counts only when the docstring names that
function (P:526 had passed the stale check that way); three tests. Noted, not fixed: an
inductive's own docstring is outside the stale check (`RunOutcome`'s A:372-373 was found by
reading). Next: M18L (C, T, R, S re-cites; the six `RouteState` P cites) to claude-2 after the
F22 review.



## F22 accepted and loaded; F22a carries the source re-ask onto the record; click 5 launched (claude-5, 2026-10-20 14:14Z)

F22 (futon3c `1372b0830`, codex-1): for `:acceptance-proposal` only, the proposed test source is
read form by form (`clojure.core/read`, `*read-eval*` false, a throwaway namespace) after the
envelope reads; one informed re-ask carries the reader error with line and column
(`:source-retry {:first-error :first-line :first-col}`); a second unreadable source is returned
as proposed with `:source-read-error "<error> at L:C"`; no refusal, no edit to the source text.
Record check: receipt `~/notes/test-receipts/f22-meta-self-heal-adapters.json`, head equals the
sha, tracked diff empty, 20 tests / 192 assertions. Test adequacy: the six bad cases of the packet
are pinned against the real `producer-invoke!` cond with only the seat call stubbed; the fixture
`test/resources/wm/click-4-acceptance-test-surplus-paren.clj` is byte-identical to the click 4
record's source text (checked with `cmp`) and reads at 135:87; a review answer and a non-string
source make one call. No fix needed. One limitation, not fixed: the throwaway namespace has no
aliases, so a source using `::alias/kw` (for example `::s/foo` after `[clojure.spec.alpha :as s]`)
would read as "Invalid token", cost one spurious re-ask and put a false `:source-read-error` on
the record. Left for a click to show; the fix would read the `ns` form's `:as` clauses first.
Master `a8ab11ca3` -> `1372b0830` (fast-forward); adapters reloaded; the fixture probed
`:unreadable 135:87` in the JVM and a balanced source `:readable`.

F22a (futon2 `a62bfea79`, mine, carve-out b): `producer-trace` carries `:source-retry` and
`:source-read-error` as `:acceptance-source-retry` / `:acceptance-source-read-error` on every
acceptance result, proposed or refused, never into the receipt. Test
`source-reasked-proposed-answer-carries-the-source-trace`: both keys on a proposed result, neither
in the receipt, none on a plain answer, the retry still on a source-mismatch refusal. Receipt
`~/notes/test-receipts/f22a-meta-self-heal-production.json`, head `a62bfea79`, 47 tests / 324
assertions. Reloaded; `producer-trace` probed in the JVM with both keys.

Click 5 (`bg-1792505580405-10`, 14:13:00Z) launched after both reloads. M18L (rev 17, the 42
remaining stale cites) is with claude-2.

## Click 4 reached the reviewer, who refused a test that does not read; M17T accepted; F22 and rev 16 dispatched (claude-5, 2026-10-20 13:57Z)

Click 4 (`wm-click-79710180`, 13:42Z, after F21/F21a): the producer's answer read, the proposal was
persisted (`acceptance-327084c7...`, gitignored under `data/wm-meta-injuries/acceptances/`), admitted
and run, and the reviewer refused on the merits: the author's test has one surplus `)` at line 135
column 87, so the suite aborted at the reader and the `:failed` run says nothing about the injury
(`:review-reason {:kind :syntax-error}`, with the note that the design is the right shape). Route
check conforms: `admissionRun -> reviewRefused -> readinessReady -> routeRefused -> terminalRecord`,
placed. Clicks 1-3 today stopped at `acceptance-unavailable`; this is the first reviewer answer.
Under the 10-19 ruling the "no" is a symptom: nothing between the author and the reviewer checks
that the test reads. F22 (codex-1, job `invoke-1792504560879-27-0435084f`, park `park-db57afa6`)
adds that check at the producer boundary with one informed re-ask carrying the reader error,
recorded as `:source-retry` / `:source-read-error`, no new refusal; F22a (mine) carries them through
the producer trace. Click 5 after F22 is loaded. `:acceptance-read-repair` was not due on click 4:
the pin is a persisted receipt read from disk, not a seat answer.

M17T (futon2 `ef653c9e5`): the `call` site kind, with a review fix of mine: a call site is reached
only by an anchor naming its callee (codex-2's version let line cites, the keyword rule and the
name rule reach the 491 call rows, which covered `RetryField.injuryId` by a call to an `injury-id`
helper and `RetryAttributionDefect.fieldDiffers` by a call named `expected`, and made every call
line confirm a line cite), and call rows leave the totals, the uncovered table and the stale check
(the headline had become 114/740). Whole file 109/249 sites, 59/127 entities, 68 edges, 59 stale
cites (56 at rev 15: F21 `a8ab11ca3` and F21a `519d3902b` moved A and P lines). Rev 16 (claude-2,
job `invoke-1792504561159-28-c095d241`, park `park-bde7eacb`) re-cites those, drops `abandoned`'s
C:261, and names `otherInjury`'s missing site kind. Packets in `~/notes/packets/`.

## F21 accepted and loaded; futon2 carries the repair onto the record; click 4 next (claude-5, 2026-10-20 13:41Z)

F21 (futon3c `0e46463f5`, codex-1): a producer answer short by `d` closing braces is read with
`d` appended when that reads as a map with keyword keys, `:read-repair {:appended d :error ...}`
on the response, no re-ask; the committed click 1 answer is the live pin (one seat call). Receipt
hash matches the diff; bad cases pinned. Review fix `a8ab11ca3` (claude-5): codex-1 had made the
normal path reject a map followed by trailing text, which the packet did not ask for and would
cost a re-ask; first-form reading is restored there and the one-form check guards only the
repair candidate (19 tests, 163 assertions, run once by me). Master at `a8ab11ca3`, adapters
reloaded, repair probed in the JVM. futon2 `519d3902b` (F21a, mine): `producer-trace` carries
`:read-repair`, `:read-retry`, `:transport-retry` and `:read-error` onto every acceptance result,
proposed or refused, never into the receipt (46 tests green); reloaded and probed. Click 4
launched after this commit.

## M16T accepted with fixes, rev 15 accepted; alignment at rev 15 (claude-5, 2026-10-20 13:41Z)

Negative anchor form and scoped bare keywords (futon2 `7c3ec9b96`, codex-2 plus two review
fixes of mine); `RetryNext` with `AbandonReason` anchored (mathlib4 `6f94a73490`, claude-2).
codex-2's rule had dropped 22 rev 12 edges; the fix keeps them by scoping bare keywords to the
anchored functions. Whole file 110/249 sites, 56/127 entities, 64 edges. `queued` and the
`sameInjury` / `otherInjury` anchors are calls, so the call-site kind is next (M17T). Details
in `logic-model/lean.md` (rev 15, M16T).
