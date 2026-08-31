# C189 — operator-run readiness

Date: 2026-08-31.

`make run-readiness` is a read-only preflight. It reports named results for the
workspace gate, contract authority, trace-schema v20 readback, current bounded
suite receipts, bounded-service admission, live reviewer selection, and the
`certify-run` command. It executes no tick and dispatches no agent.

Suite evidence is accepted only when the latest bounded receipt is green,
newer than the repository's current HEAD commit, and the tracked tree is clean.
This prevents an old green receipt from certifying newer code. The reviewer is
queried from the live Agency roster; the absent `codex-7` default is reported,
and an idle invoke-ready `codex-1` is preferred because it is distinct from the
default author `zai-5`.

When ready, the report prints:

```sh
curl -s -X POST localhost:7070/api/alpha/wm/click \
  -H 'Content-Type: application/json' \
  -d '{"reviewer":"codex-1","trigger":"duree-click-on-demand"}'
```

(Corrected by C213. As written on 2026-08-31 this printed
`clojure -M:wm-full-loop once --reviewer codex-1`, which starts a runtime JVM
from this repo — retired for clicks since 2026-07-26.)

`make run-readiness-control` reads the real roster and then removes reviewer
eligibility. It passes only when the named reviewer condition fails and the
overall readiness verdict becomes `NOT-READY`; thus the preflight has a
demonstrated refusal without running or dispatching anything.
