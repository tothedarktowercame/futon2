(ns futon2.aif.observation-parameters-test
  (:require [clojure.test :refer [deftest is]]
            [futon2.aif.observation-model :as obs]))

(def bare {:schema :wm/observation-model-v1 :backend :exact-enumeration
           :kind :independent-judgement :universe #{:a :b}
           :provenance {:status :synthetic :calibrated false :source "parameter identity fixture"}
           :rates {:a {:false-neg 0 :false-pos 9/64}
                   :b {:false-neg 0 :false-pos 9/64}}})
(def shared (-> bare
                (assoc :parameters {:intake-refusal-fp {:value 9/64 :basis "synthetic-test#population"}})
                (assoc-in [:rates :a :false-pos] {:param :intake-refusal-fp})
                (assoc-in [:rates :b :false-pos] {:param :intake-refusal-fp})))
(defn row [model] (obs/query model {:op :row :state #{}}))

(deftest six-controls
  (is (= :unknown-observation-parameter
         (:kind (row (assoc-in shared [:rates :a :false-pos] {:param :absent})))))
  (let [r (row shared)]
    (is (= :computed (:status r)))
    (is (= 1 (count (get-in r [:model :parameters]))))
    (is (= {:param :intake-refusal-fp}
           (get-in r [:model :rates :a :false-pos])
           (get-in r [:model :rates :b :false-pos])))
    (is (= shared (:model r)))
    (is (= (* 9/64 9/64) (get-in r [:distribution #{:a :b}]))))
  (let [r (row bare)]
    (is (= :computed (:status r)))
    (is (not (contains? (:model r) :parameters)))
    (is (= 9/64 (get-in r [:model :rates :a :false-pos])))
    (is (= (:distribution r) (:distribution (row shared)))))
  (doseq [path [[:rates :variance] [:rates :a :variance] [:rates :a :false-pos :variance]]]
    (is (= :inline-rate-variance (:kind (row (assoc-in shared path 1/100))))))
  (is (= :invalid-model-parameters
         (:kind (row (assoc-in shared [:parameters :intake-refusal-fp :value] 0.140625)))))
  (is (= bare (obs/validate! bare))))

(deftest values-resolve-for-every-kind-and-remeasurement
  (let [revised (-> shared
                    (assoc-in [:parameters :intake-refusal-fp :value] 1/2)
                    (assoc-in [:parameters :intake-refusal-fp :basis] "synthetic-test#revision"))]
    (is (= 1/4 (get-in (row revised) [:distribution #{:a :b}])))
    (is (= (:rates shared) (:rates revised))))
  (let [perfect (-> shared (assoc :kind :exact-checks)
                    (assoc-in [:parameters :intake-refusal-fp :value] 0))]
    (is (= {#{} 1} (:distribution (row perfect))))
    (is (= :nonzero-checkable-rate (:kind (row (assoc shared :kind :exact-checks))))))
  (let [coupled (-> shared (dissoc :rates) (assoc :kind :coupled-judgement
                         :components [{:id :one :weight 1/2 :rates (:rates shared)}
                                      {:id :two :weight 1/2 :rates (:rates bare)}]))]
    (is (= (:distribution (row shared)) (:distribution (row coupled))))
    (is (= :unknown-observation-parameter
           (:kind (row (assoc-in coupled [:components 0 :rates :a :false-neg] {:param :absent}))))))
  ;; Metadata is not interpreted as a probability or used for propagation.
  (is (= :computed (:status (row (assoc-in shared [:parameters :intake-refusal-fp :variance] 2)))))
  (doseq [bad [nil 2 -1 1.0]]
    (is (= :invalid-model-parameters
           (:kind (row (assoc-in shared [:parameters :intake-refusal-fp :value] bad)))))))

(deftest established-refusals-unchanged
  (doseq [[path value kind] [[[:schema] :wrong :invalid-model-schema]
                            [[:backend] :wrong :unsupported-observation-backend]
                            [[:universe] #{} :observation-universe-out-of-bounds]
                            [[:provenance :status] :measured :synthetic-provenance-required]
                            [[:rates] nil :invalid-model-rates]]]
    (is (= kind (:kind (row (assoc-in bare path value)))))))
