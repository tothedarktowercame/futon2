# Packet: retain the habit outcome receipt at the witnessed-outcome boundary (2026-10-22)

From claude-2. Recording change only; selection, ln E and routes do not change.
Step 2 of section 4 in `NOTE-r20-habit-counts-2026-10-22.md` (read sections 1, 3
and 4 first). Step 1 landed as `c9cbf519a` / `4b2c6bf93` (`:habit-fold-snapshot`).

## The grain is per policy (settled)
Lean darktower `de33cabb48` (claude-7, Joe's ruling 2026-10-22) removed the
action-class layer: habit counts are per policy, as in Friston 2016 and Parr 2022
p.209. The existing fold already keys counts by structural policy identity
(`enactment_habit.clj`, `cascade_prior.clj`), so key by that. Do not map to
observation classes or target classes. Successes count (Parr p.209), using the
existing W_c increment rule (`enactment_habit.clj` increment: W_c pass = +1;
failure / absent verdict / refusal = 0) — that is the success authority.

## Goal (one behaviour)
The tick run record gains a top-level `:habit-outcome-receipt` (next to
`:habit-fold-snapshot`), the Clojure counterpart of Lean
`R15TemporalHierarchy.TwoTickRecord`'s before / outcome / after:

    {:schema :wm/habit-outcome-receipt-v1
     :status :present
     :policy-key <structural identity of the selected policy, as the fold keys it>
     :event-id <the unique id the fold deduplicates on ([click candidate])>
     :witness <W_c verdict or typed none> :admitted? <bool> :delta <0 or 1>
     :before-sha256 <the :habit-fold-snapshot :sha256 of this click>
     :counts-before <count at :policy-key> :counts-after <before + delta>}

- `:delta` comes from the existing increment function, never recomputed by hand.
- If this click has no W_c verdict (ordinary clicks may not), record `:status :absent`
  with the reason and where the verdict would have come from. Do not count a runner
  close or a selection as a success; do not revive the retired close writer.
- Persisting the receipt somewhere the next click's fold reads it is step 3; this
  packet only puts it on the record. Say in your bell whether the existing flight
  persistence already makes it durable.

## Validation (`scoring_input_receipts.clj` validate-record*)
- `:counts-after` ≠ `:counts-before` + `:delta` → `:habit-outcome-arithmetic`;
- `:before-sha256` ≠ this record's `:habit-fold-snapshot :sha256` → `:habit-outcome-before-mismatch`;
- `:delta 1` with `:admitted? false` → `:habit-outcome-unadmitted-success`;
- key missing → `:habit-outcome-receipt-not-observed` (update old fixtures, not the rule).

## Acceptance tests
1. A record built as the runner builds it with a W_c pass: present, delta 1, validates.
2. A W_c failure: delta 0, after = before, validates.
3. Bad cases each caught with its own error: after edited; before-sha changed;
   delta 1 on an unadmitted verdict; key removed.

## Do not
Change selection, the objective, ln E, the fold, or any route; write count files;
touch `meta_live_outer_selector.clj` or `policy.clj` (codex-4 is in policy.clj
for the precision packet).

## Gates
clj-kondo; `emacs --batch -l ~/code/futon4/dev/check-parens.el`; `clojure -M:test`
for scoring-input-receipts and touched fixtures (FUTON3C_INVOKE_JOBS_FILE to a temp
path). Rebase onto master before committing. No live clicks, no JVM hot-load.
If the selected policy's W_c verdict is never available at record time on the live
route, stop and bell that finding with anchors; that is a useful answer.

Bell claude-2 back with a summary + commit shas.
