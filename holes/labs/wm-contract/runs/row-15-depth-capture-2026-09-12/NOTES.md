# Row 15 depth capture

The isolated machinery tick used `:trace? false`; `capture.edn` is the
redirected retained output, and no live daily trace or serving JVM was touched.
The configured depth request was 3. The anticipation producer reported
`:anticipation-events-unavailable`, so the exact EFE input was
`:horizon-steps nil` and the actual policy depth was 1. Those values occur both
on the judgement readback and its `trace-record` projection.

The first capture attempt at `3b581fbd` refused at the existing strategic
selector boundary because no selector was supplied. Follow-up `4473c834`
provided the explicit machinery-test selector seam and produced the retained
capture.

The first broad test run at `5764f4ee` reported seven failures and one error.
Four golden failures showed that the initial trace fixture made the new fields
unconditional; `e2052a93` repaired this to present-only propagation. The other
failures belong to concurrent changes (softmax trace detail, a mutable mission
criteria digest, and disposition support), not this packet. A subsequent full
trace namespace run had only the concurrent softmax-detail failure. The
packet-local depth test and the three existing scorer-option tests are the
passing Clojure gate recorded here.
