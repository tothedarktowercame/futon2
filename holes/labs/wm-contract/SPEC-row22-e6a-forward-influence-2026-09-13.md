# Row 22 E6a: bounded paired forward-influence verifier

The verifier resolves one pinned fixed comparison context and two pinned R6
correspondence-status records, then independently reruns the reviewed E5
resolver for two distinct arms. The fixed context owns model/revision/run/tick,
the complete ordered occurrence/action domain, unchanged independent depth,
distinct arm identities, and the two slow modes. Only slow state may differ.

The two E5 shaped tables must be complete. Identical tables return the honest
nonqualifying result `:no-behavioral-influence`. Distinct tables establish only
that slow mode changed the R6 inputs. Today there is no independently reviewed
canonical correspondence from those inputs through complete R6 scoring and
posterior calculation. Both pinned status records must bind the exact shaped
arm and explicitly record that absence; asserted `:verified` scores refuse.
The verifier therefore stops with
`:e6a/r6-scoring-correspondence-unavailable`, recording the first changed
occurrence and that E2a, E3, and E2b were not reached.

This deliberately cannot report a changed selected/enacted occurrence. Doing
so would require inventing the missing R6 proof. Counterfactual fixtures are
comparison arms, not two real enactments. Once a canonical R6 correspondence
exists, a later packet must re-resolve E1/E2a, exact selection, canonical E3
pre-enact authorization, and E2b exact enactment for each arm. No unchanged
selection can qualify causal influence.

Production mode refuses unconditionally. The prospective runtime seam begins
after E5 shaping, before R6 scoring, and ultimately must make E3 mandatory
immediately before enactment. No runtime wiring occurs here.

