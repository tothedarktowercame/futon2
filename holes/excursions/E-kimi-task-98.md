# E-kimi-task-98 — HG2-Ia: field entries carry delta-G_t or typed absence (H-G-target part 2, field side)

**Requisition:** completed — 2026-09-27T03:17:51Z, job invoke-1790477763602-25256-f86b8560, state done

**VERDICT (2026-10-09, provisional):** DONE — Requisition header and git commit both record the kimi job completed with state done; spot-check of task-98 found its deliverable (ready-delta-g) in src. _(WM status classification by zai-1, high confidence; not yet confirmed by the author.)_

Clocked in by claude-8 for kimi-1 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# HG2-Ia: the field side of H-G-target part 2 — a `:ready` entry carries its ΔG_t (value + universe) or the typed absence (⟨1⟩3 of PROOF-2a-PLAN; first implementation packet after HG2-D)

HG2-Ia (claude-8 → kimi-1), under the coding-handoff protocol: you implement, claude-8 reviews; bell claude-8 back with the futon2 commit sha, the warrant ids and the report below. Your HG2-D (E-kimi-task-96) fixed the boundary: the field gains a per-entry `:delta-g`, the law stays in `outer_cascade.clj` (HG2-Ib, after this). Under ~250 lines including tests. No change to `outer_cascade.clj`, no runner, no map, no registry, no flight, no click, no load into :6768. Joe's direction 9df277b13 and the mixture law d615d06e need no further ruling (HG2-D §v, agreed by claude-8).

Two corrections to HG2-D's premises, from claude-8's read of the source before this packet:
1. `assess` (`target_field.clj:300-316`) calls `ic/support` ONLY — the support step "before any G" (`interpretation_construction.clj:101-110`), which returns `{:status :supported :family …}` and never a receipt or a baseline G. The baseline G, the constructor run and the receipt with `:g-of-best {:value :universe}` live in `ic/construct` (`interpretation_construction.clj` from ~:170: support, then the NONFINITE-G left-out rule, `baseline-g`, `construction/construct` with `:initial-family [baseline]`, the receipt). `assess` never calls it. So ΔG_t requires calling `ic/construct` for the `:ready` entries — exactly the direction's "construct first but not for all 343: ΔG is defined only for :ready targets".
2. `ic/construct` needs `:evaluate-g` (and `:move-cost`, `:budget`, `:horizon`). The field's caller supplies these under `(:construction sources)` in the convention `cascade_problems.clj:102-107` documents (`{:construct … :budget … :move-cost … :evaluate-g (fn [problem candidate] G)}`); trace whether `target-field`'s `sources` (from `flight_driver.clj` / `outer_loop`) carry `:construction` at HEAD. If they do, use it; if they do not, the entry records `:delta-g {:absent :no-evaluator-supplied}` — a typed absence, never an evaluator invented in the field, and say so in the report (that is a wiring finding for the caller, not this packet).
3. `compare-g` is PRIVATE in `construction.clj` (:79-91). Do not copy it: expose one public function (`construction/delta-g [baseline-g g-of-best]` or similar) that calls it, so the field's ΔG is the SAME comparison the constructor records (`{:delta … :universe …}` or `{:incommensurable {:universes [ua ub]}}`). Say where the baseline's G with its universe is available after `construct` (the receipt, or `baseline-g` in `ic/construct`'s scope — if only the latter, return it on `ic/construct`'s `:constructed` result beside `:candidates`, one key, named).

The behaviour (`target_field.clj`, `assess`'s `:ready` branch or a `with-delta-g` enrichment applied to `:ready` entries — say which, and why): a `:ready` entry gains `:delta-g`, one of
- `{:value Δ :universe U :receipt-digest <sha256 of the construction receipt> :baseline-g … :g-of-best …}` when `ic/construct` returns `:constructed` and the comparison is commensurable (Δ = baseline − best over the same universe, positive meaning the constructed candidate improved, as `compare-g` defines it);
- `{:absent :incommensurable :universes [ua ub]}` when the universes differ;
- `{:absent :no-constructed-candidate :reason <construct's refusal kind or :construction-not-taken> :next-step …}` when construction refuses or takes no move;
- `{:absent :no-evaluator-supplied}` per correction 2.
Non-`:ready` entries carry `{:absent :no-constructed-candidate :next-step <the entry's next step>}`. Never a number standing in; `:universe` on a value entry is the receipt's own. `with-pair-overlap` and every other reader are untouched.

Tests (`target_field_test.clj`, extend; keep the 14 existing deftests green unchanged): a `:ready` target with a supplied evaluator (reuse the constructor fixtures `interpretation_construction_test` / `construction_test` use — say which) records a numeric Δ with the receipt's universe and digest; the same target with the evaluator removed from sources records `:no-evaluator-supplied`; a non-ready target records the typed absence with its `:next-step`; an incommensurable case (universe on the baseline ≠ universe on the best — construct it by the fixture, do not stub `compare-g`) records `:incommensurable`, never a number; the record of an entry with a value is replayable (same inputs ⇒ identical `:delta-g`).

Gates: clj-kondo 0/0; `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` OK; run once each in futon2's own JVM from `/home/joe/code/futon2` with `clojure -M:test -m cognitect.test-runner -n <ns>`: `target-field-test`, `target-field-overlap-test`, `outer-cascade-test`, `construction-test`, `interpretation-construction-test` (or whatever pins `ic/construct`; say which). Commit by explicit path (never `git add -A`, never `--amend`, never `git stash`; other lanes' files untouched — `git status` first and list what is dirty that is not yours); register ONCE each from the futon2 root after the commit with `AUTHOR=kimi-1 /home/joe/code/futon2/scripts/wm/register-warrant.sh --pinned <sha> <ns>` for every namespace you ran whose source changed. Nothing under `data/`.

Report: sha and warrant ids; where `:delta-g` is attached and where the evaluator came from at HEAD (the route, or the typed absence and the wiring finding); the public comparison function's name; one value entry and one absence entry pr-str; the line count of src + test; anything you found that is not yours, unfixed.
