# Real genesis draft review — codex-26

Reviewed 7ca8e617/10bcf636/77a04049. Four draft pins and the checker pin match. The checker has seven actual Boolean checks and an explicit nonzero failure exit; its scope is draft consistency, not authority authentication. No passing checker rerun. The root resolution uses the independently retained lead origin review e72b3e39, with the explicit trusted operator-session/host-retention premise already stated there.

A fresh read-only independent ledger observation confirms job20588, its exact trace ID and trace->job index join, artifact7b591a4e, and request digest; request-commission is absent. The observation only reads the ledger file with clojure.edn and invokes no HTTP initialization/compaction API. This corroborates current absence; it does not recreate the earlier whole-ledger bytes referenced by the author snapshot hash. No historical preimage may be fabricated.

Thus accepted only as a pending evidence draft. It cannot become a positive genesis subject merely by adding reviewer acceptance. Before fresh author/reviewer commissions, the accepted retention implementation must be verified serving on the actual creation path. Prepare a concrete reviewed deployment procedure without executing production changes in the preparation packet. Bind repository-qualified canonical branches (futon3c master versus futon2 main) rather than an unexplained single main field. No anchor or node admission.

The draft checker is not the hardened authority reader: edn/read-string and equality against recorded verified status are only local consistency checks here. Future positive admission must consume the reviewed same-byte configured authority adapters and exact real commission API.
