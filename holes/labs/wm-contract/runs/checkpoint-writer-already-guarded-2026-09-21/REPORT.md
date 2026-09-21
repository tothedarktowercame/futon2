# Premise mismatch: checkpoint writer already has the guard

Request: claude-12 invoke-1789963692351-22872-29ff1449.
Author: codex-5. No implementation or test source was changed.

The requested guard already exists in canonical full_loop_cohort.clj, and was
introduced at this exact write site by 10d3dc90:

- printed-edn (line 223) calls pr-str, then clojure.edn/read-string on that string.
- writable-edn (line 243) returns the original string when readable, otherwise
  substitutes typed :non-edn-payload placeholders while preserving readable siblings.
- write-new! (line 266) writes (:text (writable-edn value)) plus newline.
- append-checkpoint! (line 613) calls that same write-new! for checkpoint events.

This is a shared writer guard, not a dispatch-only guard. The requested
extension would duplicate behavior that is already on this checkpoint path.

## Exact retained poison through production writers

Retained source: data/wm-quarantine/machinery-67-attempt-001/002-selection.edn.
SHA-256: 9085b8ce5048b4c9a6d66e7d15f7e9c8d291cb91736ce546f7e881df76b80a2d.
A direct production EDN read refuses `Invalid token: :hole/2f9b03b16170`.

probe.clj reconstructs the VALUE by temporarily giving digit-leading hole
keywords readable names, reading the whole document, and restoring the original
keyword values via clojure.walk/postwalk. It does not change either writer or
reader. Printing/re-reading that value still throws Invalid token; hash-map
iteration makes the first reported invalid token :hole/6adae34e06ad.

The exact reconstructed event was written through private write-new!, and its
payload through public append-checkpoint! after a real activation and time-step
in a temporary cohort. Both persisted records read back through cohort/read-edn
and contain 1208 typed :non-edn-payload placeholders. No port was stubbed.
probe-results.edn transcribes the executed probe's three output forms.
Command: `clojure -M:test /tmp/codex5-checkpoint-probe.clj`; the exact script is
retained as probe.clj. Only temporary cohort data was written; quarantine is intact.

## Controls

Fresh-JVM command: `clojure -M:test -m cognitect.test-runner -n
futon2.aif.full-loop-cohort-test`. Result: 30 tests / 170 assertions / 0 failures /
0 errors, exit 0; log retained. This includes existing byte-identical valid-write
controls, public append/run-continuity controls, and 707e680f's retained old-poison
exclusion test. clj-kondo on probe.clj: 0 errors / 0 warnings; check-parens: OK.

Stopped on the contradicted implementation premise; no duplicate guard added.
The full-loop-runner suite was not rerun for this evidence-only report.
This fresh tooling JVM proof does NOT establish what writer code was loaded in
the serving JVM when click 5 wrote its checkpoint. A stale loaded writer is a
possible explanation, not an established finding. No serving reload occurred.
