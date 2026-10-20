

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
