# STOP: blanket history exclusion loses scheduler identity

Request: invoke-1789962162138-22854-31a64771, claude-12.
Author: codex-8. No implementation landed. No live loads, ticks, or repair-store writes.

The exact bypass is full_loop_cohort.clj:313 attempt-events -> read-edn:40.
The producer's quarantine note already establishes the chain through
beyond-window-attempt?, ledger, resolve-lineage!, and the upstream apply-binding.
receipt_construction.clj:71 history-read! is a different guarded reader;
its history-roots enumeration is not this initialization call chain.

A draft separated strict checkpoint mutation from tolerant historical scans,
recording target attempt, path, typed reason, and string-valued reader error.
However, historical scans also serve opened-opportunity-ids (:355), consumed
by start-attempt! (:490). Skipping an unreadable time-step loses the identity
that prevents duplicate scheduler opportunities. A fresh tooling JVM reproduced:

- Readable original time-step: same opportunity refused, "duplicate scheduler opportunity".
- After corrupting ONLY the scratch time-step: same opportunity admitted as
  attempt-002, despite the typed exclusion being recorded.

See boundary.log and boundary-reproduction.clj.txt. This is a rejected draft,
not a production-success claim. rejected-draft.patch.txt retains the exact
proposal that was tested. All three edited source/test files were restored to
HEAD after checking that their differences were solely this draft.
The real quarantine was not modified. The poison selection-file acceptance,
full runner suite, and warrant registration have not been completed; they cannot
warrant this rejected implementation. kondo.log records an intermediate test
parenthesis error; it was corrected before the boundary reproduction, and is not
claimed as a passing gate.

Required ruling/structural boundary: tolerate and record unreadable non-identity
historical checkpoints, but keep authoritative time-step identity unreadability
as a typed admission refusal (preserving uniqueness and window accounting).
Alternatively, independent immutable identity evidence must be available and
validated before admitting a new opportunity when a time-step cannot be read.
The blanket requirement "unreadable history ... skipped — never fatal" cannot
apply to lost admission authority without weakening an existing invariant.

The receipt-construction history guard must also remain fail-closed when damage
prevents establishing previous construction; skipped unknown evidence is not
proof that no previous construction exists. No change was made to that reader.

No store disposition: independent Agency-job review is still required after a
valid implementation. COMMITTED IS NOT LOADED; this packet contains only stopped
work evidence, not serving code.
