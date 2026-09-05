(require '[futon2.aif.belief :as b])
(println "status-set count" (count b/status-set) (sort (map name b/status-set)))
(println "uniform" (into (sorted-map) (b/uniform-prior)))
(println "entropy uniform" (b/entropy (b/uniform-prior)) "log7" (Math/log 7))
;; collision: same argmax, same entropy, different posterior
(let [p  {:spawned 0.4 :refined 0.3 :strengthened 0.2 :addressed 0.1
          :falsified 0.0 :foreclosed 0.0 :reopened 0.0}
      p' {:spawned 0.4 :refined 0.2 :strengthened 0.3 :addressed 0.1
          :falsified 0.0 :foreclosed 0.0 :reopened 0.0}]
  (println "p  argmax" (b/most-likely-status p)  "entropy" (b/entropy p))
  (println "p' argmax" (b/most-likely-status p') "entropy" (b/entropy p'))
  (println "distinct?" (not= p p') "same-argmax?" (= (b/most-likely-status p) (b/most-likely-status p'))
           "entropy-delta" (- (b/entropy p) (b/entropy p'))))
;; three-tick re-entry
(let [peaked {:spawned 0.02 :refined 0.02 :strengthened 0.9 :addressed 0.02
              :falsified 0.02 :foreclosed 0.01 :reopened 0.01}
      mu0    {"e" peaked "f" (b/uniform-prior)}
      fresh1 (b/initial-belief-state ["f"])           ; e out of domain at t1
      mu1    (b/reconcile-belief-carry fresh1 mu0)
      fresh2 (b/initial-belief-state ["e" "f"])       ; e back at t2
      mu2    (b/reconcile-belief-carry fresh2 mu1)]
  (println "t1 domain" (sort (keys mu1)) "e present?" (contains? mu1 "e"))
  (println "t2 e =" (into (sorted-map) (get mu2 "e")))
  (println "t2 e = uniform?" (= (get mu2 "e") (b/uniform-prior)))
  (println "peaked lost?" (not= (get mu2 "e") peaked)))
;; survivor / new / vanished
(let [fresh   (b/initial-belief-state ["a" "b"])
      carried {"a" {:spawned 1.0 :refined 0.0 :strengthened 0.0 :addressed 0.0
                    :falsified 0.0 :foreclosed 0.0 :reopened 0.0}
               "c" (b/uniform-prior)}
      r (b/reconcile-belief-carry fresh carried)]
  (println "result domain" (sort (keys r)))
  (println "a carried?" (= (get r "a") (get carried "a")))
  (println "b fresh?"   (= (get r "b") (b/uniform-prior)))
  (println "c dropped?" (not (contains? r "c")))
  (println "r = carried?" (= r carried) " r = fresh?" (= r fresh)))
(println "cold start" (= (b/reconcile-belief-carry (b/initial-belief-state ["a"]) nil)
                         (b/initial-belief-state ["a"])))
(println "channels-with-likelihood" (count b/channels-with-likelihood) (sort (map name b/channels-with-likelihood)))
