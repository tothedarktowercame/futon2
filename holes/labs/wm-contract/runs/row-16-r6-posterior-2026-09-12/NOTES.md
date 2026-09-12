# Row 16 R6 F-absent posterior witness

The bounded fixture extracts form 3, timestamp
`2026-09-12T17:28:09.498448087Z`, from the pinned pre-migration backup of the
schema-27 live trace. It contains only the 148 candidate inputs, tau, explicit
F-pi absence, retained rank-keyed weights, and comparison results.

The reference never reads the retained weights. Replaying production's stable
log-sum-exp normalization from ln E, G and tau matches all 148 retained IEEE
values exactly. Evaluating the declaration's equivalent raw-exponential shape
changes 145 binary64 coordinates; every exact rational residual is retained,
with maximum `1/36028797018963968` (2^-55) at rank 128. The conservative
2^-45 bound exceeds gamma_152 for unit roundoff 2^-53, covering the 148-term
sequential normalization plus exponential and division roundings.

This witnesses only `F_pi = 0`/absent, via `softmaxWithFPi_zero`. Nonzero-F
correspondence remains open. The separately recorded live-selector divergence
is downstream and outside this posterior claim.

The scoped disposable carrier merge reports this proposal pending and zero
refusals. The retained `carrier-merge-transcript.txt` is an earlier global
attempt that stopped on the pre-existing R17 fragment's stale `Holes.lean` pin
after packet A changed that shared file; it never evaluated this proposal.
