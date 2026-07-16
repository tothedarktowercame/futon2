# vendored: fargonauts/copycat (Python Copycat)

**Upstream:** https://github.com/fargonauts/copycat
**Pinned at:** `5593a109ab69fc38474c1cc626b8453954c442d7` (2025-04-23)
**Vendored:** 2026-07-16, for the Copycat xenotype-domain probe (M-propagators L5 / xeno loop).

## Lineage — read before trusting it as ground truth

Mitchell's Common Lisp → **Scott Boland's Java** → **J. Alan Brogan's Python**. Two
generations of translation. Brogan is explicit and honest about the method:

> In cases where I could not grok the Java implementation easily, I took ideas from the
> LISP implementation, or directly from Melanie Mitchell's book "Analogy-Making as
> Perception".

The sibling repo `jalanb/co.py.cat` describes itself as "not a direct translation, more
like *based on a true story*". So this is **not** the original artefact in the sense that
`futon5/vendor/metaca/` is — that one is the actual 2014 Elisp under study. This is a
re-implementation used as a *tool*, and its fidelity is an open question (see
FIDELITY below).

## Local changes

One, recorded in `py312-getfullargspec.patch`:

- `inspect.getargspec` was removed in Python 3.11; upstream is broken on ≥3.11.
  Replaced with `getfullargspec(...)[:4]`. Verified on Python 3.12.3.

Nothing else is edited. `copycat/gui/` is kept: `copycat.py` does
`from .gui import GUI` at module import time, so it is a hard dependency **even though
`gui=False` is the default** and no window is opened.

## It already runs headless — no port required

```python
from copycat.copycat import Copycat
d = Copycat(rng_seed=7).run('abc', 'abd', 'mrrjjj', 10)
# {'mrrkkk': {'count': 5, ...}, 'mrrjjk': {'count': 5, ...}}
```

`Copycat(rng_seed=..., gui=False)` — seeded and headless by construction. It prints some
chatter to stdout; redirect it.

## Measured on 2026-07-16 (the facts that decided the probe)

| property | result | why it matters |
|---|---|---|
| **Headless** | ✓ no GUI code path needed | no port required — this was the expected cost, and it's zero |
| **Deterministic per seed** | ✓ same seed → byte-identical distribution (`xyz`×30 twice; `mrrjjj`×10 twice) | **the sham can return exactly 0.0** — the ant gate's load-bearing element |
| **Seed-sensitive** | ✓ `mrrjjj` seed 7 → 5/5, seed 123 → 7/3 | there is variance to measure |
| **Stateless per trial** | ✓ `runTrial()` calls `slipnet.reset()` | unlike Metacat, which carries answer memory across runs |
| **Produces distributions** | ✓ `mrrjjj`×30 → mrrkkk 22, mrrjjk 7, mrrjjjj 1 | the natural observable |
| **Slipnet fidelity (partial)** | ✓ the 5 `opposite` pairs are **identical** to Marshall's Metacat `slipnet.ss:716-720` | evidence the port is faithful at least in slipnet wiring |

### The σ channel set

`slipnet.py:131-137` — the `opposite` links, i.e. the natural transpositions:

```python
opposites = [
    (self.first, self.last),
    (self.leftmost, self.rightmost),
    (self.left, self.right),
    (self.successor, self.predecessor),
    (self.successorGroup, self.predecessorGroup),
]
```

**10 nodes, 5 disjoint transpositions.** The ant C-vector's σ domain is also **10**
channels (the union; see `../../README-xeno-loop.md` §2). Same size, coincidentally.

## FIDELITY — `run()` silently suppresses the most important answer

Anchor data is in `anchor-mitchell-1993.edn` (transcribed from the book's bar graphs).
Measured against it:

| problem | port | Mitchell 1993 (1000 runs) | verdict |
|---|---|---|---|
| `ijk` | ijl 20/20 (100%) | ijl 96.9%, ijd 2.7% | ✓ consistent |
| `mrrjjj` | mrrkkk 73.3%, mrrjjk 23.3%, mrrjjjj 3.3% (n=30) | mrrkkk 70.5%, mrrjjk 19.7%, mrrjjjj 4.2% | ✓ good match |
| `xyz` | **depends entirely on the temperature formula** — see below | xyd 81.1%, **wyz 11.4%**, yyz 6.0% | ⚠ |

### The finding: the temperature-adjustment formula decides whether wyz exists at all

`xyz`, 100 trials per formula (seeds 1-4 × 25):

| formula | xyd | wyz | yyz | other | wyz vs book 11.4% |
|---|---:|---:|---:|---|---|
| **`inverse`** (class default) | 90 | **2** | 1 | xyz 4, xyy 3 | present, but ~6× low (p≈.002) |
| `pbest` (**forced by `run()`**) | **100** | **0** | 0 | — | **absent** |
| `original` | **100** | **0** | 0 | — | **absent** |

For comparison, `jalanb/co.py.cat` (the sibling port, no formula machinery):
xyd 96, xyy 2, yyz 1, **wyz 1** — present, ~11× low.

**So `Copycat.run()` — the obvious API, the one every example uses — hardcodes
`useAdj('pbest')`, and `pbest` suppresses `wyz` entirely.** Our first measurement went
through `run()`, which is why `xyz` looked like a flat xyd 30/30. The class *default* is
`inverse`, which does produce wyz. The two disagree, and nothing warns you.

**`_original` also suppresses wyz**, which is the strange part — it is named for Mitchell's
published curve and ends in `return max(f, 0.5)`, which floors every adjusted probability
at 0.5. Whether that matches Appendix B of the book is **unverified and worth checking**;
if it does not, the formula named `original` is misnamed, and that is a trap for anyone
who reaches for it expecting fidelity.

### Why `wyz` specifically is the one to protect

`wyz` is the answer Mitchell calls "the most elegant solution" — the only one requiring
Copycat to *restructure* after hitting the z-snag. Per Mitchell p.82, getting it requires
noticing that

> A and Z are at opposite ends of the alphabet, so there is a plausible correspondence
> between them **if the spatial and alphabetic directions of the two strings are also
> seen as opposite**.

That is `leftmost↔rightmost` **and** `successor↔predecessor` slipping together — **two of
the five `opposite` transpositions σ would permute**. The answer that most directly
exercises the σ channel set is therefore the one most sensitive to this configuration
wart. **Any σ experiment must pin the formula explicitly and verify wyz > 0 in the
σ=identity arm before trusting a single number.** A silent `pbest` would make every σ look
equally dead — the cyberant error, reproduced exactly.

Both ports under-produce wyz vs the book (2% and 1% vs 11.4%) even at their best. That gap
is unexplained and remains open; `coderack_pressure` exists in `jalanb/co.py.cat` and is
**absent** from this one, which is one candidate.

`distributions/` in the upstream repo is **empty** — nothing shipped to catch any of this.

## Anchor data

`anchor-mitchell-1993.edn` — all five target problems, both Copycat's 1000-run
frequencies and the human survey, transcribed 2026-07-16 from Mitchell 1993 ch.4.

**Read it before designing an objective.** Two things in it are easy to get wrong:

1. **The human frequencies are not a target.** Mitchell (p.76, p.78) says so explicitly:
   the survey was to collect *ranges*, not frequencies; subjects gave multiple answers;
   and for `xyz` the elicitation actively shaped the responses (subjects were told xya
   was impossible and asked for alternatives). What the data supports is a
   **range/set-membership** criterion, not distribution-matching.
2. **Copycat does not match humans on `kji`** — its mode is kjh (56.1%), the human mode is
   kjj (~49%), nearly inverted — and Mitchell does not treat that as a failure.

**Transcribe from page images, never the OCR text layer.** The OCR produced `mirkkk` for
`mrrkkk`, `murjjjj` for `mrrjjjj`, and silently **dropped `lji (8)` entirely** from the
kji survey — the third-most-common human answer.

## Reproduce the measurements above

```sh
cd futon2/vendor/copycat
python3 -c "
import sys, io, contextlib; sys.path.insert(0,'.')
from copycat.copycat import Copycat
def run(seed, tgt, n):
    with contextlib.redirect_stdout(io.StringIO()):
        return {k: v['count'] for k, v in Copycat(rng_seed=seed).run('abc','abd',tgt,n).items()}
print(run(7,'mrrjjj',10), run(7,'mrrjjj',10))   # determinism
print(run(123,'mrrjjj',10))                     # seed sensitivity
"
```

`abc:abd::xyz:?` is slow (~minutes for 30 trials). `mrrjjj` and `ijk` are fast.
