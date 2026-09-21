(ns futon2.aif.learning-trial-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-model-manifest :as model]
            [futon2.aif.cascade-selection :as selection]
            [futon2.aif.efe :as efe]
            [futon2.aif.token-outcome :as outcome]))

 ;; Resolve dynamically so the historical bad case fails assertions on base
;; main, where the prospective receipt API does not yet exist.
(try (require 'futon2.aif.learning-trial) (catch java.io.FileNotFoundException _ nil))
(defn receipt [input]
  (when-let [n (find-ns 'futon2.aif.learning-trial)]
    (when-let [f (ns-resolve n 'receipt)] (f input))))

(def fixture (edn/read-string (slurp "test/fixtures/learning-trial/1789964661.edn")))
(def updater ["M-aif-policy-conditioned-eig" :hole/h6378c65a4012])
(defn inputs []
  (let [{:keys [action terms source-record]} fixture
        q0 (get-in terms [:D :value])
        prediction {:status :frozen :action action :initial-belief q0 :horizon 2
                    :wanted [{:token updater :predicted 1}]}
        comparison (outcome/compare-outcomes prediction (:after-token-evidence source-record)
                                             (get-in source-record [:revision-pair :after]))]
    {:comparison comparison :source-record source-record
     :occurrence (get-in source-record [:dispatch :occurrence]) :route (:route source-record)}))
(defn first-trial [inputs] (first (:trials (receipt inputs))))

(deftest historical-updater-is-held-not-learned
  (let [input (inputs) before (pr-str input) row (first-trial input)]
    (is (= :held (:status row)))
    (is (= :observation-placement-not-declared (:reason row)))
    (is (false? (:counted? row)))
    (is (false? (:after-observation row)))
    (is (= :missing (get-in row [:before-observation :status])))
    (is (= :execution-clock-contract-missing (get-in row [:performed-step :reason])))
    (is (= 9/11 (get-in row [:shadow :if-counted-rollout :theta])))
    (let [p #(reduce + (for [[s mass] (get-in row [:shadow % :rollout :belief]) :when (contains? s updater)] mass))]
      (is (= 99/100 (p :prior-rollout)))
      (is (= 117/121 (p :if-counted-rollout))))
    (is (= before (pr-str input)))
    (is (= (receipt input) (receipt input)))))

(deftest held-negatives-do-not-become-counts
  (let [input (inputs) row (first-trial input)
        id (get-in row [:deduplication :identity])
        cases [[:observation-missing (assoc-in input [:comparison :tokens 0 :observed] {:status :missing})]
               [:unselected-target (assoc-in input [:comparison :prediction :action :target] "other")]
               [:unselected-target (assoc-in input [:occurrence :action/value :target] "other")]
               [:effect-already-present (assoc-in input [:comparison :prediction :initial-belief] {#{updater} 1})]
               [:duplicate-replay (assoc input :seen-identities #{id})]
               [:revised-meaning (assoc input :previous-meanings {updater "old-meaning"})]]]
    (doseq [[reason in] cases]
      (let [r (first-trial in)]
        (is (= reason (:reason r)))
        (is (false? (:counted? r)))
        (is (= :held (get-in r [:shadow :status])))))
    (is (= :held (get-in (first-trial (second (first cases))) [:shadow :status])))
    (is (= :illustrative-prior-invalid
           (:reason (first-trial (assoc input :prior {:alpha 0 :beta 1})))))))

(deftest production-scores-and-posterior-are-byte-identical
  (let [terms (:terms fixture)
        c (get-in terms [:C :value]) terminal (:distribution (last (:steps c)))
        spec {:want (set (keys (:weights terminal))) :weights (:weights terminal) :lam 1 :mu 0
              :evidence #{} :zeroed (:zeroed terminal) :c-schedule (:schedule c)}
        score #(efe/rank-cascade-actions {:cascade-belief (get-in terms [:D :value])}
                                         (mapv :id (:candidates fixture))
                                         {:horizon-steps 2 :cascade-spec spec :adjudication-rates (get-in terms [:A :value])})
        before (score)
        post #(selection/selection-posterior {:beta 1 :candidates (:candidates fixture)})
        before-post (post)]
    (receipt (inputs))
    (is (= (pr-str before) (pr-str (score))))
    (is (= (pr-str before-post) (pr-str (post))))
    (doseq [entry before]
      (is (= (:G-efe entry) (:g (first (filter #(= (:action entry) (:id %)) (:candidates fixture)))))))
    (is (= (:posterior fixture) before-post))
    (is (= :first-enabled-union-theta-v1
           (get-in (model/rollout-evaluation (constantly (get-in fixture [:action :precedence]))
                                            (get-in terms [:D :value]) 2)
                   [:evaluations 0 :model :semantics])))))
