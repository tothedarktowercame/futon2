Two tasks: find P1's first buildable prediction producer and consumer, and review codex-28's checklist UI change. I'll start with the new priority section in WORK-REMAINING, the checklist UI diff, and a search for the existing prediction modules from rows 6–11 and the machine-Q adapter.**P1 answer.** The first missing producer on P1's chain is the belief row for the target the controller actually selects. Every piece further along is built on a different kind of entity, so this is where the chain first breaks.

## Why this link comes first

1. **The controller picks missions and tickets.** Since 09-15 it selects over all open missions and live T- tickets (futon2 e9c70b9f, a24cb621).
2. **The belief doesn't cover them.** The belief's entities are only stack-annotation sections plus sorry-registry ids (`war_machine.clj:6114`, `bootstrap-from-stack-annotations`). The latest retained trace (`data/wm-trace/wm-trace-2026-09-12.edn`) has 414 rows in its carried belief (`:mu-post`), all `arxana/stack/…`, and none for any mission or ticket.
3. **So the row-7 reader refuses.** For any selected target it returns `{:ok false :refusal {:kind :missing-entity :path [:stored-belief <target>]}}` (`machine_belief.clj`, the `(nil? posterior)` branch). The whole chain (row 7 belief, row 8 B, row 9 Q(s|π), row 6 Q(o|π), then risk against C) stops there for every target the controller chooses.
4. **Rows 6–11 were exercised on a different kind of entity.** The row-7 witness entity is `arxana/stack/futon-v1/leaf/2/2`. Row 8's declared B and the row-9 policies use `:advance-mission` and `:apply-cascade`, which in production target missions. The modules exist, but no production entity joins them to the selected work.
5. **Where a mission does get a belief row, it's invented.** The forward model emits `{:entity-id <mission> :type :addressed}` for `:advance-mission` and `:advance-ticket`, and `belief/update-belief` gives an untracked entity `uniform-prior` before applying the event. I found no consumer of `:next-belief` in efe, policy, rollout, war_machine or epistemic_value, so today this changes no score. It is still a silent uniform fallback, and it should not become the source of belief.

## The ruling this needs, which I'm making as F13 owner

`SPEC-fundamentals-build-2026-09-12.md:118` lists "entity/joint State" as an unresolved model obligation for the packet owners and reviewer, not a question for Joe. I settle it as follows, subject to independent review of the declaration. Joe may overrule.

**S1. Each registry mission and ticket is its own belief entity** over the existing seven-status carrier. The key is the exact string the controller uses as the action's `:target` (`mission-target-id`, and the ticket's target id).
- This keeps the MachineModel contract, the row 7/8/9/6 code and the Lean `MachineBeliefState` unchanged. That Lean module takes its bootstrap domain as a parameter, so it needs no Lean edit.
- A mission-specific lifecycle-phase carrier would be a larger change: the validator's state-carrier check, the Lean carrier, and a new A and B. Tickets have no phases.

**S2. The domain is every registry mission and ticket, whatever its status,** not just the live ones. `reconcile-belief-carry` and Lean `carriedOnlyEntityIsDropped` both drop carried entities that are missing from a tick's bootstrap. A live-only domain would therefore erase a completed mission's belief at exactly the point where its outcome matters for A.

**S3. Declared observation mapping** from registry status-class transitions observed between ticks to status events. Authority is `:declared` with rationale, never "measured". Events go through the existing `apply-arena-belief-events` (the same `:aif` filter), so there is no new update law.

| Registry observation (previous → current) | Event |
|---|---|
| First observation of an id already in the registry | none (baseline recorded; the entity starts at declared D, `initial-prior-v1`) |
| An id newly appearing in the registry | `:spawned` |
| draft → any live class | `:refined` |
| Live class → a different live class (open/identify/active/partial/unknown; ticket `:live` changes) | `:refined` |
| Live → `:complete` | `:addressed` |
| Live → `:inactive` | `:foreclosed` |
| `:complete` or `:inactive` → live | `:reopened` |
| Unchanged, or the registry is unreadable | none; an unreadable registry is a typed refusal in the record |
| `:falsified`, `:strengthened` | never emitted from the registry (it has no signal for either) |

I propose codex-25 (independent observation lane) or zai-8 to review the S1–S3 declaration itself. The code reviewer below does not cover that.

## The coding handoff

- **Checklist codes:** WM-02 (and R1). It makes the target's post-update belief exist and reach row 7. It does not discharge WM-03, WM-04 or WM-05.
- **Author:** codex-22, which has row-7/row-14 context. codex-27 is an alternative. Not codex-26.
- **Reviewer:** claude-20.

**Commission this failing check first** (a test, and a receipt over retained bytes):
- The call: `machine-belief/belief-state-distribution` with the model-context `{:entity/id <a live mission target> :mode :single-entity :state-support machine-belief/state-support :model {...} :policy-entities [<same>]}`, over `:mu-post` from `wm-trace-2026-09-12.edn`.
- Today it returns `:missing-entity`.
- Repeat with a live T- ticket.
- Keep a non-registry id as the permanent negative control.

**Inputs:**
- `mission-registry/load-missions` and `load-tickets` (id, status-class, source path, and the sha256 of the bytes read);
- the previous tick's observed status-class snapshot, taken from the previous trace record;
- the carried `:mu-post`.

**Outputs, retained in the tick's trace record:**
- the bootstrap domain, now with all registry targets added to `extra-ids`;
- the observed snapshot `{target status-class}`, with its source pins;
- the emitted events `{:entity-id :type :weight 1.0 :evidence/id :evidence/time-provenance :source {:path :sha256 :previous :current}}`;
- the mapping-table revision.

**Where the code goes:**
- the `wm-belief-pre` binding at `scripts/futon2/report/war_machine.clj:6114`, the single integration edit;
- a new `futon2.aif.target-belief-evidence` that maps a pair of snapshots to events, as a pure function;
- the trace-record key for the snapshot.

**Invariants:**
1. Belief keys equal controller `:target` strings exactly.
2. A new entity gets the named, declared D, recorded in the provenance record, never an unlabelled uniform.
3. No event on first observation or on an unchanged status.
4. Only the S3 transitions emit.
5. Events are applied only through `apply-arena-belief-events`.
6. An unreadable registry produces a typed refusal, not the silent fallback to `extra-ids` that the stack-annotation read uses.

**Acceptance:**
- Rerun the failing check on the new bootstrap. It now returns `:ok` with a seven-status row for the mission and for the ticket, and its provenance is either D or carried. The non-registry id still refuses.
- One test per S3 row.
- A carry test: a mission completes, then disappears from the live set, and its belief survives.
- A trace readback of the snapshot and the events.

**Gates:**
- clj-kondo and `futon4/dev/check-parens.el` on the changed files;
- `clojure -X:test :nses '[futon2.aif.belief-test futon2.aif.machine-belief-test futon2.aif.mission-registry-test futon2.aif.target-belief-evidence-test]'`;
- the war-machine test namespace that covers `wm-belief-pre`.

**Receipts:** `holes/labs/wm-contract/runs/p1-target-belief-2026-09-15/` holds the before and after row-7 readbacks, the gate outputs and the commit shas.

**Separate steps, not part of this packet:**
- Serving activation: reloading `war_machine`, `belief` and the new namespace from master is allowed.
- A traced tick that shows registry targets in `:mu-post`: a click is Joe's call.

**Out of scope:**
- B coverage, A, and policy plans;
- `update-belief`'s silent uniform initialisation (report it as its own defect);
- the stack-annotation read fallback.

## The next blockers after this packet, in order

1. **B covers two action classes.** Row-8 B (`wm-status-action-prior-v1`) declares only `:advance-mission` and `:apply-cascade`. Live rankings also contain `:advance-ticket`, `:address-sorry`, `:fire-pattern`, `:learn-action-class` and `:no-op` (TN-row14-live-wiring-blocker, gap 3). Documented THEN interpretations are permitted (Sept 7 ruling, l.309), but each class needs a declared authority row.
2. **Q(s|π) is tied to A.** `controlled-transition-kernel` calls `machine-model/validate`, and `validate*` requires an admitted `:A` kernel. So neither B nor Q(s|π) can be produced until A is admitted, even though Q(s|π) doesn't depend on A mathematically (SPEC l.82). The candidate packet splits dynamics admission from observation admission.
3. **Measured A has zero observations.** The rubric has 0/7 qualifying statuses, and 0/86 retained closes can be licensed. Close retention's state port accepts only `:independent-categorical-observation` of the target's status, and refuses the belief as state (`close_retention.clj:112-120`). Before this packet, missions and tickets had no status carrier at all, so no (state, disposition) pair could exist for the targets being closed. After it, the carrier exists, but an independent observation producer is still needed (codex-22/25 lane).
4. **No policy plan per candidate.** The judge ranks actions, not versioned full-policy plans, and nothing maps a candidate occurrence to one (TN-row14, gap 2).

## Review of the UI change (p4ng 8c2296f)

No specific defect found.

**What I checked:**
- I rendered it onto a copy of the live page. All 14 `<details>` and 14 `<summary>` pairs balance.
- An HTML-parser nesting check found no errors and no unclosed tags.
- Only h2/h3 headings sit inside `<summary>`, which HTML permits.
- All 103 `#ck-` anchors are present, with one script.
- `data-md-sha` still matches R4 (`466a2e19…`), and the R4 Markdown bytes are unchanged.
- The reveal walks every `DETAILS` ancestor, including the outer one, so a link into a collapsed section opens it.

**Residuals, none of them a defect:**
- The hash isn't URI-decoded. That's harmless, because all codes are ASCII.
- `scrollIntoView()` repeats the browser's own scroll to a fragment. Also harmless.
- Neither of us tested it in a real browser.
