# What the pattern tagger is actually reading

**Excursion:** `holes/E-operator-as-attached-agent.md`. **Run by:** claude-10,
2026-09-04, from Joe's questions on the stopword idea and on why library
coverage looks good under a naive method. **Read only.**

Producers: `embedding_probe.py` (the experiment), `harvest_activations.bb`
(the activation harvest). Results: `probe.json`, `activations.edn`.

---

## 1. The pipeline, read off the code

`futon3c/dev/futon3c/dev.clj`:

```clojure
;; context-retrieval! (:998-1045)
user-msg         (extract-user-message prompt-str)            ; :826-833
response-preview (subs response-text 0 (min 200 ...))
proto-text       (str (subs user-msg 0 (min 200 ...)) " " response-preview)
results          (run-futon3a-search proto-text)              ; :835-864

;; emit-context-evidence! (:951-969)
"query" (subs proto-text 0 (min 100 (count proto-text)))
```

`run-futon3a-search` shells out to `futon3a/scripts/notions_search.py` with
`--top 3`, which loads `sentence-transformers/all-MiniLM-L6-v2`, encodes the
query, and takes cosine against
`futon3a/resources/notions/minilm_pattern_embeddings.json` — **1408 patterns,
384 dimensions**. There is **no score floor**: every turn tags exactly three
patterns whatever the scores are.

Four things follow, and the first one corrects what census run 1 reported.

**(a) The store does not record the embedding input.** Up to 401 characters are
embedded (200 of the turn + a space + 200 of the reply); 100 characters are
stored. You cannot reconstruct from an evidence record what produced its tags.
Census run 1 read the 100-character record as the embedder's input and reported
"three characters of Joe's words reach the embedder" on the codex surface. The
true figure is ~103 (see (c)); three characters is what reached the *record*.

**(b) Half the input is the agent's reply, by design.** The comment calls
`proto-text` the "A->B protopattern". The tagged object is the exchange, not the
operator's ask — which matches Joe's 2026-09-04T06:12 dictation, *"it's all A,
B exchanges where the A is the Operator."* A tag that reads like the reply is
the instrument working as built.

**(c) The codex surface loses about half the operator's 200-character budget.**
`extract-user-message` cuts at the literal marker `"\nUser message:\n"`. Claude
surfaces emit it, so their header is stripped. The codex bell envelope
(`--- CURRENT TURN --- … ---`, 97 characters) carries no such marker and is not
stripped, so ~103 characters of Joe survive instead of 200. This is an
envelope-format mismatch in one function, not a design choice.

**(d) No stopword handling anywhere**, in either the query path or the
pattern-embedding path — which is the question Joe raised. See §3.

### Has it changed silently?

Partly, and less than feared. `git log -L` over `context-retrieval!` in
futon3c gives six touches since 2026-02-20, the last on **2026-07-13**
("Harden federation backoff and context notification"); the 200/200/100
arithmetic is not among the things those commits moved. The embeddings *file*
is a different story: it was moved out of the futon3a repo on 2026-06-15
("Move generated notions data out of the repo to ~/code/data/notions") and is
now regenerated untracked by `futon3a/scripts/index_patterns.sh` — its mtime
today is 2026-09-04 05:33. So the model and the query arithmetic have been
stable for months, while **the pattern corpus it ranks against is rebuilt
regularly with no version recorded in the retrieval record**. A record from
June and a record from today may have been ranked against different libraries
and there is nothing in either record that says so.

That is the honest answer to "maybe it changed silently": the *method* did not,
the *corpus* does, and neither is stamped.

### One stale pointer

The excursion's pilot receipts cite `GET /api/alpha/patterns/activation` as the
server-side aggregation. That route does not exist on futon1b (:7073) or on
futon3c (:7070) — both return 404. It was a futon1a route. `harvest_activations.bb`
pages `GET /api/alpha/evidence?tags=context-retrieval` instead.

---

## 2. The coverage question, with a null control

Joe: *"it's just kind of impressive that we have reasonably good coverage of
almost all of the patterns… even though the embedding approach has apparently
been quite naive."*

`embedding_probe.py` runs the shipped geometry over **563 operator turns**
(author `joe`, `chat-turn`, since 2026-08-01, ≥40 chars) under five input
conditions and counts how much of the 1408-pattern library the top-3 touches.

| condition | input | distinct patterns in top-3 | share of library | mean top-1 score |
|---|---|---|---|---|
| `prefix200` | `text[:200]` — the operator half of the shipped input | 490 | 34.8% | 0.420 |
| `full` | whole turn | 461 | 32.7% | 0.454 |
| `nostop` | whole turn, stopwords removed | 431 | 30.6% | 0.452 |
| `shuffled` | whole turn, **words shuffled** | 469 | 33.3% | 0.458 |
| `random` | **random unit vectors, no text at all** | **822** | **58.4%** | 0.146 |

**Coverage is not evidence that the tagger is working.** Random noise vectors
reach 58% of the library; real operator turns reach 35%. Under uniform sampling
1689 draws from 1408 patterns would be expected to touch about 984 of them
(~70%), so even random directions under-cover — the pattern cloud has hubs. Real
text under-covers *further*, because real turns occupy a narrow region of MiniLM
space and keep landing on the same few hundred patterns.

So the impressive-looking coverage comes from taking top-3 with no score floor,
repeatedly, over a long history. Any input distribution would accumulate broad
coverage that way. The question worth asking instead is whether the tags are
*right*, and coverage cannot answer it.

**Cumulative coverage over the whole store** is in `activations.edn`
(`harvest_activations.bb`, every `context-retrieval` record paged out of the
store). Read the ratio there against 1408, and against the 822 that random
vectors reach on 563 draws, before treating a high number as a quality signal.

### The tags do carry some signal

Shuffling every word in a turn changes the top-1 pattern **77% of the time**
(top-1 agreement `full` vs `shuffled` = 0.226; mean Jaccard over top-3 = 0.203).
So word order matters and the tagger is not a bag of words.

But: **the score does not tell you whether the input meant anything.** Shuffled
word salad scores *higher* on top-1 than real text (0.458 vs 0.454). Random
vectors score 0.146, so the score separates "not text" from "text" and does not
separate "meaningful text" from "scrambled text". There is therefore no
threshold that would filter junk tags — which is the practical reason the
`--top 3`-with-no-floor design has never visibly failed.

---

## 3. Stopwords: what the probe can and cannot say

Joe's proposal was to take the entire turn, drop stopwords, and embed that.
Measured against the shipped input:

- `nostop` vs `full`: top-1 agreement **0.300**, mean Jaccard **0.258** — it
  changes the answer on most turns.
- `prefix200` vs `full`: top-1 agreement **0.332** — so moving from the 200-char
  prefix to the whole turn *also* changes most answers, by about as much.
- Coverage and mean score barely move: 431 vs 461 patterns, 0.452 vs 0.454.

**The probe can show that stopword removal moves the tags a lot. It cannot show
that it moves them toward better,** because there is no labelled answer to score
against. Reporting "it changes 70% of the tags" as an improvement would be the
mistake this excursion exists to avoid.

Two things worth knowing before spending on it:

- **MiniLM is not a bag-of-words model.** Stopword lists earn their keep against
  TF-IDF and similar, where function words dominate the count vector.
  Sentence-transformers are trained on ordinary sentences and their attention
  already discounts function words; feeding them de-stopworded text moves the
  input off the training distribution. The measured result here — same score,
  slightly *lower* coverage, most tags changed — is consistent with "different,
  not better".
- **"The whole turn" is not available anyway.** MiniLM's `max_seq_length` is
  **256 word-pieces**. Over the 563-turn corpus the median turn is **289**
  word-pieces and **292 turns (52%) exceed the cap**; the longest is 7321. The
  200-character prefix currently covers a **median of 18%** of a turn (only 116
  of 563 turns fit inside it). Removing the 200-char cap would raise that to
  roughly the first 1000-1200 characters and no further — the model truncates
  the rest silently. Embedding a whole long dictation needs chunk-and-pool, or a
  longer-context embedder, not just a bigger `subs`.

### The cheap way to settle it

Label ~50 turns with the pattern a reader thinks applies — Joe, or an agent with
Joe adjudicating a sample — and score the conditions against that. Fifty labels
is enough to separate "no better" from "clearly better" at these effect sizes,
and it is the only thing that turns this from a change-detector into a decision.
Until then the honest statement is: the current tags are tags on the first ~200
characters of the turn plus the first 200 of the reply, and nobody has checked
whether they are the right tags.

---

## 4. The two-vocabulary cross, run on a lifecycle window

Census run 1 could not run Joe's cross because the ruling window contains no
mission-phase words. `M-vsatarcs-invariants-integration` (futon4, minted
2026-06-01) supplies the contrast: IDENTIFY through INSTANTIATE in a single
day, Joe framing, with a compact clocking run inside it.

**2026-06-01: 154 operator turns, 16 carrying a phase word (10%)** — against
0 of 18 in the ruling window. Between 19:05 and 19:38 Joe walks one mission
through five phases in 33 minutes:

```
19:05:38  identify map   We created M-vsatarcs-invariants-integration.md …
19:08:05  map            Is MAP complete?
19:08:28  map            OK, please work through MAP
19:14:30  derive         OK, we can progress to DERIVE
19:18:16  derive         We also need to include in DERIVE …
19:19:38  argue          OK, we are ready for ARGUE
19:21:59  verify         Let's do VERIFY
19:24:03  verify inst.   codex-2 pushed the mission forward up to VERIFY — review before we INSTANTIATE?
19:26:40  map verify …   Review by Claude. The 100% VERIFY pass reminds me …
19:31:11  verify         the recent core.logic VERIFY is a crucial upgrade …
19:38:14  instantiate    I'd move the INSTANTIATE-0 items to INSTANTIATE-1 …
```

15 of the 16 phase-word turns have a retrieval record, all joinable on content.
`futon-theory/mission-interface-signature` fires on **10 of 15**, and on 7 of
the 8 turns in the clocking run. That is the shape Joe hoped for — one pattern
accumulating on the turns where the operator moves the agent between sections.

**The control says don't believe it yet.** That same pattern fires on **73 of
all 232 retrieval records that day (31.5%)**, including **30 of codex-3's 35
(86%)** — an agent not in the clocking run at all. Enrichment on phase-word
turns is real (67% vs a 31.5% base rate) but the whole day is saturated:
everyone was working on missions, and half of every embedding input is the
agent's reply, which on that day was mission-document text. So the cross as run
detects **the day's topic at session granularity**, not the operator's clocking
act at turn granularity.

What would separate them: score the same pattern against operator turns from a
day with comparable mission traffic but no phase words, and against non-operator
turns inside the clocking window. Both are cheap and neither has been done.

The 19:38:14 turn — `e-66dc0a39`, the excursion's own usage-verification
exemplar — has **no retrieval record** among the 232 fetched for that day.

---

## 5. What to do next, in the order that pays

1. **Stamp the record.** Add the pattern-library version (a hash of the
   embeddings file) and the full embedding input, or its hash, to the
   `context-retrieval` body. Without this, no census of the tagging layer is
   reproducible against a corpus that is rebuilt untracked.
2. **Fix the envelope strip.** `extract-user-message` should recognise the bell
   envelope terminator as well as `"\nUser message:\n"`. One function, and it
   returns ~97 characters of operator budget on every codex-surface turn.
3. **Get 50 labels** before changing the query construction at all (§3).
4. **Then** decide between: raise the 200-char cap toward the model's 256-token
   ceiling; chunk-and-pool long turns; embed the operator's half separately from
   the reply so the two can be told apart; or leave it. Stopword removal is the
   weakest of these on the evidence here.
