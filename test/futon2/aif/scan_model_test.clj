(ns futon2.aif.scan-model-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.belief :as belief]
            [futon2.aif.scan-model :as scan-model]
            [futon2.aif.trace :as trace]
            [futon2.report.war-machine :as wm]))

(def prior {:alpha 1 :beta 1 :authority "scan-model-test"})
(def statuses (vec (sort belief/status-set)))

(defn- mu [& pairs]
  (merge (zipmap statuses (repeat 0)) (into {} pairs)))

(defn- record [belief-row support attack]
  {:mu-pre {"entity" belief-row}
   ;; Deliberately poisonous: the learner must never read this posterior.
   :mu-post {"entity" (mu [:foreclosed 1])}
   :scan-exposures {:support support :attack attack}})

(defn- hand-set []
  {:support (get belief/channel-emission-matrix :support-coverage)
   :attack (get belief/channel-emission-matrix :attack-coverage)})

(deftest scan-exposures-are-exact-and-persisted
  (let [entries [{:evidence/at "2099-01-01"
                  :evidence/body {:text "evidence AIF commercial complexity"}}]
        scan (wm/scan-support-attack 30 entries)
        exposures (:scan-exposures scan)
        traced (trace/trace-record {:belief {} :belief-pre {}
                                    :observation {} :decision nil
                                    :scan-exposures exposures})]
    (is (= {:support {:covered 3 :claims 5}
            :attack {:covered 1 :claims 4}}
           exposures))
    (is (= exposures (:scan-exposures traced))))
  (with-redefs-fn {#'wm/claim-patterns {}}
    (fn []
      (let [scan (wm/scan-support-attack 30 [])]
        (is (= {:support {:status :absent :reason :no-claims}
                :attack {:status :absent :reason :no-claims}}
               (:scan-exposures scan)))
        ;; Existing numeric consumers retain their old zero projection.
        (is (= 0.0 (:support-coverage scan) (:attack-coverage scan)))))))

(deftest learn-uses-mu-pre-and-fractional-counts
  (let [records [(record (mu [:spawned 1/2] [:refined 1/2])
                         {:covered 2 :claims 5}
                         {:status :absent :reason :no-claims})
                 (record (mu [:spawned 1/4] [:refined 3/4])
                         {:covered 1 :claims 4}
                         {:covered 3 :claims 4})]
        learned (scan-model/learn records "entity" prior)]
    (is (= {:alpha 9/4 :beta 13/4 :success 5/4 :failure 9/4}
           (get-in learned [:posterior :support :spawned])))
    (is (= {:alpha 11/4 :beta 19/4 :success 7/4 :failure 15/4}
           (get-in learned [:posterior :support :refined])))
    (is (= {:alpha 13/4 :beta 7/4 :success 9/4 :failure 3/4}
           (get-in learned [:posterior :attack :refined])))
    (is (= [{:record 0 :channel :attack :reason :no-claims}]
           (:excluded learned)))))

(deftest tied-reduction-has-registry-sign
  (testing "equal status rates favour tying"
    (let [records (mapv #(record (mu [% 1]) {:covered 5 :claims 10}
                                {:status :absent :reason :no-claims}) statuses)
          score (scan-model/score (scan-model/learn records "entity" prior)
                                  prior (hand-set))]
      (is (neg? (get-in score [:reductions :support :tied :delta-f])))
      (is (= false (:applied score)))
      (is (= -3.0 (:threshold score)))
      (is (= :wm/scan-model-v1 (:schema score)))))
  (testing "strongly different status rates disfavour tying"
    (let [[a b] statuses
          records [(record (mu [a 1]) {:covered 100 :claims 100}
                           {:status :absent :reason :no-claims})
                   (record (mu [b 1]) {:covered 0 :claims 100}
                           {:status :absent :reason :no-claims})]
          score (scan-model/score (scan-model/learn records "entity" prior)
                                  prior (hand-set))]
      (is (pos? (get-in score [:reductions :support :tied :delta-f]))))))

(deftest hand-set-reduction-is-computed-or-typed
  (let [learned (scan-model/learn
                 [(record (mu [:strengthened 1]) {:covered 5 :claims 5}
                          {:covered 4 :claims 4})]
                 "entity" prior)
        score (scan-model/score learned prior (hand-set))]
    (is (or (number? (get-in score [:reductions :support :hand-set :delta-f]))
            (true? (get-in score [:reductions :support :hand-set
                                  :impossible-under-reduced]))))
    (is (= :hand-set-not-comparable
           (get-in (scan-model/score learned prior {})
                   [:reductions :support :hand-set :status])))))

(deftest invalid-prior-is-a-typed-refusal
  (doseq [bad [{:alpha 1 :beta 1} {:alpha 0 :beta 1 :authority "bad"}]]
    (is (= :invalid-prior (:kind (scan-model/learn [] "entity" bad))))
    (is (= :invalid-prior (:kind (scan-model/score {} bad {}))))))
