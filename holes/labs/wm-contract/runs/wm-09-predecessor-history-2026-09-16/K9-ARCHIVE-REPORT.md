# K9 bounded archive structural diagnostic

Executed exact implementation ae721644 in an isolated JVM. Scope: the 24 archive closes pinned by the coordinator plus only the three named nonarchive counterparts compared in K9 (wm-outer-loop-40-v1 attempts 001, 002, 024). This is not a new all-root census; nonarchive means storage location, not a claim that a target is currently live.

All 27 closes were discovered. The structural identity check refused before target relevance/admission. Diagnostic checkpoint comparisons retain three duplicated cohort/attempt identities: attempt-001 has identical seven-checkpoint sets at distinct paths; attempts 002 and 024 have differing sets. Neither path nor README supplies equivalence or independent occurrence authority.

K9-ARCHIVE-STRUCTURE.edn retains every discovered path, identity and checkpoint digest, the exact source refusal and all three pair comparisons. Requested target is nil: target-relative evaluation was deliberately not run after this target-independent structural failure. No archived record is claimed admitted or target-excluded by this run. The earlier K8 archive-only tally remains historical evidence at its old scope and source; it cannot establish absence of archive/nonarchive collisions under K9.

Copy discipline: k9-archive-structure.py verifies the 24 pinned close digests, freshly pins the three exact named counterparts, copies only those 27 attempt directories without links, verifies every copied file, and checks original bytes again after the diagnostic. K9-ARCHIVE-COPY.json retains per-file original/copy/digest mappings. Production bytes remained unchanged; temporary copies were removed. Zero manifest rebindings occurred: this is structural comparison, not admission or a rewritten-identity experiment.

Diagnostic exit 0 means the expected structural refusal and three pair comparisons were reproduced; it does not mean discovery is operationally ready. Script lint and parentheses pass, retained in K9-ARCHIVE-GATES.json. Source tests remain 25/301, callers 2/16 and other gates in K9-GATES.json.

B2's annotated close remains an additional all-root blocker outside this bounded sample. These observations supply AUTH-history-disposition evidence, not recovery cost, discharge authority, production changes or acceptance. Independent review of the exact successor remains owed.
