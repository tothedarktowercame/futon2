#!/usr/bin/env python3
"""embedding_probe.py -- what the pattern tagger is actually reading.

Runs the shipped retrieval geometry (MiniLM all-MiniLM-L6-v2, cosine against
1408 precomputed pattern vectors, top-3, no score floor) over one corpus of
operator turns under five input conditions, and reports how much the tags move.

    /home/joe/code/futon3a/.venv/bin/python3 embedding_probe.py \
        --turns /tmp/joe-aug.edn --out probe.json

Conditions
  prefix200  text[:200]            -- the operator half of the shipped input
  full       whole turn            -- what Joe asked about (model still caps
                                      at 256 word-pieces)
  nostop     whole turn, stopwords removed -- Joe's proposal
  shuffled   whole turn, words shuffled    -- semantic null: same vocabulary,
                                      no word order
  random     random unit vectors    -- geometry null: no text at all

Read only: loads the embeddings file and the model, writes one JSON file.
Deterministic: fixed seed for shuffled/random.
"""
from __future__ import annotations

import argparse, json, math, random, re, sys
from pathlib import Path
import numpy as np

STOPWORDS = set("""
a about above after again against all am an and any are aren't as at be because been before being
below between both but by can cannot could couldn't did didn't do does doesn't doing don't down
during each few for from further had hadn't has hasn't have haven't having he he'd he'll he's her
here here's hers herself him himself his how how's i i'd i'll i'm i've if in into is isn't it it's
its itself let's me more most mustn't my myself no nor not of off on once only or other ought our
ours ourselves out over own same shan't she she'd she'll she's should shouldn't so some such than
that that's the their theirs them themselves then there there's these they they'd they'll they're
they've this those through to too under until up very was wasn't we we'd we'll we're we've were
weren't what what's when when's where where's which while who who's whom why why's with won't would
wouldn't you you'd you'll you're you've your yours yourself yourselves
just really kind sort like well okay ok yeah maybe think guess bit lot thing things stuff
""".split())

WORD = re.compile(r"[A-Za-z0-9'_/-]+")


def read_turns(path: Path, limit: int) -> list[str]:
    """Pull :evidence/body {:text ...} strings out of an EDN evidence page.

    Parsed with a regex rather than an EDN reader on purpose: this file is a
    store response, and the only field the probe needs is the turn text.
    """
    raw = path.read_text()
    out = []
    for m in re.finditer(r':evidence/body \{:event "chat-turn"(.*?)\}, :evidence/session-id', raw, re.S):
        seg = m.group(1)
        t = re.search(r':text "((?:[^"\\]|\\.)*)"', seg, re.S)
        if not t:
            continue
        s = t.group(1).encode().decode('unicode_escape', errors='replace')
        if len(s) >= 40:
            out.append(s)
        if len(out) >= limit:
            break
    return out


def strip_stopwords(text: str) -> str:
    return " ".join(w for w in WORD.findall(text) if w.lower() not in STOPWORDS)


def shuffle_words(text: str, rng: random.Random) -> str:
    ws = WORD.findall(text)
    rng.shuffle(ws)
    return " ".join(ws)


def main() -> None:
    ap = argparse.ArgumentParser()
    ap.add_argument("--turns", required=True)
    ap.add_argument("--embeddings",
                    default="/home/joe/code/futon3a/resources/notions/minilm_pattern_embeddings.json")
    ap.add_argument("--model", default="sentence-transformers/all-MiniLM-L6-v2")
    ap.add_argument("--limit", type=int, default=1000)
    ap.add_argument("--top", type=int, default=3)
    ap.add_argument("--out", required=True)
    args = ap.parse_args()

    rng = random.Random(20260904)
    np_rng = np.random.default_rng(20260904)

    turns = read_turns(Path(args.turns), args.limit)
    if not turns:
        sys.exit("no turns parsed")

    entries = json.loads(Path(args.embeddings).read_text())
    ids = [e["id"] for e in entries]
    M = np.array([e["vector"] for e in entries], dtype=np.float32)
    M /= np.linalg.norm(M, axis=1, keepdims=True)

    from sentence_transformers import SentenceTransformer
    model = SentenceTransformer(args.model)
    max_tokens = model.max_seq_length
    tok = model.tokenizer

    conditions = {
        "prefix200": [t[:200] for t in turns],
        "full": turns,
        "nostop": [strip_stopwords(t) for t in turns],
        "shuffled": [shuffle_words(t, rng) for t in turns],
    }

    def topk(Q):
        Q = Q / np.linalg.norm(Q, axis=1, keepdims=True)
        S = Q @ M.T
        idx = np.argpartition(-S, args.top, axis=1)[:, :args.top]
        rows = []
        for r, cols in enumerate(idx):
            cols = cols[np.argsort(-S[r, cols])]
            rows.append([(ids[c], float(S[r, c])) for c in cols])
        return rows

    results = {}
    for name, texts in conditions.items():
        Q = model.encode(texts, normalize_embeddings=True, batch_size=64,
                         show_progress_bar=False)
        results[name] = topk(np.asarray(Q, dtype=np.float32))

    # geometry null: random unit vectors, no text
    R = np_rng.normal(size=(len(turns), M.shape[1])).astype(np.float32)
    results["random"] = topk(R)

    # ---- metrics -------------------------------------------------------
    def distinct(rows):   return len({p for row in rows for p, _ in row})
    def distinct1(rows):  return len({row[0][0] for row in rows})
    def meantop1(rows):   return sum(row[0][1] for row in rows) / len(rows)

    def agree(a, b):
        """mean Jaccard of the top-k id sets, and top-1 exact agreement"""
        js, t1 = [], 0
        for ra, rb in zip(a, b):
            sa, sb = {p for p, _ in ra}, {p for p, _ in rb}
            js.append(len(sa & sb) / len(sa | sb))
            t1 += int(ra[0][0] == rb[0][0])
        return sum(js) / len(js), t1 / len(a)

    tok_lens = [len(tok(t, add_special_tokens=True)["input_ids"]) for t in turns]
    pfx_cov = [min(200, len(t)) / len(t) for t in turns]

    summary = {
        "corpus": {
            "turns": len(turns),
            "chars": {"min": min(map(len, turns)), "median": int(np.median([len(t) for t in turns])),
                      "max": max(map(len, turns)), "mean": float(np.mean([len(t) for t in turns]))},
            "wordpiece_tokens_full_turn": {
                "median": int(np.median(tok_lens)), "max": int(max(tok_lens)),
                "over_model_cap": int(sum(1 for n in tok_lens if n > max_tokens)),
                "model_max_seq_length": int(max_tokens)},
            "fraction_of_turn_inside_the_200_char_prefix": {
                "median": round(float(np.median(pfx_cov)), 4),
                "mean": round(float(np.mean(pfx_cov)), 4),
                "turns_fully_covered": int(sum(1 for c in pfx_cov if c >= 1.0))},
        },
        "library": {"patterns": len(ids), "dim": int(M.shape[1]), "top_k": args.top,
                    "score_floor": None},
        "per_condition": {
            name: {"distinct_patterns_top3": distinct(rows),
                   "distinct_patterns_top1": distinct1(rows),
                   "coverage_of_library_top3": round(distinct(rows) / len(ids), 4),
                   "mean_top1_score": round(meantop1(rows), 4)}
            for name, rows in results.items()},
        "agreement_vs_prefix200": {
            name: dict(zip(("mean_jaccard_top3", "top1_exact"),
                           map(lambda x: round(x, 4), agree(results["prefix200"], rows))))
            for name, rows in results.items() if name != "prefix200"},
        "agreement_full_vs_nostop": dict(zip(("mean_jaccard_top3", "top1_exact"),
                                             map(lambda x: round(x, 4),
                                                 agree(results["full"], results["nostop"])))),
        "agreement_full_vs_shuffled": dict(zip(("mean_jaccard_top3", "top1_exact"),
                                               map(lambda x: round(x, 4),
                                                   agree(results["full"], results["shuffled"])))),
        "most_activated_top3": {
            name: sorted(
                ((p, sum(1 for row in rows for q, _ in row if q == p))
                 for p in {q for row in rows for q, _ in row}),
                key=lambda kv: -kv[1])[:12]
            for name, rows in results.items()},
    }

    Path(args.out).write_text(json.dumps(summary, indent=2, sort_keys=True))
    print(json.dumps(summary["per_condition"], indent=2))
    print("wrote", args.out)


if __name__ == "__main__":
    main()
