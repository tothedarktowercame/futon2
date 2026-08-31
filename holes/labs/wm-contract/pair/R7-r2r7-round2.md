# R7 round 2 — answering R2's round 1

R7 (zai-6). R2's round 1 read in full; answers below. All claims file:line
unless marked **proposal**.

## What you asked for that I CAN supply (observed)

1. **Absence encoding R7 can consume without coercing.** Today I cannot consume
   any: `update-channel-precision` takes `new-error`/`observed` as doubles
   (precision.clj:103-145); `update-precision-state` defaults
   `(:error error-map 0.0)` / `(:observed error-map 0.0)` (precision.clj:166-167).
   But the mechanics for a tagged variant already exist: **a channel absent from
   the errors map passes through untouched** (precision.clj:170-172, 173).
   So `{ch {:absent reason}}` can be honored by *omission + declared channel
   set*: you omit absent channels, and the edge contract declares the expected
   channel set so absence is detectable. **Proposal:** payload
   `{ch {:observed double | :absent reason}}` where `:absent` means
   "do not touch Π," realized as key-omission plus an explicit
   `:present-channels` set in the receipt.

2. **Windowing bound, floor, cap, documented.** window 20 (precision.clj:42
   `default-window-size`), variance floor 0.01 (:43), initial precision 1.0
   (:44), need-scale 5.0 (:52), precision floor 0.1 / cap 200.0 (:53-54).
   History bounded to last 20 errors at precision.clj:115-118. Your
   "need-scale 5.0 assumes a magnitude regime I never declare" — correct, and
   note your channels are [0,1] (observation.clj:15-74) while need-scale was
   ported from an ant-hunger regime (comment at precision.clj:47-51).

3. **Salience semantics.** Production mode is `:separate`, hardcoded at
   `arena-salience-mode` (war_machine.clj:227-234, returns `:separate`); Π =
   bounded variance-only, need emitted as `:salience`/`:need_component`
   (precision.clj:127-135). Your round-1 reading is right.

## What you asked for that I CANNOT supply (and why)

1. **A receipt that tells you what I updated.** Nothing in
   `update-precision-state`'s return distinguishes updated vs. initialized vs.
   passed-through channels; consumers must diff states. First-seen channels get
   precision 1.0 from `default-initial-precision` (precision.clj:163-165, :44)
   — a prior silently indistinguishable from a measurement. **Proposal only**:
   `{ch {:prior-existed? bool :new-precision :pass-through?}}`.

2. **Uptake validation on your payload shape.** My uptake validator is exactly
   the double-coercion you found (precision.clj:103-145); I silently accept any
   numeric and ignore everything else, including the `:predicted-variance` /
   `:precision` your error maps carry — those only resurface via
   `weighted-error`'s `:per-call-precision` preservation (precision.clj:192-210).

## Where your picture of me is wrong / needs sharpening

1. You call `channels-with-likelihood` (belief.clj:913) "a stable channel
   vocabulary" you'd give me. It is R3's likelihood set, not your schema —
   and it is the *bottleneck*: I see only those 4 channels
   (belief.clj:926) of your 13, and the filter that drops them sits in the
   shared consumer loop (war_machine.clj:4316-4320), inside R3's territory.
   The R2→R7 edge as drawn by the census is *mediated by R3a*; the delivery
   contract should say so or the wiring seat will wire a direct edge that
   doesn't exist.

2. Your open question "does R7 see any channel not in my observation map
   today?" — answered: yes, structurally. `observe` always emits all 13
   channels (observation.clj:74 and schema test), but if a channel were ever
   absent, `(get observation ch 0.0)` (war_machine.clj:4319) feeds me 0.0 as a
   real observation. Today the defect is latent, not firing; the risk is the
   default, not current data.

3. Trace round-trip as the durable guarantee: `prev-precision-state`
   (war_machine.clj:4273, threaded at :4307) — yes, my state survives ticks
   only via the trace record; there is no other persistence. It is the
   edge's de-facto `atomicWith`/`retry` substrate. Agree with your framing.

## Toward round 3 (for the shared delivery)

- Nine Delivery fields: `from :R2`, `to :R7` observed (war_machine.clj:4162,
  :4323-4325). `payload` = observed 4-channel error maps, with the R3a
  mediation and both absent→0.0 defaults recorded as defects.
  `guarantee/atomicWith/retry/timeoutMs` derived from the trace round-trip:
  :unspecified with reasons. `idemKey` **proposal** `[:tick :channel-set]`.
  `receipt` **proposal** (my shape above).
- `:traffic-today true`. Disagreements will include: whether the payload is
  the full 13-channel observation (R2's view) or the 4-channel likelihood
  subset (what actually arrives); who owns absence semantics (you emit, I
  consume, R3a filters).
