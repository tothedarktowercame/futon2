# E-futon1b-foothold — XTDB 2 foothold via the memory-seam swap

**Status: COMPLETE (2026-07-04, same day). Owner: zai-8 (build) /
claude-16 (dispatch, review, and finishing fixes). Parent: M-custom-harness
D-11.iii + §11 Axis-A2 spike; ratified by Joe 2026-07-04.**

**Findings (full record in `futon1b/NOTES.md`):** both excursion questions
answered YES — (1) XTQL expresses all three futon1a idioms, with
multivalued membership via pipeline `unnest` reading cleanly; (2) the
D-11.i seam swaps: `memory-search-1b` on XTDB 2 returns a shape-identical
§12.3 envelope, nothing above the read fn changes. **The finding that
prices the A2 trigger: two silent/late failure modes** — a wrong `unnest`
formulation returns unfiltered rows without erroring, and Clojure-isms
inside XTQL `where` fail only at runtime — so a real port needs runtime
correctness assertions at every one of the ~34 translated sites, not
compile checks. Sub-verdict on the "model-as-query-author" axis: XTQL
looks like Clojure but is not Clojure; familiarity is partly false economy.

A bounded excursion, not the port. Two questions, both answerable with a
throwaway process and synthetic data:

1. **Does XTQL read cleanly for the store's most Datalog-idiomatic
   pattern** — multivalued-endpoint membership (`[e :hx/endpoints ep]`
   unification, the idiom used at ~34 sites in futon1a)?
2. **Does the D-11.i seam actually swap** — can one memory tool's read fn
   (`memory-search`-equivalent) be re-implemented against XTDB 2 returning
   the identical §12.3 envelope, with zero changes above the seam?

The answers feed the D-3/A2 trigger in M-custom-harness §12.1: they are
*evidence for deciding whether* a full futon1b port mission opens — they do
not commit anyone to it.

## Hard constraints

- **I-0:** the XTDB 2 node is a *temporary dev process* (in-process node in
  a REPL or short-lived JVM under `/home/joe/code/futon1b`). It serves no
  request path, and it is killed when the excursion pauses. It must never
  become a second serving JVM.
- **Never touch the live `:7071` futon1a store or the futon3c serving JVM.**
  All data in this excursion is synthetic.
- No commits; work stays in the working tree for review.
- Gates on any Clojure written: clj-kondo (0 errors) and
  `futon4/dev/check-parens.el`.

## Steps

- **S1 — scaffold.** `/home/joe/code/futon1b/` with a minimal `deps.edn`.
  Determine current XTDB 2.x coordinates from docs.xtdb.com /
  search.maven.org (curl); expect something like `com.xtdb/xtdb-core` 2.x.
  Start an in-process node and round-trip one document.
- **S2 — synthetic seed.** Read futon1a's model source
  (`/home/joe/code/futon1a/src/…` — `compat/futon1_graph.clj` and `model/`
  namespaces document the `hx/` schema) and generate ~100 synthetic
  hyperedges conforming to it (mixed `:hx/type`s, shared endpoints so
  membership queries have real work to do), plus ~30 synthetic evidence
  entries shaped like futon3c `EvidenceEntry`. Ingest into the node.
- **S3 — the query spike.** Translate to XTQL (and, separately, to XTDB 2
  SQL if cheap): (a) endpoint-membership — all hyperedges having endpoint X;
  (b) by-type with limit; (c) evidence by single tag + limit. Record each
  side-by-side with the 1.x Datalog original and a **reads-cleanly verdict
  with evidence** (correct results on the seed, and a sentence on ergonomics).
- **S4 — the seam swap** (second dispatch, after S1–S3 review). Re-implement
  `memory-search`'s read fn against the futon1b node, returning the exact
  §12.3 envelope (`:frame` / `:query` / `:items` with `:id :at :author`),
  limits clamped. Prove shape-identity against the futon3c implementation's
  output on equivalent data.

## Acceptance bar

S1–S3: node starts; seed ingests; all three queries return verifiably
correct results on the synthetic seed; side-by-side translations recorded in
`/home/joe/code/futon1b/NOTES.md` with the verdict. S4: envelope
shape-identical; no change needed above the seam.

## Kill criteria (report and stop — a clean kill is a successful excursion)

- K1: XTDB 2 dependency cannot be resolved/started within two dispatch
  rounds of effort.
- K2: the endpoint-membership idiom has no faithful XTQL translation
  (semantics, not syntax — if multivalued unification can't be expressed,
  that is the finding).
- K3: resource constraints (the node needs more RAM/disk than a dev
  process should take on this box).

## Checkpoint — S1 complete (2026-07-04)

Two dispatches. zai-8's first run resolved the deps (`com.xtdb/xtdb-core` +
`xtdb-api` 2.0.0, Maven Central) and mapped the `xtdb.api` surface, but
exhausted its 24 tool rounds on API archaeology — each one-shot
`clojure -M -e` probe boots a fresh JVM, so ~15 probes consumed the budget
(a second round-exhaustion datum for M-custom-harness §13.5b, new flavor:
unfamiliar-API discovery under a per-probe JVM-boot tax). claude-16 closed
the two gaps in two probes and verified live:

- entry point is **`xtdb.node/start-node`** (zai-8 was probing `xtdb.api`,
  which only has client/q/execute-tx);
- XTDB 2 is Arrow-based → JVM needs
  `--add-opens=java.base/java.nio=org.apache.arrow.memory.core,ALL-UNNAMED`
  — now a `:node` alias in `futon1b/deps.edn`.

Verified round trip: `put-docs` + XTQL `(from :probe [xt/id hx/type
hx/endpoints])` returns the doc, multivalued endpoints intact. S2–S3
re-dispatched to zai-8 with the incantations inlined and a process
correction (one iterated script file instead of one-shot probes; assert
expected counts against the known seed).

## Checkpoint — S2/S3 complete, all three translations PASS (2026-07-04)

Full record in `/home/joe/code/futon1b/NOTES.md` (side-by-sides + assertion
output; seed-as-oracle in `s2_s3.clj`). Headline: XTQL expresses all three
idioms, and the multivalued-membership translation is
`(-> (from …) (unnest {:ep hx/endpoints}) (where (= ep X)))` — legible,
arguably more explicit than the Datalog it replaces. **The finding with
teeth (zai-8, dispatch 3): a naive wrong `unnest` formulation returns all
rows unfiltered instead of erroring** — silent wrong answers, not crashes,
are the port's failure mode, so all ~34 membership sites need
known-data correctness assertions, not compile checks. This materially
prices the A2 trigger. Division of labor note: three zai dispatches each
hit the 24-round wall (spec-reading, API archaeology ×2 — each
`clojure -e` probe pays a JVM boot); S1-unblocks, the working idiom, and
NOTES.md were finished by claude-16 under the fix-don't-re-bell rule.
The JVM crash mid-excursion cost zai-8's session memory and one re-dispatch
(job marked worker-lost-on-restart); disk artifacts survived.

Remaining: **S4 only** (seam swap).

## Protocol

Each dispatch: gates before reporting; explicit bell back to claude-16 with
summary (`agency_send.py --from zai-8 --to claude-16 --kind bell`); final
text carries the same report for the ledger.
