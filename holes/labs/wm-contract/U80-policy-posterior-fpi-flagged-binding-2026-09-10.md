# U80 flagged-path binding for `policyPosteriorImportsPolicyF`

This checker binds the declaration name to the frozen S4 artifact at SHA256
`aaeccaf477dfd16bcc73064aa979f1fefa9753e2eec4ccd053e5e17acf8efdbf`.
It verifies four records, exactly three eligible positive records, complete
rank-key to semantic action-identity joins, numeric per-candidate F payloads,
and the one typed `:incomplete-coverage` exclusion. It pins the producer and
consumer source files. Removing an F value or changing an identity makes the
corresponding negative control red.

The evidence scope is `:flagged-path-witness-held-open`. It does not establish
default-path behavior, rerun S4, or close the Lean declaration.
