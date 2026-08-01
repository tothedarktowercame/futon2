# Reviewer's gate record — ants faithfulness scan

*claude-4, 2026-08-01. The mission's R16 witness clause requires the reviewer to
re-run from logged seeds and not trust the summary. This records what was
actually checked, including where the check fell short.*

## 1. Independent re-run — PASS (89/90)

Ran `clojure -M -m ants.aif.experiment authority 30` to a scratch path from the
seed formula documented in `authority.md`.

- committed artifact: 63,597 bytes, sha256 `fd19862b6db77f8f…`, **90** seed-keyed rows
- reviewer re-run: 47,635 bytes, sha256 `1caa5fa5a53206b0…`, **89** rows

The hashes differ **because the re-run timed out at 580 s**, not because the
numbers differ. The file it produced is valid balanced EDN — a complete write of
an incomplete run.

**On the 89 rows present in both: 89/89 yields identical. Zero mismatches.**

The single absent row is the one that had not finished when the timeout fired.
Reproducibility is therefore verified to 89/90 by an independent re-run, and the
remaining row is unverified rather than contradicted. Stated this way because
"verified" without the qualifier would be a stronger claim than was earned.

## 2. Environment not tuned mid-experiment — PASS

`git log --name-only` over the lane shows codex-9 landed **one commit**
(`2e13366`) containing the results plus the arm plumbing in
`aif/{core,experiment,policy}.clj`. No config change between runs. The declared
environment block is recorded inside `authority.md` itself.

This mattered because the lane's history contains exactly the opposite pattern
(`8d78027` "harsher env + re-test", `79ac385` "scarcity attempt").

## 3. Static verdicts from code, not from the mission doc — PASS, with one error

Spot-checked codex-8's two load-bearing claims at file:line:

- `infer-mode` (policy.clj:46) is called only at :86, inside `efe-tilt`, which is
  itself called nowhere. The live path is `affect/next-mode` (core.clj:153), a
  documented hysteresis FSM over eight hardcoded thresholds. **Confirmed.**
- `pred-variances` is `(:var mu)` — the current belief variance, identical across
  candidates — while `pred-means` varies by action (policy.clj:612-624).
  **Confirmed.**

**One error found in the static scan.** Its R14 row states adaptive `tau`
"genuinely controls selection sharpness". Selection is
`(apply max-key :p policies)` (policy.clj:1022-1025) — argmax, not sampling.
Argmax over a softmax is argmax over the logit, which is argmax over `−G` for
every τ > 0. **τ cannot affect the choice.** codex-9's A3 = exactly-A0 result is
the empirical shadow of this. R7's pass is also partly justified by adaptive
temperature; the per-channel precision half stands, the temperature half does
not.

## 4. Rework spec — NOT FOUND, correctly reported

codex-8 searched holes, missions, futon0, and git history including deleted
paths, found no policy-selection rework specification, and explicitly refused to
substitute the two adjacent unbuilt proposals
(`F-propagator-on-c-vector-NEGATIVE.md`, `cascade-ants.edn`). That refusal is the
right behaviour and is recorded as such.

## Verdict

Both halves accepted, with the R14 correction applied. The gated decision
reverses: **authority is high**, so `cyberants-replay`'s null is a real result
about the controller, and Slice 5 is worth running — after re-specification,
since its named contrast ablates a term shown to be structurally inert.
