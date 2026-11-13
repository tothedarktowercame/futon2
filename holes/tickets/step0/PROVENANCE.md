# Step 0 provenance

Retrieved 2026-11-11 UTC.

| file | source | version/date | SHA-256 |
|---|---|---|---|
| `Langtons-Loops.table` | https://gollygang.github.io/ruletablerepository/downloads/Langtons-Loops.table | GollyGang/ruletablerepository file commit `bed1ce5c54c0e7df9e3b99169234183947f71eab`, 2015-06-29 | `83dc6c0d10aecc9717c003e670a67aa81d099e9491da749f90e48484df7c2605` |
| `Byl-Loop.table` | https://gollygang.github.io/ruletablerepository/downloads/Byl-Loop.table | same commit/date | `579a42ad44acd99ed629b1721fcf5c73b0a33a5668d510c6c96b71b542649004` |
| `Langtons-Loops.rle` | Ubuntu Noble `golly` package, `/usr/share/golly/Patterns/Loops/Langtons-Loops.rle` | `3.3-1.1build3` | `333134abf0901f026ad9eac3030a799a7e548c83b2301d3cce9989877ee4de41` |
| `Byl-Loop.rle` | Ubuntu Noble `golly` package, `/usr/share/golly/Patterns/Loops/Byl-Loop.rle` | `3.3-1.1build3` | `98b53c33d19af7d0efa4a419b726a092c9a6a19761d367ed103fbbe1feb0a7ac` |

Checks performed locally: the Langton table has 219 active rows and its RLE
has 86 non-quiescent cells. The Byl table has 144 active rows plus one disabled
erroneous row (`212052`) documented in the file, and its RLE has 12
non-quiescent cells.

Primary papers used to check periods and starting configurations:

- C. G. Langton, “Self-reproduction in cellular automata,” Physica D 10
  (1984), 135–144:
  https://faculty.cc.gatech.edu/~turk/bio_sim/articles/langton_loops.pdf
- J. Byl, “Self-Reproduction in small cellular automata,” Physica D 34
  (1989), 295–299:
  https://fab.cba.mit.edu/classes/MAS.865/topics/self_replication/Byl.pdf

The Byl paper explicitly states that the Figure 3 12-cell configuration has
the parent and daughter in the initial form after 25 time steps. The Langton
151-generation period is also independently recorded by CelLab's rule manual:
https://fourmilab.ch/cellab/manual/rules.html
