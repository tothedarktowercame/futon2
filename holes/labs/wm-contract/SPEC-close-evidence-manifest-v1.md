# Close evidence manifest v1

Status: mechanism specification for review. This file does not admit evidence,
produce an annotation, or change the close path.

## Records

An admitted entry is exactly:

```clojure
{:evidence/id <nonblank string>
 :source-path <absolute path string>
 :sha256 <lowercase 64-hex SHA-256>
 :admitted-at <java.time.Instant string>}
```

The evidence id resolves to the one literal byte sequence read from
`:source-path` during this admission. `:sha256` is recomputed over those bytes.
When an expected SHA is supplied to the builder, it is an integrity constraint
and must equal the recomputed digest. It is not evidence authority by itself.
`:admitted-at` is the supplied instant at which that read occurred. Validation
does not supply a clock and does not infer the instant from filesystem or Git
metadata.

The manifest is exactly:

```clojure
{:schema :wm/close-evidence-manifest-v1
 :entries [<ordered admitted entries>]
 :manifest-sha256 <SHA-256 of UTF-8 (pr-str :entries)>}
```

The implementation reconstructs every admitted entry as an ordered map in the
field order shown above before hashing. Entry order is meaningful and is never
sorted. Evidence ids are unique.

`build-manifest` accepts exactly `{:entries [...] :read-bytes f}`. Each input
entry supplies id, absolute source path, admitted-at, and optionally
`:expected-sha256`. The reader is mandatory injected I/O and is called with the
source path. There is no filesystem or clock default.

## Retention agreement and temporal meaning

`verify-retention-agreement` validates the manifest and requires the vector of
manifest evidence ids to equal the close-retention block's
`:admitted-evidence` vector exactly, including order and multiplicity. Empty
manifest and empty admitted-evidence vectors agree.

The manifest proves pre-cutoff byte existence only when the manifest itself is
retained no later than the close cutoff. The later runner/writer seam owns that
retention fact. A filesystem mtime, Git date, or post-hoc copy cannot establish
it. This mechanism alone therefore makes no claim that a manifest was present
at close.

## Typed refusals

Every refusal is `ex-info` carrying `:evidence-manifest/refusal` and `:path`.
The v1 codes are:

- `:shape-invalid`, `:schema-mismatch`
- `:identity-invalid`, `:duplicate-evidence-id`
- `:source-path-invalid`, `:source-unavailable`
- `:read-capability-missing`
- `:sha256-invalid`, `:source-sha256-mismatch`,
  `:manifest-sha256-mismatch`
- `:timestamp-invalid`
- `:retention-evidence-invalid`, `:retention-evidence-mismatch`

Naming an evidence id without resolving and hashing its literal bytes is not
admission.
