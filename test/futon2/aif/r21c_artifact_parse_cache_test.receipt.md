# R21c verified-artifact parse-cache execution receipt

- Source commit tested: `1953884bec94ff59d560f1124fe39da77fce974a`
- Worktree: `/home/joe/worktrees/futon2/r21c-artifact-parse-cache`
- Bounded job: `bounded-1793011496737-r21c-artifact-parse-cache-final`
- Command: `clojure -X:test :nses '[futon2.aif.meta-medic-authority-test futon2.aif.meta-injury-ledger-test]'`
- Output: `/tmp/futon-bounded-tests/bounded-1793011496737-r21c-artifact-parse-cache-final.log`
- Machine receipt: `/tmp/futon-bounded-tests/bounded-1793011496737-r21c-artifact-parse-cache-final.receipt.json`
- Repository basis: clean and stable at start and finish at the source commit.
- Resource result: clean; peak 86 pids, no native-thread markers, and
  `pids.events:max` delta 0.
- Test result: **passed**, 32 tests, 288 assertions, 0 failures, 0 errors.
- Focused cache test: 1 test, 8 assertions, 0 failures, 0 errors. It observes
  one parse across two resolutions of verified identical bytes; changed bytes
  still refuse with `:artifact/hash-drift`; and malformed bytes are parsed and
  refused on both attempts rather than entering the cache.
- Static gates: clj-kondo 0 errors / 0 warnings; check-parens `OK` on both
  changed Clojure files.
- Real-ledger isolated-JVM timing: validation of the same 45-entry live ledger
  and seven-scope authority took 23,832.9 ms and 322 parses on canonical
  pre-change `main`; the worktree took 5,013.1 ms and 46 parses. Both returned
  `:valid`. The timing used
  `/home/joe/notes/wm-next/outer-selection/r21c-artifact-parse-timing.clj` and
  did not load or call the serving JVM.

The timing uses the live ledger and authority artifacts read-only. No click was
started, and no namespace was loaded or reloaded in the serving JVM.
