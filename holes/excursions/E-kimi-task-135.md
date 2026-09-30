# E-kimi-task-135 — M-象-2000 INTERACT-1 §0: dramaturge scene driver

**Requisition:** completed — 2026-09-30T03:36:27Z, job invoke-1790737913926-28100-5557ee6a, state done

Clocked in by claude-17 for kimi-3 on 2026-09-30 (one Kimi task, one excursion, so the seat's
conversation starts fresh; see scripts/kimi-task.sh).

## Packet

# Packet 1 of INTERACT-1: dramaturge scene driver (§0 only)

From claude-17 (owner of M-象-2000). Joe approved the task list on 2026-09-30.
Context, read first: futon3c/holes/labs/M-象-2000/INTERACT-1-interactional-compliance.md
(section §0 is THIS packet; features I1-I13 come later, one packet each, after
review. Do not start any of them.)

## Goal
Extend futon4/dev/arxana-dramaturge.el (it already has deftest, assertions,
`arxana-dramaturge-await`, run-all) so a test can DRIVE an Emacs surface in a
separate Emacs, not only read one. One behaviour: scripted scenes.

## Build
1. `arxana-dramaturge-scene` macro + runner, in a new file
   futon4/dev/arxana-dramaturge-scene.el (require the existing file; do not
   rewrite it). Steps, each reported pass / fail / timed-out:
   - (open-buffer NAME-OR-FORM), (type TEXT), (key "RET") — keys go through
     `execute-kbd-macro` in the target buffer, so real keymaps are exercised;
   - (await PRED :timeout S) — reuse arxana-dramaturge-await;
   - (assert-buffer REGEXP), (assert PRED DESCRIPTION);
   - (snapshot NAME) — write buffer text to <out-dir>/NAME.txt (htmlize if
     available, plain text otherwise; say which).
2. Scenes run in a SEPARATE daemon: `emacs --daemon=dramaturge -Q` with
   load-path set to /home/joe/code/futon3c/emacs and /home/joe/code/futon4/dev,
   driven with `emacsclient -s dramaturge`. A shell entry point
   futon4/dev/run-dramaturge-scenes.sh starts the daemon if absent, runs all
   scenes (or one by name), prints the report, exit 1 on any failure.
   NEVER run scenes in Joe's Emacs (the default server). The runner must refuse
   if the target server name is not "dramaturge".
3. Stub agent: a replayer that stands in for the REPL's call function
   (`claude-repl--call-claude-streaming` signature: (text callback)) and replays
   a canned NDJSON file from futon3c/emacs/scenes/streams/. Deterministic,
   no tokens. Leave a `:live` flag on scenes for later; no live scene now.

## Acceptance (both must be shown, with output pasted in your bell)
A. Re-enact the r/R keymap bug (futon3c 1253a8cc). Scene: load
   futon3c/emacs/turn-stepper.el twice (as a live reload does), open a
   buffer in turn-stepper-mode, press `r` via the key step, assert that
   `turn-stepper-rewind` was called (stub it to set a flag). Then PLANT the
   bug: in a scratch copy, bind the keys inside the defvar as before 1253a8cc
   and simulate a map left from an earlier load; the same scene must FAIL at
   the key step. Show both runs.
B. A scene whose await never becomes true reports "timed out" within its
   timeout and the runner continues to the next scene (no hang).

## Gates
- futon4/dev/check-parens.el on every .el you touch;
  byte-compile with byte-compile-error-on-warn t; ERT for any pure helpers.
- Stage explicit paths only (shared checkouts; never `git commit -a`).
- Do not touch Joe's running Emacs or the futon3c/futon1b JVMs.

Bell claude-17 back with a summary, the commit shas, and the pasted output of
acceptance A (both runs) and B.
