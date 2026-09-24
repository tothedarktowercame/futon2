# Handoff: the ten older stop-lines

Written by claude-5, 2026-09-24, after the six from 2026-09-23 were diagnosed
separately. Nothing here has been repaired. The machine is stopped.

## The rule this note is written under

Joe, 2026-09-23:

> The queue is for ordinary defects. Stop-the-line means that the defect was
> not repaired under ordinary running procedures and must be repaired from
> outside. That means that the machine must be stopped in order to do the
> repair.

So: the repair **queue** is ordinary running's own work — selection picks those
up during clicks. A **stop-line** is the escalation, for a defect more running
cannot fix. Repairing one is outside work. It needs no click budget, and no
click should be fired while one stands.

## What the numbers are

`open-obligations` over `futon2/data/wm-repair-obligations` returns 52. Split by
the rule `futon2/scripts/wm_click.sh` uses (~line 200) — open AND class is not
`:environmental-hold`:

| | count |
|---|---|
| stop-lines | 16 (all `:machine-failure`) |
| ordinary queue | 36 |

Six of the sixteen were minted on 2026-09-23, one per click, and are one
condition recorded six times — see the separate finding. **These ten are the
rest.** They are heterogeneous and are NOT that pattern.

## The ten, and what they cluster into

Ten rows, at most six distinct conditions.

### A. "incompatible preference schedules" — 3 rows, 1 condition

| id | opened |
|---|---|
| `repair-occ-d36b1b271427fc21dc365b44a3297d65d6761f2d667850154ef578658f27205f` | 2026-09-20T20:52:19Z |
| `repair-occ-5ef53a512ad80d55f4bde136724d6ad13e6293cc548a092c90ab0317674753b9` | 2026-09-20T21:38:23Z |
| `repair-occ-652aa5f8e36ebcddd88ac69aa13a65e6c3f260cb1e2d056474ee9f46a5006546` | 2026-09-20T23:58:11Z |

All three: `:failure-kind :untyped-failure`, `:failure-stage :selection`,
`:failure-outcome :incomplete`, error `"incompatible preference schedules"`, no
target. Three hours apart on one evening.

`:untyped-failure` is the tell: the error string says something specific and the
kind says nobody classified it. Start by finding what throws that string and why
it is untyped.

### B. "Invalid token: :hole/2f9b03b16170" — 2 rows, 1 run

| id | kind | stage | opened |
|---|---|---|---|
| `repair-occ-e9eec70e60b38c5f3d0d073dc33832511f8b88a75cb22d4de6dad1a7d49bc4ff` | `:initialization-failed` | `:initialization` | 2026-09-21T00:02:57Z |
| `repair-occ-88bb128331ef06bb2f9b50021e70d3f42d2d33504fcf22d8736b43a044c49d2b` | `:close-exception` | `:close` | 2026-09-21T00:03:37Z |

Forty seconds apart, same malformed token, target `:C0` on the second. One run
failing at initialization and again at close, recorded twice under two kinds.
Repairing the token source should close both; check that it does rather than
assuming it.

### C. "Limb evidence refused" — 2 rows, same message, possibly different causes

| id | kind | target | opened |
|---|---|---|---|
| `repair-ea1-259a7a93d9a9b63a45020e0ad646c508b9be83097dac6898106a063c1a1eef2e--attempt-001-revision-unchanged` | `:revision-unchanged` | `repair-ea1-3f4cac24…` | 2026-09-14T23:53:20Z |
| `repair-occ-6c6ecaf858aff5c7f9aeef1b92ad205e7ed31ed29882e89fa52f2724d3d039c8` | `:explanation-invalid` | `M-aif-policy-conditioned-eig` | 2026-09-21T23:35:32Z |

Same error string, different `:failure-kind`, different targets, a week apart.
**Do not assume these are one defect because the message matches** — the message
is generic and the kinds disagree. Establishing whether they share a cause is
part of the work.

The second names `M-aif-policy-conditioned-eig`, which is the mission whose
`:revision-wait` refusal opened the ticket the 2026-09-23 clicks spent their
whole budget repairing. Related by subject; not known to be the same defect.

### D. Singletons — 3 rows

**`repair-occ-f22800c6792de6edc80e03cb44c235cbb38a7e4e75c34b51f5f88c7c8672365a`**
(`:machine-defect`, `:selection`, 2026-09-21T00:50:00Z) — **already diagnosed,
to file and line.** Its own error field carries the analysis:

> Selection pools every EMPTY cascade into one action-marginal key and lets 21
> do-nothing candidates outvote the best acting one. `policy.clj/cascade-first-action`
> (:142) returns `(:type action)` — nil — for any cascade with empty `:precedence`.
> `cascade_selection.clj/bayes-choice` (:124) then sums posterior mass per action
> key (`update acc a (fnil + 0.0) p`). So all 21 empty cascades share the key nil
> and their mass SUMS to 0.785275, beating `:coordination/par-as-obligation` at
> 0.179031 (2 candidates) and `:cascade-construction/run-it-on-a-real-case` at
> 0.035694 (1). The machine did not prefer inaction on the merits: the per-policy
> posterior argmax is `:C1` on `M-expressions-of-interest` at 0.143225, a
> THREE-PATTERN cascade, exactly 2x the selected `:C0` `M-dionysus-winddown` at
> 0.071389 (rank 2 of 24). 'Do nothing' won because 21 candidates wore one hat.

This one needs a repair, not an investigation. It is the strongest candidate to
take first. Re-derive the numbers before changing anything — they are from
2026-09-21 and the store has moved since.

**`repair-occ-0576180e03115d74ce0a32eff59df655649d732e8cdc10f12a86f98b15f51879`**
(`:fold-output-invalid`, `:construction`, target `:C1`, 2026-09-21T00:56:55Z) —
"Construction fold wiring is missing or malformed". Six minutes after the
`:machine-defect` above, same night, different stage.

**`repair-ea1-9b6ce3a47b9bdc24a5fc6a9b9aab4f270af86a4fcb54550d94b89737bb5e4f35--attempt-003-evidence-not-single-edn`**
(`:evidence-not-single-edn`, `:close`, 2026-09-15T02:18:32Z) — "Attempt evidence
must contain one EDN form". Oldest of the ten bar one.

## What to do with this

Suggested order, and the reasoning is only that it puts the cheapest certainty
first:

1. **D/`f22800c6`** — diagnosed; needs a repair and a test that pins the bad case
   (empty-precedence cascades must not pool under one key).
2. **B** — one token, two rows; likely the smallest real fix.
3. **A** — three rows, one condition, and `:untyped-failure` means the typing is
   itself part of the defect.
4. **C** — discovery first: same or different?
5. The two remaining singletons.

Each of these is a separate handoff. Discovery and implementation are separate
handoffs with a review between, per `CLAUDE.md`.

## Constraints

- **The machine is stopped.** No clicks. Renewal-6 is exhausted (7 of 7) and
  that is not the reason — the rule is.
- **Read-only until a repair is reviewed.** No writes to
  `data/wm-repair-obligations`; no `run-opportunity!` / `run-case` outside the
  suite fixture (they write real trip reports and read the real repair store).
- **Do not touch the serving JVM** — no `load-file`, no reload from a worktree.
  One futon3c JVM on master.
- futon2 is a shared checkout on `main`: stage explicit paths, never
  `git commit -a`.
- Reading through the running JVM is usually fastest:
  `cd /home/joe/code/futon3c && ./scripts/proof-eval.sh '<form>'`. It masks
  exceptions as "Syntax error macroexpanding" — wrap in `try` and walk
  `.getCause`.

## Not established

- Whether any of these ten were ever worked on. None has a record in
  `resolutions/`, `implementations/` or `verifications/`, but absence there has
  not been cross-checked against the run history.
- Whether C is one defect or two.
- Whether the no-cross-run-dedup defect found in the 2026-09-23 six also
  produced the repetition in A and B, or whether those repeated for their own
  reasons. The shapes look alike; that is not evidence.
- Whether any of the ten is already fixed in code and merely never discharged.
  A stop-line stays open until something writes `resolutions/` or an operator
  writes `dismissals/`; an `implementations/` record only marks it
  `:awaiting-validation`. Check before repairing.

---

## Addendum, 2026-09-24: most of this list is already repaired

claude-8's analysis, checked by claude-5 against source and the live store.
**Do not start repairing from this list until the discharge path is fixed** —
three of the six conditions were repaired within the hour they were opened, and
the ledger cannot see it.

| condition | fixed by | opened → fixed |
|---|---|---|
| `f22800c6` pooled-nil action marginal | `d168d348` + `8f60f819` | 00:50 → same day |
| `Invalid token: :hole/2f9b03b16170` (×2) | `29fb2a83` "Hole want-tokens must be readable, not merely printable" | 00:02:57 → 00:06 |
| `incompatible preference schedules` (×3) | `a38becc9` "Generated mission-hole targets must adopt the family's schedule and scales" | 20:52 → 21:43 |

That is six of the ten rows. The fixes are live; none has an `implementations/`
or `resolutions/` record, because nobody ran `record-implementation!` and
`resolve!` by hand and no automatic path writes them.

### Why the automatic path writes nothing

`repair-discharge/finalize-run!` is NOT silent — it runs on every click and
refuses. Nine `discharge-operations/` records exist from 2026-09-23, six of them
stamped at the exact minutes the six clicks closed (17:44, 18:21, 19:06, 19:58,
21:02, 21:44). Every one:

```clojure
{:result {:status :evidence-unavailable
          :repair/id nil
          :stage :binding
          :reason :unsafe-repair-id}}
```

Two independent blocks, both in `src/futon2/aif/repair_discharge.clj`:

1. `bind-selected!` (:15-30) takes `(:repair/id action)` for a non-legacy
   action. A ticket-queue action carries its target as a `T-repair-…` string and
   no `:repair/id`, so `evidence/safe-id!` is handed nil and refuses before the
   finding is ever read.
2. `finalize!` (:86-90) requires `(true? (:grounded? close))`. The closes are
   `:grounded? false`, so even past the binding stage there is no route from a
   ticket going DONE to a `resolutions/` record.

Nothing reads `ticket-links/` backwards, so the ticket route and the obligation
ledger never join.

### What this means for the order of work

The note above suggests repairing `f22800c6` first. That is now known to be
wrong: it needs no repair. **The first piece of work is the join** — the three
conditions claude-8 names:

- the selected action carries the finding id when its target is a repair ticket;
- ticket acceptance (Status DONE) writes, or is read as, a resolution;
- fixes that land outside a click get recorded at all.

Until then, every repair done from outside adds a fix the ledger cannot see, and
the list gets longer rather than shorter.
