# C405 — declared-not-derived boundary vocabulary

Date: 2026-09-01

Three existing records need one narrow vocabulary for an honest pin boundary.
This record does not propose a general theory of evidence boundaries.

This is the **derivation axis only**. C417 defines the orthogonal authority
axis: who writes, selects, stores, retains, can roll back, and verifies the
authority. Neither axis upgrades the other.

```clojure
{:boundary/type :declared-not-derived
 :subject <the boundary being described>
 :pinned <the exact object the check identifies>
 :not-pinned <the stronger closure or correspondence it does not identify>
 :derivation-status :derivable-not-adopted | :not-exactly-derivable
 :reason <site-specific reason derivation stopped>}
```

The two derivation statuses are intentionally distinct:

- `:derivable-not-adopted` says the stronger object can be derived, but this
  artefact did not adopt that derivation.  The reason must say why.  The Lean
  dependency closure is the measured example: it is derivable, but the
  16,107-constant softmax closure made content-pinning it impractical.  The
  fixture correspondence site is also derivable in principle, but only after
  supplying an identity-preserving decoder and equality proof that do not
  currently exist.
- `:not-exactly-derivable` says the runtime does not expose an exact graph from
  which the stronger boundary follows.  The Clojure serving closure is the
  observed example: higher-order injection, `requiring-resolve`, mutable Vars,
  and an external HTTP selector mean the namespace graph is not the program.

At the Lean receipt site, every receipt now uses the vocabulary twice:

1. dependency closure pins author-declared declaration slices and explicitly
   does not pin the complete transitive constant closure; and
2. fixture correspondence pins fixture identity, expected-value shape, and
   retained-slice occurrence, while explicitly not pinning semantic equality
   between the serialized value and Lean value.

The receipt validator requires the complete maps, so removing or changing the
status, pinned object, excluded object, or reason rejects the receipt.  C399's
serving-topology record uses the same fields with
`:derivation-status :not-exactly-derivable`.  None of the three underlying
claims has been strengthened: this change makes their common limit legible
without erasing why each boundary remains open.
