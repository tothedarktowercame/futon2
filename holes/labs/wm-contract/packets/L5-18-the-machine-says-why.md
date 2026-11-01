# L5-18 — The machine says why: a selection explanation on every record (claude-2 → codex-6, 2026-11-01)

Authority: Joe, 2026-11-01 ~20:50Z: "the selections that are being returned are frankly a bit
bizarre, like interim Director doesn't really make a whole lot of sense at this point … why don't we
ask the machine why it is selecting what it's selecting, that's what these outer cascades are
supposed to be telling us". Lean: **no change**. This adds a carrier no requirement reads yet; the
record stays conforming. No new refusal, no new gate. Plain report of facts already on the record.

## What a reader cannot see today (the worked example)

Click 12 (`data/wm-runs/selection-record-2026-11-01-wm-click-7d89656e-b180-4f9e-bfb0-780d225b41de.edn`,
Giraffe run `20261101T195517.913Z-c38bc009`, pass dir `~/code/storage/giraffe/<run-id>/`). Two classes:

1. `find-out-before-building` (steps `[{:status-gate :undetermined} :by-rank]`) picked
   **M-interim-director**. Why: the gate left the 52 undetermined tasks; `:by-rank` took rank 1.
   Rank 1 was computed by the Giraffe pass (`futon3c/src/futon3c/giraffe/pass.clj` sort key:
   ready first, then −unblocks, −cone, source order) **before** the reading stage; at that point
   the task counted as ready and unblocked 9 downstream missions. The reading stage
   (`futon3c/src/futon3c/giraffe/excursions.clj` rows) then set status `:undetermined`, reason
   `:needs/dependency-reading`, prerequisite `M-interest-network-coupling` (evidence:
   `futon7/holes/M-interim-director.md` line 7, "Predecessor step (b) — event vocabulary committed in
   code — is the hard predecessor"), and **kept the rank**. 42 of the 52 undetermined tasks sit
   inside the ready region of the ranking this way.
2. `only-what-can-start-now` / `giraffe-order` (steps `[:ready-gate :by-cone :by-unblocks]`) picked
   **M-interim-director-proxy-metric-inventory**. Why: ready gate left 499; `:by-cone` put its cone
   of 9 (`M-autonomous-doc-maintenance … M-vsatarcs-writer`) first; `:by-unblocks` 1 (it unblocks
   M-interim-director). Runner-up on cone: E-memory-whitepaper-v2-programme (3).
3. Source facts a reader needs beside the pick: the mission header says `Date 2026-05-14`,
   `Status OPEN`, `Timebox 2 weeks`; the scaffold doc says the parent was `READY-TO-CLOSE`
   2026-05-19; §9 (2026-06-03) says close is Joe's call. None of this is on the record.
4. G: proxy-metric class 3.3896 = risk 3.0854 + ambiguity 0.4218 − novelty 0.1175; the
   interim-director class 3.2782 (predicts exactly what stopping predicts, so it carries the
   altitude after L5-16); abstention 3.2782 + altitude 10.

Every one of these facts is on the record or in the pinned pass. None is in words, and they are
in four places. The machine never says "I picked X because it is the ready task with the largest
cone (9), and its one unblock is a May 2026 mission that nine others declared as predecessor."

## Goal

`[:outer/selection :why]` on every selection record, typed, assembled only from the record and the
pinned Giraffe pass (no new sources, no invented values, no free text), plus a deterministic
rendered line per class and for the decision; the loop ledger row and alert bell carry the
rendered lines.

```clojure
:why {:schema :wm/selection-why-v1
      :classes
      [{:class "<class id>" :pick "M-interim-director" :cascades [:find-out-before-building]
        :steps [{:step {:status-gate :undetermined} :in 591 :out 52}
                {:step :by-rank :pick-value 1
                 :runner-up {:target "M-apm-capability-ratchet" :value 10} :margin 9}]
        :giraffe {:rank 1 :status :undetermined :reason :needs/dependency-reading
                  :unblocks nil :cone nil
                  :rank-basis :computed-before-reading      ; or :as-ranked when status :ready
                  :prerequisites ["M-interest-network-coupling"]
                  :evidence {:path … :line 7 :quote … :sha256 …}}
        :source {:path "/home/joe/code/futon7/holes/M-interim-director.md" :sha256 "aad85c…"
                 :pin :matches                              ; or :drifted
                 :header {:date "2026-05-14" :status "OPEN — HEAD authored …" :timebox "2 weeks maximum …"}}
        :terms {:risk … :ambiguity … :novelty … :altitude …} :G …
        :margin-to-next … :margin-to-abstention …
        :rendered "find-out-before-building picked M-interim-director: status gate :undetermined left 52 of 591; by-rank took rank 1 (runner-up M-apm-capability-ratchet at 10). Rank was computed before the reading stage demoted it (prerequisite M-interest-network-coupling unresolved). Header: dated 2026-05-14, status OPEN, timebox 2 weeks. G 3.2782 (+10 altitude: predicts stopping)."}
       …]
      :abstention {:terms … :G … :rendered "stopping: G 13.2782 = risk 3.2782 + altitude 10 (resources/wm/outer-abstention-altitude.edn b1e352…)"}
      :decision {:status :chosen :pick "…" :margin-to-abstention …
                 :rendered "chose M-interim-director-proxy-metric-inventory (G 3.3896) over stopping (13.2782) and 1 other class; margin to stopping 9.8886 nats"}}
```

- Header facts: parse only `**Date:**`, `**Status:**`, `**Timebox:**` (and `**Status**` without
  colon) from the first 30 lines of the pinned source; each `:absent` when missing. Compare the
  file's sha256 to the record's pin: `:pin :matches` or `:drifted` (then still parse, mark it).
- Runner-up: the second element after the ordering step, with its value; `:margin` the difference.
  For a gate step, `:in`/`:out` counts only.
- `:rank-basis`: `:computed-before-reading` when the Giraffe row status is not `:ready` yet has a
  rank; `:as-ranked` otherwise. (The ranking itself is not changed here; see Not in scope.)
- Rendered strings are produced by one pure function from the typed row. Tests compare whole
  strings. No LLM, no prose from any seat.

## Where

- `src/futon2/aif/outer_score.clj`: add `apply-selection-cascade-traced` returning
  `{:field … :trace [...]}` with the per-step counts and runner-up; `apply-selection-cascade`
  and `cascade-pick` stay as they are (Lean maps to them). The runtime calls the traced form once
  per constructed cascade for the explanation only; the pick used for scoring stays the untraced
  one, and a test asserts the two agree on every cascade of the click-12 record.
- `src/futon2/aif/outer_select.clj` (near where `:classes` and `:cascade-pick-relations` are
  assembled, ~line 157–172): build `:why` from classes, abstention, traces, the Giraffe order rows
  and dependency-reading rows already on the record (`:field-facts`/pass pin), and the header parse.
- `scripts/wm_click_repair_loop.py`: `click_record` already reads the record; add the rendered
  decision line and the class lines to the ledger `click` row (`why`) and to the alert bell text.
- `scripts/wm_run_facts.clj`: untouched (no Lean fact changes).

## Tests (live-pin rule: one pin per test from the click-12 record)

1. Traced cascade on the click-12 record: `find-out-before-building` → pick M-interim-director,
   gate out 52, by-rank value 1, runner-up value 10; `giraffe-order` → pick
   M-interim-director-proxy-metric-inventory, cone 9, runner-up cone 3, unblocks 1.
2. Traced pick equals untraced `cascade-pick` for all three cascades of that record.
3. `:rank-basis :computed-before-reading` for M-interim-director; `:as-ranked` for the proxy task.
4. Header parse of `futon7/holes/M-interim-director.md` → date `2026-05-14`, status begins `OPEN`,
   timebox begins `2 weeks`; a file without those lines → all `:absent`; sha mismatch → `:drifted`.
5. Rendered strings for both classes, the abstention and the decision equal the expected text.
6. Loop: a ledger row for a chosen click carries `why.decision` and `why.classes` (python test
   beside the existing loop tests).

Gates: clj-kondo, `futon4/dev/check-parens.el`, `clojure -X:test :nses '[…]'` for the touched
namespaces, loop python tests. Worktree `~/worktrees/futon2/codex6-l5-18`, branch `codex6/l5-18`
from `main` (**never check out a branch in `/home/joe/code/futon2`**, it is the serving source).
Commit; bell claude-2 with the sha, test receipt, and the four rendered lines for click 12.

## Not in scope (findings to report, not fix)

- Re-ranking after the reading stage demotes a task (42 undetermined tasks hold ready-region
  ranks). Report the count from the pass you read; the fix belongs to the Giraffe owner / L5-17-D.
- Whether M-interim-director is open. Joe's call; the record will now show the header so he can see it.
- Any change to the kernel, the cascades, or the G terms.
