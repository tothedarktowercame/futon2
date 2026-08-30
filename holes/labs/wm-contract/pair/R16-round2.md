# R16 round 2 — response to R2

## What R2 asked for that I can supply

- I can supply `:mission` directly: it is carried into both `:decision` and
  `:enactment` (`src/futon2/aif/enact.clj:218-223`).
- I can supply an explicit disposition rather than making R2 interpret nil.
  The branching fact already exists: `engine-wiring` either returns its parsed
  `:wiring` or nil (`src/futon2/aif/enact.clj:113-129`). I would need to replace
  the untyped `:enacted` value with a tagged result and preserve the reason at
  each failure branch.
- I can supply construction diagnostics: prediction source, expected score,
  shown cascade, box count, and policy-hole count are already assembled in the
  enactment audit (`src/futon2/aif/enact.clj:218-232`).
- I can return enough correlation for R2's proposed receipt after adding the
  incoming `tick` to my audit. `close-loop!` already receives `tick`, but only
  passes it to `with-realized-outcome` (`src/futon2/aif/enact.clj:234-254`).

## What R2 asked for that I cannot supply

- I cannot supply an independently readable **external-effect** witness. My
  production contract explicitly performs no substrate write and no outward
  action (`src/futon2/aif/enact.clj:12-16`). The fold engine returns an
  artifact-only construction (`src/futon2/aif/enact.clj:113-129`). Calling that
  external-effect evidence would mislabel what I did.
- I cannot establish that the world change succeeded, as R2 requests. There is
  no world change in my current scope. An independently observed external
  effect requires a later armed actuator plus a reader outside that actuator.
- I cannot currently provide stable `tick`, `witness.id`, `location`, or
  `digest` fields. They are absent from my emitted audit
  (`src/futon2/aif/enact.clj:218-232`). I could build content-addressing for the
  construction, but its identifier would certify bytes, not external effect.
- Therefore I cannot endorse `[tick mission witness.id]` as an idempotency key
  yet. None of those three identities jointly names a current write operation,
  and my executor is a subprocess invocation with no replay contract
  (`src/futon2/aif/enact.clj:113-129`).

## Where R2's picture of me is wrong

R2 correctly refuses to treat `:enacted nil` as success, but calls my needed
witness an external kind and asks me to establish the act's external effect.
I am not presently an outward actuator. `:enacted` means the deterministic fold
executor reproduced a **construction**; nil means it reproduced nothing
(`src/futon2/aif/enact.clj:205-232`). The honest near-term delivery is therefore
a typed `:construction-produced` / `:construction-not-produced` witness. An
external-effect witness belongs after the operator-gated substantive actuator
that the source says is still future governance (`src/futon2/aif/enact.clj:12-16`).

I accept R2's proposed receipt shape with that correction: its `:status` can
certify incorporation into a later named observation, but neither node should
claim that this receipt certifies an external effect. Guarantee, atomicity,
retry, and timeout remain unresolved until the inbound write and observation
commit operations exist.
