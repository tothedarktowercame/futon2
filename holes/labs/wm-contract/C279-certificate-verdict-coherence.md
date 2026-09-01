# C279 — certificate verdict coherence

Date: 2026-09-01. Owner: `wm-organization`.

Two adversarial cases from C273 are now certificate conditions rather than
command-line side facts.

1. Fixture content-pin validity is written into `:fixture-pins` and
   `:checks :fixture-pins-valid?` before the certificate is persisted. An
   invalid pin forces the persisted `:verdict` to `:fail`; command exit and
   saved artifact can no longer disagree about provenance.
2. Completion evidence is schema-specific and explicit. A production click
   requires a grounded terminal outcome (`:grounded-change` or
   `:grounded-no-change`). A bounded diagnostic requires both independently
   observed terminal exits (`:command-exit 0` and `:wrapper-exit 0`). The
   committed legacy fixture requires its explicit `:service-result :success`.
   An unknown schema, or an absent member of the applicable evidence pair, is
   incomplete. No schema treats missing evidence as success.

The certificate exposes the applicable rule at
`:execution-status :completion-evidence`; resource health remains separate
from execution completion.

Focused verification:

```sh
clojure -X:test :nses '[wm-operational-certificate-test]'
# 8 tests, 31 assertions, 0 failures, 0 errors

clj-kondo --lint checks/wm_operational_certificate.clj \
  test/wm_operational_certificate_test.clj
# 0 errors, 0 warnings
```

The bad-pin control invokes the real CLI entry point and reads the written
certificate: exit `1`, persisted verdict `:fail`, fixture-pin validity false.
The diagnostic control removes one of the two terminal exit observations from
an otherwise clean, parseable resource: execution status `:incomplete` and
certificate verdict `:incomplete`.

No production click or reload was performed. The full workspace gate and full
repository suites were deliberately not run for this focused repair.
