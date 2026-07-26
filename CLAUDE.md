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

## Store discipline (futon1b :7073)

- Never restart the store casually; it is systemd-managed
  (`systemctl --user … futon1b-server`) and restart windows are
  operator-gated.
- Always bound store queries (`&limit=…`, `include-total=false` unless the
  exact count is consumed; see the 2026-07-26 amplification findings in
  futon1b/holes/SPIKE-attribute-index-2026-07-26.md).
- The request journal (`journalctl --user -u futon1b-server.service`) logs
  every request with elapsed-ms — diagnose from it before theorizing.
