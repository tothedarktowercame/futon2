# Genesis verifier review — codex-26, 2026-09-13

Subject futon3c 580f1a2a. All four source/test/fixture/runner pins match
actual bytes (lead-pins.json). Source review, not a rerun of passing checks.
Disposition: changes requested; no verifier acceptance or anchor admission.

1. commission! recomputes the digest of a candidate-supplied envelope. It does
   not consume independently resolved invoke-job-request-commission output.
   An attacker can replace a prompt and recompute its digest without changing
   the job IDs. Resolve commissions by job ID outside the candidate, then bind
   retained job-join trace/agent fields to independently resolved trace evidence.
2. artifacts may be nil/empty, so the doseq is vacuous and verification succeeds
   with no source, tests or review. Require exact mandatory roles, proper SHA256
   pins and IDs, and independently resolve the bytes. Current fixture strings
   source-sha/tests-sha are not source-backed artifact hashes.
3. Acceptance checks delegate/main/reviewer job only, not exact source/test/review
   artifact pins or author job. A different artifact set with independently valid
   hashes can borrow the same acceptance. Bind the complete subject, root identity,
   author/reviewer jobs, commissions and review outcome to the retained acceptance.
   Distinct nonempty author/reviewer identities AND job IDs are required.
4. resolved! accepts a missing authority-origin and trusts a verified flag. Keep
   injected fixtures as tests, but define a real host boundary and evidence schema;
   do not report production readiness from flags or candidate-controlled resolver
   inputs. located-host-event-resolver does not read a file at all: it echoes
   input and always refuses. Label that a refusal stub; current probe is not an
   executed authenticated record reader. Retain the actual ownership/hash commands
   and output if asserting those observations.

Negative controls should include omitted artifacts, correctly rehashed counterfeit
commission, same job under different roles, borrowed acceptance for different
source/test pins, and missing authority provenance. Positive case must use the
real retained commission API with isolated temporary storage and real byte pins.

The existing policy does not mandate an operator cryptographic signature: an
independently verified external event under an explicit reviewed host trust model
is an allowed route. Lack of a signature alone is not proof that no route exists.
Host mutability and external-origin verification must still be addressed honestly;
do not replace them with a candidate-authored :verified assertion. No live
ledger mutation, root signing, anchor, or admission is authorized by this packet.
