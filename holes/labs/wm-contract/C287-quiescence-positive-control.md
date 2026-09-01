# C287 — quiescence positive-control assessment

Date: 2026-09-01. Owner: `wm-evidence`.

No live lane was stopped, no live registry row was changed, and no background
job was launched or killed. This assessment exercised isolated inputs and
read the current state only.

## Verdict

The conditions described as quiescence in
`C247-quiet-window-drain-and-run.md` are jointly attainable, but there is no
single executable quiescence check whose PASS can presently be demonstrated.
The procedure is a human conjunction of repository status, lane-registry
output, ordinary-job output, and bounded-job output.

That distinction is material. The lane validator accepts a consistent active
holding, and both job-list commands are reporting commands rather than
quiescence verdicts. Therefore a zero exit from every command in C247 step 1
does not establish quiescence.

## Positive fixtures and independent falsifiers

### Repository state

An isolated Git repository with one empty commit produced zero bytes from
`git status --porcelain=v1` and a readable HEAD. This proves the repository
predicate can pass. The live five-repository loop independently demonstrates
the dirty-tree falsifier: Futon2 has one tracked modification, Futon3c has
four untracked files, and p4ng has two tracked modifications plus one
untracked PDF; mathlib4 and Futon3 are clean as observed.

Acceptance condition: each of the five porcelain outputs is empty and each
HEAD is readable in the same observation window.

### Lane registry

Calling `validate-registry` with four complete idle rows produced
`{:pass? true, :errors []}` and reported all four lanes `:idle`. A terminal
job retained by a holding produced `:stale-holding` and failed, as intended.

However, one well-formed active holding with job state `:running` also
produced `{:pass? true, :errors []}`. This is valid registry state but not
quiescence. Thus `lane_registry_check.clj` can prove registry integrity and
reject stale holdings; it cannot prove the C247 requirement that every lane
is idle. The operator must currently inspect all four reported `:state`
values.

Acceptance condition: validator exit 0 **and** exactly four lane reports,
each with `:state :idle`. Exit 0 alone is insufficient.

### Ordinary and bounded jobs

The live ordinary-job report contained only terminal entries. The bounded
report contained 107 historical records and zero units whose `ActiveState`
was `active` or `activating`. This establishes that the no-job portion is
attainable on the actual service without stopping anything.

Both `bg.py list` and `bg.py test-list` are inventory commands and return exit
0 regardless of whether the returned population includes active work.
Consequently their zero exits are not quiescence evidence by themselves.

Acceptance condition: no ordinary row has a live status, and no bounded row
has systemd `ActiveState` equal to `active` or `activating`.

## Can every condition be satisfied in practice?

Yes, as state predicates. Clean Git fixtures, four idle registry rows, and
zero active ordinary/bounded jobs have each been observed. The present
failure is operational state, not an intrinsically unreachable condition.

Generated artifacts, receipts, and logs are not intrinsically exempt. Only
ignored paths may accumulate without dirtying porcelain. The current
untracked Futon3c Markdown files and p4ng PDF are visible to Git and therefore
must be committed, deliberately ignored by an owner-approved rule, or moved
outside the repository before quiescence. Deleting or broadly exempting them
merely to obtain PASS would be indefensible.

The apparatus gap remains: because no command computes the conjunction, a
human can accidentally accept active holdings or jobs while every invoked
command exits zero. C287 does not modify that check; making the conjunction
executable is a separate decision.

## Shortest honest path from the observed state

1. Stop dispatching and allow the two current holdings (`wm-evidence` C287
   and `wm-nouns` C288 at observation time) to finish.
2. Clear the two completed-but-stale holdings (`wm-verbs` C285 and
   `wm-organization` C286) through the normal registry update path; do not
   edit their history away.
3. Have the owning lanes resolve the dirty trees: one tracked Futon2 file;
   four untracked Futon3c files; two tracked p4ng generated TeX files and one
   untracked p4ng PDF. Re-run porcelain rather than assuming the list stayed
   fixed.
4. Confirm ordinary and bounded inventories still contain no active work.
5. In one settled observation window, record all five clean porcelain
   outputs and HEADs, four explicit `:idle` lane states, and both empty active
   job populations. Any intervening repository movement closes the window.

This is a drain procedure, not yet a machine-issued PASS.

## Canonical focused commands

```sh
for repo in /home/joe/code/futon2 /home/joe/code/futon3c \
            /home/joe/code/mathlib4 /home/joe/code/p4ng \
            /home/joe/code/futon3; do
  git -C "$repo" status --porcelain=v1
  git -C "$repo" rev-parse HEAD
done
bb checks/lane_registry_check.clj
python3 /home/joe/code/futon3c/scripts/bg.py list
python3 /home/joe/code/futon3c/scripts/bg.py test-list
```

Interpretation must apply the acceptance conditions above; command exits
alone are insufficient.
