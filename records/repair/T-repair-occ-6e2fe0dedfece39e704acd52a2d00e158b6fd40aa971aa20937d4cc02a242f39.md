# Correct reviewer falsifier failed

**Status:** OPEN

Parent: T-jvm-provenance-before-multi-jvm

## Observed failure

Target: T-jvm-provenance-before-multi-jvm; stage: :reviewer-wait.

Context: {:phase :reviewer-wait, :selected-entry {:action {:interpretation-debt {:schema :wm/reading-interpretation-debt-v1, :analysis-digest "a223d0df5714a74d45d1bac9979604cf0ba4a649e2aec4cd378aeb79273e84dc", :policy-id "T-jvm-provenance-before-multi-jvm/policy-50b16277a70ead4b74b4", :arrangement-digest "e8c6e2c8a12c90f44aabdedaa176b934fd1ec65659f09444fd8070b4ba90d23e", :digest "bf1b685f99e2963929910003f337dd45ebd6a51695bf6e7d6c801a62c6001398", :excerpt-digest "7235ac4e83713085a9216b116a70fce1bebfe246ab19624a6a960644797ac84b", :owed [], :rearm {:condition :all-owed-nodes-validated-for-this-identity, :requires [:policy-id :digest :every-owed-pattern]}, :order [["invariant-coherence/shape-first-identify" 0] ["invariant-coherence/single-locus" 0] ["contracts/holder-states-the-claim" 1] ["invariant-coherence/reachable-from-boot" 2] ["invariant-coherence/shape-first-identify" 3] ["invariant-coherence/single-locus" 4] ["contracts/every-entry-has-a-falsifier" 6] ["invariant-coherence/shape-first-identify" 7] ["invariant-coherence/reachable-from-boot" 7]], :policy-key [:wm/reading-policy-v1 "T-jvm-provenance-before-multi-jvm" "T-jvm-provenance-before-multi-jvm/policy-50b16277a70ead4b74b4" {:nodes [[["invariant-coherence/shape-first-identify" 0] "invariant-coherence/shape-first-identify"] [["invariant-coherence/single-locus" 0] "invariant-coherence/single-locus"] [["contracts/holder-states-the-claim" 1] "contracts/holder-states-the-claim"] [["invariant-coherence/reachable-from-boot" 2] "invariant-coherence/reachable-from-boot"] [["invariant-coherence/shape-first-identify" 3] "invariant-coherence/shape-first-identify"] [["invariant-coherence/single-locus" 4] "invariant-coherence/single-locus"] [["contracts/every-entry-has-a-falsifier" 6] "contracts/every-entry-has-a-falsifier"] [["invariant-coherence/shape-first-identify" 7] "invariant-coherence/shape-first-identify"] [["invariant-coherence/reachable-from-boot" 7] "invariant-coherence/reachable-from-boot"]], :edges [[["contracts/ever … (see finding).

Reviewer falsifier checks did not pass

## Scoped task

Reproduce the reported behaviour at the named stage and correct its cause. Use the linked evidence to establish the scope; record any missing reproduction inputs explicitly.

## Acceptance criteria

- A reproduction or regression check detects the reported failure at the named stage. Check: a test namespace/name or command recorded in this ticket. **Not started.**
- That check passes after the change, with the scoped validation output retained. Check: commit SHA and test output. **Not started.**
- Ticket review accepted the scoped work. Check: Status line reads DONE. **Not started.**

Review the task evidence through ordinary ticket review; mark this ticket DONE only when its scoped work is accepted.

## Provenance

Finding: [repair-occ-6e2fe0dedfece39e704acd52a2d00e158b6fd40aa971aa20937d4cc02a242f39](/home/joe/code/futon2/data/wm-repair-obligations/findings/repair-occ-6e2fe0dedfece39e704acd52a2d00e158b6fd40aa971aa20937d4cc02a242f39.edn)

Finding SHA-256: `aa5657d59a86d0120f23683d5f2279399bf5d5ca7fea00e845626bf800d2637d`

## Injury provenance

```edn
{:injury-id "injury/76d912d35d995adc3794ae757bda36b905ecf949", :cause-record-pin {:path "/home/joe/code/futon2/data/wm-runs/tick-run-record-2026-11-05-7bc9acb9-e46c-41ba-9a09-1516fec6c0ac.edn", :sha256 "0792e160b1d9f203fd39a6babd7f5edfe753b7d63681c3d533328e52423f344d"}, :origin-run/id "2026-11-05-7bc9acb9-e46c-41ba-9a09-1516fec6c0ac", :finding-sha256 "aa5657d59a86d0120f23683d5f2279399bf5d5ca7fea00e845626bf800d2637d"}
```
