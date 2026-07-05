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
   `:delta-G/source` for the bake-off.

   ESCROW SEAM (E-live-loop-2 2e, built DARK): when `*escrow-replay?*` is true
   AND the caller injects `:escrow-turn-fn` (from `fold-escrow/escrow-turn-fn`
   over loaded deposits), a classical-fold abstention falls through to the LLM
   fold replaying a RECORDED turn — a hash-gated table lookup, never an LLM
   call on a request path (fold-llm's INCIDENT-SAFE clause). Any prompt drift
   ⇒ nil ⇒ abstain, byte-identical to today. Flag defaults FALSE: with it off
   (or no turn-fn injected) the output map is unchanged, key-for-key —
   finding 6's repair stays dark until 2g arming (Joe's word)."
  (:require [futon2.aif.fold-classical :as fc]
            [futon2.aif.fold-llm :as fl]))

(def ^:dynamic *escrow-replay?*
  "Off until 2g (operator-armed). Off ⇒ the escrow is never consulted and
   `act-gate-from-lane-entry` behaves exactly as before the seam existed."
  false)

(defn act-gate-from-lane-entry
  "PURE: a cascade-lane entry → the act-gate map.
   Entry: {:mission :F-free-energy :G-rollout :shown [pattern-ids…] …}.
   Returns {:delta-F :delta-G :delta-G/source :fold} — :delta-G is the rollout
   leg if present, else the classical fold's coverage-ΔG over :shown, else
   (dark seam, flag-gated) an escrowed fold-turn's replayed ΔG, else nil
   (gate abstains). The fold output is carried for provenance / policy-holes;
   when the escrow leg fires, its full fold output rides `:fold-escrow` (the
   key is ABSENT whenever the escrow was not consulted or returned nothing)."
  ([entry] (act-gate-from-lane-entry entry {}))
  ([entry circumstance] (act-gate-from-lane-entry entry circumstance {}))
  ([{:keys [F-free-energy G-rollout shown] :as _entry} circumstance
    {:keys [escrow-turn-fn prose-fn]}]
   (let [fold-out   (when (seq shown) (fc/classical-fold (vec shown) circumstance))
         fold-g     (:delta-g fold-out)
         escrow-out (when (and *escrow-replay?* escrow-turn-fn
                               (not (number? G-rollout)) (not (number? fold-g))
                               (seq shown))
                      (fl/llm-fold (vec shown) circumstance
                                   {:turn-fn escrow-turn-fn :prose-fn prose-fn}))
         escrow-g   (:delta-g escrow-out)
         delta-G    (cond (number? G-rollout) G-rollout
                          (number? fold-g)    fold-g
                          (number? escrow-g)  escrow-g
                          :else               nil)]
     (cond-> {:delta-F F-free-energy
              :delta-G delta-G
              :delta-G/source (cond (number? G-rollout) :rollout-g-for
                                    (number? fold-g)    :fold
                                    (number? escrow-g)  :fold-escrow
                                    :else               nil)
              :fold fold-out}
       (number? escrow-g) (assoc :fold-escrow escrow-out)))))

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
