(ns wm-ihtb2-check
  "IHTB-2 pre-flip check for the :risk-mode :kl PRODUCTION FLIP
   (E-KL-refinements; mission §8 IHTB-2: the 0.0 cascade-placeholder rows are
   load-bearing — a mode change that pushes ordinary G-totals ABOVE 0.0 hands
   the placeholders argmin wins).

   SIM-only, one-shot (wm_e6_shadow pattern): runs the production judge ONCE
   (read-only), then re-ranks its stashed inputs under the POST-FLIP config
   (:risk-mode :kl, uniform weights, default T; :ambiguity-mode
   :gaussian-entropy — the D5c mode already live). Reports ordinary G-total
   stats vs the placeholder rows' totals.

   PASS iff every placeholder total stays ABOVE (worse than) the ordinary
   winner — i.e. the argmin cannot land on a placeholder.

   RESOLVED AT FLIP TIME (2026-07-03): `efe/rank-actions` NEVER emits
   placeholder rows — the arena appends the cascade rows AFTER `wm-decision`
   is computed (war_machine.clj Car-3 seam: 'so wm-admissible/wm-decision …
   are unaffected'), so placeholders are STRUCTURALLY excluded from the
   selection pool and n-placeholders is 0 here by construction, not by tick
   luck. The check is kept as a regression tripwire should the cascade lane
   ever move ahead of selection. The measured post-flip fact that still
   matters: ordinary G-totals go POSITIVE (≈ +1.7..+6.7) — downstream stats
   consumers must keep excluding placeholder rows when comparing G-totals
   (E6/census convention).

   Usage: clojure -M -m wm-ihtb2-check [days]
   Writes: holes/labs/M-evaluate-policies/ihtb2-flip-check.edn"
  (:require [clojure.pprint :as pp]
            [futon2.aif.efe :as efe]
            [futon2.report.war-machine :as wm]))

(def out-file "holes/labs/M-evaluate-policies/ihtb2-flip-check.edn")

(defn -main [& args]
  (let [days (if (seq args) (Integer/parseInt (first args)) 14)
        _ (println "IHTB-2 check: one read-only production judge run, days =" days "…")
        {:keys [judgement]} (wm/generate-war-machine days)
        {:keys [wm-state candidates]} @wm/!last-wm-inputs
        prod-ranked (:ranked-actions judgement)
        tp (or (:time-pressure (first (filter :G-risk prod-ranked))) 0.0)
        hs (:horizon-steps (first (filter :G-risk prod-ranked)))
        base-opts ((var-get #'wm/live-star-map-efe-opts)
                   ((var-get #'wm/live-gap-view-efe-opts)
                    {:time-pressure tp :horizon-steps hs}))
        post-flip (merge base-opts {:risk-mode :kl
                                    :ambiguity-mode :gaussian-entropy})
        ranked (efe/rank-actions wm-state candidates post-flip)
        ordinary (vec (filter :G-risk ranked))
        placeholders (vec (remove :G-risk ranked))
        totals (mapv #(double (:G-total %)) ordinary)
        ph-totals (mapv #(double (:G-total % 0.0)) placeholders)
        o-min (when (seq totals) (apply min totals))
        o-max (when (seq totals) (apply max totals))
        n-above-min-ph (when (seq ph-totals)
                         (count (filter #(> % (apply min ph-totals)) totals)))
        winner (first (sort-by :G-total ranked))
        pass? (and (seq totals)
                   (or (empty? ph-totals)
                       ;; every placeholder is strictly worse than the winner
                       (every? #(> % o-min) ph-totals)))
        result {:generated-by "scripts/wm_ihtb2_check.clj (pre-flip IHTB-2)"
                :sim-only true :days days
                :config {:risk-mode :kl :kl-channel-weights :uniform-default
                         :c-temperature :default-0.1
                         :ambiguity-mode :gaussian-entropy}
                :n-ordinary (count ordinary)
                :n-placeholders (count placeholders)
                :placeholder-totals ph-totals
                :ordinary-totals {:min o-min :max o-max
                                  :mean (when (seq totals)
                                          (/ (reduce + totals) (count totals)))}
                :n-ordinary-above-placeholder n-above-min-ph
                :winner {:action [(get-in winner [:action :type])
                                  (get-in winner [:action :target])]
                         :G-total (:G-total winner)
                         :placeholder? (nil? (:G-risk winner))}
                :ihtb2-pass? pass?}]
    (spit out-file (with-out-str (pp/pprint result)))
    (pp/pprint result)
    (println "Wrote" out-file)))
