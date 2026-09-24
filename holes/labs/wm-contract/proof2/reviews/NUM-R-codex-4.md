# NUM-R independent Codex review

Date: 2026-09-24. Reviewer: codex-4 (Codex, GPT-6); not the implementer.
Reviewed commit: `604ecfd724fc0488ec1425b15433472ca869fbae`, author seat kimi-3.
Specification: SPEC-N §3 at `634877af`, its codex-14 review, CERT-S §§1/3, and the PROOF-2 strategy NUM-R boundary. The governing A16 recorded-value distinction and W/P/X separation remain unchanged.

**Verdict: REJECT.** The registered tests pass, but the decoder rejects valid canonical binary64 subnormals, accepts noncanonical exponent spellings, and lets an oversized exponent escape as an untyped exception. The subnormal defect is positively enforced by an incorrect test. These are implementation/test repairs, not grounds to weaken SPEC-N. The namespace choice is correct for the repository classpath.

## 1. Record check and the single registered run

`git show 604ecfd7 --stat` contains exactly two added files: `checks/proof2_numbers.clj` (176 lines) and `test/checks/proof2_numbers_test.clj` (134 lines). The reviewed commit says `Warrant: PENDING-REGISTRATION`; it provides no warrant id. No changes to these two files existed relative to that commit when reviewed.

Executed once through `AUTHOR=codex-4 scripts/wm/register-warrant.sh --pinned 604ecfd7 checks.proof2-numbers-test`. The script itself runs the namespace and registers it, so I did not run a separate test invocation. It used a temporary detached sibling worktree pinned to the reviewed commit and removed that worktree on completion. The existing shared checkout's other files were not staged or altered.

- Warrant id: `test-registry-b6d9f220c4ac845061ed59a5ede485a2be686cbbd0df2e35d6d473900dc13334`.
- Run id: `b30ff698-76cf-4203-a3ff-3461742af2e6`.
- Result: **8 tests, 32 assertions, 0 failures, 0 errors, exit 0**; `:warrant? true`, `:execution/stable? true`, `:postcheck {:status :matched}`.
- Recorded command: `["clojure" "-M:test" "-n" "checks.proof2-numbers-test"]`.
- Source SHA-256: `0022b86f73d051ddafb07328bd71c9a948a3c5904e4be6a662bff7c48ab2a7ad`.
- Test SHA-256: `8d977be0a175f5e1c0c8e8bf5dbc22fddc6bc66dc4f41533afee8dccdffbcf81`.
- Log: `/home/joe/code/storage/test-registry/artifacts/b30ff698-76cf-4203-a3ff-3461742af2e6.log`, SHA-256 `e490cb0bef24e9a85d7c7e21283d1763f4268e71e1b9b80a450ce87ec636cb5c`; registry ledger `/home/joe/code/storage/registry-ledger`.

I read the resulting log and compared source/test file hashes with the warrant. Both match. This warrant replaces repeat runs of these unchanged bytes; it certifies the observed test result, not test adequacy. Corrected source/tests require a new warrant because their content changes.

Additional counterexamples below were evaluated as direct decoder calls in a fresh local `clojure -M -e` process, not a rerun of the test namespace or a shared JVM load. No WM click, runtime reload, or write under `data/` occurred. The only external write was the explicitly requested test registration and its artifacts.

## 2. Blocking findings

### R1 — valid short canonical subnormals are rejected

At `checks/proof2_numbers.clj:125–133`, the implementation asserts that canonical subnormals have exactly thirteen fractional hex digits and rejects any other length. That premise is false: canonical printing strips trailing zeros for subnormals too.

Observed direct calls:

| Input payload of `#wm/double` | `Double/toHexString(Double/parseDouble(input))` | Decoder result |
|---|---|---|
| `0x0.1p-1022` | `0x0.1p-1022` | typed refusal `:double-not-binary64`, fraction length 1 |
| `0x0.8p-1022` | `0x0.8p-1022` | typed refusal `:double-not-binary64`, fraction length 1 |

The mathematical check is independent of the round trip: the first denotes `2^-1026 = 2^48 × 2^-1074`, the second `2^-1023 = 2^51 × 2^-1074`. Both have a nonzero fraction M below `2^52`, E=0, and therefore satisfy SPEC-N's exact subnormal formula. Both are lawful canonical record values.

The test at `test/checks/proof2_numbers_test.clj:87–89` explicitly expects the first value to be rejected. Merely making that test pass perpetuates the defect. Replace this mistaken negative with an exact positive, add short positive/negative subnormal encodings, and keep a genuine nonrepresentable subnormal negative (for example a nonzero bit below `2^-1074`). For canonical nonzero subnormals, fractional length may be 1 through 13; require the canonical grammar and trailing-zero rule, exponent -1022, and actual binary64 range/precision. Do not replace a rejected value with zero or absence.

### R2 — canonical exponent validation is incomplete

At `checks/proof2_numbers.clj:86–87,103–104`, `-?[0-9]+` permits leading zeros and negative zero in the exponent. Parsing into a long loses the spelling distinction, and nothing checks it afterward.

Reviewer-created bad case absent from the tests: `#wm/double "0x1.0p00"`. Observed result is successful decoding to rational `1N` with the noncanonical input retained as raw hex. Its canonical Java spelling is `0x1.0p0`. The separate input `0x1.0p-0` also incorrectly succeeds. **The decoder does not catch either bad case.** SPEC-N requires canonical hexadecimal grammar, not merely an equivalent real value; CERT-S uses canonical bytes for identity.

Require canonical exponent spelling and add these negatives. Canonical exponent text is zero, a positive integer without leading zeros, or a negative nonzero integer without leading zeros. The existing fraction-canonicality tests do not cover this independent condition.

### R3 — malformed exponent violates the typed-refusal contract

`#wm/double "0x1.0p9223372036854775808"` passes the regex, then `Long/parseLong` at line 104 throws `java.lang.NumberFormatException` with no ex-data. This was observed directly. The public decoder's lines 150–157 promise typed refusals for other forms. This input is outside binary64, and must receive a typed range/grammar refusal rather than escape through an unrelated exception class.

Validate bounded exponent syntax/range before unsafe conversion, or handle conversion failure into the specified typed refusal. Add both signs of oversized exponents. This does not require assigning any numeric meaning to the malformed value.

## 3. Required-case test adequacy

Line numbers refer to the reviewed test file. “Would fail” below is a source-level mutation analysis of the named assertion, not a claim that a mutation suite was run. Only the original namespace was run once.

| Required case | Existing test and lines | Adequacy / effect of removing the relevant behavior |
|---|---|---|
| Swapped numerator/denominator | `integers-and-ratios-decode-exactly`, 21–24; misleadingly named `unreduced-and-swapped-ratios-refuse`, 26–49 | The exact `3/4` and `-1/16` output assertions would fail if decoding inverted numerator and denominator. But 4/6 is unreduced, not a swapped-pair test; 3/-4 tests sign placement, not swapping. There is no explicit paired 3/4 versus 4/3 test. Both are valid ratios, so a decoder cannot reject a swapped valid input without an expected source value. Correct the test description and add the pair to distinguish transcription orientation from source-correspondence checking. |
| Unreduced ratio | `unreduced-and-swapped-ratios-refuse`, 34–49 | Constructed 4/6 exercises coprimality; 0/5 exercises canonical zero; 3/-4 exercises denominator positivity. Removing the corresponding guard returns a value instead of the expected refusal kind, failing its assertion. Zero denominator is not explicitly tested. |
| Decimal literal | `decimal-literals-never-accepted`, 51–58 | Double 0.333, double 1.0 and BigDecimal 0.1M require `:decimal-literal`. Removing that branch gives a different fallback refusal, so these tests fail even though the fallback still rejects. A permissive decimal conversion would also fail. The comment says Float is tested, but no `(float ...)` input exists. |
| Real hex not binary64 | `hex-double-grammar-and-representability`, 60–67 | `0x1.000000000000001p0` needs 57 significand bits. Without the precision guard the exact-arithmetic decoder returns the nonrepresentable dyadic, so the expected refusal fails. Normal exponent range boundaries are not separately covered. |
| Infinity and NaN | Same test, 68–76 | Tagged Infinity, -Infinity and NaN all require `:double-non-finite`. Removing that branch produces generic noncanonical-hex refusal, so the typed-result assertions fail. They establish the finite decoder's classification, not mathematical infinite-risk handling. |
| Signed zero | `signed-zero-and-subnormal-decode`, 113–120 | Requires both rational values zero, explicit false/true sign flags, and distinct raw identities. Removing sign preservation fails at least the flag assertion; raw inequality alone would be weaker because the original hex strings already differ. |
| Subnormal | Same test, 121–123; grammar test, 87–91 | Smallest subnormal equals exact `2^-1074`, so wrong scaling/underflow-to-zero fails. Wrong exponent for that bit pattern is refused. Coverage is nevertheless defective: line 89 enforces rejection of a valid short subnormal. R1 must be fixed, not waived by the smallest-subnormal success. |
| SPEC-N pinned log encoding | `spec-n-example-decodes-exactly`, 93–99 | Exact expected `6243314768165359/9007199254740992` and raw hex/sign are independently pinned. Wrong exponent/significand arithmetic or a rounded decimal substitution fails. This is decoding the machine number, not proving equality to real log 2. |
| Record 1790199409 posterior residual | `record-1790199409-posterior-sum`, 101–110 | Two fixed record-derived hex encodings must sum to `1 + 7/36028797018963968`, explicitly not 1. Rounding the result to normalized 1 would fail. This is a realistic shape fixture for exact scalar decoding, not an end-to-end record extractor or a vector-normalization test: no vector decoder exists here. |

Other-form rejection at lines 128–134 covers keyword/string/map/nil, but not a non-string `wm/double` payload or wrong tag. Those are reasonable supplementary tests; the three observed blocking findings above suffice for rejection. No claim is made that the nine requested categories constitute exhaustive malformed-input coverage.

## 4. Namespace and value-boundary assessment

`deps.edn` declares `:paths ["src" "resources" "." "scripts"]`, and `:test` adds `"test"`. The classpath root `.` resolves `checks.proof2-numbers` to `checks/proof2_numbers.clj`; the added test root resolves `checks.proof2-numbers-test` to `test/checks/proof2_numbers_test.clj`. The successful registered require/run also demonstrates that resolution.

Therefore **the chosen namespace is right for the pinned file path**. `futon2.checks.proof2-numbers` would conventionally need `futon2/checks/proof2_numbers.clj` relative to a classpath root (for example under `src/`), a file not delivered by this packet. Accept the dispatch-name amendment to `checks.proof2-numbers`; do not move working code merely to preserve a mismatched instruction. The docstring's “only lawful” wording is stronger than necessary—manual loading can bypass ordinary lookup—but ordinary namespace resolution supports this choice.

The decoder consumes already-read Clojure values, not record bytes. Its own comment correctly observes that the EDN reader normalizes literal ratios before it receives them. Consequently `:raw :printed` cannot establish whether raw record bytes originally said 4/6 or 2/3, nor preserve every original integer spelling. The constructed-Ratio tests establish validation of the supplied object, not lexical canonicality of the input file. Keep this boundary explicit for GEN-E/CERT-S: canonical byte validation must occur at the reader/codec boundary, and a value decoder cannot retroactively authenticate a lost spelling. If NUM-R is intended to own lexical preservation, amend its API to accept the source token/bytes; otherwise document that obligation in the extractor. Do not describe the current output as proof of raw-file identity.

## 5. Required resubmission

Repair R1–R3 in the author lane, correct the swapped-ratio and Float coverage descriptions, and add tests for the observed defects. Retain the exact log and posterior-residual fixtures. Register the changed namespace once with new content hashes and submit the new diff for independent review; the current warrant remains evidence for the old bytes only. No theorem clause, numeric refinement bridge, or live certificate is certified by this review.
