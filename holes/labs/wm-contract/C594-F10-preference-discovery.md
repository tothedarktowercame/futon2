# C594 — F10 preference discovery

This is C591 slice 4 only. The extractor accepts a pinned EDN fragment and the
ruled seed, retains only records explicitly typed `:preference`, and refuses
out-of-support, non-numeric, provenance-free, and duplicate claims
(`src/futon2/aif/preference_discovery.clj:11-27`,
`src/futon2/aif/preference_discovery.clj:36-50`). It does not normalise or fill
the fragment. Each proposed component carries its citation and the original
claim, while only a numeric disagreement becomes a decision-sheet row
(`src/futon2/aif/preference_discovery.clj:51-64`). The returned sheet is data;
the namespace contains no write operation
(`src/futon2/aif/preference_discovery.clj:1-6`).

The t6 fixture pins the seed values to the recorded C candidate at
`holes/labs/wm-contract/runs/D1-evidence/kl-worked-example.edn:10-15` and also
contains a non-preference observation that must not become proposed C
(`test/fixtures/f10-rulings-fragment.edn:1-13`). The test plants one seeded
disagreement in memory. The agreeing component retains provenance, the planted
disagreement alone becomes a question, the observation is excluded, and the
ruled seed remains equal to its pre-extraction value
(`test/futon2/aif/preference_discovery_test.clj:9-28`). No registry choice or
decision is written.

Validation: `futon2.aif.preference-discovery-test` passed 1 test / 8 assertions
/ 0 failures / 0 errors. clj-kondo reported 0 errors / 0 warnings and
check-parens reported OK on both Clojure files. No live tick, run lock, or
publish regeneration was involved.
