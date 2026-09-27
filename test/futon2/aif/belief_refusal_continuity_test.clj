(ns futon2.aif.belief-refusal-continuity-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.belief :as b]
            [futon2.aif.free-energy :as fe]
            [futon2.aif.trace :as trace]))

(deftest impossible-evidence-remains-refused-through-readers
  (let [prior (zipmap b/status-set (map #(if (= % :spawned) 1 0) b/status-set))
        ;; Valid dense categorical A with zero likelihood for the supplied event.
        a (into {} (for [o b/status-set]
                     [o (zipmap b/status-set (repeat (if (= o :spawned) 1 0)))]))
        event {:type :strengthened :weight 1 :entity-id :e}
        opts {:likelihood-mode :aif :observation-model a :transition-model b/transition-model-v1}
        post (b/update-belief {:e prior} event opts)
        refusal (:e post)
        carried (b/reconcile-belief-carry {:e (b/uniform-prior)} post)
        next-post (b/update-belief carried (assoc event :type :spawned) opts)
        record (trace/trace-record {:belief-pre {:e prior} :belief next-post})]
    (is (= {:status :refused :reason :impossible-observation} refusal))
    (is (= refusal (b/entity-expected-health refusal)))
    (is (= post carried next-post (:mu-post record)))
    (doseq [[_ prediction] (b/predict-observation next-post {} {})]
      (is (= :refused (:status prediction)))
      (is (not (contains? prediction :mean)))
      (is (= :refused (:status (fe/compute-prediction-error 0.5 prediction)))))))

(deftest missing-likelihood-is-not-zero-likelihood
  (let [a (update b/observation-model-v1 :spawned dissoc :refined)]
    (is (false? (b/valid-observation-model? a)))
    (is (= :invalid-observation-model
           (:reason (b/categorical-filter-step (b/uniform-prior)
                     {:type :spawned} a b/transition-model-v1 {:weight 1}))))))

(deftest explicit-missing-model-does-not-select-the-default
  (doseq [[key reason] [[:observation-model :invalid-observation-model]
                        [:transition-model :invalid-transition-model]]]
    (is (= reason
           (:reason (b/update-entity-belief (b/uniform-prior)
                      {:type :spawned :weight 1}
                      {:likelihood-mode :aif key nil}))))))
