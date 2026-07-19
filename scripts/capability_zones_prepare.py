#!/usr/bin/env python3
"""Prepare the uncapped FUTON git corpus and BGE embeddings for S1 harvest."""

from __future__ import annotations

import argparse
import json
import re
import subprocess
from pathlib import Path

REPOS = [
    "futon0", "futon1", "futon1a", "futon1b", "futon1bi", "futon2",
    "futon3", "futon3a", "futon3b", "futon3c", "futon4", "futon5",
    "futon5a", "futon6", "futon7", "futon7a", "futonY",
]
MODEL = "BAAI/bge-large-en-v1.5"
RECORD_SEP = "\x1e"
FIELD_SEP = "\x1f"
# bge-large truncates input at 512 tokens; characters past ~4k never reach the
# model, so capping here only removes bytes the embedder would silently drop.
# The cap is recorded per item (text_chars, text_truncated) — no silent caps.
TEXT_CAP = 4000
CHUNK = 256


def git(repo: Path, *args: str, check: bool = True) -> str:
    result = subprocess.run(
        ["git", "-C", str(repo), *args], text=True,
        stdout=subprocess.PIPE, stderr=subprocess.PIPE,
    )
    if check and result.returncode:
        raise RuntimeError(f"git {' '.join(args)} failed in {repo}: {result.stderr}")
    return result.stdout


def default_ref(repo: Path) -> str:
    remote = git(repo, "symbolic-ref", "--quiet", "--short",
                 "refs/remotes/origin/HEAD", check=False).strip()
    if remote:
        return remote
    for candidate in ("main", "master"):
        if git(repo, "rev-parse", "--verify", "--quiet", candidate,
               check=False).strip():
            return candidate
    return "HEAD"


def likely_paths(block: str) -> list[str]:
    paths = []
    for line in block.splitlines():
        line = line.strip()
        if (line and " " not in line and not line.startswith(":")
                and ("/" in line or "." in Path(line).name)):
            paths.append(line)
    return sorted(set(paths))


def reachable_text(repo: Path, paths: list[str]) -> str:
    chunks = []
    for rel in paths:
        path = repo / rel
        if path.is_file():
            try:
                chunks.append(path.read_text(encoding="utf-8"))
            except (OSError, UnicodeDecodeError):
                continue
    return "\n".join(chunks)


def classify_grain(repo: Path, block: str, paths: list[str]) -> tuple[str, str]:
    joined = "\n".join(paths)
    feature_paths = [p for p in paths if re.search(
        r"(?i)(attempt|feature[-_ ]?card|full[-_ ]?loop)", p)]
    if re.search(r"(?i)(:feature-card|feature[- ]card|attempt-\d{3})", block):
        return "attempt-feature", block + "\n" + reachable_text(repo, feature_paths)
    mission_paths = [p for p in paths if re.search(
        r"(?i)(^|/)holes/missions/.*\.md$|(^|/)patterns?/", p)]
    if mission_paths or re.search(r"\bM-[A-Za-z0-9_-]+", block):
        return "mission-pattern", block + "\n" + reachable_text(repo, mission_paths)
    return "commit", block + "\nChanged paths:\n" + joined


def commits(repo: Path, ref: str) -> list[dict]:
    raw = git(repo, "log", ref, "--date=iso-strict",
              f"--format={RECORD_SEP}%H{FIELD_SEP}%aI{FIELD_SEP}%B",
              "--name-only")
    items = []
    reverted = set(re.findall(r"(?im)^This reverts commit ([0-9a-f]{40})", raw))
    for record in raw.split(RECORD_SEP):
        if not record.strip():
            continue
        fields = record.split(FIELD_SEP, 2)
        if len(fields) != 3:
            continue
        sha, committed_at, block = (part.strip() for part in fields)
        paths = likely_paths(block)
        grain, text = classify_grain(repo, block, paths)
        subject = next((line.strip() for line in block.splitlines()
                        if line.strip()), "")
        full_chars = len(text)
        items.append({
            "id": f"harvest:item:{repo.name}:{sha}",
            "repo": repo.name,
            "sha": sha,
            "committed_at": committed_at,
            "subject": subject,
            "grain": grain,
            "paths": paths,
            "path_count": len(paths),
            "followthrough": sha not in reverted,
            "text": text[:TEXT_CAP],
            "text_chars": full_chars,
            "text_truncated": full_chars > TEXT_CAP,
        })
    return items


def embed_checkpointed(combined: list[dict], checkpoint_dir: Path) -> list[list[float]]:
    """Encode all texts with one in-process model load, in resumable chunks.

    Each chunk's vectors are written to checkpoint_dir keyed by chunk index and
    verified against the chunk's item ids on reuse, so a killed run resumes
    instead of starting over and a changed corpus re-encodes rather than
    silently reusing stale vectors.
    """
    from sentence_transformers import SentenceTransformer

    checkpoint_dir.mkdir(parents=True, exist_ok=True)
    model = None
    vectors: list[list[float]] = []
    total = (len(combined) + CHUNK - 1) // CHUNK
    for n in range(total):
        chunk = combined[n * CHUNK:(n + 1) * CHUNK]
        ids = [c.get("id") or f"seed:{c.get('class')}" for c in chunk]
        ckpt = checkpoint_dir / f"chunk-{n:05d}.json"
        if ckpt.is_file():
            try:
                saved = json.loads(ckpt.read_text())
                if saved.get("ids") == ids:
                    vectors.extend(saved["vectors"])
                    print(f"[prepare] chunk {n + 1}/{total} reused", flush=True)
                    continue
            except (OSError, json.JSONDecodeError, KeyError):
                pass
        if model is None:
            print(f"[prepare] loading {MODEL}", flush=True)
            model = SentenceTransformer(MODEL)
        encoded = model.encode([c["text"] for c in chunk],
                               normalize_embeddings=True)
        chunk_vectors = [[float(x) for x in vec] for vec in encoded]
        tmp = ckpt.with_suffix(".tmp")
        tmp.write_text(json.dumps({"ids": ids, "vectors": chunk_vectors},
                                  separators=(",", ":")))
        tmp.rename(ckpt)
        vectors.extend(chunk_vectors)
        print(f"[prepare] chunk {n + 1}/{total} encoded", flush=True)
    return vectors


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("--root", default="/home/joe/code")
    parser.add_argument("--seeds", required=True)
    parser.add_argument("--prepared", required=True)
    parser.add_argument("--seed-output", required=True)
    args = parser.parse_args()
    root = Path(args.root)
    seeds = json.loads(Path(args.seeds).read_text())
    items: list[dict] = []
    coverage = {}
    for name in REPOS:
        repo = root / name
        if not (repo / ".git").exists():
            coverage[name] = {"reachable": False, "reason": "not a git repository",
                              "total": 0, "described": 0, "grains": {}}
            continue
        ref = default_ref(repo)
        repo_items = commits(repo, ref)
        grains = {}
        for item in repo_items:
            grains[item["grain"]] = grains.get(item["grain"], 0) + 1
        coverage[name] = {"reachable": True, "default_ref": ref,
                          "total": len(repo_items), "described": len(repo_items),
                          "grains": grains}
        items.extend(repo_items)

    combined = ([{"kind": "seed", "class": seed["class"],
                  "text": seed["text"]} for seed in seeds]
                + [{"kind": "item", "id": item["id"], "text": item["text"]}
                   for item in items])
    checkpoint_dir = Path(args.prepared).parent / "embed-checkpoints"
    embedded = embed_checkpointed(combined, checkpoint_dir)
    seed_embedded = embedded[:len(seeds)]
    item_embedded = embedded[len(seeds):]
    seed_records = []
    for seed, emb in zip(seeds, seed_embedded, strict=True):
        seed_records.append({**seed, "text_seed": emb,
                             "embed_model": MODEL,
                             "embed_dim": len(emb)})
    Path(args.seed_output).parent.mkdir(parents=True, exist_ok=True)
    Path(args.seed_output).write_text(json.dumps(seed_records,
                                                 separators=(",", ":")) + "\n")
    for item, emb in zip(items, item_embedded, strict=True):
        item["embedding"] = emb
        item.pop("text", None)
    prepared = {"schema": "capability-zone-harvest-prepared.v1",
                "generated_at": "2026-07-19T00:00:00Z",
                "embed_model": MODEL,
                "embed_dim": seed_records[0]["embed_dim"],
                "corpus": REPOS, "coverage": coverage, "items": items}
    Path(args.prepared).parent.mkdir(parents=True, exist_ok=True)
    Path(args.prepared).write_text(json.dumps(prepared,
                                              separators=(",", ":")) + "\n")
    print(json.dumps({"items": len(items), "coverage": coverage}, indent=2))


if __name__ == "__main__":
    main()
