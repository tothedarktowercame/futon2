(ns futon2.aif.token-belief-predecessor-test
  (:require [clojure.edn :as edn]
            [clojure.test :refer [deftest is]]
            [futon2.aif.cascade-habit-store :as habit]
            [futon2.aif.declaration-reads-test :as files]
            [futon2.aif.full-loop-runner :as runner]
            [futon2.aif.scoring-input-receipts :as receipts]
            [futon2.aif.token-belief-carry-test :as fixture]
            [futon2.aif.token-belief-predecessor :as predecessor]
            [futon2.aif.trace :as trace]
            [futon2.report.cascade-decision-test :as cascade-fixture]
            [futon2.report.war-machine :as wm]))

(defn decision [previous prior]
  (with-redefs [habit/default-path fixture/absent-habit-path]
    (:decision
     (wm/cascade-decision
      (fixture/assembled)
      (assoc cascade-fixture/live-c-opts
             :cascade-habit-path fixture/absent-habit-path
             :prospective-token-carry prior
             :token-belief-predecessor-trace previous
             :token-belief-context {:occurrence-id "d-2b-fixture"})))))

(deftest real-runner-self-comparison-never-admits-execution
  (let [d (fixture/decision nil)
        selected (:action d)
        comparison (runner/selection-enaction-record selected selected {:source :runner-selection})
        previous {:timestamp "2026-09-20T16:17:55Z" :decision d
                  :selection-enaction comparison
                  :enactment-plan {:chosen-cascade (:id selected) :acting-order []}
                  :acting-order-after []
                  :enactment {:admitted true :authority :production :executed selected}}
        prior (assoc (get-in d [:selection-certificate :token-belief-stage :prospective-carry])
                     :belief {#{} 1})
        result (decision previous prior)
        stage (get-in result [:selection-certificate :token-belief-stage])
        input (get-in result [:selection-certificate :token-belief-input])
        inspected (get-in input [:carry-admission :inspected])]
    (is (= :match (:verdict comparison)))
    (is (= :carry-no-predecessor (get-in input [:carry-admission :kind])))
    (is (= :carry-no-predecessor (get-in input [:carry-admission :authority :kind])))
    (is (= [:selection-is-not-enactment :selection-self-comparison-is-not-enactment
            :independent-token-transition-evidence-unestablished :record-absent
            :plan-is-not-enactment :simulated-order-is-not-enactment]
           (mapv :reason inspected)))
    (is (every? #(= :refused (:admission %)) inspected))
    (is (= (:value (:initial-belief-receipt result)) (:continuation-belief input)))
    (is (not= (:belief prior) (:continuation-belief input)))
    (is (every? #(= (:continuation-belief input) (get-in % [:evaluations 0 :incoming-belief]))
                (get-in result [:selection-certificate :node-evaluation-traces])))
    (is (= (pr-str (fixture/outcomes d)) (pr-str (fixture/outcomes result))))
    (is (= [] (:observation-updates input)))
    (is (not (contains? input :observation)))
    (is (not (contains? input :belief-update-receipt)))
    (is (predecessor/valid-input? input stage))
    (doseq [bad [(assoc input :continuation-belief {#{} 1})
                 (assoc-in input [:carry-admission :status] :admitted)
                 (assoc-in input [:carry-admission :inspected 1 :reason] :enacted)
                 (assoc input :observation-updates [{:status :value}])
                 (assoc-in input [:inspection :candidates 1 :record :verdict] :typed-divergence)]]
      (is (not (predecessor/valid-input? bad stage))))
    (println "D-2B-ADMISSION" (pr-str {:kind (get-in input [:carry-admission :kind])
                                       :candidate-reasons (mapv :reason inspected)
                                       :conditioning-status (:conditioning-status input)
                                       :outcome-bytes-identical true}))))

(deftest changed-domain-initializes-fresh-without-remapping
  (let [d (decision nil nil)
        prior (get-in d [:selection-certificate :token-belief-stage :prospective-carry])
        changed (decision nil (update prior :universe conj [:elsewhere :new-token]))
        input (get-in changed [:selection-certificate :token-belief-input])]
    (is (= :carry-domain-changed (get-in input [:carry-admission :kind])))
    (is (= (:value (:initial-belief-receipt changed)) (:continuation-belief input)))
    (is (= (fixture/outcomes d) (fixture/outcomes changed)))))

(deftest refusal-survives-trace-and-read-time-validation
  (files/with-dir
   (fn [dir]
     (let [d (decision nil nil)
           path (trace/write-trace! {:decision d :belief {"entity" {:healthy 1}}}
                                   :dir (str dir))
           record (edn/read-string (slurp path))
           input-path [:decision :selection-certificate :token-belief-input]
           record (-> record
                      (assoc :habit-reads (receipts/habit-log []))
                      (assoc-in [:decision :selection-certificate :candidates] []))]
       (is (= (get-in d [:selection-certificate :token-belief-input]) (get-in record input-path)))
       (is (= :carry-no-predecessor (get-in record (into input-path [:carry-admission :kind]))))
       (is (= :valid (:status (receipts/validate-record record))))
       (is (= :invalid (:status (receipts/validate-record
                                (assoc-in record (conj input-path :continuation-belief) {#{} 1})))))))))
