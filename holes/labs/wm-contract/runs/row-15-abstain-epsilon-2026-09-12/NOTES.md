# Row 15 abstain-epsilon capture

`policy/select-action` now adds the flat `:abstain-epsilon` field to every
decision.  A flat field is the narrowest honest record: this packet captures
one resolved selector option, so a versioned options envelope would add schema
without another semantic.  Both comparison branches read the same resolved
local and the returned decision records that local.  The action, rank, scores,
weights, and abstention condition are unchanged.

`trace/strip-decision` already removes exactly `:softmax-weights` and
`:ranked-actions`; it therefore passes the new field without a `trace.clj`
change.  `capture.edn` is the retained readback from an explicitly redirected
`write-trace!`, with policy-detail persistence bound off as the trace tests do.
Neither the live daily trace nor the serving JVM was touched.

The repaired broad ambient run executed 211 tests / 1,033 assertions.  The
packet's tests passed; the remaining one failure and three errors reproduce
unrelated concurrent findings in the mutable mission digest, policy-detail
fixture rank joins, and disposition support.  `failed-attempts.edn` retains
that result and the earlier wrapper/capture/lint attempts.  The positive gate
is the two exact packet test vars (2 tests / 9 assertions), including default,
explicit, abstain, chosen, strip, redirected write, and readback checks.
