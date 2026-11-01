# L5-4g — Relation producer: derive relations for excursions and tickets; declare the embedding floor (claude-2 → codex-5, 2026-11-01)

Base: futon2 `main` at `c0a58b0c6`. Authority: `reconciliation/DECISIONS-by-reference-claude2-2026-11-01.md`
§2–3 and `FINDING-relation-coverage-claude2-2026-11-01.md`: with the live relation context the
producer classifies 92 of 626 open tasks; all 255 E- and 190 T- targets and 89 M- targets are
unknown. Scope: `src/futon2/aif/focus_receipt.clj` (`classify-target`, `m-derivation`,
`stated-relation-path`, `embedding-neighbour`), its tests, and the focus inputs resource
`resources/wm/focus/commit-facets-v1.json` for the floor. Read `classify-target` (lines ~270–400)
before anything; the derivation is gated by `(str/starts-with? target "M-")` and by a non-nil
relation context.

## Behaviour

1. **E- targets derive like M- targets.** Remove the prefix gate for E-: an excursion with no
   declared row derives through (a) stated-relation hops over its text (excursion files live under
   `<repo>/holes/E-*.md`; extend `default-mission-text` to find them) and (b) the embedding
   neighbour if the excursion is embedded; otherwise typed unknown with the existing reasons.
2. **T- targets derive through the parent.** A ticket's parent (existing `ticket-parent`) is
   classified with the same derivation; the ticket inherits the parent's class with
   `:derived-via {:kind :ticket-parent :parent … :then <parent's derived-via>}`. No parent →
   `:no-parent-relation` as today.
3. **Declared embedding floor.** The inputs resource gains `:embedding-floor <cosine>` (start at
   0.0 so behaviour is unchanged until Joe sets a value; the producer refuses to derive when the
   floor key is absent, so the parameter is declared, not defaulted). A neighbour below the floor
   yields unknown with `:reason :embedding-below-floor :cosine c :floor f`. Record the floor and
   the cosine in every embedding-derived receipt.
4. **Receipts carry the chain.** Every derived receipt records `:derived-via` fully (kind, hops,
   neighbour, cosine, floor) so the projector's `pickRelation {"observed" <digest>}` digests the
   chain, not just the class.

## Tests (one live pin each, using the real inputs resource and real task files, read-only)

- The census from the finding, rerun with the same context, focus and as-of: report the new
  table (prefix × class × source) in the bell; the test pins at least that some E- and some T-
  targets now classify, with counts stated.
- An E- target with a stated relation in its text classifies through the hop; one without, and not
  embedded, stays unknown with its reason.
- A T- target inherits its parent's class and records the chain.
- Floor: with `:embedding-floor 0.1` in a test copy of the inputs, the 11-01 picks (cosines −0.014
  and 0.073) read unknown `:embedding-below-floor`; with 0.0 they read associated. Missing floor key
  → the producer refuses derivation by name.
- Existing `focus_receipt` tests and the 12 outer namespaces vs `main`; the test JVM's hermetic
  root guards data; report the count.

## Gates

kondo, parens; no reload (the serving JVM loads this namespace; merged ≠ loaded, and clicks are held);
no merge. Branch `codex5/l5-4g-relation-coverage` in `~/worktrees/futon2-l5-4g`. Bell claude-2 with
sha, the census table, test counts vs main, and anything that does not fit.
