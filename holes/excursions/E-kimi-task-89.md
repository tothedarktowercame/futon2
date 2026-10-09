# E-kimi-task-89 — ITEM6-ADAPTER-I: seven-column accumulation BMR adapter + aggregate score receipt (PROOF-2a <1>2 item 6, prototype label)

**Requisition:** completed — 2026-09-27T02:08:45Z, job invoke-1790474640347-25229-4ae210c4, state done

**VERDICT (2026-10-09, provisional):** DONE — Header states the requisition is completed with job id and timestamp. _(WM status classification by zai-5, high confidence; not yet confirmed by the author.)_

Clocked in by claude-8 for kimi-4 on 2026-09-27 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# ITEM6-ADAPTER-I: the explicit seven-column adapter and the aggregate score, as a replayable receipt (⟨1⟩2 item 6 of PROOF-2a-PLAN)

ITEM6-ADAPTER-I (claude-8 → kimi-4), under the coding-handoff protocol: you implement, claude-8 reviews; report the futon2 commit sha and warrant id. Joe's rulings (2026-09-27 01:08Z and ~01:40Z; registry `:model-reduction` row at futon2 cbfce5d9f — READ IT FIRST: `:declared-model`, `:rationale`, `:ruling`): the seven channel-given-status reading is declared provisionally; a replayable reduction score consumed and recorded suffices; adoption deferred; positive soft-prior proposals first; the whole thing carries the label "prototyping our way forward, not closure of the proof". The discovery it rests on: `holes/labs/wm-contract/ITEM6-NORMALIZER-D.md` (codex-2, futon2 8d58215f5) — its §2 fixture and §5 findings define your test and your bad cases. This packet is the ADAPTER + SCORER only; no consumer wiring, no registry edit, no adoption.

Build `src/futon2/aif/accumulation_bmr.clj` (namespace `futon2.aif.accumulation-bmr`), pure, no IO:
- `factors [state]` — from the machine accumulation state (`machine_accumulation.clj`: `{channel {status concentration}}` with its ordered supports; read the state shape and the initialization's scalar prior there; name the lines) to SEVEN ordered Dirichlet vectors, one per status, each over the fourteen channels in the DECLARED support order (never alphabetical by accident: refuse `{:status :refused :kind :unordered-supports}` when the state carries no order). Missing cell → typed absence `{:status :absent :kind :missing-cell :channel c :status-key s}`, never 0 or the prior.
- `parent-prior [state]` — the actual declared initialization as seven vectors (the scalar prior broadcast is what the state records; say so in the docstring); no invented prior.
- `proposal [state deltas]` — a positive soft-prior constraint: `deltas` is `{[channel status] d}` with every d a positive rational; the reduced prior a′ = a + deltas; refuse `:kind :non-positive-delta` otherwise (ruling (3): no merging, no negative deltas).
- `score [state deltas]` — for each of the seven factors, `bmr.clj`'s existing `ln B` machinery (name the fn and lines you call; do NOT reimplement log-Γ) on (a, A, a′, A′); the aggregate ΔF = the sum of the seven differences; returns a replayable receipt `{:schema :wm/accumulation-bmr-score-v1 :declared-model :channel-given-status :factors 7 :label "prototyping our way forward, not closure of the proof" :per-factor [{:status s :ln-b {:a :A :a' :A'} :delta-f …} ×7] :delta-f <sum> :inputs {:state-digest :deltas-digest :support-order} :rule {:threshold -3 :applied false}}`. Adoption is NOT decided here: no `:accepted?` key, the threshold is recorded as not applied.

Tests, `test/futon2/aif/accumulation_bmr_test.clj`:
- The ITEM6-NORMALIZER-D §2 fixture reproduced exactly (all-ones, one tick with o_c = c/14, μ_s = s/28 through the REAL accumulator — `machine_accumulation.clj`'s step, not a hand-built matrix — so A[c,s] = 1 + c·s/392; the same proposal, +1 at [:active-repo-ratio :addressed]): `:delta-f` = 0.016404153337 to 1e-9, and the per-factor vector shows the one changed factor carrying all of it; and the two OTHER readings' scores (0.071195702167 joint, 0.007604599385 status-given-channel) are NOT what this returns — assert both inequalities, so the adapter's factorisation is pinned and cannot silently drift to a flattened or per-row call.
- Bad cases: a state without support order → refused; a missing cell → typed absence on the receipt and no `:delta-f`; a negative or zero delta → refused; a delta on a cell outside the supports → refused `:kind :unknown-cell`; replay: the same inputs give the same receipt (pr-str-identical), different deltas give a different `:deltas-digest`.
- The parent prior is the state's declared initialization (read it back from a state produced by `machine_accumulation`'s initialization; assert the seven vectors are that prior, not uniform-by-assumption).

Gates: clj-kondo 0/0 on both files; `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el <file>` OK; run once in futon2's own JVM from `/home/joe/code/futon2` (never :6768): the new namespace and `bmr-test` (or whatever pins bmr.clj; unchanged). Commit by explicit path (the two new files only; never `git add -A`, never `--amend`, never `git stash`; `git status` first — other lanes may have futon2 files open, do not stage them); register ONCE from the futon2 root after the commit: `AUTHOR=kimi-4 /home/joe/code/futon2/scripts/wm/register-warrant.sh --pinned <sha> futon2.aif.accumulation-bmr-test`. No registry edit, no consumer, no flight, no click, no load into :6768, nothing under `data/`.

Report: sha and warrant id; the receipt for the fixture (pr-str, trimmed to the per-factor summary); the bmr.clj functions and lines you call; the state shape and support-order source you read (lines); anything you found that is not yours (an accumulator state that carries no support order in production, for instance), unfixed.
