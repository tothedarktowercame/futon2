# C210 — Isolated-worktree design and shared-JVM boundary

Date: 2026-08-31
Status: design only; not implemented; no worktree created

## Finding

An isolated Futon2 worktree cannot currently run the production War Machine
click. `CLAUDE.md` retires a fresh `clojure -M:wm-full-loop` click process and
requires production clicks to enter through `POST :7070/api/alpha/wm/click`.
That endpoint runs inside the one serving Futon3c JVM. Its runner service uses
`requiring-resolve` for `futon2.aif.full-loop-runner/config` and
`run-opportunity!`; it executes the Futon2 namespace already loaded in that
JVM. It does not ask a worktree process to execute code.

Loading the worktree copy into that JVM would violate the explicit rule
“Never `load-file` a worktree copy into a shared JVM.” Starting a second
serving JVM from the worktree would violate the one-serving-JVM rule. The
design therefore stops before either action.

The Agency boundary is otherwise endpoint-shaped: the runner calls roster,
bell, job-status/cancel and strategic-selection HTTP routes, and the Futon1b
store over HTTP. Those calls do not load worktree code. However, the click
itself is not merely an HTTP client: Futon3c invokes the already-loaded Futon2
runner in-process. Shared Agency state, selection service, agents, and evidence
store remain shared even in a future isolated-input design.

## Why a worktree alone does not remove TOCTOU

A detached worktree at commit X would make its committed files immutable by
convention and is useful for tests. It would not establish that production ran
X: the serving JVM may contain a different loaded Futon2 namespace. Moreover,
the runner currently opens mutable canonical paths outside such a worktree,
including:

- `/home/joe/code/futon2/holes/labs/M-aif-full-loop-46/cohort.edn`;
- `/home/joe/code/futon2/data/wm-full-loop` and the phase log;
- repair-obligation, morning-brief, and tripwire stores below the canonical
  Futon2 `data/` directory;
- repository discovery rooted at `/home/joe/code` and mission/artifact paths
  resolved from live repositories.

Thus neither code identity nor input identity is bounded by creating the
worktree. Active changes to canonical files could still affect a click.

## A viable future boundary

The smallest honest architecture has two independently pinned parts:

1. **Serving code provenance.** The serving JVM reports the Futon2 code/build
   identity actually loaded. It is deployed only from the canonical checkout
   by the approved reload/restart path, never from a worktree.
2. **Immutable run bundle.** Read-only inputs for one click are materialized
   from commit X into a content-addressed bundle. The click request names its
   manifest hash. Every run-time read that affects selection or execution is
   either from that bundle or from an explicitly named live service. Mutable
   outputs go to separately configured durable stores, never back into the
   bundle.

Readiness would then compare: tested tree X; bundle manifest derived from X;
serving JVM code identity compatible with X; all named live-service health and
schema checks; and no unclassified filesystem reads. The operator receipt
would carry all identities. Development could continue in the shared checkout
because the run consumes neither its code nor its read-only inputs.

This requires a filesystem-read inventory and configurable roots before it is
safe. Until those exist, C207's repository-wide quiescence remains the valid
boundary.

## Cost and lifecycle

The Git object database is about 52 MiB and is shared between linked
worktrees, but the present non-`.git` checkout is about 1.2 GiB. A sparse or
bundle-only checkout could be smaller only after the dependency inventory
exists. Worktree creation itself should take seconds; bounded verification
still costs roughly 105 seconds, plus bundle construction and hashing.

The stale-fork hazard is avoided only if the worktree is detached, ephemeral,
read-only, and never a development branch: create at an already committed SHA,
produce a receipt/bundle, then remove it after the run. There is nothing to
merge back. A persistent or writable worktree recreates the recorded five-fork
problem and is outside this design.

## Decision required from Joe

Do not approve “run production from a worktree” as a shell-command change. The
choice is whether to fund the run-bundle and serving-code-provenance boundary.
Without it, pause lanes for the approximately two-minute verification window
and the operator run, as C207 specifies.
