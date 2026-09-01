# refs/ -- sources for aif-equations.edn

Retrieved 2026-09-01 by claude-1 for the TN-edge review
(`holes/TN-edge-review-aif-wiring.md` §7). PDFs and extracted text are
**not committed** (see .gitignore); this index records what was fetched,
from where, and its checksum so the same file can be re-fetched and
verified. Equation numbers cited in the registry were read from these
files; where the text extraction garbled a formula, the registry says so.

| key | file | sha256 (first 16) | pages | source | access |
|---|---|---|---|---|---|
| buckley2017 | buckley2017.pdf | 44da91a454474f8a | 77 | https://arxiv.org/pdf/1705.09156 | arXiv preprint (open) |
| dacosta2020 | dacosta2020.pdf | 66bfbf026448835f | 36 | https://arxiv.org/pdf/2001.07203 | arXiv preprint (open) |
| friston2016 | friston2016.pdf | b798a2a3efeb66fd | 18 | https://europepmc.org/articles/PMC5167251?pdf=render | PMC (open; CC BY) |
| friston2017 | friston2017.pdf | c0f3c5f090f21e1b | 49 | https://www.fil.ion.ucl.ac.uk/~karl/Active%20Inference%20A%20Process%20Theory.pdf | author's page (Neural Computation is OA) |
| friston2018bmr | friston2018bmr.pdf | 4d55b4997dfcc28a | 32 | https://arxiv.org/pdf/1805.07092 | arXiv preprint (open) |
| kass1995 | kass1995.pdf | 08ac5292c9595325 | 23 | https://www.stat.cmu.edu/~kass/papers/bayesfactors.pdf | author's page; **scanned image PDF**, no text layer; Table 2 verified by eye (p. 777) |
| parr2022 | parr2022.pdf | f1ddb2efaf4f86f9 | 313 | https://www.math4wisdom.com/files/2022_Textbook-ActiveInference.pdf (mirror of the MIT Press open-access PDF `book_9780262369978.pdf`; located by wm-evidence, C453) | MIT Press OA (CC BY-NC-ND); direct.mit.edu and OAPEN refused automated fetch |

Extraction: `pdftotext -layout <pdf> <txt>` (and `-raw` for the two-column
Friston 2016). Re-verify a file with `sha256sum`.
