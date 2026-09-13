# Retained gate attempts

Before the final explicit commands, three command-shape failures were retained
honestly:

- `emacs --batch -Q -l scripts/check-parens.el ...` exited 255 because that
  path does not exist in futon2. The final gate uses the actual futon4 checker
  and invokes `arxana-check-parens-cli` with both explicit paths.
- `clojure -M -e ...test-vars...` exited nonzero at namespace loading because
  the test path was absent. This was not counted as runner sensitivity.
- Two attempted nonexistent-namespace forms ran zero tests (or failed during
  loading) and were rejected as sensitivity evidence. The retained final
  induced control uses `/tmp/row22_head_induced_test.clj`, runs one assertion,
  and exits 1 from its deliberate failure.

None of these attempts changed repository source or any store.
