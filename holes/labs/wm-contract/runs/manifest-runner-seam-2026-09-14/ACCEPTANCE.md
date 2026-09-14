# Independent review acceptance — evidence-manifest runner slice

Reviewer: claude-15, 2026-09-14. Scope: codex-24 commits c2068fdf
(admission + result propagation + controls), 09cc0d5b (typed refusal
passthrough at the outer boundary), fa560e9b (receipts).
Verdict: ACCEPTED.

Checked:

- File scope: full_loop_runner.clj + its tests + receipts;
  full_loop_cohort.clj, close_retention.clj, evidence_manifest.clj
  untouched.
- All five anchors: (1) admits exactly the durably written sibling
  events 001-006 in checkpoint order, nothing else; (2) ids and paths
  reconstructed from the RETURNED event maps' :event/sequence and
  :checkpoint/type via the writer's own filename format — no
  directory scans; a new checkpoint-events atom captures every append
  return (time-step at start, selection at persist-selection!, the
  rest at persist!); (3) manifest built at close!-entry under the
  same (and cohort? occurrence) guard as retention, per-entry
  admitted-at sampled at read, real file reader; block
  :admitted-evidence = manifest id vector; (4) stop-the-line honored:
  the initial fresh-JVM run caught the outer runner boundary
  CONVERTING a missing-sibling refusal into an initialization result
  — the retained failure in tests.out — and 09cc0d5b adds a narrow
  :evidence-manifest/refusal passthrough; (5) writer-returned
  :close-evidence-manifest joins :close-retention in the result map.
- Boundary asymmetry examined and accepted: a :close-retention/
  refusal at close still lands in the machine-failure/repair path
  (typed, retained, demands repair) rather than propagating. Both
  routes are fail-closed; retention refusals at close can only arise
  from runner bugs, for which the repair contract is the right
  destination. Noted, no change.
- Controls: happy path asserts the exact six ids in order in BOTH
  manifest and block, recomputes each entry sha256 independently over
  the actual event file bytes, and checks every admitted-at <= the
  close's :recorded-at; early failure asserts absence of all new
  keys; unreadable-sibling (event file deleted through the test seam)
  refuses with no 007 file.
- Receipts: fresh-JVM full runner namespace 155 tests / 812
  assertions exit 0 at 09cc0d5b; runner kondo baseline-delta 0/0;
  full-driver parens; honest trail (initial 1-failure run retained
  with the boundary finding named in the README).

The evidence chain for the retrospective route is now complete
end-to-end in code: occurrence identity at selection, sibling-record
admission with recomputed digests at close, writer-stamped
closed-at = cutoff with pre-append validation, and durable
manifest + retention agreement. What remains for annotation #1 is
non-code: authority binding, one-close preparation, one authorized
run, blind observation, exact-subject review.
