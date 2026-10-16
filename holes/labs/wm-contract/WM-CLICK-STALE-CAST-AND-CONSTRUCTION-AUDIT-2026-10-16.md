# WM click stale-cast and construction audit — 2026-10-16

## Finding 1: the stale names were an active second source of truth

The serving JVM and `scripts/wm_click.sh` were not loading an old cast. The
old identities came from `scripts/wm_click_repair_loop.py` itself. That file
was introduced in commit `97eae1e03` for the older overnight loop and retained
these argparse defaults after the designated WM cast changed:

    owner / issuing caller  claude-1
    author                  codex-proof2d
    reviewer                codex-proof2e
    repair reviewer         codex-2
    repair seat             wmfix-1

`fire` passed those values explicitly to `wm_click.sh`, overriding that
script's current `wm-author`, `wm-reviewer`, and `wm-repair-reviewer` defaults.
The loop's stop notification then recommended a restart command without any
cast arguments, guaranteeing that its obsolete defaults would be used again.

This is repaired by removing every identity default. A loop run cannot start
until owner, issuing caller, author, reviewer, and repair reviewer are stated
explicitly. The repair worker defaults only to the explicitly named repair
reviewer. The stop notice says to reuse the cast recorded in the ledger's
start row. `test/scripts/wm_click_repair_loop_test.py` proves omission fails
closed and the current explicit cast is retained without a `proof*` identity.

## Finding 2: `no-policy-family` concealed a pre-construction refusal

Run `2026-10-16-8ac6038a-086b-40b4-b269-fd23df9ccf05` did not attempt
cascade construction and discover that no useful policy existed. Its retained
`:selected-target-reading` says that the reading for `M-the-futon-stack` was
refused before publication because display-cue offsets did not match their
interpretation span. The later abstention contains only the summary
`:no-policy-family` and no nested failure, so the debugger printed
`no-policy-family/unknown`.

The debugger now joins that target with the retained reading phase and reports
`selected-target-reading-validation-refused/before-cascade-construction`,
including the validator detail. This is diagnostic repair, not a claim that a
G(pi) construction ran.

## What `policy family` means on the current route

When the prerequisite reading is valid, `target-policy-field` calls
`family-selection/families-for-field` to construct target-local structural
cascades from that reading and the pinned pattern graph. `select-over-families`
then calls `cascade-shape-g/score-policy` on every constructed cascade and
makes one selection over their G(pi) values. The policies are therefore
computed arrangements for the selected problem, not a static catalogue of
pre-existing actions.

The problematic wording was the shared wrapper `:no-policy-family`: it was
used both when construction genuinely formed no scored policy and when the
reading prerequisite failed before construction. The debugger now keeps
those cases distinct. The production correction-pass for malformed readings
is separately implemented in `485960046`; this audit does not weaken its
validation.
