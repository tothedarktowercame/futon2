(ns wm-t-calibration
  "E-KL-refinements item 3 — the T-calibration APPARATUS (data + plumbing, NOT a
   changed default; `pref/default-c-temperature` stays 0.1). Measures what
   `:c-temperature` WOULD match the production `:hinge` risk dispersion, so the
   operator can decide a `:kl` flip informed by numbers.

   SIM-ONLY, one-shot (modeled on `scripts/wm_e6_shadow.clj`): runs the production
   judge ONCE (read-only — no trace, no enactment), then re-ranks the judge's OWN
   stashed inputs (`wm/!last-wm-inputs`) under `:hinge` and under `:kl` across a
   log-spaced T grid, at BOTH weight configs (uniform + :pragmatic-parity). Cascade
   placeholder rows (no `:G-risk`) are excluded.

   E4 LESSON (binds): calibrate on WITHIN-TICK dispersion only, never corpus σ. The
   artifact is a SINGLE-TICK snapshot; the flip decision can re-run it on later
   ticks. The per-channel T table is a flagged PROPOSAL, not applied.

   Run: cd futon2 && clojure -M -m wm-t-calibration [days]"
  (:require [clojure.java.io :as io]
            [clojure.pprint :as pp]
            [futon2.aif.efe :as efe]
            [futon2.aif.forward-model :as fm]
            [futon2.aif.preferences :as pref]
            [futon2.report.war-machine :as wm]))

(def out-file "holes/labs/M-evaluate-policies/t-calibration.edn")

(defn- sd [xs]
  (let [xs (map double xs) n (count xs)]
    (if (< n 2) 0.0
        (let [m (/ (reduce + xs) n)
              v (/ (reduce + (map #(let [d (- % m)] (* d d)) xs)) (dec n))]
          (Math/sqrt v)))))

(defn- log-grid [lo hi n]
  (let [llo (Math/log lo) lhi (Math/log hi)]
    (mapv (fn [i] (Math/exp (+ llo (* (/ (double i) (dec n)) (- lhi llo))))) (range n))))

(defn- t-star
  "Bisection (in log-T) for kl-sd(T*) = target. Brackets from the grid crossing
   (robust to non-strict monotonicity), then bisects assuming kl-sd DECREASING in T
   (softer C ⇒ smaller risk spread) within the bracket. nil if no grid crossing."
  [kl-sd-fn grid sds target]
  (when-let [[t0 t1] (some (fn [[[a sa] [b sb]]]
                             (when (<= (min sa sb) target (max sa sb)) [a b]))
                           (partition 2 1 (map vector grid sds)))]
    (loop [lo t0 hi t1 i 0]
      (if (>= i 50)
        (Math/sqrt (* lo hi))
        (let [mid (Math/sqrt (* lo hi)) s (kl-sd-fn mid)]
          (cond (< (Math/abs (- s target)) 1e-9) mid
                (> s target) (recur mid hi (inc i))     ; too dispersed ⇒ need softer (larger) T
                :else        (recur lo mid (inc i))))))))

(defn -main [& args]
  (let [days (if (seq args) (Integer/parseInt (first args)) 14)
        _ (println "T-calibration: one read-only production judge run, days =" days "…")
        {:keys [judgement]} (wm/generate-war-machine days)
        {:keys [wm-state candidates]} @wm/!last-wm-inputs
        ordinary (fn [ranked] (vec (filter :G-risk ranked)))
        prod (ordinary (:ranked-actions judgement))
        tp (or (:time-pressure (first prod)) 0.0)
        hs (:horizon-steps (first prod))
        base-opts ((var-get #'wm/live-star-map-efe-opts)
                   ((var-get #'wm/live-gap-view-efe-opts)
                    {:time-pressure tp :horizon-steps hs}))
        risk-sd (fn [extra] (sd (map :G-risk (ordinary (efe/rank-actions wm-state candidates
                                                                         (merge base-opts extra))))))
        hinge-sd (risk-sd {})
        grid (log-grid 0.02 2.0 30)
        wcfgs {:uniform {} :pragmatic-parity :pragmatic-parity}
        curves (into {} (for [[label w] wcfgs]
                          [label (mapv (fn [t] (risk-sd {:risk-mode :kl :c-temperature t
                                                         :kl-channel-weights w})) grid)]))
        t-stars (into {} (for [[label sds] curves]
                           [label (t-star (fn [t] (risk-sd {:risk-mode :kl :c-temperature t
                                                            :kl-channel-weights (get wcfgs label)}))
                                          grid sds hinge-sd)]))
        ;; per-channel WITHIN-TICK predicted σ_ch across the scored candidates
        cc-keys (vec (keys (pref/current-C)))
        sig-rows (keep (fn [c]
                         (let [v (try (get-in (fm/predict wm-state c) [:next-observation :variance])
                                      (catch Throwable _ nil))]
                           (when (map? v)
                             (into {} (for [ch cc-keys :when (number? (get v ch))]
                                        [ch (Math/sqrt (max 0.0 (double (get v ch))))])))))
                       candidates)
        sig-per-ch (into {} (for [ch cc-keys
                                  :let [xs (keep #(get % ch) sig-rows)]
                                  :when (seq xs)]
                              [ch {:mean (/ (reduce + xs) (count xs))
                                   :max  (apply max xs)}]))
        sigma-bar (let [ms (map (comp :mean val) sig-per-ch)]
                    (if (seq ms) (/ (reduce + ms) (count ms)) 0.0))
        ;; T* came back nil for both configs — kl-sd never crosses hinge-sd in the
        ;; grid. Characterise WHY: kl-sd floors (softest T, T→∞ ⇒ C uniform ⇒ KL →
        ;; -H[Q~], whose spread across candidates is T-independent). If that floor
        ;; is above hinge-sd, NO T matches — the dispersion gap is structural (the
        ;; entropy term), not a T-choice.
        closest-T (into {} (for [[label sds] curves]
                             [label (first (apply min-key (fn [[_ s]] (Math/abs (- s hinge-sd)))
                                                  (map vector grid sds)))]))
        floor (into {} (for [[label sds] curves]
                         [label {:softest-T-sd (last sds)
                                 :ratio-to-hinge (when (pos? hinge-sd) (/ (last sds) hinge-sd))
                                 :crosses-grid? (boolean (get t-stars label))}]))
        ;; anchor the per-channel proposal on the exact T* if it exists, else the
        ;; closest-achievable grid T — flagged illustrative, since no T matches here.
        anchor-T (or (:uniform t-stars) (:uniform closest-T))
        per-channel-T (when (and anchor-T (pos? sigma-bar))
                        (into {} (for [[ch {m :mean}] sig-per-ch]
                                   [ch (* anchor-T (/ m sigma-bar))])))
        result
        {:generated-by "scripts/wm_t_calibration.clj (E-KL-refinements item 3)"
         :sim-only true :days days
         :n-candidates (count prod)
         :cascade-rows-excluded (- (count (:ranked-actions judgement)) (count prod))
         :reference {:hinge-within-tick-risk-sd hinge-sd}
         :T-grid grid
         :kl-risk-sd-curves curves
         :T-star t-stars
         :closest-achievable-T closest-T
         :dispersion-floor floor
         :per-channel-sigma sig-per-ch
         :sigma-bar sigma-bar
         :per-channel-T-PROPOSAL
         {:formula "T_ch = anchor-T · (σ_ch_mean / σ̄),  σ̄ = mean over channels of σ_ch_mean"
          :anchor-T anchor-T
          :anchor-is-exact-T*? (boolean (:uniform t-stars))
          :applied? false
          :table per-channel-T}
         :headline-finding
         (if (some identity (vals t-stars))
           "A T matches hinge dispersion (see :T-star) — the flip is a T-choice."
           (format (str "NO T matches hinge dispersion (sd %.4f). kl-sd floors at "
                        "uniform×%.1f / parity×%.1f even at the softest T — the gap is "
                        "STRUCTURAL (the entropy term -H[Q~] disperses across candidates "
                        "independent of T), not a T-choice. :pragmatic-parity (×%.1f) is "
                        "far closer than uniform (×%.1f).")
                   hinge-sd
                   (get-in floor [:uniform :ratio-to-hinge])
                   (get-in floor [:pragmatic-parity :ratio-to-hinge])
                   (get-in floor [:pragmatic-parity :ratio-to-hinge])
                   (get-in floor [:uniform :ratio-to-hinge])))
         :caveats
         ["SINGLE-TICK snapshot — within-tick dispersion only (E4 lesson); re-run on later ticks before a flip."
          "default-c-temperature stays 0.1 (unfitted/dark); this is data for the operator's flip decision, not a changed default."
          "kl-sd assumed monotone-decreasing in T for the bisection; the full curve is emitted so the operator can eyeball it."
          "no exact T* here ⇒ per-channel-T is anchored on the CLOSEST-achievable T and is ILLUSTRATIVE plumbing, not a fit."
          ":pragmatic-parity is a comparability tool (Σw·KL is joint-KL only at uniform weights); uniform is the canonical flip candidate."]}]
    (io/make-parents out-file)
    (spit out-file (with-out-str (pp/pprint result)))
    (println "\n=== T-CALIBRATION SUMMARY ===")
    (println (format "hinge within-tick risk sd = %.5f" hinge-sd))
    (doseq [[label t*] t-stars]
      (println (format "T* (%s) = %s  (floor sd %.4f = ×%.1f hinge; closest grid-T %.3f)"
                       (name label) (if t* (format "%.4f" t*) "NO crossing")
                       (get-in floor [label :softest-T-sd])
                       (get-in floor [label :ratio-to-hinge])
                       (get closest-T label))))
    (println (:headline-finding result))
    (println (format "σ̄ = %.5f over %d channels; per-channel-T anchored on %.4f (exact-T*? %s, not applied)"
                     sigma-bar (count sig-per-ch) (or anchor-T 0.0) (boolean (:uniform t-stars))))
    (println "wrote" out-file)))
