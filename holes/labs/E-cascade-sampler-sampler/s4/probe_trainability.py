"""Exit-4 trainability probe: can the TB sampler learn from flight records?

Corpus: fable-1's r11-training-examples.edn — 33 examples, 31 masked :out
(derivation-thin; the derived-never-authored mask refused them), 2 :in, of
which 1 :clean. The probe trains on the ONE clean flight-grounded reward and
reports P(chosen-target in sample) untrained vs trained. n=1, reported as
n=1 — the probe demonstrates the PIPE, the corpus grows one flight at a time.

Reward: selections containing the flight's chosen target score |realised-G|;
others score epsilon. Pure flight-grounding — no C in this probe.
"""
import json
import sys
import time
from pathlib import Path

sys.path.insert(0, "/home/joe/code/gflownet")
sys.path.insert(0, str(Path(__file__).parent))

import cascade_proxy
import torch
from gflownet.proxy.base import Proxy

HERE = Path(__file__).parent
TARGET = "M-daily-scan"          # the one :clean :in example's chosen target
REALISED_G = 4.039678471388555   # |realised g| from live-957a4836
N_STEPS = 600
K = 256
SEED = 20260612


class FlightGroundedProxy(Proxy):
    def __init__(self, **kw):
        kw.setdefault("reward_min", 1e-6)
        kw.setdefault("do_clip_rewards", True)
        super().__init__(**kw)

    def setup(self, env=None):
        pass

    def __call__(self, states):
        vals = []
        for st in states:
            sel = cascade_proxy.selection_from_proxy_state(st)
            tgts = {cascade_proxy.MOVES[i].get("source_action", {}).get("target")
                    for i in sel}
            vals.append(REALISED_G if TARGET in tgts else 1e-3)
        return torch.tensor(vals, dtype=self.float, device=self.device)


def target_rate(gfn, moves):
    batch, _ = gfn.sample_batch(n_forward=K, train=False)
    states = batch.get_terminating_states()
    hit = 0
    for st in states:
        sel = cascade_proxy.selection_from_proxy_state(gfn.env.states2proxy([st])[0])
        tgts = {moves[i].get("source_action", {}).get("target") for i in sel}
        hit += TARGET in tgts
    return hit / len(states)


def main():
    import train_cascade_gfn as t
    from gflownet.utils.common import gflownet_from_config
    circs = json.load(open(HERE / "circumstances-v0.json"))
    circ = next(c for c in circs
                if any(m.get("source_action", {}).get("target") == TARGET
                       for m in c["moves"]))
    print(f"probe circumstance: {circ['id']} ({len(circ['moves'])} moves)")
    cascade_proxy.MOVES = circ["moves"]
    config = t.make_config(len(circ["moves"]))
    config.proxy._target_ = "probe_trainability.FlightGroundedProxy"
    config.gflownet.optimizer.n_train_steps = N_STEPS
    gfn = gflownet_from_config(config)
    pre = target_rate(gfn, circ["moves"])
    t0 = time.time()
    gfn.train()
    post = target_rate(gfn, circ["moves"])
    out = {"corpus": {"total": 33, "masked-out": 31, "masked-in": 2, "clean": 1},
           "probe": {"target": TARGET, "circumstance": circ["id"],
                     "n-train-steps": N_STEPS, "k-samples": K, "seed": SEED,
                     "p-target-untrained": pre, "p-target-trained": post,
                     "train-seconds": round(time.time() - t0, 1)},
           "verdict": ("pipe-trains" if post > pre + 0.15 else "null"),
           "honesty": "n=1 clean example; 31/33 refused by the derived-never-authored mask — the corpus grows one flight at a time"}
    (HERE / "trainability-probe.json").write_text(json.dumps(out, indent=1))
    print(json.dumps(out["probe"], indent=1))
    print("verdict:", out["verdict"])


if __name__ == "__main__":
    main()
