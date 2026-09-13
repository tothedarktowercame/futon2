# E6b complete-capture codec v1

Scope: pure serialization of an already recovered, isolated store-v2 capture.
It is structural evidence only. It grants no authority, completeness,
freshness, restart permission, prospective write permission, or retrospective
success.

The encoded record has schema `:wm/e6b-complete-capture-v1` and an exact fixed
top-level field set. It retains store id, generation, the ordered unique chain,
the ordered exact five-field application index, the immutable HEAD descriptor,
and transaction/provenance objects as explicit
`{:digest :bytes/base64 :source-sha256}` descriptors. Transaction descriptors
follow chain order; provenance descriptors sort lexically by digest. This makes
equivalent byte inputs independent of input-map insertion order.

At construction, every mutable input array is cloned once. The clone is the
only buffer hashed, strictly decoded, and Base64 encoded. The codec validates:

- strict UTF-8 containing exactly one EDN form;
- descriptor hash/key/byte equality and unique complete object sets;
- exact HEAD schema, raw digest, store, generation, current transaction, and
  application-index joins;
- a unique ordered genesis-to-HEAD transaction chain with generation and parent
  joins;
- exact index-to-transaction application identities and complete reachable
  provenance membership; and
- every provenance object through the reviewed pure provenance readback.

Readback requires an externally supplied raw SHA-256, parses the exact supplied
bytes, repeats all validation, reconstructs via the constructor, and requires
the resulting raw digest to be identical. Parsed status labels are never an
authority input. Returned data contains immutable strings/maps rather than
mutable backing arrays.

This packet intentionally does not recreate the six-field retrospective ledger
or invoke `verify-feedback`. That later adapter must resolve each indexed
transaction application, retain the exact four-key retrospective input digest
view, and obtain a separately owned completeness acceptance over this exact
complete-capture digest. It must also preserve the unresolved later-prior,
installed-code, freshness, production ownership, first-install, and historical
20588 limitations.

Pinned dependencies for this contract are retained with the execution receipts.
