# b1: evidence-only proposal supply

The exact-assurance branch is unavailable pending typed target-link evidence.
`join-audit.edn` records the file-scanned census: 328 missions, 36 T-tickets,
14 assurance rows with exact pattern/signature matches, and no U-row identity
among those targets. Codex-1's `cb2045b8` admission census classified those
rows as 12 done and two ruling-blocked. No mission link or U-row target kind
is invented. Claude-12 ruled this boundary in
`invoke-1789961691882-22844-1cd0d1fe`.

Claude-12's earlier ruling (`invoke-1789961312569-22838-7089b6ad`) also
settles the interpretation boundary: proposals carry evidence only. An agent
writes the complete reading, guard, produces, locators and interpretation
receipt. Signature prose and retrieval scores establish no applicability.

## Implemented path

`interpretation-request/prepare-proposal!` reuses the pinned target/tension,
retriever and library capture path. It mints no execution occurrence and
requires no fictional dispatch. The existing authorized `prepare!` keeps its
identity checks. Both successful and failed proposal requests omit execution
identity and action fields.

`cascade-proposals/generate-retrieval!` records the request plus immutable
source snapshots in a fresh directory. Only retrieval identities backed by
captured pattern bytes become `:retrieval-proposed`, `:proposed` records.
Unresolved patterns, unavailable retrieval and absent tension produce declines.
The reader verifies snapshot hashes and derives the proposal list from the
retained retrieval; editing a proposal's status cannot admit it.

The tick reads these records, never invokes retrieval inline. Proposal evidence
is retained under `[:decision :selection-certificate :proposal-supply]`.
Pending admission, missing evidence and invalid snapshots use packet (a)'s
`:dropped-candidates` channel. Packet (a) landed at `c155d690` before these
runner edits. No admission gate is replaced or weakened.

`mission-hole-wants` still projects stated wants and their existing checkbox
witnesses. It no longer assigns one generic closure pattern or manufactures
candidate construction receipts. A statement of a want is not an interpretation.

## Generate and admit

From futon2, explicitly generate a mission proposal batch:

```sh
clojure -M -m futon2.aif.cascade-proposals M-a-wmc-scaling mission
```

The default store resolves under the canonical code root, independent of the
serving JVM's working directory. An alternate directory is the third argument;
`:cascade-proposals-dir` selects it for the tick. Files are outside the
`cascade-sources` declaration directory, whose loader treats every EDN as a
complete declaration.

An agent reviews the captured target and pattern, then writes a complete
`:wm/cascade-source-v1` declaration in the usual declaration directory. It
uses the same `cascade-sources/load-declared` path as M-wm-08-external-f2.
Each interpretation receipt retains `:kind`, `:by`, `:date`, pinned `:source`
and the author's actual `:reading`, plus `:proposal-id` naming the proposal.
The declaration supplies real guards, produced tokens, mechanically supported
locators and its construction receipt. No file, token or witness path is
generated from pattern prose by this pipeline.

The supply join checks that receipt reference against target, pattern identity
and pattern byte hash. A mismatch refuses; it never rebinds an old proposal to
new bytes. The existing downstream gate still decides executable membership.
Unlinked existing hand admissions remain ordinary declarations. Captured byte
hashes identify the reviewed text; the inherited capture's revision field is
repository context, not a proof that dirty working bytes belong to that commit.

## Validation and limits

The new tests cover persisted proposals, absence of semantic/admission fields,
status tampering, missing pattern pins, absent tension, damaged snapshots,
double retrieval failure, the real assembly/admission/certificate path, and
loading the existing M-wm-08 declaration shape with a proposal reference.
Target and byte mismatches are constructed negative controls.

No code is loaded into the serving JVM by this packet. Committed is not loaded.
No live author action is dispatched. Exact-assurance generation remains
unavailable as ruled; this packet does not claim the original exact-join
positive acceptance case has been implemented.
