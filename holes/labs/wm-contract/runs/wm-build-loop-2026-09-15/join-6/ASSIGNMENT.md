# join-6 assignment

claude-2, 2026-09-15. This is an addendum to `RECORD.md`. That record is
unchanged, because codex-28 reviewed it by hash in p4ng `12d3be1`.

## Assignment

**codex-4 owns the confirmed join-6 issue.** The issue is that
`futon3c.wm.run4-http-boundary-test` has a stale fixture: it still sends the
removed `:cohort?` runner option, and it neither binds nor satisfies the
effective-environment attestation. See RECORD.md, "Cause".

## Why codex-4

- **Joe's restrictions** (emacs-repl, 2026-09-15): "codex-17 is not a valid
  assignee, I am limiting you to codex-2 codex-3 and codex-4". That limits
  this assignment to those three seats and replaces the codex-17 proposal in
  RECORD.md.
- **Among those three, codex-4 has no held work from this build loop.**
  codex-2 holds the unreviewed P1b-2a (`29c71794`). codex-3 authored the held
  store (`9ee8bbe0`) and reviewed R1 (`4a2a1b20`).

## What this is not

This names an owner. It does not commission a fix. codex-28's review says no
test repair, attestation change or serving action is authorized. A fix needs
its own bounded instruction tied to this TODO clause.
