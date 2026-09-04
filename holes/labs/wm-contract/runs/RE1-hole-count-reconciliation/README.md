# RE1 — the two open-hole feeds reconciled, 2026-09-04

Worklist item `:RE1` (`EPIC-run-era.md`). The row observed that at the
2026-09-04 01:03 publish `variable-situation-accounting.edn` carried **11**
`:content-status :open-hole` rows while the model-coverage table reported
**10 open** — cited in `:U45`'s review and `:U49`'s evidence — and asked
whether the two feeds count different populations or one feed's logic is
stale.

**Answer: different populations, stated by name below.** No generator logic is
stale, no count is wrong, and nothing was changed by hand. The set difference
is exactly one row, `Strategic mission selection`, which is a glossary
paragraph and not a contract declaration; the coverage table's open column
counts contract declarations only.

## The shas this enumeration was taken at

| repo | identity |
|---|---|
| futon2 | commit `f57d172fa2f9cf8872a065bc917618c28c9c89bf`, tree `965ead6742fd5073a5675120210ab8bd70aea8e8` (`variable-situation-accounting.edn` unmodified in the working tree at that commit) |
| p4ng | commit `4f2bd910ff2399c252f1a60e6f04fa706f45c099` |
| mathlib4 | commit `49e8528116a8ed8cebb59a29514c79d9f4f938e7`; `holes-contract.json` `source.git-sha` `11c2e44affa167bf85b0a2d7c29d8705f89a8d08` |

The accounting's `:authority :contract-git-sha` is `11c2e44aff…`, equal to the
contract's own `source.git-sha`, so there is **no registry lag** in this
comparison: `gen_model_coverage.py:169` would have rendered a visible
`registry lags contract` clause and did not.

## (1) The two populations, enumerated by name

### Feed A — `variable-situation-accounting.edn`, 11 `:open-hole` rows

Reproduce with:

    bb -e '(require (quote [clojure.edn :as edn]))
           (doseq [x (sort-by :name (filter #(= :open-hole (:content-status %))
                                            (:rows (edn/read-string (slurp "variable-situation-accounting.edn")))))]
             (println (:name x) (:row-source x) (:area x) (:closability x)))'

| # | name | `:row-source` | `:area` | `:closability` | row at |
|---|---|---|---|---|---|
| 1 | `C` | `:contract-declaration` | `:preferences` | `:pre-run-closable` | `variable-situation-accounting.edn:733` |
| 2 | `find` | `:contract-declaration` | `:demo` | `:pre-run-closable` | `variable-situation-accounting.edn:751` |
| 3 | `organise` | `:contract-declaration` | `:demo` | `:pre-run-closable` | `variable-situation-accounting.edn:790` |
| 4 | `preferenceStackLiveRecorded` | `:contract-declaration` | `:preferences` | `:run-gated` | `variable-situation-accounting.edn:878` |
| 5 | `wmRunsOnce` | `:contract-declaration` | `:run` | `:run-gated` | `variable-situation-accounting.edn:892` |
| 6 | `wmRunConformsToWiring` | `:contract-declaration` | `:run` | `:run-gated` | `variable-situation-accounting.edn:907` |
| 7 | `enactedEqualsSelectedWhenRankOneGated` | `:contract-declaration` | `:run` | `:run-gated` | `variable-situation-accounting.edn:929` |
| 8 | `dirichletAccumulationImportAbsent` | `:contract-declaration` | `:learning` | `:pre-run-closable` | `variable-situation-accounting.edn:942` |
| 9 | `policyPrecisionIsGammaFromBeta` | `:contract-declaration` | `:policy` | `:run-gated` | `variable-situation-accounting.edn:953` |
| 10 | `policyPosteriorImportsPolicyF` | `:contract-declaration` | `:policy` | `:run-gated` | `variable-situation-accounting.edn:967` |
| 11 | **`Strategic mission selection`** | **`:glossary-paragraph`** | `:policy` | `:run-gated` | `variable-situation-accounting.edn:1075` |

### Feed B — the model-coverage table, 10 open

The names are printed in the generated table itself, one per area row, and are
readable without re-running anything: `p4ng/sec-model-coverage-generated.tex:5`
(`C`, `preferenceStackLiveRecorded`), `:6` (`policyPrecisionIsGammaFromBeta`,
`policyPosteriorImportsPolicyF`), `:7` (`dirichletAccumulationImportAbsent`),
`:8` (`find`, `organise`), `:10` (`wmRunsOnce`, `wmRunConformsToWiring`,
`enactedEqualsSelectedWhenRankOneGated`). That is rows 1–10 of the table above.

The same ten are exactly the `"kind": "hole"` declarations of
`mathlib4/DarkTower/WarMachine/holes-contract.json:1` (the file is minified to
one line, so `:1` is the only pointer it can carry):

    python3 -c "import json;d=json.load(open('holes-contract.json'));print(sorted(x['name'] for x in d['declarations'] if x.get('kind')=='hole'))"

## (2) The set difference and its reason

**Difference: `{Strategic mission selection}`. Type: DIFFERENT POPULATIONS —
not stale logic in either feed.**

* Feed A counts every row of the accounting, all 133 of them, across both
  `:row-source` values (124 `:contract-declaration` + 9 `:glossary-paragraph`).
* Feed B's closed/open **columns** aggregate `decls` only —
  `gen_model_coverage.py:130` filters `row-source == "contract-declaration"`
  before any counting, and `open_total` at `:162` sums over that filtered set.
  Glossary paragraphs are held apart at `:131` and reported in the Total row's
  stamp instead (`:136`, `:238`, `:245`).
* The population split is a decision already on record — U14, 2026-09-02 —
  and its reason is written at `gen_model_coverage.py:123-129`: pooling the two
  would double count, because a glossary paragraph is the *owner* of the
  declaration that already appears in a column.
* `Strategic mission selection` has no contract declaration. Its own row says
  so in as many words (`variable-situation-accounting.edn:1075`, `:witness-note`:
  "This is a GLOSSARY-side hole, not a contract declaration: it does not appear
  in holes-contract.json and is therefore reported outside the declaration
  columns until a declaration is minted for it"), and the contract enumeration
  above confirms the absence.

### A correction to the row's premise

`:RE1` says "neither artifact says so". One of them does. The **rendered
coverage table already names the eleventh hole**, in the Total row's stamp
(`p4ng/sec-model-coverage-generated.tex:12`):

> 1 promoted to an open hole (`Strategic mission selection`$^{r}$), counted
> outside the columns above

The accounting artifact also carries the split numerically —
`:counts :row-source`, `:counts :glossary-content` (`:open-hole 1`) and
`:counts :declaration-closability` (which totals 10, against `:closability`
which totals 11). So a reader with either artifact open can derive the
difference. What was missing is a plain statement of *which population each
generator counts* at the top of each generator, where someone comparing two
numbers from two reviews would look. That is what §4 adds.

### What is NOT claimed here

This note types the discrepancy; it does not rule on whether the two
populations *should* be counted apart. That question — like `:U49`'s C5 finding
about 22-vs-21 drawn edges — would want a `:decisions` entry and is Joe's.
Nothing is written to `aif-equations.edn :choices` or
`control-map-edges.edn :decisions` by this row.

## (3) Both feeds re-run green at these shas

Bare exit codes, run at futon2 `f57d172f` / p4ng `4f2bd910`:

| command | stdout | exit |
|---|---|---|
| `bb scripts/generate_variable_situation_accounting.bb --check` (futon2 root) | `variable-situation-accounting: PASS {:rows 133, :content {… :open-hole 11 …}, :row-source {:contract-declaration 124, :glossary-paragraph 9}, :glossary-content {… :open-hole 1}, :closability {:pre-run-closable 4, :run-gated 7}, :declaration-closability {:pre-run-closable 4, :run-gated 6}}` | `0` |
| `GMC_OUT=/tmp/re1-gmc.tex python3 gen_model_coverage.py` (p4ng/empirics-futon) | `gen_model_coverage: 133 variables (124 declarations, 9 glossary paragraphs) -> 114 closed, 10 open, 0 unclassified; fence 4 pre-run-closable / 6 run-gated; glossary {'closed-by-record-with-witness': 7, 'framing': 1, 'open-hole': 1}; pin 11c2e44aff` | `0` |
| `cmp /tmp/re1-gmc.tex ../sec-model-coverage-generated.tex` | *(silent)* | `0` |

The coverage generator was run with `GMC_OUT` pointing at a temp file rather
than into the publish (loop rule 4 / TN §9a: regeneration into the paper
happens after review). The `cmp` line is what shows the committed artifact is
the one this run reproduces, byte for byte.

Both stamps agree on every shared number: 133 rows, 124 declarations, 9
glossary paragraphs, 4 pre-run-closable / 6 run-gated on the declaration side,
1 glossary-side open hole. **11 = 10 + 1**, with no number appearing in one feed
that the other contradicts.

## (4) What changed

No count, no row, no artifact. Two comment-only edits, one per generator
header, each stating which population that feed counts and why the other feed's
number may differ:

* `futon2/scripts/generate_variable_situation_accounting.bb:2-11` (this commit)
* `p4ng/empirics-futon/gen_model_coverage.py:11-18` (p4ng `bee603f`)

Both files' outputs are unchanged by these edits, shown by re-running the two
commands in §3 after them.
