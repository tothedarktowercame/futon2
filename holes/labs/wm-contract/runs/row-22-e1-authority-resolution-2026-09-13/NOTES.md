# E1 authority-resolution repair

`machine-budget-mapping` remains the unchecked deterministic construction
helper. `machine-budget-authority/resolve-and-map` is the verified boundary: its
API accepts only an independently supplied resolver root, mode, paths and
expected hashes. Support, values, identity, scope and provenance come from the
resolved bytes, not candidate metadata.

Each source is read once. Those same bytes are SHA-256 checked, decoded with a
strict UTF-8 decoder, and parsed as exactly one EDN form. All five sources must
agree on scope and model/run/tick identity. Their values must cover the complete
support before the canonical arbiter runs. The retained readback resolves five
isolated fixture files, preserves three occurrence ids including two equal
semantic actions, accounts for all three, and replays identically.

No supported transformation implementation is registered in this packet.
Consequently every `:supported-transformation` source refuses
`:r6-r11/transformation-unsupported`; a transformation name and pins alone are
not execution authority.

Limitations: fixtures and their resolver configuration are explicitly
`:isolated-test`; they establish the byte-resolution mechanism, not production
authority or E1 firing. `:production` mode currently refuses
`:r6-r11/production-authority-unavailable`, so relabelled test files cannot
promote themselves. A production deployment still needs independently
owned production paths and immutable expected pins for the complete ranked
support plus field, cost, utility and budget sources. E2, selection, enactment,
and live integration remain untouched. Row 18 remains open.

Source hashes:

- `machine_budget_authority.clj`: `9b205741fe3843d027ca993a7d054c37c33aeca946e700b71611c698441816af`
- `machine_budget_mapping.clj`: `5072c34fa55db6683faeef38ac2b8026107c9ef0c9d7174a9a4026ab3ec2110f`
- `machine_budget_authority_test.clj`: `390ad19e69ff59577202ff4cb9701375fa2a4313ef9012bfe682c56a3a8cdba8`
- `readback.clj`: `9d078bda20f09cbd6e001e4f9c60c1fcb2037386b007050ffeed51f70739b01e`
