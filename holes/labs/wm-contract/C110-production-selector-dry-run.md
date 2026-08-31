# C110 — production selector dry-run

Date: 2026-08-31

No tick was executed.  This pass called only the production HTTP bridge,
`full-loop-runner/strategic-selection!`, against the live Agency endpoint with
the three authorized mission IDs and trace ID
`C110-selector-dry-run-2026-08-31`.  It returned on the first attempt:

```clojure
{:status :verified-live-selection
 :selected-mission-ids
 ["M-shared-memory-control-build-test" "M-aif-policy-conditioned-eig"]
 :selected-policy-id "pi-s-9dbc2ceb3317bc38050c41ce"
 :candidate-domain
 ["M-aif-policy-conditioned-eig"
  "M-shared-memory-control-build-test"
  "M-wm-aif-policy-grain-compliance"]
 :serving-cache-gate {:status "warm" :attempt-count 1}
 :actuation {:executed? false
             :authorized? true
             :status :machine-authorized-bounded-autonomy}}
```

The call performs selection and reads its supporting substrate evidence, but
does not enter `run-opportunity!`, dispatch an agent, score a tick, enact, or
write a trace.  The selector is therefore separable from the loop's side
effects.

## Live operator readiness

`clojure -M:wm-full-loop status` reported:

```clojure
{"zai-5"  {:available? true :status "idle" :invoke-ready? true}
 "codex-7" {:available? nil :status nil :invoke-ready? nil}
 "codex-1" {:available? true :status "idle" :invoke-ready? true}}
```

Thus the selector is live, but the default ordinary reviewer remains
unavailable.  If the selected entry is ordinary work, the loop will stop at
`:agent-readiness` with `:outcome :agent-unavailable`; it does not dispatch or
silently substitute another reviewer.  A machine-repair selection uses the
separate repair reviewer, currently `codex-1`.

## Typed failure surfaces

- An empty or unauthorized ranking receives HTTP 400 and becomes
  `:failure-kind :strategic-selection-unavailable`, `:failure-stage
  :selection`, `:failure-detail :deterministic-rejection`, with one recorded
  attempt.  The live endpoint returned this exact shape for the unauthorized
  dry-run probe.
- Transport errors, timeouts, HTTP 429, and HTTP 5xx use the bounded three-step
  retry ladder. Exhaustion throws the same typed failure kind with
  `:failure-detail :transient-exhausted` and an `:attempt-failures` vector.
- A response lacking `{:ok true :selection map}` is a deterministic typed
  rejection. A valid selection that yields no addressable entry later throws
  `{:outcome :abstained}` or `{:outcome :no-selection}`; it is never treated as
  `nil` success.
- An unavailable reviewer throws `:outcome/:failure-kind :agent-unavailable`,
  `:failure-stage :agent-readiness`, with the author, reviewer, role, and
  failure detail.

## Operator inputs

The command is `clojure -M:wm-full-loop once`.  No agent flag is syntactically
required. Defaults today are author `zai-5`, ordinary reviewer `codex-7`,
repair reviewer `codex-1`, a 14-day window, and a 45-minute agent budget.
The operator should supply `--reviewer` naming an available agent before an
ordinary run; `--author` and `--repair-reviewer` override their corresponding
defaults.  Optional controls are `--window-days`, `--agent-budget-seconds`,
`--batch-id`, and `--tripwire-action`.  Author and reviewer must be distinct
and available.

Canonical read-only invocations:

```sh
clojure -M:wm-full-loop status
clojure -M -e '(require (quote futon2.aif.full-loop-runner))
  (prn (futon2.aif.full-loop-runner/strategic-selection!
    {:agency-base "http://127.0.0.1:7070"
     :strategic-selection-timeout-ms 30000}
    {:scheduler-habit-ranking
     ["M-aif-policy-conditioned-eig"
      "M-shared-memory-control-build-test"
      "M-wm-aif-policy-grain-compliance"]
     :trace-id "C110-selector-dry-run-2026-08-31"}))'
```

The real response was summarized above rather than committed wholesale: its
supporting-memory bodies are large and remain available from the authoritative
endpoint.  No scoring or selection behavior changed.
