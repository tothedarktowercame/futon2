# ⟨1⟩6 — the second limb, fired and reported from the records

zai-1, 2026-09-23. Pre-flight clean in the serving JVM, then fired.

## Pre-flight (all three clean)

1. **Queue**: `:front-stratum`, the reference ticket the only eligible
   target.
2. **The decision now**: with `:repair/split-declared-valid` observed FALSE
   (the head in the wrong shape — a string value, not line-initial), the
   class model still prefers **:C2**: G(C2)=0.5978, G(C1)=2.9957;
   posterior {C2 0.9167, C1 0.0833}; marginal unique maximum at
   `:aif/declare-the-conditioning` (0.9167). Reason: C2's first-action
   guard (needs task-stated, FORBIDS split-declared-valid) still holds on
   the false observation — the re-declaration route remains the preferred
   limb. Same candidate as last time, for the state the ticket is in.
3. **The author brief carries the acceptance block** (verified in the
   serving JVM through the runner's own renderer): the produced token's C4
   requirement — `resources/wm/eig/held-out-split.edn` must contain the
   exact head `"HELD-OUT-SPLIT-DECLARED"` at the start of a line — and the
   target's own acceptance (`"**Status:** DONE"` line-initial).

## The click, from the records

- **Click** `wm-click-8b290466-0cdb-4c1f-aca5-5bbbd4268cad`; **run**
  `2026-09-23-1790136186`; **attempt**
  `machinery-72/wm-contract-machinery-72-v1/attempt-002`; **outcome**
  `:grounded-change`; **binding** verified/confirmed.
- **Selected**: the reference ticket's **:C2** again —
  `:decided-by :ticket-queue`, eligible targets `[T-repair-occ-444fb018…]`,
  choice `:aif/declare-the-conditioning` producing
  `[T :repair/split-declared-valid]`, stratum posterior keyed on the
  ticket's C2.
- **THE THING THIS CLICK WAS FOR — the artifact satisfies its own declared
  locator THIS TIME.** The delivered commit `0798f96a` "Make held-out split
  C4-observable" rewrote the disposition as a **line-initial head**:
  `resources/wm/eig/held-out-split.edn` line 2 is now the bare token
  `HELD-OUT-SPLIT-DECLARED`. The author, briefed with the exact
  requirement, produced exactly that shape.

**The predicate's conjunct-by-conjunct verdict on attempt-002:**
- **(a) fresh binding: HOLDS** (commit 0798f96a, descendant of the
  pre-dispatch head, corroborated, in-window).
- **(b) declared products observed: HOLDS** — `[T
  :repair/split-declared-valid]` observed **true** at the after-revision
  through its own C4 locator (this is the conjunct that failed last time;
  the brief fixed it).
- **(c) the target's own acceptance: FAILS — and this is now the ONLY
  failing conjunct.** The recorded verdict on the close:
  `{:accepted? false :failed :c :reason :acceptance-not-observed
  :evidence {:acceptance-token :restoration-accepted
  :acceptance-result {:observed false :check :C4 …sha 0798f96a…}}}` —
  the ticket's Status line still reads OPEN, exactly as the repair's state
  demands (limb 1 of 4 re-declared correctly; limbs 2-4 outstanding).
- **B update: NOT written — correctly** — the guard requires
  `{:accepted? true}`; (c) is false, so `b-update` was never called. The
  conjunct that stopped it: **(c)**, named.

**Certificate**: `:preference-audit` `{:status :recorded
:consumed-preference-kind :class-emission :class-preference {focused
11/20 …} :class-preference-provenance … Joe 2026-09-22 ruling}`;
`:precision-family` model-id `0f06c582…` with the class q0 and horizon 4.

## What this click demonstrates

The machine's own loop diagnosed its failure (the predicate's typed (b)
failure), the fix was made at the dispatch (the criterion stated
verbatim), and the very next click's author — reading only its brief —
produced an artifact that passes its own declared test. The predicate now
fails at exactly the conjunct that reflects the true state of the repair:
**limb 1 landed, the ticket is honestly OPEN.** ⟨1⟩6's "accepted close"
awaits the later limbs; every piece between selection and acceptance is
now demonstrably working.
