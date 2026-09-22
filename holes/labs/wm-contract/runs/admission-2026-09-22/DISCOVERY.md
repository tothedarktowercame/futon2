# Why the live family has one candidate

Discovery against futon2 main `91b467f5ae13fafe7cac97b8b0c26ab30744bd41`, branch `fix/admission-supply`, 2026-09-22. No production edits, clicks, serving-JVM evaluation, retrieval generation, ticket publication, or store writes.

**The running tick consumes declarations; it does not author them.** Retrieval supplies evidence for an author, not executable cascades. The generic mission-hole step already supplies universes and locators for checkboxes, but deliberately supplies no pattern interpretation or candidate. The immediate deficit is target-specific interpretation/construction, plus an unwired preselection authoring workflow. Creating another generic universe generator would duplicate existing work and leave selection empty.

Two corrections to the question matter:

- Click 2 has **262**, not 261, missing-universe refusals: **198 mission targets, 22 ordinary ticket targets, and 42 repair-ticket targets**. They are not all missions needing the same missing step.
- M-aif-policy-conditioned-eig is not out of unchecked work: **two checkboxes remain open**. Its declaration's sole pattern produces only the already-completed updater token. M-wm-08-external-f2 is exhausted because its report artifact exists, not because all its mission checkboxes were ticked.

## 1. The 90 proposals and the missing author

Retained evidence:

- `data/wm-runs/tick-run-record-2026-09-21-1790033693.edn`
- `data/wm-runs/tick-run-record-2026-09-22-1790037762.edn`
- In each, `[:decision :selection-certificate :proposal-supply]`, especially `:proposals`, `:admissions`, `:declines`, `:files`, and `:repair-scan`.
- `data/wm-trace/wm-trace-2026-09-22.edn`, the form with `:run/id "2026-09-22-1790037762"`, `[:cascade-problems :refusals]`.

| Retained quantity | Click 1, 1790033693 | Click 2, 1790037762 |
|---|---:|---:|
| Retrieval proposals | 48 | 48 |
| Distinct retrieval targets | 1 | 1 |
| Repair proposals | 41 | 42 |
| Total proposals | 89 | 90 |
| Proposal admission joins | 0 | 0 |
| Malformed repair findings declined before proposal creation | 1 | 1 |
| Open findings in supply snapshot | 42 | 43 |

All 48 retrieval proposals target **M-a-wmc-scaling**. They are 48 distinct retrieved pattern identities for one target, not 48 executable alternatives and not 48 missions. Both clicks cite the same bundle:

`data/wm-cascade-proposals/8961ab24-0c3a-4e7c-adbd-7413b5e3e5fa/proposal.edn`.

Example retrieved identities include `aif/term-to-channel-traceability`, `pattern-interpretation/put-uncertainty-in-theta`, `peripherals/read-existing-seam-before-implementing`, and `data-mining/smoke-before-the-paid-run`. Their presence is evidence of retrieval, **not an applicability judgment**. No proposed interpretation of these patterns is admitted by this report.

The additional rejected finding is `repair-attempt-054`: its retained decline names missing `:failure-kind`, `:failure-stage`, `:discharge-contract`, contract `:requires`, and `:artifact-shape`. It is not part of the 90. These are the historical supply counts; they must not be substituted for today's open-obligation count.

The actual pipeline and authority, with file:line references at the pinned main:

| Step | Code authority | What it does / does not do |
|---|---|---|
| Retrieve and pin candidate bytes | `src/futon2/aif/interpretation_request.clj:257`, `prepare-proposal!`; `cascade_proposals.clj:50`, `generate-retrieval!` | Explicit preselection capture outside the tick; persists evidence. The docstring says no agent is invoked or impersonated. The generator's caller in production source is its CLI, not the tick. None was invoked here. |
| Read retained proposals | `cascade_proposals.clj:103`, `load-proposals`; `:115`, `load-supply` | Rechecks captured bytes, rederives proposals, adds a fresh repair scan. It does not create interpretations. |
| Author a complete declaration | `cascade_sources.clj:106`, `check-file!`; `:133`, `load-declared` | Requires target, context/beta, facts, wants, locators, interpreted patterns, interpretation receipts and constructed candidates. Observes declared facts and pins interpretation source bytes. An author must supply the substantive reading, guards, produces and orders. |
| Join proposal evidence to an authored declaration | `cascade_proposals.clj:130–155`, `record-supply` | If a receipt contains `:proposal-id`, checks target, pattern identity and pinned pattern SHA256, then records `:authority :agent-authored-declaration`. It **does not write** the receipt or promote a proposal into a source. |
| Admit and select | `scripts/futon2/report/war_machine.clj:6349`, `admit-cascade-problem`; `:6402`, `cascade-decision` | Requires nonempty interpreted/receipted orders and newly achievable wanted tokens within the horizon. Only surviving candidates are scored. |

The join is **optional provenance**, not the whole executable-admission test: a fully authored declaration can be admitted without a `:proposal-id`. Conversely, a matching proposal join alone does not supply locators, useful progress or a valid constructed order.

A source/caller search across futon2 `src/`, `scripts/`, `resources/wm/cascade-sources/` and futon3c `src/` found **no running preselection agent-dispatch-and-publication step** for these receipts. `interpretation_request.clj:1` explicitly describes an unjudged request with no construction/runner caller. `war_machine.clj:6810–6849` loads declarations and supply, assembles, and records their relationship; it never asks an interpreter to fill a missing declaration. The runner's author prompt (`full_loop_runner.clj:1890`, selected target at `:1923`) is downstream of selection, so it cannot supply the alternatives needed for that same selection.

All four checked-in declarations are hand-authored and none contains `:proposal-id`: codex-1 for M-aif-policy-conditioned-eig and M-f11-find-production-successor, codex-3 for M-expressions-of-interest, claude-4 for M-wm-08-external-f2. This establishes the provenance of the current executable sources, not an exhaustive claim about every historical agent action. Adding more retrieval bundles alone will make the pending evidence list longer.

Repair proposals have an additional explicit hold. `repair_proposals.clj:54–63` records closure observation as unavailable because an unversioned resolution has no admitted locator. `cascade_proposals.clj:136–145,168–174` removes assembled open-repair targets even if someone supplied a declaration, records `:repair-closure-observation-unavailable`, and excludes them from proposal admission joins. This is not merely a missing retrieval hit.

## 2. Missing universes versus missing interpretations

The click-2 trace's 282 refusal rows split exactly as follows:

| Refusal | Missing field/evidence | Count |
|---|---|---:|
| `:universe-not-admitted` | `:universes` | 262 |
| `:no-admitted-interpretation` | `:interpretations` | 17 |
| `:universe-not-admitted` | `:locators` | 1 |
| `:no-constructed-candidate` | `[:new-wanted-token-within-horizon]` | 2 |

Together with the one selected candidate this accounts for the 283 considered targets. These counts match the [click-2 narrative](../click-r4-2-2026-09-22/narrative.md), selection section. The locator failure is M-expressions-of-interest; missing tokens include `:change-authored-and-bound`, `:obligation-resolved-through-the-account`, and `:premise-refused-before-work` (also retained in [fix-16's census](../fix-16-2026-09-21/README.md)).

`mission_hole_wants.clj:74–91`, `mission-source`, already produces:

```clojure
{:universe (zipmap tokens (repeat false))
 :want tokens
 :locators <C4 checks for the checked forms of the mission's open checkboxes>
 :interpretation {:patterns {} :receipts {}}
 :candidates []}
```

The producer uses the mission registry's unchecked-task records; these are declaration-shaped observations of checked completion statements, not evidence of the truth of completed engineering. `merge-into-sources` at `:118–176` inserts these universes, wants and locators, inherits the one agreed family schedule/scales, and defers entirely to an existing handwritten target declaration. It does **not** discard a generated universe merely because interpretation is missing; assembly then correctly refuses it at the next boundary.

The click records' `:mission-hole-coverage` give the precise supply:

- 218 live missions; 585 retained open-hole records.
- 116 unchecked-task holes projected across 19 missions.
- 469 unprojected: 141 work markers, 226 pending-lifecycle records, 102 open-section items.
- 17 targets added; two deferred to declarations: M-f11-find-production-successor and M-aif-policy-conditioned-eig.
- Family schedule/scales agreed; `:not-generated-reason nil`.

Those **17 generated targets are exactly the 17 missing-interpretation refusals**: M-dionysus-winddown, M-daily-scan, M-distributed-frontiermath, M-G-wm-wiring, M-kangaroo, M-chipwitz-corps, M-wm-aif-policy-grain-compliance, M-canon-fingerprint-store, M-self-documenting-stack, M-apm-capability-ratchet, M-usage-hacking, M-futonzero-mvp, M-action-cost-modelling, M-aif4iad, M-federated-agency-hardening, M-futonzero-generative and M-futon-forward-model.

A universe needs an explicitly observable token domain and facts. A checkbox mission already has a generic source for that. A prose-only mission needs an authored observable acceptance boundary, not an invented checkbox or a blanket “task exists” token that claims completion. Either kind still needs target-specific pattern interpretations and useful candidate orders. That semantic work can be assigned by one reusable workflow, but cannot honestly be replaced by one generic assertion that every retrieved pattern applies.

There is also an ingestion distinction. The production zero-argument `mission_registry/load-missions` reads substrate-2 (`mission_registry.clj:458`); it never falls back to a filesystem scan. A separate **explicit read-only file scan** here found 328 missions. Among the 198 historical missing-universe mission ids, 197 match current files; **10 would now generate checkbox universes from those files**, including M-a-wmc-scaling (5 open checkboxes). The other nine are M-aif-wiring, M-goals-and-holes, M-points-de-fuite, M-superpod-mark3, M-stack-geometry, M-categorical-code, M-a-sorry-enterprise, M-hypergraph-operator and M-or-training-as-learning-system. The remaining 187 matched files do not currently generate an observable mission source under this rule.

That does not prove today's substrate is stale: it proves a difference between the retained click's admitted input and current explicit file-derived potential. The [top-level mission admission report](../mission-top-level-admission-2026-09-20/REPORT.md) documents the earlier widened scan and explicitly says no substrate backfill was performed in that slice. For M-a-wmc-scaling, the retained click proves no universe entered assembly even though today's source document supplies five checkboxes. Verify ingestion/snapshot contents before attributing an exact historical cause. This packet queried no live substrate and performed no ingestion. A handwritten declaration is an explicit source, not a hidden filesystem fallback.

## 3. Front-ticket semantics and the foundations

`ticket_queue.clj:89`, `plan`, orders by insertion instant then ticket id and selects the **earliest admitted supported ticket**. `policy.clj:358–369` recomputes the same posterior within that target's stratum. Unrestricted comparisons remain diagnostic; they do not overrule the front target.

Consequences of Joe's “repairs go to the front” rule:

- A nonempty queue is not by itself a singleton selection. Unsupported entries are recorded as not admitted; the next admitted ticket is eligible. If none is admitted, this selector falls back to unrestricted policy selection. That implementation detail is **not permission to violate the owner’s stop-the-line instruction**.
- An admitted front ticket intentionally prevents B/C/G/E from picking another target. It still permits those quantities to choose **how to do the front ticket**, if that ticket has two supported cascades with distinct first actions.
- Merely duplicating an order/id does not create an action choice. Policies sharing the same first action aggregate into that action's posterior mass.
- B is not moot: alternative routes can have different predicted outcomes or reliability. C is not moot when those routes predict different wanted outcomes. It cancels when their predicted preference-relevant outcomes are identical. A focus-versus-other-mission preference cannot move the machine off the front ticket under the ruled stratum. Parameter novelty could distinguish equally desirable routes; it still needs its own production model connection.
- If every ticket has exactly one eligible first action, the queue determines the action irrespective of those foundations. More tickets then do not solve the within-ticket choice problem.

There is a concrete integration conflict to resolve before claiming repaired tickets supply alternatives. New `finding_ticket.clj:55–72` publication makes ordinary ticket `T-<finding-id>` and enqueues it. Legacy `repair_proposals.clj:13` uses **the same identity**, and `record-supply` still withholds every open finding's target on the old produced-resolution-evidence requirement. Publication creates task/provenance, not a universe, interpretation or executable cascade; the ordinary ticket text itself has no generated cascade-source declaration. This is not cured by a front-queue write.

At inspection the canonical live queue file `data/wm-ticket-queue/queue.edn` was absent; the versioned fallback `resources/wm/ticket-queue.edn` was empty. No claim is made about a later owner publication. The conflict is with the code path that will receive those tickets, not a claim that a front ticket was already selected here. Resolve it through the ruled ordinary-ticket admission/observation contract; do not rename ticket ids, suppress the hold, or invent a resolution locator to sneak a repair into selection.

## 4. Smallest honest supply changes, ranked and sized

Current file counts below were obtained with the existing mission parser's `observable-hole?` predicate, not by counting every prose TODO. They are **open checkbox counts**, not automatically executable tasks.

| Target | Open checkboxes now | Existing source / suitability |
|---|---:|---|
| M-aif-policy-conditioned-eig | 2 | `futon2/holes/missions/M-aif-policy-conditioned-eig.md:214,233`: typed Q(o\|pi) boundary and EIG shadow/calibration. Existing source already declares all three wants but produces only the completed updater; extend or replace its target-specific readings for remaining work. |
| M-a-wmc-scaling | 5 | `futon2/holes/M-a-wmc-scaling.md:146–150`: WMC backend, exactness, parameter identity/variance, state-observation MI, and resource measurements. Already has 48 pinned retrieval proposals; no executable declaration. Best new-source authoring packet. |
| M-futon-forward-model | 6 | `futon2/holes/M-futon-forward-model.md`: already among the 17 generated-universe targets; needs interpretations/orders. Useful follow-on, not needed just to restore two candidates. |
| M-f11-find-production-successor | 2 | `futon2/holes/missions/M-f11-find-production-successor.md:101–102`. Sole surviving declaration. Click 2 exposed cross-repository artifact-scope conflict; authoring more names does not fix that acceptance/dispatch problem. |
| M-expressions-of-interest | 0 | `futon5a/holes/missions/M-expressions-of-interest.md`. Existing behavioral wants are not checkbox-generated; three tokens lack admitted locators. Not a cheap “fill in missing checkbox” option. |
| M-wm-08-external-f2 | not a registry-checkbox mission in this scan | Its single declared want is a C3 report-exists token, already true. Retire/update the task only when there is genuinely new accepted work; do not manufacture an absent report. |

**Rank 1 — one bounded source-authoring packet (smallest data change).** Author **one** new declaration for M-a-wmc-scaling, with **two** justified constructed alternatives and distinct first pattern actions; consume the retained retrieval pins via `:proposal-id` where appropriate. At minimum this means two pattern readings (guards/produces/scope limits), two construction receipts, and checkable locators for every wanted/guard token, with real progress under T=2 and consistent beta/schedule/scales. It is a small declaration-and-regression packet, not 48 admissions and not a production-engine rewrite. Applicability remains author/reviewer work; this report has not established which retrieved patterns should be used.

An even smaller edit is to the existing M-aif-policy-conditioned-eig declaration: give a remaining open acceptance task a justified producing pattern/order. With F11 retained that restores two **unrestricted targets**; authoring two genuinely distinct first-action routes for its remaining work is more robust than relying on F11's blocked scope. Size: one existing EDN declaration, one or two new readings/orders, necessary locator extensions, and a real assembly→selection regression. N more whole mission files are unnecessary. Neither option makes a non-front mission eligible once an admitted repair ticket is ahead of it. Under that condition, put the two justified alternatives on the **actual front ticket**, after resolving its ordinary-ticket admission contract. No next click is authorized by this recommendation.

**Rank 2 — wire a reusable proposal→author→admission workflow (medium implementation, sustainable supply).** The absent component must dispatch bounded interpretation work **before selection**, using pinned target/pattern bytes; obtain explicit guards, effects, locators and orders; independently review/validate them; publish complete declarations without overwriting concurrent author work; then reload/reassemble and retain proposal joins/declines. Scope is a workflow plus an agent output contract and publication/replay tests, spanning `interpretation_request`, `cascade_proposals`, `cascade_sources` and the preselection orchestration seam. It is not a one-line call to `record-supply` and should not recursively require selecting the target that needs admission. A first slice can use the one retained M-a-wmc-scaling bundle and stop at a reviewable declaration. Generalization to repair tickets additionally needs the ruled ordinary-ticket acceptance/observation path. Retrieval itself remains evidence, not authority.

**Rank 3 — “generic universe from mission holes” (already implemented; alone insufficient).** No new generator is needed for the 17 existing checkbox-derived universes. Verify/repair ingestion separately for file-backed missions absent from the authoritative snapshot, then test file→substrate→source equality through the authorized ingestion lifecycle. That can move a missing-universe refusal to missing-interpretation, but cannot by itself create an executable first action. For prose-only tasks, the remaining work is an observable task-specific acceptance boundary. Automatically asserting generic interpretations/produces from text would weaken the very admission rule this packet must preserve.

The acceptance for the next supply packet should assert **two supported, non-duplicate first actions in the actual eligible stratum**, real locators and receipts intact, with each order capable of a new wanted token within the horizon. A raw count of proposal ids, universes, tickets, or unrestricted scored policies is not that acceptance.

## Method and gates

Read retained EDN directly in standalone Clojure processes; no runner entry point or live service was invoked. Temporary read-only inspection scripts grouped proposals by `:origin`, grouped trace refusals by `[:kind :missing]` and target prefix, and ran `mission-registry/load-missions-from-files` explicitly for the separate current-file census. They did not call default substrate readers or any `!` store verb. Key fresh aggregates:

```clojure
{:click2-proposals {:retrieval-proposed 48 :repair-finding-proposed 42}
 :click2-admissions []
 :missing-universe {:mission 198 :ordinary-ticket 22 :repair-ticket 42}
 :no-interpretation 17
 :file-scan-missions 328
 :historical-missing-mission-file-matches 197
 :historical-missing-but-current-file-generatable 10}
```

Checks on the three temporary inspection scripts (`/tmp/admission_census.clj`, `/tmp/admission_read.clj`, `/tmp/admission_trace.clj`): `clj-kondo --lint ...` **0 errors / 0 warnings**; `emacs --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --files ...` **OK**. `git diff --check` and `git diff --cached --check` passed with no output. Only this Markdown report is committed; no production source, declarations, evidence store, or test suite changed.
