"""S4 trainer: one GFlowNet (TB) per circumstance; emit sampler-protocol entries.

Per circumstance: Choices env (exact-6 of frontier; fewer-than-max is
NotImplementedError in the library — recorded contest finding), CascadeCProxy
(verified C port), trajectory-balance, small MLP, CPU. After training, sample
K terminal states, keep the distinct selections, emit buildout-shaped entries
to gflownet-entries.json for the Clojure referee.
"""
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, "/home/joe/code/gflownet")
sys.path.insert(0, str(Path(__file__).parent))

import cascade_proxy
from c_proxy import score_buildout_c

HERE = Path(__file__).parent
N_STEPS = int(sys.argv[1]) if len(sys.argv) > 1 else 1200
PROXY_TARGET = sys.argv[2] if len(sys.argv) > 2 else "cascade_proxy.CascadeCProxy"
OUT_NAME = sys.argv[3] if len(sys.argv) > 3 else "gflownet-entries.json"
SAMPLER_ID = sys.argv[4] if len(sys.argv) > 4 else "gflownet-tb"
K_SAMPLES = 128
BUDGET = 6
SEED = 20260612


def make_config(n_options: int):
    from hydra import compose, initialize_config_dir
    from hydra.core.global_hydra import GlobalHydra
    GlobalHydra.instance().clear()
    initialize_config_dir(config_dir="/home/joe/code/gflownet/config", version_base="1.1")
    return compose(config_name="tests", overrides=[
        "env=choices",
        f"env.n_options={n_options}",
        f"env.max_selection={BUDGET}",
        "env.with_replacement=False",
        "env.can_select_fewer_than_max=False",
        "proxy=uniform",
        f"proxy._target_={PROXY_TARGET}",
        "gflownet=trajectorybalance",
        f"gflownet.optimizer.n_train_steps={N_STEPS}",
        "policy.forward.n_hid=64",
        "policy.forward.n_layers=2",
        "evaluator.first_it=False",
        "evaluator.period=-1",
        "logger.do.online=False",
        f"seed={SEED}",
        "device=cpu",
    ])


def run_circumstance(circ: dict) -> dict:
    from gflownet.utils.common import gflownet_from_config
    moves = circ["moves"]
    cascade_proxy.MOVES = moves
    t0 = time.time()
    config = make_config(len(moves))
    gfn = gflownet_from_config(config)
    gfn.train()
    train_ms = (time.time() - t0) * 1000.0
    batch, _ = gfn.sample_batch(n_forward=K_SAMPLES, train=False)
    states = batch.get_terminating_states()
    seen, entries = set(), []
    for st in states:
        sel = tuple(sorted(cascade_proxy.selection_from_proxy_state(
            gfn.env.states2proxy([st])[0])))
        if not sel or sel in seen:
            continue
        seen.add(sel)
        policy = [moves[i] for i in sel]
        entries.append({
            "buildout/id": f"gfn-{circ['id']}-{len(entries):02d}",
            "policy": policy,
            "implied-moves": [m["move_id"] for m in policy],
            "C": score_buildout_c(policy),
            "selection": list(sel),
        })
    entries.sort(key=lambda e: -e["C"])
    return {"circumstance": circ["id"], "wall-clock-ms": train_ms,
            "distinct-selections": len(entries), "k-sampled": K_SAMPLES,
            "entries": entries[:8]}


def main():
    circs = json.load(open(HERE / "circumstances-v0.json"))
    results = []
    for circ in circs:
        r = run_circumstance(circ)
        print(f"{r['circumstance']}: {r['distinct-selections']} distinct / "
              f"{r['k-sampled']} sampled · {r['wall-clock-ms']:.0f} ms", flush=True)
        results.append(r)
    out = HERE / OUT_NAME
    out.write_text(json.dumps({"sampler/id": SAMPLER_ID,
                               "n-train-steps": N_STEPS, "seed": SEED,
                               "budget": BUDGET,
                               "fewer-than-max": "NotImplementedError in library — exact-6 restriction recorded",
                               "results": results}, indent=1))
    print(f"wrote {out}")


if __name__ == "__main__":
    main()
