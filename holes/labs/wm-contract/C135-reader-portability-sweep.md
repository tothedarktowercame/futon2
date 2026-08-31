# C135 — reader portability sweep

Date: 2026-08-31

## Correction to the incident model

The C129 discrepancy is not between two EDN readers. On the witness token
`:r8/a/v1`:

| Reader | JVM | Babashka |
|---|---:|---:|
| language/source `read-string` | accepts | rejects |
| `clojure.edn/read-string` | accepts | accepts |

Babashka's source reader is edamame; its EDN API is a separate path. Thus the
portable spelling chosen by C129 was prudent, but a persisted value containing
the original spelling would parse under both `clojure.edn` implementations.
The exposed seam is a `.bb` program using bare `read-string` to consume data.

## Method and results

The sweep compared JVM and Babashka source and EDN readers over multiple-slash
keywords/symbols, auto-resolved keywords, ratios, bigint/BigDecimal literals,
`##NaN`, regex literals, reader conditionals, metadata, namespaced maps, and
`#inst`. The two `clojure.edn` implementations agreed on every case. Expected
EDN exclusions (`::auto`, regexes, and reader conditionals) were rejected by
both EDN APIs. Source-reader differences were:

- multiple-slash keywords and symbols: JVM accepts, Babashka source rejects;
- reader conditionals: Babashka source selects its branch while the tested JVM
  `read-string` call rejects without `:read-cond` options.

Both EDN implementations then parsed every `.edn` file reachable below the
repository, excluding `.git` and `target`: **1,160 files / 1,930 top-level
forms / 0 parse errors**.

A structural walk found one persisted multiple-slash token:
`holes/labs/M-legacy-sorry-cleanup/legacy-sorries-snapshot.edn` contains
`:code/v05/sorry`. No current reader references that snapshot, so it is a
latent compatibility finding, not a live break, and this pass does not migrate
it.

## Cross-reader ports

| Artefact family | Writer / reader | Exposure |
|---|---|---|
| WM trace records | JVM `trace/write-trace!`; JVM trace readers and Babashka R2/R8/census checks use `clojure.edn` | Cross-runtime, currently portable. This is the highest-value port. |
| Preference-stack and other serialized witnesses | JVM production invocation or committed EDN; JVM binding checks and Babashka shape checks use `clojure.edn` | Cross-runtime, currently portable. |
| Fold escrow / repair obligations / run records | JVM writers and readers; several Babashka checks use `clojure.edn` | Cross-runtime where checked, currently portable. |
| Edge and witness fragments, merged registry, dispositions | Agent-authored EDN; Babashka mergers/checkers | Primarily Babashka-only. Not a JVM→bb port, but exposed to bare source reads. |
| Contract JSON | JSON producers/consumers | Not EDN; outside this reader class. |

The bare-file-reader census found these active Babashka boundaries using
source `read-string`: `merge_edges.bb`, `merge_witnesses.bb`, `edge_census.bb`,
`work_units.bb`, and `holder_check.clj`. Their current inputs pass, but a
JVM-accepted/source-reader-rejected token could break them. Replacing those
calls with `clojure.edn/read-string` is the bounded preventive repair; it is
not performed in this census.

## Proposed check and cost

A useful portability gate has two layers:

1. lint persisted-file consumers and reject bare `read-string` at those
   boundaries; require `clojure.edn/read` or `clojure.edn/read-string`;
2. have the JVM serialize a small adversarial value corpus, have Babashka read
   it through `clojure.edn`, and compare values. Separately parse the named
   cross-runtime artefacts under both EDN implementations.

The complete repository scan took roughly 29 seconds per runtime here, so a
two-runtime all-file gate costs about one minute and belongs in CI/nightly,
not every quick check. A scoped syntax corpus plus the named cross-runtime
ports costs one JVM startup and a Babashka invocation and is worth adding to
the normal workspace gate. The bare-reader lint is cheap and catches the
actual incident class more directly.

## Catalogue disposition

This is a sharpening of class 7, duplicate representation, not a tenth class.
The duplicated representation is the grammar/reader interpretation of one
serialized form. The falsifier is one byte string accepted or interpreted
differently at the two endpoints. The repair is one declared data reader at
every persistence port plus a cross-runtime round trip; it is not migration of
unreached historical data.

No runtime, artefact, reader, or check was changed in this pass.

## Reproduction

```sh
clojure -M -e '(println (read-string ":r8/a/v1"))'
bb -e '(read-string ":r8/a/v1")'          # rejects
clojure -M -e '(println (clojure.edn/read-string ":r8/a/v1"))'
bb -e '(println (clojure.edn/read-string ":r8/a/v1"))' # accepts

rg -n "clojure.edn|edn/read|read-string" src scripts checks test tools
bb -cp . checks/r8_f_contract.clj --report /tmp/c135-r8.edn
clojure -T:build ci
cd /home/joe/code/futon3 && clojure -X:test
```
