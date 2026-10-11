(ns futon2.aif.application-order-certificate-test
  (:require [clojure.test :refer [deftest is testing]]
            [futon2.aif.application-order-certificate :as sut]
            [futon2.aif.three-halves-policy-test :as fixture]
            [futon2.aif.wm.cascade-decision :as decision]))

(defn fixture-data []
  (let [problem (fixture/real-problem)
        candidate (first (:constructed-candidates problem))
        policy (fixture/triangle candidate)
        registry-base {:identity 601 :version 1 :authority 602 :receipt 603
                       :implementation-pin :sut/exact-row-order-v1}
        registry (fixture/with-pin "order-law" registry-base)
        certificate-base
        {:schema sut/schema
         :policy-identity (:projection policy)
         :square-population [:s0 :s1 :s2]
         :construction-edges []
         :application-rows [[0 0 0] [1 1 0] [2 2 0]]
         :application-sequence [0 1 2]
         :identity 601 :version 1 :authority 602 :receipt 603
         :implementation-pin :sut/exact-row-order-v1
         :selector-authority 604}
        certificate (fixture/with-pin "application-order" certificate-base)]
    {:problem problem :policy policy :registry registry :certificate certificate
     :controlled {:mode :controlled-sut :policy policy
                  :captured-sources @fixture/captured
                  :application-registry [registry]
                  :application-certificate certificate}}))

(defn reseal-certificate [certificate]
  (fixture/with-pin "application-order-resealed" (dissoc certificate :source)))

(deftest certified-order-is-consumed-by-real-scoring-and-enactment-preparation
  (let [{:keys [problem controlled]} (fixture-data)
        result (decision/controlled-application-order-run controlled problem)]
    (is (= :prepared-for-enactment (:status result)))
    (is (= [:P :Q :R] (get-in result [:scoring :consumed-order])))
    (is (= [:P :Q :R] (get-in result [:dispatch :consumed-order])))
    (is (= (get-in result [:scoring :order-digest])
           (get-in result [:dispatch :order-digest])))
    (is (= [:P :Q :R]
           (mapv :id (get-in result [:lane :selected-action :precedence]))))
    (is (= [:P :Q :R] (get-in result [:dispatch :acting-order])))))

(deftest certificate-and-registry-substitutions-refuse-before-rollout
  (let [{:keys [problem controlled certificate registry]} (fixture-data)
        run (fn [c] (decision/controlled-application-order-run c problem))
        cert-change (fn [path value]
                      (reseal-certificate (assoc-in certificate path value)))]
    (doseq [[label changed]
            [[:reordered-claim (cert-change [:application-sequence] [1 0 2])]
             [:duplicate (cert-change [:application-rows] [[0 0 0] [1 0 0] [2 2 0]])]
             [:omitted (cert-change [:application-rows] [[0 0 0] [1 1 0]])]
             [:unknown (cert-change [:application-rows] [[0 0 0] [1 1 0] [2 9 0]])]
             [:construction-edge (cert-change [:construction-edges] [[0 1]])]
             [:policy-substitution (cert-change [:policy-identity :target] "foreign")]
             [:self-authorized (cert-change [:selector-authority] 602)]
             [:receipt-mismatch (cert-change [:receipt] 999)]]]
      (testing (name label)
        (is (= :application-order-invalid
               (:reason (run (assoc controlled :application-certificate changed)))))))
    (testing "executable substitution is not accepted from the caller registry"
      (let [substituted (assoc registry :decode (constantly [0 1 2]))]
        (is (= :application-order-invalid
               (:reason (run (assoc controlled :application-registry [substituted])))))))
    (testing "no production registry means refusal, never historical precedence fallback"
      (is (= :application-order-invalid
             (:reason (run (assoc controlled :application-registry []))))))))

(deftest consumed-order-mismatch-is-detected-at-both-real-boundaries
  (let [{:keys [problem controlled]} (fixture-data)]
    (is (= :dispatch-order-mismatch
           (:reason (decision/controlled-application-order-run
                     (assoc controlled :enactment-order [:R :Q :P]) problem))))
    (is (= :scoring-order-mismatch
           (:reason (decision/controlled-application-order-run
                     (assoc controlled :scoring-order [:R :Q :P]) problem))))))
