#!/usr/bin/env python3
"""h_value_d.py — H-VALUE-D: a value term with timing and degree, swept.

kimi-7, 2026-09-24. PROOF-2 packet H-VALUE-D (claude-8's requisition).
Deterministic, stdlib only, no repo writes.

V(i) = sum_{o in served(i)} w_o * d_i(o) * u_o  -  lam * E[attempts_i]

Authority of every parameter:
- served(i): mission-C.edn :served-by (claude-1, owner, d0b2e864), binary
  links; the degree d_i(o) in [0,1] is what the binary link leaves unstated.
- E[attempts]: measured, target-cost.edn (claude-10, theta 0.8): 4=8.75,
  5=10, 6=10, 7=8.75.
- w_o: mission states NO weights (:preference :unstated, mission span
  [17969 18029]). Swept over the simplex, step 1/10 (1001 points) —
  declared-with-no-ruling, exactly as claude-10's target_value.py.
- lam: cost weight. No ruling. Swept {0,.02,.05,.1,.2,.4} (claude-10's grid).
- u_o (timing factor per outcome): text-stated for vs-code only — "He is
  **not** asking for those adapters to be built now" (mission span
  [14618 14667]) — read as a deferred value, u_V swept
  {0.1,0.25,0.5,0.75,1.0}. All other u_o = 1: for rob via 4 the text says
  "immediately" (span [18227 18277], i.e. no delay), and no other timing is
  stated, so u=1 there is the absence-of-timing reading, not a claim.
- d_5(rob) (degree of instance 5's service of rob-can-run-the-stack): the
  text states 4 "unblocks provider substitution for Rob immediately"
  (unqualified -> d_4(rob)=1) and 5 "retires matrix-ircd" (span
  [18337 18358]) without stating how fully that serves Rob's whole outcome
  (which mission-C's statement spans provider, shell, transport, editor).
  Degree unstated -> swept {0.25,0.5,0.75,1.0}; d=1 is claude-10's binary
  reading, d<1 the "5 retires one shim of several" reading.
Instance 6 carries no rank in the mission ("Instance 6 is the warning",
span [18901 18926]): the mission order is over {4,5,7} and 6's position is
a typed absence, so 4>5>7 is counted irrespective of where 6 lands.

Outputs: fractions of the swept space under which the mission's 4>5>7
holds, and under which 7 leads, overall and conditioned on the
text-anchored readings (d_5(rob) <= 0.5 and/or u_V <= 0.5), per lambda.
"""
import itertools, json

served = {"4": {"R", "S"}, "5": {"R", "S", "D"}, "6": {"D", "C"}, "7": {"S", "D", "V"}}
cost = {"4": 8.75, "5": 10.0, "6": 10.0, "7": 8.75}
OUTS = ["R", "S", "D", "C", "V"]  # rob, second-impl, drift, coupling, vs-code
N = 10
GRID = [c for c in itertools.product(range(N + 1), repeat=len(OUTS)) if sum(c) == N]
LAMS = [0, 0.02, 0.05, 0.1, 0.2, 0.4]
U_V = [0.1, 0.25, 0.5, 0.75, 1.0]
D5R = [0.25, 0.5, 0.75, 1.0]

def V(i, w, lam, u_v, d5r):
    v = 0.0
    for o in served[i]:
        d = d5r if (i == "5" and o == "R") else 1.0
        u = u_v if o == "V" else 1.0
        v += w[o] * d * u
    return v - lam * cost[i]

def fractions(points):
    n = len(points)
    ok = sum(1 for w, u_v, d5r in points
             if V("4", w, LAM, u_v, d5r) > V("5", w, LAM, u_v, d5r) > V("7", w, LAM, u_v, d5r))
    seven_first = sum(1 for w, u_v, d5r in points
                      if all(V("7", w, LAM, u_v, d5r) > V(j, w, LAM, u_v, d5r) for j in ("4", "5", "6")))
    return {"points": n, "share_4>5>7": round(ok / n, 4), "share_7_first": round(seven_first / n, 4)}

out = {"term": "V(i) = sum_o w_o*d_i(o)*u_o - lam*E[attempts_i]",
       "swept": {"w_simplex_step": 0.1, "grid_points_per_w": len(GRID), "lambda": LAMS,
                  "u_vs_code": U_V, "d_5_rob": D5R},
       "results": {}}
WS = [{o: x / N for o, x in zip(OUTS, c)} for c in GRID]
for LAM in LAMS:
    allpts = [(w, u_v, d5r) for w in WS for u_v in U_V for d5r in D5R]
    baseline = [(w, 1.0, 1.0) for w in WS]  # claude-10's binary, no-timing reading
    text_reading = [p for p in allpts if p[1] <= 0.5 and p[2] <= 0.5]
    deg_only = [p for p in allpts if p[2] <= 0.5]
    tim_only = [p for p in allpts if p[1] <= 0.5]
    out["results"][str(LAM)] = {
        "baseline_d1_u1": fractions(baseline),
        "full_sweep": fractions(allpts),
        "text_reading_u_V<=.5_and_d5R<=.5": fractions(text_reading),
        "degree_only_d5R<=.5": fractions(deg_only),
        "timing_only_u_V<=.5": fractions(tim_only)}
print(json.dumps(out, indent=1, sort_keys=True))
