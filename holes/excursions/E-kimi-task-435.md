# E-kimi-task-435 — R1 deep-temporal port: pure namespace + Lean-transcribed tests (handoff 1)

**Requisition:** in-progress — dispatched 2026-11-25T04:25:49Z to kimi-1 as invoke-1795580749627-28915-53f12cb5

Clocked in by claude-14 for kimi-1 on 2026-11-25 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Port R1 (deep temporal form) to Clojure: a pure namespace with tests transcribed from Lean

Assigned by Joe (2026-11-25, claude-14 REPL): "look into whether we could use Kimi for the implementation in Clojure and we could start to develop some examples to run as test data". This is that named task under futon2/CLAUDE.md rule 1. Proposal it implements: ~/notes/claude-14/P-r1-benefit-belief-port-2026-11-25.md, handoff 1 ONLY.

SCOPE: one new namespace + its test. NO wiring into the click, selection, runner, or any existing namespace. Do not edit any other file. Do not hot-load into the serving JVM.

Files to create (futon2, /home/joe/code/futon2):
- src/futon2/aif/r1_belief.clj  (ns futon2.aif.r1-belief)
- test/futon2/aif/r1_belief_test.clj

The specification is Lean (mathlib4 worktree /home/joe/worktrees/mathlib4/c14-cascade-outcome, branch c14/cascade-outcome-type). READ these first; every function below names its Lean counterpart:
- DarkTower/WarMachine/BenefitBelief.lean  (Rung, Rung.up, climb, predictRung)
- DarkTower/WarMachine/R1DeepTemporal.lean (SlowBelief, lik, evid, post, update, update_cons, update_flat, update_pinned, predict, CauseCounts, theta, learn, learn_theta, learn_other_link, learn_other_col)
- DarkTower/WarMachine/R1DeepTemporalB17.lean (the B17 instance: uniform prior, traceProb, noTrace_realised = 3/34)
Literature behind it: ~/notes/claude-14/causal/R1-literature-alignment-2026-11-25.md.

Data (plain Clojure maps; use exact rationals, Clojure ratios, wherever the Lean uses ℝ literals, so the B17 numbers come out exactly):
- rungs: [:none :available :used :witnessed :realised]; (up r) as Rung.up.
- slow belief: {rung -> prob}, nonneg, sums to 1. Refuse (ex-info) on a belief that does not sum to 1 or has a negative entry.
- likelihood A: (fn [rung record-kind] -> prob) or a map {rung {kind prob}}.
- (lik A records rung) = product over records; (evid belief A records); (post belief A records) -> belief. If evid is 0, REFUSE (ex-info :zero-evidence), never divide.
- (update belief A records) = post.
- (climb theta) as a kernel {from {to prob}}: from :realised stays; else up with theta, stay with 1-theta.
- (predict belief theta pD) per R1DeepTemporal.predict.
- cause counts: {link {rung [climb stay]}}, all positive; (theta counts link rung); (learn counts link s-prev w) per Lean learn.

Tests (clojure.test), each named after the Lean theorem it transcribes:
- update-nil, update-cons (one record then the rest == all at once; exact equality with ratios), update-flat (constant likelihood leaves belief unchanged), update-pinned (likelihood zero except at k -> certainty at k).
- b17-notrace-realised: uniform 1/5 prior, traceProb {:none 1/10 :available 1/10 :used 3/10 :witnessed 2/5 :realised 7/10}, record :no-trace with likelihood 1 - traceProb -> realised = 3/34 exactly; evid = 17/25.
- learn-theta (formula), learn-other-link, learn-other-col (s-prev 0 at k -> column unchanged).
- Planted bad cases (each must be caught): a belief summing to 0.9 is refused; zero evidence is refused (not NaN); a likelihood giving negative probability is refused.
Report which mutants you tried: e.g. dropping the zero-evidence guard, or updating record-by-record with re-normalisation omitted, must fail a test.

Test data: add test/futon2/aif/r1_belief_examples.edn with the B17 example (records o1-o7 from futon3c holes/missions/M-cascade-live-causal.md, §"B17 as a cookie-jar case", each {:id :date :source :kind}) and the WM-loop want's six clauses (§"Wants as sentences") as {:clause :source :verdict} data. The test reads the edn and asserts the B17 numbers from it.

Gates: clj-kondo clean on both files; futon4/dev/check-parens.el on both; run ONLY `clojure -M:test -n futon2.aif.r1-belief-test` in futon2 (short timeout; a timeout is a failure). Commit the three files to futon2 master in one small commit.
Bell claude-14 back with: the sha, test count/results, the mutants and which test caught each, and anything in the Lean you could not transcribe exactly (say so; do not approximate silently).
