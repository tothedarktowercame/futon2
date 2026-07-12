#!/usr/bin/env python3
"""
Mark-vocabulary clustering probe (M-points-de-fuite §6.5 follow-on;
C-futon1b-features §1a).

Pulls all joe-authored coordination turns from the futon1b store (:7073),
embeds them with MiniLM, clusters with KMeans over a small k-grid, and
outputs per-cluster descriptors for Joe to NAME.

DESIGN NOTES
------------
- Read-only API: GET only, no writes.
- Deterministic: fixed seed throughout.
- Checkpoint after the pull (turns-cache.json) and after embedding
  (embeddings-cache.npy) so a killed run resumes from cache.
- Runs from a DIFFERENT cwd (futon2/holes/labs/M-zaif-harness/) so it
  does not touch the live futon3c tree.
- EDN is parsed with regex field extraction (only 4 fields needed), not
  a full EDN reader — the API output is regular enough, and this avoids
  adding a dependency.
"""

import json
import os
import re
import sys
import time
import hashlib
from pathlib import Path

import numpy as np
import urllib.request

# ─── config ───────────────────────────────────────────────────────────
API_BASE = "http://127.0.0.1:7073/api/alpha/evidence"
PAGE_SIZE = 500
MODEL_NAME = "sentence-transformers/all-MiniLM-L6-v2"
SEED = 42
K_GRID = [8, 12, 16, 20]
MAX_SNIPPET_LEN = 120
TOP_TERMS = 10
N_REPR = 5

# paths (relative to this script)
SCRIPT_DIR = Path(__file__).resolve().parent
TURNS_CACHE = SCRIPT_DIR / "turns-cache.json"
EMB_CACHE = SCRIPT_DIR / "embeddings-cache.npy"
CLUSTERS_JSON = SCRIPT_DIR / "mark-vocab-clusters.json"
CLUSTERS_MD = SCRIPT_DIR / "mark-vocab-clusters.md"


# ─── phase 1: pull turns ──────────────────────────────────────────────

def _extract_quoted_field(chunk: str, field_marker: str) -> str:
    """
    Extract a quoted string value after a field marker like ':text'.
    Handles escaped quotes (\\") and backslashes within the EDN string.
    Returns the unescaped string value, or '' if not found.
    """
    idx = chunk.find(field_marker)
    if idx < 0:
        return ''
    # Find the opening quote after the marker
    q_start = chunk.find('"', idx + len(field_marker))
    if q_start < 0:
        return ''
    # Scan from q_start+1, handling escape sequences
    pos = q_start + 1
    chars = []
    while pos < len(chunk):
        c = chunk[pos]
        if c == '\\' and pos + 1 < len(chunk):
            # Escaped character — the EDN string uses \" for literal quotes,
            # \\ for literal backslashes, etc. We take the escaped char.
            next_c = chunk[pos + 1]
            if next_c == '"':
                chars.append('"')
            elif next_c == '\\':
                chars.append('\\')
            elif next_c == 'n':
                chars.append('\n')
            elif next_c == 't':
                chars.append('\t')
            elif next_c == 'r':
                chars.append('\r')
            else:
                # Unknown escape — keep both chars
                chars.append('\\')
                chars.append(next_c)
            pos += 2
            continue
        if c == '"':
            # Unescaped closing quote
            break
        chars.append(c)
        pos += 1
    return ''.join(chars)


def _extract_entries(edn_text: str) -> list[dict]:
    """
    Extract evidence entries from the EDN API response using regex.
    We split on top-level entry boundaries `{:evidence/` and extract the
    fields we need from each chunk.
    """
    # Split on entry boundaries — each entry starts with `{:evidence/`
    # The response shape is {:entries [ {...} {...} ] :count N}
    # We find all `{:evidence/in-reply-to` or `{:evidence/body` etc. starts
    # and split there.
    parts = re.split(r'(?=\{:evidence/[^}]*\{:evidence/)', edn_text)
    # Actually, let's use a simpler approach: find each entry by scanning
    # for :evidence/id and extracting the surrounding map.
    results = []
    # Pattern: find the text field, at, id, and clocked-mission within
    # reasonable proximity. Each entry is a single map in the vector.
    
    # Better approach: split the entries vector into individual maps.
    # Find `:entries [` then split on `} {:evidence/` boundaries.
    m = re.search(r':entries \[', edn_text)
    if not m:
        return results
    # Everything from after `:entries [` to the closing `]` before `:count`
    start = m.end()
    # Find the matching close — look for `] :count` or `]}` at top level
    end_m = re.search(r'\] :count', edn_text[start:])
    if end_m:
        entries_blob = edn_text[start:start + end_m.start()]
    else:
        entries_blob = edn_text[start:]
    
    # Split into individual entries. Each starts with `{` and the entries
    # are separated by `} {` — but nested maps make this tricky.
    # Robust approach: split on `} {:evidence/` which is the boundary
    # between consecutive entries in the vector.
    raw_entries = re.split(r'\} \{:evidence/', entries_blob)
    # First chunk starts with `{`, rest start after the split
    chunks = []
    for i, chunk in enumerate(raw_entries):
        if i == 0:
            chunks.append(chunk)
        else:
            chunks.append('{:evidence/' + chunk)
    
    for chunk in chunks:
        chunk = chunk.strip()
        if not chunk.startswith('{:evidence/'):
            continue
        entry = {}
        
        # Extract :evidence/id
        id_m = re.search(r':evidence/id "([^"]+)"', chunk)
        if id_m:
            entry['id'] = id_m.group(1)
        
        # Extract :evidence/at
        at_m = re.search(r':evidence/at "([^"]+)"', chunk)
        if at_m:
            entry['at'] = at_m.group(1)
        
        # Extract :text from the body map using a quote-aware scanner.
        # The text is delimited by `"..."` and may contain escaped quotes
        # written as `\"` in the EDN. We scan char-by-char from the `:text "`
        # marker, skipping escaped chars, until we hit the unescaped closing `"`.
        entry['text'] = _extract_quoted_field(chunk, ':text')
        
        # Extract :clocked-mission (optional)
        cm_m = re.search(r':clocked-mission "([^"]+)"', chunk)
        if cm_m:
            entry['clocked_mission'] = cm_m.group(1)
        else:
            # Try :mission-id
            mi_m = re.search(r':mission-id "([^"]+)"', chunk)
            if mi_m:
                entry['clocked_mission'] = mi_m.group(1)
            else:
                entry['clocked_mission'] = None
        
        # Extract :evidence/claim-type (optional, for reference)
        ct_m = re.search(r':evidence/claim-type :(\w+)', chunk)
        if ct_m:
            entry['claim_type'] = ct_m.group(1)
        
        if entry.get('id') and entry.get('text'):
            results.append(entry)
    
    return results


def pull_turns() -> list[dict]:
    """
    Page through all joe coordination evidence, dedup by id,
    checkpoint to turns-cache.json.
    """
    # Check cache first
    if TURNS_CACHE.exists():
        print(f"[cache] Loading turns from {TURNS_CACHE}")
        with open(TURNS_CACHE) as f:
            turns = json.load(f)
        print(f"[cache] {len(turns)} turns loaded from cache")
        return turns
    
    all_turns = {}  # id → entry, for dedup
    cursor = None  # ISO timestamp for before=
    page = 0
    
    while True:
        params = [f"author=joe", f"type=coordination", f"limit={PAGE_SIZE}"]
        if cursor:
            params.append(f"before={cursor}")
        url = f"{API_BASE}?" + "&".join(params)
        
        try:
            req = urllib.request.Request(url)
            with urllib.request.urlopen(req, timeout=30) as resp:
                edn_text = resp.read().decode('utf-8')
        except Exception as e:
            print(f"[ERROR] API request failed: {e}", file=sys.stderr)
            if all_turns:
                print(f"[checkpoint] Partial pull: {len(all_turns)} turns. Saving cache.")
                _save_turns_cache(all_turns)
            raise
        
        # Count raw entries from the API response (before text-filtering)
        # so the "last page" check is accurate — _extract_entries drops
        # entries without :text, which would make a full page look short.
        raw_ids = re.findall(r':evidence/id "([^"]+)"', edn_text)
        raw_count = len(raw_ids)
        # Also extract all :at values from raw EDN for pagination cursor
        raw_ats = re.findall(r':evidence/at "([^"]+)"', edn_text)
        
        entries = _extract_entries(edn_text)
        page += 1
        
        if not entries and raw_count == 0:
            print(f"[pull] Page {page}: 0 entries — done.")
            break
        
        new_count = 0
        oldest_at = None
        for e in entries:
            eid = e['id']
            if eid not in all_turns:
                all_turns[eid] = e
                new_count += 1
            at = e.get('at')
            if at and (oldest_at is None or at < oldest_at):
                oldest_at = at
        
        # Use raw_ats for the pagination cursor (more reliable than parsed)
        if raw_ats:
            raw_oldest = min(raw_ats)
            if oldest_at is None or raw_oldest < oldest_at:
                oldest_at = raw_oldest
        
        print(f"[pull] Page {page}: {raw_count} raw ({len(entries)} parsed, {new_count} new), total={len(all_turns)}, oldest={oldest_at}")
        
        if raw_count < PAGE_SIZE:
            print(f"[pull] Last page reached ({raw_count} < {PAGE_SIZE}).")
            break
        
        if new_count == 0:
            print(f"[pull] No new entries on this page — possible dedup exhaustion, stopping.")
            break
        
        cursor = oldest_at
        
        # Be gentle with the API
        time.sleep(0.1)
        
        # Checkpoint every 20 pages
        if page % 20 == 0:
            print(f"[checkpoint] Saving {len(all_turns)} turns at page {page}...")
            _save_turns_cache(all_turns)
    
    # Final checkpoint
    _save_turns_cache(all_turns)
    print(f"[pull] COMPLETE: {len(all_turns)} unique turns cached.")
    return list(all_turns.values())


def _save_turns_cache(turns: dict | list):
    """Save turns to cache file."""
    if isinstance(turns, dict):
        data = list(turns.values())
    else:
        data = turns
    # Sort by at for stable output
    data.sort(key=lambda e: e.get('at', ''))
    with open(TURNS_CACHE, 'w') as f:
        json.dump(data, f, indent=2, ensure_ascii=False)


# ─── phase 2: embed ────────────────────────────────────────────────────

def embed_turns(turns: list[dict]) -> np.ndarray:
    """
    Embed all turn texts with MiniLM (normalized).
    Checkpoint to embeddings-cache.npy.
    """
    if EMB_CACHE.exists():
        print(f"[cache] Loading embeddings from {EMB_CACHE}")
        emb = np.load(EMB_CACHE)
        if emb.shape[0] == len(turns):
            print(f"[cache] Shape matches ({emb.shape}). Using cached embeddings.")
            return emb
        else:
            print(f"[cache] Shape mismatch ({emb.shape[0]} vs {len(turns)} turns). Re-embedding.")
    
    from sentence_transformers import SentenceTransformer
    
    print(f"[embed] Loading model: {MODEL_NAME}")
    model = SentenceTransformer(MODEL_NAME)
    
    texts = [t['text'] for t in turns]
    print(f"[embed] Encoding {len(texts)} texts...")
    
    # Batch encode for efficiency
    embeddings = model.encode(
        texts,
        normalize_embeddings=True,
        show_progress_bar=True,
        batch_size=64,
        convert_to_numpy=True,
    )
    
    print(f"[embed] Done. Shape: {embeddings.shape}")
    
    # Checkpoint
    np.save(EMB_CACHE, embeddings)
    print(f"[checkpoint] Embeddings saved to {EMB_CACHE}")
    
    return embeddings


# ─── phase 3: cluster ──────────────────────────────────────────────────

def cluster_turns(embeddings: np.ndarray) -> tuple[int, np.ndarray, float]:
    """
    Run KMeans over the k-grid, pick by silhouette score.
    Returns (best_k, labels, silhouette_score).
    """
    from sklearn.cluster import KMeans
    from sklearn.metrics import silhouette_score
    
    results = []
    for k in K_GRID:
        km = KMeans(n_clusters=k, random_state=SEED, n_init=10)
        labels = km.fit_predict(embeddings)
        sil = silhouette_score(embeddings, labels, sample_size=min(5000, len(embeddings)), random_state=SEED)
        print(f"[cluster] k={k}: silhouette={sil:.4f}")
        results.append((k, labels, sil, km))
    
    # Pick best by silhouette
    best = max(results, key=lambda r: r[2])
    best_k, best_labels, best_sil, best_km = best
    print(f"[cluster] Best: k={best_k}, silhouette={best_sil:.4f}")
    
    return best_k, best_labels, best_sil, best_km


def _tokenize(text: str) -> list[str]:
    """Simple word tokenizer for tf-idf."""
    return re.findall(r'\b[a-zA-Z][a-zA-Z0-9\-]{1,}\b', text.lower())


def _compute_tfidf_distinguishing(
    cluster_texts: list[str],
    all_texts: list[str],
    cluster_labels: np.ndarray,
    this_cluster: int,
) -> list[tuple[str, float]]:
    """
    Compute tf-idf where idf is computed as: how distinguishing is this term
    of THIS cluster vs the rest of the corpus?
    Uses a simple approach: term frequency in cluster / term frequency in corpus.
    """
    from sklearn.feature_extraction.text import TfidfVectorizer
    
    # Build corpus: all texts
    # For cluster-level tf-idf, we compute tf per cluster (concatenated) vs idf across clusters
    # Simpler and more interpretable: concatenate each cluster's texts into one "document",
    # then tf-idf with clusters as documents.
    
    # Build per-cluster documents
    unique_clusters = sorted(set(cluster_labels.tolist()))
    cluster_docs = {}
    for c in unique_clusters:
        mask = cluster_labels == c
        cluster_docs[c] = ' '.join(t for t, m in zip(all_texts, mask) if m)
    
    docs = [cluster_docs[c] for c in unique_clusters]
    
    vec = TfidfVectorizer(
        max_features=5000,
        stop_words='english',
        token_pattern=r'\b[a-zA-Z][a-zA-Z0-9\-]{1,}\b',
        sublinear_tf=True,
    )
    tfidf_matrix = vec.fit_transform(docs)
    feature_names = vec.get_feature_names_out()
    
    # Get this cluster's row
    c_idx = unique_clusters.index(this_cluster)
    row = tfidf_matrix[c_idx]
    
    # Sort terms by tf-idf weight for this cluster
    term_weights = []
    for j in row.nonzero()[1]:
        term_weights.append((feature_names[j], row[0, j]))
    term_weights.sort(key=lambda x: -x[1])
    
    return term_weights[:TOP_TERMS]


def build_cluster_descriptors(
    turns: list[dict],
    embeddings: np.ndarray,
    labels: np.ndarray,
    k: int,
    km,
) -> list[dict]:
    """Build per-cluster descriptor dicts."""
    all_texts = [t['text'] for t in turns]
    
    # Load b1-live-marks for mark/PZ1-hit checking
    marks_data = _load_marks_data()
    
    clusters = []
    for c in range(k):
        mask = labels == c
        cluster_indices = np.where(mask)[0]
        cluster_size = len(cluster_indices)
        
        if cluster_size == 0:
            continue
        
        # Distinguishing terms
        dist_terms = _compute_tfidf_distinguishing(all_texts, all_texts, labels, c)
        
        # Representative snippets: closest to centroid
        centroid = km.cluster_centers_[c]
        cluster_embs = embeddings[cluster_indices]
        # Cosine distance to centroid (embeddings are normalized)
        dists = np.linalg.norm(cluster_embs - centroid, axis=1)
        closest_local = np.argsort(dists)[:N_REPR]
        
        repr_snippets = []
        for li in closest_local:
            gi = int(cluster_indices[li])
            turn = turns[gi]
            text = turn['text']
            if len(text) > MAX_SNIPPET_LEN:
                text = text[:MAX_SNIPPET_LEN - 3] + '...'
            repr_snippets.append({
                'text': text,
                'id': turn['id'],
                'at': turn.get('at', ''),
            })
        
        # Mark/PZ1-hit share
        mark_count = 0
        for gi in cluster_indices:
            turn = turns[int(gi)]
            if _turn_has_mark(turn, marks_data):
                mark_count += 1
        mark_share = mark_count / cluster_size if cluster_size > 0 else 0.0
        
        # Mission distribution
        mission_counts = {}
        for gi in cluster_indices:
            turn = turns[int(gi)]
            m = turn.get('clocked_mission')
            if m:
                mission_counts[m] = mission_counts.get(m, 0) + 1
        top_missions = sorted(mission_counts.items(), key=lambda x: -x[1])[:3]
        
        clusters.append({
            'cluster_id': c,
            'size': cluster_size,
            'share_of_corpus': cluster_size / len(turns),
            'top_terms': [{'term': t, 'tfidf': round(w, 4)} for t, w in dist_terms],
            'representative_snippets': repr_snippets,
            'mark_hit_share': round(mark_share, 4),
            'mark_hit_count': mark_count,
            'top_missions': [{'mission': m, 'count': n} for m, n in top_missions],
        })
    
    # Sort clusters by size (largest first)
    clusters.sort(key=lambda c: -c['size'])
    # Renumber
    for i, c in enumerate(clusters):
        c['display_id'] = i + 1
    
    return clusters


def _load_marks_data():
    """Load b1-live-marks.edn for mark/PZ1-hit checking if trivially available."""
    marks_path = SCRIPT_DIR / "b1-live-marks.edn"
    if not marks_path.exists():
        return None
    
    try:
        with open(marks_path) as f:
            edn = f.read()
        # Extract evidence ids from the marks file
        ids = set(re.findall(r':ref/id "([^"]+)"', edn))
        return {'mark_evidence_ids': ids}
    except Exception:
        return None


def _turn_has_mark(turn: dict, marks_data) -> bool:
    """Check if a turn carries an existing mark (✘/✓/💡) or is in PZ1 hit set."""
    text = turn.get('text', '')
    # Check for mark glyphs
    if any(g in text for g in ['✘', '✓', '💡']):
        return True
    # Check against b1-live-marks ids
    if marks_data and turn.get('id') in marks_data.get('mark_evidence_ids', set()):
        return True
    return False


def render_markdown(clusters: list[dict], k: int, sil: float, n_turns: int) -> str:
    """Render the one-page markdown for Joe."""
    lines = []
    lines.append("# Mark Vocabulary — Cluster Probe")
    lines.append("")
    lines.append(f"**Corpus:** {n_turns} joe-authored turns · "
                 f"**Embedding:** MiniLM (all-MiniLM-L6-v2, normalized) · "
                 f"**Clustering:** KMeans (k-grid {K_GRID}, seed={SEED})")
    lines.append(f"**Chosen k:** {k} (best silhouette = {sil:.4f})")
    lines.append("")
    lines.append("---")
    lines.append("")
    lines.append("*Joe: write a NAME (or \"skip\") next to each cluster. "
                 "Each cluster is described mechanically — no semantic labels proposed.*")
    lines.append("")
    
    for c in clusters:
        did = c['display_id']
        pct = c['share_of_corpus'] * 100
        lines.append(f"## Cluster {did} — [your name here]")
        lines.append(f"**Size:** {c['size']} turns ({pct:.1f}% of corpus) · "
                     f"**mark-hit share:** {c['mark_hit_share']:.1%} ({c['mark_hit_count']} turns)")
        
        # Terms
        terms = ', '.join(f"`{t['term']}`" for t in c['top_terms'])
        lines.append(f"**Distinguishing terms:** {terms}")
        
        # Missions
        if c['top_missions']:
            missions = ', '.join(f"{m['mission']} ({m['count']})" for m in c['top_missions'])
            lines.append(f"**Top missions:** {missions}")
        
        # Snippets
        lines.append(f"**Representative turns:**")
        for s in c['representative_snippets']:
            lines.append(f"  > {s['text']}")
            lines.append(f"  > — `{s['id']}` ({s.get('at', '?')[:10]})")
        
        lines.append("")
    
    lines.append("---")
    lines.append("")
    lines.append("## Method")
    lines.append("")
    lines.append(f"- **Pull:** all `type=coordination` evidence authored by `joe` from "
                 f"the futon1b store (:7073), deduplicated by `:evidence/id`.")
    lines.append(f"- **Embed:** `sentence-transformers/all-MiniLM-L6-v2`, L2-normalized, "
                 f"batch size 64.")
    lines.append(f"- **Cluster:** KMeans (`n_init=10`, seed={SEED}) over k ∈ {K_GRID}. "
                 f"k={k} selected by highest silhouette score ({sil:.4f}).")
    lines.append(f"- **Terms:** per-cluster tf-idf (sublinear, English stopwords) where "
                 f"each cluster is treated as one document against the cluster-doc corpus.")
    lines.append(f"- **Snippets:** 5 turns closest to the cluster centroid (cosine distance "
                 f"in normalized embedding space).")
    lines.append(f"- **Mark-hit share:** fraction of turns in the cluster carrying a "
                 f"✘/✓/💡 glyph or appearing in `b1-live-marks.edn`.")
    lines.append(f"- **Determinism:** fixed seed throughout; checkpointed at pull "
                 f"(turns-cache.json) and embedding (embeddings-cache.npy).")
    
    return '\n'.join(lines)


# ─── main ──────────────────────────────────────────────────────────────

def main():
    t0 = time.time()
    print("=" * 60)
    print("MARK VOCAB PROBE — M-points-de-fuite §6.5")
    print("=" * 60)
    
    # Phase 1: pull
    print("\n[Phase 1] Pulling joe turns...")
    turns = pull_turns()
    n_turns = len(turns)
    
    if n_turns == 0:
        print("[FATAL] No turns pulled. Aborting.", file=sys.stderr)
        sys.exit(1)
    
    # Phase 2: embed
    print(f"\n[Phase 2] Embedding {n_turns} turns...")
    embeddings = embed_turns(turns)
    
    # Phase 3: cluster
    print(f"\n[Phase 3] Clustering...")
    k, labels, sil, km = cluster_turns(embeddings)
    
    # Build descriptors
    print(f"\n[Phase 4] Building cluster descriptors...")
    clusters = build_cluster_descriptors(turns, embeddings, labels, k, km)
    
    # Save JSON
    output = {
        'n_turns': n_turns,
        'k_chosen': k,
        'silhouette': float(sil),
        'k_grid': K_GRID,
        'seed': SEED,
        'model': MODEL_NAME,
        'clusters': clusters,
    }
    with open(CLUSTERS_JSON, 'w') as f:
        json.dump(output, f, indent=2, ensure_ascii=False)
    print(f"[output] JSON saved to {CLUSTERS_JSON}")
    
    # Render markdown
    md = render_markdown(clusters, k, sil, n_turns)
    with open(CLUSTERS_MD, 'w') as f:
        f.write(md)
    print(f"[output] Markdown saved to {CLUSTERS_MD}")
    
    elapsed = time.time() - t0
    print(f"\n{'=' * 60}")
    print(f"SUMMARY: n_turns={n_turns}, k={k}, silhouette={sil:.4f}, elapsed={elapsed:.1f}s")
    print(f"{'=' * 60}")


if __name__ == '__main__':
    main()
