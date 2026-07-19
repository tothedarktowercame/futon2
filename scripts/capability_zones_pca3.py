#!/usr/bin/env python3
"""Fit deterministic, inspectable PCA-3 for capability-zones S1.5."""
from __future__ import annotations
import hashlib, json
from pathlib import Path
import numpy as np
from sklearn.decomposition import PCA

PREPARED = Path("data/capability_zones/harvest-prepared.json")
SEEDS = Path("resources/capability_zones/action_class_seeds.json")
OUTPUT = Path("data/capability_zones/reduction-pca3-v1.json")

prepared = json.loads(PREPARED.read_text())
seeds = json.loads(SEEDS.read_text())
item_vectors = [item["embedding"] for item in prepared["items"]]
seed_rows = []
for seed in seeds:
    use_centroid = seed.get("centroid_evidence_count", 0) > 0
    vector = seed["centroid_seed"] if use_centroid else seed["text_seed"]
    seed_rows.append({"class": seed["class"],
                      "generation": "centroid-seed" if use_centroid else "text-seed",
                      "vector": vector})
matrix = np.asarray(item_vectors + [row["vector"] for row in seed_rows], dtype=np.float64)
pca = PCA(n_components=3, svd_solver="full")
pca.fit(matrix)
digest = hashlib.sha256()
digest.update("\n".join(item["id"] for item in prepared["items"]).encode())
digest.update(matrix.tobytes(order="C"))
artifact = {
    "schema": "capability-zone-reduction.v1",
    "version": "pca3-v1",
    "method": "PCA",
    "deterministic": True,
    "rationale": "PCA-3 is deterministic and matrix-auditable. If operator walking rejects v1, a separately versioned UMAP-3 with pinned random_state may be proposed; no v2 is built here.",
    "mean": pca.mean_.tolist(),
    "components": pca.components_.tolist(),
    "explained_variance": pca.explained_variance_.tolist(),
    "explained_variance_ratio": pca.explained_variance_ratio_.tolist(),
    "fit_corpus": {
        "items": len(item_vectors), "seeds": len(seed_rows),
        "total_vectors": int(matrix.shape[0]), "embedding_dimension": int(matrix.shape[1]),
        "item_source": str(PREPARED), "seed_source": str(SEEDS),
        "order": "prepared items in artifact order, then action-class seeds in seed-file order",
        "seed_generations": [{"class": r["class"], "generation": r["generation"]} for r in seed_rows],
        "sha256_ordered_ids_and_float64_matrix": digest.hexdigest(),
    },
}
OUTPUT.write_text(json.dumps(artifact, separators=(",", ":")) + "\n")
print(json.dumps({"output": str(OUTPUT), "shape": list(matrix.shape),
                  "explained_variance_ratio": artifact["explained_variance_ratio"]}))
