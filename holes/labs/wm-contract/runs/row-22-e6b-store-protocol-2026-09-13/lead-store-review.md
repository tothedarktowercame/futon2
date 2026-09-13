# Independent isolated-store review

Reviewed final source b97186c0 and receipts 6e526b66. All five receipt pins match current and historical bytes (lead-controls/pins.json). Retained raw gates show 10 tests / 27 assertions, zero failures/errors, clean kondo/parens; deliberate failure shows one failure and receipt exit 1. No passing suite was rerun. Independent c8805944 design review is consumed.

Acceptance withheld. Two new isolated temporary-directory controls executed with exit 0:

1. Changing only HEAD state/revision and state-sha256 to invented values leaves the referenced genesis unchanged. recover accepts both inconsistent records, and capture returns local-chain-consistent? true. validate-chain! does not join HEAD state fields to the head transaction. It also does not join each child's prior revision/state digest to its actual parent's next/genesis state (source inspection).
2. Adding an Object to the application payload lets compare-and-commit! return successfully after publishing HEAD. Recovery then refuses invalid-edn. Only next state is round-trip validated; application, authority extras and full proposal/transaction are not validated before publication.

Raw controls, stdout/stderr and pins are retained in lead-controls/. These are local consistency failures, distinct from the acknowledged inability to detect rollback to a valid old HEAD without external freshness authority. No production root or live control was touched.

Next bounded packet: common strict publication/recovery schema and complete EDN round-trip boundary; HEAD/head-transaction and every child/parent state revision/digest/generation join; exact proposal/application consistency and revision rules. Validate before any disk publication, preserve previous recoverable head on invalid input, retain strict no-overwrite publication and owner poisoning on uncertain writes. Add commissioned controls and missing-parent/capture completeness checks as appropriate. Production construction, completeness authentication, rollback freshness and runtime integration remain unavailable.
