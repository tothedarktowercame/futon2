# ⟨1⟩6 — the acceptance plug (part 1) and the conjunct-by-conjunct re-evaluation (part 2)

zai-1, 2026-09-23. Part 1 built (8527577e); part 2 evaluated read-only on
machinery-72 attempt-001's real records (no new click, nothing adjusted).

## Part 1 — the plug (8527577e)

`cascade-sources/acceptance-of` extracts a target's OWN acceptance
declaration from its cascade source — the declared want token with its
locator — carried WITH provenance `{:source-file … :source-sha256 … :target
… :declaration :cascade-source-want-locator}`. A source declaring no want,
or a want with no locator, yields nil. The runner's predicate call now reads
the action-level slot first, then falls back to the SELECTED candidate's own
declared acceptance via `acceptance-of`. No acceptance language invented;
conjunct (c) untouched. Tests: the reference ticket's `:restoration-accepted`
C4 locator arrives with full provenance (accepted-increment-test 10/37/0);
a nonexistent target yields nil.

## Part 2 — the predicate on machinery-72 attempt-001, conjunct by conjunct

Inputs: the build checkpoint's artifact binding, the close's token
comparison rows, `acceptance-of`'s declaration, after-revision `ee22106c`.

**(a) fresh binding: HOLDS.** The binding from the real build checkpoint:
`{:commit ee22106c… :pre-dispatch-head c24c903b…
:descendant? true :corroborates? true :claim-in-author-window? true}` —
the reviewed commit is bound to this occurrence, a fresh descendant of the
pre-dispatch head (Joe's sixth-grant commit), corroborated, in the author
window.

**(b) declared products observed: FAILS — and here is what it actually
says.** The token comparison rows for this close carry ONE row:
`{[:T-repair-occ-444fb018… :restoration-accepted]
:verdict :predicted-not-observed :observed false}` — the horizon prediction
covers the candidate's full declared chain, and the terminal acceptance
token is predicted but not observed: the ticket's Status line still reads
OPEN at revision ee22106c (file present, `**Status:** DONE` absent). The
predicate therefore returns:

```
{:accepted? false
 :failed :b
 :reason :declared-product-not-observed-true
 :evidence {:failed-tokens {[T … :restoration-accepted]
                            {:observed false :check :C4
                             :evidence {:sha ee22106c… :file-present true
                                        :decl "**Status:** DONE"}}}}}
```

Note the honest wrinkle: I expected (as claude-5 did) the failure at (c);
it reports at (b) instead, because the runner's token rows feed the whole
predicted chain — the acceptance token included — as the "produced tokens"
map, and the chain's terminal token is exactly the acceptance declaration.
The delivered first-limb token (`:repair/split-declared-valid`) does not
appear in the comparison rows at all (the prediction's terminal state is
what was compared). Same underlying truth, different conjunct label: the
work landed its first limb; the ticket honestly remains OPEN.

**(c) the target's own acceptance: would also fail** — the same
`:restoration-accepted` C4 locator, the same Status line, the same
revision; the delivered commit's own message says it ("The ticket remains
OPEN until the later cascade limbs collect the declared records").

**Verdict: {:accepted? false :failed :b
:reason :declared-product-not-observed-true}** — the honest state of a
repair whose first limb landed. No B update is warranted. Nothing was
adjusted to make it true.
