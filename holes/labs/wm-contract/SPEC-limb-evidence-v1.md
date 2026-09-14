# Limb evidence v1

Status: pure mechanism specification for review. This packet creates no
production evidence and performs no runner or cohort integration.

## Limb receipt

An executing author creates one strict one-form EDN record at execution time
for one mechanical discharge-contract limb:

```clojure
{:schema :wm/limb-receipt-v1
 :repair/id <nonblank string>
 :limb <contract keyword>
 :command <literal nonblank command line>
 :exit <integer>
 :stdout-sha256 <lowercase 64-hex digest of captured stdout bytes>
 :stderr-sha256 <lowercase 64-hex digest of captured stderr bytes>
 :recorded-at <Instant string>}
```

A prose claim that tests passed is not a receipt. The captured stdout and
stderr byte identities are mandatory even when either stream is empty.

## Target standing decision

A seat distinct from the implementation author decides, before close, whether
the same selected target remains live or has been resolved:

```clojure
{:schema :wm/target-standing-decision-v1
 :entity/id <nonblank selected-entity id>
 :decision :still-live|:resolved
 :decided-by <nonblank seat id>
 :implementation-author <nonblank different seat id>
 :decided-at <Instant string>
 :evidence [<one or more nonblank record ids>]}
```

The record refuses a self-decision. Neither actor is defaulted.

## Entity revision pair

The selected entity itself is captured at the action boundaries. An
implementation commit is not a substitute for either entity capture:

```clojure
{:schema :wm/entity-revision-pair-v1
 :entity/id <nonblank selected-entity id>
 :before {:source-path <nonblank path> :sha256 <64-hex> :captured-at <Instant>}
 :after  {:source-path <nonblank path> :sha256 <64-hex> :captured-at <Instant>}
 :dimensions [<one or more keywords naming what narrowed or changed>]}
```

Equal before/after SHA-256 values refuse: unchanged bytes are not a revision.

## Admission, cutoff, and coverage

Each record must be created before the close and admitted by literal bytes into
that close's evidence manifest no later than its cutoff. These validators prove
shape only; the later wiring packet owns creation, one-form reading, admission,
and temporal linkage.

`validate-limb-bundle` validates a strict `{:requires [...]}` discharge
contract and all supplied records. Only limb receipts cover required limbs.
The result preserves requirement order and contains `:covered` and `:absent`
vectors. Missing coverage is an `:incomplete` report, never a validation error.

The fourth limb, `:distinct-production-shaped-successor`, is evidenced by a
later close. It is intentionally outside these pre-close record shapes and will
normally remain absent in the current close's bundle.

## Refusals

All refusals are `ex-info` with `:limb-evidence/refusal` and `:path`:

- `:shape-invalid`, `:schema-mismatch`
- `:text-invalid`, `:keyword-invalid`, `:exit-invalid`
- `:sha256-invalid`, `:timestamp-invalid`
- `:standing-decision-invalid`, `:standing-decision-not-independent`,
  `:standing-evidence-invalid`
- `:revision-unchanged`, `:dimensions-invalid`
- `:requires-invalid`, `:records-invalid`
