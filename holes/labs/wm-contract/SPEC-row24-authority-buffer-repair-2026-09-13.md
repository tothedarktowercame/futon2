# Authority-buffer parser and immutability repair

JSON is now scanned with Jackson strict duplicate detection and explicit root
token counting before Cheshire conversion. Trailing roots and duplicate keys,
including nested duplicates, refuse; one `null` or `false` root remains a real
value. EDN EOF uses a fresh object sentinel, so an authored keyword cannot
impersonate EOF.

Pointer traversal uses an index rather than key truthiness. Nil and false map
keys are supported explicitly, and trailing segments must resolve. Captures no
longer expose parsed mutable objects: only immutable retained source text and
its raw digest are returned. Every pointer read rechecks that text against the
captured digest and reparses it, so editing the public capture map refuses.

Value digests use the exact recursive canonical function presently used by
F11: maps sort keys by `pr-str`, sets sort members by `pr-str`, sequential
values become vectors, then the canonical value is printed as UTF-8. This is a
compatibility contract with that helper, separate from raw source SHA-256; it
is not a general canonical-EDN standard.

The first focused run exposed the unavailable Jackson databind dependency and
is retained. The implementation was changed to token scanning plus Cheshire,
then the final focused run passed 5 tests/20 assertions. No emitter or runtime
integration occurred.
