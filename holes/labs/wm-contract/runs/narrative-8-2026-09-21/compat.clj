(require '[futon2.aif.cascade-habit-reinforcement :as reinforcement]
         '[futon2.aif.cascade-habit-reinforcement-test :as fixture]
         '[futon2.report.cascade-habit-read-test :as store])
;; Independent CLI JVM only; never load a branch into the shared server.
(load-file "/home/joe/code/futon2-fix-10a/src/futon2/aif/token_outcome.clj")
(let [compare-outcomes (requiring-resolve 'futon2.aif.token-outcome/compare-outcomes)
      decision {:action (second (store/menu))}
      prediction (:prediction (fixture/comparison decision true))
      token (get-in prediction [:wanted 0 :token])]
  (doseq [observed [true false]]
    (let [receipt (compare-outcomes
                   prediction [{:token token :result {:observed observed :evidence {:resolved-sha "sha"}}}] "sha")
          rule (reinforcement/evaluate decision :grounded-change receipt)]
      (assert (= (if observed :increment :none) (:reinforcement rule)))
      (prn {:observed observed :verdict (get-in receipt [:tokens 0 :verdict])
            :reinforcement (:reinforcement rule) :delta (:delta rule)}))))
