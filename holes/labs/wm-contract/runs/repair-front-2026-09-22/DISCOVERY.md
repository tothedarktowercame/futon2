# Repair first through ordinary cascade selection — discovery

Examined futon2 `6714b3ac` in branch `fix/repair-front-of-queue`. This answers the **corrected** owner packet. No production edits, clicks, serving-JVM evaluations, registry writes, repair-store writes, or Lean builds. The only process probe reads `repair/open-obligations`; its output is retained beside this note.

Joe's rule: “there should be no distinction between ordinary selection and repair, but repair should go to the front of the queue. The mechanism absolutely exists. It is the standard mechanism.”

**Recommendation:** finish admission of ordinary `:cascade-candidate` repairs through the existing source/interpretation/observation contracts; declare repair-first eligibility at the ordinary action-marginal choice, retaining the unmodified full-family G, posterior and marginal. Do not revive `repair-entry`, convert the selected cascade to a legacy action, manufacture negative-infinite G, or treat a finding as an interpretation. This is several missing connections, not a one-line removal of the withholding guard.

## 1. Admission and the wanted observation

Existing path:

- `repair_proposals.clj/supply` uses `repair/open-obligations`, names `T-<repair-id>`, pins finding bytes and retains the verbatim discharge contract. It does **not** invent a pattern, guard, produces set, construction or interpretation. It currently includes environmental holds.
- `cascade_proposals.clj/load-supply` merges these into proposal supply. `war_machine.clj:6812–6838` includes proposal targets in ordinary assembly and calls `record-supply`, then `select-and-record-cascade!` / `cascade-decision`.
- `record-supply:130` excludes all repair-target problems regardless of declaration quality. Its refusal is `:repair-closure-observation-unavailable`, with missing `[:produced-resolution-evidence]`. It also excludes repairs from proposal admission joins. That join assumes a retrieved pattern/source, whereas a repair proposal has no `:pattern` or `:pattern-source`; simply deleting both exclusions would not establish the missing join.
- Source declarations still need `:facts`, `:want`, `:locators`, `:patterns` with `:guard`/`:produces`, `:interpretation-receipts`, nonempty `:candidates` with `:construction-receipt`, context/beta and commensurable schedule/scales. See `resources/wm/cascade-sources/M-f11-find-production-successor.edn`. No repair declarations are currently supplied there. Assembly checks every token; `admit-cascade-problem` also rejects no newly predicted wanted token.

There IS now a produced closure artifact, beyond the earlier proposal comment. `repair_discharge_receipt.clj`:

- `derive` reads canonical finding, implementation and resolution records, requires `:resolved`, both retained discharge contexts, distinct implementation/successor identities, and matching successor/witness bindings.
- `verify!` checks the entire derived value against the store and embedded bytes/hashes.
- `publish!` commits `holes/labs/wm-contract/discharges/<repair-id>.edn`; `published` returns an existing `:c3-locator` (`:class :C3`, `:repo`, `:sha`, `:path`). Publication failure does not undo store resolution; `catch-up!` retries publication independently of the proposal queue.

**Smallest existing observation:** declare the future produced receipt at its deterministic `receipt-path`, in the repair source's `:locators` map, using C3 with `:repo "futon2"`, `:sha "HEAD"`, and that relative path. This has the same declaration placement as mission C4 checkbox locators. `observation_checks.clj/check-path-exists` resolves HEAD once and records the resolved commit; a valid missing path is observed false. A resolution need not already exist to admit a candidate that predicts its production. Requiring the wanted fact true at admission would make repair impossible (and trigger the already-satisfied exclusion).

**Important semantic limit:** C3 means “the published path exists at the observed revision”, not “the unversioned resolution store currently contains a valid resolution”. It cannot inspect an ignored store file through Git. The existing receipt verifier supplies the production provenance, but C3 alone does not prove those bytes were produced by that verifier; a hand-created file must not count as an authoritative resolution. Admission should bind the declared token to the deterministic publisher and its contract, and positive observation must retain/check its produced receipt against the store. This validation connection is missing. Keep store resolution and publication-pending separately visible.

If the wanted token must literally mean **store resolution exists**, including the interval before publication, C3 is insufficient: extend the declared observation contract with a mechanical produced-resolution check (class identifier and locator schema are **not yet declared**), using `discharge-record`, strict identity and receipt/store verification. Wire it through `observation_checks`, `cascade_problems/checkable-classes`, `decision_gate/check-guard-locators!`, `resources/wm/observation-contract.edn`, and the post-build observer. Do not relabel this as C4 or broaden C3 silently. For the packet’s literal wanted token I recommend this additional declared mechanical observation, reusing the existing store/receipt validation rather than a new resolution authority. The C3 projection is smaller only if the owner explicitly chooses the narrower published-evidence meaning; it is not a substitute for the requested store observation.

Post-build timing also matters: `d_predecessor_task_authority.clj` currently measures C3/C4 at the author's artifact commit; the publication occurs **after immutable execution close**, at a later commit. That observation must not be retroactively rewritten as true at the author commit. Retain the later publisher observation (or observe it at next selection), with its actual revision/time. Missing observation stays unknown.

## 2. Repair-first selection and a truthful certificate

Keep repair targets as ordinary cascades. Carry their validated binding through source loading, `cascade_problems/assemble`, and the explicit map construction at `war_machine.clj:6145–6170`. The action fields already consumed by `repair-discharge/bind-selected!` are `:repair/id`, `:finding-source {:path :sha256}`, `:discharge-contract`, plus nonempty `:interpretation-receipts`. **The source-to-action transport of these fields is missing**; current joint candidate construction drops them. Prefix `T-repair-` alone is expressly insufficient authority.

The smallest precedence rule is lexicographic eligibility **after scoring**, inside `policy/select-action-cascades` at the current `cascade-selection/bayes-choice` call:

1. Compute G, F, habit, beta, full posterior and summed action marginal over **all admitted candidates**, exactly as now.
2. If any admitted, bound, non-environmental repair exists, restrict enacted choice to the oldest such obligation. Within that obligation use the existing action-marginal rule and declared action-name tie-break. Otherwise return today's selector result unchanged.
3. Preserve the unrestricted Bayes winner and comparisons as diagnostics, separately from the enacted constrained choice. Do not claim that a lower-marginal repair was the unrestricted Bayes action.

`repair/open-obligations:932` already orders by `:opened-at`. Preserve that oldest-first meaning. Declare deterministic equal-time ordering by native `:repair/id` (new tie rule), then existing action-name ordering within the obligation. Use the validated timestamp from the pinned finding, not proposal arrival order. Include awaiting-validation obligations: `open-obligations` returns them too. Exclude environmental holds with a typed recorded admission decline, rather than granting them repair priority or dropping their evidence.

Existing certificate fields: `:selection-certificate` has `:beta`, `:candidates` (each candidate's `:g`, `:habit`, `:f`, statuses), `:policies`, `:node-evaluation-traces`, `:scoring`, `:g-term-decomposition`; WM adds precision/token-belief/preference/proposal evidence. Existing `:selection-law` has `:requested`, `:applied`, `:posterior`, `:action-marginal`, `:softmax-weights`, `:per-policy-argmax`, tie rule, `:policy-comparison` and `:action-comparison`. Their `:decided-by` currently describes posterior terms/counterfactual flips, not priority.

**Missing declaration/record, proposed schema work:** declare the rule and authority alongside selection configuration; add a certificate precedence receipt recording that declaration, all eligible repair bindings and ordering keys, excluded/declined targets, unrestricted winner, constrained winner, and reason. Exact new field names are not existing APIs. A proposed `:precedence-comparison` with `:decided-by :repair-precedence` makes the new cause explicit. Keep fix-7 comparisons labelled unrestricted; do not overwrite `:habit`/`:robust` with a claim about a different comparison. Retain the same receipt in `:selection-law` or reference it explicitly from there so downstream narrative/run-record consumers see it.

`decision_gate/check-cascade-decision!` currently independently requires the selected action to be the **global** marginal maximizer. It must independently validate the declared eligible repair set/order and check the maximizer within it when precedence applies, while still checking full posterior normalization, receipts, real action membership and the selected action's **full-family** marginal mass. Never disable `:chosen-not-bayes-action` generally. If changing the applied law identifier to denote constrained choice, update `selected-entry:1175` and all law consumers together; the existing identifier does not currently express this rule. `controller_authority/authorize` can still validate an ordinary repair cascade's real finite G and membership, rather than accepting the legacy `##-Inf` score.

A numerical edge needs a test: a repair with finite but extremely bad score can underflow to zero posterior. Today's positive-mass entry lookup cannot enact it. Declare selection within the eligible tier using stable/log-domain scores and retain the underflowed full-family mass honestly; do not add fake mass or alter the recorded G. This requires a corresponding explicit gate contract for a zero represented global mass, not a silent exception.

No-open-repair byte identity is achievable by returning the existing decision unchanged with no precedence fields added. When repairs are admitted, mission G may itself change because scoring uses the enlarged target-qualified joint outcome universe. The requirement is to retain actual G/posteriors for every candidate, **not** to promise mission scores equal to a mission-only run.

## 3. Execution and the real queue

Admission does not finish the route:

- `selected-entry` can already carry a selected cascade to `construct-selected-action :cascade-candidate`; keep it that way.
- `mission-for-decision:1575` already recognizes a cascade's native `:repair/id` and binds the finding using `repair-discharge/bind-selected!`. Transporting the missing fields enables this.
- `repair-action?` at runner:4072 recognizes only legacy `:repair-machine-failure` / `:revalidate-historical-repair`. Thus `participants/select-reviewer!` currently sends an ordinary repair cascade to the ordinary reviewer. It needs the verified repair binding, not a target-prefix shortcut. Audit the other type tests (repository choice at `machine-repair-repository`, prompt/repair contract, terminal route/context) for the same issue, preserving cascade selected/enacted identity and trace.
- Close already calls `repair-discharge/finalize-run!` after writing `007-closed.edn` (runner:3838). The generic cascade branch verifies finding identity/pin/contract/interpretation and executed independent review. Implementation A invokes `record-implementation!`; distinct successor B invokes `successor-resolution!` / `resolve!`, then publishes the receipt. A normal grounded close is not itself repair resolution.
- **No production evaluator admissions:** `resources/wm/repair-evaluator-admissions.edn` is `{:schema :wm/repair-evaluator-index-v1 :evaluators {}}`. Registry code only implements `:history-artifact-read`, constrained to `:initialization-failed` and a pinned poisoned-artifact input. That does not cover all initialization failures, let alone the triage's other kinds. Without authored, independently reviewed evaluator admissions, close records `:no-repair-evaluator`; it cannot resolve these findings.
- Historical revalidation is not automatically entered for a code-fixed target. The old `historical-revalidation-entry` is a legacy action with separate execution port and no author dispatch; it only establishes awaiting-validation. Do not revive it. The generic discharge path needs a retained per-obligation implementation context and a distinct production successor. Git ancestry/unit tests are not that context. An adapter admitting historical implementation evidence to the existing store contract is additional work; do not fabricate a new implementation commit or weaken distinct-attempt/review gates.

Triage rows 1–2 therefore need ordinary cascade tasks for corrected deposits; rows 3–10 need ordinary cascade tasks supplying the missing implementation/review admission (where absent) and production-shaped validation. Relevant defect groups remain: rows 3–5 schedule mismatch (`a38becc9`), 6–7 token print/read (`29fb2a83`), 8 empty cascade/marginal (`d168d348`, `8f60f819`, `c155d690`), 9 fold schema (`c91261fd`), 10 reviewer explanation (`6d45e8b7`). Row 5 particularly needs executed-version/input evidence; row 10 needs the actual corrected deposit. None is automatically “historical revalidation complete”.

### Count discrepancy worth resolving before implementation

The corrected packet says 11 open obligations. The read-only canonical store query retained in `open-obligations.edn` returns **44 records: 10 open machine failures, 19 awaiting-validation machine failures, 3 awaiting-validation independent-review failures, and 12 environmental holds**, because the store's `open-obligations` includes awaiting-validation and environmental holds. Use that output's `:status-class`, not a hardcoded eleven or the ten-row triage, as the discovered population. It includes the ten triage machine findings and newer environmental hold `repair-occ-917bbee700382c988dce8f4d2d69de454062adbeb5de7070eb61973a301af1f6` (September 22, revision-wait guardrail refusal). That latter finding must not enter repair priority. Older awaiting-validation repairs also remain in this reader and would precede the triage rows if admitted. I did not dismiss them or infer closure from their age.

## 4. Sized implementation and decisive tests

This is a medium cross-boundary change, approximately three reviewable slices, not a runner diversion:

1. **Admission/observation and identity (roughly 5–7 source/declaration loci plus tests):** authored repair source(s), declared produced-resolution observation, proposal receipt join, replace blanket withholding with actual admission, environmental-hold decline, propagate the existing native binding fields. Start with one frozen real finding and one fully evidenced interpretation. Bulk pattern applicability cannot be inferred from the other findings' prose.
2. **Declared precedence (roughly 3–5 selector/gate/record consumer loci plus tests):** retain full-family scoring; select oldest admitted repair; independently validate the same eligibility contract; retain unrestricted versus precedence attribution; preserve no-repair branch byte-for-byte. Register any new click-path namespace in load identity.
3. **Execution evidence (runner integration plus evaluator admissions/tests; variable work per defect kind):** use the bound cascade repair for reviewer/repository/contract routing, reuse generic discharge, admit actual historical implementation contexts where supportable, and supply independently reviewed evaluators. This is the part that makes the selected task able to finish, not merely win. Do not call a ranking-only patch “self-repair works”.

Tests before landing:

- Frozen byte copy of triage row 9's real finding (`repair-occ-0576180e03115d74ce0a32eff59df655649d732e8cdc10f12a86f98b15f51879.edn`) in a temp store; use its true `:fold-output-invalid` contract. Admit a complete declaration via the real loader/assembly/decision path. The current blanket withholding must fail the positive test on this base. Mutated pin/contract and target-prefix-only impostor must still refuse.
- Repair versus mission with deliberately overwhelming mission G/posterior advantage: repair wins only because of the declared precedence; both candidates' scores/posteriors and unrestricted winner remain present. Include numerical underflow. Two repairs in reversed input order choose the oldest, with equal-time/id and within-target action tie cases.
- No repairs: compare serialized decision bytes against today's recorded/replayed baseline, including comparisons and certificate. Do not compare timestamps from two uncontrolled scans.
- Environmental hold from a frozen real finding: evidence retained, typed decline, absent from candidate set even if older and assigned attractive G. Also verify resolved/dismissed findings disappear via the real store reader.
- Independent gate rejects forged priority, missing declaration, mismatched finding, and selecting a newer repair; accepts a certified constrained cascade without dropping its ordinary receipt requirements.
- Missing future C3 receipt observes false and does not block potential production; malformed/forged produced receipt cannot stand for store resolution. Store success with publication failure is separately recorded and catch-up does not call resolve again. Observation at author commit remains distinct from publication commit.
- Grounded runner fixture selects the actual cascade, dispatches independent repair reviewer, preserves selected=enacted action, and reads durable `007-closed.edn`. Temp repair root/repository only. First implementation cannot self-resolve; distinct successor with admitted real evaluator can; absent evaluator stays typed. No live store/JVM in tests.

## Reproduction and validation

Standalone read-only process, from this worktree:

```sh
clojure -M -e '(require (quote futon2.aif.repair-obligation)) (let [rs (futon2.aif.repair-obligation/open-obligations "/home/joe/code/futon2/data/wm-repair-obligations")] (prn {:read-at (str (java.time.Instant/now)) :root futon2.aif.repair-obligation/default-root :count (count rs) :status-class (frequencies (map (juxt :repair/status :repair/class) rs)) :rows (mapv #(select-keys % [:repair/id :repair/status :repair/class :failure-kind :failure-stage :opened-at]) rs)}))'
```

Code/history inspection included `git show 8b6827da -- src/futon2/aif/full_loop_runner.clj`, the named source files above, the current observation/evaluator declarations, and the stopline triage. No probe script added. `git diff --check` and staged equivalent are the gates for this documentation/data-only packet; no Clojure source changed, so lint, paren and namespace-test gates are not applicable. Implementation tests above are proposed, not run or claimed passing.
