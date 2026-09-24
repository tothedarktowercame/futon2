# Independent review — OBS-D

- Reviewer: `codex-23`
- Review job: `invoke-1790226086975-23612-69dee469`
- Packet: `proof2/packets/OBS-D.md`
- Packet commit: `3277e9e6`
- Verdict: **REJECT**

## 1. Record check

`git show 3277e9e6 --stat` changes only
`holes/labs/wm-contract/proof2/packets/OBS-D.md` (315 inserted lines). The
packet therefore satisfies the single-packet record boundary.

## 2. Independent citation and record checks

I opened the cited sources at their pinned revisions rather than relying on
the packet's line numbers.

1. `b03017...:src/futon2/aif/token_outcome.clj`: `compare-token-outcome`
   consumes the measurement's `:observed`, returns typed absence for missing,
   duplicate, non-boolean, and revision-mismatched measurements, and derives
   its verdict from prediction versus observation. **Held.**
2. `b03017...:src/futon2/aif/observation_checks.clj`: C4 is an anchored
   declaration check at a resolved commit and emits `:observed`, `:check`, and
   evidence. **Held.**
3. `b03017...:src/futon2/aif/accepted_increment.clj`: the accepted-increment
   calculation reuses C3/C4 observations and makes `:accepted?` a compound
   verdict. **Held.** It is not independent truth.
4. `b03017...:src/futon2/aif/run_ending_classification.clj`: classification
   uses a fixed projection and describes itself as record-only. **Held.** It
   is not an observation/truth pair.
5. `77fdbda5...:DarkTower/WarMachine/TokenObservation.lean`: `falseNeg` and
   `falsePos` require an established distinction between prediction and
   truth. **Held.** A close verdict cannot silently occupy the truth leg.
6. `93531d41:src/futon2/aif/full_loop_runner.clj`: close assembly includes the
   four packet categories, but also `:learning-trial-receipt` and
   `:kernel-example`. **The packet's completeness claim did not hold.** Both
   omitted values carry token/outcome evidence.

Using `bb` on
`data/wm-full-loop-machinery-76/wm-contract-machinery-76-v1/attempt-002/007-closed.edn`,
I verified the key path
`[:payload :judgment :route-attestation :token-outcome-comparison]`. It is a
`:wm/token-outcome-comparison-v1` record with `:status :compared` and a token
row containing predicted and observed values. The comparison is also copied
at the root judgment level.

### Population reproduction

I read all ten `007-closed.edn` files under machinery 72 through 76, attempts
001 and 002, plus `resources/wm/eig/held-out-calibration.edn`. The closes
contain nine `:compared` comparisons and one typed `:absent` comparison
(machinery 75 attempt 002). Among the nine comparisons, eight observations
are false and one is true. The held-out file has two calibration rows (run
ids `1790184736` and `1790187227`) and one excluded row (`1790189901`). None
of these records supplies an independent boolean truth adjudication, so the
packet's examined population yields zero eligible pairs. That zero is only
valid for the examined inventory; the omitted close carriers must be added
to the census before claiming completeness.

## 3. Concrete falsifier

A concrete bad case is the machinery-75 attempt-002 typed absence treated as
boolean false, with an `:accepted-increment :accepted? true` verdict supplied
as truth. The proposed eligible-pair checks reject it twice: the observation
leg is not a boolean locator report, and the truth leg lacks an independent
truth source plus revision join. No current executable validator enforces the
proposed pair schema, however; today this is caught by the proposal's admission
rules only. OBS-P must make that refusal executable.

The proposed definition excludes the accepted-increment **verdict** from both
legs by construction: observation has a locator/report shape, while truth has
an independent adjudication shape. Underlying locator evidence used by an
accepted-increment conjunct may be re-cited as observation evidence, but the
compound verdict may not. This is presently a proposed schema, not an
implemented invariant.

## 4. CERT-S key-path cross-check

The proposed certificate path
`[:decision :selection-certificate :model-inputs <id> :A]` and its keys
`:rates`, `:estimator`, `:population`, `:window`, `:counts`,
`:source-row-identities`, `:value-sha256`, and `:consumed-at` match CERT-S
sections 1 and 3.

The upstream paths `:token-outcome-comparison`, `:accepted-increment`,
`:run-ending-classification`, `:occurrence`, and `:outcome` are not CERT-S
certificate paths. The proposed nested `:token-outcome-pairs` path and
`:pair-sha256` are also absent from CERT-S. In particular, `:pair-sha256`
does not specify canonical bytes or whether it is a CERT-S content reference
or a raw-record identity. That must be an explicit CERT-S/GEN-D amendment,
not an implicit new hash meaning.

## 5. Sibling cross-check

- **THEOREM W1 vs OBS-D:** W1 says "extracted accepted outcome rows" without
  the eligible-pair definition. Read literally, it can admit the
  accepted-increment verdict that OBS-D excludes. Both cannot be followed
  unambiguously until W1 adopts the OBS-D eligibility definition.
- **CERT-S A row vs OBS-D:** the final A shape agrees, but CERT-S does not name
  the proposed pair carrier or eligibility predicate. This is an omission,
  not a value contradiction, and requires amendment before the carrier is
  authoritative.
- **GEN-D vs OBS-D hashing:** GEN-D distinguishes raw run-record SHA identity
  from canonical extracted-value hashing. OBS-D's embedded `:pair-sha256`
  defines neither. Treating it as both would contradict GEN-D's distinction;
  its byte domain and role must be stated.

OBS-D labels its ASSUME/THEOREM changes as amendments and does not justify a
claim by appeal to a ruling. The additional CERT-S/GEN-D changes identified
above are not yet fully stated as amendments.

## 6. Verdict

**REJECT.** The first defect to fix is the incomplete four-site inventory.
At pinned runner revision `93531d41`, `:kernel-example` and
`:learning-trial-receipt` are additional close-time token/outcome carriers.
Add both, inspect their retained rows, and recompute the eligible population.
Then (1) define `:pair-sha256` consistently with CERT-S/GEN-D, (2) amend W1
and CERT-S with the eligibility boundary, and (3) make the bad-case refusal
executable. The currently examined records still support 9 compared, 1 typed
absence, 2 calibration rows, and 0 eligible pairs; they do not support the
stronger claim that every close-time carrier was inventoried.
