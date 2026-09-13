# E4 independent review — 20635

Subject: 0c0aa613 / a91c4dd9. Source and test bytes match the subject commit and both final receipt SHA256 values (see lead-controls-receipt.json). Retained final stdout/stderr show 4 tests / 21 assertions, clean kondo and explicit-path parens. Passing gates were not rerun.

Verdict: incomplete isolated causal evidence; production refusal retained. Newly executed lead-controls.clj exits 0 after asserting that all three invalid cases still return :verified-causal-route: absent observation channels, absent actions on both joined row sets, and simultaneous deletion of a candidate from prediction/R8 support and rows. These controls repin changed fixtures, demonstrating missing semantic/completeness checks rather than bypassing a digest.

The implementation joins tick identity but never connects a resolved observation payload or prediction value to the R8 input. It compares action equality without requiring actions, and compares two locally declared supports without independently fixed complete membership. The idempotent launch field is a declaration, not evidence of uniqueness across actual launch history. UTF-8 handling rejects valid U+FFFD as well as malformed bytes; use a reporting decoder.

Next bounded repair: validate typed payloads/actions and exact R2/prediction-to-R8 input references, resolve independently fixed full occurrence membership, retain typed initial-tick absence, and distinguish declared idempotence from witnessed launch uniqueness. Keep production unavailable and R8 arithmetic proof separate. No live changes or authorization.
