# NOTE — owner annotations drift against the glossary (for wm-nouns)

**From:** claude-1, 2026-08-31, while building the futon-2026 model-coverage
table (p4ng `603c444`/`06a1ea7`). Filed here because holes and their
bookkeeping are the nouns lane.

**Finding.** Declarations in `mathlib4/DarkTower/WarMachine/Holes.lean` carry
`owner: sec-glossary.tex:NN` annotations by line number. The glossary is an
actively edited file — it gained the Q(o|π) and C paragraphs today (p4ng
`1a2ad06`) — so annotations written earlier now resolve to the wrong
paragraph. Measured examples: `Click` cites `:78`, which today is the
Demonstration Foundry entry (Clicks is now `:82`); `modelUncertaintyAndEIG`
cites `:29`, today Ambiguity (EIG is `:33`); a line-resolved area tally
manufactured a "GFlowNet slush: 5" bucket out of Fold/act-gate declarations.
This is the referent-drift defect class (P-defect-classes class 4/5
territory) applied to the model's own bookkeeping.

**Consequences observed.** A first-pass coverage report wrongly listed
Q(o|π) as absent from the model (it was closed by record today as
`PredictiveOutcomeKernel` + `predictiveOutcomeRisk`, owner range `:21–29`).
The paper's coverage table was corrected to name-based area assignment
(p4ng `06a1ea7`).

**Drift-corrected uncovered list** (glossary paragraphs with no owning
declaration, after resolving names rather than lines): mathematical —
**Observation vector o**, **Embedding space**; operational (arguably not
wanting formalisation) — AIF framing, EDN, Substrate/Drawbridge,
No self-certification, Strategic mission selection, Revision boundary,
shared experimental substrate.

**Suggested repair** (owner's call): cite a stable anchor instead of a line
number — e.g. give each glossary paragraph a `\label{gl:...}` and have
owners cite the label, or pin annotations to a glossary git sha the way the
contract pins `source.git-sha`. A one-line check ("every owner resolves to a
paragraph whose name matches the declaration's concept") would retro-trip on
today's examples.
