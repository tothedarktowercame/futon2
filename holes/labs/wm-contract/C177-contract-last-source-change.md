# C177 — contract provenance is the last Lean content change

Date: 2026-08-31

C175's committed checker already avoided the impossible repository-HEAD rule:
it compared `Holes.lean` blobs and passed across a later contract-regeneration
commit. C177 makes the provenance relation explicit and independently visible.

The primary comparison is now:

```sh
contract :source :git-sha
git log -1 --format=%H -- DarkTower/WarMachine/Holes.lean
```

The recorded/current blob equality remains as corroboration, and a dirty
working copy remains a failure. The check's output states its claim in one
line: **the contract was generated from the current content of `Holes.lean`.**

At the positive run, recorded authority and last source change are both
`e48a3158efa23533c42c53b0d7b5205e9a36b59a`, while repository HEAD is the
later regeneration commit. This is green: the regeneration commit changes the
export, not the Lean source it describes.

The negative control substitutes both a stale authority and a different source
blob. It is rejected for `:contract-authority-not-last-source-change` and
`:contract-source-not-current`, demonstrating that a real post-export Lean
change remains visible.
