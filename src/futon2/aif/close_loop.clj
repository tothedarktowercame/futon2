(ns futon2.aif.close-loop
  "R16 close-the-loop helper — construct the act-gate's two legs for a served
   mission, so the gate can verdict `:pass`/`:fail` instead of `:abstain`
   (E-close-the-loop). PURE given a cascade-lane entry; the live pilot wires
   `act-gate-for` in (a separate, careful step).

   The act-gate is `{:delta-F :delta-G}`. cascade-lane already computes both:
     ΔF = `:F-free-energy` — the cascade's marginal-likelihood F (F>0 = Bayesian-
          Occam accept). Already solved upstream.
     ΔG = `:G-rollout`     — best-rollout G(π) over the v2 move-set restricted to
          the mission. **nil whenever the mission has no moves in the set** —
          that's why the gate abstains.

   This helper RECONCILES ΔG (Joe, 2026-06-26): prefer the move-set-grounded
   `:G-rollout` when present; **fall back to the FOLD's coverage-ΔG**
   (`futon2.aif.fold-classical` over the cascade's `:shown` patterns) when it is
   nil — exactly the case the classical fold (impl #1) rescues. The reconciliation
   is itself a build×evaluation grid cell; the source is recorded in
   `:delta-G/source` for the bake-off."
  (:require [futon2.aif.fold-classical :as fc]))

(defn act-gate-from-lane-entry
  "PURE: a cascade-lane entry → the act-gate map.
   Entry: {:mission :F-free-energy :G-rollout :shown [pattern-ids…] …}.
   Returns {:delta-F :delta-G :delta-G/source :fold} — :delta-G is the rollout
   leg if present, else the classical fold's coverage-ΔG over :shown, else nil
   (gate abstains). The fold output is carried for provenance / policy-holes."
  ([entry] (act-gate-from-lane-entry entry {}))
  ([{:keys [F-free-energy G-rollout shown] :as _entry} circumstance]
   (let [fold-out (when (seq shown) (fc/classical-fold (vec shown) circumstance))
         fold-g   (:delta-g fold-out)
         delta-G  (cond (number? G-rollout) G-rollout
                        (number? fold-g)    fold-g
                        :else               nil)]
     {:delta-F F-free-energy
      :delta-G delta-G
      :delta-G/source (cond (number? G-rollout) :rollout-g-for
                            (number? fold-g)    :fold
                            :else               nil)
      :fold fold-out})))

(defn preview-verdict
  "What the act-gate WOULD decide for these legs (mirrors the pilot backend's
   gate, for previewing/tests — the live verdict stays the gate's)."
  [{:keys [delta-F delta-G]}]
  (cond (or (nil? delta-F) (nil? delta-G)) :abstain-missing-leg
        (and (pos? delta-F) (neg? delta-G)) :pass
        :else :fail))

(defn act-gate-for
  "LIVE path: build act-gates for the top :open-mission ranked actions by running
   the cascade-lane (which shells the cascade builder, memoized) then reconciling
   each entry. Returns [{:mission :act-gate :verdict} …]. Side-effecting only via
   the read-only/memoized cascade-lane; no writes, no loop."
  [ranked-actions]
  (let [lane ((requiring-resolve 'futon2.report.cascade-lane/cascade-lane) ranked-actions)]
    (mapv (fn [entry]
            (let [ag (act-gate-from-lane-entry entry)]
              {:mission (:mission entry) :act-gate ag :verdict (preview-verdict ag)}))
          lane)))
