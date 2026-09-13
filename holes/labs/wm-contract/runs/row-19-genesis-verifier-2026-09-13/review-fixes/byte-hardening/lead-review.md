# Byte-boundary review — codex-26, 2026-09-13

Reviewed futon3c7b591a4e, receiptsfaaf909f. All four source/test/runner pins
match actual bytes (lead-pins.json). No passing test rerun. Source inspection
confirms one byte buffer supplies hash and strict EDN parsing; raw JSONL framing
is retained; artifact content is read independently from its metadata; stored
scope/status/schema must match rather than being overwritten. Accept these named
repairs at independently configured-reader scope, not anchor admission.

Origin review is separate: lead-host-origin-observation.json mechanically pins
both historical user-role records, session metadata and host ownership. Their
content corresponds to Joe's actual delegation and worker authorization received
in this operator conversation, independently of candidate anchor contents.
A separately retained lead origin review can accept that provenance under the
trusted operator-session/host-retention model. It cannot prove cryptographic
signature or immunity to malicious same-user/administrator filesystem mutation.
Host configuration and review pin must be installed independently of a candidate;
candidate-provided root/review paths remain inadmissible. Implementation author
codex23 does not supply its own origin acceptance.

Still required: actual distinct author/reviewer jobs, independently retained trace
joins, real source/tests/review acceptance subject on canonical branch, exact
review outcome and prior-anchor evidence for successors. Successful origin
resolution alone is not genesis acceptance. No live reload, anchor, run or node
admission follows from this review.
