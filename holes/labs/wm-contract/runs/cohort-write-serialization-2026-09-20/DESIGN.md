# Cohort write-time EDN serialization

Authority: Joe's request via claude-12 bell
invoke-1789940633988-22789-4eb16b00. The adopted rule is to record an unreadable
payload as a typed placeholder at write time, rather than halt the run or defer
the error to a later ledger read. Implementation owner codex-5; independent
review owner claude-12.

The private write-new! path now prints and reads back the emitted string with
clojure.edn/read-string. Successful output uses that ORIGINAL string plus the
existing LF, retaining prior bytes. It does not regenerate valid output from
the decoded object or infer readability from Java types.

On failed emission/readback, readable map fields and keys remain intact;
unreadable values become {:payload/status :non-edn-payload,
:reason :not-round-trippable, :summary <sanitized prefix up to 256 chars>}.
An unreadable collection value, including the captured dispatch argument
vector, becomes one placeholder. Unreadable keys replace the containing map
rather than risk key collisions. A tagged/custom map that has readable fields
but unreadable printed representation also becomes a placeholder, so the tag
failure cannot silently disappear. Affected parent maps are rechecked before
writing. Control/format characters and hexadecimal object addresses are
sanitized in the diagnostic prefix. Printing exceptions/stack overflow also
produce a marker. Filesystem errors and CREATE_NEW collision behavior retain
the existing filesystem contract; no reader, checkpoint validator, or selector
changes are included.

Regression tests reconstruct the retained baseline event layout with actual
functions/atoms at its #object sites, using a TEST-ONLY tagged reader. They
first prove its pr-str still fails the production EDN reader, then pass it
through the actual writer and use the unchanged cohort/read-edn reader to
verify the placeholder and event identity. The public append/checkpoint/close
path and actual cohort/ledger show continued execution. The saved artifact's
SHA-256 is checked by the test, and it is included in the cohort warrant scope:
850cb9737d9c22ad6d5b35e0e987c8fe27ba2e664a7bd127aaf90026292ec536.

The valid control compares actual UTF-8 file bytes to the previous writer
formula, (str (pr-str value) "\n"). Other controls cover a function, atom,
plain object, unreadable map key, tagged record, and valid sibling fields.
No tests or guards are removed. The runner tests remain unchanged.

This resolves the write-new! boundary identified in the migration report;
it does not rewrite already-corrupt historical files, change other writers,
or claim that a placeholder supplies the missing evidence.
