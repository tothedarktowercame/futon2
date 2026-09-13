# Retained failed elaborations

Both failures used
`lake env lean DarkTower/WarMachine/FullCertificatePredicate.lean` from
`/home/joe/code/mathlib4`. They preceded the final explicit `.olean` command.

1. Commit `1a566fdb9a`, source SHA-256
   `075d9374ac08703d8e9b984fe912cc632ad8df200e7ff05ed15493b280346437`,
   exit 1. `rejects_existing_lead_counterexample` left an implication goal and
   `rejects_unclosed_node` selected the wrong conjunction projection. Lean's
   interim axiom print therefore included `sorryAx` for those unfinished
   declarations. This is not a proof receipt.
2. Commit `d1586d9a2f`, source SHA-256
   `d23731e017508e13857b1732695e69a2ecbfcac23378e078550ae7b7be759e99`,
   exit 1. The lead-counterexample proof was repaired; the remaining error was
   the node-positivity projection (`hnodes.right.right` still had an equality
   paired with the universal condition). Its interim axiom print retained
   `sorryAx` only for `rejects_unclosed_node`. This is not a proof receipt.

Commit `cea8a83769` then elaborated, but was superseded by the source change that
pinned the sole permitted retirement to the exact delegated ruling and original
J2 SHA-256 values and required nonempty equation/declaration identities. Final
source and `.olean` are from `b9bbcb833e`.
