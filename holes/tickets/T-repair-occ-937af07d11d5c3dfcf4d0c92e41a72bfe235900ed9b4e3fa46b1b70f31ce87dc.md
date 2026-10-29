# Correct reviewer falsifier failed

**Status:** OPEN

Parent: M-reachable-from-boot

## Observed failure

Target: M-reachable-from-boot; stage: :reviewer-wait.

Context: {:phase :reviewer-wait, :selected-entry {:action {:interpretation-debt {:schema :wm/reading-interpretation-debt-v1, :analysis-digest "b8206e8c53d31177276a14f784ce827242085b4832b1961a69be22b31a6cbe29", :policy-id "M-reachable-from-boot/policy-7d91338b4da14feb2f06", :arrangement-digest "773c7b7cbceb53268ef9b8a8369a1e4108a654554e5ac986ae73b03c887a2f7c", :digest "bedba30c9d6dee044c9119cd091c821e45db773f22a60332ce6fa4129de57b7d", :excerpt-digest "fdb316719187cbe448c427f65b999cc138e80d6901d33194348c48c5eab8059c", :owed [], :rearm {:condition :all-owed-nodes-validated-for-this-identity, :requires [:policy-id :digest :every-owed-pattern]}, :order [["agent/evidence-over-assertion" 0] ["agent/escalation-cost-vs-risk" 1] ["agent/handoff-preserves-context" 2]], :policy-key [:wm/reading-policy-v1 "M-reachable-from-boot" "M-reachable-from-boot/policy-7d91338b4da14feb2f06" {:nodes [[["agent/evidence-over-assertion" 0] "agent/evidence-over-assertion"] [["agent/escalation-cost-vs-risk" 1] "agent/escalation-cost-vs-risk"] [["agent/handoff-preserves-context" 2] "agent/handoff-preserves-context"]], :edges [[["agent/escalation-cost-vs-risk" 1] ["agent/handoff-preserves-context" 2] :context-enables] [["agent/evidence-over-assertion" 0] ["agent/escalation-cost-vs-risk" 1] :contrasts-with]], :order [["agent/evidence-over-assertion" 0] ["agent/escalation-cost-vs-risk" 1] ["agent/handoff-preserves-context" 2]]}], :target "M-reachable-from-boot", :node-status [["agent/evidence-over-assertion" :accepted] ["agent/escalation-cost-vs-risk" :accepted] ["agent/handoff-preserves-context" :accepted]]}, :cascade-digest "8db4afa8c1653922cdf23c49e537c783495fa74f8b5312989d521f0fb66211a3", :id "M-reachable-from-boot/policy-7d91338b4da14feb2f06", :kind :cascade-candidate, :policy-key [:wm/reading-policy-v1 "M-reachable-from-boot" "M-reachable-from-boot/policy-7d91338b4da14feb2f06" {:nodes [[["agent/evidence-over-assertion" 0] "agent/evidence-over-assertion"] [["agent/escalation-cost-vs-risk" 1] "agent/esca … (see finding).

Reviewer falsifier checks did not pass

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance evidence

Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output. Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-937af07d11d5c3dfcf4d0c92e41a72bfe235900ed9b4e3fa46b1b70f31ce87dc.edn)

Finding SHA-256: `d3ce53d5c901753966b692723e3d5c523c0d2fa05e716048f5dac497bcf54650`
