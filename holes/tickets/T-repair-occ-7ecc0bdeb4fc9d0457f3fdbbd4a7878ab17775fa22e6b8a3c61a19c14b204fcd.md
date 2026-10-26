# Correct invalid author refusal

**Status:** OPEN

Parent: M-recommendation-bindings

## Observed failure

Target: M-recommendation-bindings; stage: :author-wait.

Context: {:phase :author-wait, :selected-entry {:action {:interpretation-debt {:schema :wm/reading-interpretation-debt-v1, :analysis-digest "54558739922fa7db94f035fc7573d430033fbee7e87acce089da6b5f4ed38441", :policy-id "M-recommendation-bindings/policy-7e8a0806fea29fa47914", :arrangement-digest "bc789e701629d40a5091c827b5620fdaa35b7bc1ba9e81c072dc860934356e38", :digest "7bb71ba60b11b3584548b504c96ab8aac984b6c6ca126f93c68a931855e5f1a0", :excerpt-digest "e8678feb63133e7a13d5d3ed7a2722c430f8ff80f2355b6f98a4d09dd01fe941", :owed [], :rearm {:condition :all-owed-nodes-validated-for-this-identity, :requires [:policy-id :digest :every-owed-pattern]}, :order [["contracts/resolve-pointers-at-head" 0] ["futon-theory/mission-lifecycle" 1] ["agent/handoff-preserves-context" 2] ["agent/evidence-over-assertion" 3] ["agent/evidence-over-assertion" 4] ["contracts/resolve-pointers-at-head" 5] ["agent/evidence-over-assertion" 6] ["contracts/resolve-pointers-at-head" 7] ["agent/evidence-over-assertion" 8] ["measurement/warrant-travels-with-the-number" 10] ["measurement/warrant-travels-with-the-number" 11] ["agent/evidence-over-assertion" 12]], :policy-key [:wm/reading-policy-v1 "M-recommendation-bindings" "M-recommendation-bindings/policy-7e8a0806fea29fa47914" {:nodes [[["contracts/resolve-pointers-at-head" 0] "contracts/resolve-pointers-at-head"] [["futon-theory/mission-lifecycle" 1] "futon-theory/mission-lifecycle"] [["agent/handoff-preserves-context" 2] "agent/handoff-preserves-context"] [["agent/evidence-over-assertion" 3] "agent/evidence-over-assertion"] [["agent/evidence-over-assertion" 4] "agent/evidence-over-assertion"] [["contracts/resolve-pointers-at-head" 5] "contracts/resolve-pointers-at-head"] [["agent/evidence-over-assertion" 6] "agent/evidence-over-assertion"] [["contracts/resolve-pointers-at-head" 7] "contracts/resolve-pointers-at-head"] [["agent/evidence-over-assertion" 8] "agent/evidence-over-assertion"] [["measurement/warrant-travels-with-the-number" 10] "measurement/warrant- … (see finding).

Author refusal lacks a reason or claims an artifact

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance evidence

Retain a reproduction or regression check that detects the reported failure and passes after the change, together with the scoped validation output. Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-7ecc0bdeb4fc9d0457f3fdbbd4a7878ab17775fa22e6b8a3c61a19c14b204fcd](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-7ecc0bdeb4fc9d0457f3fdbbd4a7878ab17775fa22e6b8a3c61a19c14b204fcd.edn)

Finding SHA-256: `49bbaff8fc7f1378e0d063c733425a6e3b56349042e39fa843fb5dfecc314f52`
