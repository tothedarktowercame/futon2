# L11 — iiching/iching triage (lab note, discovery only)

Row `:L11`, class `:M`, DISCOVERY ONLY: no annotations written, no library
file modified. Measured at futon3 `5704359975a41ff125ffa94f7ce227dcf5d53217`.

## What these 321 patterns are

- **`iiching/` — 257 files**: 256 `exotype-NNN` encoding records (0x00–0xFF)
  plus `TEMPLATE.flexiarg`. Per the section's own README: "This directory
  encodes the 256 8-bit exotypes as explicit, named patterns. The goal is to
  make exotype knowledge concrete and inspectable, separate from the theory in
  `library/exotic`." Bodies are thin reference records (encoding fields, and
  — where populated — `@exotype-program`/`@exotype-lift`/`@exotype-params`
  transcriptions from the futon5 manifest and lift registry). The rationale is
  the README's goal sentence, shared by construction.
- **`iching/` — 64 files**: one per hexagram, full five-component flexiargs
  with distinct conclusions, evidence lines quoting the traditional Judgment,
  and CT/improvisation framing (`@audience improvisers, system designers, CT
  practitioners`). Each has an individual rationale in the philosophical
  register, but none names a problem in the recorded problem corpus.

**Corpus probe (the row's key measurement).** With the grounding corpus
extended to its widest sensible span — the 32 `problems/*` nodes, all 37
`futon-theory/*` patterns, and all 11 `exotic/*` patterns (80 nodes) — the
L6/L8 checker (`l6_no_source_check.py iiching,iching <lib> <out> problems,futon-theory,exotic`)
finds **0 matches for all 321 patterns**: no problem node, futon-theory
pattern, or exotic pattern holds or names any iiching/iching pattern by token
or text. There is no mechanical grounding source today.

## Five sampled patterns (quoted)

1. `iiching/exotype-000` — `! conclusion: Exotype 000 is encoded; program/params
   mapped from futon5; lift registry evidence recorded.` — a reference record;
   its HOWEVER is `The lift registry records pattern bindings but no
   lift-rules.`
2. `iiching/exotype-167` — carries a full `@exotype-lift` block whose
   `:pattern-ids` list binds 13 real patterns (`meta/layering-design-into-argument`,
   `or/bridge-before-portal`, …) to this exotype — the section's only outward
   edges, and they are lift-registry transcriptions, not rationales.
3. `iching/hexagram-01-qian` — `! conclusion: Pure creative force initiates
   but does not complete; it requires The Receptive (坤) to manifest form.`
   HOWEVER: `Pure yang without yin is unstable. "The dragon that flies too
   high has cause for repent."`
4. `iching/hexagram-02-kun` (sampled for contrast) — `! conclusion: Pure
   receptive force sustains and completes but does not initiate; it requires
   The Creative (乾) to provide form.` — the receptive complement; same
   register, distinct conclusion and exit condition.
5. `iiching/TEMPLATE` — the section's own template file, counted by the census
   as a pattern (`iiching/TEMPLATE`): placeholder title `Exotype XYZ (0xYY)`,
   bits `00000000` — an artifact of the section's minting workflow, not a
   pattern with content.

## Options and cost

- **(a) Pattern-by-pattern backfill (321 annotations).** Cost: one more
  backfill pass + receipt + review, mechanically identical to L6–L10. Value:
  for `iching` (64), each annotation would be a no-source note (the corpus
  probe returns zero), so the cost buys no graph movement; for `iiching` (256)
  it would stamp the same sentence 256 times — 256 near-identical no-source
  notes that dilute the signal the no-source receipts exist to carry.
- **(b) Section-grain shared rationale (2 statements).** Cost: minimal — a
  statement per section (the `iiching` README already *is* one; `iching` needs
  one sentence naming the shared improvisation/CT frame), plus a census-side
  convention for section-grain rationale. Value: honest — `iiching`'s
  rationale genuinely is shared; but it does not create `@why` edges and so
  does not move reachability either.
- **(c) Defer until a grounding source exists.** Cost: zero now; the corpus
  probe already documents the absence reproducibly.

## Recommendation (with its own reversal)

Recommend **(b) for `iiching` (section-grain, no per-file annotations)** and
**(c) for `iching` (defer)**: minting 321 no-source annotations would spend a
review cycle to record, 321 times, the one fact the probe receipt already
records once. Section-grain for `iiching` matches how that section's rationale
actually exists (its README goal sentence). Defer `iching` because its
patterns are genuinely individually-rationaled, so the right eventual form is
pattern-by-pattern `@why` — but only once a problem corpus names their
problems; writing it now would be invention.

**Reversal condition:** this recommendation reverses the moment a grounding
source appears — e.g. an `exotic`- or `futon-theory`-grain problem node that
names the exotype programme or the hexagram frame (the corpus probe turns
nonzero), or a ruling that section-grain rationale must be machine-readable
`@why` (then option (b) needs a carrier node, not a README sentence). It also
reverses if the `iiching/TEMPLATE` census artifact is ruled a defect (it is
currently counted as a pattern).

No annotations were written; no library file was modified by this row.
