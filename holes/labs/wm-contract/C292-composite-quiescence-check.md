# C292 — executable composite quiescence check

Date: 2026-09-01. Owner: `wm-evidence`.

## Deliverable

Canonical operator command:

```sh
cd /home/joe/code/futon2
python3 checks/quiescence_check.py
```

The command issues exactly one of three typed outcomes:

- exit 0, `QUIESCENT`: five clean repositories, exactly four explicitly idle
  lanes, no active ordinary jobs, no active bounded jobs, and a stable state
  sandwich;
- exit 1, `NOT-QUIESCENT`: a complete observation contains one or more named
  blockers;
- exit 3, `UNAVAILABLE`: an input could not be read or the observed state
  changed between the start and finish snapshots.

It does not retry. A moving observation cannot become clean merely because a
later retry happens to land between commits.

The check composes `lane_registry_check.clj` without changing its semantics.
Registry validity remains that check's claim. The composite adds the stronger
requirement that the four named lane rows each report `:idle`; a valid active
holding therefore fails quiescence.

The state sandwich covers each repository's HEAD and exact porcelain output,
the registry content digest and validator/lane reports, and the identities of
active ordinary and bounded jobs. All are captured twice. A difference is an
unavailable observation rather than either clean or non-quiescent.

## Controls

Canonical controls:

```sh
python3 checks/quiescence_check.py --self-test
```

Observed exit 0 from the control runner, with these expected/actual composite
exits:

```text
clean             0 / 0  QUIESCENT
dirty-tree        1 / 1  NOT-QUIESCENT
active-holding    1 / 1  NOT-QUIESCENT
stale-holding     1 / 1  NOT-QUIESCENT
ordinary-active   1 / 1  NOT-QUIESCENT
bounded-active    1 / 1  NOT-QUIESCENT
state-moved       3 / 3  UNAVAILABLE
```

The clean fixture supplies five clean, readable repository bases, the exact
four-lane idle population, and empty ordinary/bounded active sets. This is the
positive control missing from C283. Each negative mutates one condition only.

## Live focused result

The live command returned exit 1, `NOT-QUIESCENT`, as required. At observation
time it named:

- dirty trees in Futon2, Futon3c, and p4ng;
- active holdings in all four lanes;
- bounded job `bounded-1788222835143-workspace-gate` active;
- no active ordinary job.

The observation was stable across its start/finish sandwich. No lane was
stopped, no registry row was edited, and no job was launched or killed.
