# Source protocol review and lead touch-up — 2026-09-13

Reviewed 6d016f4a..82bcad95. ThreadLocal ownership keyed by normalized lock
path closes conveyed-binding bypass; separate nested identities acquire separately.
Actual writer API fixtures cover trip/finding/implementation/resolution, partial
logical publication refuses, cross-process and future contention now have controls.
Production entrypoint is an unconditional writer-participation-unverified refusal:
this correctly prevents source availability from being claimed as deployment.

Lead fixed two small remaining defects: normalize store roots before choosing
canonical lock identity (./ or .. aliases previously selected another lock), and
reject symlink .publication.lock entries in reader enumeration. Added targeted
regressions; final namespace run passes 10 tests/32 assertions. Kondo zero warnings/
errors; explicit arxana-check-parens-cli returns OK. Commands below; exact captured
outputs/exit statuses in lead-touchup-gates.json, final source pins alongside.

Commands (cwd futon2):
- clojure -X:test :nses '[futon2.aif.interoceptive-manifest-test]'
- clj-kondo --lint src/futon2/aif/interoceptive_store_lock.clj src/futon2/aif/interoceptive_manifest.clj test/futon2/aif/interoceptive_manifest_test.clj
- emacs -Q --batch -l /home/joe/code/futon4/dev/check-parens.el --eval '(arxana-check-parens-cli)' -- --no-defaults src/futon2/aif/interoceptive_store_lock.clj src/futon2/aif/interoceptive_manifest.clj test/futon2/aif/interoceptive_manifest_test.clj

Accept the source protocol and explicit production refusal at this scope. No
frozen witness or witness wrapper changed in the lead touch-up. Production
activation still needs secure stable lock provisioning, complete actual writer
participation evidence, and an implemented verified activation path (current
production entrypoint always refuses). Old writer-census prose/pins describe the
pre-repair version and do not prove deployment; searched source roots are bounded.
Cross-store logical history refusal remains unmodified. No live reload, production
store mutation, global confidence or node admission occurred. Independent review
of the lead touch-up remains owed before activation.
