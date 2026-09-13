# Row 24 immutable authority-buffer reader

The lead corrections in `89133782` are accepted. Acquisition must capture one
byte buffer before hashing or parsing; the drawing ledger is discovery rather
than corrected E1–E6 authority; and chronology is lifecycle-specific rather
than a universal checkpoint order.

`futon2.aif.authority-buffer/capture!` is an isolated helper. Its caller must
supply an expected path, digest and format from external configuration. It
reads once, clones the bytes, checks the raw SHA-256, decodes strict UTF-8 and
parses exactly one EDN or JSON value. It returns immutable text/value data, not
the backing byte array. It grants no ownership or production approval.

`resolve-pointer!` traverses one exact vector pointer against that retained
value and reports both the raw-source SHA and the pointed value digest. These
are deliberately distinct. Missing or ambiguous pointer shapes refuse.
Refusals also cover missing configuration/source, pin mismatch, invalid UTF-8,
malformed/trailing forms and unsupported formats.

The isolated tests demonstrate retained replay after the file changes and
refusal when the changed file is recaptured under the stale authority digest.
No emitter output or production source was touched. Future F11 integration
must supply independently authenticated configuration and lifecycle-specific
pointer/timestamp rules; this helper does not construct obligation universes.
