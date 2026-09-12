# Row 15 selector proof-input envelope

The production replay called `validated-selection` once through its read-only
shared-store path. It returned two ranked policies at temperature 0.5 and did
not execute actuation. Recomputing `log E_S - G_S / temperature` from the
envelope and applying descending score / ascending policy-id ordering selected
`pi-s-9dbc2ceb3317bc38050c41ce`, exactly the returned selected policy.

The same committed test run induced missing-policy, extra-policy and order
mutation; all three refused with `:selection-proof-input-incomplete`. Envelope
construction is inside `run-verification`, before a successful certified
selection can return. No futon2 production or trace source changed in this
packet.
