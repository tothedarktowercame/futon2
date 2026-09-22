# Proof reference field (⟨1⟩2 CHECK snapshot)

Frozen 2026-09-22 by zai-1. **Inputs only — no expected answer is recorded
here.** The reference target is chosen (front repair ticket); which candidate
wins is what steps ⟨1⟩3–⟨1⟩5 demonstrate on these inputs. Evaluating this
snapshot read-only never executes its work; the work is executed once, at
⟨1⟩5.

## B0 (baseline commits)

- futon2: `80608d0bc4e90b6423a7948c36822aafbad3c564` (this snapshot's commit
  is the freeze itself)
- futon3c: `826a9a59ee7f635a1fa2fe5dec23ef853e5db490`
- mathlib4: unchanged by this step (no locator in this field reads it); the
  contract bundle is A1 of the proof plan.

## The target and its eligibility

- Target: `T-repair-occ-444fb018cbbb656d09b8f4f67c063f1d51a1932a9b1c281d999c567cf22a2ade`
  (copy: `ticket.md`, sha256
  `0a8626dfc338b93b2337f0b5a1cedaed41736a805d711fb958e2d55278450aad`,
  committed at futon2 `ae69f5e7`).
- Queue state: `queue.edn.snapshot` (sha256
  `a1fd0ebc80c2e6fcd6301c01e60e76d6abf8a262ab6cb39223772d5715e0c59b`) —
  the ticket is the ONLY entry, at the front, inserted 2026-09-22T05:13:31.54Z.
- Eligible stratum: per `ticket_queue.clj:89-115` (`plan`), the single front
  entry becomes the admitted eligible stratum once candidates for its target
  are admitted; with the cascade source below, `policy.clj:358-374` feeds it
  as the eligible target. Admission verified read-only 2026-09-22:
  `:admission/task-stated true` (C4 at HEAD), 2 constructed candidates.

## The cascade source and the two candidates

- Source: `cascade-source.snapshot.edn` (copy of
  `resources/wm/cascade-sources/T-repair-occ-444fb018.edn`, sha256
  `500c6add5601b141638f9a90894c97d1fb80ea0817b9b377ba89f861e2abb98d`,
  published at futon2 `22b1971c`, locator paths fixed at `80608d0b`).
- **:C1** `[:apparatus/done-is-observed-running]` — first action: run the
  precondition probe, retain the dated disposition. Declared effect:
  `:repair/verified-dated-recheck`, at
  `resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn` (C3).
- **:C2** `[:aif/declare-the-conditioning]` — first action: author and
  commit the preregistered held-out split declaration with no-result
  handling. Declared effect: `:repair/restored-held-out-split`, at
  `resources/wm/eig/held-out-split.edn` (C3).
- Locators (all resolve at B0: task-stated observed **true**; both produced
  tokens observed **false** — the work is not yet done, which is the point).

## Feasibility (⟨2⟩3) — verdicts

- **:C1 FEASIBLE.** Writes one EDN record at
  `resources/wm/rechecks/repair-occ-444fb018-dated-recheck.edn` (directory
  whitelisted at `80608d0b`). Acceptance it runs: the C3 locator at HEAD
  (git `cat-file`), plus ordinary ticket review of the recheck content
  (ticket's "Acceptance evidence" section). Evidence it needs exists now:
  the finding record's statement of the refused precondition, click-3
  STAGES, the withdrawn route's reason (`8f97757b`). One repository
  (futon2). Nothing needs mathlib4, another repo, or evidence that does not
  exist.
- **:C2 FEASIBLE.** Writes one EDN declaration at
  `resources/wm/eig/held-out-split.edn` (whitelisted at `80608d0b`). Its
  acceptance is declaration review (parent mission C2's no-result/failure/
  timeout classes; the withdrawn route names this split as its
  precondition). It explicitly does NOT produce post-split outcomes, so it
  cannot fail on click-3's missing-evidence ground — that failure belonged
  to the calibration route, not to authoring the split. One repository
  (futon2), evidence exists (the pattern file, the mission text, the
  withdrawal note).
- FEASIBILITY DEFECT FOUND AND FIXED at `80608d0b`: both produced tokens
  originally pointed into gitignored paths (`data/*`, `resources/wm/*`),
  and C3 reads git at a commit — they could never have been observed true.
  Relocated/whitelisted; admission re-verified.

## Past outcome evidence (⟨2⟩4, per Joe's ruling 444c8f3f)

- **Compatible recorded outcomes: NONE** for this target's action families
  (`apparatus/done-is-observed-running` has no trials; the repair family has
  none).
- Learning-ledger state at freeze: `data/wm-learning-trials/attempts.edn`,
  sha256 `2a9402d4875190637ea7576bbb5a3c91dbe4939d4ca4eff0d265ea2a4b3ca03f`
  — exactly one trial (`apparatus/one-authority-per-question` →
  M-aif-policy-conditioned-eig shared-updater hole, success 1, `:consumption
  :not-authorized`), a different pattern and token; not claimed as evidence.
- B is therefore the declared prior for both candidates, distinguished by
  their declared effects.

## Model configuration selection reads for this input

- `resources/wm/cascade-sources/*.edn` (all five; the ticket source above is
  the one that changed — others pinned by B0's tree).
- `data/wm-ticket-queue/queue.edn` (pinned above).
- The cascade source's own `:beta {:value 1 :status :declared}`, guard and
  precedence declarations, and the two pattern receipt sources
  (`futon3/library/apparatus/done-is-observed-running.flexiarg`,
  `futon3/library/aif/declare-the-conditioning.flexiarg`), whose sha256 is
  pinned by `read-receipt-source` at load.
- No `:c-schedule`, `:lam`, `:mu` or `:token-initialization` entries are
  declared in this source; their absence is part of the frozen input.
