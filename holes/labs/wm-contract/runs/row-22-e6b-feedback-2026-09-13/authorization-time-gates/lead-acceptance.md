# E6b authorization chronology review — 20664

Subject17e85c05/61db346f/8d5c1550. Five source/test/spec pins match current and final source commit. Read retained10tests76assertions, clean kondo/parens and induced1failure exit1; initial stale timestamp control retained. No passing rerun.

Accepted at isolated replay scope: E3 returns already verified review/admission and authorization timestamps. The exact lifecycle subject includes that receipt and canonical output digest. E6b parses and enforces review/admission < authorization <= enacted time before its existing outcome/review/destination checks. Equality is explicitly tested; borrowed receipt time and changed canonical pin refuse. Previous enactment-before-authorization hole is closed by source comparison, not merely a moved fixture.

This accepts pure conditional replay given independently configured sources. It does not authenticate production authority owners, enforce live state revisions, prevent competing stores, establish complete production ledger acquisition, or prove actual outcome observation. E3's additive output invalidates old exact result digests; consumers must resolve new outputs and pins, not relabel old receipts. Earlier historical reviews remain valid for their exact subjects.

Next bounded packet is the already specified separate slow-state store/application-ledger ownership protocol. Design compare-and-commit, atomic joint publication and recovery, complete snapshot authority and first installation boundary; no actual store or runtime mutation. All E6a semantic, Row14/18/19 and F11/full certificate obligations remain open.
