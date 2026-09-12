# Node witness receipt interface

Run `bb scripts/merge_witnesses.bb --output /tmp/witness-review.edn` from
futon2. `--fragments DIR` selects owning fragments. `--authorities FILE`
accepts an EDN map with `:roots` (repo strings to absolute roots) and
`:equations`, `:nodes`, `:fundamentals` pairs `[repo relative-path]`.
Defaults use the canonical sibling checkouts. No live services are consulted.

Fragments preserve the existing `:witnesses` owner and all other fields.
Optional `:node-witnesses` is a vector of `:wm/node-witness-v1` records using
the adopted TN's fields. IDs are optional; supplied IDs must be unique.
Against an existing output, their semantic identity and artifact pins cannot
change. Admitted verification/review pins cannot change under the same ID
either. Use a new revision for those changes. This compares the supplied
prior output; it is not a separate global ID history service.

Locators have `:repo`, relative `:path`, SHA-256 `:sha256`, and a selector.
Lean selectors use `:module` and qualified `:declaration`. EDN selectors use
nonempty `:selector` vectors interpreted by `get-in`. Optional `:lines [a b]`
is checked for bounds. Paths, symlinks, pins and selectors resolve from a
single read cache. Unsupported or incomplete admission claims refuse before
any output is replaced; proposals remain explicitly pending.

Verification and semantic review wrappers use
`{:status :verified :receipt <EDN locator>}`. Their selected records both
contain `:subject`, exactly the projection returned by `node-witness/subject`.
The verification record requires `:executed? true`, `:exit 0`, `:result :passed`,
a nonblank `:command`, a pinned `:transcript`, and nonempty `:dependencies`
containing the exact artifact and subject locators. The review record requires
`:verdict :approved`, a named `:reviewer`, exact `:verification` receipt locator,
and pinned nonempty `:dependencies`. These are retained evidence interfaces:
assembling a synthetic record does not constitute executing or reviewing it.

The declaration census locator selects a map whose `:declarations` maps
qualified names to `{:source {:repo … :path … :sha256 …} :kind …}`. All kinds
must resolve the bound subject declaration through this census. Lean theorem
entries additionally contain `:module`, nonblank `:proposition`, and `:axioms`;
`sorryAx` is refused. Lean verification also requires `:typecheck :passed`,
`:axiom-check :passed`, nonempty pinned `:import-closure`, `:toolchain`, and
`:checker`. The census must come from the checker, never source-name grep.

Commissioning artifacts select an executed case with `:case`, `:expected`,
`:observed`, `:executed? true`, `:result :passed`, `:production-entrypoint`,
and pinned `:mechanisms`. The verification record repeats the same case,
entry point and mechanisms. Expected and observed must both exist and agree.

Replay artifacts select the retained original capture. Verification names
that exact locator as `:capture`, supplies nonempty `:source-identity`, and
pins `:checker` plus an `:assertion` record with `:passed? true`, the exact
`:capture`, and matching claim `:scope`.

The generated vector retains its format; comments carry admissions,
dependencies and the digest of parsed fragments plus every resolved dependency.
`--check` checks that digest as well as entries when node claims exist.
Canonical registry/accounting publication is separate from this implementation.
