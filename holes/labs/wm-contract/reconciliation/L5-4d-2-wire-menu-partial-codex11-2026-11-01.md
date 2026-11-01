# L5-4d-2: constructed menu and retained pending replay (items 0–2)

**Stopped at the expressly authorized item-2 boundary.** Base futon2 `743209eca`, futon3 `e7a951e`. Neither branch merged or reloaded. No click, no live makeup/state migration. Items 3–5 remain unimplemented in this packet; the real retained field still refuses on discovery gain, as a new test proves.

## Changes

- Mapping version 2: `what-others-wait-on-first` is an alias of `giraffe-order`. Its README citation and interpretation remain. Aliases must point directly to executable entries, never another alias or a missing id; they are not menu members. Constructor groups executable cascades by `[steps units]` and refuses `:duplicate-policy` with both identities if a duplicate is reintroduced. Live-library menu count is three.
- `outer_select.clj:55`: calls the real constructor. The makeup's `:cascades` is never used for selection. Records retain constructor units/edges/digests and complete `:construction`; chosen identity additionally carries `:cascade-digest`. Scored entries keep all candidate predictions and terms. A poisoned makeup menu is ignored in the new unstubbed test.
- `outer_score.clj:31`: interpreter now accepts recorded `{:status-gate s}` as well as legacy `{:gate-status s}`. Typed serialization keeps the recorded form.
- `outer_state.clj:175`: pending recovery uses the chosen digest to locate exactly one retained entry and recomputes its composition digest. Older entries use their retained typed steps (from the matched entry or top-level steps). No fallback to today's makeup or menu index. Missing, ambiguous or corrupted recovery evidence refuses `:pending-steps-unrecoverable`. Empty retained step vectors remain valid no-op legacy cascades; named/functions are not executable retained steps.
- `outer_state.clj:300`: pure makeup migration admits **menu removal only** in addition to the existing precision extension, and refuses any simultaneous parameter change. It preserves all learned values and existing hash/receipt checks. Branch-copy makeup loses only `:cascades`; the live file remains Joe's to change. Pending entries with no retained steps remain unrecoverable even after a pin migration; nothing fabricates them from the old menu.

## Tests and their boundary

The new `outer_menu_wiring_test.clj` uses the **real constructor and selector**, not a stub: menu provenance, ignoring a poisoned makeup menu, alias exclusion, duplicate/alias negatives, replay independent of index/current menu, legacy replay, tampered/missing replay refusal, and count-preserving menu-only migration.

The prior small-menu scoring tests deliberately used arbitrary handwritten menus (including function gates, one-member and tied menus). They now use an explicit test-only constructor fixture in `outer_menu_fixture.clj`, via `with-redefs` of the constructor, to keep those scoring/retention laws isolated. These are not proof of production construction. Affected namespaces: outer-select, outer-observation, outer-relation-admission, outer-selection-wiring, outer-state-wiring. The legacy fixture identity test was renamed accordingly. New unstubbed wiring and constructor tests do not install that fixture. Old state-update and state-wiring fixtures now retain their own `:steps` rather than borrow them from makeup.

Retained pass hash is checked before the full 609-row pass is supplied to the real selector: `a9ad27a8cb6fd0c808413ce639019b117aed746fbcb6b2945560cfc71423ff91`. This test **expects and observes** `:outer-score/graph-term-unavailable`, `:term :unblocks`, with the three-policy construction attached. It is a blocker witness, not a successful full 626-task selection. Undetermined gain handling is item 3, not silently implemented here.

## Remaining work / nine Lean outcomes

- Item 3: `outer_score.clj` outer-model still derives gain from unblocks. Implement `:undetermined → :none`, retain `:gain-source :discovery-yields-no-ready`, and update the kernel carrier if its semantics change. Replace the blocker test with a positive real-field witness.
- Item 4: `scripts/wm_run_facts.clj:823–845,931–974` still projects historical makeup positions. Add modern construction/digest projection with manifest/unit checks and preserve fallback. A modern menu-less makeup is not yet projectable through that old branch.
- Item 5: **checker and Lean not run; all nine outcomes unestablished for a new selection record**. No `.known`, `holds`, or conformance claim. Do not spend a click on this partial branch.
- Live installation remains separate: mapping v2 first; supported source reload; explicit live makeup/pending-state migration with retained evidence. This packet performs none of those operations.

## Validation

### baseline

Ran 146 tests containing 581 assertions.
17 failures, 2 errors.

Log `/home/joe/notes/l5-4d-2/baseline.log`; sha256 `217d5051870ddcdc2e40be2219b6bc97f5eb67b6ba5c410d41a27bc93512f171`. Canonical data files 45737 → 45737; exit 1.

### focused

Ran 15 tests containing 72 assertions.
0 failures, 0 errors.

Log `/home/joe/notes/l5-4d-2/focused.log`; sha256 `9deba1e2188641fcf361a6483dd5784bfa7890a62f08e285ad987a4752b47a02`. Canonical data files 45737 → 45737; exit 0.

### final

Ran 152 tests containing 602 assertions.
17 failures, 2 errors.

Log `/home/joe/notes/l5-4d-2/final.log`; sha256 `f2aeb17b0fdb3df654f1ebc27cce4a882c994441077a84d6eb8ff983f58b01b8`. Canonical data files 45737 → 45737; exit 1.

Final suite has identical failure-name multiplicities to main: 17 pre-existing wiring/learning assertions and two historical Giraffe verification errors. Six new wiring tests add 21 assertions, none failing. Focused run preceded the final retained-field blocker test; the final requested suite includes it. All modified/new Clojure files: kondo zero errors/warnings, Emacs parens OK, diff whitespace check clean.

Branch test environment additionally set `L54D_LIBRARY_ROOT=/home/joe/worktrees/futon3-l5-4d-2/library/task-selection`; baseline used v1. FUTON_WM overrides for final run:

```json
{
  "FUTON_WM_LEARNING_TRIAL_LEDGER_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_LEARNING_TRIAL_LEDGER_ROOT",
  "FUTON_WM_META_INJURY_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_META_INJURY_ROOT",
  "FUTON_WM_MORNING_BRIEF_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_MORNING_BRIEF_ROOT",
  "FUTON_WM_PHASE_LOG": "/tmp/l5-4d-2-final/FUTON_WM_PHASE_LOG",
  "FUTON_WM_PHASE_REPLAY_INPUT_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_PHASE_REPLAY_INPUT_ROOT",
  "FUTON_WM_REPAIR_OBLIGATION_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_REPAIR_OBLIGATION_ROOT",
  "FUTON_WM_RUN_CONFIG": "/tmp/l5-4d-2-final/FUTON_WM_RUN_CONFIG",
  "FUTON_WM_RUN_LOCK": "/tmp/l5-4d-2-final/FUTON_WM_RUN_LOCK",
  "FUTON_WM_RUN_RECORD_DIR": "/tmp/l5-4d-2-final/FUTON_WM_RUN_RECORD_DIR",
  "FUTON_WM_TICKET_PUBLICATION_ROOT": "/tmp/l5-4d-2-final/FUTON_WM_TICKET_PUBLICATION_ROOT",
  "FUTON_WM_TRACE_DIR": "/tmp/l5-4d-2-final/FUTON_WM_TRACE_DIR"
}
```

## Exact branch-only makeup diff

Live SHA after tests remains `f312583522597c49b4134b08c4a186a013baaaa12a6f1c332fe9875e57f58e3a`. Canonical file count is not a full-byte audit.

```diff
diff --git a/data/wm-learning/outer-makeup.edn b/data/wm-learning/outer-makeup.edn
index f1a96a16e..f5f8be337 100644
--- a/data/wm-learning/outer-makeup.edn
+++ b/data/wm-learning/outer-makeup.edn
@@ -27,16 +27,4 @@
  :omega-gain 9/10
  ;; Runtime task reach is absent; this explicit starter value records that current modelling limit.
  :default-reach 1/2
- :cascades
- [;; futon3/library/task-selection/only-what-can-start-now.flexiarg and what-others-wait-on-first.flexiarg.
-  {:id :what-others-wait-on-first
-   :patterns ["only-what-can-start-now" "what-others-wait-on-first"]
-   :steps [:ready-gate :by-cone :by-unblocks]}
-  ;; futon3/library/task-selection/only-what-can-start-now.flexiarg.
-  {:id :only-what-can-start-now
-   :patterns ["only-what-can-start-now"]
-   :steps [:ready-gate]}
-  ;; futon3/library/task-selection/find-out-before-building.flexiarg.
-  {:id :find-out-before-building
-   :patterns ["find-out-before-building"]
-   :steps [{:gate-status :undetermined} :by-cone]}]}
+}
```

## Mapping v2 diff

```diff
diff --git a/library/task-selection/executable-steps.edn b/library/task-selection/executable-steps.edn
index b95e680..f07b2ec 100644
--- a/library/task-selection/executable-steps.edn
+++ b/library/task-selection/executable-steps.edn
@@ -1,9 +1,9 @@
 {:schema :wm/task-selection-executable-steps-v1
- :version 1
+ :version 2
  :readme-sha256 "51d2f2784b36afd065e680146d8d1358e83f62edf224a88c6947e321cd6b8337"
  :interpretation-authority "L5-4d-impl, claude-2 ruling job 9326; review required, not a transcription of executable library code"
  :cascades
- [{:id :what-others-wait-on-first :version 1 :status :executable
+ [{:id :what-others-wait-on-first :version 2 :status :alias :alias-of :giraffe-order
    :readme-cascade 1 :readme-lines [55 58]
    :realises {:row 1 :component true :full-row-executable true}
    :interpretation "Retained makeup component: ready, stable cone then stable unblocks. Cone is now explicitly bound to prepare-the-ground. Last ordering is primary."
```
