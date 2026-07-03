#!/usr/bin/env python3
"""M-evaluate-policies E5 — the conditioned-cascade probe (§8.13 / §9.7).

Measures how cascade valuation shifts when the circumstance is enriched with the
LIVE situation (the latest wm-trace tick's out-of-range channels) versus the
production psi scrap (title+status only, the cascade_lane Q-C recipe).

Baseline vs conditioned, over the 13-mission sample (same as
spike_spectral_potentials.py). Per mission: Δwholeness, ΔF, Δsize, Jaccard of the
shown pattern-sets, top-pattern change. Aggregate: mean |Δ|, #top-pattern flips,
Spearman of the F-ordering (rank stability).

HONESTY CAVEATS (also emitted in the artifact):
 (a) This enriches the PROSE conditioning — it probes sensitivity to situation-in-
     the-psi. It is NOT the full repair, which conditions the SCORE on the belief
     state directly.
 (b) MiniLM cosine over longer text can DILUTE relevance. If size/relevance
     collapse UNIFORMLY (all sizes shrink, Jaccard uniformly low), that is the
     embedding-artifact failure mode, NOT a situatedness result. The summary flags
     this if detected.

Deterministic given the same latest tick (recorded in the artifact).
Usage: futon3a/.venv/bin/python futon2/scripts/e5_conditioned_probe.py
"""
import json, re, subprocess, sys
from pathlib import Path

CODE = Path("/home/joe/code")
sys.path.insert(0, str(CODE / "futon3a/holes/labs/M-memes-arrows"))
from cascade_construct import construct_cascade  # noqa: E402

BUDGET, EPSILON = 6, 0.15
OUT = CODE / "futon2/holes/labs/M-evaluate-policies/e5-conditioned-probe.json"

MISSION_SAMPLE = [
    "M-evaluate-policies", "M-G-over-cascades", "M-aif2", "M-fold-ansatz",
    "M-typed-bells", "M-first-flights", "M-apm-solutions",
    "M-invariant-queue-unstuck", "M-futonzero-grounding",
    "M-differentiable-substrate", "M-efe-bge-followon-actions",
    "M-canon-fingerprint-store", "M-bayesian-structure-learning",
]
MISSION_DOC_ROOTS = [CODE / r / s
                     for r in ["futon0", "futon1", "futon2", "futon3", "futon3a", "futon3b",
                               "futon3c", "futon4", "futon5", "futon5a", "futon6", "futon7"]
                     for s in ["holes/missions", "holes"]]


def mission_psi(target):
    """Production psi = cascade_lane.clj mission->psi (Q-C have->want recipe)."""
    stem = re.sub(r"^M-", "", target).replace("-", " ")
    for root in MISSION_DOC_ROOTS:
        f = root / f"{target}.md"
        if f.exists():
            lines = f.read_text().splitlines()
            title = next((m.group(1) for l in lines
                          if (m := re.match(r"^#\s*(?:Mission:)?\s*(.*)", l))), None)
            status = next((m.group(1) for l in lines
                           if (m := re.match(r"(?i)^\*\*Status:?\*\*:?\s*(.*)", l))), "")
            if title:
                psi = f"{stem} — want: {title.strip()[:160]}"
                if status.strip():
                    psi += f". have: {status.strip()[:160]}"
                return psi
    return stem


# --- the LIVE observation summary from the latest wm-trace tick (bb reads EDN) ---
BB_SNIPPET = r"""
(require '[clojure.string :as s] '[clojure.java.io :as io])
(let [dir (io/file "/home/joe/code/futon2/data/wm-trace")
      f (->> (file-seq dir) (map str) (filter #(s/ends-with? % ".edn")) sort last)
      forms (clojure.edn/read-string (str "[" (slurp f) "]"))
      r (last forms)
      pc (get-in r [:free-energy :per-channel])
      oor (->> pc (filter (fn [[_ v]] (false? (:in-range? v))))
               (sort-by (fn [[_ v]] (- (double (:gap v))))))
      parts (map (fn [[k v]] (format "%s (val %.2f, want %s, gap %.2f)"
                                     (name k) (double (:value v)) (:preferred v) (double (:gap v))))
                 (take 6 oor))
      summary (str "Current situation: " (count oor) "/" (count pc)
                   " belief channels out of preferred range — " (s/join "; " parts) ".")]
  (println (str (:timestamp r)))
  (println (subs summary 0 (min 400 (count summary)))))
"""


def live_observation():
    p = subprocess.run(["bb", "-e", BB_SNIPPET], capture_output=True, text=True, timeout=60)
    lines = [l for l in p.stdout.splitlines() if l.strip()]
    if len(lines) < 2:
        raise RuntimeError(f"bb obs extraction failed: {p.stdout!r} {p.stderr!r}")
    return lines[0].strip(), lines[1].strip()  # (tick-id, summary)


def cascade(psi):
    r = construct_cascade(psi, epsilon=EPSILON)
    shown = [p for (p, _rel, _mc) in r["cascade"][:BUDGET]]
    return {"shown": shown, "wholeness": r["wholeness"], "F": r["F-free-energy"]}


def jaccard(a, b):
    sa, sb = set(a), set(b)
    return (len(sa & sb) / len(sa | sb)) if (sa | sb) else 1.0


def mean_pairwise_jaccard(sets):
    import itertools
    pairs = list(itertools.combinations(sets, 2))
    return round(sum(jaccard(a, b) for a, b in pairs) / len(pairs), 3) if pairs else 1.0


def spearman(xs, ys):
    def ranks(v):
        order = sorted(range(len(v)), key=lambda i: v[i])
        rk = [0] * len(v)
        for r, i in enumerate(order):
            rk[i] = r
        return rk
    rx, ry = ranks(xs), ranks(ys)
    n = len(xs)
    d2 = sum((rx[i] - ry[i]) ** 2 for i in range(n))
    return 1 - (6 * d2) / (n * (n * n - 1)) if n > 1 else 1.0


def main():
    tick, obs = live_observation()
    print(f"conditioned on tick {tick}\nobservation: {obs}\n")
    rows = []
    for target in MISSION_SAMPLE:
        psi = mission_psi(target)
        base = cascade(psi)
        cond = cascade(f"{psi} {obs}")
        rows.append({
            "mission": target,
            "base": base, "cond": cond,
            "d-wholeness": round(cond["wholeness"] - base["wholeness"], 4),
            "d-F": round(cond["F"] - base["F"], 4),
            "d-size": len(cond["shown"]) - len(base["shown"]),
            "jaccard": round(jaccard(base["shown"], cond["shown"]), 3),
            "top-changed": (base["shown"][:1] != cond["shown"][:1]),
        })
        print(f"{target:34s} ΔW={rows[-1]['d-wholeness']:+.3f} ΔF={rows[-1]['d-F']:+.3f} "
              f"Δsize={rows[-1]['d-size']:+d} J={rows[-1]['jaccard']:.2f} "
              f"top{'≠' if rows[-1]['top-changed'] else '='}")

    n = len(rows)
    mean_abs_w = round(sum(abs(r["d-wholeness"]) for r in rows) / n, 4)
    mean_abs_f = round(sum(abs(r["d-F"]) for r in rows) / n, 4)
    mean_dsize = round(sum(r["d-size"] for r in rows) / n, 3)
    top_flips = sum(1 for r in rows if r["top-changed"])
    mean_jac = round(sum(r["jaccard"] for r in rows) / n, 3)
    rho = round(spearman([r["base"]["F"] for r in rows], [r["cond"]["F"] for r in rows]), 3)

    # honesty caveat (b): detect the embedding-dilution failure mode
    all_shrink = all(r["d-size"] <= 0 for r in rows) and mean_dsize < 0
    dilution_flag = bool(all_shrink and mean_jac < 0.34)

    # the observation is GLOBAL (same suffix for all missions), so distinguish
    # per-mission situatedness from homogenization (all missions pulled toward the
    # same situation-patterns). +ve = conditioning converges the cascades.
    mpj_base = mean_pairwise_jaccard([r["base"]["shown"] for r in rows])
    mpj_cond = mean_pairwise_jaccard([r["cond"]["shown"] for r in rows])
    homogenization = round(mpj_cond - mpj_base, 3)

    aggregate = {
        "n-missions": n,
        "mean-abs-d-wholeness": mean_abs_w,
        "mean-abs-d-F": mean_abs_f,
        "mean-d-size": mean_dsize,
        "top-pattern-flips": top_flips,
        "mean-jaccard": mean_jac,
        "F-ordering-spearman": rho,
        "dilution-artifact-suspected?": dilution_flag,
        "inter-mission-jaccard-baseline": mpj_base,
        "inter-mission-jaccard-conditioned": mpj_cond,
        "homogenization": homogenization,
    }
    artifact = {
        "experiment": "M-evaluate-policies E5 — conditioned-cascade probe",
        "spec": "M-evaluate-policies.md §8.13 (E5), §9.7 (value-is-conditional)",
        "conditioned-on-tick": tick,
        "observation-summary": obs,
        "budget": BUDGET, "epsilon": EPSILON,
        "honesty-caveats": {
            "a-prose-conditioning": ("Enriches the PROSE conditioning (situation-in-psi); NOT the full "
                                     "repair, which conditions the SCORE on the belief state directly."),
            "b-embedding-dilution": ("MiniLM cosine over longer text can dilute relevance. If size/relevance "
                                     "collapse uniformly, that is an embedding artifact, not situatedness. "
                                     f"dilution-artifact-suspected? = {dilution_flag}."),
            "c-homogenization": ("The observation is GLOBAL (identical suffix on all missions), so some shift "
                                 "is missions converging on shared situation-patterns, not per-mission "
                                 f"situatedness. homogenization (Δ inter-mission Jaccard) = {homogenization} "
                                 f"(base {mpj_base} → cond {mpj_cond}); large +ve ⇒ read the shift partly as convergence."),
        },
        "aggregate": aggregate,
        "per-mission": rows,
    }
    OUT.parent.mkdir(parents=True, exist_ok=True)
    OUT.write_text(json.dumps(artifact, indent=2))

    print("\n=== AGGREGATE ===")
    for k, v in aggregate.items():
        print(f"  {k:28s} {v}")
    if dilution_flag:
        print("  ⚠ DILUTION ARTIFACT SUSPECTED — sizes collapse uniformly + low Jaccard; "
              "read as embedding effect, NOT situatedness.")
    print(f"\nwrote {OUT}")


if __name__ == "__main__":
    main()
