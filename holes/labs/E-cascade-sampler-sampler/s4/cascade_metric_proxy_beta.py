"""GFlowNet proxy adapter: metric-C with reward temperature R = exp(beta*(C - 2)).

TN-metric-review.md lever 1: the TB target R/Z gets no gradient from a linear
C with ~1.6x within-k range; exp(beta*C) restores one (beta=3 -> ~35x,
beta=8 -> ~13000x, measured on circ-06). The -2 shift keeps magnitudes tame
and only shifts logZ. Beta from env CASCADE_BETA. Everything else identical
to cascade_metric_proxy (same matrix, same C) so beta is the ONLY lever.
"""
import math
import os

import torch
import cascade_proxy
from gflownet.proxy.base import Proxy
from metric_c_proxy import metric_c, selection_targets

BETA = float(os.environ.get("CASCADE_BETA", "3.0"))


class CascadeMetricBetaProxy(Proxy):
    def __init__(self, **kw):
        kw.setdefault("reward_min", 1e-9)
        kw.setdefault("do_clip_rewards", True)
        super().__init__(**kw)

    def setup(self, env=None):
        pass

    def __call__(self, states):
        vals = []
        for st in states:
            sel = cascade_proxy.selection_from_proxy_state(st)
            c = metric_c(selection_targets(cascade_proxy.MOVES, sel))
            vals.append(math.exp(BETA * (c - 2.0)))
        return torch.tensor(vals, dtype=self.float, device=self.device)
