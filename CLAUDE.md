# CLAUDE.md — futon2

(For the clj-ants-aif agent contract see AGENTS.md; this file carries
cross-repo operational invariants that apply when working in futon2.)

## I-0: One Serving JVM (inherited from futon3c — binding here too)

**There is exactly one serving JVM on this machine (the futon3c omni-JVM),
plus one sanctioned exception: the futon1b evidence store on :7073** —
separate *because it is the evidence store* (durability and
restart-independence; each has been restarted without touching the other,
and that independence has paid for itself operationally).

**Do NOT start runtime JVMs from this repo.** In particular:

- `clojure -M:wm-full-loop …` as a process is RETIRED for clicks
  (M-omni-wm-runner, 2026-07-26). War Machine clicks run in-process in the
  serving JVM: `POST :7070/api/alpha/wm/click`. The alias remains only for
  `status`/`brief`/`review` style read-only CLI use and tests.
- The runner *code* lives here (futon2.aif.full-loop-runner) and is loaded
  by the serving JVM; changes to it reach production via Drawbridge reload
  or the next JVM restart — never via a fresh process.
- Short-lived tooling JVMs (`clojure -X:test`, `-M:wm-full-loop status`,
  linters) are fine: they exit when done and serve nothing.

Why this is written here and not only in futon3c: the invariant lived in
futon3c/CLAUDE.md while the violating pattern (the `-m` click entrypoint,
futon2 `d0be5e6`) grew in this repo unremarked for twelve days. Invariants
must live where the violations get built.

## Test discipline: check a warrant before you run a suite

**Do not run futon2's full suite as a matter of course.** It is minutes of CPU
on a shared box, and one wedged test takes it away from everyone: on
2026-09-17 a WM-08 rehearsal in `test/futon2/aif/` was killed at 400 s, then
again at 150 s after its fix, and while it sat there `clojure -M:test -m
cognitect.test-runner` did not terminate for anybody.

Reach for evidence first, execution second:

```bash
# Does it STILL hold? Sub-second, on the running futon3c JVM — never launch a
# JVM to read (Joe's ruling 2026-09-19; a cold `clojure … check` costs ~30 s).
curl -s -X POST localhost:7070/api/alpha/test-registry/check \
  -H 'Content-Type: application/json' \
  -d '{"entry-id":"test-registry-…","repo-root":"/home/joe/code/futon2","changed-paths":[]}'
# {:check {...} :meaning "validity-now, …"}; GET /api/alpha/evidence/<id> is the
# MINT verdict (who/what/counts), a different answer from "still holds".
# On :stale-sha, read :changed-files — it names WHICH file moved. Uncommitted
# drift = a lane is mid-edit (wait); committed drift = superseded (re-mint).

# If you must run, run the narrowest thing that answers the question
clojure -M:test -m cognitect.test-runner -n futon2.aif.some-test
clojure -M:test -m cognitect.test-runner -n futon2.aif.some-test -v futon2.aif.some-test/one-case
```

A check refuses — `:stale-sha`, `:environment-mismatch`,
`:results-log-mismatch` — the moment the code, tests, environment or load
closure move, and that refusal is your signal to run. The warrant's log lives
in the write-only ledger under its own sha256, so it survives the file being
moved or deleted; see futon3c/CLAUDE.md I-6 for the full contract.

You changed the code? Then the warrant refuses by design and running is the
point. Everything else is re-execution in place of evidence.

## Store discipline (futon1b :7073)

- Never restart the store casually; it is systemd-managed
  (`systemctl --user … futon1b-server`) and restart windows are
  operator-gated.
- Always bound store queries (`&limit=…`, `include-total=false` unless the
  exact count is consumed; see the 2026-07-26 amplification findings in
  futon1b/holes/SPIKE-attribute-index-2026-07-26.md).
- The request journal (`journalctl --user -u futon1b-server.service`) logs
  every request with elapsed-ms — diagnose from it before theorizing.

## Claude sessions on the War Machine: ground rules (Joe, 2026-10-18)

From 2026-10-10 to 2026-10-18 no Claude session worked on the War Machine:
repeated Claude interventions had rewired a working machine to suit our own
taste, invented process in place of the written one, and spent clicks under
conditions known to fail. Joe lifted that ban on 2026-10-18 and replaced it
with these rules. The division of labour behind them: Claude is good at
coming up with ideas, Codex at carrying out instructions, and an idea reaches
the machine only through someone who did not come up with it.

1. **Roles.** A Claude session may read everything, analyse runs and records,
   write proposals and notes under `holes/labs/`, and review Codex commits.
   It does not edit WM code, hot-load into the serving JVM, spend or grant
   clicks, or touch run data, unless Joe assigns it a named task. Codex
   builds; the Codex owner of the current campaign decides whether a Claude
   proposal gets built.
2. **The specification is fixed.** `Requirements.lean` and the wm-contract
   are the specification. A proposal that would change it says so in its
   first line and goes to Joe; it is never implemented on the way to
   something else.
3. **Read the written procedure first**: `holes/labs/A-self-heal.md`,
   `futon3c/README-pilot.md`, and the current campaign TN under
   `holes/labs/wm-contract/`. If no procedure covers the case, say so; do not
   invent one and present it as the way things are done.
4. **Only the serving JVM certifies.** When a click fails, repair the held
   phase live in the debugger and `:retry`; do not abort and re-fire. A replay
   of recorded inputs is a way to find defects and must run through the
   serving JVM's own code path. A copy of the machine certifies nothing: on
   2026-10-10 one stubbed the trace writer and passed a path that then ran out
   of memory live.
5. **An open question that bears on whether an action is valid blocks the
   action.** Investigate it or stop. Do not file it as a question for Joe and
   carry on.
6. **The Codex lab partner is a peer.** Bring it ideas and doubts as
   questions, not task lists. It may decline, and its disagreement is a reason
   to stop and reconsider, not something to absorb into the plan.
7. **Small, reviewable changes.** Speed of change is not progress.
