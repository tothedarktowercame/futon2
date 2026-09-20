# PROPOSAL — closing the C591 hole (for Joe's sign-off)

The hole: `Holes.lean:157` defines `C` for pragmatic vertices as a
definition-level `sorry`, under a DEFERRAL contract (Items 17/18c,
approved 09-08/09) whose text fixes the structure — "a time-indexed
Cτ family whose terminal member is the ruled outcome-kind
distribution... does not license choosing its value" — and defers to
"recorded observation-to-disposition evidence" and operator rulings.

What has happened since the deferral was written, all operator-adopted:
the C replacement declaration derives preference content from the
corpus through the mission-declared-wants projection; production
supplies C as a step-indexed family whose terminal member is the
declared spec (certified :varies-across-horizon, conformance rows
1-9); the schedule is declared-as-such (placement :terminal,
elsewhere :uniform-over-non-ruled-zero). The structure the contract
deferred to now exists and runs.

## The proposed closure

Replace the sorry with the RULED STRUCTURE, value still parameterized:

    def C ... (spec : PreferenceSpec V) (schedule : ...) : ℕ → Obs v → ℝ

shaped by `ZeroPreferenceExclusion.preferenceAt` (already proven
nonneg/sum-1): the declared spec at its declared (terminal) step, the
declared elsewhere-distribution otherwise. The def takes the spec as
an ARGUMENT — no value is chosen in Lean, honoring "does not license
choosing its value"; which spec applies remains a per-declaration
runtime fact with provenance, exactly as today.

Docstring: DEFERRAL block replaced by a CLOSED-BY-STRUCTURE block
citing the original rulings, the certified production shape, and this
proposal; the disposition-bridge (P(d|o)) sentence stays, explicitly
marked as the remaining open sub-obligation (it is about fitting a
bridge from evidence, untouched by this closure).

## Safety facts (verified at source tonight)

- No proven theorem consumes `Holes.C`: every certified module prints
  clean axioms, so the sorry propagates nowhere — the type/def change
  breaks no proofs.
- The sha256 pins in Holes.lean are fixtures pinned BY its closed
  rows, not pins on the file; the edit adds no pin breakage there.
  (The futon2-side admitted-pin check runs again at edit time as the
  gate.)

## What sign-off authorizes

One commit to Holes.lean (the def + docstring), a small companion
module with the structure lemmas if any are needed beyond
preferenceAt's, `lake build` proof that nothing else moved, and the
hole's row moving from HOLE to CLOSED-BY-STRUCTURE in the same
commit. Without sign-off, nothing moves: the contract text is
explicit that an operator ruling fixes this.
