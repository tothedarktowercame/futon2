# R8-D2 findings — F census and era boundary

Date: 2026-08-30. Corpus filter:
`data/wm-trace/wm-trace-*.edn`; dotfiles and `wm-shadow-step.json` excluded.

## Instrument

- **Observed.** `checks/r8_f_contract.clj:17` filters regular files by the
  commissioned name, and `checks/r8_f_contract.clj:24` repeatedly calls
  `clojure.edn/read` with an EOF sentinel and tagged-literal default reader.
  Command: `bb checks/r8_f_contract.clj --report
  holes/labs/wm-contract/R8-D2-report.edn`. Result: pass, 53 files, 792 forms
  (`holes/labs/wm-contract/R8-D2-report.edn:1`).
- **Observed.** The classifier reads exactly `:prediction-errors`,
  `:precision-state`, and `:variational-free-energy`, in the same three arms
  as `r8Disposition` (`checks/r8_f_contract.clj:51`). The fixture explicitly
  exercises prediction errors absent while precision is present
  (`test/r8_f_contract_test.clj:57`).

## Three labelled quantities

1. **Can fail — missing-F recomputation.** The disposition census is 755
   missing-F-computable, 32 stored-F, and 5 insufficient-inputs. It sums to
   792. The 755 previously unread values are finite: non-finite count 0, min
   1.8470582213146736, q25 3.4789225075453594, median 5.9633632851740455,
   q75 8.422339997688276, max 10.637526080614668
   (`holes/labs/wm-contract/R8-D2-report.edn:1`). The 1.847 floor is an
   operational alarm across the missing-F-computable population, not evidence
   that F is a generative-model variational free energy.
2. **Cannot fail here — tautological consistency check.** The 32 stored-F
   forms recompute with epsilon/max absolute delta 0.0. Their distribution is
   min 0.1903302937544315, q25 0.2311622432567551, median
   0.2593710081820776, q75 0.3016215721233697, max 0.5223336238034448
   (`holes/labs/wm-contract/R8-D2-report.edn:1`). This confirms serialized
   arithmetic only; it is not evidence.
3. **One contingent fact and one write site.** The era partition is 760 forms
   without stored F and 32 with it. At boundary 20260714, stored-before-boundary
   violations = 0 and suffix-without-stored violations = 0: the finding is
   non-interleaving (`holes/labs/wm-contract/R8-D2-report.edn:1`). The other
   four directed mismatch counts are also 0, but stored F, selection gain, and
   free-energy shape are unconditional keys of one map literal
   (`scripts/futon2/report/war_machine.clj:4664`,
   `scripts/futon2/report/war_machine.clj:4665`,
   `scripts/futon2/report/war_machine.clj:4687`); those checks confirm one
   write site rather than three independent facts.

The two partitions are not conflated: 760 is the no-stored-F era population;
755 is its computable subset, with the other 5 typed insufficient-inputs
(`checks/r8_f_contract.clj:150`).

## Era measurements

- **Observed.** Among usable no-stored-F forms, per-channel precision mean is
  94.58450289071568, mean absolute error is 0.3123533534077551, and mean
  channels/form is 7.287417218543046. For the 32 stored-F forms the respective
  values are 9.49054874145808, 0.27213535266920513, and 8.0
  (`holes/labs/wm-contract/R8-D2-report.edn:1`). Precision is the proximate
  arithmetic factor in `F = ½·mean(Π ε²)`; cause of the dated change is
  **inferred, untested** (`checks/r8_f_contract.clj:230`).

## Content pin and reproducibility

- **Observed.** The report pins 53 files / 792 forms and digest
  `c9add16ac96c973ba4fd9a0c61f3b7319780c304424e2d14ea7b477309947880`.
  The declared algorithm hashes `pr-str` of each parsed form, sorts the 792
  SHA-256 values, joins them with newlines, then SHA-256 hashes that sequence
  (`checks/r8_f_contract.clj:111`, `checks/r8_f_contract.clj:116`). Two runs
  produced byte-identical reports with file SHA-256
  `067dc46e8c0174c045c360b9bf880026db198b11d82fb30b4601af6e32b393db`.
- **Observed; refusal.** The commissioned prefix `c434950f2e6a7e9b` did not
  reproduce under that explicit parsed-form algorithm, although all registered
  counts reproduce. The packet gives no form-serialization or delimiter rule,
  so I do not relabel this as corpus drift and do not substitute its digest.
  The corpus has no Git pin because `data/*` is ignored (`.gitignore:46`).

## Hole movement and interface refusal

- **Observed.** `r8CensusWmTrace` receives fixture evidence: the report names
  all tick ids in each disposition and computes 755/32/5 using the Lean keys
  (`holes/labs/wm-contract/R8-D2-report.edn:1`).
- **Observed.** `r8EraBoundary` receives fixture evidence: the report names
  boundary 20260714 and all six directed violation lists, each empty
  (`holes/labs/wm-contract/R8-D2-report.edn:1`).
- **Refusal.** Neither Lean hole can honestly receive a body from this fixture.
  `r8CensusWmTrace` claims every arbitrary list of length 792 has the fixed
  census; a list of 792 identical insufficient-input ticks is a counterexample.
  `r8EraBoundary` claims its equivalences for every arbitrary corpus and every
  boundary; even the actual corpus with boundary 0 violates the date
  biconditional. The declarations have no premise or data definition tying
  their variables to this report. This packet supplies the permitted stated
  fixture evidence, but it does not close those universal Lean propositions
  (`/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:322`,
  `/home/joe/code/mathlib4/DarkTower/WarMachine/Holes.lean:328`).
