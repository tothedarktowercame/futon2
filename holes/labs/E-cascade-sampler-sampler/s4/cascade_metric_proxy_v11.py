"""GFlowNet proxy adapter for metric-C v1.1 (resolution-scaled + presence-masked)."""
import torch
import cascade_proxy
from gflownet.proxy.base import Proxy
from metric_c_proxy_v11 import metric_c, selection_targets


class CascadeMetricProxyV11(Proxy):
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
            vals.append(metric_c(selection_targets(cascade_proxy.MOVES, sel)))
        return torch.tensor(vals, dtype=self.float, device=self.device)
