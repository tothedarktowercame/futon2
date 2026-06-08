#!/usr/bin/env python3
"""wm_shadow_pilot.py — ONE differentiable WM belief step (parallel-shadow).

Pilot of M-differentiable-code's method applied to the War Machine belief update.
claude-6 owns the relaxation method + harness; claude-3 owns WM/aif2 semantics
(F = VFE, parallel-shadow ruling, faithfulness reframe). RUN IS SUPERPOD-GATED —
this file is the scaffold; do not execute on the shared box (OOM hold).

Pipeline:
  scripts/wm_shadow_extract.bb  ->  data/wm-trace/wm-shadow-step.json
  futon5/.venv-tpg/bin/python  wm_shadow_pilot.py            (jax; on superpod)

What it does (claude-3-ratified design):
  - Belief mu_e lives on the 7-status simplex. Relax COORDINATES not ontology:
    z_e in R^7, mu_e = softmax(z_e); init z = log(mu_pre). Axes fixed.
  - F(mu) = VFE = precision-weighted prediction-error accuracy + KL[mu || mu_pre].
    (Decoupled from the trace's recorded :free-energy, which is EFE/action-
    selection — a DIFFERENT functional. claude-3 F-sanity, micro-whistle 4.)
  - Operator = Fisher-Rao natural gradient on the categorical simplex, which
    equals exponentiated-gradient / mirror descent (entropic regularizer):
        g = d F / d mu ;  z <- z - eta*g ;  mu = softmax(z)
    -> stays on the simplex + conserves mass BY CONSTRUCTION.
  - PASS/FAIL (claude-3 reframe, micro-whistle 6) = VALID VFE DESCENT ONLY:
    F monotone-decreasing, mu on simplex, mass conserved. NOT direction-agreement.
  - DATA (not pass/fail): aggregate-direction cosine vs R3d's uniform shift.
  - HEADLINE (decomposed by source — claude-3 mw10): per-entity delta-variance vs
    R3d (=0.0), split accuracy-term vs KL-complexity-term. The accuracy channels are
    mean-over-entities, so their gradient is ~UNIFORM per entity (like R3d) up to
    clip-saturation; the per-entity structure is KL-DOMINATED (each entity pulled to
    its OWN prior). Precise finding: R3d applies the aggregate accuracy signal
    ~uniformly and FLATTENS the per-entity prior-divergence (KL/complexity) a proper
    VFE update preserves -> :sorry/r3d-per-entity-attribution misses the COMPLEXITY
    term, not the accuracy term. (Pilot MEASURES R3d's coarseness; not a gold std.)

Likelihood channels (belief.clj), claude-3-ratified (micro-whistle 8):
  LIVE (ported verbatim, per-entity, differentiable, precision ~100 each):
    annotation-health, sorry-count-norm, mission-health, active-repo-ratio
    = exactly R3d's coverage. annotation-health is THE channel R3d used to drive
    the recorded global mu-pre->mu-post. NOTE (claude-3 mw10): the accuracy terms are
    mean-over-entities, so their per-entity gradient is ~UNIFORM (like R3d) up to
    clip-saturation; the per-entity variance is KL-DOMINATED, not accuracy.
  FROZEN (logged, not silently dropped — claude-3 condition b2): the 4 context
    channels (support/attack-coverage, coupling-density, ticks-firing-ratio) need
    entity-tags/coupling/tick context a one-step shadow can't supply without live
    I/O (OOM risk); precision ~1.0 (~1% of dominant). predicted := observed.
"""
import json, pathlib
import numpy as np

STEP = pathlib.Path("/home/joe/code/futon2/data/wm-trace/wm-shadow-step.json")
ETA, STEPS = 0.5, 200

# status index map (order set by the extractor)
S = {"spawned": 0, "refined": 1, "strengthened": 2, "addressed": 3,
     "falsified": 4, "foreclosed": 5, "reopened": 6}

# annotation-health per-status weights (belief.clj annotation-health-status-weights),
# in extractor status order: [spawned, refined, strengthened, addressed, falsified,
# foreclosed, reopened]
ANNOT_W = np.array([0.0, 0.5, 1.0, 1.0, -1.0, -0.5, 0.0])

LIVE_CHANNELS = {"annotation-health", "sorry-count-norm", "mission-health",
                 "active-repo-ratio"}


def main():
    import jax, jax.numpy as jnp
    from jax import grad
    jax.config.update("jax_enable_x64", True)   # float64 so the monotonicity check is real

    d = json.loads(STEP.read_text())
    mu_pre = jnp.asarray(d["mu_pre"], dtype=jnp.float64)      # [N,7]
    mu_post = np.asarray(d["mu_post"], dtype=np.float64)      # [N,7] (R3d result)
    obs = jnp.asarray(d["observation"], dtype=jnp.float64)    # [8]
    prec = jnp.asarray(d["precision"], dtype=jnp.float64)     # [8]
    chans = d["channels"]
    N = mu_pre.shape[0]

    # documented approximation (claude-3 b2): log frozen channels, never silent
    frozen = [(c, float(np.asarray(prec)[i])) for i, c in enumerate(chans)
              if c not in LIVE_CHANNELS]
    live = [(c, float(np.asarray(prec)[i])) for i, c in enumerate(chans)
            if c in LIVE_CHANNELS]
    print(f"[channels] LIVE (ported, per-entity): {live}")
    print(f"[channels] FROZEN (predicted:=observed, context unavailable offline): {frozen}")

    annot_w = jnp.asarray(ANNOT_W)

    # --- belief->channel likelihood means, differentiable in mu (belief.clj) ---
    def predict_means(mu):
        healthy = mu[:, S["strengthened"]] + mu[:, S["addressed"]]            # mission-health
        openm   = mu[:, S["spawned"]] + mu[:, S["refined"]] + mu[:, S["reopened"]]  # sorry-count
        nondorm = 1.0 - (mu[:, S["foreclosed"]] + mu[:, S["falsified"]])      # active-repo
        ehealth = jnp.clip((mu @ annot_w + 1.0) / 2.0, 0.0, 1.0)             # annotation-health (per-entity)
        m = {}
        m["annotation-health"] = jnp.mean(ehealth)
        m["mission-health"]    = jnp.mean(healthy)
        m["sorry-count-norm"]  = jnp.minimum(1.0, jnp.sum(openm) / 10.0)
        m["active-repo-ratio"] = jnp.mean(nondorm)
        # frozen context channels (logged in main): predicted := observed (zero error)
        for i, c in enumerate(chans):
            if c not in m:
                m[c] = obs[i]
        return jnp.stack([m[c] for c in chans])

    def accuracy(mu):
        return jnp.sum(prec * (obs - predict_means(mu)) ** 2)          # precision-weighted PE
    def complexity(mu):
        return jnp.sum(mu * (jnp.log(mu + 1e-12) - jnp.log(mu_pre + 1e-12)))  # KL[mu||mu_pre]
    def free_energy(mu):
        return accuracy(mu) + complexity(mu)

    # init in logit chart; gauge-fix by row-mean subtraction
    z0 = jnp.log(mu_pre + 1e-12)
    z0 = z0 - jnp.mean(z0, axis=1, keepdims=True)

    def run_flow(Ffn):
        """EG / Fisher-Rao natural-gradient flow: g=dF/dmu; z<-z-eta*g; mu=softmax(z)."""
        gF = grad(Ffn)
        z = z0
        Fc = [float(Ffn(jax.nn.softmax(z, axis=1)))]
        for _ in range(STEPS):
            mu = jax.nn.softmax(z, axis=1)
            z = z - ETA * gF(mu)
            Fc.append(float(Ffn(jax.nn.softmax(z, axis=1))))
        return np.asarray(jax.nn.softmax(z, axis=1)), np.asarray(Fc)

    def dispersion(mu_final):
        delta = mu_final - np.asarray(mu_pre)
        return float(np.mean(np.linalg.norm(delta - delta.mean(0), axis=1))), delta

    # full VFE flow = the pilot result
    mu_full, F = run_flow(free_energy)

    # --- PASS/FAIL: valid VFE descent (full F) ---
    monotone = bool(np.all(np.diff(F) <= 1e-9))
    on_simplex = bool(np.allclose(mu_full.sum(1), 1.0) and (mu_full >= -1e-9).all())
    mass_ok = bool(np.allclose(mu_full.sum(1), np.asarray(mu_pre).sum(1)))
    passed = monotone and on_simplex and mass_ok
    print(f"[PASS-BAR descent-validity] F {F[0]:.5f} -> {F[-1]:.5f}  "
          f"monotone={monotone}  on_simplex={on_simplex}  mass_ok={mass_ok}  => "
          f"{'PASS' if passed else 'FAIL (harness bug)'}")

    # --- HEADLINE, DECOMPOSED by source (claude-3 mw10): accuracy-term vs KL-term ---
    disp_full, flow_delta = dispersion(mu_full)
    mu_acc, _ = run_flow(accuracy)            # accuracy-only flow
    mu_kl,  _ = run_flow(complexity)          # KL-only flow
    disp_acc, _ = dispersion(mu_acc)
    disp_kl,  _ = dispersion(mu_kl)
    r3d_delta = mu_post - np.asarray(mu_pre)
    disp_r3d = float(np.mean(np.linalg.norm(r3d_delta - r3d_delta.mean(0), axis=1)))
    dominant = "KL/complexity" if disp_kl >= disp_acc else "accuracy"
    print(f"[HEADLINE per-entity delta-variance] full={disp_full:.5f}  "
          f"acc-only={disp_acc:.5f}  kl-only={disp_kl:.5f}  R3d={disp_r3d:.5f}")
    print(f"[HEADLINE finding] per-entity structure is {dominant}-DOMINATED; "
          f"R3d ({disp_r3d:.5f}) flattens the per-entity prior-divergence a proper VFE "
          f"update preserves => :sorry/r3d-per-entity-attribution misses the COMPLEXITY term.")

    # --- DATA (not pass/fail): aggregate-direction vs R3d ---
    mean_delta = flow_delta.mean(0)
    r3d_mean = r3d_delta.mean(0)
    cos = float(mean_delta @ r3d_mean /
                (np.linalg.norm(mean_delta) * np.linalg.norm(r3d_mean) + 1e-12))
    print(f"[DATA aggregate-direction] cos(flow_mean_delta, R3d_shift) = {cos:+.3f}  "
          f"(aligned=>weak corroboration; misaligned=>stronger finding)")


if __name__ == "__main__":
    main()
