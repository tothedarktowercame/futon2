"""scoreboard.py — B3 of SPEC-full-loop-gfn: the preregistered S1–S4, one command.

Reads (all substrate-1, no JVM):
  - flown-fold labels: fold_ground_truth.load_records() (closure-folds +
    adjudicated deposits) — S3's label set;
  - CH2 fold events: futon3a/data/ch2-discharge-events.edn — S1/S2's stream
    once the escrow wiring (B4) lands;
  - proposal batches + sealed identities: findings/proposals/ — S1/S2 tagging
    and S4 diversity.

Appends one row to findings/scoreboard-history.jsonl (append-only; each row is
a dated S-point) and prints the table. Metric definitions are FIXED by the
preregistration (p4ng/main-2026.tex sec:prereg); changing them here after
flown-fold data = a new preregistration.

Run: cd ~/code/futon3a && .venv/bin/python3 ~/code/futon2/holes/labs/slush-demo/scoreboard.py
"""
from __future__ import annotations
import datetime as _dt
import json, re, sys
from pathlib import Path

LAB3A = "/home/joe/code/futon3a/holes/labs/M-memes-arrows"
sys.path.insert(0, LAB3A)

BASE = Path("/home/joe/code/futon2/holes/labs/slush-demo")
CH2_SINK = Path("/home/joe/code/futon3a/data/ch2-discharge-events.edn")
HISTORY = BASE / "findings" / "scoreboard-history.jsonl"
PROPOSALS = BASE / "findings" / "proposals"


def load_ch2_events():
    if not CH2_SINK.exists():
        return []
    out = []
    for line in CH2_SINK.read_text().splitlines():
        line = line.strip()
        if not line:
            continue
        d = dict(re.findall(r':([\w/?-]+)\s+("(?:[^"\\]|\\.)*"|true|false|\S+)', line))
        out.append({"move_id": d.get("move/id", "").strip('"'),
                    "discharged": d.get("discharged?") == "true"})
    return out


def load_sealed():
    sealed = {}
    for f in sorted(PROPOSALS.glob("proposals-*.SEALED.json")):
        sealed.update(json.load(open(f)))
    return sealed


def latest_batch():
    files = sorted(f for f in PROPOSALS.glob("proposals-*.json")
                   if "SEALED" not in f.name)
    return (json.load(open(files[-1])), files[-1].name) if files else ([], None)


def s4_diversity(batch, sealed):
    """Distinct GFN proposals per mission + mean pairwise Jaccard distance."""
    per = {}
    for row in batch:
        who = sealed.get(row["proposal"], {}).get("proposer")
        if who == "gfn":
            per.setdefault(row["mission"], []).append(frozenset(row["patterns"]))
    out = {}
    for m, sets in per.items():
        dists = [1 - len(a & b) / len(a | b)
                 for i, a in enumerate(sets) for b in sets[i + 1:]]
        out[m] = {"distinct": len(set(sets)),
                  "mean_jaccard_dist": round(sum(dists) / len(dists), 3) if dists else None}
    return out


def main():
    from fold_ground_truth import load_records
    from reward_v0 import loo_s3
    from reward_v1 import loo_s3_v1
    records = load_records()
    y = [r["success"] for r in records]

    events = load_ch2_events()
    sealed = load_sealed()
    gfn_hashes = {h for h, v in sealed.items() if v.get("proposer") == "gfn"}
    inc_hashes = {h for h, v in sealed.items() if v.get("proposer") == "incumbent"}
    # S1/S2: CH2 events attributable to a proposal (move-id carries the hash
    # once B4 lands; until then these are honestly empty)
    flown_gfn = [e for e in events if any(h in e["move_id"] for h in gfn_hashes)]
    flown_inc = [e for e in events if any(h in e["move_id"] for h in inc_hashes)]

    s3_auc, s3_null, _ = loo_s3_v1(records)  # primary = v1.2 (operator flip 2026-07-11)
    s3v0_auc, s3v0_null, _ = loo_s3(records)  # v0 kept as reference column
    batch, batch_name = latest_batch()
    s4 = s4_diversity(batch, sealed)

    row = {
        "date": _dt.date.today().isoformat(),
        "n_labels": len(records),
        "labels": {"success": sum(y), "fail": len(y) - sum(y)},
        "S1_discharge_rate": (sum(e["discharged"] for e in flown_gfn) / len(flown_gfn)
                              if flown_gfn else None),
        "S1_n_flown_gfn": len(flown_gfn),
        "S2_incumbent_rate": (sum(e["discharged"] for e in flown_inc) / len(flown_inc)
                              if flown_inc else None),
        "S2_n_flown_incumbent": len(flown_inc),
        "S3_auc": round(s3_auc, 3),
        "S3_null95": round(s3_null, 3),
        "S3_clears_null": s3_auc > s3_null,
        "S3_reward": "v1.2",
        "S3_v0_auc": round(s3v0_auc, 3),
        "S3_v0_null95": round(s3v0_null, 3),
        "S4_per_mission": s4,
        "proposal_batch": batch_name,
        "n_proposals_outstanding": len(batch),
        "n_ch2_events": len(events),
    }
    HISTORY.parent.mkdir(parents=True, exist_ok=True)
    with open(HISTORY, "a") as f:
        f.write(json.dumps(row) + "\n")

    print(f"SCOREBOARD {row['date']}  (labels: {row['labels']['success']}+/"
          f"{row['labels']['fail']}-)")
    print(f"  S1 discharge rate (flown GFN folds) : "
          f"{row['S1_discharge_rate'] if row['S1_n_flown_gfn'] else 'no flown GFN folds yet'}")
    print(f"  S2 vs incumbent                     : "
          f"{row['S2_incumbent_rate'] if row['S2_n_flown_incumbent'] else 'no flown incumbent folds yet'}")
    print(f"  S3 discrimination v1.2 (LOO vs null): {row['S3_auc']} "
          f"(null-95 {row['S3_null95']}) {'PASS' if row['S3_clears_null'] else 'below'}"
          f"   [v0 ref: {row['S3_v0_auc']} vs {row['S3_v0_null95']}]")
    print(f"  S4 diversity                        : " +
          (", ".join(f"{m}: {v['distinct']} modes (J̄ {v['mean_jaccard_dist']})"
                     for m, v in s4.items()) if s4 else "no batch yet"))
    print(f"  outstanding blinded proposals       : {row['n_proposals_outstanding']} "
          f"({batch_name})")
    print(f"  appended -> {HISTORY.name}")


if __name__ == "__main__":
    main()
