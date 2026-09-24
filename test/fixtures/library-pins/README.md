# library-pins — the flexiarg bytes the close-path tests pin

`cascade-sources/read-receipt-source` re-reads every interpretation
receipt's `:source` file at admission/close and refuses when no admitted
root holds bytes hashing to the receipt's pinned `:sha256`. The tests that
replay RECORDED decisions bind `futon2.aif.cascade-sources/*code-roots*`
here so the pin check keeps its full force against bytes versioned WITH
the tests, never against the live sibling checkout
(`holes/labs/wm-contract/proof2/packets/RUNNER-SUITE-D.md`, futon2
795d4001).

Two generations are needed because the recorded decisions and the current
declared sources pin the SAME relative paths at DIFFERENT revisions — one
directory cannot hold two contents at one path, so each generation is its
own tree:

## `r7fc6a050/` — what the recorded decisions pin

Bytes for the receipts in `test/fixtures/new-wanted-token/1789964661.edn`,
`test/fixtures/occurrence-1789964661.edn`, and
`test/fixtures/eig-source-remaining/updater-only-declaration.edn`:

| path (relative to this tree) | pinned sha256 | bytes recovered from futon3 commit |
|---|---|---|
| `futon3/library/apparatus/one-authority-per-question.flexiarg` | `42371c5db7fa2cfbd82688b63aac9ab85518cc0baad7eeb69ccca94aca9bcda1` | `278185c` (2026-09-08; identical content at the receipt's declared `:revision 7fc6a050`) |
| `futon3/library/apparatus/done-is-observed-running.flexiarg` | `301a19b722382997e8e1d455be1c40605ebc987c998aedd5ce5302bcb4a70653` | `278185c` |
| `futon3/library/cascade-construction/run-it-on-a-real-case.flexiarg` | `3d006bdff9d3af808112139f9c0342bd93d1c4ab38adf550d9c4754a026d9046` | `19f363f` (2026-09-17) |

## `current/` — what the declared sources pin

A verbatim snapshot (2026-09-24, futon3 `9c248d5`) of every
`futon3/library/...` file pinned by `resources/wm/cascade-sources/*.edn`
(12 files); verified byte-for-byte against the declared pins at snapshot
time. When a lane legitimately re-pins a declared source (the way 031d9452
did), update this tree in the same commit — here an update is OWNED, not a
treadmill: nothing in the suite reads futon3's HEAD.

`../library-pins-tampered/` is the same layout with ONE byte flipped in
`r7fc6a050/futon3/library/apparatus/one-authority-per-question.flexiarg`;
the falsifier binds it and asserts the close still refuses
`:interpretation-source-hash-mismatch`.
