# U80 flagged-path binding for `policyPosteriorImportsPolicyF`

This checker binds the declaration name to the frozen S4 artifact at SHA256
`aaeccaf477dfd16bcc73064aa979f1fefa9753e2eec4ccd053e5e17acf8efdbf`.
It verifies four records, exactly three eligible positive records, complete
one-to-one semantic action-identity joins, finite numeric per-candidate F payloads,
and the one typed `:incomplete-coverage` exclusion. It pins the producer and
consumer source files. Removing an F value or changing an identity makes the
corresponding negative control red.

Each negative control first requires the unchanged fixture and source pins to
pass. Source or fixture drift is reported as baseline drift and cannot count as
detection of a mutation. Only the two named mutations are accepted, each must
change its intended in-memory evidence, and the resulting failure must have the
registered candidate-F cause. Unknown control names are refused.

The evidence scope is `:flagged-path-witness-held-open`. It does not establish
default-path behavior, rerun S4, or close the Lean declaration.
