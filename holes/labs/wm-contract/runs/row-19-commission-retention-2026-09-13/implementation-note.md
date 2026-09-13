# Row 19 invoke commission retention

The single persistence boundary is
`futon3c.transport.http/create-invoke-job!`
(`futon3c/src/futon3c/transport/http.clj:1349-1415`). It is reached by
auto-bellback (`:1031-1048`), headless parked resume (`:1162-1172`), direct
invoke/build response (`:4360` onward), bell (`:4939` onward), announce
(`:5140-5200`), activation of the already-bound announced job (`:5200-5260`),
whistle-stream (`:5515` onward), and whistle (`:5663` onward). These surfaces
do not construct independent digests: all new jobs pass through this boundary.

The canonical preimage is the map returned by `normalized-invoke-commission`
(`:1305-1319`): string agent id, exact prompt, defaulted/string caller and
surface, and model only when supplied. `create-invoke-job!` stores that exact
map beside the digest before any prompt event exists (`:1373-1382`). Terminal
compaction explicitly retains it (`:403-417`). The explicit legacy queued-job
migration binds the supplied authority and stores the same preimage at the same
time (`:1417-1462`); it does not reconstruct historical prompts.

The read API is `futon3c.transport.http/invoke-job-request-commission`
(`:1321-1347`). It recomputes the existing ledger digest from retained bytes
and returns `:agency/invoke-request-commission-v1`; missing jobs, legacy jobs
without retention, and digest mismatches refuse. The later anchor packet should
consume this API for both producer and reviewer job ids, then give its returned
`:commission` directly to `futon2.aif.r9-checker/check-independence`. It must
still independently verify cross-agent genesis and delegated canonical-branch
acceptance: this packet adds no anchor and changes no `valid-anchor?` rule.

The production-shaped test creates a bell review job with model/caller/surface,
persists it after D13 has reduced four events (including the prompt) to the
accepted/done edges, resets the in-memory ledger, reads the exact commission,
and verifies the digest is unchanged. Removing the commission refuses
`:request-commission-missing`; changing its prompt refuses
`:request-commission-digest-mismatch`.

The first full namespace run retained below is not the acceptance run. It
exposed that the fixture timestamp entered the seven-day deletion stage and
also reported unrelated existing namespace failures. Follow-up commit
`ff03ea32` pins the compaction clock inside the 24-hour-to-seven-day interval;
the targeted test then passed. Two attempts using the test alias's var path
were blocked during namespace discovery by the unrelated absent
`futon2.aif.hermetic-repair-fixture`; the successful command uses the same
project plus an explicit test Unity path and loads only the target namespace.
