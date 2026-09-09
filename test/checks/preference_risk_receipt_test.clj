(ns checks.preference-risk-receipt-test
  (:require [clojure.test :refer [deftest is]]
            [checks.preference-risk-receipt :as cert]
            [futon2.aif.disposition-risk :as risk]
            [futon2.aif.efe :as efe]
            [futon2.aif.ruled-outcome-c :as ruled]))

(def artifact
  {:schema :wm/disposition-kernel-v1
   :source {:ledger "control" :sha256 "control"}
   :conditioning {:grain :checkpoint-trajectory}
   :support (vec (sort (:support ruled/seeded-c)))
   :states [{:sample-size 3
             :probability (assoc (zipmap (:support ruled/seeded-c) (repeat 0)) :grounded-change 1)}]})

(deftest boundary-and-provenance-controls
  (let [adapter (risk/constant-checkpoint-kernel artifact)
        original efe/compute-efe]
    (is (= 10 (count (:checks (cert/runtime-checks artifact adapter)))))
    (with-redefs [efe/compute-efe (fn [s a o] (assoc (original s a o) :G-ruled-outcome-c 0.0))]
      (is (cert/refused? #(cert/runtime-checks artifact adapter))))
    (is (cert/refused? #(cert/runtime-checks artifact
                                          (with-meta adapter (assoc (meta adapter) :observation-model-bridge :closed)))))))

(deftest binding-support-and-exactness
  (is (cert/refused? #(cert/binding-proof {} ruled/seeded-c))))
