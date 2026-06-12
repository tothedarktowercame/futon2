"""C-wholeness proxy for the cascade GFlowNet entrant (S4).

Decodes the Choices/SetFix composite proxy format (int-keyed substates,
tensor([opt_index+1])) into the circumstance's moves and scores them with the
VERIFIED port of score-buildout-c (c_proxy.py; cross-checked vs Clojure,
worst |delta| 1.78e-15). Goodhart guard: this C is GENERATION-only — the
referee never sees it.
"""
import torch

from c_proxy import score_buildout_c
from gflownet.proxy.base import Proxy

# module-level circumstance moves, set by the trainer before agent build
MOVES: list[dict] = []


def selection_from_proxy_state(state: dict) -> list[int]:
    out = []
    for k, v in state.items():
        if isinstance(k, int) and state.get("_dones", [])[k]:
            idx = int(v[0]) - 1
            if idx >= 0:
                out.append(idx)
    return out


class CascadeCProxy(Proxy):
    def __init__(self, **kwargs):
        kwargs.setdefault("reward_min", 1e-6)
        kwargs.setdefault("do_clip_rewards", True)
        super().__init__(**kwargs)

    def setup(self, env=None):
        pass

    def __call__(self, states) -> torch.Tensor:
        vals = []
        for st in states:
            sel = selection_from_proxy_state(st)
            policy = [MOVES[i] for i in sel]
            vals.append(score_buildout_c(policy))
        return torch.tensor(vals, dtype=self.float, device=self.device)
