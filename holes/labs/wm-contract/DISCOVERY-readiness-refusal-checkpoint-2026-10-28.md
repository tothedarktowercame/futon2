# Readiness refusal checkpoint: stop before implementation

For claude-11, 2026-10-28. Discovery only; no source edits, commits, tests, reloads or dispatches. Checkout is `main` (not master); observed HEAD `e2fd1ad553a3dd84685ceddbdaf364cdd8973774`. References below are canonical file lines read this turn.

## (a) Refused answer shape

Retained contact:
`data/wm-ticket-readiness/manifest.edn.contacts/invoke-1793119192416-5620-354be23f.edn`
(all data paths in this note are under `/home/joe/code/futon2`). SHA256:
`bacedb28ba11e8057c5242a8193fa938cccae84fe07217ff087b4fe8501637d6`.

Its complete value is:

```edn
{:schema :wm/ticket-readiness-contact-v1,
 :job-id "invoke-1793119192416-5620-354be23f", :seat "wm-author",
 :result "[context compacted to a summary: provider-refused, 26959 tokens]",
 :response-state "failed"}
```

Read-only GET `http://127.0.0.1:7070/api/alpha/invoke/jobs/invoke-1793119192416-5620-354be23f` confirmed the job is `"failed"`, result null, one text event equal to the retained result above, `terminal-code` `"invoke-error"`, and terminal-message:

```
provider refused the prompt on both attempts (1261 "Prompt exceeds max length"); attempt 1: {"message":"HTTP 400","body":{"error":{"code":"1261","message":"Prompt exceeds max length"}}} | attempt 2, after compaction: {"message":"HTTP 400","body":{"error":{"code":"1261","message":"Prompt exceeds max length"}}}
```

Job created `2026-10-27T16:39:52.416449899Z`, finished `2026-10-27T16:40:42.584894305Z`. Both GETs used `curl -fsS --max-time 20` with `jq` projections; neither contacted a seat or invoked code in the serving JVM.

`flight_runner.clj:110–118,138–168` reconstructs the contact result as `{:seat "wm-author" :job-id "invoke-1793119192416-5620-354be23f" :state "failed" :text "[context compacted to a summary: provider-refused, 26959 tokens]" :library-root {:absent :not-in-flight-opts}}` on the default production port. It does **not** forward terminal-code/message. This is a source-derived reconstruction, not a claim that the complete response map was persisted.

`full_loop_runtime.clj:187–217` answer-fn adds `:answer nil`, the retained contact's `:result-pin` above, and a `:contact-resolver` function. `handoff_contract.clj:109–122` returns nil for response-map on `:state "failed"`. The resolver is callable process state, not serialized evidence. `ticket_readiness.clj:293–301` persists only job ID, seat, text and state. Thus a future refusal-reporting implementation cannot obtain provider code/message from the returned response alone; it must preserve or read the actual job error through the existing port.

## (b) Input change is not established as refusal-caused

Assessment record:
`data/wm-ticket-readiness/manifest.edn.records/T-car3-queue-routing-design-note-e539d69d02ab-2b3a39297895931d0b2c.edn`, SHA256
`09e0a1b4ef05ea13216b6ae8115b94bed7e9d8e0b6efa3b62167cfdb8b97bee7`.

Relevant values verbatim:

```edn
:assessment {:classification :unknown :reason :inputs-changed-during-contact}
:assessed-at "2026-10-27T16:40:46.126786344Z"
:ownership-evidence
{:state :unowned,
 :source {:path "http://127.0.0.1:7070/api/alpha/agents",
          :sha256 "4c7fe72d766eaa09c0f3211a9a5ea852064bf48604795ff3d04953a2f61a4a39"}}
```

The other retained inputs are the ticket source pin, constant unknown dependency/implementation evidence, operator status `:live`, requisition `{:absent :no-requisition}`, and root-route `:standalone`.

The input producer has an independent volatile dependency:

1. `full_loop_runtime.clj:195–198`: every input-fn call fetches a new pipeline snapshot. The closure holds the same ticket map, but not the same snapshot.
2. `meta_live_outer_selector.clj:82–112`: snapshot includes a fresh `registry-status` result (or HTTP equivalent); the source pin hashes the **entire** generated JSON/body (`:62–80`), not just ownership facts for this ticket.
3. `meta_live_outer_selector.clj:488–503`: ownership-observation copies that whole-roster source pin even when the ticket remains `:unowned`.
4. `meta_live_outer_selector.clj:508–522`: ticket-readiness-input includes ownership-observation. It also derives root-route from fresh pipeline membership.
5. futon3c `agency/registry.clj:1873–1905,1949–1984`: registry-status includes session/status/last-active, job counts, invoke activity, and time-dependent `:invoke-quiet-ms` and inbox age when applicable. A successful contact or unrelated agent activity can alter the hash just as a refused contact can. Input identity is not restricted to ticket ownership stability.
6. `ticket_readiness.clj:377–386`: compares full before-inputs with full after-inputs, overwriting any validator assessment on inequality. `:388–404` retains **only after-inputs**, not before-inputs or a difference. The assessment's `:unowned` state is therefore not evidence that the before/after roster pins were equal.

The records establish inequality, but not the exact changed field: before-inputs and the two roster bodies are not present in this assessment/contact pair. The strongest source-backed explanation is roster-source pin drift; changed pipeline membership/root-route is another possible difference. Refusal itself is not an argument to input-fn. It can affect the roster through contact activity, but that does not make drift specific to provider refusal. To discriminate historically would require the exact pre-contact input/snapshot and post-contact snapshot; a fresh probe now cannot reconstruct them.

## Decision

**Stop at Step 1.** The required premise for Step 2 (refusal alone was mislabelled) is not established, and the code exposes an independent whole-roster freshness issue. Implementing refusal precedence here would not resolve why otherwise valid contacts could also lose their assessment. The original validator assessment is also overwritten and not retained, so I do not claim a particular pre-override classification from this record.

No choice was made about failing the click versus continuing a typed readiness failure. No acceptance test or gates were run because no implementation was authorized past this checkpoint. No commit SHA to report. The full suite was not run.
