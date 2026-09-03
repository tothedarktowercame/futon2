# Tensions as proto-patterns — design note

claude-1, 2026-09-03, at Joe's ask ("patterns as tensions... seems very
worthwhile to get written down with some focus. I'm pretty sure we developed
the core ideas in this session"). Confirmed: the core is the **Carried
tensions** block of M-zaif-harness-v1.md:35-40, from Joe's dictated punch-in
of 2026-09-02 ("operator dictation in session, 2026-09-02, three exchanges"),
plus U39(c)'s just-minted machine-side half (refuted selection rationales
mint tension records).

## 1. The live instance (all that exists today)

M-zaif-harness-v1.md HEAD names three tensions, numbered, each with poles
and a resolution path:

1. arms→cascades deferred to LA2/LA3 — poles: build-now vs defer; the U5
   opaque-candidate seam is the recorded *promise that deferral is cheap*.
2. the ask arm structurally unreachable at shipped constants — poles:
   retune-now vs measure-first; Z3's preregistration-gated A/B is the
   sanctioned resolution.
3. lifecycle→node mapping: formal claim vs fertile analogy — carried
   deliberately, unresolved *on purpose*.

Already load-bearing: the mission doc's own §1 and the zaif board's U10
matrix row both cite "carried tension 3" as a premise ("treat as fertile
until a row cashes it"). So tensions are referenced-by-number working
objects — but they are prose, unqueryable, statusless.

## 1.5 Verbatim origins (recovered from the evidence store, 2026-09-03)

**The buffer moment Joe pointed at** (this session, turn 143,
2026-09-02T07:50Z, retrieval record e-aa3dd403-81c0-4c10-b185-4b92525316f2;
Joe's turn verbatim):

> "yes, exactly ... indeed, the tension could be recorded as a new design
> pattern (even if just partial or in draft) which would turn the refusals
> into the source of new design knowledge!"

Two things that moment carries beyond §2's claim: (i) **refusals as the
mint** — the machine's refusals (and, per U39, its refuted selection
rationales) are the *source* of new design knowledge, not noise to
suppress; (ii) **partial/draft is fine** — a tension-pattern needs no
finished PLoP form to be worth recording.

**The retrieval system answered that turn by surfacing patterns that
already exist** — the library is ahead of this note:
`problems/tension-proposes-candidates` ("Tension Proposes Candidates",
rank 1), `p4ng/tension-detection` ("Tension Detection", rank 2),
`ukrns/design-as-function-of-evidence` (rank 3), all in futon3a. U41's
retrofit must LINK to these entities rather than mint duplicates.

**Deeper origins, same store**: 2026-06-05 (Joe, e-a9b20f38...): tensions
enter the constellation "via a pointer to a *design pattern*" — a peer,
not an interest-star — and the same day claude-6's grounding "a flexiarg
pattern is *already* a structured tension"; one of the ratified "strong 6"
was a tension→pattern. 2026-05-11 (claude-3, e-98825174...): "real pattern
mining *from* the existing missions" with the PLoP method paper as
adaptation target. The idea has been converging from three directions for
four months; this note is where it gets one schema.

## 2. The claim: a tension is a pattern before it has earned its name

In the paper's own terms (pattern ≡ policy at every grain, §1c; PLoP form),
a pattern's **forces** section IS a tension: two poles that both pull, named
honestly, with the resolution the pattern crystallizes. A carried tension is
therefore a **proto-pattern**: forces named, resolution not yet earned.
The birth rule: when the *same* tension is resolved the *same* way in
enough distinct contexts (≥2 with typed receipts), that recurrence is a
pattern candidate for the futon4 pattern class — written from evidence,
never authored from taste. The tension ledger is the pattern nursery.

## 3. The typed record (proposal — U41 implements)

    {:tension/id :zaif-v1/T3
     :tension/poles ["formal claim" "fertile analogy"]
     :tension/statement "..."                      ; verbatim from the doc
     :tension/carried-by "M-zaif-harness-v1"       ; mission/excursion id
     :tension/resolution-path "..."                ; named at carry time, may be nil
     :tension/status :carried | :cashed | :refuted | :dissolved
     :tension/cashed-by {:row :U10 :evidence "..."} ; when status moves
     :tension/born-of :operator-dictation | :refuted-rationale | :build-event
     :tension/provenance {...}}                    ; who, when, record-ids

Lifecycle events are typed records too: carrying, cashing, refuting each
appends evidence (never edits the tension in place — the D10/D11 dated
pattern). Status moves only with a pointer to the cashing/refuting row.

## 4. Sources that mint tensions (three, two of which already exist)

- **Operator dictation** at mission authoring (the live instance).
- **Refuted selection rationales** (U39(c) — the machine's own regrets).
- **Build events**: a review finding that reveals two goods in conflict
  (e.g. today's "distinct-count headline vs noise-fakeability" from the
  U11 review) — authored by the reviewer at finding time, cheap.

## 5. Consumers (why typing pays)

- The catalog reader (U23) adds tensions as a carrier — kin missions
  sharing a tension is precisely the recurrence the birth rule watches for.
- U36's runtime-validation catalog can list "tensions cashed per run".
- The futon4 pattern class receives the graduated ones with their receipts
  as the forces-evidence a PLoP writeup wants.
- Joe's review surface: "what is the machine carrying, and what has it
  learned" becomes a query, not an archaeology session (this note exists
  because it took a grep chain to answer where the tensions live).

## 6. Not in scope

No retro-conversion of every old doc; no automatic tension mining from
prose; no pattern auto-authoring (the birth rule proposes, a person or a
reviewed row writes). The three zaif-v1 tensions are the seed corpus.
