# Correct reviewer falsifier failed

**Status:** OPEN

Parent: M-interim-director-proxy-metric-inventory

## Observed failure

Target: M-interim-director-proxy-metric-inventory; stage: :reviewer-wait.

Context: {:phase :reviewer-wait, :selected-entry {:action {:kind :cascade-candidate, :id :C1, :target "M-interim-director-proxy-metric-inventory", :want [:hole/h729a33cf7677 :hole/h3fa53d7f01a5 :hole/hb0acfa5c247c :hole/h22612e4da4e8], :precedence [{:id :ukrns/reader-run-path, :authority :documented-interpretation, :produces #{["M-interim-director-proxy-metric-inventory" :hole/h3fa53d7f01a5]}, :guard {:status :interpreted, :operator :and, :clauses [{:status :interpreted, :present #{}, :absent #{}}]}, :transition {:status :interpreted, :operator :union, :produces #{["M-interim-director-proxy-metric-inventory" :hole/h3fa53d7f01a5]}, :authority :documented-interpretation}, :target "M-interim-director-proxy-metric-inventory"} {:id :war-machine/state-capture, :authority :documented-interpretation, :produces #{["M-interim-director-proxy-metric-inventory" :hole/h729a33cf7677]}, :guard {:status :interpreted, :operator :and, :clauses [{:status :interpreted, :present #{}, :absent #{["M-interim-director-proxy-metric-inventory" :hole/h729a33cf7677]}}]}, :transition {:status :interpreted, :operator :union, :produces #{["M-interim-director-proxy-metric-inventory" :hole/h729a33cf7677]}, :authority :documented-interpretation}, :target "M-interim-director-proxy-metric-inventory"} {:id :orchestration/recorded-handoff, :authority :documented-interpretation, :produces #{["M-interim-director-proxy-metric-inventory" :hole/hb0acfa5c247c]}, :guard {:status :interpreted, :operator :and, :clauses [{:status :interpreted, :present #{}, :absent #{}}]}, :transition {:status :interpreted, :operator :union, :produces #{["M-interim-director-proxy-metric-inventory" :hole/hb0acfa5c247c]}, :authority :documented-interpretation}, :target "M-interim-director-proxy-metric-inventory"} {:id :measurement/warrant-travels-with-the-number, :authority :documented-interpretation, :produces #{["M-interim-director-proxy-metric-inventory" :hole/h22612e4da4e8]}, :guard {:status :interpreted, :operator :and, :clauses [{:statu … (see finding).

Reviewer falsifier checks did not pass

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance criteria

- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**
- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-2e74f4c48d75d01ec34abb72ef65e6dfdcc97d2411bbc6f108842af1f88fb179.edn)

Finding SHA-256: `ff4d3512112c4880d3bfb980dc5fee775483685f8978af0abf5151b79c289ffb`
