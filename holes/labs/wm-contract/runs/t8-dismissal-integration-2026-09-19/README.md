# T8 dismissal integration — 2026-09-19

Implementation commit: `15680a13`.

The tripwire repair snapshot includes dismissals/, effective-statuses gives dismissal records precedence after findings/implementations/resolutions, and cross-run closed IDs include :dismissed-unexecuted. T8 signature, recency window and count threshold are unchanged.

Fixtures write only temporary findings and dismissal records. They exercise the actual snapshot → effective status → cross-run observation → T8 evaluation path. All-dismissed groups produce no witness; three live members plus a dismissed member still produce a three-member witness excluding the dismissed id; a single live member remains below the unchanged >2 threshold. This interprets the requested live-group control without treating one surviving finding as three.

Gates: clj-kondo clean; check-parens OK (adjacent outputs). The suite executed once through the durable validation CLI from futon3c, after committing source scope:

```
clojure -M -m futon3c.test-registry.validation register /home/joe/code/futon2/holes/labs/wm-contract/runs/t8-dismissal-integration-2026-09-19/register.edn
```

evidence-id test-registry-4fc6a7eb174f64a25ebdda8052a8587d03378aec1c63e71c447110f779432229
warrant? true
results {:assertions 12, :duration-ms 1492, :errors 0, :exit 0, :failures 0, :tests 3}
bound T8-dismissal-integration -> test-registry-4fc6a7eb174f64a25ebdda8052a8587d03378aec1c63e71c447110f779432229

No real finding/disposition was written, no click was performed, and no shared JVM was reloaded. The only service mutation is the requested test-registry warrant and subject binding. Runner log and closure receipt are retained in execution/; report.stdout is a fresh-JVM validation report.
