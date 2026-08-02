(require '[ants.aif.experiment :as e])
;; PROBE, not a registered experiment. Sole purpose: locate a viability window.
;; Hypothesis: R-0 held patch COUNT fixed while area grew 13x, so food DENSITY
;; collapsed and the colonies starved. Fix = patches proportional to area.
(let [base-size 10 base-patches 4 base-ticks 300
      grids [10 24 36]
      arms {:aif-full {} :no-directed-eig {:epistemic 0.0}}
      seeds (range 5)]
  (println "grid patches ticks | arm              | yield   starv | identical-to-full")
  (doseq [g grids]
    (let [area-ratio (/ (* g g) (double (* base-size base-size)))
          patches (max 1 (int (Math/round (* base-patches area-ratio))))
          ticks   (int (Math/round (* base-ticks (/ g (double base-size)))))
          results (into {}
                    (for [[arm ov] arms]
                      [arm (vec (for [i seeds]
                                  (#'ants.aif.experiment/run-single
                                    :aif :patchy
                                    (+ 202611110 (* 1000 g) (* 2 i))
                                    (+ 202611111 (* 1000 g) (* 2 i))
                                    [g g] ticks false
                                    :choice-seed (+ 202661110 (* 1000 g) i)
                                    :metabolism 0.06 :initial-reserves 0.5
                                    :ants-per-side 3
                                    :efe-lambda-overrides ov
                                    :food-opts {:num-patches patches :patch-radius 2})))]))
          full (:aif-full results)]
      (doseq [[arm rs] results]
        (let [y (/ (reduce + (map :yield rs)) (double (count rs)))
              s (/ (reduce + (map :starved rs)) (double (count rs)))
              same (count (filter true? (map = full rs)))]
          (println (format "%4d %7d %5d | %-16s | %7.2f  %.3f | %d/%d"
                           g patches ticks (name arm) y s same (count rs))))))))
