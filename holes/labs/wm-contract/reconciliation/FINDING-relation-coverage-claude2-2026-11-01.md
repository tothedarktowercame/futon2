# Finding: the relation producer covers 92 of 626 open tasks (claude-2, 2026-11-01 15:45Z)

Context. L5-4d-3's replay of the restored selection on the retained 11-01 field refused
`:tie-at-minimum` with all three cascades at G 4.7875, the abstention value: every pick's
relation read `:unknown`, so every pick landed on the stopped kernel. That replay called
`focus-receipt/classify-target` with four arguments. The producer derives an M- target's
relation (stated-relation hops, then the embedding neighbour) only when a relation context is
passed (`focus_receipt.clj`, `m-derivation`, "when a relation context is given (the scoring
path's)"); the live scoring path passes one (`outer_observation.clj:205-213`, `:relation-context`).
With the context, the two picks read `:associated`, derived through the embedding neighbour
`M-action-cost-modelling` (a codex-12 hand-reviewed row of 2026-09-21). The replay's refusal was
therefore an artefact of the call, not the field; the witness's relation classes stand and now
disclose the derivation.

Census, same producer, same context, focus retained "WM", as-of 2026-11-01T06:02Z, over the 626
open ids pinned in `witnesses/witnesses-open-tasks-2026-11-01.edn`:

| Prefix | Class | Source | Count |
|---|---|---|---|
| M- | focus | declared row | 5 |
| M- | associated | declared row | 3 |
| M- | associated | embedding neighbour | 84 |
| M- | unknown | `:embedding-node-not-retained` | 89 |
| E- | unknown | `:relation-not-declared` (no derivation for E-) | 255 |
| T- | unknown | `:no-parent-relation` | 190 |

So 92 tasks (14.7%) have a measured relation; 534 do not. Under the restored criteria a pick on
an unknown relation fails Q7 (relation absent on the path), lands on the stopped kernel and
fails Q11 (G equals abstention, prediction equals stopping). The field is the whole open set
(ruling 4), but only 92 of its members can be a certified pick until the relation producer
covers excursions, tickets and the 89 missions absent from the structure embedding. This is a
producer gap, not a reason to shrink the field; the declared-row corpus is 9 rows.

Open for Joe: whether an embedding-derived relation counts as measured (it is derived from
pinned embeddings and a hand-reviewed row, and the receipt records `:derived-via`), and how
relations for E- and T- targets are to be produced (ticket parent exists for T- but found no
parent relation for any of the 190; nothing derives for E-).

## Update 2026-11-01 15:40Z, after L5-4g (codex-5, futon2 `399657ec2`, merged)

Same census, same context, derivation extended to E- (stated hops, embedding) and T- (through the
parent, chain recorded), embedding floor declared at 0.0:

| Prefix | Class | Source | Count |
|---|---|---|---|
| M- | focus / associated | declared row | 5 / 3 |
| M- | associated | embedding neighbour | 84 |
| M- | unknown | `:embedding-node-not-retained` | 89 |
| E- | associated | embedding neighbour | 1 |
| E- | unknown | `:embedding-node-not-retained` | 254 |
| T- | focus / associated | ticket parent | 2 / 1 |
| T- | associated | open repair obligation | 40 |
| T- | unknown | `:open-repair-obligation` (repaired target unclassified) | 110 |
| T- | unknown | `:no-parent-relation` | 33 |
| T- | unknown | ticket parent unclassified | 4 |

136 of 626 classified (21.7%, from 14.7%). The remaining gap is now one thing: 343 tasks (254 E-,
89 M-) are not nodes of the structure embedding the inputs pin
(`futon6/data/mission-structure-embed/*`, files dated 2026-06-25), and 110 tickets inherit an
unclassified repaired target, most of which are those same unembedded tasks. The lever is the
embedding's coverage: regenerate the structure embedding over the current open set (a futon6 data
producer, with its pins updated in the focus inputs), not more derivation rules. The floor is
declared at 0.0 pending Joe's value; at 0.1 the two November picks (cosines −0.014 and 0.073) read
unknown, so the value decides how many of the 84 + 1 embedding-derived relations survive.

## Update 2026-11-01 16:25Z, after L5-4i (codex-5 discovery, futon2 `6cf534346`)

The pinned structure embedding is a 2026-06-09 snapshot of 198 substrate-2 mission scopes with
structural features (phases, wholeness class, binder histogram, applied patterns), not an embedding
of the repositories' holes; no excursion or ticket was ever a node, and 78 of the 94 absent open
missions were created after it. There is no command that regenerates it over the current open set,
and a regenerated one would still inherit from the same nine hand-reviewed rows (three of them
nodes today). Regeneration is therefore not the lever.

The lever is already pinned per run. Giraffe's `input.edn` for the 11-01 run carries 96 declared
dependency edges and 891 reading-evidence links (kinds: relates-to 650, part-of 137, advances 97,
superseded-by 7; bases: declared 793, inferred 98), each with an evidence pin (file, line, quote).
Reach from the nine anchors over that graph, undirected:

| Hops | Open tasks reached | By prefix (3 hops) |
|---|---|---|
| 1 | 29 | |
| 2 | 93 | |
| 3 | 202 | M- 97, E- 91, T- 14 |
| 5 | 345 | |
| any edge or link | 402 of 626 | |

Theory: relation-to-focus is an observation; a hop path through pinned, evidenced links from a
declared anchor is a measurement with recorded provenance, the same shape as the producer's
existing stated-relation hops over task text (`relation-hop-bound`), now over the field-wide graph
Giraffe already reads. Parameters to declare in the focus inputs, not default: the hop bound, the
admitted link kinds, the admitted bases (declared only, first). Reliability by distance belongs in
the ambiguity term later, as with the embedding cosine. Implementation: L5-4j (codex-5).
