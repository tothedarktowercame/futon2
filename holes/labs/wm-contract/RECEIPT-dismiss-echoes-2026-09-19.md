# Receipt: dismiss four disposed-source T8 echoes

Date: 2026-09-19 UTC

Authority supplied for each disposition:

> Joe -> claude-12 repair-queue ownership 2026-09-19;
> FINDING-t8-duplicate-livelock-2026-09-19.md @ 465f5641;
> CLASSIFY @ 3db4e8e3

Each call used `:reason :repetition-echo-sources-disposed` and
`:actor "codex-24"`. The reviewed `futon2.aif.repair-obligation/dismiss-echo!`
at `da42cf3b34ea39542d195df3f10ba91805aa3bed` derived source membership
solely from each immutable finding's retained T8 witness.

## Queue counts

The runner-eligible predicate was applied to `open-obligations`: status
`:open`, excluding class `:environmental-hold`.

- Before: **13**
- After: **9**

All four calls returned `:repair/status :dismissed-echo`. There were no
typed refusals.

## Immutable dismissal records

- `data/wm-repair-obligations/dismissals/repair-initialization-8ceaae14-d8b5-4ceb-a8bf-ce9a4d4eed6a-tripwire-tripped.edn`
  — SHA-256 `7d6e04f0a4a432f1b19fe4801ed553e9b62ffecd2a02b5e9fcedbf8706c0e756`
- `data/wm-repair-obligations/dismissals/repair-initialization-82224aab-86f0-40eb-bede-b377b4216671-tripwire-tripped.edn`
  — SHA-256 `e88f4bc4f4b090d3e7a0da4f821586e95da5cc7da6c045f4bda8e42341a9fd9d`
- `data/wm-repair-obligations/dismissals/repair-initialization-4e120541-560a-4c43-b0b4-dabb95a5e7fd-tripwire-tripped.edn`
  — SHA-256 `6a4d505e8c9c1243a63b0c35eb172d1f52680cb55a004ab3dc295bd6a338a2ac`
- `data/wm-repair-obligations/dismissals/repair-initialization-d31f5b22-a76d-4431-9c1f-6f0300d34a2f-tripwire-tripped.edn`
  — SHA-256 `4a844243fd342d2f135625f0f42cd51246494bf2f70264856424cd952b3a9574`

No other finding was disposed and no click was performed.

## Re-currented validation warrant

Executed once through the validation CLI using the already committed
`runs/dismiss-unexecuted-2026-09-19/register.edn` scope:

```text
evidence-id test-registry-c06e22dad31f89b8403007713e510b64f8828851bb04f5e9a63b2ffd483279f1
warrant? true
results {:assertions 106, :duration-ms 1378, :errors 0, :exit 0, :failures 0, :tests 22}
bound repair-store/dismiss-unexecuted -> test-registry-c06e22dad31f89b8403007713e510b64f8828851bb04f5e9a63b2ffd483279f1
```

This replaces the stale binding caused by the later committed changes to
`repair_obligation.clj` and its test.
