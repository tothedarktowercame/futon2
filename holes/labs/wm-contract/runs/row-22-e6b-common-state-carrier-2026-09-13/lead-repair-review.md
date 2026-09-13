# Carrier contract repair review

Reviewed fd93f7cf against unchanged verifier/store pins (lead-repair-pins.json). Seven-role to four-key digest mapping and distinct full-record versus carrier output hashes now agree with source. Versioned provenance-first publication is a design, not implemented durability. No tests rerun.

Lead adds two exact replay obligations: ledger entry is the verifier's six-field map including prior-state/revision and excluding store-only transition/subject; canonical E3/E2b configured inputs and transitive evidence closure must survive or resolve independently after restart, since output hashes plus seven transition sources cannot reproduce their calls. No synthetic authority.

Accept projection/digest design with corrections. Next bounded implementation is only a pure common-carrier codec: complete schema validation, prior/next projections, pinned serialization and distinct digest views, exact subject joins, no filesystem/publication/HEAD/store-v2 API. Independent review of lead corrections first. Durable canonical-input closure, store-v2, prospective-to-commit adapter, later-prior acquisition and external postcommit completeness remain open.
